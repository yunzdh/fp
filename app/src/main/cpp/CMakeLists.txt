cmake_minimum_required(VERSION 3.28.0)
project(FolkPatch)

# 配置 C++ 标准（与 build.gradle 一致）
set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)
set(CMAKE_CXX_EXTENSIONS OFF)

# 关键：设置 Android 相关配置（从 NDK 工具链自动获取）
if (DEFINED ANDROID)
    set(CMAKE_ANDROID_ARCH_ABI arm64-v8a)
    set(CMAKE_ANDROID_NDK ${ANDROID_NDK_HOME})
    set(CMAKE_ANDROID_STL_TYPE c++_shared) # 与 build.gradle 一致，支持 cxx
endif()

# 头文件路径（包含 cxx 生成的头文件和项目本地头文件）
include_directories(
    ${CMAKE_SOURCE_DIR}/include
    # 指向 Rust cxx 生成的头文件目录（Cargo 编译后自动生成）
    ${PROJECT_ROOT_DIR}/apd/target/aarch64-linux-android/release/include
    # 指向 NDK 头文件（适配 KernelPatch 依赖）
    ${ANDROID_NDK_HOME}/sysroot/usr/include
)

# 链接库路径（指向你的 libs 目录和 Rust 生成的库目录）
link_directories(
    ${PROJECT_DIR}/libs/arm64-v8a
    ${PROJECT_ROOT_DIR}/apd/target/aarch64-linux-android/release
)

# 构建核心库（与你的项目结构匹配）
add_library(
    folkpatch-core
    SHARED
    # 这里替换为你的 C++ 源文件（如 native-lib.cpp、kernelpatch-wrapper.cpp 等）
    # 示例：src/main/cpp 下的所有 C++ 文件
    ${CMAKE_SOURCE_DIR}/native-lib.cpp
    ${CMAKE_SOURCE_DIR}/kernelpatch-utils.cpp
)

# 链接依赖库（匹配你的项目依赖）
target_link_libraries(
    folkpatch-core
    PRIVATE
    # Android 系统库
    log
    android
    # KernelPatch 相关库（已下载到 libs/arm64-v8a）
    kpatch
    kptools
    # Rust 生成的库（Cargo 编译后生成的 libapd.so）
    apd
    # cxx 库（自动链接，无需手动指定，依赖 c++_shared）
    ${CMAKE_CXX_STANDARD_LIBRARIES}
)

# 配置输出路径（与 build.gradle 一致）
set_target_properties(
    folkpatch-core
    PROPERTIES
    LIBRARY_OUTPUT_DIRECTORY ${PROJECT_DIR}/build/intermediates/cxx/RelWithDebInfo/obj/arm64-v8a
    OUTPUT_NAME "folkpatch-core"
)

# 确保编译顺序：先编译 Rust 库，再编译 C++ 库
if (TARGET buildApd)
    add_dependencies(folkpatch-core buildApd)
endif()
