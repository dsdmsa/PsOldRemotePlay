#!/usr/bin/env python3
"""
Train NeuralUp-v2: Optimized tiny neural network upscaler.

Architecture: 17 -> 32 (PReLU) -> 1
  Input:  9 luma (3x3) + 4 luma (dilated ±2) + gradient_mag + variance + 2 sub-pixel = 17
  Output: 1 luma residual (added to bilinear baseline)

Key improvements over v1 (209 params, +0.42dB):
  - 17 inputs: dilated cross for 5x5 receptive field, gradient mag, local variance
  - 32 hidden neurons with PReLU (learnable negative slopes)
  - Charbonnier + gradient loss (edge-preserving)
  - 8-fold data augmentation (rotations + flips)
  - Cosine annealing with warm restarts (SGDR)
  - Learning rate warmup
  - Stochastic Weight Averaging (SWA)
  - Residual output scaling (folded into weights after training)

Parameters: 641 -> folded to 609 after training
MACs/pixel: ~601 (vs 192 in v1)
Expected: +1.0-1.5 dB over bilinear (vs +0.42 in v1)

Usage:
    python3 tools/train-tiny-sr/train_v2.py

Output:
    tools/train-tiny-sr/weights_v2.glsl  - GLSL const arrays
    tools/train-tiny-sr/model_v2.pth     - PyTorch checkpoint
"""

import os, sys, io, glob, time, copy
import numpy as np
import torch
import torch.nn as nn
import torch.nn.functional as F
import torch.optim as optim
from torch.utils.data import DataLoader, TensorDataset
from PIL import Image

# --- Configuration ---
SCALE = 3
HIDDEN = 32
N_INPUTS = 17  # 9 luma + 4 dilated + grad_mag + variance + fx + fy
EPOCHS = 400
BATCH = 4096
LR = 1e-3
JPEG_Q = 75
MAX_SAMPLES_PER_IMAGE = 80000
WARMUP_STEPS = 1000
GRAD_CLIP = 1.0
SWA_START_FRAC = 0.8  # Start SWA at 80% of training
RESIDUAL_SCALE_INIT = 0.1  # Initial output scaling factor
LOSS_ALPHA = 1.0       # Charbonnier weight
LOSS_BETA = 0.1        # Gradient loss weight
CHARBONNIER_EPS = 1e-3


class NeuralUpV2(nn.Module):
    """17 -> 32 (PReLU) -> 1 with learnable residual scaling."""

    def __init__(self):
        super().__init__()
        self.fc1 = nn.Linear(N_INPUTS, HIDDEN)
        self.prelu = nn.PReLU(num_parameters=HIDDEN, init=0.1)
        self.fc2 = nn.Linear(HIDDEN, 1)
        self.residual_scale = nn.Parameter(torch.tensor(RESIDUAL_SCALE_INIT))

        # Kaiming init for PReLU (a=0.1 = initial negative slope)
        nn.init.kaiming_normal_(self.fc1.weight, a=0.1, mode='fan_in', nonlinearity='leaky_relu')
        nn.init.zeros_(self.fc1.bias)
        nn.init.normal_(self.fc2.weight, std=0.01)
        nn.init.zeros_(self.fc2.bias)

    def forward(self, x):
        h = self.prelu(self.fc1(x))
        return self.fc2(h) * self.residual_scale

    def fold_residual_scale(self):
        """Fold residual_scale into fc2 weights/bias for deployment."""
        with torch.no_grad():
            s = self.residual_scale.item()
            self.fc2.weight.mul_(s)
            self.fc2.bias.mul_(s)
            self.residual_scale.fill_(1.0)

    def param_count(self):
        return sum(p.numel() for p in self.parameters())


def luma(rgb):
    return 0.2126 * rgb[..., 0] + 0.7152 * rgb[..., 1] + 0.0722 * rgb[..., 2]


def jpeg_degrade(img, q=JPEG_Q):
    buf = io.BytesIO()
    img.save(buf, format='JPEG', quality=q)
    buf.seek(0)
    return Image.open(buf).convert('RGB')


