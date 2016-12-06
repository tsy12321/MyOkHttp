package com.tsy.sample.myokhttp;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.tsy.sdk.myokhttp.download_mgr.AbstractDownloadMgr;
import com.tsy.sdk.myokhttp.download_mgr.DownloadStatus;
import com.tsy.sdk.myokhttp.download_mgr.DownloadTask;
import com.tsy.sdk.myokhttp.download_mgr.DownloadTaskListener;

import java.io.File;

/**
 * 下载管理Demo代码 仅供参考 不可运行
 */
public class DownloadMgrActivity extends AppCompatActivity implements View.OnClickListener {

    TextView txt_progress;
    Button btn_status;

    private DownloadTask mDownloadTask;
    private DownloadMgr mDownloadMgr;
    private DownloadTaskListener mDownloadTaskListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_download_mgr);

        txt_progress = (TextView) findViewById(R.id.txt_progress);
        btn_status = (Button) findViewById(R.id.btn_status);

        btn_status.setOnClickListener(this);

        mDownloadMgr = MyApplication.getInstance().getDownloadMgr();
    }

    @Override
    public void onClick(View v) {
        if(mDownloadTask == null) {     //还没有加入任务
            AbstractDownloadMgr.Task task = new AbstractDownloadMgr.Task();
            task.setTaskId(mDownloadMgr.genTaskId());
            task.setUrl("This is your url");
            task.setFilePath("This is your save file path");
            task.setCompleteBytes(0L);      //如果新加任务的可以不设置 默认是0L（当恢复任务的时候需要设置已经完成的bytes 从本地读取）
            task.setDefaultStatus(DownloadMgr.DEFAULT_TASK_STATUS_START);       //默认添加进对了自动开始
            mDownloadTask = mDownloadMgr.addTask(task);
        } else {
            switch (mDownloadTask.getStatus()) {
                case DownloadStatus.STATUS_DOWNLOADING:         //暂停任务
                    mDownloadMgr.pauseTask(mDownloadTask.getTaskId());
                    break;

                case DownloadStatus.STATUS_PAUSE:       //开始任务
                    mDownloadMgr.startTask(mDownloadTask.getTaskId());
                    break;

                default:
                    break;
            }
        }
    }

    @Override
    protected void onStart() {
        super.onStart();

        //显示activity时加入监听
        mDownloadTaskListener = new DownloadTaskListener() {
            @Override
            public void onStart(String taskId, long completeBytes, long totalBytes) {
                mDownloadTask = mDownloadMgr.getDownloadTask(taskId);

                btn_status.setText("暂停");
            }

            @Override
            public void onProgress(String taskId, long currentBytes, long totalBytes) {
                mDownloadTask = mDownloadMgr.getDownloadTask(taskId);

                //ps:建议不要每次刷新 可以通过handler postDelay延时刷新 防止刷新频率过快
                int percent = (int) (((float)currentBytes/totalBytes) * 100);
                txt_progress.setText(percent + "%");
            }

            @Override
            public void onPause(String taskId, long currentBytes, long totalBytes) {
                mDownloadTask = mDownloadMgr.getDownloadTask(taskId);

                btn_status.setText("继续");
            }

            @Override
            public void onFinish(String taskId, File file) {
                mDownloadTask = mDownloadMgr.getDownloadTask(taskId);

                txt_progress.setText("100%");
                btn_status.setText("已完成");
            }

            @Override
            public void onFailure(String taskId, String error_msg) {
                mDownloadTask = mDownloadMgr.getDownloadTask(taskId);

                btn_status.setText("下载失败");
            }
        };

        mDownloadMgr.addListener(mDownloadTaskListener);
    }

    @Override
    protected void onStop() {
        super.onStop();

        //不显示时移除监听 放置内存泄露
        mDownloadMgr.removeListener(mDownloadTaskListener);
        mDownloadTaskListener = null;
    }
}
