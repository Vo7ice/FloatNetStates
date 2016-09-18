package com.cn.greenorange.floatnetstates;

import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.IBinder;

public class FloatNetStatesService extends Service {

    FloatWindow mFloatWindow = null;

    public FloatNetStatesService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (null == intent) {
            stopSelf();
        } else {
            createFloatWindow();
        }
        return super.onStartCommand(intent, flags, startId);
    }

    private void createFloatWindow() {
        if (null == mFloatWindow){
            mFloatWindow = new FloatWindow(this);
            mFloatWindow.addToWindow();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (null != mFloatWindow){
            mFloatWindow.removeFromWindow();
            mFloatWindow = null;
            SharedPreferences sharedPreference =
                    SettingsFragment.getSharedPreference(getApplicationContext());
            sharedPreference.edit().putBoolean("isStart",false).apply();
        }
    }
}
