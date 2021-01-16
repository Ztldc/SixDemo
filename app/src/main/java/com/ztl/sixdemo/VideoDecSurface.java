package com.ztl.sixdemo;

import android.content.Context;
import android.graphics.Color;
import android.graphics.ImageFormat;
import android.hardware.Camera;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.os.Handler;
import android.view.WindowManager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

import videoCodec.*;

public class VideoDecSurface extends SurfaceView implements SurfaceHolder.Callback {
    private static String TAG = "VideoDecSurface";
    public static final boolean DEBUG = BuildConfig.DEBUG;

    //定义SurfaceHolder
    private SurfaceHolder holder;
    Context context;

    private AvcEncoder avcEncoder;
    private AvcDecode avcDecode;
    int previewW = 0;
    int previewH = 0;
    byte[] h264;
    //构造函数
    public VideoDecSurface(Context context, int prewW, int prewH) {
        super(context);
        this.context = context;

        //获取Holder
        holder = getHolder();
        //加入SurfaceHolder.Callback在类中implements
        holder.addCallback(this);
        holder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        //保持屏幕常亮
        //holder.setKeepScreenOn(true);
        previewW = prewW;
        previewH = prewH;
        h264 = new byte[previewW * previewH * 3 / 2];
        //setBackgroundColor(Color.RED);
    }

    @Override
    public void surfaceCreated(SurfaceHolder surfaceHolder) {
        if (DEBUG) Log.i(TAG, "VideoDecSurface" + " surfaceCreated..");

        avcEncoder = new AvcEncoder(previewW, previewH, 15, 2000000);
        avcDecode = new AvcDecode(previewW, previewH, holder.getSurface());
    }

    @Override
    public void surfaceChanged(SurfaceHolder surfaceHolder, int format, int width, int height) {
        //if (DEBUG) Log.e(TAG, "surfaceChanged..");
        Log.i(TAG, "surfaceChanged: wh" + width + "x" + height);
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder surfaceHolder) {
        if (DEBUG) Log.d(TAG, "surfaceDestroyed");
        stop();
    }

    void stop(){
        if( avcEncoder != null){
            avcEncoder.close();avcEncoder = null;
        }

        if( avcDecode != null){
            avcDecode = null;
        }
    }

    public void feed(byte[] data){
        int ret = avcEncoder.offerEncoder(data, h264);
        //Log.d("Buffer", "ByteByffer data : " + data.length + "\t h264 : " + h264.length+" ret:"+ret);
       // if (ret > 0)
        {

//                saveBytesToFile( "/sdcard/camera_rk3288.h264",  h264,ret);
            avcDecode.decodeH264(h264,ret);

//                ByteBuffer[] byteBuffers = avcDecode1.decodeH264ToData(h264);
//                for (int i = 0; i < byteBuffers.length; i++) {
//                    ByteBuffer buffer = byteBuffers[i];
//                    byte[] b = new byte[buffer.remaining()];
//                    Log.d("Buffer","ByteByffer : " + b.length);
//                }
        }
    }

}
