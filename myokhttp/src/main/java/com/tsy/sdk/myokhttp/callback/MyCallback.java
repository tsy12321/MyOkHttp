package com.tsy.sdk.myokhttp.callback;

import android.os.Handler;
import android.os.Looper;

import com.tsy.sdk.myokhttp.response.GsonResponseHandler;
import com.tsy.sdk.myokhttp.response.IResponseHandler;
import com.tsy.sdk.myokhttp.response.JsonResponseHandler;
import com.tsy.sdk.myokhttp.response.RawResponseHandler;
import com.tsy.sdk.myokhttp.util.LogUtils;
import com.google.gson.Gson;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;
import okhttp3.ResponseBody;

/**
 * Created by tsy on 16/9/18.
 */
public class MyCallback implements Callback {

    private IResponseHandler mResponseHandler;

    private static Handler mHandler = new Handler(Looper.getMainLooper());

    public MyCallback(IResponseHandler responseHandler) {
        mResponseHandler = responseHandler;
    }

    @Override
    public void onFailure(Call call, final IOException e) {
        LogUtils.e("onFailure", e);

        mHandler.post(new Runnable() {
            @Override
            public void run() {
                mResponseHandler.onFailure(0, e.toString());
            }
        });
    }

    @Override
    public void onResponse(Call call, final Response response) throws IOException {
        ResponseBody body = response.body();

        try {
            if(response.isSuccessful()) {
                final String response_body = body.string();

                if(mResponseHandler instanceof JsonResponseHandler) {       //json回调
                    try {
                        final JSONObject jsonBody = new JSONObject(response_body);
                        mHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                ((JsonResponseHandler)mResponseHandler).onSuccess(response.code(), jsonBody);
                            }
                        });
                    } catch (JSONException e) {
                        LogUtils.e("onResponse fail parse jsonobject, body=" + response_body);
                        mHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                mResponseHandler.onFailure(response.code(), "fail parse jsonobject, body=" + response_body);
                            }
                        });
                    }
                } else if(mResponseHandler instanceof GsonResponseHandler) {    //gson回调
                    mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                Gson gson = new Gson();
                                ((GsonResponseHandler)mResponseHandler).onSuccess(response.code(),
                                        gson.fromJson(response_body, ((GsonResponseHandler)mResponseHandler).getType()));
                            } catch (Exception e) {
                                LogUtils.e("onResponse fail parse gson, body=" + response_body, e);
                                mResponseHandler.onFailure(response.code(), "fail parse gson, body=" + response_body);
                            }

                        }
                    });
                } else if(mResponseHandler instanceof RawResponseHandler) {     //raw字符串回调
                    mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            ((RawResponseHandler)mResponseHandler).onSuccess(response.code(), response_body);
                        }
                    });
                }
            } else {
                LogUtils.e("onResponse fail status=" + response.code());

                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        mResponseHandler.onFailure(response.code(), "fail status=" + response.code());
                    }
                });
            }
        } finally {
            if(body != null) {
                body.close();
            }
        }
    }
}
