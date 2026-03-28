# FSR Upscaling & Frame Generation for PS3 Remote Play Stream — Full Research

## The Problem
PS3 Remote Play outputs at **480x272** (PSP res) up to **852x480**. Modern phones have 1080p-2K+ screens. The stream also maxes at 30fps. We need upscaling and potentially frame interpolation.

---

## 1. FSR 1 (Spatial Upscaling) — BEST OPTION FOR VIDEO

### What It Is
Single-pass spatial upscaling: EASU (Edge-Adaptive Spatial Upsampling) + RCAS (Robust Contrast-Adaptive Sharpening). Works on individual frames — no motion vectors or frame history needed.

### Why It Works for Video Streams
- Only needs the current decoded frame (video frames are just 2D images)
- Already proven for video: AMD brought FSR upscaling to YouTube and VLC (Q1 2024)
- Ported to mpv as GLSL shaders for video playback
- MIT licensed, fully open source

### Mobile-Optimized Implementation EXISTS
An optimized FSR 1 for mobile (GLSL/OpenGL ES 3) combines EASU+RCAS into a single pass, uses half-precision math:
- **Source:** https://gist.github.com/atyuwen/78d6e810e6d0f7fd4aa6207d416f2eeb (MIT)

### FSR 1 for mpv (video-specific port)
- Works on luma plane, supports up to 4x upscaling
- **Source:** https://gist.github.com/agyild/82219c545228d70c5604f865ce0b0ce5

### Caveat: Codec Artifacts
AMD requires input to be "antialiased and noise-free." Lossy H.264 at PSP resolution WILL have compression artifacts. FSR's RCAS sharpening **amplifies these artifacts**. Solution: apply a denoising/deblocking pass BEFORE FSR.
- Source: ALVR Issue #780 — https://github.com/alvr-org/ALVR/issues/780

### 480x272 → 1080p Feasibility
- 4x linear upscale — right at FSR 1's upper designed limit
- Modern Android GPUs (Adreno 600+, Mali G-series) can run this in **under 2ms per frame**
- Input resolution is small (130K pixels), texture reads are cheap
- Recommended: two-step upscale (480x272 → 960x544 → 1920x1080) for better quality

---

## 2. Alternative Spatial Upscalers for Android

### Snapdragon Game Super Resolution (SGSR)
- **SGSR 1:** Single-pass Lanczos-like 12-tap filter with adaptive sharpening (similar to FSR 1)
- Written in GLSL, optimized for Adreno GPUs
- BSD 3-Clause license
- **Source:** https://github.com/SnapdragonGameStudios/snapdragon-gsr
- SGSR 1 could theoretically process video frames like FSR 1

### Arm ASR (Accuracy Super Resolution)
- Derived directly from AMD FSR 2.2, optimized for Mali/Immortalis GPUs
- 50% reduced GPU load vs desktop FSR 2
- MIT license
- **Source:** https://github.com/arm/accuracy-super-resolution
- NOTE: Requires motion vectors (FSR 2-based), so NOT directly usable for video

### Anime4K
- Real-time upscaling shaders, 3ms for 1080p→2160p on desktop
- Optimized for stylized content; poor PSNR on natural video
- **Source:** https://github.com/bloc97/Anime4K

### libplacebo
- Video processing library used by mpv and VLC
- Supports Vulkan (including Android), OpenGL
- High-quality Jinc/Lanczos upscaling
- Can load custom FSR shaders
- LGPLv2.1+
- **Source:** https://github.com/haasn/libplacebo

---

## 3. FSR 2+ and FSR 3 — NOT Usable for Video

### FSR 2.x (Temporal Upscaling)
- Requires per-pixel motion vectors + depth buffers from game engine
- Video streams don't have these → **NOT applicable**

### FSR 3.0 (Frame Generation)
- Open-sourced under MIT (FSR 3.0.3)
- Requires DX12 only (no Vulkan, no OpenGL ES)
- Needs game-engine motion vectors
- **NOT directly applicable to video streams**

### FSR 4 (ML-based)
- Launched March 2025, not yet open source

---

## 4. Frame Generation / Interpolation for Video

### SVPlayer (SmoothVideo Project) — PROVEN ON ANDROID
- Available on Google Play
- Real-time MEMC (Motion Estimation, Motion Compensation)
- Targets 48/60/120 fps from any source
- **Requires minimum Snapdragon 865** for 1080p
- $12 paid unlock for frame interpolation
- Heavy battery drain
- **Source:** https://www.svp-team.com/ / Google Play

### Mob-FGSR (SIGGRAPH 2024)
- Lightweight frame generation + super resolution for mobile
- No neural networks — uses motion splatting
- Tested on Snapdragon 8 Gen 3: ~22 FPS → ~50 FPS with frame gen
- Deployed in OnePlus Ace 3 Pro (Genshin Impact at 120 FPS)
- **Requires game-engine motion vectors** → NOT directly for video
- **Source:** https://mob-fgsr.github.io/ / https://github.com/Mob-FGSR/MobFGSR

### Hardware MEMC Chips
Some phones have **Pixelworks X5** dedicated motion interpolation hardware:
- OnePlus 8/9 Pro, OPPO Find X2 Pro, Xiaomi Mi 11X
- Works at driver/display level on any video content

