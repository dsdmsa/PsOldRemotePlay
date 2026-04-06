#!/usr/bin/env python3
"""
Train a tiny neural network upscaler (209 params) for PS2 game streaming.
Learns a luma residual correction over bilinear interpolation.

Architecture: 11 -> 16 (ReLU) -> 1
  Input:  9 luma values (3x3 neighborhood) + 2 sub-pixel fractional coords
  Output: 1 luma residual (added to bilinear baseline)

Usage:
    python3 tools/train-tiny-sr/train.py

Output:
    tools/train-tiny-sr/weights.glsl  - GLSL const arrays
    tools/train-tiny-sr/weights.txt   - Raw weights for debugging
"""

import os, sys, io, glob
import numpy as np
import torch
import torch.nn as nn
import torch.optim as optim
from torch.utils.data import Dataset, DataLoader
from PIL import Image

SCALE = 3
HIDDEN = 16
EPOCHS = 200
BATCH = 8192
LR = 3e-3
JPEG_Q = 75

class TinySR(nn.Module):
    def __init__(self):
        super().__init__()
        self.fc1 = nn.Linear(11, HIDDEN)
        self.fc2 = nn.Linear(HIDDEN, 1)
        nn.init.kaiming_normal_(self.fc1.weight, nonlinearity='relu')
        nn.init.zeros_(self.fc1.bias)
        nn.init.normal_(self.fc2.weight, std=0.01)
        nn.init.zeros_(self.fc2.bias)

    def forward(self, x):
        return self.fc2(torch.relu(self.fc1(x)))

def luma(rgb):
    return 0.2126 * rgb[..., 0] + 0.7152 * rgb[..., 1] + 0.0722 * rgb[..., 2]

def jpeg_degrade(img, q=JPEG_Q):
    buf = io.BytesIO()
    img.save(buf, format='JPEG', quality=q)
    buf.seek(0)
    return Image.open(buf).convert('RGB')

def make_dataset(ref_paths):
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

        # Pad LR for 3x3 extraction
        lr_pad = np.pad(lr_l, 1, mode='edge')

        for iy in range(lr_h):
            for ix in range(lr_w):
                patch = lr_pad[iy:iy+3, ix:ix+3].flatten()  # 9 values

                # For each of the SCALE*SCALE output pixels in this input pixel's area
                for sy in range(SCALE):
                    for sx in range(SCALE):
                        oy = iy * SCALE + sy
                        ox = ix * SCALE + sx
                        if oy >= hr_l.shape[0] or ox >= hr_l.shape[1]:
                            continue

                        fy = (sy + 0.5) / SCALE
                        fx = (sx + 0.5) / SCALE

                        target = hr_l[oy, ox] - bic_l[oy, ox]

                        inp = np.concatenate([patch, [fx, fy]])
                        all_x.append(inp)
                        all_y.append(target)

    X = torch.tensor(np.array(all_x, dtype=np.float32))
    Y = torch.tensor(np.array(all_y, dtype=np.float32)).unsqueeze(1)
    return X, Y

def train_model(model, X_train, Y_train, X_val, Y_val):
    train_ds = torch.utils.data.TensorDataset(X_train, Y_train)
    val_ds = torch.utils.data.TensorDataset(X_val, Y_val)
    train_dl = DataLoader(train_ds, batch_size=BATCH, shuffle=True)
    val_dl = DataLoader(val_ds, batch_size=BATCH)

    opt = optim.Adam(model.parameters(), lr=LR)
    sched = optim.lr_scheduler.CosineAnnealingLR(opt, T_max=EPOCHS)
    loss_fn = nn.MSELoss()

    best_val = float('inf')
    best_state = None

    for ep in range(EPOCHS):
        model.train()
        t_loss = 0; n = 0
        for x, y in train_dl:
            pred = model(x)
            loss = loss_fn(pred, y)
            opt.zero_grad(); loss.backward(); opt.step()
            t_loss += loss.item() * x.size(0); n += x.size(0)
        t_loss /= n

        model.eval()
        v_loss = 0; n = 0
        with torch.no_grad():
            for x, y in val_dl:
                v_loss += loss_fn(model(x), y).item() * x.size(0); n += x.size(0)
        v_loss /= n
        sched.step()

        if v_loss < best_val:
            best_val = v_loss
            best_state = {k: v.clone() for k, v in model.state_dict().items()}

        if (ep+1) % 20 == 0 or ep == 0:
            # Compute PSNR improvement
            with torch.no_grad():
                bic_mse = (Y_val ** 2).mean().item()
                nn_mse = ((model(X_val) - Y_val) ** 2).mean().item()
                bic_psnr = 10 * np.log10(1.0 / max(bic_mse, 1e-10))
                nn_psnr = 10 * np.log10(1.0 / max(nn_mse, 1e-10))
            print(f"  Epoch {ep+1:3d}/{EPOCHS}  train={t_loss:.8f}  val={v_loss:.8f}  "
                  f"bicPSNR={bic_psnr:.1f}dB  nnPSNR={nn_psnr:.1f}dB  gain={nn_psnr-bic_psnr:+.2f}dB")

    model.load_state_dict(best_state)
    return model

