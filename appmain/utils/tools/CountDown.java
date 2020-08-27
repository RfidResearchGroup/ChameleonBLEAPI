package com.proxgrind.chameleon.utils.tools;

import java.util.Timer;
import java.util.TimerTask;

/*
 * 倒计时任务!
 * */
public class CountDown extends TimerTask {

    private int timeDelay = 1000;
    private transient int countdynamic = 0;
    private Progress mProgress;
    private Timer timer;

    public CountDown(Progress progress) {
        timer = new Timer();
        mProgress = progress;
    }

    //倒计时回调接口
    public interface Progress {
        boolean onProgress(int countdynamic);
    }

    //设置回调!
    public void setProgress(Progress progress) {
        mProgress = progress;
    }

    //设置每次的延迟
    public CountDown setTimeDelay(int ms) {
        timeDelay = ms;
        return this;
    }

    //设置需要延迟多少次!
    public CountDown setCountDown(int count) {
        countdynamic = count;
        return this;
    }

    //开始倒计时!
    public CountDown startCountDown() {
        if (countdynamic == 0) return this;
        timer.schedule(this, 0, timeDelay);
        return this;
    }

    //取消倒计时
    public CountDown cancelCountDown() {
        cancel();
        countdynamic = 0;
        return this;
    }

    //是否处于倒计时状态!
    public boolean isRunning() {
        return this.countdynamic != 0;
    }

    @Override
    public void run() {
        //判断为零，立刻回调结束
        if (countdynamic == 0) {
            mProgress.onProgress(0);
            cancelCountDown();
        } else {
            //回调且判断需不需要继续下次回调!
            if (!mProgress.onProgress(countdynamic)) {
                cancelCountDown();
                return;
            }
            --countdynamic;
        }
    }
}
