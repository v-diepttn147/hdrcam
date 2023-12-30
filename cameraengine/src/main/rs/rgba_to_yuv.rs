#pragma version(1)
#pragma rs java_package_name(com.hdrcam.camera);
#pragma  rs_fp_relaxed

//uint width ;
//rs_allocation rgbaIn;
//rs_allocation y_out;
rs_allocation yuv_out;
//rs_allocation v_out;
uint width;
uint y_len;

void  __attribute__((kernel)) rgba_to_nv21_convert_full_swing(uchar4 in, uint32_t x, uint32_t y) {
    uint yIndex = x + width * y;
    uchar y_value = clamp((((77 * in.r + 150 * in.g + 29 * in.b + 128) >> 8)),
    0,255);
    uchar u_value = clamp((((-43 * in.r - 84 * in.g + 127 * in.b + 128) >> 8) + 128),0,255);
    uchar v_value = clamp((((127 * in.r - 106 * in.g - 21 * in.b + 128) >> 8) + 128),0,255);


    rsSetElementAt_uchar(yuv_out, y_value, yIndex);

    if (x%2==0 && y%2==0) {
        uint uvIndex=  y_len + (x) + width*(y/2) ;
        rsSetElementAt_uchar(yuv_out, v_value, uvIndex-2);
        rsSetElementAt_uchar(yuv_out, u_value, uvIndex-1);
    }
}

void  __attribute__((kernel)) rgba_to_nv12_convert_full_swing(uchar4 in, uint32_t x, uint32_t y) {
    uint yIndex = x + width * y;
    uchar y_value = clamp((((77 * in.r + 150 * in.g + 29 * in.b + 128) >> 8)),
    0,255);
    uchar u_value = clamp((((-43 * in.r - 84 * in.g + 127 * in.b + 128) >> 8) + 128),0,255);
    uchar v_value = clamp((((127 * in.r - 106 * in.g - 21 * in.b + 128) >> 8) + 128),0,255);


    rsSetElementAt_uchar(yuv_out, y_value, yIndex);

    if (x%2==0 && y%2==0) {
        uint uvIndex=  y_len + (x) + width*(y/2) ;
        rsSetElementAt_uchar(yuv_out, v_value, uvIndex-1);
        rsSetElementAt_uchar(yuv_out, u_value, uvIndex-2);
    }
}
