cmake_minimum_required(VERSION 3.22.1)
project("oemcluster_native" LANGUAGES C CXX)

set(CMAKE_CXX_STANDARD 20)
set(CMAKE_CXX_STANDARD_REQUIRED ON)
set(CMAKE_CXX_EXTENSIONS OFF)

if(CMAKE_SYSTEM_PROCESSOR MATCHES "aarch64|arm64")
    add_compile_options(-march=armv8-a+simd -O3 -ffast-math -funroll-loops)
elseif(CMAKE_SYSTEM_PROCESSOR MATCHES "arm")
    add_compile_options(-mfpu=neon -O3 -ffast-math -funroll-loops)
else()
    add_compile_options(-O3 -ffast-math -funroll-loops)
endif()

add_library(
    oemcluster_native
    SHARED
    math/Matrix3x3.cpp
    kalman/MountCalibrator.cpp
    kalman/StrapdownINS15StateEKF.cpp
    acoustic/AcousticFftRpmDetector.cpp
    physics/VirtualEcuPhysicsEngine.cpp
    jni/TelemetryJniBridge.cpp
)

target_include_directories(
    oemcluster_native
    PRIVATE
    ${CMAKE_CURRENT_SOURCE_DIR}
    ${CMAKE_CURRENT_SOURCE_DIR}/math
    ${CMAKE_CURRENT_SOURCE_DIR}/kalman
    ${CMAKE_CURRENT_SOURCE_DIR}/acoustic
    ${CMAKE_CURRENT_SOURCE_DIR}/physics
    ${CMAKE_CURRENT_SOURCE_DIR}/jni
)

find_library(log-lib log)
find_library(android-lib android)
find_library(gles3-lib GLESv3)
find_library(aaudio-lib aaudio)

target_link_libraries(
    oemcluster_native
    ${log-lib}
    ${android-lib}
    ${gles3-lib}
    ${aaudio-lib}
)

