package com.tsy.sdk.myokhttp.response;

import com.tsy.sdk.myokhttp.util.LogUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.IOException;

import okhttp3.Response;
import okhttp3.ResponseBody;

/**
 * json类型的回调接口
 * Created by tsy on 16/8/15.
 */
public abstract class JsonResponseHandler implements IResponseHandler {

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

        try {
            Object result = new JSONTokener(responseBodyStr).nextValue();
            if(result instanceof JSONObject) {
                onSuccess(response.code(), (JSONObject) result);
            } else if(result instanceof JSONArray) {
                onSuccess(response.code(), (JSONArray) result);
            } else {
                LogUtils.e("onResponse fail parse jsonobject, body=" + responseBodyStr);
                onFailure(response.code(), "fail parse jsonobject, body=" + responseBodyStr);
            }
        } catch (JSONException e) {
            e.printStackTrace();
            LogUtils.e("onResponse fail parse jsonobject, body=" + responseBodyStr);
            onFailure(response.code(), "fail parse jsonobject, body=" + responseBodyStr);
        }
    }

    public void onSuccess(int statusCode, JSONObject response) {
        LogUtils.w("onSuccess(int statusCode, JSONObject response) was not overriden, but callback was received");
    }

    public void onSuccess(int statusCode, JSONArray response) {
        LogUtils.w("onSuccess(int statusCode, JSONArray response) was not overriden, but callback was received");
    }

    @Override
    public void onProgress(long currentBytes, long totalBytes) {

    }
}
