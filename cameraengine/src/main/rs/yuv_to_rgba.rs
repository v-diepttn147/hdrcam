#pragma version(1)
#pragma rs java_package_name(com.hdrcam.camera);
#pragma rs_fp_relax


uint uvRowStride;
uint y_len;
uint u_len;

rs_allocation ypsIn;
 // The LaunchOptions ensure that the Kernel does not enter the padding  zone of Y, so yRowStride can be ignored WITHIN the Kernel.


uchar4 __attribute__((kernel)) nv12_doConvert_float_1(uint32_t x, uint32_t y) {

    uint uIndex= y_len + uvRowStride*(y/2) + 2*(x/2) ;

    // get the y,u,v values
    uchar yps = rsGetElementAt_uchar(ypsIn, x, y);
    uchar u= rsGetElementAt_uchar(ypsIn, uIndex);
    uchar v= rsGetElementAt_uchar(ypsIn, uIndex+1);

    uchar4 argb;
    argb.r = clamp(yps + v * 1436 / 1024 - 179,0,255);
    argb.g = clamp(yps - u * 46549 / 131072 -v * 93604 / 131072 + 135,0,255);
    argb.b = clamp(yps + u * 1814 / 1024 - 227,0,255);
    argb.a = 255;
    return argb;
}


uchar4 __attribute__((kernel)) nv12_doConvert_int(uint32_t x, uint32_t y) {

    uint uIndex= y_len + uvRowStride*(y/2) + 2*(x/2) ;

    // get the y,u,v values
    uchar yps = rsGetElementAt_uchar(ypsIn, x, y);
    uchar u= rsGetElementAt_uchar(ypsIn, uIndex);
    uchar v= rsGetElementAt_uchar(ypsIn, uIndex+1);

    uchar4 argb;
    int c = 298 * (yps - 16);
    int d = u - 128;
    int e = v - 128;

    int r = (c + 409*e + 128) >> 8;
    int g = (c - 100*d - 208*e + 128) >> 8 ;
    int b = (c + 516*d + 128) >> 8;
    argb.r = (uchar)clamp(r,0,255);
    argb.g = (uchar)clamp(g,0,255);
    argb.b = (uchar)clamp(b,0,255);
    argb.a = 255;
    return argb;
}
uchar4 __attribute__((kernel)) nv12_doConvert_opencv(uint32_t x, uint32_t y) {

    uint uIndex = y_len + uvRowStride * (y / 2) + 2 * (x / 2);

    // get the y,u,v values
    uchar yps = rsGetElementAt_uchar(ypsIn, x, y);
    uchar u = rsGetElementAt_uchar(ypsIn, uIndex);
    uchar v = rsGetElementAt_uchar(ypsIn, uIndex + 1);

    uchar4 argb;

    int r = yps + 1.403f * v - 179.584f;
    int g = yps - 0.714f * u - 0.344f * v + 135.424f;
    int b = yps + 1.773f * u - 226.944f;
    argb.r = (uchar)clamp(r,0,255);
    argb.g = (uchar)clamp(g,0,255);
    argb.b = (uchar)clamp(b,0,255);
    argb.a = 255;

    return argb;
}

uchar4 __attribute__((kernel)) nv12_doConvert_opencv_original(uint32_t x, uint32_t y) {
    //https://docs.opencv.org/4.2.0/de/d25/imgproc_color_conversions.html
    uint uIndex = y_len + uvRowStride * (y / 2) + 2 * (x / 2);

    // get the y,u,v values
    uchar yps = rsGetElementAt_uchar(ypsIn, x, y);
    uchar u = rsGetElementAt_uchar(ypsIn, uIndex); //Cb
    uchar v = rsGetElementAt_uchar(ypsIn, uIndex + 1);//Cr

    uchar4 argb;

    int r = round(yps + 1.403f * (v - 128.0f));
    int g = round(yps - 0.714f * (v - 128.0f) - 0.344f * (u - 128.0f));
    int b = round(yps + 1.773f * (u - 128.0f));
    argb.r = (uchar)clamp(r,0,255);
    argb.g = (uchar)clamp(g,0,255);
    argb.b = (uchar)clamp(b,0,255);
    argb.a = 255;

    return argb;
}

uchar4 __attribute__((kernel)) nv12_doConvert_android_wiki(uint32_t x, uint32_t y) {
     //https://en.wikipedia.org/wiki/YUV
    uint uIndex = y_len + uvRowStride * (y / 2) + 2 * (x / 2);

    // get the y,u,v values
    uchar yps = rsGetElementAt_uchar(ypsIn, x, y);
    uchar u = rsGetElementAt_uchar(ypsIn, uIndex);
    uchar v = rsGetElementAt_uchar(ypsIn, uIndex + 1);

    uchar4 argb;

    u = u - 128;
    v = v - 128;

    int r = yps + 1.370705f * v;
    int g = yps - 0.698001f * v - 0.337633f * u;
    int b = yps + 1.732446f * u;
    argb.r = (uchar)clamp(r,0,255);
    argb.g = (uchar)clamp(g,0,255);
    argb.b = (uchar)clamp(b,0,255);
    argb.a = 255;

    return argb;
}

uchar4 __attribute__((kernel)) nv21_doConvert_float_1(uint32_t x, uint32_t y) {

    uint uIndex= y_len + uvRowStride*(y/2) + 2*(x/2) ;

    // get the y,u,v values
    uchar yps = rsGetElementAt_uchar(ypsIn, x, y);
    uchar v= rsGetElementAt_uchar(ypsIn, uIndex);
    uchar u= rsGetElementAt_uchar(ypsIn, uIndex+1);

    uchar4 argb;
    argb.r = clamp(yps + v * 1436 / 1024 - 179,0,255);
    argb.g = clamp(yps - u * 46549 / 131072 -v * 93604 / 131072 + 135,0,255);
    argb.b = clamp(yps + u * 1814 / 1024 - 227,0,255);
    argb.a = 255;
    return argb;
}