def compute_features(lr_pad, iy, ix):
    """
    Extract 17 input features for a given LR pixel position.

    lr_pad: padded LR luma array (padded by 2 on each side)
    iy, ix: position in the UNPADDED LR image (so lr_pad coords are iy+2, ix+2)

    Returns: [9 luma (3x3), 4 dilated, grad_mag, variance]  (15 values, fx/fy added later)
    """
    # Offset for padding=2
    cy, cx = iy + 2, ix + 2

    # 3x3 neighborhood (9 values)
    patch = lr_pad[cy-1:cy+2, cx-1:cx+2].flatten()  # shape (9,)

    # Dilated cross at ±2 (4 values)
    d_top    = lr_pad[cy - 2, cx]
    d_bottom = lr_pad[cy + 2, cx]
    d_left   = lr_pad[cy, cx - 2]
    d_right  = lr_pad[cy, cx + 2]

    # Gradient magnitude (Sobel-like from 3x3)
    # Horizontal gradient: right column - left column
    gx = (patch[2] + 2*patch[5] + patch[8]) - (patch[0] + 2*patch[3] + patch[6])
    # Vertical gradient: bottom row - top row
    gy = (patch[6] + 2*patch[7] + patch[8]) - (patch[0] + 2*patch[1] + patch[2])
    grad_mag = np.sqrt(gx*gx + gy*gy + 1e-8)

    # Local variance of 3x3
    mean_val = patch.mean()
    variance = ((patch - mean_val) ** 2).mean()

    features = np.concatenate([
        patch,                                    # 9: 3x3 luma
        [d_top, d_bottom, d_left, d_right],       # 4: dilated cross
        [grad_mag],                                # 1: gradient magnitude
        [variance],                                # 1: local variance
    ])  # total: 15 (fx, fy appended per sub-pixel)

    return features


def augment_patch(features, aug_idx):
    """
    Apply one of 8 augmentations (4 rotations x 2 flips) to the 3x3 + dilated features.

    aug_idx: 0-7
      0: identity
      1: rot90
      2: rot180
      3: rot270
      4: flip_h
      5: flip_h + rot90
      6: flip_h + rot180
      7: flip_h + rot270

    features layout: [9 luma (3x3 row-major), 4 dilated (top,bottom,left,right), grad_mag, variance]
    """
    # Extract components
    patch_3x3 = features[:9].reshape(3, 3)
    d_top, d_bottom, d_left, d_right = features[9], features[10], features[11], features[12]
    grad_mag = features[13]
    variance = features[14]

    flip_h = aug_idx >= 4
    rot = aug_idx % 4

    if flip_h:
        patch_3x3 = patch_3x3[:, ::-1].copy()
        d_left, d_right = d_right, d_left

    for _ in range(rot):
        patch_3x3 = np.rot90(patch_3x3, -1)  # clockwise rotation
        d_top, d_right, d_bottom, d_left = d_left, d_top, d_right, d_bottom

    return np.concatenate([
        patch_3x3.flatten(),
        [d_top, d_bottom, d_left, d_right],
        [grad_mag],   # rotation-invariant
        [variance],   # rotation-invariant
    ])


def augment_frac(fx, fy, aug_idx):
    """Apply the same augmentation to fractional sub-pixel coordinates."""
    flip_h = aug_idx >= 4
    rot = aug_idx % 4

    if flip_h:
        fx = 1.0 - fx

    for _ in range(rot):
        fx, fy = 1.0 - fy, fx

    return fx, fy


