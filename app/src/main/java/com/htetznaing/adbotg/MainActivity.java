package com.htetznaing.adbotg;

import static com.htetznaing.adbotg.Message.CONNECTING;
import static com.htetznaing.adbotg.Message.DEVICE_FOUND;
import static com.htetznaing.adbotg.Message.DEVICE_NOT_FOUND;
import static com.htetznaing.adbotg.Message.FLASHING;
import static com.htetznaing.adbotg.Message.INSTALLING_PROGRESS;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbInterface;
import android.hardware.usb.UsbManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.cgutman.adb.AdbBase64;
import com.cgutman.adb.AdbConnection;
import com.cgutman.adb.AdbCrypto;
import com.cgutman.adb.AdbStream;
import com.cgutman.adb.UsbChannel;
import com.htetznaing.adbotg.Adapter.SliderAdapterExample;
import com.htetznaing.adbotg.Model.SliderItem;
import com.htetznaing.adbotg.UI.SpinnerDialog;
import com.smarteist.autoimageslider.IndicatorView.animation.type.IndicatorAnimationType;
import com.smarteist.autoimageslider.SliderAnimations;
import com.smarteist.autoimageslider.SliderView;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class MainActivity extends AppCompatActivity implements TextView.OnEditorActionListener, View.OnKeyListener {
    private Handler handler;
    private UsbDevice mDevice;
    private TextView tvStatus, logs;
    private ImageView usb_icon;
    private AdbCrypto adbCrypto;
    private AdbConnection adbConnection;
    private UsbManager mManager;
    private RelativeLayout terminalView;
    private LinearLayout checkContainer;
    private EditText edCommand;
    private Button btnRun;
    private ScrollView scrollView;
    private String user = null;
    private SliderAdapterExample adapter;
    private SliderView sliderView;
    private boolean doubleBackToExitPressedOnce = false;
    private AdbStream stream;
    private SpinnerDialog waitingDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tvStatus = findViewById(R.id.tv_status);
        usb_icon = findViewById(R.id.usb_icon);
        logs = findViewById(R.id.logs);
        terminalView = findViewById(R.id.terminalView);
        checkContainer = findViewById(R.id.checkContainer);
        edCommand = findViewById(R.id.edCommand);
        btnRun = findViewById(R.id.btnRun);
        scrollView = findViewById(R.id.scrollView);
        mManager = (UsbManager) getSystemService(Context.USB_SERVICE);

        handler = new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(@NonNull android.os.Message msg) {
                switch (msg.what) {
                    case DEVICE_FOUND -> {
                        closeWaiting();
                        tvStatus.setText(getString(R.string.adb_device_connected));
                        usb_icon.setColorFilter(Color.parseColor("#4CAF50"));
                        checkContainer.setVisibility(View.GONE);
                        terminalView.setVisibility(View.VISIBLE);
                        initCommand();
                        showKeyboard();
                    }
                    case CONNECTING -> {
                        waitingDialog();
                        closeKeyboard();
                        tvStatus.setText(getString(R.string.waiting_device));
                        usb_icon.setColorFilter(Color.BLUE);
                        checkContainer.setVisibility(View.VISIBLE);
                        terminalView.setVisibility(View.GONE);
                    }
                    case DEVICE_NOT_FOUND -> {
                        closeWaiting();
                        closeKeyboard();
                        tvStatus.setText(getString(R.string.adb_device_not_connected));
                        usb_icon.setColorFilter(Color.RED);
                        checkContainer.setVisibility(View.VISIBLE);
                        terminalView.setVisibility(View.GONE);
                    }
                    case FLASHING ->
                            Toast.makeText(MainActivity.this, "Flashing", Toast.LENGTH_SHORT).show();
                    case INSTALLING_PROGRESS ->
                            Toast.makeText(MainActivity.this, "Progress", Toast.LENGTH_SHORT).show();
                }
            }
        };

        AdbBase64 base64 = new MyAdbBase64();
        try {
            adbCrypto = AdbCrypto.loadAdbKeyPair(base64, new File(getFilesDir(), "private_key"), new File(getFilesDir(), "public_key"));
        } catch (Exception e) {
            e.printStackTrace();
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
        filter.addAction(Message.USB_PERMISSION);

        ContextCompat.registerReceiver(this, mUsbReceiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED);

        //Check USB
        UsbDevice device = getIntent().getParcelableExtra(UsbManager.EXTRA_DEVICE);
        if (device != null) {
            System.out.println("From Intent!");
            asyncRefreshAdbConnection(device);
        } else {
            System.out.println("From onCreate!");
            for (String k : mManager.getDeviceList().keySet()) {
                UsbDevice usbDevice = mManager.getDeviceList().get(k);
                handler.sendEmptyMessage(CONNECTING);
                if (mManager.hasPermission(usbDevice)) {
                    asyncRefreshAdbConnection(usbDevice);
                } else {
                    mManager.requestPermission(
                            usbDevice,
                            PendingIntent.getBroadcast(getApplicationContext(),
                                    0,
                                    new Intent(Message.USB_PERMISSION),
                                    PendingIntent.FLAG_IMMUTABLE));
                }
            }
        }

        //Slider
        sliderView = findViewById(R.id.imageSlider);
        adapter = new SliderAdapterExample();
        sliderView.setSliderAdapter(adapter);
        sliderView.setIndicatorAnimation(IndicatorAnimationType.WORM); //set indicator animation by using SliderLayout.IndicatorAnimations. :WORM or THIN_WORM or COLOR or DROP or FILL or NONE or SCALE or SCALE_DOWN or SLIDE and SWAP!!
        sliderView.setSliderTransformAnimation(SliderAnimations.SIMPLETRANSFORMATION);
        sliderView.setAutoCycleDirection(SliderView.AUTO_CYCLE_DIRECTION_BACK_AND_FORTH);
        sliderView.setIndicatorSelectedColor(Color.WHITE);
        sliderView.setIndicatorUnselectedColor(Color.GRAY);
        sliderView.setScrollTimeInSec(3);
        sliderView.setAutoCycle(true);
        sliderView.startAutoCycle();

        SliderItem sliderItem = new SliderItem();
        sliderItem.setImageUrl(R.drawable.p2p_howto);
        sliderItem.setDescription("Connect phone to phone");
        adapter.addItem(sliderItem);

        SliderItem sliderItem1 = new SliderItem();
        sliderItem1.setImageUrl(R.drawable.deb);
        sliderItem1.setDescription("Enable developer options and USB debugging");
        adapter.addItem(sliderItem1);

        edCommand.setImeActionLabel("Run", EditorInfo.IME_ACTION_DONE);
        edCommand.setOnEditorActionListener(this);
        edCommand.setOnKeyListener(this);

//        //Guide
//        LinearLayout guideContainer = findViewById(R.id.guideContainer);
//        String [] split = getString(R.string.guide_for_dev_option).split("=============================");
//        for (String a:split){
//            String[] b = a.trim().split("\n");
//            String mTitle = b[0];
//            String content = a.replace(mTitle,"").replace(b[1],"").trim();
//
//            View view = getLayoutInflater().inflate(R.layout.list_item,null);
//            TextView title = view.findViewById(R.id.title);
//            title.setText(mTitle);
//            ExpandableTextView exp = view.findViewById(R.id.expand_text_view);
//            exp.setText(content);
//
//            guideContainer.addView(view);
//        }
    }

    private void closeWaiting() {
        if (waitingDialog != null)
            waitingDialog.dismiss();
    }

    private void waitingDialog() {
        closeWaiting();
        waitingDialog = SpinnerDialog.displayDialog(this, "IMPORTANT ⚡",
                "You may need to accept a prompt on the target device if you are connecting " +
                        "to it for the first time from this device.", false);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.go_to_github) {
            startActivity(new Intent(Intent.ACTION_VIEW).setData(Uri.parse("https://github.com/KhunHtetzNaing/ADB-OTG")));
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        System.out.println("From onNewIntent");
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
            String action = intent.getAction();
            Log.d(Const.TAG, "mUsbReceiver onReceive => " + action);
            if (UsbManager.ACTION_USB_DEVICE_DETACHED.equals(action)) {
                UsbDevice device = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                String deviceName = device != null ? device.getDeviceName() : null;
                if (mDevice != null && mDevice.getDeviceName().equals(deviceName)) {
                    try {
                        Log.d(Const.TAG, "setAdbInterface(null, null)");
                        setAdbInterface(null, null);
                    } catch (Exception e) {
                        Log.w(Const.TAG, "setAdbInterface(null,null) failed", e);
                    }
                }
            } else if (Message.USB_PERMISSION.equals(action)) {
                System.out.println("From receiver!");
                UsbDevice usbDevice = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                handler.sendEmptyMessage(CONNECTING);
                if (mManager.hasPermission(usbDevice))
                    asyncRefreshAdbConnection(usbDevice);
                else
                    mManager.requestPermission(usbDevice, PendingIntent.getBroadcast(getApplicationContext(), 0, new Intent(Message.USB_PERMISSION), PendingIntent.FLAG_IMMUTABLE));
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

        if (device != null && intf != null) {
            UsbDeviceConnection connection = mManager.openDevice(device);
            if (connection != null) {
                if (connection.claimInterface(intf, false)) {
                    handler.sendEmptyMessage(CONNECTING);
                    adbConnection = AdbConnection.create(new UsbChannel(connection, intf), adbCrypto);
                    adbConnection.connect();
                    //TODO: DO NOT DELETE IT, I CAN'T EXPLAIN WHY
                    adbConnection.open("shell:exec date");

                    mDevice = device;
                    handler.sendEmptyMessage(DEVICE_FOUND);
                    return true;
                } else {
                    connection.close();
                }
            }
        }

        handler.sendEmptyMessage(DEVICE_NOT_FOUND);

        mDevice = null;
        return false;
    }

    @Override
    public void onResume() {
        super.onResume();
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
            e.printStackTrace();
        }

    }

    private void initCommand() {
        // Open the shell stream of ADB
        logs.setText("");
        try {
            stream = adbConnection.open("shell:");
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
            return;
        }

        // Start the receiving thread
        new Thread(() -> {
            while (!stream.isClosed()) {
                try {
                    // Print each thing we read from the shell stream
                    final String[] output = {new String(stream.read(), StandardCharsets.US_ASCII)};
                    runOnUiThread(() -> {
                        if (user == null) {
                            user = output[0].substring(0, output[0].lastIndexOf("/") + 1);
                        } else if (output[0].contains(user)) {
                            System.out.println("End => " + user);
                        }

                        logs.append(output[0]);

                        scrollView.post(() -> {
                            scrollView.fullScroll(ScrollView.FOCUS_DOWN);
                            edCommand.requestFocus();
                        });
                    });
                } catch (InterruptedException | IOException e) {
                    e.printStackTrace();
                    return;
                }
            }
        }).start();

        btnRun.setOnClickListener(v -> putCommand());
    }

    private void putCommand() {
        if (!edCommand.getText().toString().isEmpty()) {
            // We become the sending thread
            try {
                String cmd = edCommand.getText().toString();
                if (cmd.equalsIgnoreCase("clear")) {
                    String log = logs.getText().toString();
                    String[] logSplit = log.split("\n");
                    logs.setText(logSplit[logSplit.length - 1]);
                } else if (cmd.equalsIgnoreCase("exit")) {
                    finish();
                } else {
                    stream.write((cmd + "\n").getBytes(StandardCharsets.UTF_8));
                }
                edCommand.setText("");
            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
            }
        } else Toast.makeText(MainActivity.this, "No command", Toast.LENGTH_SHORT).show();
    }

    public void open(View view) {

    }

    public void showKeyboard() {
        edCommand.requestFocus();
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0);
    }

    public void closeKeyboard() {
        View view = this.getCurrentFocus();
        if (view != null) {
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }

    @Override
    public void onBackPressed() {
        if (doubleBackToExitPressedOnce) {
            super.onBackPressed();
            return;
        }

        this.doubleBackToExitPressedOnce = true;
        Toast.makeText(this, "Please click BACK again to exit", Toast.LENGTH_SHORT).show();

        new Handler().postDelayed(() -> doubleBackToExitPressedOnce = false, 2000);
    }

    @Override
    public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
        /* We always return false because we want to dismiss the keyboard */
        if (adbConnection != null && actionId == EditorInfo.IME_ACTION_DONE) {
            putCommand();
        }

        return true;
    }

    @Override
    public boolean onKey(View v, int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_ENTER) {
            /* Just call the onEditorAction function to handle this for us */
            return onEditorAction((TextView) v, EditorInfo.IME_ACTION_DONE, event);
        } else {
            return false;
        }
    }
}

