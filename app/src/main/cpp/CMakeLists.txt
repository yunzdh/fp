cmake_minimum_required(VERSION 3.22)
project(fp)

# 设置 C++ 标准
set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

# 引入 Android NDK 工具链
include(${CMAKE_ANDROID_NDK}/build/cmake/android.toolchain.cmake)

# 关键：不使用系统 find_package(cxx)，而是直接链接 Cargo 生成的 cxx 库
# 假设你的 Rust 代码通过 Cargo 构建，会生成 libfp_rust.so 和 cxx 相关对象
# 如果是纯 C++ 项目，直接引入 cxx 头文件路径

# 添加头文件路径（根据实际项目调整）
include_directories(
    ${CMAKE_SOURCE_DIR}/include
    ${CMAKE_SOURCE_DIR}/../rust/target/include  # cxx 生成的头文件路径
)

# 添加源文件（根据实际项目调整）
add_library(
    fp
    SHARED
    native-lib.cpp  # 你的 C++ 源文件
    # 其他源文件...
)

# 链接 KernelPatch 相关库
target_link_libraries(
    fp
    log
    android
    kpatch  # 已下载的 libkpatch.so
    kptools # 已下载的 libkptools.so
    # 如果有 Rust 生成的库，添加：
    # ${CMAKE_SOURCE_DIR}/../rust/target/armv7-linux-androideabi/release/libfp_rust.a
)
