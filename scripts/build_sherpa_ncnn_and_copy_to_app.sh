#!/usr/bin/env bash
# Build sherpa-ncnn for Android (static libs, with the Model::Create null-check fix)
# and copy the merged libsherpa-ncnn-core.a to the app's jniLibs so the app no longer crashes.
#
# Prerequisites: ANDROID_NDK set (or edit sherpa-ncnn/build-android-arm64-v8a.sh).
# Run from repo root (parent of sherpa-ncnn and Kotlin-ASR-with-ncnn, or where sherpa-ncnn lives).
set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"
SHERPA_DIR="${REPO_ROOT}/sherpa-ncnn"
TARGET_ABI="${1:-arm64-v8a}"

case "${TARGET_ABI}" in
  arm64-v8a)
    SHERPA_BUILD_SCRIPT="build-android-arm64-v8a.sh"
    SHERPA_BUILD_DIR="build-android-arm64-v8a"
    ;;
  armeabi-v7a)
    SHERPA_BUILD_SCRIPT="build-android-armv7-eabi.sh"
    SHERPA_BUILD_DIR="build-android-armv7-eabi"
    ;;
  *)
    echo "Unsupported ABI: ${TARGET_ABI}"
    echo "Usage: $0 [arm64-v8a|armeabi-v7a]"
    exit 1
    ;;
esac

APP_JNILIBS="${REPO_ROOT}/core/media/src/main/cpp/jniLibs/${TARGET_ABI}"

echo "Building sherpa-ncnn in: ${SHERPA_DIR}"
echo "Target ABI: ${TARGET_ABI}"
echo "Target jniLibs: ${APP_JNILIBS}"

cd "${SHERPA_DIR}"
./"${SHERPA_BUILD_SCRIPT}"

BUILD_LIB="${SHERPA_DIR}/${SHERPA_BUILD_DIR}/lib"
if [ ! -d "${BUILD_LIB}" ]; then
  echo "Expected build dir not found: ${BUILD_LIB}"
  exit 1
fi

mkdir -p "${APP_JNILIBS}"

# Merge static libs into one libsherpa-ncnn-core.a so the app can link a single .a
# (sherpa-ncnn-core depends on kaldifst_core, fst, fstfar; we merge them to avoid adding more jniLibs)
MERGED="${APP_JNILIBS}/libsherpa-ncnn-core.a"
BUILD_ROOT="${SHERPA_DIR}/${SHERPA_BUILD_DIR}"

if command -v libtool >/dev/null 2>&1; then
  # Collect all .a under build dir except ones the app already links (ncnn, kaldi, kissfft, glslang)
  TO_MERGE=()
  while IFS= read -r -d '' a; do
    base=$(basename "$a")
    case "$base" in
      libncnn.a|libkaldi*|libkissfft*|libglslang*|libSPIRV*|libMachineIndependent*|libGenericCodeGen*|libOSDependent*|libglslang-default*) ;;
      *) TO_MERGE+=("$a") ;;
    esac
  done < <(find "${BUILD_ROOT}" -name "*.a" -print0 2>/dev/null)
  if [ ${#TO_MERGE[@]} -eq 0 ]; then
    echo "No .a files found; copying libsherpa-ncnn-core.a from lib/ if present."
    cp -v "${BUILD_LIB}/libsherpa-ncnn-core.a" "${MERGED}"
  else
    libtool -static -o "${MERGED}" "${TO_MERGE[@]}"
    echo "Merged ${#TO_MERGE[@]} libs -> ${MERGED}"
  fi
else
  # Fallback: copy only sherpa-ncnn-core (may need extra .a in jniLibs if link fails)
  cp -v "${BUILD_LIB}/libsherpa-ncnn-core.a" "${MERGED}"
  echo "Copied libsherpa-ncnn-core.a (no libtool; if link fails, add other .a from ${BUILD_LIB} to jniLibs and CMake)"
fi

# Copy sherpa-ncnn's libncnn.a (non-Vulkan) to avoid Vulkan crashes on some devices
[ -f "${BUILD_LIB}/libncnn.a" ] && cp -v "${BUILD_LIB}/libncnn.a" "${APP_JNILIBS}/" && echo "Copied libncnn.a (non-Vulkan)" || echo "libncnn.a not found, app will use prebuilt"

echo "Done. Rebuild the Android app so it uses the new lib (with the null-check fix)."
