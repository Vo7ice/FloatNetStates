package com.cn.greenorange.floatnetstates;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.PixelFormat;
import android.net.TrafficStats;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.view.WindowManager.LayoutParams;
import android.widget.GridLayout;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;

/**
 * Created by guojin.hu on 2016/9/13.
 */

public class FloatWindow {

    private static final String TAG = "FloatWindow";
    private static final int RECORD = 0;
    private WindowManager mWm;
    private int mStatusBarHeight;
    private DisplayMetrics mDm;

    private Context mContext;
    private LayoutParams mWmParams;
    private GridLayout mRootView = null;

    private RealTimeNetStats mTotalRx;
    private RealTimeNetStats mTotalTx;
    private RealTimeNetStats mTotal;

    private RealTimeNetStats mCellularTx;
    private RealTimeNetStats mCellularRx;
    private RealTimeNetStats mCellularTotal;

    private RecordRunnable mRecordRunnable;
    private Handler mHandler;

    private int mUpdateTime;

    public FloatWindow(Context context) {
        mContext = context;
        mWm = (WindowManager) mContext.getSystemService(Context.WINDOW_SERVICE);
        mStatusBarHeight = getStatusBarHeight();
        mDm = new DisplayMetrics();
        mWm.getDefaultDisplay().getMetrics(mDm);

        mHandler = new Handler();
        SharedPreferences sharedPreference =
                SettingsFragment.getSharedPreference(context);
        int index = sharedPreference.getInt("speed_rate", 4);
        mUpdateTime = Integer.valueOf(mContext.getResources().getStringArray(R.array.speed_rate)[index]);
    }

    private int getStatusBarHeight() {
        int result = 0;
        int resourceId = mContext.getResources().getIdentifier("status_bar_height", "dimen", "android");
        if (resourceId > 0) {
            result = mContext.getResources().getDimensionPixelSize(resourceId);
        }
        return result;
    }

    public void addToWindow() {
        mWm = (WindowManager) mContext.getApplicationContext().getSystemService(
                Context.WINDOW_SERVICE);
        mWmParams = createLayoutParams();
        mRootView = (GridLayout) LayoutInflater.from(mContext).inflate(R.layout.float_net_states, null);
        mTotalTx = (RealTimeNetStats) mRootView.findViewById(R.id.text_total_tx);
        mTotalRx = (RealTimeNetStats) mRootView.findViewById(R.id.text_total_rx);
        mCellularTx = (RealTimeNetStats) mRootView.findViewById(R.id.text_cellular_tx);
        mCellularRx = (RealTimeNetStats) mRootView.findViewById(R.id.text_cellular_rx);
        mTotal = (RealTimeNetStats) mRootView.findViewById(R.id.text_total);
        mCellularTotal = (RealTimeNetStats) mRootView.findViewById(R.id.text_cellular_total);

        //设置数据类型和流量类型
        mTotalTx.setType(RealTimeNetStats.TX, RealTimeNetStats.TOTAL);
        mTotalRx.setType(RealTimeNetStats.RX, RealTimeNetStats.TOTAL);
        mCellularTx.setType(RealTimeNetStats.TX, RealTimeNetStats.CELLULAR);
        mCellularRx.setType(RealTimeNetStats.RX, RealTimeNetStats.CELLULAR);
        mTotal.setType(RealTimeNetStats.ALL, RealTimeNetStats.TOTAL);
        mCellularTotal.setType(RealTimeNetStats.ALL, RealTimeNetStats.TOTAL);

        //设置刷新时间
        mTotalTx.setDelayUpdate(mUpdateTime);
        mTotalRx.setDelayUpdate(mUpdateTime);
        mCellularTx.setDelayUpdate(mUpdateTime);
        mCellularRx.setDelayUpdate(mUpdateTime);
        mTotal.setDelayUpdate(mUpdateTime);
        mCellularTotal.setDelayUpdate(mUpdateTime);

        mWm.addView(mRootView, mWmParams);

        startRecord(System.currentTimeMillis());

    }

