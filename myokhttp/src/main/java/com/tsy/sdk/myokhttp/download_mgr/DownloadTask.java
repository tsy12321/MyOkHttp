package com.tsy.sdk.myokhttp.download_mgr;

import com.tsy.sdk.myokhttp.MyOkHttp;
import com.tsy.sdk.myokhttp.response.DownloadResponseHandler;

import java.io.File;

import okhttp3.Call;

/**
 * Created by tangsiyuan on 2016/11/23.
 */

public class DownloadTask {

    private MyOkHttp mMyOkHttp;

    private String mTaskId;     //task id
    private String mUrl;        //下载url
    private String mFilePath;   //保存文件path
    private long mCompleteBytes;    //断点续传 已经完成的bytes
    private long mCurrentBytes;     //当前总共下载了的bytes
    private long mTotalBytes;       //文件总bytes
    private int mStatus;       //Task状态
    private long mNextSaveBytes = 0L;       //距离下次保存下载进度的bytes

    private Call mCall;     //本次请求
    private DownloadTaskListener mDownloadTaskListener;    //task监听事件
    private DownloadResponseHandler mDownloadResponseHandler;       //下载监听

    public DownloadTask() {

        mTaskId = "";
        mUrl = "";
        mFilePath = "";
        mCompleteBytes = 0L;
        mCurrentBytes = 0L;
        mTotalBytes = 0L;
        mStatus = DownloadStatus.STATUS_DEFAULT;       //初始默认状态
        mNextSaveBytes = 0L;

        //myokhttp的下载监听
        mDownloadResponseHandler = new DownloadResponseHandler() {
            @Override
            public void onStart(long totalBytes) {
                mTotalBytes = mCompleteBytes + totalBytes;      //下载总bytes等于上次下载的bytes加上这次断点续传的总bytes

                mDownloadTaskListener.onStart(mTaskId, mCompleteBytes, mTotalBytes);
            }

            @Override
            public void onFinish(File download_file) {
                mStatus = DownloadStatus.STATUS_FINISH;
                mCurrentBytes = mTotalBytes;
                mCompleteBytes = mTotalBytes;
                mDownloadTaskListener.onFinish(mTaskId, download_file);
            }

            @Override
            public void onProgress(long currentBytes, long totalBytes) {
                if(mStatus == DownloadStatus.STATUS_DOWNLOADING) {
                    mNextSaveBytes += mCompleteBytes + currentBytes - mCurrentBytes;        //叠加每次增加的bytes
                    mCurrentBytes = mCompleteBytes + currentBytes;      //当前已经下载好的bytes
                    mDownloadTaskListener.onProgress(mTaskId, mCurrentBytes, mTotalBytes);
                } else if(mStatus == DownloadStatus.STATUS_PAUSE) {
                    mCompleteBytes = mCurrentBytes;
                    if(!mCall.isCanceled()) {
                        mCall.cancel();
                    }
                } else {
                    mCompleteBytes = mCurrentBytes;
                    if(!mCall.isCanceled()) {
                        mCall.cancel();
                    }
                }
            }

            @Override
            public void onCancel() {
                mDownloadTaskListener.onPause(mTaskId, mCurrentBytes, mTotalBytes);
            }

            @Override
            public void onFailure(String error_msg) {
                mStatus = DownloadStatus.STATUS_FAIL;

                mDownloadTaskListener.onFailure(mTaskId, error_msg);
            }
        };
    }

    /**
     * 开始下载
     * @return
     */
    public boolean doStart() {
        if(mStatus == DownloadStatus.STATUS_DOWNLOADING || mStatus == DownloadStatus.STATUS_FINISH) {
            return false;
        }

        mStatus = DownloadStatus.STATUS_DOWNLOADING;

        mCall = mMyOkHttp.download()
                .url(mUrl)
                .filePath(mFilePath)
                .setCompleteBytes(mCompleteBytes)
                .enqueue(mDownloadResponseHandler);

        return true;
    }

    /**
     * 暂停下载
     */
    public void doPause() {
        if(mStatus == DownloadStatus.STATUS_PAUSE || mStatus == DownloadStatus.STATUS_FINISH) {
            return;
        }

        if(mStatus == DownloadStatus.STATUS_DOWNLOADING) {
            mStatus = DownloadStatus.STATUS_PAUSE;
            mCall.cancel();
        } else {
            mStatus = DownloadStatus.STATUS_PAUSE;
        }
    }

    public void doDestroy() {
        mDownloadTaskListener = null;
        mDownloadResponseHandler = null;

        if(mCall != null) {
            if(!mCall.isCanceled()) {
                mCall.cancel();
            }
            mCall = null;
        }
    }

    public MyOkHttp getMyOkHttp() {
        return mMyOkHttp;
    }

    public void setMyOkHttp(MyOkHttp myOkHttp) {
        mMyOkHttp = myOkHttp;
    }

    public String getTaskId() {
        return mTaskId;
    }

    public void setTaskId(String taskId) {
        mTaskId = taskId;
    }

    public String getUrl() {
        return mUrl;
    }

    public void setUrl(String url) {
        mUrl = url;
    }

    public String getFilePath() {
        return mFilePath;
    }

    public void setFilePath(String filePath) {
        mFilePath = filePath;
    }

    public Long getCompleteBytes() {
        return mCompleteBytes;
    }

    public void setCompleteBytes(Long completeBytes) {
        mCompleteBytes = completeBytes;
        mCurrentBytes = mCompleteBytes;
    }

    public Long getTotalBytes() {
        return mTotalBytes;
    }

    public int getStatus() {
        return mStatus;
    }

    public void setStatus(int status) {
        mStatus = status;
    }

    public long getCurrentBytes() {
        return mCurrentBytes;
    }

    public void setCurrentBytes(long currentBytes) {
        mCurrentBytes = currentBytes;
    }

    public long getNextSaveBytes() {
        return mNextSaveBytes;
    }

    public void setNextSaveBytes(long nextSaveBytes) {
        mNextSaveBytes = nextSaveBytes;
    }

    public void setDownloadTaskListener(DownloadTaskListener downloadTaskListener) {
        mDownloadTaskListener = downloadTaskListener;
    }
}
