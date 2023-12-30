package com.hdrcam.camera.utils.gles.program;

import android.opengl.GLES20;

import com.hdrcam.camera.utils.gles.GlUtil;
import com.hdrcam.camera.utils.gles.drawable.Drawable2d;
import com.hdrcam.camera.utils.gles.drawable.Drawable2dFull;

public class TextureYUV420Program extends Program{
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
        /*    "uniform mat4 uMVPMatrix;\n" +
                    "uniform mat4 uTexMatrix;\n" +
                    "attribute vec4 aPosition;\n" +
                    "attribute vec4 aTextureCoord;\n" +
                    "varying vec2 vTextureCoord;\n" +
                    "void main() {\n" +
                    "    gl_Position = uMVPMatrix * aPosition;\n" +
                    "    vTextureCoord = (uTexMatrix * aTextureCoord).xy;\n" +
                    "}\n";
*/

    private static final String FRAGMENT_SHADER = "precision highp float;//精度 为float\n" +
            "varying vec2 v_texPo;//纹理位置  接收于vertex_shader\n" +
            "uniform sampler2D sampler_y;//纹理y\n" +
            "uniform sampler2D sampler_u;//纹理u\n" +
            "uniform sampler2D sampler_v;//纹理v\n" +
            "\n" +
            "void main() {\n" +
            "    //yuv420->rgb\n" +
            "    float y,u,v;\n" +
            "    y = texture2D(sampler_y,v_texPo).r;\n" +
            "    u = texture2D(sampler_u,v_texPo).r- 0.5;\n" +
            "    v = texture2D(sampler_v,v_texPo).r- 0.5;\n" +
            "    vec3 rgb;\n" +
            "    rgb.r = y + 1.403 * v;\n" +
            "    rgb.g = y - 0.344 * u - 0.714 * v;\n" +
            "    rgb.b = y + 1.770 * u;\n" +
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

    //shader  yuv变量
    private int sampler_y;
    private int sampler_u;
    private int sampler_v;



    public TextureYUV420Program(String vertexShader, String fragmentShader, int textureTarget) {
        super();
        mTextureTarget = textureTarget;
        mProgramHandle = GlUtil.createProgram(vertexShader, fragmentShader);
        mDrawable2d = new Drawable2dFull();
        glGetLocations();
    }

    public void drawFrame(int[] textureId_yuv, float[] texMatrix, float[] mvpMatrix) {
        GlUtil.checkGlError("draw start");

        // Select the program.
        GLES20.glUseProgram(mProgramHandle);
        GlUtil.checkGlError("glUseProgram");

        // Set the texture.
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(mTextureTarget, textureId_yuv[0]);

        GLES20.glActiveTexture(GLES20.GL_TEXTURE1);
        GLES20.glBindTexture(mTextureTarget, textureId_yuv[1]);

        GLES20.glActiveTexture(GLES20.GL_TEXTURE2);
        GLES20.glBindTexture(mTextureTarget, textureId_yuv[2]);

        GLES20.glUniform1i(sampler_y, 0);
        GLES20.glUniform1i(sampler_u, 1);
        GLES20.glUniform1i(sampler_v, 2);

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
        //获取yuv字段

        muMVPMatrixLoc = GLES20.glGetUniformLocation(mProgramHandle, "uMVPMatrix");
        GlUtil.checkLocation(muMVPMatrixLoc, "uMVPMatrix");
        muTexMatrixLoc = GLES20.glGetUniformLocation(mProgramHandle, "uTexMatrix");
        GlUtil.checkLocation(muTexMatrixLoc, "uTexMatrix");

        sampler_y = GLES20.glGetUniformLocation(mProgramHandle, "sampler_y");
        sampler_u = GLES20.glGetUniformLocation(mProgramHandle, "sampler_u");
        sampler_v = GLES20.glGetUniformLocation(mProgramHandle, "sampler_v");
    }

    public static TextureYUV420Program createTexture2D() {
        return new TextureYUV420Program(VERTEX_SHADER, FRAGMENT_SHADER, GLES20.GL_TEXTURE_2D);
    }

    @Override
    protected Drawable2d createDrawable2d() {
        return new Drawable2dFull();
    }

}
