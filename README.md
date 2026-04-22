# smeeting

On-device **streaming speech recognition** for Android using **Sherpa-NCNN** and **ncnn**, with optional **on-device summarization** of saved transcripts using a small **Qwen3** model exported for **ncnn** (same native stack as ASR: Vulkan when available, CPU fallback).

**Current app version:** `4.1.2` (`versionName` in `:app`).

---

## Overview

- **ASR**: Real-time speech-to-text runs entirely on the device (no cloud ASR). Audio is captured at 16 kHz mono, processed through a native JNI pipeline (`libncnn_asr.so`), and shown in a Jetpack Compose UI.
- **Privacy**: Microphone audio, ASR, and optional LLM inference stay on-device. Transcripts live in local history (Room). Network is used only when you **download** the optional LLM asset bundle.
- **Summaries (optional)**: From **Transcription history**, download the quantized **Qwen3 0.6B** ncnn bundle once, then generate **streaming summaries** (key points, action items, etc.). The app coordinates ASR and LLM so heavy models are not loaded together longer than necessary, which helps on low-RAM phones.

---

## Features

| Area | Description |
|------|-------------|
| Streaming ASR | Live partial and final transcripts with endpointing |
| GPU / CPU | ASR tries **Vulkan** first where supported, then CPU; encoder path mirrors Sherpa-NCNN / ncnn conventions |
| History | Saved entries, copy/delete, detail view |
| On-device LLM | Download **Qwen3 0.6B** ncnn FP16 assets; summarize history entries offline |
| Settings | Theme, beam search, and related preferences |

---

## Architecture

Gradle modules (simplified):

| Module | Role |
|--------|------|
| `:app` | Application shell, navigation, Hilt |
| `:domain` | Models, repository interfaces, use cases |
| `:data` | Room, repository implementations (ASR, history, LLM) |
| `:core:media` | Audio capture, **Sherpa-NCNN** JNI (`ncnn_asr`), bundled ncnn prebuilts under `src/main/cpp` |
| `:core:llm` | **ncnn** LLM via JNI (`llm_inference`), sources under `src/main/cpp/ncnn_llm` |
| `:core:startup` | Startup DAG, **ASR** and **LLM** model lifecycle (`AsrModelManager`, `LlmModelManager`) |
| `:core:common` | Shared utilities (e.g. inference coordination) |
| `:core:ui` | Compose theme |
| `:feature:home` | Main ASR screen |
| `:feature:history` | History list, detail, summarize / download UI |
| `:feature:settings` | Settings screen |

Native ASR follows the upstream **sherpa-ncnn** layout: the repo expects a **`sherpa-ncnn/`** directory at the project root (see `core/media/src/main/cpp/CMakeLists.txt`) with Android build outputs so headers and `libncnn.a` can be resolved. Prebuilt **ncnn** Android Vulkan packages are vendored next to the CMake tree for fallback linking.

---

## Requirements

- **Android Studio** with **Android SDK** and **NDK** (CMake builds `:core:media` and `:core:llm`)
- **JDK 17** (Gradle / Kotlin toolchain)
- **Device / ABI**: `arm64-v8a` and `armeabi-v7a` (`ndk { abiFilters … }` in Gradle)
- **Permissions**: `RECORD_AUDIO`; `INTERNET` for **downloading** the optional LLM asset pack (inference itself is offline)

---

## Clone and native prerequisites

```bash
git clone https://github.com/YihanLi-erisaer/smeeting.git
cd smeeting
git submodule update --init --recursive
git clone https://github.com/k2-fsa/sherpa-ncnn.git
# more steps to set up for android see https://k2-fsa.github.io/sherpa/ncnn/android/build-sherpa-ncnn.html#download-sherpa-ncnn
```

Then set up **Sherpa-NCNN** for Android the same way the upstream project does: clone **`sherpa-ncnn`** beside the app sources and follow the official Android build guide so the expected **`build-android-*`** trees and optional **`jniLibs/<abi>/libncnn.a`** layout exist for CMake.

