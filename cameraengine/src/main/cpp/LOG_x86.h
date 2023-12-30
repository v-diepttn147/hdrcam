//
// Created by JK on 9/5/20.
//

#ifndef V_CAMERA2GL3_LOG_X86_H
#define V_CAMERA2GL3_LOG_X86_H

#endif //V_CAMERA2GL3_LOG_X86_H

#include <string>
#include <stdio.h>

void LOGI(std::string format, ...)
{
    va_list args;
    va_start(args, format);
    printf("INFO: ");
    vfprintf(stdout, format.c_str(), args);
    printf("\n");
    va_end(args);
}

void LOGE(std::string format, ...)
{
    va_list args;
    va_start(args, format);
    printf("ERROR: ");
    vfprintf(stdout, format.c_str(), args);
    printf("\n");
    va_end(args);
}

