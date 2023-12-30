#pragma version(1)
  #pragma rs java_package_name(com.hdrcam.camera);
  #pragma  rs_fp_relaxed

  int32_t width;
  int32_t height;

  //uint picWidth;
  uint uvRowStride ;
  rs_allocation ypsIn,uIn,vIn;

 // The LaunchOptions ensure that the Kernel does not enter the padding  zone of Y, so yRowStride can be ignored WITHIN the Kernel.
 uchar4 __attribute__((kernel)) doConvert(uint32_t x, uint32_t y) {

 // index for accessing the uIn's and vIn's
    uint uvIndex=  x/2 + uvRowStride*(y/2);

    // get the y,u,v values
    uchar yps = rsGetElementAt_uchar(ypsIn, x, y);
    uchar u= rsGetElementAt_uchar(uIn, uvIndex);
    uchar v= rsGetElementAt_uchar(vIn, uvIndex);

    int r = (int) (yps + (1.370705 * (v-128)));
    int g = (int) (yps - (0.698001 * (v-128)) - (0.337633 * (u-128)));
    int b = (int)(yps + (1.732446 * (u-128)));

    r = r>255? 255 : r<0 ? 0 : r;
    g = g>255? 255 : g<0 ? 0 : g;
    b = b>255? 255 : b<0 ? 0 : b;


    uchar4 res4;
    res4.r = (uchar)r;
    res4.g = (uchar)g;
    res4.b = (uchar)b;
    res4.a = 0xFF;

return res4;
}