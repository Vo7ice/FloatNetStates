package com.cn.greenorange.floatnetstates;

import android.content.Context;
import android.net.TrafficStats;
import android.support.annotation.IntDef;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

/**
 * Created by JasWorkSpace on 16/5/23.
 */
public class RealTimeNetStats extends TextView {
    public final static String SETTINGSKEY = "RealTimeNetStatsSETTINGSKEY";

    @IntDef({TOTAL, CELLULAR, WIFI})
    public @interface DATA_TYPE {

    }

    public static final int TOTAL = 0;
    public static final int CELLULAR = 1;
    public static final int WIFI = 2;

    @IntDef({RX, TX, ALL})
    public @interface FLOW_TYPE {

    }

    public static final int RX = 0;
    public static final int TX = 1;
    public static final int ALL = 2;

    private int data_type = TOTAL;
    private int flow_type = RX;

    public RealTimeNetStats(Context context) {
        super(context);
    }

    public RealTimeNetStats(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public RealTimeNetStats(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    private boolean isAttachedToWindow = false;

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        isAttachedToWindow = true;
        startUpdateIfNeed();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        isAttachedToWindow = false;
    }

    @Override
    protected void onVisibilityChanged(View changedView, int visibility) {
        super.onVisibilityChanged(changedView, visibility);
        startUpdateIfNeed();
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        startUpdateIfNeed();
    }

    ///////////////////
    private volatile long mLastTXDataTatal = 0;
    private volatile long mLastUpdateTime = 0;

    private boolean isNeedUpdateUI() {
        return System.currentTimeMillis() - mLastUpdateTime > DELAY_UPDATE;
    }

    private Object mObject = new Object();

    private String getRealTotalNetStats(@FLOW_TYPE int flow_type) {
        long mStart = 0;
        //we only load rx data. no use tx
        long mStartRX = TrafficStats.getTotalRxBytes();
        long mStartTX = TrafficStats.getTotalTxBytes();
        if (mStartRX == TrafficStats.UNSUPPORTED || mStartTX == TrafficStats.UNSUPPORTED
                || mStartRX <= 0) { // make sure its ok.
            return null;
        }
        switch (flow_type) {
            case RX:
                mStart = mStartRX;
                break;
            case TX:
                mStart = mStartTX;
                break;
            case ALL:
                mStart = mStartRX + mStartTX;
                break;
        }
        synchronized (mObject) {
            long mcurrenttime = System.currentTimeMillis();
            double speed = 1000 * (mStart - mLastTXDataTatal) / (mcurrenttime - mLastUpdateTime);
            mLastUpdateTime = mcurrenttime;
            mLastTXDataTatal = mStart;
            return getSpeedString(speed);
        }
    }

    private String getRealCellularNetStats(@FLOW_TYPE int flow_type) {
        long mStart = 0;
        //we only load rx data. no use tx
        long mStartRX = TrafficStats.getMobileRxBytes();
        long mStartTX = TrafficStats.getMobileTxBytes();
        if (mStartRX == TrafficStats.UNSUPPORTED || mStartTX == TrafficStats.UNSUPPORTED
                || mStartRX <= 0) { // make sure its ok.
            return null;
        }
        switch (flow_type) {
            case RX:
                mStart = mStartRX;
                break;
            case TX:
                mStart = mStartTX;
                break;
            case ALL:
                mStart = mStartRX + mStartTX;
                break;
        }
        synchronized (mObject) {
            long mcurrenttime = System.currentTimeMillis();
            double speed = 1000 * (mStart - mLastTXDataTatal) / (mcurrenttime - mLastUpdateTime);
            mLastUpdateTime = mcurrenttime;
            mLastTXDataTatal = mStart;
            return getSpeedString(speed);
        }
    }

    private String getRealWifiNetStats(@FLOW_TYPE int flow_type) {
        long mStart = 0;
        //we only load rx data. no use tx
        long mStartRX = TrafficStats.getMobileRxBytes();
        long mStartTX = TrafficStats.getMobileTxBytes();
        if (mStartRX == TrafficStats.UNSUPPORTED || mStartTX == TrafficStats.UNSUPPORTED
                || mStartRX <= 0) { // make sure its ok.
            return null;
        }
        switch (flow_type) {
            case RX:
                mStart = mStartRX;
                break;
            case TX:
                mStart = mStartTX;
                break;
            case ALL:
                mStart = mStartRX + mStartTX;
                break;
        }
        synchronized (mObject) {
            return calculateSpped(mStart);
        }
    }

    private String calculateSpped(long mStartRX) {
        long mcurrenttime = System.currentTimeMillis();
        double speed = 1000 * (mStartRX - mLastTXDataTatal) / (mcurrenttime - mLastUpdateTime);
        mLastUpdateTime = mcurrenttime;
        mLastTXDataTatal = mStartRX;
        return getSpeedString(speed);
    }

    private String getRealNetStats() {
        String result = null;
        switch (data_type) {
            case TOTAL:
                result = getRealTotalNetStats(flow_type);
                break;

            case CELLULAR:
                result = getRealCellularNetStats(flow_type);
                break;

            case WIFI:
                result = getRealWifiNetStats(flow_type);
                break;
            default:
                break;
        }
        return result;
    }

    private final static double KB = 1024 * 1.0;
    private final static double MB = 1024 * 1024 * 1.0;
    private int DELAY_UPDATE = 1000;//2s

    private String getSpeedString(double speed) {
        if (speed >= MB) {//MB
            return String.format("%1$03.01fMB/s", speed / MB);
        } else {//KB
            return String.format("%1$03.01fKB/s", speed / KB);
        }
    }

    private Runnable updateUIRunnable = new Runnable() {
        @Override
        public void run() {
            try {
                if (isNeedUpdateUI()) {
                    String speed = getRealNetStats();
                    String tag = null;
                    if (getTag() != null) {
                        tag = (String) getTag();
                    }

                    if (!TextUtils.equals(tag, speed)) {
                        setText(speed);
                    }
                }
                startUpdateIfNeed();
            } catch (Throwable e) {
                e.printStackTrace();
            }
        }
    };

    private synchronized void startUpdateIfNeed() {
        if (isAttachedToWindow && getVisibility() == VISIBLE) {
            removeCallbacks(updateUIRunnable);
            postDelayed(updateUIRunnable, DELAY_UPDATE);
        }
    }

    public int getFlowType() {
        return flow_type;
    }

    public void setFlowType(@FLOW_TYPE int flow_type) {
        this.flow_type = flow_type;
    }

    public int getDataType() {
        return data_type;
    }

    public void setDataType(@DATA_TYPE int data_type) {
        this.data_type = data_type;
    }

    public void setType(@FLOW_TYPE int flow_type, @DATA_TYPE int data_type) {
        this.flow_type = flow_type;
        this.data_type = data_type;
    }

    public void setDelayUpdate(int delayUpdate) {
        this.DELAY_UPDATE = delayUpdate;
    }
}
