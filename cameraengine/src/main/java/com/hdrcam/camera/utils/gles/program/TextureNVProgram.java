package com.hdrcam.camera.utils.gles.program;

import android.opengl.GLES20;

import com.hdrcam.camera.utils.ExImageFormat;
import com.hdrcam.camera.utils.gles.GlUtil;
import com.hdrcam.camera.utils.gles.drawable.Drawable2d;
import com.hdrcam.camera.utils.gles.drawable.Drawable2dFull;

public class TextureNVProgram extends Program {
    private static final String VERTEX_SHADER =
            "uniform mat4 uMVPMatrix;\n"
                    + "uniform mat4 uTexMatrix;\n"
                    + "attribute vec4 av_Position;\n"
                    + "attribute vec4 af_Position;\n"
                    + "varying vec2 v_texPo;\n"
                    + "void main() {\n"
                    + "v_texPo =  (uTexMatrix * af_Position).xy;\n"
                    + "gl_Position = uMVPMatrix * av_Position;\n"
                    + "}";


    private static final String FRAGMENT_SHADER_NV12 =
            "precision mediump float;\n" +
                    "varying vec2 v_texPo;\n" +
                    "uniform sampler2D sampler_y;//\n" +
                    "uniform sampler2D sampler_uv;//\n" +
                    "uniform float brightnessAddedValue;"+
                    "uniform float contrastValue;"+
                    "void main() {\n" +

                    "    float y,u,v;\n" +
                    "    y = texture2D(sampler_y,v_texPo).r + brightnessAddedValue;\n" +
                    "    u = texture2D(sampler_uv,v_texPo).r- 0.5;\n" +
                    "    v = texture2D(sampler_uv,v_texPo).a- 0.5;\n" +
                    "    y = y*(1.0+contrastValue);\n" +
                    "    vec3 rgb;\n" +
                    "    rgb.r = y + 1.13983 * v;\n" +
                    "    rgb.g = y - 0.39465 * u - 0.58060 * v;\n" +
                    "    rgb.b = y + 2.03211 * u;\n" +

                    "\n" +
                    "    gl_FragColor=vec4(rgb,1);\n" +
                    "}";

    private static final String FRAGMENT_SHADER_NV21 =
            "precision mediump float;\n" +
                    "varying vec2 v_texPo;\n" +
                    "uniform sampler2D sampler_y;//\n" +
                    "uniform sampler2D sampler_uv;//\n" +
                    "uniform float brightnessAddedValue;"+
                    "uniform float contrastValue;"+
                    "void main() {\n" +

                    "    float y,u,v;\n" +
                    "    y = texture2D(sampler_y,v_texPo).r + brightnessAddedValue;\n" +
                    "    v = texture2D(sampler_uv,v_texPo).r- 0.5;\n" +
                    "    u = texture2D(sampler_uv,v_texPo).a- 0.5;\n" +
                    "    y = y*(1.0+contrastValue);\n" +
                    "    vec3 rgb;\n" +
                    "    rgb.r = y + 1.13983 * v;\n" +
                    "    rgb.g = y - 0.39465 * u - 0.58060 * v;\n" +
                    "    rgb.b = y + 2.03211 * u;\n" +

                    "\n" +
                    "    gl_FragColor=vec4(rgb,1);\n" +
                    "}";

    private static final String FRAGMENT_SHADER_NV21_intel_YCrCb =
//    R' = 1.164*(Y'-16) + 1.596*(Cr'-128)
//    G' = 1.164*(Y'-16) - 0.813*(Cr'-128) - 0.392*(Cb'-128)
//    B' = 1.164*(Y'-16) + 2.017*(Cb'-128)
            "precision mediump float;\n" +
                    "varying vec2 v_texPo;\n" +
                    "uniform sampler2D sampler_y;//\n" +
                    "uniform sampler2D sampler_uv;//\n" +

                    "void main() {\n" +

                    "    float y,u,v;\n" +
                    "    y = texture2D(sampler_y,v_texPo).r * 1.164 - 0.07275 ;\n" +
                    "    u = texture2D(sampler_uv,v_texPo).r - 0.5;\n" +
                    "    v = texture2D(sampler_uv,v_texPo).a - 0.5;\n" +
                    "    vec3 rgb;\n" +
                    "    rgb.r =y + 1.596*(u);\n" +
                    "    rgb.g = y- 0.813*(u) - 0.392*(v);\n" +
                    "    rgb.b = y+ 2.017*(v);\n" +

                    "\n" +
                    "    gl_FragColor=vec4(rgb,1);\n" +
                    "}";
    private int mTextureTarget;
    // Handles to the GL program and various components of it.
    private int mProgramHandle;
    private Drawable2d mDrawable2d;

    private int maPositionLoc;
    //纹理位置
    private int maTextureCoordLoc;
    private int muMVPMatrixLoc;
    private int muTexMatrixLoc;

    private int maBrightnessValueLoc;
    private int maContrastValueLoc;
    //shader  yuv变量
    private int sampler_y;
    private int sampler_uv;
    //  private int sampler_v;


    private TextureNVProgram(String vertexShader, String fragmentShader, int textureTarget) {
        super();
        mTextureTarget = textureTarget;
        mProgramHandle = GlUtil.createProgram(vertexShader, fragmentShader);
        mDrawable2d = new Drawable2dFull();
        glGetLocations();
    }

    public void drawFrame(int[] textureId_yuv, float[] texMatrix, float[] mvpMatrix) {
        drawFrame(textureId_yuv, texMatrix, mvpMatrix, 0.0f, 0.5f);
    }

