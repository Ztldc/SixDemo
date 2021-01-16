package com.ztl.sixdemo;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.graphics.Color;
import android.graphics.ImageFormat;
import android.hardware.Camera;
import android.media.MediaPlayer;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.os.Handler;
import android.view.View;
import android.view.WindowManager;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

import videoCodec.*;

public class VideoPlayerSurface extends SurfaceView implements SurfaceHolder.Callback {
    private static String TAG = "VideoDecSurface";
    public static final boolean DEBUG = BuildConfig.DEBUG;

    //定义SurfaceHolder
    private SurfaceHolder holder;
    Context context;
    MediaPlayer player = null;
    //构造函数
    public VideoPlayerSurface(Context context) {
        super(context);
        this.context = context;

        //获取Holder
        holder = getHolder();
        //加入SurfaceHolder.Callback在类中implements
        holder.addCallback(this);
        holder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        //保持屏幕常亮

        //setBackgroundColor(Color.RED);
    }

    @Override
    public void surfaceCreated(SurfaceHolder surfaceHolder) {
        if (DEBUG) Log.i(TAG, "VideoDecSurface" + " surfaceCreated..");

        try {
            if(player == null){
                player = new MediaPlayer();
                //将播放器和SurfaceView关联起来
                player.setDisplay( holder );

                //findViewById(R.id.videoPlayer).setVisibility(View.VISIBLE);
                //只有当播放器准备好了之后才能够播放，所以播放的出发只能在触发了prepare之后
                player.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                    @Override
                    public void onPrepared(MediaPlayer mp) {
                        mp.start();
                        mp.setLooping(true);
                    }
                });

                player.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                    @Override
                    public void onCompletion(MediaPlayer mediaPlayer) {
                        Log.e("oncome","msgOnVideoComplete");
                        //Message message = Message.obtain();
                        //message.what = MessageType.msgOnVideoComplete.ordinal();
                        //if (mHandler != null) mHandler.sendMessage(message);
                    }
                });
            }
            else
                player.reset();

//                    File tempFile = new File(videoFiles.get(videoIndex).getPath());
//                    FileInputStream fis = new FileInputStream(tempFile);
//                    player.setDataSource(fis.getFD());


            AssetFileDescriptor afd = getResources().getAssets().openFd("game.mp4");
            player.setDataSource(afd.getFileDescriptor(),afd.getStartOffset(),afd.getLength());

            //player.setDataSource("/sdcard/Download/game.mp4");
            player.prepare();
        } catch (IOException e) {
            e.printStackTrace();
            player.release();
            player = null;

        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder surfaceHolder, int format, int width, int height) {
        //if (DEBUG) Log.e(TAG, "surfaceChanged..");
        Log.i(TAG, "surfaceChanged: wh" + width + "x" + height);
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder surfaceHolder) {
        if (DEBUG) Log.d(TAG, "surfaceDestroyed");
        player.stop();
        player.release();
        player = null;
    }


}
