package pers.wtt.opengles20.render;

import android.content.Context;
import android.graphics.BitmapFactory;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.GLUtils;
import android.opengl.Matrix;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import pers.wtt.opengles20.R;

/**
 * Created by WT on 2018/4/8.
 */
public class DBitmapGLRender implements GLSurfaceView.Renderer {

    private Context mContext;

    public DBitmapGLRender(Context context) {
        this.mContext = context;
    }

    //顶点着色器代码
    private final String vertex = "" +
            "attribute vec4 vPosition;" +
            "attribute vec2 aCoord;" +
            "uniform mat4 vMatrix;" +
            "varying vec2 vCoord;" +
            "void main(){" +
            "gl_Position=vMatrix*vPosition;"+
            "vCoord=aCoord;" +
            "}";

    //片元着色器代码
    private final String fragment = "" +
            "precision mediump float;" +
            "uniform sampler2D vTexture;" +
            "varying vec2 vCoord;" +
            "void main(){" +
            "gl_FragColor=texture2D(vTexture,vCoord);"+
            "}";

    //顶点坐标
    private final float[] pos = {
            -1.0f,1.0f,
            1.0f,1.0f,
            -1.0f,-1.0f,
            1.0f,-1.0f
    };

    //纹理坐标
    private final float[] coord = {
            1.0f,0.0f,
            0.0f,0.0f,
            1.0f,1.0f,
            0.0f,1.0f,
    };

    //GL程序
    int program;
    //定点坐标Buffer
    FloatBuffer vertexBuffer;
    FloatBuffer coordBuffer;
    float[] mMVPMatrix;

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        //将背景设置为灰色，这里只是设置，并没有立即生效
        GLES20.glClearColor(0.5f,0.5f,0.5f,1.0f);
        //创建一个定点坐标Buffer，一个float为4字节所以这里需要
        ByteBuffer byteBuffer = ByteBuffer.allocateDirect(pos.length*4);
        byteBuffer.order(ByteOrder.nativeOrder());
        vertexBuffer  = byteBuffer.asFloatBuffer();
        vertexBuffer .put(pos);
        vertexBuffer .position(0);

        ByteBuffer cbyteBuffer = ByteBuffer.allocateDirect(coord.length*4);
        cbyteBuffer.order(ByteOrder.nativeOrder());
        coordBuffer  = cbyteBuffer.asFloatBuffer();
        coordBuffer .put(coord);
        coordBuffer .position(0);

