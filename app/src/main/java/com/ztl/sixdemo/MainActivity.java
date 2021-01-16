package com.ztl.sixdemo;

import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.ConstraintSet;
import androidx.core.app.NavUtils;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Color;
import android.hardware.Camera;
import android.os.Bundle;
import android.os.Handler;
import android.text.Layout;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import ZtlApi.ZtlManager;

public class MainActivity extends AppCompatActivity {

    boolean isBarOpenst = false;

    int screenWidth;
    int screenHeight;
    boolean isLandscape = false;//是否横屏

    RelativeLayout la1bottom = null;

    class CameraInfo {
        public DCCameraSurface preview;//实际的预览控件
        public TextView txt_info;//信息控件用于分辨率和帧率更新
        public ImageView imgClose;//打开关闭控件。用于打开关闭摄像头

        Button lastbtnResolutions;
        Button lastbtnDegree;
        int toApplyW = -1;
        int toApplyH = -1;
        int toApplyDegree = -1;
    }

    ArrayList<CameraInfo> cameraInfos = new ArrayList<>();
    VideoDecSurface vds3;//传说中的两路视频解码显示
    VideoDecSurface vds4;//传说中的两路视频解码显示
    ConstraintLayout clroot = null;
    int cameraCount = 0;
    Thread thUpdateFPS = null;
    int v1Index = 3;
    int v2Index = 4;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        supportRequestWindowFeature(Window.FEATURE_NO_TITLE);//去掉标题栏
        setContentView(R.layout.activity_main);

        if (BuildConfig.DEBUG) Log.e("MainActivity", "onCreate");

        ZtlManager.GetInstance().setContext(this);
        isBarOpenst = ZtlManager.GetInstance().isSystemBarOpen();
        ZtlManager.GetInstance().openSystemBar(false);

        la1bottom = findViewById(R.id.la1bottom);

        //获取屏幕参数。横屏 竖屏？
        WindowManager wm = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
        DisplayMetrics metrics = new DisplayMetrics();
        wm.getDefaultDisplay().getRealMetrics(metrics);

        screenWidth = metrics.widthPixels;
        screenHeight = metrics.heightPixels;
        Log.e("屏幕尺寸", "" + screenWidth + "x" + screenHeight);
        if (screenWidth > screenHeight)
            isLandscape = true;

