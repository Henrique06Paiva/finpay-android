cmake_minimum_required(VERSION 3.22.1)

project("finpay")

add_library(${CMAKE_PROJECT_NAME} SHARED
        native-lib.cpp
        core/PayrollCalculator.cpp
        core/DateEngine.cpp
        core/DebtSimulator.cpp
        core/YieldCalculator.cpp)

target_include_directories(${CMAKE_PROJECT_NAME} PRIVATE
        ${CMAKE_CURRENT_SOURCE_DIR}
        ${CMAKE_CURRENT_SOURCE_DIR}/core)

target_link_libraries(${CMAKE_PROJECT_NAME}
        android
        log)
