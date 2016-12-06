package com.tsy.sdk.myokhttp.download_mgr;

import android.support.annotation.NonNull;
import android.util.Log;

import com.tsy.sdk.myokhttp.MyOkHttp;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Locale;

/**
 * 下载管理
 * Created by tangsiyuan on 2016/11/23.
 */

public abstract class AbstractDownloadMgr {

    protected final String TAG = "DownloadMgr";
    protected boolean DEBUG = false;        //调试Log开关

    private MyOkHttp mMyOkHttp;

    private int mMaxDownloadIngNum;        //最大同时下载数量
    private int mCurDownloadIngNum;         //当前同时下载数量
    private long mSaveProgressBytes;        //每下载bytes后保存一次下载进度

    private ArrayList<String> mDownloadTaskQuene;               //下载队列（taskId）
    private HashMap<String, DownloadTask> mDownloadTaskPool;    //下载池

    private DownloadTaskListener mDownloadTaskListener;     //任务监听
    private LinkedList<DownloadTaskListener> mDownloadTaskListenerList;    //task监听事件List

    public static final int DEFAULT_MAX_DOWNLOADING_NUM = 5;                //默认最大同时下载5个
    public static final long DEFAULT_SAVE_PROGRESS_BYTES = 50 * 1024;       //默认每下载50kb保存下载进度

    public static final int DEFAULT_TASK_STATUS_START = 1;        //Task加入下载队列默认状态-开始（如果超过同时下载数量则转为等等）
    public static final int DEFAULT_TASK_STATUS_PAUSE = 2;      //Task加入下载队列默认状态-暂停

    public AbstractDownloadMgr(Builder builder) {
        mMyOkHttp = builder.mMyOkhttp;
        mMaxDownloadIngNum = builder.mMaxDownloadIngNum;
        mSaveProgressBytes = builder.mSaveProgressBytes;

        mCurDownloadIngNum = 0;

        mDownloadTaskQuene = new ArrayList<>();
        mDownloadTaskPool = new HashMap<>();
        mDownloadTaskListenerList = new LinkedList<>();

        mDownloadTaskListener = new DownloadTaskListener() {
            @Override
            public void onStart(String taskId, long completeBytes, long totalBytes) {
                if(DEBUG) {
                    Log.i(TAG, "onStart " + taskId + " startCompleteBytes=" + completeBytes + " totalBytes=" + totalBytes);
                }

                onTaskStart(taskId);

                for(DownloadTaskListener downloadTaskListener : mDownloadTaskListenerList) {
                    if(downloadTaskListener != null) {
                        downloadTaskListener.onStart(taskId, completeBytes, totalBytes);
                    }
                }
            }

            @Override
            public void onProgress(String taskId, long currentBytes, long totalBytes) {
                if(DEBUG) {
                    Log.i(TAG, "onProgress " + taskId + " currentBytes=" + currentBytes + " totalBytes=" + totalBytes);
                }

                if(mDownloadTaskPool.get(taskId).getNextSaveBytes() > mSaveProgressBytes) {     //每mSaveProgressBytes保存一次进度
                    if(DEBUG) {
                        Log.i(TAG, "saveProgress");
                    }

                    mDownloadTaskPool.get(taskId).setNextSaveBytes(0L);
                    saveProgress(taskId, currentBytes, totalBytes);
                }

                for(DownloadTaskListener downloadTaskListener : mDownloadTaskListenerList) {
                    if(downloadTaskListener != null) {
                        downloadTaskListener.onProgress(taskId, currentBytes, totalBytes);
                    }
                }
            }

            @Override
            public void onPause(String taskId, long currentBytes, long totalBytes) {
                if(DEBUG) {
                    Log.i(TAG, "onPause " + taskId + " currentBytes=" + currentBytes + " totalBytes=" + totalBytes);
                    Log.i(TAG, "saveProgress");
                }

                saveProgress(taskId, currentBytes, totalBytes);     //保存一次进度

                onTaskPause(taskId);

                for(DownloadTaskListener downloadTaskListener : mDownloadTaskListenerList) {
                    if(downloadTaskListener != null) {
                        downloadTaskListener.onPause(taskId, currentBytes, totalBytes);
                    }
                }

                //自动开始下一个等待中的任务
                mCurDownloadIngNum --;
                startNextTask();
            }

            @Override
            public void onFinish(String taskId, File file) {
                if(DEBUG) {
                    Log.i(TAG, "onFinish " + taskId + " filePath=" + file.getAbsolutePath());
                }

                //清理任务 移除队列和下载池
                DownloadTask downloadTask = mDownloadTaskPool.get(taskId);
                downloadTask.doDestroy();
                mDownloadTaskQuene.remove(taskId);
                mDownloadTaskPool.remove(taskId);

                onTaskFinish(taskId);

                for(DownloadTaskListener downloadTaskListener : mDownloadTaskListenerList) {
                    if(downloadTaskListener != null) {
                        downloadTaskListener.onFinish(taskId, file);
                    }
                }

                //自动开始下一个等待中的任务
                mCurDownloadIngNum --;
                startNextTask();
            }

            @Override
            public void onFailure(String taskId, String error_msg) {
                if(DEBUG) {
                    Log.w(TAG, "onFailure " + taskId + " " + error_msg);
                }

                onTaskFail(taskId);

                for(DownloadTaskListener downloadTaskListener : mDownloadTaskListenerList) {
                    if(downloadTaskListener != null) {
                        downloadTaskListener.onFailure(taskId, error_msg);
                    }
                }

                //自动开始下一个等待中的任务
                mCurDownloadIngNum --;
                startNextTask();
            }
        };
    }

