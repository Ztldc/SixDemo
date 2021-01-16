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

public class DCCameraSurface extends SurfaceView implements SurfaceHolder.Callback {
    private static String TAG = "DCCameraSurface";
    public static final boolean DEBUG = BuildConfig.DEBUG;
    public Camera mCamera = null;//正在使用的摄像机
    public int cameraId = 0; //摄像机的id
    public int previewW = -1;
    public int previewH = -1;
    public int degree = 0;//预览旋转角

    public List<android.hardware.Camera.Size> supportedPreviewSizes;//保存到外面动态生成界面

    int total_frame = 0;
    public float frame_rate = 0.0f;
    long timeFirstPack = 0;
    //相机预览数据回调
    private Camera.PreviewCallback previewCallback = new Camera.PreviewCallback() {
        @Override
        public void onPreviewFrame(byte[] data, Camera camera) {
            synchronized (this) {
                if (data == null) {
                    Camera.Parameters params = camera.getParameters();
                    Camera.Size size = params.getPreviewSize();
                    int bufferSize = (size.width * size.height * ImageFormat.getBitsPerPixel(params.getPreviewFormat())) / 8;
                    camera.addCallbackBuffer(new byte[bufferSize]);
                } else {
                    if (dataHandler != null) {
                        dataHandler.onPreviewFrame(DCCameraSurface.this, data);
                    }

                    if (BuildConfig.DEBUG) {
                        total_frame += 1;
                        if (total_frame % 20 == 0 && total_frame > 0) {
                            long nowTM = System.currentTimeMillis();
                            frame_rate = total_frame / ((nowTM - timeFirstPack) / 1000.0f);
                            //System.out.println("camera_"+cameraId+" fps:" + frame_rate);//打印帧率
                            timeFirstPack = System.currentTimeMillis();
                            total_frame = 0;
                        }
                    }

                    camera.addCallbackBuffer(data);
                }
            }
        }
    };

    //定义SurfaceHolder
    private SurfaceHolder holder;
    Context context;

    //构造函数
    public DCCameraSurface(Context context, int cameraId, int degree) {
        super(context);
        this.context = context;
        this.cameraId = cameraId;
        this.degree = degree;

        //获取Holder
        holder = getHolder();
        //加入SurfaceHolder.Callback在类中implements
        holder.addCallback(this);
        holder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        //保持屏幕常亮
        //holder.setKeepScreenOn(true);
        //setBackgroundColor(Color.BLUE);
    }

