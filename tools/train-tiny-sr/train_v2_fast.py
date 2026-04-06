#!/usr/bin/env python3
"""Quick v2 training — subsampled data, 100 epochs, ~2 minutes."""

import os, sys, io, glob
import numpy as np
import torch
import torch.nn as nn
import torch.optim as optim
from torch.utils.data import DataLoader, TensorDataset
from PIL import Image

SCALE = 3; HIDDEN = 32; EPOCHS = 100; BATCH = 4096; LR = 3e-3; JPEG_Q = 75
MAX_SAMPLES = 50000  # per image

class TinySRv2(nn.Module):
    def __init__(self):
        super().__init__()
        self.fc1 = nn.Linear(17, HIDDEN)
        self.prelu = nn.PReLU(HIDDEN)
        self.fc2 = nn.Linear(HIDDEN, 1)
        nn.init.kaiming_normal_(self.fc1.weight, nonlinearity='leaky_relu')
        nn.init.zeros_(self.fc1.bias)
        nn.init.normal_(self.fc2.weight, std=0.01)
        nn.init.zeros_(self.fc2.bias)

    def forward(self, x):
        return self.fc2(self.prelu(self.fc1(x)))

def luma(rgb): return 0.2126*rgb[...,0] + 0.7152*rgb[...,1] + 0.0722*rgb[...,2]

def charbonnier(pred, target, eps=1e-6):
    return torch.sqrt((pred - target)**2 + eps**2).mean()

def make_dataset(ref_paths, augment=False):
    all_x, all_y = [], []
    for path in ref_paths:
        hr = Image.open(path).convert('RGB')
        w, h = hr.size; lr_w, lr_h = w//SCALE, h//SCALE
        lr = hr.resize((lr_w, lr_h), Image.LANCZOS)
        buf = io.BytesIO(); lr.save(buf, format='JPEG', quality=JPEG_Q); buf.seek(0)
        lr = Image.open(buf).convert('RGB')
        bic = lr.resize((w, h), Image.BILINEAR)

        hr_l = luma(np.array(hr, np.float32)/255.0)
        lr_l = luma(np.array(lr, np.float32)/255.0)
        bic_l = luma(np.array(bic, np.float32)/255.0)
        lr_pad = np.pad(lr_l, 2, mode='edge')  # pad 2 for dilated samples

        # Simple subsampling (augmentation handled by random sampling)
        total = lr_h * lr_w * SCALE * SCALE
        n = min(MAX_SAMPLES, total)
        indices = np.random.choice(total, n, replace=False)

        for idx in indices:
                pix = idx // (SCALE*SCALE); sub = idx % (SCALE*SCALE)
                iy = pix // lr_w; ix = pix % lr_w
                sy = sub // SCALE; sx = sub % SCALE
                oy = iy*SCALE+sy; ox = ix*SCALE+sx
                if oy >= hr_l.shape[0] or ox >= hr_l.shape[1]: continue

                # 3x3 core (offset +2 for padding)
                cy, cx = iy+2, ix+2
                patch = lr_pad[cy-1:cy+2, cx-1:cx+2].flatten()  # 9 values
                # Dilated cross at ±2
                d_up = lr_pad[cy-2, cx]; d_down = lr_pad[cy+2, cx]
                d_left = lr_pad[cy, cx-2]; d_right = lr_pad[cy, cx+2]
                # Gradient magnitude (Sobel on 3x3)
                gx = -lr_pad[cy-1,cx-1] + lr_pad[cy-1,cx+1] - 2*lr_pad[cy,cx-1] + 2*lr_pad[cy,cx+1] - lr_pad[cy+1,cx-1] + lr_pad[cy+1,cx+1]
                gy = -lr_pad[cy-1,cx-1] - 2*lr_pad[cy-1,cx] - lr_pad[cy-1,cx+1] + lr_pad[cy+1,cx-1] + 2*lr_pad[cy+1,cx] + lr_pad[cy+1,cx+1]
                grad_mag = np.sqrt(gx*gx + gy*gy)
                # Local variance
                mean = patch.mean()
                variance = ((patch - mean)**2).mean()

                fy = (sy+0.5)/SCALE; fx = (sx+0.5)/SCALE
                target = hr_l[oy, ox] - bic_l[oy, ox]
                inp = np.array([*patch, d_up, d_down, d_left, d_right, grad_mag, variance, fx, fy], np.float32)
                all_x.append(inp); all_y.append(target)

    return torch.tensor(np.array(all_x, np.float32)), torch.tensor(np.array(all_y, np.float32)).unsqueeze(1)

