package com.tsy.sdk.myokhttp.builder;

import android.support.annotation.NonNull;

import com.tsy.sdk.myokhttp.MyOkHttp;
import com.tsy.sdk.myokhttp.body.ResponseProgressBody;
import com.tsy.sdk.myokhttp.callback.MyDownloadCallback;
import com.tsy.sdk.myokhttp.response.DownloadResponseHandler;
import com.tsy.sdk.myokhttp.util.LogUtils;

import java.io.File;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

import okhttp3.Call;
import okhttp3.Headers;
import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

/**
 * download builder
 * Created by tsy on 16/9/18.
 */
public class DownloadBuilder {

    private MyOkHttp mMyOkHttp;

    private String mUrl = "";
    private Object mTag;
    private Map<String, String> mHeaders;

    private String mFileDir = "";        //文件dir
    private String mFileName = "";       //文件名
    private String mFilePath = "";       //文件路径 （如果设置该字段则上面2个就不需要）

    private Long mCompleteBytes = 0L;    //已经完成的字节数 用于断点续传

    public DownloadBuilder(MyOkHttp myOkHttp) {
        mMyOkHttp = myOkHttp;
    }

    public DownloadBuilder url(@NonNull String url) {
        this.mUrl = url;
        return this;
    }

    /**
     * set file storage dir
     * @param fileDir file directory
     * @return
     */
    public DownloadBuilder fileDir(@NonNull String fileDir) {
        this.mFileDir = fileDir;
        return this;
    }

    /**
     * set file storage name
     * @param fileName file name
     * @return
     */
    public DownloadBuilder fileName(@NonNull String fileName) {
        this.mFileName = fileName;
        return this;
    }

    /**
     * set file path
     * @param filePath file path
     * @return
     */
    public DownloadBuilder filePath(@NonNull String filePath) {
        this.mFilePath = filePath;
        return this;
    }

    /**
     * set tag
     * @param tag tag
     * @return
     */
    public DownloadBuilder tag(@NonNull Object tag) {
        this.mTag = tag;
        return this;
    }

    /**
     * set headers
     * @param headers headers
     * @return
     */
    public DownloadBuilder headers(@NonNull Map<String, String> headers) {
        this.mHeaders = headers;
        return this;
    }

    /**
     * set one header
     * @param key header key
     * @param val header val
     * @return
     */
    public DownloadBuilder addHeader(@NonNull String key, @NonNull String val) {
        if (this.mHeaders == null)
        {
            mHeaders = new LinkedHashMap<>();
        }
        mHeaders.put(key, val);
        return this;
    }

    /**
     * set completed bytes (BreakPoints)
     * @param completeBytes 已经完成的字节数
     * @return
     */
    public DownloadBuilder setCompleteBytes(@NonNull Long completeBytes) {
        if(completeBytes > 0L) {
            this.mCompleteBytes = completeBytes;
            addHeader("RANGE", "bytes=" + completeBytes + "-");     //添加断点续传header
        }
        return this;
    }

    /**
     * 异步执行
     * @param downloadResponseHandler 下载回调
     */
    public Call enqueue(final DownloadResponseHandler downloadResponseHandler) {
        try {
            if(mUrl.length() == 0) {
                throw new IllegalArgumentException("Url can not be null !");
            }

            if(mFilePath.length() == 0) {
                if(mFileDir.length() == 0 || mFileName.length() == 0) {
                    throw new IllegalArgumentException("FilePath can not be null !");
                } else {
                    mFilePath = mFileDir + mFileName;
                }
            }
            checkFilePath(mFilePath, mCompleteBytes);

            Request.Builder builder = new Request.Builder().url(mUrl);
            appendHeaders(builder, mHeaders);

            if (mTag != null) {
                builder.tag(mTag);
            }

            Request request = builder.build();

            Call call = mMyOkHttp.getOkHttpClient().newBuilder()
                    .addNetworkInterceptor(new Interceptor() {      //设置拦截器
                        @Override
                        public Response intercept(Chain chain) throws IOException {
                            Response originalResponse = chain.proceed(chain.request());
                            return originalResponse.newBuilder()
                                    .body(new ResponseProgressBody(originalResponse.body(), downloadResponseHandler))
                                    .build();
                        }
                    })
                    .build()
                    .newCall(request);
            call.enqueue(new MyDownloadCallback(downloadResponseHandler, mFilePath, mCompleteBytes));

            return call;
        } catch (Exception e) {
            LogUtils.e("Download enqueue error:" + e.getMessage());
            downloadResponseHandler.onFailure(e.getMessage());
            return null;
        }
    }

    //检查filePath有效性
    private void checkFilePath(String filePath, Long completeBytes) throws Exception {
        File file = new File(filePath);
        if(file.exists()) {
            return ;
        }

        if(completeBytes > 0L) {       //如果设置了断点续传 则必须文件存在
            throw new Exception("断点续传文件" + filePath + "不存在！");
        }

        if (filePath.endsWith(File.separator)) {
            throw new Exception("创建文件" + filePath + "失败，目标文件不能为目录！");
        }

        //判断目标文件所在的目录是否存在
        if(!file.getParentFile().exists()) {
            if(!file.getParentFile().mkdirs()) {
                throw new Exception("创建目标文件所在目录失败！");
            }
        }
    }

    //append headers into builder
    private void appendHeaders(Request.Builder builder, Map<String, String> headers) {
        Headers.Builder headerBuilder = new Headers.Builder();
        if (headers == null || headers.isEmpty()) return;

        for (String key : headers.keySet()) {
            headerBuilder.add(key, headers.get(key));
        }
        builder.headers(headerBuilder.build());
    }
}
