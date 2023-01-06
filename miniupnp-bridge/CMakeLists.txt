cmake_minimum_required(VERSION 3.22)

project( Miniupnp-bridge )

if( CMAKE_BUILD_TYPE STREQUAL "Debug")
    set( BUILD_VARIANT "debug" )
else()
    set( BUILD_VARIANT "release" )
endif()

add_definitions(-DANDROID)

set( MINIUPNP_INCLUDE_DIRS "${ANDROID_LIB_PATH}/miniupnp" )

add_library( MINIUPNP SHARED IMPORTED )
set_target_properties( # Specifies the target library.
        MINIUPNP

        # Specifies the parameter you want to define.
        PROPERTIES IMPORTED_LOCATION

        # Provides the path to the library you want to import.
        ${ANDROID_LIB_PATH}/libs/${BUILD_VARIANT}/${ANDROID_ABI}/libminiupnp-build.so)
include_directories(${MINIUPNP_INCLUDE_DIRS})

set( LIBNATPMP_INCLUDE_DIRS "${ANDROID_LIB_PATH}/libnatpmp" )

add_library( LIBNATPMP SHARED IMPORTED )
set_target_properties( # Specifies the target library.
        LIBNATPMP

        # Specifies the parameter you want to define.
        PROPERTIES IMPORTED_LOCATION

        # Provides the path to the library you want to import.
        ${ANDROID_LIB_PATH}/libs/${BUILD_VARIANT}/${ANDROID_ABI}/libnatpmp.so)

include_directories(${ANDROID_LIB_PATH})

SET(CMAKE_CXX_FLAGS  "${CMAKE_CXX_FLAGS} -O3 -DWITH_UPNP")
set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED YES)

add_library (miniupnp-bridge SHARED
        src/PortManager.cpp
        src/PortManagerNatPmp.cpp)

target_compile_definitions(miniupnp-bridge PUBLIC -DANDROID)

# Specify the libraries which our native library is dependent on, including Oboe
target_link_libraries(miniupnp-bridge log MINIUPNP LIBNATPMP)
