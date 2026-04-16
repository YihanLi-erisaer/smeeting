# smeeting

On-device **streaming speech recognition** for Android, powered by **Sherpa-NCNN** and **ncnn**, plus optional **on-device text summarization** of saved transcripts using **llama.cpp** and a small **Qwen2.5** GGUF model.

---

## Overview

- **ASR**: Real-time speech-to-text runs entirely on the device (no cloud ASR). Audio is captured at 16 kHz mono, processed through a native JNI pipeline, and results are shown in a Jetpack Compose UI.
- **Privacy**: All inference is performed locally. Microphone audio and ASR inference stay on-device. Transcripts can be stored in local history (Room).
- **Summaries (optional)**: From **Transcription history**, you can download a quantized LLM (~1 GB) once, then generate **streaming summaries** (key points, action items, etc.). Inference uses **llama.cpp** on the CPU; ASR and LLM are coordinated so they do not run at the same time to reduce memory pressure.

---

## Features

| Area | Description |
|------|-------------|
| Streaming ASR | Live partial and final transcripts with endpointing |
| GPU / CPU | Tries Vulkan for the encoder when available, falls back to CPU |
| History | Saved entries, copy/delete, detail view |
| On-device LLM | Download **Qwen2.5-1.5B-Instruct** `Q4_K_M` GGUF; summarize history entries offline |
| Settings | Theme, beam search, and related preferences |

---

## Architecture

Gradle modules (simplified):

| Module | Role |
|--------|------|
| `:app` | Application shell, navigation, Hilt |
| `:domain` | Models, repository interfaces, use cases |
| `:data` | Room, repository implementations (ASR, history, LLM) |
| `:core:media` | Audio capture, **Sherpa-NCNN** JNI (`ncnn_asr`) |
| `:core:llm` | **llama.cpp** via JNI (`llm_inference`) |
| `:core:startup` | Startup DAG, **ASR** and **LLM** model lifecycle (`AsrModelManager`, `LlmModelManager`) |
| `:core:common` | Shared utilities (e.g. inference coordination) |
| `:core:ui` | Compose theme |
| `:feature:home` | Main ASR screen |
| `:feature:history` | History list, detail, summarize / download UI |
| `:feature:settings` | Settings screen |

Native ASR follows the upstream **sherpa-ncnn** / **ncnn** integration pattern (see project `CMakeLists.txt` and `sherpa-ncnn` tree). The LLM stack is a **git submodule** under `core/llm/src/main/cpp/llama.cpp`.

---

## Requirements

- **Android Studio** with **Android SDK** and **NDK** (project uses native CMake for `:core:media` and `:core:llm`)
- **JDK 17** (toolchain aligned with Gradle / Kotlin)
- **Device / ABI**: `arm64-v8a` and `armeabi-v7a` (see `ndk { abiFilters … }` in Gradle)
- **Permissions**: `RECORD_AUDIO`; `INTERNET` only for **downloading** the LLM GGUF (ASR and LLM inference does not require network)

---

## Clone and submodules

```bash
git clone https://github.com/YihanLi-erisaer/smeeting.git
cd smeeting
git submodule update --init --recursive
git clone https://github.com/k2-fsa/sherpa-ncnn.git
# more steps to set up for android see https://k2-fsa.github.io/sherpa/ncnn/android/build-sherpa-ncnn.html#download-sherpa-ncnn
cd core\llm\src\main\cpp
git clone https://github.com/ggml-org/llama.cpp.git
```

Initialize at least:

- `core/llm/src/main/cpp/llama.cpp` — **llama.cpp** (for on-device summarization builds)

Sherpa-ncnn / ncnn native libraries and ASR model assets for Android are **not** fully covered by this README; follow the official sherpa-ncnn Android build guide to produce the expected **`jniLibs`** and **assets** layout expected by `AsrModelManager` / native code in your environment.

---

## ASR model assets

Streaming ASR expects model files under Android **assets** (names such as `encoder.param`, `encoder.bin`, `decoder.*`, `joiner.*`, `tokens.txt`). Paths are wired in startup / native init — keep filenames consistent with `AsrModelManager` when you swap models.

## LLM model

LLM model will be downloaded within the application.

---

## On-device LLM (summarization)

| Item | Detail |
|------|--------|
| **Runtime** | **llama.cpp** (linked as `libllm_inference.so`) |
| **Default model** | **Qwen2.5-1.5B-Instruct** GGUF **`q4_k_m`** (~1 GB on disk) |
| **Storage** | Downloaded to app **internal storage**: `context.filesDir` / `qwen2.5-1.5b-instruct-q4_k_m.gguf` |
| **Source** | Hugging Face: `Qwen/Qwen2.5-1.5B-Instruct-GGUF` (URL is defined in `LlmModelManager`) |

First-time summarization: use the in-app **download** action in history detail; after the file is present, the model is loaded and **Summarize** becomes available.

---

## Build

Open the **`Kotlin-ASR-with-ncnn`** directory in Android Studio and sync Gradle, then build **Debug** or **Release** for a physical ARM device (recommended) or a compatible emulator.

```bash
./gradlew :app:assembleDebug
```

On Windows:

```bat
gradlew.bat :app:assembleDebug
```

If CMake fails for **`armeabi-v7a`**, the project disables **GGML LLAMAFILE** for that ABI in `core/llm/src/main/cpp/CMakeLists.txt` to avoid NEON FP16 intrinsics that are unavailable on typical 32-bit ARM targets.

---

## User interface (screenshots)

Screenshots are **not** embedded in this README. Add your own under e.g. `docs/screenshots/` and reference them here.

| Placeholder | Suggested capture |
|-------------|-------------------|
| `<!-- SCREENSHOT: home-asr -->` | Main screen: recording / streaming transcript, backend indicator |
| `<!-- SCREENSHOT: history-list -->` | History list with entries and actions |
| `<!-- SCREENSHOT: history-detail -->` | Entry detail: full transcript |
| `<!-- SCREENSHOT: history-summary -->` | Same screen with AI summary / download / progress |
| `<!-- SCREENSHOT: settings -->` | Settings: theme, beam search, version |

Example markdown once files exist:

```markdown
![Home ASR](docs/screenshots/home-asr.png)
```

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
- [llama.cpp](https://github.com/ggerganov/llama.cpp)  
- [Qwen2.5 GGUF](https://huggingface.co/Qwen/Qwen2.5-1.5B-Instruct-GGUF)

---

## License

This project is licensed under the Apache License 2.0. See the [LICENSE](LICENSE) file for details.