        clroot = (ConstraintLayout) findViewById(R.id.root_t);
        if (BuildConfig.DEBUG) {//没有5个摄像头
            v1Index = 0;
            v2Index = 1;
        }
    }

    @Override
    protected void onStop() {
        super.onStop();

        closeCameras();

        //恢复成原样
        ZtlManager.GetInstance().openSystemBar(isBarOpenst);
    }

    @Override
    protected void onResume() {
        super.onResume();

        openCameras();
    }

    //获取摄像机个数 并动态生成预览和相关界面
    public void openCameras() {
        cameraCount = 5;//Camera.getNumberOfCameras();
        Log.e("摄像头个数", "" + cameraCount);
        if (cameraCount == 0) {
            Toast.makeText(this, "没有检测到摄像头", Toast.LENGTH_SHORT).show();
            return;
        }

        if (cameraInfos.size() != 0) {
            Toast.makeText(this, "已打开。无需重复", Toast.LENGTH_SHORT).show();
            return;
        }

        Toast.makeText(this, "检测到" + cameraCount + "个摄像头", Toast.LENGTH_SHORT).show();

        for (int i = 0; i < cameraCount; i++) {
            CameraInfo cameraInfo = genCameraUI(i);
            cameraInfos.add(cameraInfo);
        }

        if (cameraCount == 5) {
            //隐藏掉3，4因为老王规定前3路直接预览。后2路编码并显示
            //创建2个VideoDecSurface 同时connect到3 4

            VideoPlayerSurface vps = new VideoPlayerSurface(MainActivity.this );
            vps.setId( View.generateViewId() );
            clroot.addView( vps );

            //横屏搞成3X2 竖屏搞成2X3
            ConstraintSet cloneSet = new ConstraintSet();
            cloneSet.clone(clroot); //get constraints from ConstraintSet

            int onecamW = isLandscape ? screenWidth / 3 : screenWidth / 2;
            int onecamH = isLandscape ? screenHeight / 2 : screenHeight / 3;
            for (int i = 0; i < cameraInfos.size(); i++) {
                cloneSet.constrainWidth(cameraInfos.get(i).preview.getId(), onecamW);
                cloneSet.constrainHeight(cameraInfos.get(i).preview.getId(), onecamH);
            }

            if (isLandscape) {
                cloneSet.connect(cameraInfos.get(1).preview.getId(), ConstraintSet.LEFT, cameraInfos.get(0).preview.getId(), ConstraintSet.RIGHT);
                cloneSet.connect(cameraInfos.get(2).preview.getId(), ConstraintSet.LEFT, cameraInfos.get(1).preview.getId(), ConstraintSet.RIGHT);
                cloneSet.connect(cameraInfos.get(3).preview.getId(), ConstraintSet.TOP, cameraInfos.get(0).preview.getId(), ConstraintSet.BOTTOM);
                cloneSet.connect(cameraInfos.get(4).preview.getId(), ConstraintSet.LEFT, cameraInfos.get(3).preview.getId(), ConstraintSet.RIGHT);
                cloneSet.connect(cameraInfos.get(4).preview.getId(), ConstraintSet.TOP, cameraInfos.get(3).preview.getId(), ConstraintSet.TOP);
            } else {
                cloneSet.connect(cameraInfos.get(1).preview.getId(), ConstraintSet.LEFT, cameraInfos.get(0).preview.getId(), ConstraintSet.RIGHT);
                cloneSet.connect(cameraInfos.get(2).preview.getId(), ConstraintSet.TOP, cameraInfos.get(0).preview.getId(), ConstraintSet.BOTTOM);

                cloneSet.connect(cameraInfos.get(3).preview.getId(), ConstraintSet.LEFT, cameraInfos.get(2).preview.getId(), ConstraintSet.RIGHT);
                cloneSet.connect(cameraInfos.get(3).preview.getId(), ConstraintSet.TOP, cameraInfos.get(2).preview.getId(), ConstraintSet.TOP);

                cloneSet.connect(cameraInfos.get(4).preview.getId(), ConstraintSet.TOP, cameraInfos.get(2).preview.getId(), ConstraintSet.BOTTOM);
            }

            cameraInfos.get(3).preview.setVisibility(View.INVISIBLE);
            cameraInfos.get(4).preview.setVisibility(View.INVISIBLE);

            cloneSet.constrainWidth(vps.getId(), onecamW);
            cloneSet.constrainHeight(vps.getId(), onecamH);
            cloneSet.connect(vps.getId(), ConstraintSet.TOP, cameraInfos.get(4).preview.getId(), ConstraintSet.TOP);
            cloneSet.connect(vps.getId(), ConstraintSet.LEFT, cameraInfos.get(4).preview.getId(), ConstraintSet.RIGHT);

            cloneSet.applyTo(clroot);
        }

        //开一个线程 定时更新帧率
        if (thUpdateFPS == null) {
            thUpdateFPS = new Thread(new Runnable() {
                @Override
                public void run() {
                    while (true) {
                        try {
                            Thread.sleep(1000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }

                        if (mHandler != null) {
                            mHandler.sendEmptyMessage(100);
                        }
                    }
                }
            });
            thUpdateFPS.start();
        }
    }

    //关闭
    public void closeCameras() {
        Log.e("closeCameras", "closeCameras");
        for (int i = 0; i < cameraCount; i++) {
            if (cameraInfos.get(i).preview != null) {
                cameraInfos.get(i).preview.stopCamera();
                clroot.removeView(cameraInfos.get(i).preview);
                clroot.removeView(cameraInfos.get(i).txt_info);
                clroot.removeView(cameraInfos.get(i).imgClose);
                cameraInfos.get(i).preview = null;
            }
        }

        cameraInfos.clear();

        if (thUpdateFPS != null) {
            thUpdateFPS.interrupt();
            thUpdateFPS = null;
        }
    }

    public Handler mHandler = new Handler() {
        @SuppressLint("ResourceType")
        public void handleMessage(android.os.Message msg) {
            switch (msg.what) {
                case 100: {
                    //Log.e("adsf","ddddddddddddd");
                    for (int i = 0; i < cameraInfos.size(); i++) {
                        CameraInfo camInfo = cameraInfos.get(i);
                        //Log.i("asdfasd", "asdfasdf" + i);
                        if (camInfo.preview != null && camInfo.txt_info != null) {
                            camInfo.txt_info.setText(String.format("ID:%d %dx%d@%.1f ROT:%d",
                                    camInfo.preview.cameraId,
                                    camInfo.preview.previewW,
                                    camInfo.preview.previewH,
                                    camInfo.preview.frame_rate,
                                    camInfo.preview.degree
                            ));
                        }
                    }
                }
                break;
            }
        }
    };

    CameraInfo genCameraUI(int camID) {
        final CameraInfo cameraInfo = new CameraInfo();

        cameraInfo.preview = new DCCameraSurface(this, camID, 0);
        cameraInfo.preview.setId(View.generateViewId());
        cameraInfo.preview.setTag(cameraInfo);
        cameraInfo.preview.setDataHandler(new DCCameraSurface.PreviewDataHandler() {
            @Override
            public void onPreviewSizeConfirm(DCCameraSurface ds, int width, int height) {
                final CameraInfo camInfo = (CameraInfo) ds.getTag();

                if (vds3 == null && ds.cameraId == v1Index) {
                    vds3 = new VideoDecSurface(MainActivity.this, camInfo.preview.previewW, camInfo.preview.previewH);
                    vds3.setId(View.generateViewId());
                    clroot.addView(vds3);
                    ConstraintSet cloneSet = new ConstraintSet();
                    cloneSet.clone(clroot); //get constraints from ConstraintSet
                    int onecamW = isLandscape ? screenWidth / 3 : screenWidth / 2;
                    int onecamH = isLandscape ? screenHeight / 2 : screenHeight / 3;
                    cloneSet.constrainWidth(vds3.getId(), onecamW);
                    cloneSet.constrainHeight(vds3.getId(), onecamH);
                    cloneSet.connect(vds3.getId(), ConstraintSet.LEFT, cameraInfos.get(3).preview.getId(), ConstraintSet.LEFT);
                    cloneSet.connect(vds3.getId(), ConstraintSet.TOP, cameraInfos.get(3).preview.getId(), ConstraintSet.TOP);

                    cloneSet.applyTo(clroot);
                } else if (vds4 == null && ds.cameraId == v2Index) {
                    vds4 = new VideoDecSurface(MainActivity.this, camInfo.preview.previewW, camInfo.preview.previewH);
                    vds4.setId(View.generateViewId());
                    clroot.addView(vds4);

                    ConstraintSet cloneSet = new ConstraintSet();
                    cloneSet.clone(clroot); //get constraints from ConstraintSet
                    int onecamW = isLandscape ? screenWidth / 3 : screenWidth / 2;
                    int onecamH = isLandscape ? screenHeight / 2 : screenHeight / 3;
                    cloneSet.constrainWidth(vds4.getId(), onecamW);
                    cloneSet.constrainHeight(vds4.getId(), onecamH);
                    cloneSet.connect(vds4.getId(), ConstraintSet.LEFT, cameraInfos.get(4).preview.getId(), ConstraintSet.LEFT);
                    cloneSet.connect(vds4.getId(), ConstraintSet.TOP, cameraInfos.get(4).preview.getId(), ConstraintSet.TOP);
                    cloneSet.applyTo(clroot);
                }


                //动态生成界面控件。Id:0 分辨率 角度 帧率 关闭按钮
                if (camInfo.txt_info == null) {
                    camInfo.txt_info = new TextView(MainActivity.this);
                    camInfo.txt_info.setText(String.format("ID:%d %dx%d@%.1f ROT:%d",
                            camInfo.preview.cameraId,
                            camInfo.preview.previewW,
                            camInfo.preview.previewH,
                            camInfo.preview.frame_rate,
                            camInfo.preview.degree
                    ));
                    camInfo.txt_info.setBackgroundColor(android.graphics.Color.RED);
                    camInfo.toApplyW = width;
                    camInfo.toApplyH = height;
                    camInfo.toApplyDegree = camInfo.preview.degree;
                    camInfo.txt_info.setTextColor(Color.WHITE);
                    camInfo.txt_info.setId(View.generateViewId());
                    camInfo.txt_info.setTextSize(40);
                    camInfo.txt_info.setTag(camInfo);
                    camInfo.txt_info.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            la1bottom.removeAllViews();
                            //动态生成分辨率txt 和角度txt
                            TextView txtResolution = new TextView(MainActivity.this);
                            txtResolution.setText("分辨率");
                            txtResolution.setId(View.generateViewId());
                            txtResolution.setTextColor(Color.WHITE);
                            RelativeLayout.LayoutParams txtParam = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                            txtParam.setMargins(20, 10, 0, 0);
                            txtResolution.setLayoutParams(txtParam);
                            la1bottom.addView(txtResolution);

                            TextView txtRot = new TextView(MainActivity.this);
                            txtRot.setText("旋转");
                            txtRot.setId(View.generateViewId());
                            txtRot.setTextColor(Color.WHITE);
                            RelativeLayout.LayoutParams rotParam = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                            rotParam.setMargins(20, 0, 0, 10);
                            rotParam.addRule(RelativeLayout.ALIGN_RIGHT, txtResolution.getId());
                            rotParam.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM, la1bottom.getId());
                            txtRot.setLayoutParams(rotParam);

                            la1bottom.addView(txtRot);
                            //显示底边栏并初始化好该摄像头所支持的分辨率
                            int lastId = txtResolution.getId();
                            //动态生成分辨率控件。然后选中一个当前使用的分辨率
                            if (camInfo.preview.supportedPreviewSizes.size() > 0) {
                                for (int i = 0; i < camInfo.preview.supportedPreviewSizes.size(); i++) {
                                    Camera.Size cs = camInfo.preview.supportedPreviewSizes.get(i);
                                    RelativeLayout.LayoutParams relParam = new RelativeLayout.LayoutParams(80, 40);
                                    // relParam.addRule(RelativeLayout.CENTER_VERTICAL);
                                    relParam.addRule(RelativeLayout.END_OF, lastId);
                                    //relParam.setMarginStart(20);
                                    relParam.setMargins(10, 10, 0, 0);
                                    Button btnResolution1 = new Button(MainActivity.this);
                                    btnResolution1.setText(String.format("%dx%d", cs.width, cs.height));
                                    btnResolution1.setLayoutParams(relParam);
                                    btnResolution1.setId(View.generateViewId());
                                    btnResolution1.setBackground(MainActivity.this.getResources().getDrawable(R.drawable.btnbg_unsel));
                                    btnResolution1.setTextColor(Color.WHITE);
                                    btnResolution1.setOnClickListener(new View.OnClickListener() {
                                        @Override
                                        public void onClick(View view) {
                                            if (camInfo.lastbtnResolutions != null) {
                                                camInfo.lastbtnResolutions.setBackground(MainActivity.this.getResources().getDrawable(R.drawable.btnbg_unsel));
                                                camInfo.lastbtnResolutions.setTextColor(Color.WHITE);
                                            }

                                            Button btnMe = (Button) view;
                                            btnMe.setTextColor(Color.RED);
                                            btnMe.setBackground(MainActivity.this.getResources().getDrawable(R.drawable.btnbg));
                                            camInfo.lastbtnResolutions = btnMe;

                                            String[] splitValue = btnMe.getText().toString().split("x");
                                            camInfo.toApplyW = Integer.valueOf(splitValue[0]);
                                            camInfo.toApplyH = Integer.valueOf(splitValue[1]);
                                        }
                                    });

                                    String strCur = String.format("%dx%d", camInfo.preview.previewW, camInfo.preview.previewH);
                                    if (strCur.equals(btnResolution1.getText())) {
                                        btnResolution1.setBackground(MainActivity.this.getResources().getDrawable(R.drawable.btnbg));
                                        btnResolution1.setTextColor(Color.RED);

                                        camInfo.lastbtnResolutions = btnResolution1;
                                    }
                                    lastId = btnResolution1.getId();
                                    la1bottom.addView(btnResolution1);
                                }
                            }

                            lastId = txtRot.getId();
                            //动态生成旋转角控件，选中当前的旋转角
                            for (int i = 0; i < 4; i++) {
                                RelativeLayout.LayoutParams relParam = new RelativeLayout.LayoutParams(80, 40);
                                // relParam.addRule(RelativeLayout.CENTER_VERTICAL);
                                relParam.addRule(RelativeLayout.END_OF, lastId);
                                //relParam.setMarginStart(20);
                                relParam.setMargins(10, 60, 0, 0);
                                //relParam.addRule(RelativeLayout.ALIGN_BOTTOM, 10);
                                Button btnDegree = new Button(MainActivity.this);
                                btnDegree.setText(String.format("%d", i * 90));
                                btnDegree.setLayoutParams(relParam);
                                btnDegree.setId(View.generateViewId());
                                btnDegree.setBackground(MainActivity.this.getResources().getDrawable(R.drawable.btnbg));
                                btnDegree.setTextColor(Color.WHITE);
                                btnDegree.setOnClickListener(new View.OnClickListener() {
                                    @Override
                                    public void onClick(View view) {
                                        if (camInfo.lastbtnDegree != null) {
                                            camInfo.lastbtnDegree.setBackground(MainActivity.this.getResources().getDrawable(R.drawable.btnbg_unsel));
                                            camInfo.lastbtnDegree.setTextColor(Color.WHITE);
                                        }

                                        Button btnMe = (Button) view;
                                        btnMe.setTextColor(Color.RED);
                                        btnMe.setBackground(MainActivity.this.getResources().getDrawable(R.drawable.btnbg));
                                        camInfo.lastbtnDegree = btnMe;
                                        camInfo.toApplyDegree = Integer.valueOf(btnMe.getText().toString());
                                    }
                                });

                                String strCur = String.format("%d", camInfo.preview.degree);
                                if (strCur.equals(btnDegree.getText())) {
                                    btnDegree.setBackground(MainActivity.this.getResources().getDrawable(R.drawable.btnbg));
                                    btnDegree.setTextColor(Color.RED);
                                    camInfo.lastbtnDegree = btnDegree;
                                }

                                lastId = btnDegree.getId();
                                la1bottom.addView(btnDegree);
                            }

                            //在旋转角的后面生成一个应用按钮
                            RelativeLayout.LayoutParams rlBtnParam = new RelativeLayout.LayoutParams(80, 40);
                            // relParam.addRule(RelativeLayout.CENTER_VERTICAL);
                            rlBtnParam.addRule(RelativeLayout.END_OF, lastId);
                            //relParam.setMarginStart(20);
                            rlBtnParam.setMargins(20, 60, 0, 0);

                            Button btnApply = new Button(MainActivity.this);
                            btnApply.setText("应用");
                            btnApply.setLayoutParams(rlBtnParam);
                            btnApply.setId(View.generateViewId());
                            btnApply.setBackground(MainActivity.this.getResources().getDrawable(R.drawable.btnbg));
                            btnApply.setTextColor(Color.WHITE);
                            btnApply.setLayoutParams(rlBtnParam);
                            la1bottom.addView(btnApply);
                            btnApply.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View view) {
                                    la1bottom.setVisibility(View.INVISIBLE);

                                    //完全没变化。 不管
                                    if (camInfo.toApplyW == camInfo.preview.previewW
                                            && camInfo.toApplyH == camInfo.preview.previewH
                                            && camInfo.toApplyDegree == camInfo.preview.degree) {
                                        Log.e("CameraUIH", "完全没变化。不管");
                                    } else {
                                        Log.d("要应用的是", "preW" + camInfo.toApplyW + "preH" + camInfo.toApplyH + " degree" + camInfo.toApplyDegree);
                                        camInfo.preview.previewW = camInfo.toApplyW;
                                        camInfo.preview.previewH = camInfo.toApplyH;
                                        camInfo.preview.degree = camInfo.toApplyDegree;
                                        camInfo.preview.stopCamera();
                                        //分辨率有变
                                        if(camInfo.preview.cameraId == v1Index ){
                                            clroot.removeView( vds3 ); vds3 = null;
                                        }
                                        else if(cameraInfo.preview.cameraId == v2Index ){
                                            clroot.removeView( vds4); vds4 = null;
                                        }

                                        new Handler().postDelayed(new Runnable() {
                                            public void run() {
                                                camInfo.preview.openCamera();
                                            }
                                        }, 3000);
                                    }
                                }
                            });

                            la1bottom.bringToFront();
                            la1bottom.setVisibility(View.VISIBLE);

                            //开始10秒隐藏倒计时
                            new Handler().postDelayed(new Runnable() {
                                public void run() {
                                    la1bottom.setVisibility(View.INVISIBLE);
                                }
                            }, 10000);
                        }
                    });

                    clroot.addView(camInfo.txt_info);
                    ConstraintSet cloneSet = new ConstraintSet();
                    cloneSet.clone(clroot); // get constraints from ConstraintSet
                    cloneSet.connect(camInfo.txt_info.getId(), ConstraintSet.LEFT, camInfo.preview.getId(), ConstraintSet.LEFT);
                    //cloneSet.applyTo(clroot);

                    camInfo.imgClose = new ImageView(MainActivity.this);
                    camInfo.imgClose.setId(View.generateViewId());
                    camInfo.imgClose.setImageDrawable(MainActivity.this.getDrawable(R.drawable.onoff));
                    camInfo.imgClose.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            Log.e("单机", "图片控件");
                            if (camInfo.preview.mCamera != null)
                                camInfo.preview.stopCamera();
                            else
                                camInfo.preview.openCamera();
                        }
                    });

                    clroot.addView(camInfo.imgClose);
                    cloneSet.connect(camInfo.imgClose.getId(), ConstraintSet.TOP, clroot.getId(), ConstraintSet.TOP, 10);
                    cloneSet.constrainWidth(camInfo.imgClose.getId(), 50);
                    cloneSet.constrainHeight(camInfo.imgClose.getId(), 50);
                    cloneSet.connect(camInfo.imgClose.getId(), ConstraintSet.LEFT, camInfo.txt_info.getId(), ConstraintSet.RIGHT, 20);
                    cloneSet.applyTo(clroot);
                }//end of if null
                else //control exist
                {
                    camInfo.txt_info.setText(String.format("ID:%d %dx%d@%.1f ROT:%d",
                            camInfo.preview.cameraId,
                            camInfo.preview.previewW,
                            camInfo.preview.previewH,
                            camInfo.preview.frame_rate,
                            camInfo.preview.degree
                    ));
                }
            }

            @Override
            public void onPreviewFrame(DCCameraSurface camera, byte[] data) {
                if( camera.cameraId == v1Index){
                    vds3.feed( data );
                }
                else if(camera.cameraId == v2Index){
                    vds4.feed( data );
                }
            }
        });
        clroot.addView(cameraInfo.preview);

        return cameraInfo;
    }
}
