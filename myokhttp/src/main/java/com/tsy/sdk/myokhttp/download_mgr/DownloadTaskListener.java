package com.tsy.sdk.myokhttp.download_mgr;

import java.io.File;

/**
 * Created by tsy on 2016/11/24.
 */

public interface DownloadTaskListener {
    /**
     * 任务开始
     * @param taskId task id
     * @param completeBytes 断点续传 已经完成的bytes
     * @param totalBytes total bytes
     */
    void onStart(String taskId, long completeBytes, long totalBytes);

    /**
     * 任务下载中
     * @param taskId task id
     * @param currentBytes 当前已经下载的bytes
     * @param totalBytes total bytes
     */
    void onProgress(String taskId, long currentBytes, long totalBytes);

    /**
     * 任务暂停
     * @param taskId task id
     * @param currentBytes 当前已经下载的bytes
     * @param totalBytes total bytes
     */
    void onPause(String taskId, long currentBytes, long totalBytes);

    /**
     * 任务完成
     * @param taskId task id
     * @param file 下载完成后的file
     */
    void onFinish(String taskId, File file);

    /**
     * 任务失败
     * @param taskId task id
     * @param error_msg error_msg
     */
    void onFailure(String taskId, String error_msg);
}
