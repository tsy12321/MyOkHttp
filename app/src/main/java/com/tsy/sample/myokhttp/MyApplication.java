package com.tsy.sample.myokhttp;

import android.app.Application;

import com.tsy.sdk.myokhttp.MyOkHttp;

/**
 * Created by tsy on 2016/12/6.
 */

public class MyApplication extends Application {

    private static MyApplication mInstance;
    private MyOkHttp mMyOkHttp;
    private DownloadMgr mDownloadMgr;

    @Override
    public void onCreate() {
        super.onCreate();

        mInstance = this;

        //自定义OkHttp
//        OkHttpClient okHttpClient = new OkHttpClient.Builder()
//                .connectTimeout(10000L, TimeUnit.MILLISECONDS)
//                .readTimeout(10000L, TimeUnit.MILLISECONDS)
//                .build();
//        mMyOkhttp = new MyOkHttp(okHttpClient);

        mMyOkHttp = new MyOkHttp();

        mDownloadMgr = (DownloadMgr) new DownloadMgr.Builder()
                .myOkHttp(mMyOkHttp)
                .maxDownloadIngNum(5)       //设置最大同时下载数量（不设置默认5）
                .saveProgressBytes(50 * 1204)   //设置每50kb触发一次saveProgress保存进度 （不能在onProgress每次都保存 过于频繁） 不设置默认50kb
                .build();

        mDownloadMgr.resumeTasks();     //恢复本地所有未完成的任务
    }

    public static synchronized MyApplication getInstance() {
        return mInstance;
    }

    public MyOkHttp getMyOkHttp() {
        return mMyOkHttp;
    }

    public DownloadMgr getDownloadMgr() {
        return mDownloadMgr;
    }
}
