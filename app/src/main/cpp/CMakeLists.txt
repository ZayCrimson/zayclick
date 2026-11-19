cmake_minimum_required(VERSION 3.22.1)
project(zaytouch LANGUAGES CXX)

add_executable(zaytouch uinput_touch.cpp)
set_target_properties(zaytouch PROPERTIES
    CXX_STANDARD 17
    CXX_STANDARD_REQUIRED ON
    POSITION_INDEPENDENT_CODE ON
    RUNTIME_OUTPUT_DIRECTORY "${CMAKE_BINARY_DIR}/zaybin"
)
target_compile_options(zaytouch PRIVATE -Wall -Wextra -Werror=return-type -O2)
target_link_options(zaytouch PRIVATE -pie)
