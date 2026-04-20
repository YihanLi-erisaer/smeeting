cmake_minimum_required(VERSION 3.22.1)
project("llm_inference")

set(LIBRARY_NAME llm_inference)

set(MEDIA_CPP ${CMAKE_CURRENT_SOURCE_DIR}/../../../../media/src/main/cpp)
set(NCNN_VULKAN_DIR ${MEDIA_CPP}/ncnn-20260113-android-vulkan/${ANDROID_ABI})
set(NCNN_VULKAN_INCLUDE ${NCNN_VULKAN_DIR}/include/ncnn)

find_library(log-lib log)
find_library(android-lib android)
find_library(vulkan-lib vulkan)

find_package(OpenMP REQUIRED)

add_library(ncnn STATIC IMPORTED)
set_target_properties(ncnn PROPERTIES IMPORTED_LOCATION ${NCNN_VULKAN_DIR}/lib/libncnn.a)

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

set(NCNN_LLM_ROOT ${CMAKE_CURRENT_SOURCE_DIR}/ncnn_llm/src)
set(NCNN_LLM_SOURCES
    ${NCNN_LLM_ROOT}/ncnn_llm_gpt.cpp
    ${NCNN_LLM_ROOT}/sampling.cpp
    ${CMAKE_CURRENT_SOURCE_DIR}/gdr_stub.cpp
    ${NCNN_LLM_ROOT}/utils/image_utils.cpp
    ${NCNN_LLM_ROOT}/utils/prompt.cpp
    ${NCNN_LLM_ROOT}/utils/rope_embed.cpp
    ${NCNN_LLM_ROOT}/utils/tokenizer/bpe_tokenizer.cpp
    ${NCNN_LLM_ROOT}/utils/tokenizer/unigram_tokenizer.cpp
)

add_library(${LIBRARY_NAME} SHARED
    llm_jni.cpp
    ${NCNN_LLM_SOURCES}
)

set_target_properties(${LIBRARY_NAME} PROPERTIES LINK_FLAGS "-static-openmp")

set(CMAKE_CXX_STANDARD 20)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

target_include_directories(${LIBRARY_NAME} PRIVATE
    ${NCNN_LLM_ROOT}
    ${NCNN_VULKAN_INCLUDE}
    ${CMAKE_CURRENT_SOURCE_DIR}/third_party
)

target_compile_definitions(${LIBRARY_NAME} PRIVATE
    NCNN_PIXEL=1
    NCNN_VULKAN=1
)

target_link_libraries(${LIBRARY_NAME}
    ncnn
    glslang
    MachineIndependent
    GenericCodeGen
    SPIRV
    OSDependent
    glslang-default-resource-limits
    OpenMP::OpenMP_CXX
    ${log-lib}
    ${android-lib}
    ${vulkan-lib}
    atomic
)