def make_dataset(ref_paths, augment=True):
    """Build training dataset with optional 8-fold augmentation."""
    all_x, all_y = [], []

    for path in ref_paths:
        hr = Image.open(path).convert('RGB')
        w, h = hr.size
        lr_w, lr_h = w // SCALE, h // SCALE

        # Create degraded low-res
        lr = hr.resize((lr_w, lr_h), Image.LANCZOS)
        lr = jpeg_degrade(lr)

        # Bilinear upscale (baseline)
        bic = lr.resize((w, h), Image.BILINEAR)

        hr_np = np.array(hr, dtype=np.float32) / 255.0
        lr_np = np.array(lr, dtype=np.float32) / 255.0
        bic_np = np.array(bic, dtype=np.float32) / 255.0

        hr_l = luma(hr_np)
        lr_l = luma(lr_np)
        bic_l = luma(bic_np)

        # Pad LR for feature extraction (need ±2 for dilated sampling)
        lr_pad = np.pad(lr_l, 2, mode='edge')

        # Random subsample
        total_pixels = lr_h * lr_w * SCALE * SCALE
        n_samples = min(MAX_SAMPLES_PER_IMAGE, total_pixels)
        indices = np.random.choice(total_pixels, n_samples, replace=False)

        for idx in indices:
            pixel_idx = idx // (SCALE * SCALE)
            sub_idx = idx % (SCALE * SCALE)
            iy = pixel_idx // lr_w
            ix = pixel_idx % lr_w
            sy = sub_idx // SCALE
            sx = sub_idx % SCALE

            oy = iy * SCALE + sy
            ox = ix * SCALE + sx
            if oy >= hr_l.shape[0] or ox >= hr_l.shape[1]:
                continue

            fy = (sy + 0.5) / SCALE
            fx = (sx + 0.5) / SCALE
            target = hr_l[oy, ox] - bic_l[oy, ox]

            # Compute 15 spatial features
            base_features = compute_features(lr_pad, iy, ix)

            if augment:
                # Apply all 8 augmentations
                for aug_idx in range(8):
                    aug_feat = augment_patch(base_features, aug_idx)
                    aug_fx, aug_fy = augment_frac(fx, fy, aug_idx)
                    inp = np.concatenate([aug_feat, [aug_fx, aug_fy]])
                    all_x.append(inp)
                    all_y.append(target)
            else:
                inp = np.concatenate([base_features, [fx, fy]])
                all_x.append(inp)
                all_y.append(target)

    X = torch.tensor(np.array(all_x, dtype=np.float32))
    Y = torch.tensor(np.array(all_y, dtype=np.float32)).unsqueeze(1)
    return X, Y


def charbonnier_loss(pred, target, eps=CHARBONNIER_EPS):
    """Charbonnier loss: smooth L1, less sensitive to outliers than MSE."""
    return torch.mean(torch.sqrt((pred - target) ** 2 + eps * eps))


def gradient_loss(pred, target):
    """
    MSE on horizontal + vertical gradients.
    pred/target shape: (batch, 1) — we can't compute spatial gradients on individual samples.

    Instead, we use a simpler proxy: penalize large deviations more at higher residual magnitudes.
    For proper gradient loss, we'd need 2D patches. Here we use edge-weighted MSE.
    """
    # Edge-weighted: scale MSE by target magnitude (larger residuals = edges = more important)
    weights = 1.0 + 5.0 * target.abs()  # edges have larger residuals
    return torch.mean(weights * (pred - target) ** 2)


def combined_loss(pred, target):
    """Charbonnier + edge-weighted gradient loss."""
    l_charb = charbonnier_loss(pred, target)
    l_grad = gradient_loss(pred, target)
    return LOSS_ALPHA * l_charb + LOSS_BETA * l_grad


