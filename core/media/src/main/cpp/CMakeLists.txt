cmake_minimum_required(VERSION 3.22.1)
project("kotlin_asr_with_ncnn")

# Force native rebuild when sherpa-ncnn libs are updated (timestamp bump)
set(LIBRARY_NAME ncnn_asr)

find_library(log-lib log)
find_library(android-lib android)
find_library(vulkan-lib vulkan)

# OpenMP configuration
find_package(OpenMP REQUIRED)

set(ROOT_DIR ${CMAKE_CURRENT_SOURCE_DIR}/../../../../..)

# Prefer sherpa-ncnn's libncnn.a (non-Vulkan) to avoid Vulkan crashes on MediaTek/some devices
set(NCNN_JNILIBS ${CMAKE_CURRENT_SOURCE_DIR}/jniLibs/${ANDROID_ABI}/libncnn.a)
set(NCNN_VULKAN_DIR ${CMAKE_CURRENT_SOURCE_DIR}/ncnn-20260113-android-vulkan/${ANDROID_ABI})
set(NCNN_VULKAN_INCLUDE ${NCNN_VULKAN_DIR}/include/ncnn)

# sherpa-ncnn's folder name for armeabi-v7a build is "armv7-eabi".
if(ANDROID_ABI STREQUAL "armeabi-v7a")
  set(SHERPA_BUILD_ABI armv7-eabi)
else()
  set(SHERPA_BUILD_ABI ${ANDROID_ABI})
endif()

set(NCNN_SRC_DIR ${ROOT_DIR}/sherpa-ncnn/build-android-${SHERPA_BUILD_ABI}/_deps/ncnn-src/src)
set(NCNN_BUILD_SRC ${ROOT_DIR}/sherpa-ncnn/build-android-${SHERPA_BUILD_ABI}/_deps/ncnn-build/src)
if(EXISTS "${NCNN_JNILIBS}" AND EXISTS "${NCNN_SRC_DIR}" AND EXISTS "${NCNN_BUILD_SRC}")
  set(NCNN_INCLUDE ${NCNN_BUILD_SRC} ${NCNN_SRC_DIR})  # platform.h is in build dir
  message(STATUS "Using sherpa-ncnn's libncnn.a (non-Vulkan) from jniLibs")
  add_library(ncnn STATIC IMPORTED)
  set_target_properties(ncnn PROPERTIES IMPORTED_LOCATION ${NCNN_JNILIBS})
  set(NCNN_NEED_GLSLANG OFF)
else()
  set(NCNN_INCLUDE ${NCNN_VULKAN_DIR}/include/ncnn)
  add_library(ncnn STATIC IMPORTED)
  set_target_properties(ncnn PROPERTIES IMPORTED_LOCATION ${NCNN_VULKAN_DIR}/lib/libncnn.a)
  set(NCNN_NEED_GLSLANG ON)
endif()

include_directories(
    ${ROOT_DIR}/sherpa-ncnn
    ${NCNN_INCLUDE}
)

if(NCNN_NEED_GLSLANG)
  add_library(glslang STATIC IMPORTED)
  set_target_properties(glslang PROPERTIES IMPORTED_LOCATION ${NCNN_VULKAN_DIR}/lib/libglslang.a)
  add_library(SPIRV STATIC IMPORTED)
  set_target_properties(SPIRV PROPERTIES IMPORTED_LOCATION ${NCNN_VULKAN_DIR}/lib/libSPIRV.a)
  add_library(OSDependent STATIC IMPORTED)
  set_target_properties(OSDependent PROPERTIES IMPORTED_LOCATION ${NCNN_VULKAN_DIR}/lib/libOSDependent.a)
  add_library(MachineIndependent STATIC IMPORTED)
  set_target_properties(MachineIndependent PROPERTIES IMPORTED_LOCATION ${NCNN_VULKAN_DIR}/lib/libMachineIndependent.a)
  add_library(GenericCodeGen STATIC IMPORTED)
  set_target_properties(GenericCodeGen PROPERTIES IMPORTED_LOCATION ${NCNN_VULKAN_DIR}/lib/libGenericCodeGen.a)
  add_library(glslang-default-resource-limits STATIC IMPORTED)
  set_target_properties(glslang-default-resource-limits PROPERTIES IMPORTED_LOCATION ${NCNN_VULKAN_DIR}/lib/libglslang-default-resource-limits.a)
endif()

add_library(sherpa-ncnn-core STATIC IMPORTED)
set_target_properties(sherpa-ncnn-core PROPERTIES IMPORTED_LOCATION ${CMAKE_CURRENT_SOURCE_DIR}/jniLibs/${ANDROID_ABI}/libsherpa-ncnn-core.a)

add_library(kaldi-native-fbank-core STATIC IMPORTED)
set_target_properties(kaldi-native-fbank-core PROPERTIES IMPORTED_LOCATION ${CMAKE_CURRENT_SOURCE_DIR}/jniLibs/${ANDROID_ABI}/libkaldi-native-fbank-core.a)

add_library(kissfft-float STATIC IMPORTED)
set_target_properties(kissfft-float PROPERTIES IMPORTED_LOCATION ${CMAKE_CURRENT_SOURCE_DIR}/jniLibs/${ANDROID_ABI}/libkissfft-float.a)

add_library(${LIBRARY_NAME} SHARED kotlin_asr_with_ncnn.cpp)

# Use -static-openmp to avoid dependency on libomp.so at runtime
set_target_properties(${LIBRARY_NAME} PROPERTIES LINK_FLAGS "-static-openmp")

set(NCNN_LIBS ncnn sherpa-ncnn-core kaldi-native-fbank-core kissfft-float OpenMP::OpenMP_CXX ${log-lib} ${android-lib} atomic)
if(NCNN_NEED_GLSLANG)
  list(APPEND NCNN_LIBS glslang MachineIndependent GenericCodeGen SPIRV OSDependent glslang-default-resource-limits ${vulkan-lib})
endif()
target_link_libraries(${LIBRARY_NAME} ${NCNN_LIBS})
