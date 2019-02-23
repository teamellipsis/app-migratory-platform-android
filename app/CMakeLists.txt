cmake_minimum_required(VERSION 3.4.1)

add_library(
        native-lib
        SHARED
        src/main/cpp/native-lib.cpp)

include_directories( SYSTEM /usr/include/node ) # Path to Node.js include directory

add_library( node-lib
        SHARED
        IMPORTED )

set_target_properties(
        node-lib
        PROPERTIES IMPORTED_LOCATION
        ${CMAKE_SOURCE_DIR}/jniLibs/${ANDROID_ABI}/libnode.so )

find_library(
        log-lib
        log)

target_link_libraries(
        native-lib
        node-lib
        ${log-lib})
