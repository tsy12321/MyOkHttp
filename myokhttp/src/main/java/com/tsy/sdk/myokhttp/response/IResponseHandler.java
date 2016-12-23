package com.tsy.sdk.myokhttp.response;

import okhttp3.Response;

/**
 * Created by tsy on 16/8/15.
 */
public interface IResponseHandler {

    void onSuccess(Response response);

    void onFailure(int statusCode, String error_msg);

    void onProgress(long currentBytes, long totalBytes);
}