    public void openCamera() {
        if (mCamera != null) {
            return;
        }

        int cameraCount = Camera.getNumberOfCameras();
        if (cameraCount <= 0) {
            if (DEBUG) Log.i(TAG, "no camera detected。exit openCamera.");
            return;
        }

        if (DEBUG) Log.i(TAG, "detected " + cameraCount + " camera");

        try {
            mCamera = Camera.open(cameraId);
        } catch (Exception e) {
            e.printStackTrace();
            mCamera = null;
            return;
        }

        if (DEBUG) {
            Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
            Camera.getCameraInfo(cameraId, cameraInfo);
            Log.i(TAG, "camera " + cameraId + "facing " + cameraInfo.facing);
        }

        if (mCamera == null) {
            if (DEBUG) Log.i(TAG, "Camera Opening is null return.");
            return;
        }

        mCamera.stopPreview();
        Camera.Parameters parameters;
        try {
            parameters = mCamera.getParameters();
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }

        //获取支持分辨率
        List<Camera.Size> supportedPictureSizes = parameters.getSupportedPictureSizes();
        if (supportedPictureSizes.size() > 0) {
            if (DEBUG) {
                Collections.sort(supportedPictureSizes, new Comparator<Camera.Size>() {
                    @Override
                    public int compare(Camera.Size lhs, Camera.Size rhs) {
                        if (lhs.width > rhs.width) {
                            return -1;
                        } else if (lhs.width == rhs.width) {
                            if (lhs.height > rhs.height)
                                return -1;
                            else if (lhs.height < rhs.height)
                                return 1;
                            else return 0;
                        } else {
                            return 1;
                        }
                    }
                });

                String camTip = "getSupportedPictureSizes:";
                for (int i = 0; i < supportedPictureSizes.size(); i++) {
                    camTip += (supportedPictureSizes.get(i).width + "x" + supportedPictureSizes.get(i).height + " ");
                }
                Log.i(TAG, camTip);
            }
        }

        //获取预览分辨率列表
        supportedPreviewSizes = parameters.getSupportedPreviewSizes();
        if (supportedPreviewSizes.size() > 0) {
                Collections.sort(supportedPreviewSizes, new Comparator<Camera.Size>() {
                    @Override
                    public int compare(Camera.Size lhs, Camera.Size rhs) {
                        if (lhs.width > rhs.width) {
                            return -1;
                        } else if (lhs.width == rhs.width) {
                            if (lhs.height > rhs.height)
                                return -1;
                            else if (lhs.height < rhs.height)
                                return 1;
                            else return 0;
                        } else {
                            return 1;
                        }
                    }
                });
            if (DEBUG) {
                String camTip = "getSupportedPreviewSizes:";
                for (int i = 0; i < supportedPreviewSizes.size(); i++) {
                    camTip += (supportedPreviewSizes.get(i).width + "x" + supportedPreviewSizes.get(i).height + " ");
                }
                Log.i(TAG, camTip);
            }
        }

        //获取屏幕参数。横屏 竖屏？
        WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        DisplayMetrics metrics = new DisplayMetrics();
        wm.getDefaultDisplay().getRealMetrics(metrics);

        int screenWidth = metrics.widthPixels;
        int screenHeight = metrics.heightPixels;

        boolean isLandscape = false;
        if (screenWidth > screenHeight) {//横屏
            isLandscape = true;
        } else {//竖屏
            isLandscape = false;
        }

        if( previewW == -1 && previewH == -1){
            getBestPreviewSize(parameters, isLandscape, screenWidth, screenHeight);
            if (cameraCount <= 1) {
                previewW = bestPreviewWidth;
                previewH = bestPreviewHeight;
                if (dataHandler != null)
                    dataHandler.onPreviewSizeConfirm(this, bestPreviewWidth, bestPreviewHeight);

            } else {
                previewW = 1280;//1280 //640
                previewH = 720;//720 //480

                if (dataHandler != null)
                    dataHandler.onPreviewSizeConfirm(this, previewW, previewH);
            }
        }else{
            if (dataHandler != null)
                dataHandler.onPreviewSizeConfirm(this, previewW, previewH);
        }

        parameters.setPreviewSize(previewW, previewH);
        if (DEBUG) Log.d("预览分辨率", "" + previewW + "x" + previewH);
        if(android.os.Build.MODEL.equals("ZTL-A40i")){

        }else{
            parameters.setPreviewFormat(ImageFormat.YV12);//参谋说的 不然灰色
        }
        //parameters.setPreviewFormat(ImageFormat.NV21);//PixelFormat.YCbCr_420_SP //ImageFormat.NV21
        SetCameraFPS(parameters);
        /*
        官方提供CameraInfo.orientation表示相机图像的方向。它的值是相机图像顺时针旋转到设备自然方向一致时的角度。
        例如假设设备是竖屏的。后置相机传感器是横屏安装的。当你面向屏幕时，如果后置相机传感器顶边的和设备自然方向的右边是平行的，则后置相机的orientation是90。
        如果前置相机传感器顶边和设备自然方向的右边是平行的，则前置相机的orientation是270。
        camera.CameraInfo.orientation 可以获取方向，进行适配。
        需要特别说明的是，对于前置相机来说，相机预览的图像是相机采集到的图像的镜像。
        */
        mCamera.setDisplayOrientation(degree);

        Log.i(TAG, "using preview w:" + previewW + "h " + previewH);

        try {
            mCamera.setParameters(parameters);
        } catch (Exception ex) {
            Log.e(TAG, "Apply Camera Config failed.");
            return;
        }

        //w*h*1.5
        int bufferSize = (previewW * previewH * ImageFormat.getBitsPerPixel(parameters.getPreviewFormat())) / 8;
        mCamera.addCallbackBuffer(new byte[bufferSize]);

        try {
            mCamera.setPreviewCallbackWithBuffer(previewCallback);
            mCamera.setPreviewDisplay(holder);
            mCamera.startPreview();
        } catch (Exception ex) {
            mCamera.release();
            mCamera = null;
            ex.printStackTrace();
            return;
        }
    }

    //关闭摄像机
    public void stopCamera() {
        if (mCamera != null) {
            mCamera.setPreviewCallback(null);
            mCamera.stopPreview();
            mCamera.release();
            mCamera = null;
        }
    }

    @Override
    public void surfaceCreated(SurfaceHolder surfaceHolder) {
        if (DEBUG) Log.i(TAG, "cameraId" + cameraId + " surfaceCreated..");

        openCamera();
    }

    @Override
    public void surfaceChanged(SurfaceHolder surfaceHolder, int format, int width, int height) {
        //if (DEBUG) Log.e(TAG, "surfaceChanged..");
        Log.i(TAG, "surfaceChanged: wh" + width + "x" + height);

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder surfaceHolder) {
        if (DEBUG) Log.d(TAG, "surfaceDestroyed");
        stopCamera();
    }