def train_model(model, X_train, Y_train, X_val, Y_val):
    train_ds = TensorDataset(X_train, Y_train)
    val_ds = TensorDataset(X_val, Y_val)
    train_dl = DataLoader(train_ds, batch_size=BATCH, shuffle=True, num_workers=0)
    val_dl = DataLoader(val_ds, batch_size=BATCH)

    opt = optim.Adam(model.parameters(), lr=LR, betas=(0.9, 0.999))

    # Cosine annealing with warm restarts: T_0=80, T_mult=2 -> restarts at 80, 240, ...
    T_0 = EPOCHS // 5
    sched = optim.lr_scheduler.CosineAnnealingWarmRestarts(opt, T_0=T_0, T_mult=2)

    best_val = float('inf')
    best_state = None
    global_step = 0

    # SWA state
    swa_model = None
    swa_n = 0
    swa_start_epoch = int(EPOCHS * SWA_START_FRAC)

    start_time = time.time()

    for ep in range(EPOCHS):
        model.train()
        t_loss = 0
        n = 0

        for x, y in train_dl:
            # Learning rate warmup
            if global_step < WARMUP_STEPS:
                warmup_lr = LR * (global_step + 1) / WARMUP_STEPS
                for pg in opt.param_groups:
                    pg['lr'] = warmup_lr

            pred = model(x)
            loss = combined_loss(pred, y)

            opt.zero_grad()
            loss.backward()

            # Gradient clipping
            torch.nn.utils.clip_grad_norm_(model.parameters(), GRAD_CLIP)

            opt.step()
            t_loss += loss.item() * x.size(0)
            n += x.size(0)
            global_step += 1

        if global_step >= WARMUP_STEPS:
            sched.step()

        t_loss /= n

        # Validation
        model.eval()
        v_loss = 0
        n = 0
        with torch.no_grad():
            for x, y in val_dl:
                v_loss += combined_loss(model(x), y).item() * x.size(0)
                n += x.size(0)
        v_loss /= n

        if v_loss < best_val:
            best_val = v_loss
            best_state = {k: v.clone() for k, v in model.state_dict().items()}

        # Stochastic Weight Averaging
        if ep >= swa_start_epoch:
            if swa_model is None:
                swa_model = {k: v.clone() for k, v in model.state_dict().items()}
                swa_n = 1
            else:
                for k in swa_model:
                    swa_model[k] = (swa_model[k] * swa_n + model.state_dict()[k]) / (swa_n + 1)
                swa_n += 1

        if (ep + 1) % 40 == 0 or ep == 0:
            elapsed = time.time() - start_time
            with torch.no_grad():
                bic_mse = (Y_val ** 2).mean().item()
                nn_mse = ((model(X_val) - Y_val) ** 2).mean().item()
                bic_psnr = 10 * np.log10(1.0 / max(bic_mse, 1e-10))
                nn_psnr = 10 * np.log10(1.0 / max(nn_mse, 1e-10))
            lr_now = opt.param_groups[0]['lr']
            print(f"  Ep {ep+1:3d}/{EPOCHS}  loss={v_loss:.7f}  "
                  f"bicPSNR={bic_psnr:.1f}dB  nnPSNR={nn_psnr:.1f}dB  "
                  f"gain={nn_psnr-bic_psnr:+.2f}dB  lr={lr_now:.2e}  "
                  f"[{elapsed:.0f}s]")

    # Use SWA model if available, otherwise best checkpoint
    if swa_model is not None:
        print(f"\n  Using SWA model (averaged over {swa_n} checkpoints)")
        model.load_state_dict(swa_model)
    else:
        model.load_state_dict(best_state)

    return model


def export_glsl(model, path):
    """Export weights as GLSL const arrays for the v2 shader."""
    w1 = model.fc1.weight.detach().numpy()   # (32, 17)
    b1 = model.fc1.bias.detach().numpy()     # (32,)
    prelu_a = model.prelu.weight.detach().numpy()  # (32,)
    w2 = model.fc2.weight.detach().numpy()   # (1, 32)
    b2 = model.fc2.bias.detach().numpy()     # (1,)

    n_params = model.param_count()
    lines = []
    lines.append(f"// NeuralUp-v2: {N_INPUTS} -> {HIDDEN} (PReLU) -> 1, {n_params} params")
    lines.append(f"// Loss: Charbonnier + edge-weighted gradient")
    lines.append(f"// Training: 8x augmentation, SGDR, SWA, LR warmup")
    lines.append(f"const int N_INPUTS = {N_INPUTS};")
    lines.append(f"const int HIDDEN = {HIDDEN};")
    lines.append("")

    # W1 as float[HIDDEN][N_INPUTS]
    lines.append(f"const float W1[{HIDDEN}][{N_INPUTS}] = float[{HIDDEN}][{N_INPUTS}](")
    for i in range(HIDDEN):
        vals = ", ".join(f"{v:.8f}" for v in w1[i])
        comma = "," if i < HIDDEN - 1 else ""
        lines.append(f"    float[{N_INPUTS}]({vals}){comma}")
    lines.append(");")
    lines.append("")

    vals = ", ".join(f"{v:.8f}" for v in b1)
    lines.append(f"const float B1[{HIDDEN}] = float[{HIDDEN}]({vals});")
    lines.append("")

    # PReLU slopes
    vals = ", ".join(f"{v:.8f}" for v in prelu_a)
    lines.append(f"const float PRELU_A[{HIDDEN}] = float[{HIDDEN}]({vals});")
    lines.append("")

    vals = ", ".join(f"{v:.8f}" for v in w2[0])
    lines.append(f"const float W2[{HIDDEN}] = float[{HIDDEN}]({vals});")
    lines.append("")

    lines.append(f"const float B2 = {b2[0]:.8f};")

    with open(path, 'w') as f:
        f.write('\n'.join(lines))
    print(f"  GLSL weights -> {path}")


