package com.cn.greenorange.floatnetstates;


import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.provider.Settings;
import android.widget.Toast;

public class SettingsFragment extends PreferenceFragment implements
        Preference.OnPreferenceClickListener, Preference.OnPreferenceChangeListener {

    private static final String FILE_NAME = "share_prefs";

    private static final String KEY_SPEED_RATE = "preference_speed_rate";
    private static final String KEY_START = "preference_start";
    private static final String KEY_END = "preference_end";
    private static final int OVERLAY_PERMISSION_REQ_CODE = 1234;

    private SharedPreferences sharedPreference;

    private ListPreference preference_speed_rate;
    private Preference preference_start;
    private Preference preference_end;

    private boolean isStart = false;

    public SettingsFragment() {
        // Required empty public constructor
    }


    public static SettingsFragment newInstance() {
        SettingsFragment fragment = new SettingsFragment();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Activity activity = getActivity();
        sharedPreference = getSharedPreference(activity);
        addPreferencesFromResource(R.xml.ic_settings);

        preference_speed_rate = (ListPreference) findPreference(KEY_SPEED_RATE);
        preference_start = findPreference(KEY_START);
        preference_end = findPreference(KEY_END);

        isStart = sharedPreference.getBoolean("isStart", false);
        if (!isStart) {
            getPreferenceScreen().removePreference(preference_end);
        } else {
            getPreferenceScreen().removePreference(preference_start);
        }
        preference_start.setOnPreferenceClickListener(this);
        preference_end.setOnPreferenceClickListener(this);
        preference_speed_rate.setOnPreferenceChangeListener(this);

        updateSpeedRate(preference_speed_rate);

    }

    public static SharedPreferences getSharedPreference(Context context) {
        return context.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE);
    }

    private void startFloatNetStates(Context context) {
        Intent intent = new Intent();
        intent.setClass(context, FloatNetStatesService.class);
        context.startService(intent);
        getActivity().finish();
    }

    private void stopFloatNetStates(Context context) {
        Intent intent = new Intent();
        intent.setClass(context, FloatNetStatesService.class);
        context.stopService(intent);
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object o) {
        if (preference instanceof ListPreference) {
            ListPreference listPreference = (ListPreference) preference;

            //String value = listPreference.getValue();

            int indexOfValue = listPreference.findIndexOfValue((String) o);
            sharedPreference.edit().putInt("speed_rate", indexOfValue).apply();
            updateSpeedRate(listPreference);

        }
        return true;
    }

    private void updateSpeedRate(ListPreference listPreference) {
        int speed_rate = sharedPreference.getInt("speed_rate", 4);
        String speedRate = getResources().getString(R.string.current_speed_rate, preference_speed_rate.getEntries()[speed_rate]);
        listPreference.setSummary(speedRate);
    }

    @Override
    public boolean onPreferenceClick(Preference preference) {
        if (preference == preference_start) {
            isStart = true;
            getPreferenceScreen().removePreference(preference_start);
            getPreferenceScreen().addPreference(preference_end);
            sharedPreference.edit().putBoolean("isStart", isStart).apply();

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                checkForPermission();
            } else {
                startFloatNetStates(getActivity());
            }
            return true;

        } else if (preference == preference_end) {
            isStart = false;
            getPreferenceScreen().removePreference(preference_end);
            getPreferenceScreen().addPreference(preference_start);
            sharedPreference.edit().putBoolean("isStart", isStart).apply();

            stopFloatNetStates(getActivity());
            return true;
        }
        return false;
    }

    @TargetApi(Build.VERSION_CODES.M)
    public void checkForPermission() {
        if (!Settings.canDrawOverlays(getActivity())) {
            Toast.makeText(getActivity(), "当前无权限，请授权！", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:" + getActivity().getPackageName()));
            getActivity().startActivityForResult(intent, OVERLAY_PERMISSION_REQ_CODE);
        } else {
            startFloatNetStates(getActivity());
        }
    }

    @TargetApi(Build.VERSION_CODES.M)
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == OVERLAY_PERMISSION_REQ_CODE) {
            if (!Settings.canDrawOverlays(getActivity())) {
                Toast.makeText(getActivity(), "权限授予失败，无法开启悬浮窗", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(getActivity(), "权限授予成功！", Toast.LENGTH_SHORT).show();
                //启动FloatService
                startFloatNetStates(getActivity());
            }

        }
    }
}