    /**
     * 初始进入app 恢复所有未完成的任务
     */
    public abstract void resumeTasks();

    /**
     * 保存进度
     * @param taskId taskId
     * @param currentBytes 已经下载的bytes
     * @param totalBytes 总共bytes
     */
    protected abstract void saveProgress(String taskId, long currentBytes, long totalBytes);

    /**
     * 下载任务开始
     * @param taskId task id
     */
    protected abstract void onTaskStart(String taskId);

    /**
     * 下载任务暂停
     * @param taskId task id
     */
    protected abstract void onTaskPause(String taskId);

    /**
     * 下载任务完成
     * @param taskId task id
     */
    protected abstract void onTaskFinish(String taskId);


    /**
     * 下载任务失败
     * @param taskId task id
     */
    protected abstract void onTaskFail(String taskId);

    /**
     * 添加下载任务
     * @param task Task
     */
    public void addTask(Task task) {
        if(DEBUG) {
            Log.i(TAG, "addTask " + task.toString());
        }

        //检查Task参数
        checkTaskArgument(task);

        if(mDownloadTaskPool.containsKey(task.getTaskId())) {       //task已经加过
            if(DEBUG) {
                Log.w(TAG, "addTask contain " + task.getTaskId());
            }
            return ;
        }

        DownloadTask downloadTask = new DownloadTask();
        mDownloadTaskQuene.add(task.getTaskId());
        mDownloadTaskPool.put(task.getTaskId(), downloadTask);

        downloadTask.setMyOkHttp(mMyOkHttp);
        downloadTask.setTaskId(task.getTaskId());
        downloadTask.setUrl(task.getUrl());
        downloadTask.setFilePath(task.getFilePath());
        downloadTask.setCompleteBytes(task.getCompleteBytes());
        downloadTask.setDownloadTaskListener(mDownloadTaskListener);

        if(task.getDefaultStatus() == DEFAULT_TASK_STATUS_START) {
            startTask(task.getTaskId());
        } else if(task.getDefaultStatus() == DEFAULT_TASK_STATUS_PAUSE) {
            pauseTask(task.getTaskId());
        }
    }

    /**
     * 开始任务
     * @param taskId task id
     */
    public void startTask(String taskId) {
        DownloadTask downloadTask = mDownloadTaskPool.get(taskId);
        if(downloadTask == null) {
            return;
        }

        if(DEBUG) {
            Log.i(TAG, "startTask " + taskId);
        }
        
        if(mCurDownloadIngNum < mMaxDownloadIngNum) {
            if(downloadTask.doStart()) {       //初始状态开始
                mCurDownloadIngNum ++;
            }
        } else {
            downloadTask.setStatus(DownloadStatus.STATUS_WAIT);   //超过总下载数量则设置为等待
        }
    }

    /**
     * 开始所有任务
     */
    public void startAllTask() {
        if(DEBUG) {
            Log.i(TAG, "startAllTask");
        }

        for(int i = 0; i < mDownloadTaskQuene.size(); i ++) {
            startTask(mDownloadTaskQuene.get(i));
        }
    }

    /**
     * 暂停任务
     * @param taskId task id
     */
    public void pauseTask(String taskId) {
        DownloadTask downloadTask = mDownloadTaskPool.get(taskId);
        if(downloadTask == null) {
            return;
        }

        if(DEBUG) {
            Log.i(TAG, "pauseTask " + taskId);
        }

        downloadTask.doPause();
    }

    /**
     * 暂停所有任务
     */
    public void pauseAllTask() {
        if(DEBUG) {
            Log.i(TAG, "startAllTask");
        }

        for(int i = 0; i < mDownloadTaskQuene.size(); i ++) {
            pauseTask(mDownloadTaskQuene.get(i));
        }
    }

    /**
     * 删除任务
     * @param taskId 任务id
     */
    public void deleteTask(String taskId) {
        //清理任务 移除队列和下载池
        DownloadTask downloadTask = mDownloadTaskPool.get(taskId);
        downloadTask.doDestroy();
        mDownloadTaskQuene.remove(taskId);
        mDownloadTaskPool.remove(taskId);
    }

