cmake_minimum_required(VERSION 3.22.1)
project("llm_inference")

set(LIBRARY_NAME llm_inference)
set(LLAMA_CPP_DIR ${CMAKE_CURRENT_SOURCE_DIR}/llama.cpp)

# llama.cpp build options: static, no extras
set(BUILD_SHARED_LIBS OFF CACHE BOOL "" FORCE)
set(LLAMA_BUILD_COMMON OFF CACHE BOOL "" FORCE)
set(LLAMA_BUILD_TESTS OFF CACHE BOOL "" FORCE)
set(LLAMA_BUILD_TOOLS OFF CACHE BOOL "" FORCE)
set(LLAMA_BUILD_EXAMPLES OFF CACHE BOOL "" FORCE)
set(LLAMA_BUILD_SERVER OFF CACHE BOOL "" FORCE)

# Android: CPU-only (NEON is auto-detected by ggml)
set(GGML_METAL OFF CACHE BOOL "" FORCE)
set(GGML_CUDA OFF CACHE BOOL "" FORCE)
set(GGML_VULKAN OFF CACHE BOOL "" FORCE)
set(GGML_OPENCL OFF CACHE BOOL "" FORCE)

# armeabi-v7a: llamafile sgemm uses NEON FP16 intrinsics (vld1_f16) that are not available on
# typical 32-bit ARM targets; disable before add_subdirectory so ggml skips that code path.
if(ANDROID_ABI STREQUAL "armeabi-v7a")
    set(GGML_LLAMAFILE OFF CACHE BOOL "" FORCE)
endif()

if(EXISTS "${LLAMA_CPP_DIR}/CMakeLists.txt")
    add_subdirectory(${LLAMA_CPP_DIR} llama.cpp)
else()
    message(FATAL_ERROR
        "llama.cpp source not found at ${LLAMA_CPP_DIR}.\n"
        "Run: git submodule add https://github.com/ggerganov/llama.cpp core/llm/src/main/cpp/llama.cpp")
endif()

find_library(log-lib log)
find_library(android-lib android)

add_library(${LIBRARY_NAME} SHARED llm_jni.cpp)

target_include_directories(${LIBRARY_NAME} PRIVATE
    ${LLAMA_CPP_DIR}/include
    ${LLAMA_CPP_DIR}/ggml/include
)

target_link_libraries(${LIBRARY_NAME}
    llama
    ${log-lib}
    ${android-lib}
)
