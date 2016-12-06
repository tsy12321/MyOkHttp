package com.tsy.sdk.myokhttp.builder;

import com.tsy.sdk.myokhttp.MyOkHttp;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 带有param的base request body
 * Created by tsy on 2016/12/6.
 */

public abstract class OkHttpRequestBuilderHasParam<T extends OkHttpRequestBuilderHasParam> extends OkHttpRequestBuilder<T> {

    protected Map<String, String> mParams;

    public OkHttpRequestBuilderHasParam(MyOkHttp myOkHttp) {
        super(myOkHttp);
    }

    /**
     * set Map params
     * @param params
     * @return
     */
    public T params(Map<String, String> params) {
        this.mParams = params;
        return (T) this;
    }

    /**
     * add param
     * @param key param key
     * @param val param val
     * @return
     */
    public T addParam(String key, String val) {
        if (this.mParams == null)
        {
            mParams = new LinkedHashMap<>();
        }
        mParams.put(key, val);
        return (T) this;
    }
}
