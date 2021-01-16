package com.ztl.sixdemo;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;


import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class SplashActivity extends Activity {

    private String[] permissions = new String[]{
//            Manifest.permission.READ_EXTERNAL_STORAGE,
//            Manifest.permission.WRITE_EXTERNAL_STORAGE,
//            Manifest.permission.INTERNET,
           Manifest.permission.CAMERA
    };

    List<String> permissions_to_request;
    private static final int RC_REQUEST_PERMISSION = 100;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().requestFeature(Window.FEATURE_NO_TITLE);

        setContentView(R.layout.activity_splash);

        //申请权限
        if (Build.VERSION.SDK_INT >= 23) {
            permissions_to_request = new ArrayList<>();
            for (int i = 0; i < permissions.length; i++) {
                if (checkSelfPermission(permissions[i]) != PackageManager.PERMISSION_GRANTED) {
                    permissions_to_request.add(permissions[i]);
                }
            }

            if (permissions_to_request.size() > 0)
                ActivityCompat.requestPermissions(this, permissions_to_request.toArray(new String[permissions_to_request.size()]), RC_REQUEST_PERMISSION);
            else
                initData();
        } else {
            //为防止版本权限问题 这里搞成一个函数就好了
            initData();
        }
    }

    @TargetApi(23)
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode != RC_REQUEST_PERMISSION) {
            return;
        }
        // 处理申请结果
        boolean[] shouldShowRequestPermissionRationale = new boolean[permissions.length];
        for (int i = 0; i < permissions.length; ++i) {
            shouldShowRequestPermissionRationale[i] = shouldShowRequestPermissionRationale(permissions[i]);
        }
        this.onRequestPermissionsResult(permissions, grantResults, shouldShowRequestPermissionRationale);
    }

    @TargetApi(23)
    void onRequestPermissionsResult(String[] permissions, int[] grantResults, boolean[] shouldShowRequestPermissionRationale) {
        int length = permissions.length;
        int granted = 0;
        for (int i = 0; i < length; i++) {
            if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                if (shouldShowRequestPermissionRationale[i] == true) {

                    // CALLBACK.shouldShowRational(permissions[i]);//用户点击拒绝，但不勾选“不再提示”，下次请求权限时，系统弹窗依然会出现
                    Toast.makeText(getApplicationContext(), "用户点击拒绝，但不勾选“不再提示”", Toast.LENGTH_SHORT).show();
                    new Handler().postDelayed(new Runnable() {
                        public void run() {
                            finish();
                        }
                    }, 1000);
                    //原文链接：https://blog.csdn.net/centor/article/details/85157225

                } else {
                    Toast.makeText(getApplicationContext(), "permission_not_grant", Toast.LENGTH_SHORT).show();
                    //  CALLBACK.onPermissonReject(permissions[i]);//用户点击拒绝，并勾选“不再提示”，下次请求权限时，系统弹窗不会再出现
                    new Handler().postDelayed(new Runnable() {
                        public void run() {
                            finish();
                        }
                    }, 1000);
                    //原文链接：https://blog.csdn.net/centor/article/details/85157225
                }
            } else {
                granted++;
            }
        }
        if (granted == length) {
            //CALLBACK.onPermissionGranted();//权限全部获得
            initData();
        }
    }

    void initData()
    {
        Intent intent1 = new Intent(this, MainActivity.class);
        intent1.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        getApplicationContext().startActivity(intent1);
        SplashActivity.this.finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }
}
