#pragma version(1)
#pragma rs java_package_name(com.hdrcam.camera);
#pragma  rs_fp_relaxed

//uint width ;
//rs_allocation rgbaIn;
//rs_allocation y_out;
rs_allocation u_out;
rs_allocation v_out;
uint uvRowStride;

uchar __attribute__((kernel)) convert(uchar4 in, uint32_t x, uint32_t y) {
    //            Y = ((66 * R + 129 * G + 25 * B + 128) >> 8) + 16;
    //            U = ((-38 * R - 74 * G + 112 * B + 128) >> 8) + 128;
    //            V = ((112 * R - 94 * G - 18 * B + 128) >> 8) + 128;
    //int index = x + width * y;
    //uchar4 in = rsGetElementAt_uchar4(rgbaIn, index);
    uchar y_value = ((66 * in.r + 129 * in.g + 25 * in.b + 128) >> 8) + 16;
    uchar u_value = ((-38 * in.r - 74 * in.g + 112 * in.b + 128) >> 8) + 128;
    uchar v_value = ((112 * in.r - 94 * in.g - 18 * in.b + 128) >> 8) + 128;

    if (x%2==0 && y%2==0) {
        uint uvIndex=  x/2 + uvRowStride*(y/2);
        rsSetElementAt_uchar(u_out, u_value, uvIndex);
        rsSetElementAt_uchar(v_out, v_value, uvIndex);
    }

    return y_value;

}
