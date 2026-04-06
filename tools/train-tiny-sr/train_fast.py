#!/usr/bin/env python3
"""Fast training: subsample patches, fewer epochs. Runs in ~30-60 seconds."""

import os, sys, io, glob
import numpy as np
import torch
import torch.nn as nn
import torch.optim as optim
from torch.utils.data import DataLoader, TensorDataset
from PIL import Image

SCALE = 3; HIDDEN = 16; EPOCHS = 100; BATCH = 4096; LR = 3e-3; JPEG_Q = 75
MAX_SAMPLES_PER_IMAGE = 50000  # subsample instead of using ALL pixels

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

def luma(rgb): return 0.2126*rgb[...,0] + 0.7152*rgb[...,1] + 0.0722*rgb[...,2]

def make_dataset(ref_paths):
    all_x, all_y = [], []
    for path in ref_paths:
        hr = Image.open(path).convert('RGB')
        w, h = hr.size
        lr_w, lr_h = w // SCALE, h // SCALE
        lr = hr.resize((lr_w, lr_h), Image.LANCZOS)
        # JPEG degrade
        buf = io.BytesIO(); lr.save(buf, format='JPEG', quality=JPEG_Q); buf.seek(0)
        lr = Image.open(buf).convert('RGB')
        bic = lr.resize((w, h), Image.BILINEAR)

        hr_np = np.array(hr, np.float32) / 255.0
        lr_np = np.array(lr, np.float32) / 255.0
        bic_np = np.array(bic, np.float32) / 255.0
        hr_l = luma(hr_np); lr_l = luma(lr_np); bic_l = luma(bic_np)
        lr_pad = np.pad(lr_l, 1, mode='edge')

        # Random subsample of input pixels
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

            oy = iy * SCALE + sy; ox = ix * SCALE + sx
            if oy >= hr_l.shape[0] or ox >= hr_l.shape[1]: continue

            patch = lr_pad[iy:iy+3, ix:ix+3].flatten()
            fy = (sy + 0.5) / SCALE; fx = (sx + 0.5) / SCALE
            target = hr_l[oy, ox] - bic_l[oy, ox]
            all_x.append(np.concatenate([patch, [fx, fy]]))
            all_y.append(target)

    X = torch.tensor(np.array(all_x, np.float32))
    Y = torch.tensor(np.array(all_y, np.float32)).unsqueeze(1)
    return X, Y

def export_glsl(model, path):
    w1 = model.fc1.weight.detach().numpy()
    b1 = model.fc1.bias.detach().numpy()
    w2 = model.fc2.weight.detach().numpy()
    b2 = model.fc2.bias.detach().numpy()
    lines = [f"// TinySR: 11 -> {HIDDEN} (ReLU) -> 1, {sum(p.numel() for p in model.parameters())} params",
             f"const int HIDDEN = {HIDDEN};", ""]
    lines.append(f"const float W1[{HIDDEN}][11] = float[{HIDDEN}][11](")
    for i in range(HIDDEN):
        vals = ", ".join(f"{v:.8f}" for v in w1[i])
        lines.append(f"    float[11]({vals}){',' if i < HIDDEN-1 else ''}")
    lines.append(");"); lines.append("")
    lines.append(f"const float B1[{HIDDEN}] = float[{HIDDEN}]({', '.join(f'{v:.8f}' for v in b1)});"); lines.append("")
    lines.append(f"const float W2[{HIDDEN}] = float[{HIDDEN}]({', '.join(f'{v:.8f}' for v in w2[0])});"); lines.append("")
    lines.append(f"const float B2 = {b2[0]:.8f};")
    with open(path, 'w') as f: f.write('\n'.join(lines))

def main():
    d = os.path.dirname(os.path.abspath(__file__))
    while d != '/' and not os.path.exists(os.path.join(d, 'settings.gradle.kts')): d = os.path.dirname(d)
    ref_dir = os.path.join(d, 'imgs', 'reference')
    out_dir = os.path.join(d, 'tools', 'train-tiny-sr')

    ref_paths = sorted(glob.glob(os.path.join(ref_dir, '*.png')) + glob.glob(os.path.join(ref_dir, '*.jpg')))
    print(f"Images: {len(ref_paths)}, Architecture: 11->{HIDDEN}->1 (209 params)")

    n_val = max(1, len(ref_paths) // 5)
    val_paths, train_paths = ref_paths[:n_val], ref_paths[n_val:] or ref_paths

    print("Building datasets (subsampled)...")
    X_train, Y_train = make_dataset(train_paths)
    X_val, Y_val = make_dataset(val_paths)
    print(f"  Train: {len(X_train)}, Val: {len(X_val)}")

    model = TinySR()
    opt = optim.Adam(model.parameters(), lr=LR)
    sched = optim.lr_scheduler.CosineAnnealingLR(opt, T_max=EPOCHS)
    loss_fn = nn.MSELoss()
    train_dl = DataLoader(TensorDataset(X_train, Y_train), batch_size=BATCH, shuffle=True)
    val_dl = DataLoader(TensorDataset(X_val, Y_val), batch_size=BATCH)

    best_val = float('inf'); best_state = None
    print("\nTraining...")
    for ep in range(EPOCHS):
        model.train()
        for x, y in train_dl:
            loss = loss_fn(model(x), y); opt.zero_grad(); loss.backward(); opt.step()
        model.eval()
        v = sum(loss_fn(model(x), y).item() * x.size(0) for x, y in val_dl) / len(X_val)
        sched.step()
        if v < best_val: best_val = v; best_state = {k: v.clone() for k, v in model.state_dict().items()}
        if (ep+1) % 20 == 0 or ep == 0:
            with torch.no_grad():
                bm = (Y_val**2).mean().item()
                nm = ((model(X_val)-Y_val)**2).mean().item()
                bp = 10*np.log10(1/max(bm,1e-10)); np_ = 10*np.log10(1/max(nm,1e-10))
            print(f"  Ep {ep+1:3d}/{EPOCHS}  val={v:.8f}  bicPSNR={bp:.1f}  nnPSNR={np_:.1f}  gain={np_-bp:+.2f}dB")

    model.load_state_dict(best_state)
    model.eval()
    with torch.no_grad():
        bm = (Y_val**2).mean().item(); nm = ((model(X_val)-Y_val)**2).mean().item()
        bp = 10*np.log10(1/max(bm,1e-10)); np_ = 10*np.log10(1/max(nm,1e-10))
    print(f"\nFinal: bicPSNR={bp:.2f}dB  nnPSNR={np_:.2f}dB  gain={np_-bp:+.2f}dB")

    export_glsl(model, os.path.join(out_dir, 'weights.glsl'))
    torch.save(model.state_dict(), os.path.join(out_dir, 'model.pth'))
    print(f"Weights exported. Done!")

if __name__ == '__main__':
    main()
