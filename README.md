# Kotlin-Zipformer: On-device Streaming Speech Recognition System

## Overview

This project presents an on-device automatic speech recognition (ASR) system implemented in Kotlin, based on the Zipformer architecture. The system enables real-time speech-to-text transcription on edge devices, without reliance on cloud services.

The project focuses on bridging modern deep learning-based ASR models with practical mobile deployment, emphasizing latency, memory efficiency, and usability in real-world scenarios.

---

## Motivation

Recent advances in speech recognition have been dominated by Transformer-based models such as Conformer and Whisper. However, these models often require significant computational resources, making them less suitable for on-device inference.

Zipformer is a recently proposed architecture designed to improve both efficiency and accuracy in ASR systems. It introduces a multi-scale U-Net-like encoder and optimized attention mechanisms, enabling faster and more memory-efficient inference.

This project aims to explore how such state-of-the-art models can be effectively deployed in a Kotlin-based application environment.

---
## How to Use

This section provides instructions for running the Kotlin-Zipformer on-device ASR system.

### 1. Prerequisites

- **Operating System:** Android 10+ / JVM compatible OS  
- **Kotlin Version:** 1.8+  
- **Dependencies:**  
  - sherpa-ncnn (for model inference)  
  - Gradle 7.0+  
  - Audio recording permissions on mobile devices  

---

### 2. Clone the Repository

```bash
git clone https://github.com/YihanLi-erisaer/Kotlin-zipformer.git
cd Kotlin-zipformer
git clone https://github.com/k2-fsa/sherpa-ncnn.git
# more steps to set up for android see https://k2-fsa.github.io/sherpa/ncnn/android/build-sherpa-ncnn.html#download-sherpa-ncnn
```
---

## System Architecture

The system consists of the following components:

1. **Audio Input Module**

   * Captures real-time audio stream from device microphone
   * Performs preprocessing (framing, normalization)

2. **Feature Extraction**

   * Converts raw waveform into acoustic features (e.g., log-Mel spectrogram)

3. **Zipformer Inference Engine**

   * Loads pretrained Zipformer model
   * Performs streaming inference
   * Maintains low-latency decoding

4. **Decoding & Post-processing**

   * Converts model outputs into text
   * Applies token merging and formatting

5. **Application Layer**

   * Provides user interface and interaction
   * Displays transcription results in real time

---

## Key Features

* **On-device inference**: No cloud dependency, ensuring privacy and low latency
* **Streaming ASR**: Real-time transcription capability
* **Efficient model architecture**: Leveraging Zipformer for reduced computational cost
* **Cross-language support**: Supports Chinese-English mixed speech recognition
* **Lightweight deployment**: Optimized for mobile environments

---

## Technical Highlights

* Integration of state-of-the-art Zipformer ASR model
* Efficient handling of streaming audio data
* Optimization for latency-sensitive applications
* Modular system design for extensibility
* Start up optimization, Kotlin-Zipformer implements an **on-device ASR system** optimized for **low-latency and minimal memory usage**. The startup process is organized into three stages:

1. **Application Bootstrap**  
   - UI launches immediately while heavy initialization runs asynchronously using **Kotlin coroutines**.  
   - Non-critical tasks (e.g., model checks) are deferred to background threads.

2. **Model & Engine Setup**  
   - **Lazy initialization** of Sherpa-NCNN + Zipformer engine.  
   - Local **model caching** avoids redundant loading.  
   - Asynchronous setup minimizes startup latency (~250MB memory).

3. **Audio Pipeline Prewarming**  
   - Microphone and feature extraction pipelines are preloaded.  
   - Streaming buffers allocated in advance for **real-time first-frame processing**.

> **Result:** Fast app launch, low-latency inference, and ready-to-use real-time ASR on mobile or edge devices.
---

## Performance
**performance on a android device Helio G99 processor using armv8 libs**
| Metric       | Value   |
| ------------ | ------- |
| Memory Usage | ~250 MB  |
| Latency      | ~120 ms  |
| Chinese Accuracy (in chaos environment)     | ~89%  |
| Chinese Accuracy (in quiet environment)     | ~95%  |
| English Accuracy (in chaos environment)     | ~92%  |
| English Accuracy (in quiet environment)     | ~97%  |

---

## Technologies Used

* Kotlin (Android / JVM)
* sherpa-ncnn / Native inference backend
* Digital signal processing (DSP)
* Deep learning-based ASR model (Zipformer)

---

## Use Cases

* Voice assistants
* Real-time transcription
* Accessibility tools
* Edge AI applications

---

## Language support

* Chinese (Simplify)
* English

---

## User Interface

<img width="474" height="547" alt="image" src="https://github.com/user-attachments/assets/84afa1bd-c219-4438-a57f-19551a5378b4" />
<img width="476" height="548" alt="image" src="https://github.com/user-attachments/assets/0bc25075-c356-4eae-9934-7c48b5c1676d" />
<img width="476" height="548" alt="image" src="https://github.com/user-attachments/assets/833ad9b8-134e-4544-b3ab-7cfa6042b9df" />
<img width="475" height="547" alt="image" src="https://github.com/user-attachments/assets/f12f75d9-a89f-47f4-acbe-39c4b6b03475" />

---

## Limitations & Future Work

* Model size can still be optimized further
* Accuracy may degrade in noisy environments
* Future work:

  * Model quantization
  * Multi-language expansion
  * Speaker diarization
  * Integration with LLM for downstream tasks
  * Make model inferencing by GPU

---

## Conclusion

This project demonstrates the feasibility of deploying modern ASR architectures on edge devices using Kotlin. It highlights the potential of combining efficient deep learning models with practical software engineering to build scalable and user-friendly AI applications.

---

## References
[sherpa_ncnn](https://github.com/k2-fsa/sherpa-ncnn)

---

## License

This project is licensed under the Apache License 2.0. See the [LICENSE](LICENSE) file for details.

---