### RIFE (Real-Time Intermediate Flow Estimation)
- Neural network-based frame interpolation
- Very high quality but too heavy for mobile real-time
- Available via SVP desktop offloading
- **Source:** https://github.com/megvii-research/ECCV2022-RIFE

---

## 5. ML-Based Real-Time Upscaling on Android — NOT Feasible Yet

From ALVR experiments:
- TensorFlow Super Resolution: "Even upscaling tiny 50x50 images took around 500ms" on mobile
- SubPixel-BackProjection: 3 seconds for 720p→1080p even with CUDA
- Mobile chips have ~10x performance disadvantage vs desktop GPUs
- **Source:** https://github.com/alvr-org/ALVR/wiki/Real-time-video-upscaling-experiments

### WebSR (potential future option)
- WebGPU-based real-time AI upscaling using retrained Anime4K CNNs
- Designed for "production video and WebRTC applications"
- **Source:** https://github.com/sb2702/websr

---

## 6. Recommended Pipeline for Our App

```
PS3 Stream (480x272 or 852x480, up to 30fps, H.264/MPEG4)
  │
  ├─ Step 1: MediaCodec HW decode → GPU SurfaceTexture (zero-copy)
  │
  ├─ Step 2: [Optional] Deblocking/denoising pass (bilateral filter on chroma)
  │           Purpose: Clean codec artifacts BEFORE upscaling
  │
  ├─ Step 3: FSR 1 EASU upscale (480x272 → 960x544 → 1920x1080)
  │           Use: Mobile-optimized single-pass shader
  │           Time: ~1-2ms on modern Android GPU
  │
  ├─ Step 4: FSR 1 RCAS sharpen (conservative settings to avoid artifact amplification)
  │           Time: <1ms
  │
  ├─ Step 5: [Optional] Frame interpolation (30fps → 60fps)
  │           Trade-off: +16ms latency for smoother video
  │           Options: Custom optical flow shader or SVPlayer engine
  │
  └─ Step 6: Render to display SurfaceView
```

### Key Trade-offs

| Feature | Benefit | Cost |
|---------|---------|------|
| FSR 1 upscaling | Crisp image at device resolution | ~2ms GPU per frame, minimal |
| Deblocking pre-pass | Cleaner upscale, fewer artifacts | ~1ms GPU per frame |
| Frame interpolation | Smoother 60fps video | +16ms latency, heavy battery drain |
| Two-step upscale | Better quality than single 4x jump | ~1ms extra GPU per frame |

### Latency Budget (target: <100ms total)
- Network: ~5-20ms (LAN)
- Decode: ~5-10ms (HW MediaCodec)
- Upscale: ~2-3ms (FSR 1 + deblock)
- Frame interpolation: ~16ms (if enabled)
- Display: ~8-16ms (vsync)
- **Total without interpolation: ~20-49ms** (excellent for remote play)
- **Total with interpolation: ~36-65ms** (acceptable, user-configurable)

---

## 7. All Relevant Repos & Resources

### Must-Download
| Resource | URL | License | Use Case |
|----------|-----|---------|----------|
| FSR 1 Mobile Shader | https://gist.github.com/atyuwen/78d6e810e6d0f7fd4aa6207d416f2eeb | MIT | Direct GLSL for Android |
| FSR 1 for mpv | https://gist.github.com/agyild/82219c545228d70c5604f865ce0b0ce5 | MIT | Video-specific FSR |
| FidelityFX FSR 1 | https://github.com/GPUOpen-Effects/FidelityFX-FSR | MIT | Reference EASU+RCAS shaders |
| FidelityFX SDK | https://github.com/GPUOpen-LibrariesAndSDKs/FidelityFX-SDK | MIT | Full SDK (FSR 1/2/3/4) |
| Snapdragon GSR | https://github.com/SnapdragonGameStudios/snapdragon-gsr | BSD-3 | Adreno-optimized GLSL |
| Arm ASR | https://github.com/arm/accuracy-super-resolution | MIT | Mali-optimized upscaler |
| libplacebo | https://github.com/haasn/libplacebo | LGPLv2.1 | Video upscaling library |
| RIFE | https://github.com/megvii-research/ECCV2022-RIFE | Apache-2.0 | Frame interpolation reference |
| Mob-FGSR | https://github.com/Mob-FGSR/MobFGSR | — | Mobile frame gen research |
| WebSR | https://github.com/sb2702/websr | MIT | ML upscaling for video |
| Anime4K | https://github.com/bloc97/Anime4K | MIT | Real-time upscaling shaders |
| FSR 2 OpenGL Port | https://github.com/JuanDiegoMontoya/FidelityFX-FSR2-OpenGL | MIT | OpenGL FSR 2 reference |

### Reference Reading
- ALVR Upscaling Experiments: https://github.com/alvr-org/ALVR/wiki/Real-time-video-upscaling-experiments
- ALVR FSR Discussion: https://github.com/alvr-org/ALVR/issues/780
- FSR 1 Demystified: https://jntesteves.github.io/shadesofnoice/graphics/shaders/upscaling/2021/09/11/amd-fsr-demystified.html
- AMD FSR for YouTube/VLC: https://videocardz.com/newz/amd-is-bringing-fidelityfx-super-resolution-upscaling-to-youtube-and-vlc-videos
- SVP Android FAQ: https://www.svp-team.com/wiki/FAQ_(Android)
