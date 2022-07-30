cmake_minimum_required(VERSION 2.8.12)

if(POLICY CMP0048)
	cmake_policy(SET CMP0048 NEW)
endif()

project(natpmp)

set (NATPMP_SOURCES
        natpmp.c
        getgateway.c
)

if (WIN32)
    set (NATPMP_SOURCES ${NATPMP_SOURCES} wingettimeofday.c)
endif (WIN32)

# Library itself
add_library(natpmp STATIC ${NATPMP_SOURCES})
target_include_directories(natpmp PUBLIC ${CMAKE_CURRENT_LIST_DIR})
target_compile_definitions(natpmp PRIVATE -DENABLE_STRNATPMPERR)

if (WIN32)
	target_link_libraries(natpmp PUBLIC ws2_32 Iphlpapi)
	target_compile_definitions(natpmp PUBLIC -DNATPMP_STATICLIB)
endif (WIN32)

# Executables
add_executable(natpmpc natpmpc.c)
target_link_libraries(natpmpc natpmp)

add_executable(testgetgateway
	testgetgateway.c
	getgateway.c)
target_link_libraries(testgetgateway natpmp)