def export_glsl(model, path):
    w1 = model.fc1.weight.detach().numpy()  # (16, 11)
    b1 = model.fc1.bias.detach().numpy()    # (16,)
    w2 = model.fc2.weight.detach().numpy()  # (1, 16)
    b2 = model.fc2.bias.detach().numpy()    # (1,)

    lines = []
    lines.append(f"// TinySR trained weights: 11 -> {HIDDEN} (ReLU) -> 1")
    lines.append(f"// Params: {sum(p.numel() for p in model.parameters())}")
    lines.append(f"const int HIDDEN = {HIDDEN};")
    lines.append("")

    # W1 as float[16][11]
    lines.append(f"const float W1[{HIDDEN}][11] = float[{HIDDEN}][11](")
    for i in range(HIDDEN):
        vals = ", ".join(f"{v:.6f}" for v in w1[i])
        comma = "," if i < HIDDEN - 1 else ""
        lines.append(f"    float[11]({vals}){comma}")
    lines.append(");")
    lines.append("")

    vals = ", ".join(f"{v:.6f}" for v in b1)
    lines.append(f"const float B1[{HIDDEN}] = float[{HIDDEN}]({vals});")
    lines.append("")

    vals = ", ".join(f"{v:.6f}" for v in w2[0])
    lines.append(f"const float W2[{HIDDEN}] = float[{HIDDEN}]({vals});")
    lines.append("")

    lines.append(f"const float B2 = {b2[0]:.6f};")

    with open(path, 'w') as f:
        f.write('\n'.join(lines))
    print(f"  GLSL weights → {path}")

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
        print(f"No images in {ref_dir}"); return

    # Use first 80% for train, rest for val
    n_val = max(1, len(ref_paths) // 5)
    val_paths = ref_paths[:n_val]
    train_paths = ref_paths[n_val:] if len(ref_paths) > n_val else ref_paths

    print(f"Train: {len(train_paths)} images, Val: {len(val_paths)} images")
    print(f"Architecture: 11 -> {HIDDEN} (ReLU) -> 1  ({11*HIDDEN+HIDDEN+HIDDEN+1} params)")
    print(f"Scale: {SCALE}x, JPEG quality: {JPEG_Q}")
    print()

    print("Building training dataset...")
    X_train, Y_train = make_dataset(train_paths)
    print(f"  Train samples: {len(X_train)}")

    print("Building validation dataset...")
    X_val, Y_val = make_dataset(val_paths)
    print(f"  Val samples: {len(X_val)}")
    print()

    print("Training...")
    model = TinySR()
    model = train_model(model, X_train, Y_train, X_val, Y_val)
    print()

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
    print(f"  TinyNN residual PSNR:   {nn_psnr:.2f} dB")
    print(f"  Improvement:            {nn_psnr - bic_psnr:+.2f} dB")
    print()

    # Export
    export_glsl(model, os.path.join(out_dir, 'weights.glsl'))
    torch.save(model.state_dict(), os.path.join(out_dir, 'model.pth'))
    print(f"  Model checkpoint → {os.path.join(out_dir, 'model.pth')}")
    print("\nDone! Copy weights from weights.glsl into TinyNNUpscaleStrategy.kt")

if __name__ == '__main__':
    main()