- [Sherpa / NCNN Android build](https://k2-fsa.github.io/sherpa/ncnn/android/build-sherpa-ncnn.html)  
- [sherpa-ncnn](https://github.com/k2-fsa/sherpa-ncnn)

The optional summarization stack does **not** use **llama.cpp** in this tree: `:core:llm` builds **`libllm_inference.so`** from the vendored **`ncnn_llm`** sources and links against the same ncnn prebuilts as documented in `core/llm/src/main/cpp/CMakeLists.txt`.

---

## ASR model assets

Streaming ASR reads model files from Android **assets** with fixed names (see `AsrModelManager`): `encoder.param`, `encoder.bin`, `decoder.param`, `decoder.bin`, `joiner.param`, `joiner.bin`, `tokens.txt`. Keep these names when you swap models.

---

## On-device LLM (summarization)

| Item | Detail |
|------|--------|
| **Runtime** | **ncnn** (JNI library `libllm_inference.so`, project `llm_inference` in CMake) |
| **Default bundle** | **Qwen3 0.6B** ncnn **FP16** weights + tokenizer files |
| **Storage** | App **internal storage**: `filesDir/qwen3_0.6b_ncnn/` (see `LlmModelManager`) |
| **Download** | Multi-part HTTP download from `https://mirrors.sdu.edu.cn/ncnn_modelzoo/qwen3_0.6b/` (individual filenames such as `qwen3_decoder.ncnn.fp16.bin`, `model_fp16.json`, vocab, merges, etc.) |

First-time summarization: use the in-app **download** action on a history entry; after all parts are present, **Summarize** loads the graph (first load can take noticeable time on low-memory devices).

---

## Build

Open the **`Kotlin-ASR-with-ncnn`** project directory in Android Studio, sync Gradle, then build **Debug** or **Release** for a physical ARM device (recommended) or a compatible emulator.

```bash
./gradlew :app:assembleDebug
```

On Windows:

```bat
gradlew.bat :app:assembleDebug
```

If CMake cannot find Sherpa-NCNN / ncnn artifacts, re-check the **`sherpa-ncnn`** clone location and Android build outputs against `core/media/src/main/cpp/CMakeLists.txt`.

---

## User interface (screenshots version: v4.0.0-beta)
<img width="317" height="686" alt="image" src="https://github.com/user-attachments/assets/8b7ec787-c421-4074-bf2d-ea4b1521b82b" />
<img width="316" height="684" alt="image" src="https://github.com/user-attachments/assets/37fb8a4d-45c6-4fb0-b75b-900e6ee88b23" />
<img width="314" height="685" alt="image" src="https://github.com/user-attachments/assets/dd134a00-eb32-40d0-aee8-c7f90fa9d4da" />
<img width="317" height="685" alt="image" src="https://github.com/user-attachments/assets/6e25f5ba-0016-4c17-8c78-49a6a8253e30" />
<img width="316" height="681" alt="image" src="https://github.com/user-attachments/assets/cbabcd98-39e0-4522-ae85-94b602e807b7" />
<img width="317" height="686" alt="image" src="https://github.com/user-attachments/assets/592b7108-b487-4093-b8a5-7c27f55d8adb" />
<img width="317" height="685" alt="image" src="https://github.com/user-attachments/assets/e28b4b89-bf95-4fab-a51b-9111835007d9" />


---

## Performance notes

Figures depend on device, model, and Vulkan availability. The performance data only is a reference on the Helio G99 chip.

**performance on a android device Helio G99 (CPU) processor using armv8 libs only running ASR model**
| Metric       | Value   |
| ------------ | ------- |
| Memory Usage | ~250 MB  |
| Latency      | ~120 ms  |
| Chinese Accuracy (in chaos environment)     | ~89%  |
| Chinese Accuracy (in quiet environment)     | ~95%  |
| English Accuracy (in chaos environment)     | ~92%  |
| English Accuracy (in quiet environment)     | ~97%  |

**performance on a android device Helio G99 (CPU) processor running LLM model**
| Metric       | Value   |
| ------------ | ------- |
| Memory Usage | ~1.4 GB  |
| Quantization | 4-bit Integer |
| Throughput | ~1.0 token/s |
| Latency (Average Summary)      | ~60 seconds  |

---

## References

- [sherpa-ncnn](https://github.com/k2-fsa/sherpa-ncnn) — streaming ASR with ncnn  
- [Sherpa / NCNN Android build](https://k2-fsa.github.io/sherpa/ncnn/android/build-sherpa-ncnn.html)  
- [ncnn](https://github.com/Tencent/ncnn)  
- [Qwen3 0.6B ncnn bundle (download host)](https://mirrors.sdu.edu.cn/ncnn_modelzoo/qwen3_0.6b/)  

---

## License

This project is licensed under the Apache License 2.0. See the [LICENSE](LICENSE) file for details.
