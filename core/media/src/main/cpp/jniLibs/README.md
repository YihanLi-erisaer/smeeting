# Native libs for arm64-v8a (and other ABIs)

This folder must contain the **static** `.a` libraries so the app can link them:

- `libsherpa-ncnn-core.a` (must include the Model::Create null-check fix to avoid SIGSEGV on startup)
- `libkaldi-native-fbank-core.a`
- `libkissfft-float.a`

The app also uses ncnn/glslang from `../ncnn-20260113-android-vulkan/`.

## If the app crashes on startup (SIGSEGV in initModelNative)

The crash is fixed in **sherpa-ncnn source** (`sherpa-ncnn/sherpa-ncnn/csrc/recognizer.cc`). You need to **rebuild** `libsherpa-ncnn-core.a` and put it here.

### Option 1: Script (Unix / Git Bash / WSL)

From the **repo root** (parent of `sherpa-ncnn` and `core`):

```bash
# Set ANDROID_NDK if needed, e.g.:
# export ANDROID_NDK=/path/to/ndk

chmod +x scripts/build_sherpa_ncnn_and_copy_to_app.sh
./scripts/build_sherpa_ncnn_and_copy_to_app.sh
```

Then rebuild the Android app in Android Studio / Gradle.

### Option 2: Manual

1. In `sherpa-ncnn`, open `build-android-arm64-v8a.sh` and ensure it uses `-DBUILD_SHARED_LIBS=OFF` (it’s already set for this project).
2. Set `ANDROID_NDK` and run: `./build-android-arm64-v8a.sh`
3. Copy (or merge) the built static lib(s) into this folder:
   - `sherpa-ncnn/build-android-arm64-v8a/lib/libsherpa-ncnn-core.a` → here as `libsherpa-ncnn-core.a`
   - If the app fails to link with undefined references to kaldifst/fst, merge all `.a` from that `lib/` (except ncnn/kaldi/kissfft) into one `libsherpa-ncnn-core.a` with `libtool -static` and put the result here.

Create `arm64-v8a` (and optionally `armeabi-v7a`, `x86_64`) under `jniLibs/` and place the `.a` files in the matching ABI folder.