    public void drawFrame(int[] textureId_yuv, float[] texMatrix, float[] mvpMatrix,
                          float brightnessAddedValue, float contrastValue) {
        GlUtil.checkGlError("draw start");

        // Select the program.
        GLES20.glUseProgram(mProgramHandle);
        GlUtil.checkGlError("glUseProgram");

        // Set the texture.
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(mTextureTarget, textureId_yuv[0]);

        GLES20.glActiveTexture(GLES20.GL_TEXTURE1);
        GLES20.glBindTexture(mTextureTarget, textureId_yuv[1]);

        GLES20.glUniform1i(sampler_y, 0);
        GLES20.glUniform1i(sampler_uv, 1);

        GLES20.glUniform1f(maBrightnessValueLoc, brightnessAddedValue);
        GLES20.glUniform1f(maContrastValueLoc, contrastValue);
        //   GLES20.glUniform1i(sampler_v, 2);

        // Copy the model / view / projection matrix over.
        GLES20.glUniformMatrix4fv(muMVPMatrixLoc, 1, false, mvpMatrix, 0);
        GlUtil.checkGlError("glUniformMatrix4fv");

        // Copy the texture transformation matrix over.
        GLES20.glUniformMatrix4fv(muTexMatrixLoc, 1, false, texMatrix, 0);
        GlUtil.checkGlError("glUniformMatrix4fv");

        // Enable the "aPosition" vertex attribute.
        GLES20.glEnableVertexAttribArray(maPositionLoc);
        GlUtil.checkGlError("glEnableVertexAttribArray");

        // Connect vertexBuffer to "aPosition".
        GLES20.glVertexAttribPointer(maPositionLoc, Drawable2d.COORDS_PER_VERTEX,
                GLES20.GL_FLOAT, false, Drawable2d.VERTEX_STRIDE, mDrawable2d.getVertexArray());
        GlUtil.checkGlError("glVertexAttribPointer");

        // Enable the "aTextureCoord" vertex attribute.
        GLES20.glEnableVertexAttribArray(maTextureCoordLoc);
        GlUtil.checkGlError("glEnableVertexAttribArray");

        // Connect texBuffer to "aTextureCoord".
        GLES20.glVertexAttribPointer(maTextureCoordLoc, 2,
                GLES20.GL_FLOAT, false, Drawable2d.TEX_COORD_STRIDE, mDrawable2d.getTexCoordArray());
        GlUtil.checkGlError("glVertexAttribPointer");

        // Draw the rect.
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, mDrawable2d.getVertexCount());
        GlUtil.checkGlError("glDrawArrays");

        // Done -- disable vertex array, texture, and program.
        GLES20.glDisableVertexAttribArray(maPositionLoc);
        GLES20.glDisableVertexAttribArray(maTextureCoordLoc);
        GLES20.glBindTexture(mTextureTarget, 0);
        GLES20.glUseProgram(0);
    }

    /**
     * Releases the program.
     * <p>
     * The appropriate EGL context must be current (i.e. the one that was used to create
     * the program).
     */
    public void release() {
        GLES20.glDeleteProgram(mProgramHandle);
        mProgramHandle = -1;
    }

    protected void glGetLocations() {
 /*       maPositionLoc = GLES20.glGetAttribLocation(mProgramHandle, "aPosition");
        GlUtil.checkLocation(maPositionLoc, "aPosition");
        maTextureCoordLoc = GLES20.glGetAttribLocation(mProgramHandle, "aTextureCoord");
        GlUtil.checkLocation(maTextureCoordLoc, "aTextureCoord");
        muMVPMatrixLoc = GLES20.glGetUniformLocation(mProgramHandle, "uMVPMatrix");
        GlUtil.checkLocation(muMVPMatrixLoc, "uMVPMatrix");
        muTexMatrixLoc = GLES20.glGetUniformLocation(mProgramHandle, "uTexMatrix");
        GlUtil.checkLocation(muTexMatrixLoc, "uTexMatrix");*/

        maPositionLoc = GLES20.glGetAttribLocation(mProgramHandle, "av_Position");
        //获取纹理坐标字段
        maTextureCoordLoc = GLES20.glGetAttribLocation(mProgramHandle, "af_Position");
        maBrightnessValueLoc = GLES20.glGetUniformLocation(mProgramHandle,
                "brightnessAddedValue");

        maContrastValueLoc  = GLES20.glGetUniformLocation(mProgramHandle, "contrastValue");
        //获取yuv字段

        muMVPMatrixLoc = GLES20.glGetUniformLocation(mProgramHandle, "uMVPMatrix");
        GlUtil.checkLocation(muMVPMatrixLoc, "uMVPMatrix");
        muTexMatrixLoc = GLES20.glGetUniformLocation(mProgramHandle, "uTexMatrix");
        GlUtil.checkLocation(muTexMatrixLoc, "uTexMatrix");

        sampler_y = GLES20.glGetUniformLocation(mProgramHandle, "sampler_y");
        sampler_uv = GLES20.glGetUniformLocation(mProgramHandle, "sampler_uv");
        // sampler_v = GLES20.glGetUniformLocation(mProgramHandle, "sampler_v");
    }

    public static TextureNVProgram createTexture2D(int nvFormat) {
        switch (nvFormat){
            case ExImageFormat.NV12:
                return new TextureNVProgram(VERTEX_SHADER, FRAGMENT_SHADER_NV12, GLES20.GL_TEXTURE_2D);
            case ExImageFormat.NV21:
                return new TextureNVProgram(VERTEX_SHADER, FRAGMENT_SHADER_NV21,
                        GLES20.GL_TEXTURE_2D);
            default:
                return null;
        }

    }

    @Override
    protected Drawable2d createDrawable2d() {
        return new Drawable2dFull();
    }

}
