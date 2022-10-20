package com.han.devtool;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.han.activitytracker.AccessibilityUtil;
import com.han.activitytracker.TrackerService;
import com.han.cpu.CPUService;
import com.han.fps.FPSService;
import com.han.log.LogService;
import com.nolanlawson.logcat.helper.SuperUserHelper;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import ezy.assist.compat.RomUtil;
import ezy.assist.compat.SettingsCompat;

public class MainActivity extends Activity implements View.OnClickListener {

    private TextView tv = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        tv = (TextView) findViewById(R.id.info);

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {

                    tv.setMovementMethod(ScrollingMovementMethod.getInstance());
                    final String txt = readString("/system/build.prop");
                    if (!TextUtils.isEmpty(txt)) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                tv.setText(txt);
                            }
                        });
                    } else {
                        Log.e("sanbo", "read /system/build.prop failed!");
                    }

                } catch (Throwable e) {
                    Log.e("sanbo", Log.getStackTraceString(e));
                }
            }
        }).start();
    }



    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btnFps: {
                startService(new Intent(this, FPSService.class).putExtra(FPSService.FPS_COMMAND, FPSService.FPS_COMMAND_OPEN));
                break;
            }
            case R.id.btnTopActivity: {
                if (AccessibilityUtil.checkAccessibility(this)) {
                    startService(new Intent(this, TrackerService.class).putExtra(TrackerService.Tracker_COMMAND, TrackerService.Tracker_COMMAND_OPEN));
                }
                break;
            }

            case R.id.btnMemory: {
                startService(new Intent(this, CPUService.class).putExtra(CPUService.CPU_COMMAND, CPUService.CPU_COMMAND_OPEN));
                break;
            }

            case R.id.btnRequestRoot: {
                SuperUserHelper.requestRoot(this);
                break;
            }

            case R.id.button5: {
                startService(new Intent(this, LogService.class).putExtra(LogService.COMMAND, LogService.COMMAND_OPEN));
                break;
            }

            case R.id.manage:
                SettingsCompat.manageDrawOverlays(this);
                break;

            case R.id.toggle:
                boolean granted1 = SettingsCompat.canDrawOverlays(this);
                SettingsCompat.setDrawOverlays(this, !granted1);
                boolean granted2 = SettingsCompat.canDrawOverlays(this);
                Toast.makeText(this, RomUtil.getVersion() + "\n" + RomUtil.getName() + "\ngranted: " + granted2, Toast.LENGTH_LONG).show();
                break;

        }
    }

    public static String readString(String file) {
        InputStream input = null;
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        try {
            input = new FileInputStream(new File(file));
            byte[] buffer = new byte[1024 * 4];
            int n;
            while (-1 != (n = input.read(buffer))) {
                output.write(buffer, 0, n);
            }
            output.flush();
            return output.toString("UTF-8");
        } catch (IOException e) {
        } finally {
            if (input != null) {
                try {
                    input.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (output != null) {
                try {
                    output.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return "";
    }
}
