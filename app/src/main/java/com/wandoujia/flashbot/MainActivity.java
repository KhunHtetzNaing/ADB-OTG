package com.wandoujia.flashbot;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbInterface;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBarActivity;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.mrkid.adblib.AdbBase64;
import com.mrkid.adblib.AdbConnection;
import com.mrkid.adblib.AdbCrypto;
import com.mrkid.adblib.AdbStream;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class MainActivity extends ActionBarActivity {


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.container, new PlaceholderFragment())
                    .commit();
        }
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
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * A placeholder fragment containing a simple view.
     */
    public static class PlaceholderFragment extends Fragment {
        private Handler handler;

        private TextView textView;
        private Button flashButton;

        private ImageView imageView;

        private AdbConnection adbConnection;

        private UsbDevice mDevice;

        private UsbManager mManager;

        public PlaceholderFragment() {
        }


        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.fragment_main, container, false);

            textView = (TextView) rootView.findViewById(R.id.textView);

            flashButton = (Button) rootView.findViewById(R.id.flashButton);
            flashButton.setEnabled(false);

            imageView = (ImageView) rootView.findViewById(R.id.imageView);


            mManager = (UsbManager)getActivity().getSystemService(Context.USB_SERVICE);
            handler = new Handler() {
                @Override
                public void handleMessage(android.os.Message msg) {
                    Message state = Message.values()[msg.what];
                    switch (state) {
                        case DEVICE_FOUND:
                            flashButton.setEnabled(true);
                            flashButton.setBackgroundResource(R.drawable.button_ready);
                            flashButton.setText(R.string.ready_flash_apks);
                            break;

                        case DEVICE_UNAUTHORIZED:
                            flashButton.setEnabled(true);
                            flashButton.setBackgroundResource(R.drawable.button_waiting);
                            flashButton.setText(R.string.authorize_hint);
                            break;

                        case DEVICE_NOT_FOUND:
                            flashButton.setEnabled(false);
                            flashButton.setBackgroundResource(R.drawable.button_waiting);
                            flashButton.setText(R.string.waiting_device);
                            break;

                        case FLASHING:
                            flashButton.setEnabled(false);
                            flashButton.setBackgroundResource(R.drawable.button_working);
                            flashButton.setText(R.string.flashing_apks);
                            break;

                    }
                }
            };

            flashButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    File dir = new File("/sdcard/flashbot/apks");
                    if (!dir.exists()) {
                        dir.mkdirs();
                    }

                    final List<File> files = new ArrayList<File>();
                    files.addAll(Arrays.asList(dir.listFiles()));
                    Collections.sort(files, new Comparator<File>() {
                        @Override
                        public int compare(File lhs, File rhs) {
                            return lhs.getName().compareTo(rhs.getName());
                        }
                    });


                    new Thread() {
                        @Override
                        public void run() {
                            for (File local : files) {
                                try {
                                    String remotePath = "/sdcard/Download/" + local.getName();
                                    new Push(adbConnection, local, remotePath).execute();
                                    new Install(adbConnection, remotePath).execute();
                                } catch (Exception e) {
                                    Log.w(Const.TAG, "exception caught", e);
                                }
                            }

                        }

                    }.start();

//                    try {
//                        final AdbStream open = adbConnection.open("shell:exec logcat");
//                        new Thread() {
//
//                            @Override
//                            public void run() {
//                                while (!open.isClosed()) {
//                                    try {
//                                        Log.i(Const.TAG, new String(open.read()));
//                                    } catch (InterruptedException e) {
//                                        e.printStackTrace();
//                                    } catch (IOException e) {
//                                        e.printStackTrace();
//                                    }
//                                }
//                            }
//                        }.start();
//                    } catch (IOException e) {
//                        e.printStackTrace();
//                    } catch (InterruptedException e) {
//                        e.printStackTrace();
//                    }
                }
            });

            // check for existing devices
//            try {
//                for (UsbDevice device :  mManager.getDeviceList().values()) {
//                    UsbInterface intf = findAdbInterface(device);
//                    if (setAdbInterface(device, intf)) {
//
//                        break;
//                    }
//                }
//            } catch (IOException e) {
//                e.printStackTrace();
//            } catch (InterruptedException e) {
//                e.printStackTrace();
//            }


            UsbDevice device = (UsbDevice) getActivity().getIntent().getParcelableExtra(UsbManager.EXTRA_DEVICE);
            if (device != null) {
                UsbInterface intf = findAdbInterface(device);
                try {
                    setAdbInterface(device, intf);
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

            }


            // listen for new devices
            IntentFilter filter = new IntentFilter();
            filter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED);
            filter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
            getActivity().registerReceiver(mUsbReceiver, filter);

            return rootView;
        }

        BroadcastReceiver mUsbReceiver = new BroadcastReceiver() {
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();

                if (UsbManager.ACTION_USB_DEVICE_ATTACHED.equals(action)) {
                    UsbDevice device = (UsbDevice) intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                    UsbInterface intf = findAdbInterface(device);
                    if (intf != null) {
                        try {
                            setAdbInterface(device, intf);
                        } catch (Exception e) {
                            Log.w(Const.TAG, "setAdbInterface failed", e);
                        }
                    }
                } else if (UsbManager.ACTION_USB_DEVICE_DETACHED.equals(action)) {
                    UsbDevice device = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                    String deviceName = device.getDeviceName();
                    if (mDevice != null && mDevice.getDeviceName().equals(deviceName)) {
                        try {
                            setAdbInterface(null, null);
                        } catch (Exception e) {
                            Log.w(Const.TAG, "setAdbInterface to null failed", e);
                        }
                    }
                }
            }
        };

        // searches for an adb interface on the given USB device
        static private UsbInterface findAdbInterface(UsbDevice device) {
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
        private boolean setAdbInterface(UsbDevice device, UsbInterface intf) throws IOException, InterruptedException {
            if (adbConnection != null) {
                adbConnection.close();
                adbConnection = null;
                mDevice = null;
            }

            if (device != null && intf != null) {

                UsbDeviceConnection connection = mManager.openDevice(device);
                if (connection != null) {
                    if (connection.claimInterface(intf, false)) {
                        try {
                            adbConnection = AdbConnection.create(connection, intf, AdbCrypto.generateAdbKeyPair(new AdbBase64() {
                                @Override
                                public String encodeToString(byte[] data) {
                                    return Base64.encodeToString(data, Base64.NO_WRAP);
                                }
                            }));

                            adbConnection.connect();

                            mDevice = device;
                            handler.sendEmptyMessage(Message.DEVICE_FOUND.ordinal());
                            return true;
                        } catch (NoSuchAlgorithmException e) {
                            throw new RuntimeException(e);
                        }
                    } else {
                        connection.close();
                    }
                }
            }

            handler.sendEmptyMessage(Message.DEVICE_NOT_FOUND.ordinal());

            mDevice = null;
            return false;
        }
    }

}
