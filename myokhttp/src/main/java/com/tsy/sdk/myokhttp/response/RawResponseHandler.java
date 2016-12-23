package com.tsy.sdk.myokhttp.response;

import com.tsy.sdk.myokhttp.util.LogUtils;

import java.io.IOException;

import okhttp3.Response;
import okhttp3.ResponseBody;

/**
 * raw 字符串结果回调
 * Created by tsy on 16/8/18.
 */
public abstract class RawResponseHandler implements IResponseHandler {

    @Override
    public final void onSuccess(Response response) {
        ResponseBody responseBody = response.body();
        String responseBodyStr = "";

        try {
            responseBodyStr = responseBody.string();
        } catch (IOException e) {
            e.printStackTrace();
            LogUtils.e("onResponse fail read response body");
            onFailure(response.code(), "fail read response body");
            return;
        } finally {
            responseBody.close();
        }

        onSuccess(response.code(), responseBodyStr);
    }

    public abstract void onSuccess(int statusCode, String response);

    @Override
    public void onProgress(long currentBytes, long totalBytes) {

    }
}