    private LayoutParams createLayoutParams() {
        LayoutParams wmParams = new LayoutParams();
        wmParams.type = LayoutParams.TYPE_SYSTEM_ALERT;//set type
        wmParams.flags = LayoutParams.FLAG_NOT_TOUCH_MODAL | LayoutParams.FLAG_NOT_FOCUSABLE
                | LayoutParams.FLAG_KEEP_SCREEN_ON;
        wmParams.gravity = Gravity.TOP | Gravity.START;
        wmParams.format = PixelFormat.RGBA_8888;
        wmParams.x = 0;
        wmParams.y = 0;
        wmParams.width = 350;
        wmParams.height = 200;
        return wmParams;
    }

    public void removeFromWindow() {
        if (null != mRootView) {
            mWm.removeView(mRootView);
            mRootView = null;
            stopRecord();
        }
    }

    public void startRecord(long time) {
        File dir = new File(Environment.getExternalStorageDirectory().getPath(), String.valueOf(time));
        if (!dir.exists()) {
            dir.mkdirs();
        }
        new Thread(mRecordRunnable = new RecordRunnable(dir.getAbsolutePath())).start();
        //mHandler.post(mRecordRunnable);
    }

    public void stopRecord() {
        if (mRecordRunnable.isNeedRecord()) {
            mRecordRunnable.setNeedRecord(false);
            //mHandler.removeCallbacksAndMessages(null);
        }
    }

    private long mLastTotalRX = 0;
    private long mLastTotalTX = 0;
    private long mLastMobileRX = 0;
    private long mLastMobileTX = 0;


    private class RecordRunnable implements Runnable {
        private String dirPath;
        private long lastTime;

        public boolean isNeedRecord() {
            return needRecord;
        }

        public void setNeedRecord(boolean needRecord) {
            this.needRecord = needRecord;
        }

        private boolean needRecord = true;

        public RecordRunnable(String dirPath) {
            this.dirPath = dirPath;
            lastTime = System.currentTimeMillis();
        }

        @Override
        public void run() {
            while (needRecord) {
                //File data = new File(dirPath, String.valueOf(System.currentTimeMillis()));
                //data.mkdirs();
                File rxFile = new File(dirPath, "RX.txt");
                File txFile = new File(dirPath, "TX.txt");
                try {
                    if (!rxFile.exists() || !txFile.exists()) {
                        rxFile.createNewFile();
                        txFile.createNewFile();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
                //Log.d("file","rxFile-->"+rxFile.exists()+",txFile-->"+txFile.exists());
                FileWriter rxFw = null;
                FileWriter txFw = null;
                try {
                    rxFw = new FileWriter(rxFile, true);
                    txFw = new FileWriter(txFile, true);
                    long totalRx = TrafficStats.getTotalRxBytes();
                    long totalTx = TrafficStats.getTotalTxBytes();
                    long mobileRx = TrafficStats.getMobileRxBytes();
                    long mobileTx = TrafficStats.getMobileTxBytes();
                    if (totalRx == TrafficStats.UNSUPPORTED || totalTx == TrafficStats.UNSUPPORTED
                            || mobileRx == TrafficStats.UNSUPPORTED || mobileTx == TrafficStats.UNSUPPORTED
                            || totalRx <= 0 || totalTx <= 0 || mobileRx <= 0 || mobileTx <= 0) { // make sure its ok.
                        break;
                    }
                    long mcurrenttime = System.currentTimeMillis();
                    double totalRxSpeed = 1000 * (totalRx - mLastTotalRX) / mUpdateTime;
                    double totalTxSpeed = 1000 * (totalTx - mLastTotalTX) / mUpdateTime;
                    double mobileRxSpeed = 1000 * (mobileRx - mLastMobileRX) / mUpdateTime;
                    double mobileTxSpeed = 1000 * (mobileTx - mLastMobileTX) / mUpdateTime;
                    mLastTotalRX = totalRx;
                    mLastTotalTX = totalTx;
                    mLastMobileRX = mobileRx;
                    mLastMobileTX = mobileTx;
                    rxFw.write(String.valueOf(totalRxSpeed) + "\t" + String.valueOf(mobileRxSpeed) + "\t\n");
                    txFw.write(String.valueOf(totalTxSpeed) + "\t" + String.valueOf(mobileTxSpeed) + "\t\n");
                    rxFw.flush();
                    txFw.flush();
                    Thread.sleep(mUpdateTime);
                } catch (Throwable e) {
                    e.printStackTrace();
                } finally {
                    try {
                        if (null != rxFw) {
                            rxFw.close();
                        }
                        if (null != txFw) {
                            txFw.close();
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }

            }
        }
    }
}
