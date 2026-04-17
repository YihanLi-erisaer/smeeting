# Copy/rename to a custom path only if you need a fixed compiler outside the default search in
# llm-vulkan-host-toolchain.cmake. Then set in local.properties:
#   llm.vulkan.hostToolchain=C\:\\path\\to\\this\\file.cmake

set(CMAKE_C_COMPILER "C:/Program Files/LLVM/bin/clang.exe" CACHE FILEPATH "" FORCE)
set(CMAKE_CXX_COMPILER "C:/Program Files/LLVM/bin/clang++.exe" CACHE FILEPATH "" FORCE)
