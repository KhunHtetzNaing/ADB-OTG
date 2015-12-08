package com.mrkid.flashbot;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbInterface;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.cgutman.adblib.AdbBase64;
import com.cgutman.adblib.AdbConnection;
import com.cgutman.adblib.AdbCrypto;
import com.cgutman.adblib.UsbChannel;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Set;

import static com.mrkid.flashbot.Message.CONNECTING;
import static com.mrkid.flashbot.Message.DEVICE_FOUND;
import static com.mrkid.flashbot.Message.DEVICE_NOT_FOUND;
import static com.mrkid.flashbot.Message.FLASHING;
import static com.mrkid.flashbot.Message.INSTALLING_PROGRESS;

public class MainActivity extends ActionBarActivity {


    private Handler handler;

    private TextView textView;

    private TextView hintView;

    private Button flashButton;

    private Button setupButton;

    private UsbDevice mDevice;

    private ImageView imageView;

    private ProgressBar progressBar;

    private AdbCrypto adbCrypto;

    private AdbConnection adbConnection;

    // total apks to be flashed
    private int total;

    // which apk is being flashed
    private int current = 0;

    private int currentProgress = 0;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        textView = (TextView) findViewById(R.id.textView);
        hintView = (TextView) findViewById(R.id.hintView);


        flashButton = (Button) findViewById(R.id.flashButton);
        flashButton.setEnabled(false);

        setupButton = (Button) findViewById(R.id.setupButton);

        setupButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, SettingActivity.class);
                startActivity(intent);
            }
        });

        imageView = (ImageView) findViewById(R.id.imageView);

        progressBar = (ProgressBar) findViewById(R.id.progressBar);

        handler = new Handler() {
            @Override
            public void handleMessage(android.os.Message msg) {

                switch (msg.what) {
                    case DEVICE_FOUND:
                        flashButton.setEnabled(true);
                        flashButton.setBackgroundResource(R.drawable.button_ready);
                        flashButton.setText(R.string.ready_flash_apks);

                        textView.setVisibility(View.VISIBLE);
                        textView.setText(0 + "/" + total);

                        hintView.setVisibility(View.GONE);

                        imageView.setVisibility(View.INVISIBLE);
                        progressBar.setVisibility(View.INVISIBLE);

                        break;

                    case CONNECTING:
                        flashButton.setEnabled(true);
                        flashButton.setBackgroundResource(R.drawable.button_waiting);
                        flashButton.setText(R.string.connecting);

                        textView.setVisibility(View.GONE);

                        hintView.setVisibility(View.VISIBLE);
                        hintView.setText(R.string.connecting_hint);

                        imageView.setVisibility(View.INVISIBLE);
                        progressBar.setVisibility(View.INVISIBLE);
                        break;

                    case DEVICE_NOT_FOUND:

                        textView.setVisibility(View.VISIBLE);
                        textView.setText(0 + "/" + total);

                        hintView.setVisibility(View.GONE);

                        flashButton.setEnabled(false);
                        flashButton.setBackgroundResource(R.drawable.button_waiting);
                        flashButton.setText(R.string.waiting_device);
                        imageView.setVisibility(View.INVISIBLE);
                        progressBar.setVisibility(View.INVISIBLE);
                        break;

                    case FLASHING:

                        flashButton.setEnabled(false);
                        flashButton.setBackgroundResource(R.drawable.button_working);
                        flashButton.setText(R.string.flashing_apks);
                        imageView.setVisibility(View.VISIBLE);
                        progressBar.setVisibility(View.VISIBLE);

                        break;

                    case INSTALLING_PROGRESS:
                        int step = msg.arg1;
                        int progress = msg.arg2;

                        if (step == Message.PUSH_PART) {
                            currentProgress = (int) (progress * Const.PUSH_PERCENT);
                        } else if (step == Message.PM_INST_PART) {
                            currentProgress = (int) (100 * Const.PUSH_PERCENT + (1 - Const.PUSH_PERCENT) * progress);
                        }
                        progressBar.setProgress(currentProgress);
                        break;

                }
            }
        };

        flashButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                handler.sendEmptyMessage(Message.FLASHING);
                new Thread() {
                    @Override
                    public void run() {

                        Set<String> selectedPackageNames = Config.getApks(MainActivity.this);

                        current = 0;

                        final PackageManager packageManager = MainActivity.this.getPackageManager();
                        List<PackageInfo> packages = packageManager.getInstalledPackages(0);
                        for (final PackageInfo packageInfo : packages) {
                            if (selectedPackageNames.contains(packageInfo.packageName)) {

                                current++;
                                handler.post(new Runnable() {
                                    @Override
                                    public void run() {
                                        textView.setText(current + "/" + total);

                                        Drawable image = packageInfo.applicationInfo.loadIcon(packageManager);

                                        imageView.setBackground(image);
                                    }
                                });

                                File local = new File(packageInfo.applicationInfo.sourceDir);
                                String remotePath = "/data/local/tmp/" + local.getName();
                                try {
                                    new Push(adbConnection, local, remotePath).execute(handler);
                                    new Install(adbConnection, remotePath, local.length() / 1024).execute(handler);
                                } catch (Exception e) {
                                    Log.w(Const.TAG, "exception caught", e);
                                }
                            }
                        }

                        if (adbConnection != null) {
                            handler.sendEmptyMessage(Message.DEVICE_FOUND);
                        } else {
                            handler.sendEmptyMessage(Message.DEVICE_NOT_FOUND);
                        }

                        handler.post(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(MainActivity.this, "DONE", Toast.LENGTH_SHORT).show();
                            }
                        });

                    }

                }.start();
            }
        });


        AdbBase64 base64 = new MyAdbBase64();
        try {
            adbCrypto = AdbCrypto.loadAdbKeyPair(base64, new File(getFilesDir(), "private_key"), new File(getFilesDir(), "public_key"));
        } catch (Exception e) {
        }

        if (adbCrypto == null) {

            try {
                adbCrypto = AdbCrypto.generateAdbKeyPair(base64);
                adbCrypto.saveAdbKeyPair(new File(getFilesDir(), "private_key"), new File(getFilesDir(), "public_key"));
            } catch (Exception e) {
                Log.w(Const.TAG, "fail to generate and save key-pair", e);
            }
        }


        IntentFilter filter = new IntentFilter();
        filter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
        registerReceiver(mUsbReceiver, filter);

        asyncRefreshAdbConnection((UsbDevice) getIntent().getParcelableExtra(UsbManager.EXTRA_DEVICE));
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            Intent intent = new Intent(this, SettingActivity.class);
            startActivity(intent);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);

        asyncRefreshAdbConnection((UsbDevice) intent.getParcelableExtra(UsbManager.EXTRA_DEVICE));
    }

    public void asyncRefreshAdbConnection(final UsbDevice device) {
        if (device != null) {
            new Thread() {
                @Override
                public void run() {
                    final UsbInterface intf = findAdbInterface(device);
                    try {
                        setAdbInterface(device, intf);
                    } catch (Exception e) {
                        Log.w(Const.TAG, "setAdbInterface(device, intf) fail", e);
                    }

                }
            }.start();
        }
    }

    BroadcastReceiver mUsbReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            Log.d(Const.TAG, "mUsbReceiver onReceive");
            String action = intent.getAction();

            if (UsbManager.ACTION_USB_DEVICE_DETACHED.equals(action)) {
                UsbDevice device = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                String deviceName = device.getDeviceName();
                if (mDevice != null && mDevice.getDeviceName().equals(deviceName)) {
                    try {
                        Log.d(Const.TAG, "setAdbInterface(null, null)");
                        setAdbInterface(null, null);
                    } catch (Exception e) {
                        Log.w(Const.TAG, "setAdbInterface(null,null) failed", e);
                    }
                }
            }
        }
    };

    // searches for an adb interface on the given USB device
    private UsbInterface findAdbInterface(UsbDevice device) {
        int count = device.getInterfaceCount();
        for (int i = 0; i < count; i++) {
            UsbInterface intf = device.getInterface(i);
            if (intf.getInterfaceClass() == 255 && intf.getInterfaceSubclass() == 66 &&
                    intf.getInterfaceProtocol() == 1) {
                return intf;
            }
        }
        return null;
    }

    // Sets the current USB device and interface
    private synchronized boolean setAdbInterface(UsbDevice device, UsbInterface intf) throws IOException, InterruptedException {

        if (adbConnection != null) {
            adbConnection.close();
            adbConnection = null;
            mDevice = null;
        }

        UsbManager mManager = (UsbManager) getSystemService(Context.USB_SERVICE);
        if (device != null && intf != null) {
            UsbDeviceConnection connection = mManager.openDevice(device);
            if (connection != null) {
                if (connection.claimInterface(intf, false)) {

                    handler.sendEmptyMessage(Message.CONNECTING);

                    adbConnection = AdbConnection.create(new UsbChannel(connection, intf), adbCrypto);

                    adbConnection.connect();

                    //TODO: DO NOT DELETE IT, I CAN'T EXPLAIN WHY
                    adbConnection.open("shell:exec date");

                    mDevice = device;
                    handler.sendEmptyMessage(Message.DEVICE_FOUND);
                    return true;
                } else {
                    connection.close();
                }
            }
        }

        handler.sendEmptyMessage(Message.DEVICE_NOT_FOUND);

        mDevice = null;
        return false;
    }

    @Override
    public void onResume() {
        super.onResume();

        final PackageManager packageManager = getPackageManager();

        Set<String> selectedPackageNames = Config.getApks(this);

        int n = 0;

        List<PackageInfo> packages = packageManager.getInstalledPackages(0);
        for (PackageInfo packageInfo : packages) {
            if (selectedPackageNames.contains(packageInfo.packageName)) {
                n++;
            }
        }
        total = n;

        textView.setText(current + "/" + total);
        progressBar.setProgress(currentProgress);

        if (total == 0) {
            flashButton.setVisibility(View.GONE);
            setupButton.setVisibility(View.VISIBLE);
        } else {
            flashButton.setVisibility(View.VISIBLE);
            setupButton.setVisibility(View.GONE);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        unregisterReceiver(mUsbReceiver);
        try {
            if (adbConnection != null) {
                adbConnection.close();
                adbConnection = null;
            }
        } catch (IOException e) {
        }

    }
}