def export_glsl(model, path):
    w1 = model.fc1.weight.detach().numpy()
    b1 = model.fc1.bias.detach().numpy()
    pa = model.prelu.weight.detach().numpy()
    w2 = model.fc2.weight.detach().numpy()
    b2 = model.fc2.bias.detach().numpy()
    lines = [f"// TinySR v2: 17->{HIDDEN}(PReLU)->1, {sum(p.numel() for p in model.parameters())} params",
             f"const int N_INPUTS = 17;", f"const int HIDDEN = {HIDDEN};", ""]
    lines.append(f"const float W1[{HIDDEN}][17] = float[{HIDDEN}][17](")
    for i in range(HIDDEN):
        vals = ", ".join(f"{v:.8f}" for v in w1[i])
        lines.append(f"    float[17]({vals}){',' if i<HIDDEN-1 else ''}")
    lines.append(");"); lines.append("")
    lines.append(f"const float B1[{HIDDEN}] = float[{HIDDEN}]({', '.join(f'{v:.8f}' for v in b1)});"); lines.append("")
    lines.append(f"const float PRELU_A[{HIDDEN}] = float[{HIDDEN}]({', '.join(f'{v:.8f}' for v in pa)});"); lines.append("")
    lines.append(f"const float W2[{HIDDEN}] = float[{HIDDEN}]({', '.join(f'{v:.8f}' for v in w2[0])});"); lines.append("")
    lines.append(f"const float B2 = {b2[0]:.8f};")
    with open(path, 'w') as f: f.write('\n'.join(lines))

def main():
    d = os.path.dirname(os.path.abspath(__file__))
    while d != '/' and not os.path.exists(os.path.join(d, 'settings.gradle.kts')): d = os.path.dirname(d)
    ref_dir = os.path.join(d, 'imgs', 'reference')
    out_dir = os.path.join(d, 'tools', 'train-tiny-sr')

    ref_paths = sorted(glob.glob(os.path.join(ref_dir, '*.png')) + glob.glob(os.path.join(ref_dir, '*.jpg')))
    print(f"Images: {len(ref_paths)}, Arch: 17->{HIDDEN}(PReLU)->1 ({17*HIDDEN+HIDDEN+HIDDEN+HIDDEN+1} params)")

    n_val = max(1, len(ref_paths)//5)
    val_paths, train_paths = ref_paths[:n_val], ref_paths[n_val:] or ref_paths

    print("Building datasets...")
    X_train, Y_train = make_dataset(train_paths, augment=True)
    X_val, Y_val = make_dataset(val_paths, augment=False)
    print(f"  Train: {len(X_train)}, Val: {len(X_val)}")

    model = TinySRv2()
    opt = optim.Adam(model.parameters(), lr=LR)
    sched = optim.lr_scheduler.CosineAnnealingLR(opt, T_max=EPOCHS)
    train_dl = DataLoader(TensorDataset(X_train, Y_train), batch_size=BATCH, shuffle=True)

    best_val = float('inf'); best_state = None
    print("\nTraining...")
    for ep in range(EPOCHS):
        model.train()
        for x, y in train_dl:
            loss = charbonnier(model(x), y); opt.zero_grad(); loss.backward(); opt.step()
        model.eval()
        with torch.no_grad():
            val_loss = charbonnier(model(X_val), Y_val).item()
        sched.step()
        if val_loss < best_val: best_val = val_loss; best_state = {k:v.clone() for k,v in model.state_dict().items()}
        if (ep+1) % 20 == 0 or ep == 0:
            with torch.no_grad():
                bm = (Y_val**2).mean().item(); nm = ((model(X_val)-Y_val)**2).mean().item()
                bp = 10*np.log10(1/max(bm,1e-10)); np_ = 10*np.log10(1/max(nm,1e-10))
            print(f"  Ep {ep+1:3d}/{EPOCHS}  charb={val_loss:.6f}  bicPSNR={bp:.1f}  nnPSNR={np_:.1f}  gain={np_-bp:+.2f}dB")

    model.load_state_dict(best_state)
    model.eval()
    with torch.no_grad():
        bm = (Y_val**2).mean().item(); nm = ((model(X_val)-Y_val)**2).mean().item()
        bp = 10*np.log10(1/max(bm,1e-10)); np_ = 10*np.log10(1/max(nm,1e-10))
    print(f"\nFinal: bicPSNR={bp:.2f}dB  nnPSNR={np_:.2f}dB  gain={np_-bp:+.2f}dB")

    export_glsl(model, os.path.join(out_dir, 'weights_v2_fast.glsl'))
    torch.save(model.state_dict(), os.path.join(out_dir, 'model_v2_fast.pth'))
    print("Done!")

if __name__ == '__main__':
    main()