    //设置动态fps
    private void SetCameraFPS(Camera.Parameters parameters) {
        if (parameters == null)
            return;

        int[] findRange = null;

        int defFPS = 30 * 1000;

        List<int[]> fpsList = parameters.getSupportedPreviewFpsRange();
        if (fpsList != null && fpsList.size() > 0) {
            for (int i = 0; i < fpsList.size(); ++i) {
                int[] range = fpsList.get(i);
                if (range != null
                        && Camera.Parameters.PREVIEW_FPS_MIN_INDEX < range.length
                        && Camera.Parameters.PREVIEW_FPS_MAX_INDEX < range.length) {
                    if (DEBUG)
                        Log.d(TAG, "Camera index:" + i + " support fps[min-max]:" + range[Camera.Parameters.PREVIEW_FPS_MIN_INDEX] + "-" + range[Camera.Parameters.PREVIEW_FPS_MAX_INDEX]);

                    if (findRange == null) {
                        if (defFPS <= range[Camera.Parameters.PREVIEW_FPS_MAX_INDEX]) {
                            findRange = range;

                            if (DEBUG)
                                Log.d(TAG, "Camera found appropriate fps, min fps:" + range[Camera.Parameters.PREVIEW_FPS_MIN_INDEX]
                                        + " ,max fps:" + range[Camera.Parameters.PREVIEW_FPS_MAX_INDEX]);
                        }
                    }
                }
            }
        }

        if (findRange != null) {
            parameters.setPreviewFpsRange(findRange[Camera.Parameters.PREVIEW_FPS_MIN_INDEX], findRange[Camera.Parameters.PREVIEW_FPS_MAX_INDEX]);
        }
    }

    private PreviewDataHandler dataHandler = null;

    public interface PreviewDataHandler {
        void onPreviewSizeConfirm(DCCameraSurface camera, int width, int height);

        void onPreviewFrame(DCCameraSurface camera, byte[] data);
    }

    public void setDataHandler(PreviewDataHandler handler) {
        this.dataHandler = handler;
    }

    //todo 放到另一个文件里去
    public static int bestPreviewWidth;
    public static int bestPreviewHeight;
    public static int PREVIEW_MODE = 0;
    public static int PREVIEW_EQUAL_SCREEN = 0;
    public static int PREVIEW_EQUAL_SCALE = 1;
    public static int PREVIEW_EQUAL_SIDE = 2;
    public static int PREVIEW_MAX = 3;
    public static int PREVIEW_CUSTOM = 4;

    public static boolean getBestPreviewSize(Camera.Parameters var0, boolean var1, int var2, int var3) {
        List var4 = var0.getSupportedPreviewSizes();
        if (true) {
            Iterator var5 = var4.iterator();

            while (var5.hasNext()) {
                Camera.Size var6 = (Camera.Size) var5.next();
                //LogUtils.d(CameraInterface.TAG, "Supported PreviewSizes Width = " + var6.width + " Height = " + var6.height);
            }
        }

        boolean var7 = false;
        int var15;
        int var16;
        if (var1) {
            var15 = var2;
            var16 = var3;
        } else {
            var15 = var3;
            var16 = var2;
        }

        Iterator var8 = var4.iterator();

        while (var8.hasNext()) {
            Camera.Size var9 = (Camera.Size) var8.next();
            if (var9.width == var15 && var9.height == var16) {
                var7 = true;
                bestPreviewWidth = var9.width;
                bestPreviewHeight = var9.height;
                PREVIEW_MODE = PREVIEW_EQUAL_SCREEN;
                break;
            }
        }

        if (var7) {
            return true;
        } else {
            double var17 = (double) var15 * 1.0D / (double) var16;
            ArrayList var10 = new ArrayList();
            var10.clear();
            Iterator var11 = var4.iterator();

            Camera.Size var12;
            while (var11.hasNext()) {
                var12 = (Camera.Size) var11.next();
                double var13 = (double) var12.width * 1.0D / (double) var12.height;
                if (var17 == var13) {
                    var10.add(var12);
                }
            }

            int var18;
            int var19;
            int var20;
            if (var10.size() > 0) {
                var18 = 0;
                var19 = ((Camera.Size) var10.get(var18)).width;

                for (var20 = 0; var20 < var10.size(); ++var20) {
                    if (((Camera.Size) var10.get(var20)).width > var19) {
                        var18 = var20;
                        var19 = ((Camera.Size) var10.get(var20)).width;
                    }
                }

                bestPreviewWidth = ((Camera.Size) var10.get(var18)).width;
                bestPreviewHeight = ((Camera.Size) var10.get(var18)).height;
                PREVIEW_MODE = PREVIEW_EQUAL_SCALE;
                return true;
            } else {
                var11 = var4.iterator();

                while (var11.hasNext()) {
                    var12 = (Camera.Size) var11.next();
                    if (var12.width == var15 || var12.height == var16) {
                        var7 = true;
                        bestPreviewWidth = var12.width;
                        bestPreviewHeight = var12.height;
                        PREVIEW_MODE = PREVIEW_EQUAL_SIDE;
                        break;
                    }
                }

                if (var7) {
                    return true;
                } else {
                    var18 = 0;
                    var19 = 0;

                    for (var20 = 0; var20 < var4.size(); ++var20) {
                        if (((Camera.Size) var4.get(var20)).width > var19) {
                            var18 = var20;
                            var19 = ((Camera.Size) var4.get(var20)).width;
                        }
                    }

                    if (var19 != 0) {
                        bestPreviewWidth = ((Camera.Size) var4.get(var18)).width;
                        bestPreviewHeight = ((Camera.Size) var4.get(var18)).height;
                        PREVIEW_MODE = PREVIEW_MAX;
                        return true;
                    } else {
                        return false;
                    }
                }
            }
        }
    }
}