    /**
     * 遍历开始查找下一个任务
     */
    private void startNextTask() {
        if(mCurDownloadIngNum >= mMaxDownloadIngNum) {
            return;
        }

        for(int i = 0; i < mDownloadTaskQuene.size(); i ++) {
            DownloadTask downloadTask = mDownloadTaskPool.get(mDownloadTaskQuene.get(i));
            if(downloadTask.getStatus() == DownloadStatus.STATUS_WAIT) {
                if(DEBUG) {
                    Log.i(TAG, "startNextTask " + downloadTask.getTaskId());
                }
                downloadTask.doStart();
            }
        }
    }

    /**
     * 添加下载监听
     * @param downloadTaskListener
     */
    public void addListener(DownloadTaskListener downloadTaskListener) {
        mDownloadTaskListenerList.add(downloadTaskListener);
    }

    /**
     * 移除下载监听
     * @param downloadTaskListener
     */
    public void removeListener(DownloadTaskListener downloadTaskListener) {
        mDownloadTaskListenerList.remove(downloadTaskListener);
    }

    /**
     * 生成taskId yyyyMMddHHmmss+3位随机数字
     * @return
     */
    public String genTaskId() {
        SimpleDateFormat dateformat=new SimpleDateFormat("yyyyMMddHHmmss", Locale.CHINA);
        String datetime = dateformat.format(new Date());
        return datetime + getRandNum() + getRandNum() + getRandNum();
    }

    public void setDebug(boolean debug) {
        DEBUG = debug;
    }

    //获取0~9的随机数
    private int getRandNum() {
        int[] nums = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9};
        int rand = (int) Math.floor(Math.random() * 10);
        return nums[rand];
    }

    //检查Task参数
    private void checkTaskArgument(Task task) {
        if(task.getTaskId().length() == 0) {
            throw new IllegalArgumentException("taskId为空");
        }

        if(task.getUrl().length() == 0) {
            throw new IllegalArgumentException("url为空");
        }

        if(task.getFilePath().length() == 0) {
            throw new IllegalArgumentException("filePath为空");
        }

        if(task.getCompleteBytes() < 0) {
            throw new IllegalArgumentException("非法completeBytes");
        }

        if(task.getDefaultStatus() != DEFAULT_TASK_STATUS_START
                && task.getDefaultStatus() != DEFAULT_TASK_STATUS_PAUSE) {
            throw new IllegalArgumentException("非法defaultStatus");
        }
    }

    public static class Task {
        private String mTaskId;     //task id
        private String mUrl;        //下载url
        private String mFilePath;   //下载保存path
        private long mCompleteBytes;     //断点续传 已经下好的bytes
        private int mDefaultStatus;      //默认加入队列后的下载状态

        public Task() {
            mTaskId = "";
            mUrl = "";
            mFilePath = "";
            mCompleteBytes = 0L;
            mDefaultStatus = DEFAULT_TASK_STATUS_START;     //默认加入队列自动开始
        }

        public String getTaskId() {
            return mTaskId;
        }

        public void setTaskId(@NonNull String taskId) {
            mTaskId = taskId;
        }

        public String getUrl() {
            return mUrl;
        }

        public void setUrl(@NonNull String url) {
            mUrl = url;
        }

        public String getFilePath() {
            return mFilePath;
        }

        public void setFilePath(@NonNull String filePath) {
            mFilePath = filePath;
        }

        public Long getCompleteBytes() {
            return mCompleteBytes;
        }

        public void setCompleteBytes(@NonNull long completeBytes) {
            mCompleteBytes = completeBytes;
        }

        public void setDefaultStatus(int defaultStatus) {
            mDefaultStatus = defaultStatus;
        }

        public int getDefaultStatus() {
            return mDefaultStatus;
        }

        @Override
        public String toString() {
            return "Task{" +
                    "mTaskId='" + mTaskId + '\'' +
                    ", mUrl='" + mUrl + '\'' +
                    ", mFilePath='" + mFilePath + '\'' +
                    ", mCompleteBytes=" + mCompleteBytes +
                    ", mDefaultStatus=" + mDefaultStatus +
                    '}';
        }
    }

    public static abstract class Builder {

        private MyOkHttp mMyOkhttp;
        private int mMaxDownloadIngNum;     //同时最大下载数量
        private long mSaveProgressBytes;    //每下载bytes后保存一次下载进度

        public Builder() {
            mMyOkhttp = new MyOkHttp();
            mMaxDownloadIngNum = DEFAULT_MAX_DOWNLOADING_NUM;
            mSaveProgressBytes = DEFAULT_SAVE_PROGRESS_BYTES;
        }

        public Builder myOkHttp(@NonNull MyOkHttp myOkHttp) {
            mMyOkhttp = myOkHttp;
            return this;
        }

        public Builder maxDownloadIngNum(int maxDownloadIngNum) {
            mMaxDownloadIngNum = maxDownloadIngNum;
            return this;
        }

        public Builder saveProgressBytes(long saveProgressBytes) {
            mSaveProgressBytes = saveProgressBytes;
            return this;
        }

        public abstract AbstractDownloadMgr build();
    }
}
