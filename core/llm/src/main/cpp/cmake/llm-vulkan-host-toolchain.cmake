# Used only for ggml's vulkan-shaders-gen (Windows host build while cross-compiling Android).
# Must NOT use NDK clang — that linker cannot resolve -lkernel32 for Windows .exe tests.
#
# A stale CMakeCache from a failed run can keep CMAKE_C_COMPILER=NDK clang; clear before forcing LLVM.
# Override: pass -DGGML_VULKAN_SHADERS_GEN_TOOLCHAIN=... or set llm.vulkan.hostToolchain in local.properties.

unset(CMAKE_C_COMPILER CACHE)
unset(CMAKE_CXX_COMPILER CACHE)

set(_pf86 "$ENV{ProgramFiles\(x86\)}")
set(_cc "")
set(_cxx "")

if(DEFINED ENV{LLVM_BIN})
    if(EXISTS "$ENV{LLVM_BIN}/clang.exe")
        set(_cc "$ENV{LLVM_BIN}/clang.exe")
        set(_cxx "$ENV{LLVM_BIN}/clang++.exe")
    endif()
endif()
if(NOT _cc)
    foreach(_root IN ITEMS "$ENV{ProgramFiles}/LLVM" "${_pf86}/LLVM")
        if(EXISTS "${_root}/bin/clang.exe")
            set(_cc "${_root}/bin/clang.exe")
            set(_cxx "${_root}/bin/clang++.exe")
            break()
        endif()
    endforeach()
endif()

if(NOT _cc)
    file(GLOB _msvc_cl
        "$ENV{ProgramFiles}/Microsoft Visual Studio/*/VC/Tools/MSVC/*/bin/Hostx64/x64/cl.exe"
        "${_pf86}/Microsoft Visual Studio/*/VC/Tools/MSVC/*/bin/Hostx64/x64/cl.exe"
    )
    if(_msvc_cl)
        list(SORT _msvc_cl ORDER DESCENDING)
        list(GET _msvc_cl 0 _cc)
        set(_cxx "${_cc}")
    endif()
endif()

if(NOT _cc)
    find_program(_gcc NAMES gcc.exe PATHS
        "C:/msys64/ucrt64/bin"
        "C:/msys64/mingw64/bin"
        "C:/msys64/clang64/bin"
        NO_DEFAULT_PATH)
    find_program(_gxx NAMES g++.exe PATHS
        "C:/msys64/ucrt64/bin"
        "C:/msys64/mingw64/bin"
        "C:/msys64/clang64/bin"
        NO_DEFAULT_PATH)
    if(_gcc AND _gxx)
        set(_cc "${_gcc}")
        set(_cxx "${_gxx}")
    endif()
endif()

if(NOT _cc)
    message(FATAL_ERROR
        "LLM Vulkan: no Windows host C/C++ compiler for vulkan-shaders-gen.\n"
        "Install one of: LLVM for Windows (https://releases.llvm.org/) into \"C:/Program Files/LLVM\", "
        "Visual Studio C++ Build Tools, or MSYS2 MinGW. Optionally set LLVM_BIN or "
        "llm.vulkan.hostToolchain in local.properties.\n"
        "Then run :core:llm:clean and rebuild.")
endif()

# GNU-like Clang on Windows needs a resource compiler (llvm-rc or Windows SDK rc.exe).
set(_rc "")
find_program(_rc NAMES llvm-rc.exe PATHS
    "$ENV{ProgramFiles}/LLVM/bin"
    "${_pf86}/LLVM/bin"
    NO_DEFAULT_PATH)
if(NOT _rc)
    file(GLOB _kit_rc
        "${_pf86}/Windows Kits/10/bin/*/x64/rc.exe"
        "$ENV{ProgramFiles}/Windows Kits/10/bin/*/x64/rc.exe"
    )
    if(_kit_rc)
        list(SORT _kit_rc ORDER DESCENDING)
        list(GET _kit_rc 0 _rc)
    endif()
endif()
if(NOT _rc)
    find_program(_rc NAMES rc.exe PATHS ENV PATH)
endif()
if(NOT _rc)
    message(FATAL_ERROR
        "LLM Vulkan: CMAKE_RC_COMPILER not found (llvm-rc or Windows SDK rc.exe). "
        "Install Windows 10/11 SDK (Visual Studio installer) or ensure LLVM bin is on PATH.")
endif()

set(CMAKE_C_COMPILER "${_cc}" CACHE FILEPATH "" FORCE)
set(CMAKE_CXX_COMPILER "${_cxx}" CACHE FILEPATH "" FORCE)
set(CMAKE_RC_COMPILER "${_rc}" CACHE FILEPATH "" FORCE)

# MSVC 14.5x headers + LLVM Clang 18: yvals_core.h may require Clang 19+ (STL1000). Prefer upgrading LLVM;
# this define is the supported escape hatch when mixing versions.
set(CMAKE_CXX_FLAGS_INIT "-D_ALLOW_COMPILER_AND_STL_VERSION_MISMATCH" CACHE STRING "" FORCE)

message(STATUS "LLM vulkan-shaders-gen host: C=${CMAKE_C_COMPILER} CXX=${CMAKE_CXX_COMPILER} RC=${CMAKE_RC_COMPILER}")
