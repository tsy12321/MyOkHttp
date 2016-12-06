package com.tsy.sdk.myokhttp.response;

import com.google.gson.internal.$Gson$Types;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

/**
 * Gson类型的回调接口
 * Created by tsy on 16/8/15.
 */
public abstract class GsonResponseHandler<T> implements IResponseHandler {

    Type mType;

    public GsonResponseHandler() {
        Type myclass = getClass().getGenericSuperclass();    //反射获取带泛型的class
        if (myclass instanceof Class) {
            throw new RuntimeException("Missing type parameter.");
        }
        ParameterizedType parameter = (ParameterizedType) myclass;      //获取所有泛型
        mType = $Gson$Types.canonicalize(parameter.getActualTypeArguments()[0]);  //将泛型转为type
    }

    public final Type getType() {
        return mType;
    }

    public abstract void onSuccess(int statusCode, T response);

    @Override
    public void onProgress(long currentBytes, long totalBytes) {

    }
}
