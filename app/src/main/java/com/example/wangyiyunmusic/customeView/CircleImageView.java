package com.example.wangyiyunmusic.customeView;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Shader;
import android.graphics.drawable.BitmapDrawable;
import android.util.AttributeSet;

import androidx.appcompat.widget.AppCompatImageView;

public class CircleImageView extends AppCompatImageView {

    //画笔
    private Paint mPaint;
    private Matrix mMatrix;
    //圆形图片的半径
    private int mRadius;

    public CircleImageView(Context context) {
        super(context);
    }
    public CircleImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }
    public CircleImageView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        //Log.d(TAG, "onMeasure: Width = "+getMeasuredWidth()+",Height"+getMeasuredHeight());
        //由于是圆形，宽高应保持一致
        int size = Math.min(getMeasuredWidth(), getMeasuredHeight());
        //Log.d("CircleImageView", "onMeasure: size = "+size);
        mRadius = size / 2;
        setMeasuredDimension(size, size);//测量高度和宽度
    }
    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        //Log.d(TAG, "onAttachedToWindow: ");对象初始化
        if (getDrawable() != null) {
            mPaint = new Paint();
            mMatrix = new Matrix();//着色器,shader的变换矩阵，我们这里主要用于放大或者缩小
        }
    }
    @Override
    protected void onDraw(Canvas canvas) {
        if (null == getDrawable()) return;
        mPaint = new Paint();
        mMatrix = new Matrix();
        mPaint.setAntiAlias(true);//设置抗锯齿
        Bitmap bitmap = ((BitmapDrawable) getDrawable()).getBitmap();
        //渲染图像，使用图像为绘制图形着色
        BitmapShader bitmapShader = new BitmapShader(bitmap, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP);
        //计算缩放比例
        float scale = (mRadius * 2.0f) / Math.min(bitmap.getHeight(), bitmap.getWidth());
        mMatrix.setScale(scale, scale);
        //添加着色器
        bitmapShader.setLocalMatrix(mMatrix);
        mPaint.setShader(bitmapShader);
        //画圆形，指定好坐标，半径，画笔
        canvas.drawCircle(mRadius, mRadius, mRadius, mPaint);
    }
    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        //释放资源
        if (mPaint!=null) {
            mPaint.reset();
            mPaint = null;
        }
        if (mMatrix != null) {
            mMatrix.reset();
            mMatrix = null;
        }
    }
}