        //装载顶点着色器和片元着色器，从source
        int vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, vertex);
        int fragmentShader = loadShader(GLES20.GL_FRAGMENT_SHADER, fragment);
        //创建Opengl程序，获取程序句柄，为了方便onDrawFrame方法使用所以声明为成员变量
        program = GLES20.glCreateProgram();
        //激活着色器
        GLES20.glAttachShader(program, vertexShader);
        GLES20.glAttachShader(program, fragmentShader);
        //链接程序
        GLES20.glLinkProgram(program);
    }

    /**
     * 装载着色器从资源代码，需要检测是否生成成功，暂时不检测
     * @param type 着色器类型
     * @param source 着色器代码源
     * @return 返回着色器句柄
     */
    private int loadShader(int type, String source) {
        int shader = 0;
        shader = GLES20.glCreateShader(type);
        GLES20.glShaderSource(shader, source);
        GLES20.glCompileShader(shader);
        return shader;
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {

        //计算屏幕宽高比
        float ratio = (float)width/height;
        //储存投影矩阵
        float[] mPMatrix = new float[16];
        //储存相机位置矩阵
        float[] mVMatrix = new float[16];
        //最终得到的矩阵
        mMVPMatrix = new float[16];
        //透视矩阵
        //存储生成矩阵元素的float[]类型数组
        //填充起始偏移量
        //near面的left,right,bottom,top
        //near面,far面与视点的距离
        Matrix.orthoM(mPMatrix, 0, -ratio, ratio, -1,1,3, 6);
        //存储生成矩阵元素的float[]类型数组
        //填充起始偏移量
        //摄像机位置X,Y,Z坐标
        //观察目标X,Y,Z坐标
        //up向量在X,Y,Z上的分量,也就是相机上方朝向，upY=1朝向手机上方，upX=1朝向手机右侧，upZ=1朝向与手机屏幕垂直
        Matrix.setLookAtM(mVMatrix, 0, 0,0,6, 0,0,0,0,1,0);
        //以上两个方法只能得到矩阵并不能使其生效
        //下面通过矩阵计算得到最终想要的矩阵
        //存放结果的总变换矩阵
        //结果矩阵偏移量
        //左矩阵
        //左矩阵偏移量
        //右矩阵
        //右矩阵偏移量
        Matrix.multiplyMM(mMVPMatrix, 0,mPMatrix, 0, mVMatrix, 0);

        //当大小改变时重置视区大小
        GLES20.glViewport(0,0, width, height);
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        //清空缓冲区，与  GLES20.glClearColor(0.5f,0.5f,0.5f,1.0f);对应
        GLES20.glClear(GL10.GL_COLOR_BUFFER_BIT|GL10.GL_DEPTH_BUFFER_BIT);

        //使用OpenGL程序
        GLES20.glUseProgram(program);
        int vMatrix = GLES20.glGetUniformLocation(program, "vMatrix");
        GLES20.glUniformMatrix4fv(vMatrix, 1 ,false, mMVPMatrix, 0);
        //获取顶点着色器变量vPosition
        int vPositionHandler = GLES20.glGetAttribLocation(program, "vPosition");
        //允许使用顶点坐标数组
        GLES20.glEnableVertexAttribArray(vPositionHandler);
        //第一个参数顶点属性的索引值
        // 第二个参数顶点属性的组件数量。必须为1、2、3或者4，如position是由3个（x,y,z）组成，而颜色是4个（r,g,b,a））
        // 第三个参数数组中每个组件的数据类型
        // 第四个参数指定当被访问时，固定点数据值是否应该被归一化（GL_TRUE）或者直接转换为固定点值（GL_FALSE）
        // 第五个参数指定连续顶点属性之间的偏移量，这里由于是三个点 每个点4字节（float） 所以就是 3*4
        // 第六个参数前面的顶点坐标数组
        GLES20.glVertexAttribPointer(vPositionHandler, 2, GLES20.GL_FLOAT, false, 8, vertexBuffer);
        int aCoordHandler = GLES20.glGetAttribLocation(program, "aCoord");
        GLES20.glEnableVertexAttribArray(aCoordHandler);
        GLES20.glVertexAttribPointer(aCoordHandler, 2, GLES20.GL_FLOAT, false, 8, coordBuffer);
        int vTexture = GLES20.glGetUniformLocation(program, "vTexture");
        GLES20.glUniform1i(vTexture, 0);
        texture();
        //获取片元着色器变量vColor
        //int vColor = GLES20.glGetUniformLocation(program, "vColor");
        //GLES20.glUniform4fv(vColor, 1, colors, 0);
        //三角形绘制方式
        //顶点的数量
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);
        //禁止使用顶点坐标数组
        GLES20.glDisableVertexAttribArray(vPositionHandler);
    }


    int[] texture = new int[1];
    private void texture(){
        GLES20.glGenTextures(1, texture, 0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, texture[0]);
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
        //设置环绕方向S，截取纹理坐标到[1/2n,1-1/2n]。将导致永远不会与border融合
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S,GLES20.GL_CLAMP_TO_EDGE);
        //设置环绕方向T，截取纹理坐标到[1/2n,1-1/2n]。将导致永远不会与border融合
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T,GLES20.GL_CLAMP_TO_EDGE);
        GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, BitmapFactory.decodeResource(mContext.getResources(), R.mipmap.ic_launcher),0);
    }

}
