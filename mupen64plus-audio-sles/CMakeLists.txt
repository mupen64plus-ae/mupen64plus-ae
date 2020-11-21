cmake_minimum_required(VERSION 3.10)

if( CMAKE_BUILD_TYPE STREQUAL "Debug")
    set( BUILD_VARIANT "debug" )
else()
    set( BUILD_VARIANT "release" )
endif()

add_definitions(-DANDROID)

set( SOUNDTOUCH_INCLUDE_DIRS "${ANDROID_LIB_PATH}/soundtouch/include" )

add_library( SOUNDTOUCH SHARED IMPORTED )
set_target_properties( # Specifies the target library.
        SOUNDTOUCH

        # Specifies the parameter you want to define.
        PROPERTIES IMPORTED_LOCATION

        # Provides the path to the library you want to import.
        ${ANDROID_LIB_PATH}/libs/${BUILD_VARIANT}/${ANDROID_ABI}/libsoundtouch.so)

add_library( SOUNDTOUCHFP SHARED IMPORTED )
set_target_properties( # Specifies the target library.
        SOUNDTOUCHFP

        # Specifies the parameter you want to define.
        PROPERTIES IMPORTED_LOCATION

        # Provides the path to the library you want to import.
        ${ANDROID_LIB_PATH}/libs/${BUILD_VARIANT}/${ANDROID_ABI}/libsoundtouch_fp.so)


include_directories(
        ${SOUNDTOUCH_INCLUDE_DIRS}
        ${M64API_INCLUDE_PATH})

# Find the Oboe package
find_package (oboe REQUIRED CONFIG)

add_library (mupen64plus-audio-sles SHARED
        src/main.cpp
        src/osal_dynamiclib_unix.cpp)
target_compile_definitions(mupen64plus-audio-sles PUBLIC -D__SOFTFP__ -DANDROID)

# Specify the libraries which our native library is dependent on, including Oboe
target_link_libraries(mupen64plus-audio-sles log OpenSLES oboe::oboe SOUNDTOUCH)

# Build our own native library
add_library (mupen64plus-audio-sles-fp SHARED
        src/main.cpp
        src/osal_dynamiclib_unix.cpp)
target_compile_definitions(mupen64plus-audio-sles-fp PUBLIC -DFP_ENABLED -DANDROID)

# Specify the libraries which our native library is dependent on, including Oboe
target_link_libraries(mupen64plus-audio-sles-fp log OpenSLES oboe::oboe SOUNDTOUCHFP)