def main():
    # Find project root
    d = os.path.dirname(os.path.abspath(__file__))
    while d != '/':
        if os.path.exists(os.path.join(d, 'settings.gradle.kts')):
            break
        d = os.path.dirname(d)

    ref_dir = os.path.join(d, 'imgs', 'reference')
    out_dir = os.path.join(d, 'tools', 'train-tiny-sr')

    ref_paths = sorted(glob.glob(os.path.join(ref_dir, '*.png')) +
                       glob.glob(os.path.join(ref_dir, '*.jpg')))
    print(f"Reference images: {len(ref_paths)}")
    if not ref_paths:
        print(f"No images in {ref_dir}")
        return

    n_val = max(1, len(ref_paths) // 5)
    val_paths = ref_paths[:n_val]
    train_paths = ref_paths[n_val:] if len(ref_paths) > n_val else ref_paths

    model = NeuralUpV2()
    print(f"Architecture: {N_INPUTS} -> {HIDDEN} (PReLU) -> 1  ({model.param_count()} params)")
    print(f"Train images: {len(train_paths)}, Val images: {len(val_paths)}")
    print(f"Scale: {SCALE}x, JPEG quality: {JPEG_Q}")
    print(f"Loss: {LOSS_ALPHA}*Charbonnier + {LOSS_BETA}*EdgeWeightedMSE")
    print(f"Data augmentation: 8-fold (4 rotations x 2 flips)")
    print(f"Optimizer: Adam, LR={LR}, warmup={WARMUP_STEPS} steps, SGDR T_0={EPOCHS//5}")
    print(f"SWA starts at epoch {int(EPOCHS * SWA_START_FRAC)}")
    print()

    print("Building training dataset (with 8x augmentation)...")
    X_train, Y_train = make_dataset(train_paths, augment=True)
    print(f"  Train samples: {len(X_train)}")

    print("Building validation dataset (no augmentation)...")
    X_val, Y_val = make_dataset(val_paths, augment=False)
    print(f"  Val samples: {len(X_val)}")
    print()

    print("Training...")
    model = train_model(model, X_train, Y_train, X_val, Y_val)
    print()

    # Fold residual scale into weights for deployment
    print("Folding residual scale into fc2 weights...")
    model.fold_residual_scale()

    # Final evaluation
    model.eval()
    with torch.no_grad():
        bic_mse = (Y_val ** 2).mean().item()
        nn_pred = model(X_val)
        nn_mse = ((nn_pred - Y_val) ** 2).mean().item()
        bic_psnr = 10 * np.log10(1.0 / max(bic_mse, 1e-10))
        nn_psnr = 10 * np.log10(1.0 / max(nn_mse, 1e-10))

    print(f"Final results:")
    print(f"  Bilinear residual PSNR: {bic_psnr:.2f} dB")
    print(f"  NeuralUp-v2 residual PSNR: {nn_psnr:.2f} dB")
    print(f"  Improvement over bilinear: {nn_psnr - bic_psnr:+.2f} dB")
    print()

    # Export
    export_glsl(model, os.path.join(out_dir, 'weights_v2.glsl'))
    torch.save(model.state_dict(), os.path.join(out_dir, 'model_v2.pth'))
    print(f"  Model checkpoint -> {os.path.join(out_dir, 'model_v2.pth')}")
    print(f"\nDone! Copy weights from weights_v2.glsl into TinyNNUpscaleStrategy.kt")
    print(f"and update the GLSL shader to the v2 architecture.")


if __name__ == '__main__':
    main()
