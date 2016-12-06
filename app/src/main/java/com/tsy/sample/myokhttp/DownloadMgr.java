package com.tsy.sample.myokhttp;

import android.util.Log;

import com.tsy.sdk.myokhttp.download_mgr.AbstractDownloadMgr;

/**
 * Created by tsy on 2016/12/6.
 */

public class DownloadMgr extends AbstractDownloadMgr {

    private DownloadMgr(Builder builder) {
        super(builder);
    }

    /**
     * 初始进入app 恢复所有未完成的任务
     */
    @Override
    public void resumeTasks() {
        if(DEBUG) {
            Log.i(TAG, "start resumeTasks");
        }
        // TODO: 2016/12/6 读取所有本地数据库未完成任务信息
    }

    /**
     * 保存进度
     * @param taskId taskId
     * @param currentBytes 已经下载的bytes
     * @param totalBytes 总共bytes
     */
    @Override
    protected void saveProgress(String taskId, long currentBytes, long totalBytes) {
        // TODO: 2016/12/6 保存本地数据库任务信息
    }

    /**
     * 下载任务开始
     * @param taskId task id
     */
    @Override
    protected void onTaskStart(String taskId) {
        // TODO: 2016/12/6 保存本地数据库任务信息
    }

    /**
     * 下载任务暂停
     * @param taskId task id
     */
    @Override
    protected void onTaskPause(String taskId) {
        // TODO: 2016/12/6 保存本地数据库任务信息
    }

    /**
     * 下载任务完成
     * @param taskId task id
     */
    @Override
    protected void onTaskFinish(String taskId) {
        // TODO: 2016/12/6 保存本地数据库任务信息
    }

    /**
     * 下载任务失败
     * @param taskId task id
     */
    @Override
    protected void onTaskFail(String taskId) {
        // TODO: 2016/12/6 保存本地数据库任务信息
    }

    public static class Builder extends AbstractDownloadMgr.Builder {

        @Override
        public AbstractDownloadMgr build() {
            return new DownloadMgr(this);
        }
    }
}
