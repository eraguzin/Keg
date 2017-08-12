package com.cid.keg;

import android.app.ListActivity;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;
import android.os.Handler;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.apache.commons.lang.ArrayUtils;

public class BLE_Test extends AppCompatActivity {

    private static final String DEVICE_ADDRESS="20:14:10:30:05:38";
    private static final String DEVICE_NAME = "keg";
    private final UUID PORT_UUID = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb");//Serial Port Service ID
    private BluetoothDevice device;
    private BluetoothSocket socket;
    private OutputStream outputStream;
    private InputStream inputStream;

    boolean deviceConnected=false;
    byte buffer[];
    boolean stopThread;
    String fullString = "";
    String singleCommand = "";
    boolean saving=false;
    boolean iteration_done=false;
    char beginning='@';
    char ending='!';


    //System Views
    Button startButton,stopButton,sendButton;
    TextView Status1,rawIn,Status2;
    EditText rawOut;

    TextView pressureRefReceive,pressureReceive,errorReceive,kPReceive,kIReceive,kDReceive,
            PReceive,IReceive,DReceive,IntegralReceive,PumpReceive,BrightnessReceive,UpdateReceive,
            SetReceive,DispReceive,BattReceive,dPReceive,dTReceive,ModeReceive,Start_pReceive,
            BlowReceive,TTReceive,TestWaitReceive,FullnessReceive;

    Switch switchButton;

    private BluetoothAdapter mBluetoothAdapter;
    private int REQUEST_ENABLE_BT = 1;
    private Handler mHandler;
    private static final long SCAN_PERIOD = 10000;
    private BluetoothLeScanner mLEScanner;
    private ScanSettings settings;
    private List<ScanFilter> filters;
    private BluetoothGatt mGatt;

    private List<BluetoothGattCharacteristic> Charactaristics = new ArrayList<>();
    private List<BluetoothGattDescriptor> Descr = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ble__test);
        mHandler = new Handler();
        //System Views
        startButton = (Button) findViewById(R.id.buttonStart);
        stopButton = (Button) findViewById(R.id.buttonStop);
        sendButton = (Button) findViewById(R.id.buttonSend);

        Status1 = (TextView) findViewById(R.id.Status1);
        rawIn = (TextView) findViewById(R.id.rawIn);
        Status2 = (TextView) findViewById(R.id.Status2);
        rawOut = (EditText) findViewById(R.id.rawOut);

        pressureRefReceive = (TextView) findViewById(R.id.receiveRefPressure);
        pressureReceive = (TextView) findViewById(R.id.receivePressure);
        errorReceive = (TextView) findViewById(R.id.receiveError);
        kPReceive = (TextView) findViewById(R.id.receivekP);
        kIReceive = (TextView) findViewById(R.id.receivekI);
        kDReceive = (TextView) findViewById(R.id.receivekD);
        PReceive = (TextView) findViewById(R.id.receiveP);
        IReceive = (TextView) findViewById(R.id.receiveI);
        DReceive = (TextView) findViewById(R.id.receiveD);
        IntegralReceive = (TextView) findViewById(R.id.receiveIntegral);
        PumpReceive = (TextView) findViewById(R.id.receivePump);
        BrightnessReceive = (TextView) findViewById(R.id.receiveBrightness);
        UpdateReceive = (TextView) findViewById(R.id.receiveUpdate);
        SetReceive = (TextView) findViewById(R.id.receiveSet);
        DispReceive = (TextView) findViewById(R.id.receiveDisp);
        BattReceive = (TextView) findViewById(R.id.receivebatt);
        dPReceive = (TextView) findViewById(R.id.receivedP);
        dTReceive = (TextView) findViewById(R.id.receivedT);
        ModeReceive = (TextView) findViewById(R.id.receivemode);
        Start_pReceive = (TextView) findViewById(R.id.receivestart_p);
        BlowReceive = (TextView) findViewById(R.id.receiveBlow);
        TTReceive = (TextView) findViewById(R.id.receiveTT);
        TestWaitReceive = (TextView) findViewById(R.id.receiveTestWait);
        FullnessReceive = (TextView) findViewById(R.id.receiveFullness);

        switchButton = (Switch) findViewById(R.id.buttonSwitch);
        switchButton.setChecked(false);
        Status2.setText("BLE test yo");

        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, "BLE Not Supported",
                    Toast.LENGTH_SHORT).show();
            finish();
        }
        final BluetoothManager bluetoothManager =
                (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();

    }

    public void onClickSend(View view) {
        BluetoothGattCharacteristic display = Charactaristics.get(0);
        Log.i("onClickSend", display.getUuid().toString());
        Log.i("onClickSend", Integer.toString(getAssignedNumber(display.getUuid())));
        mGatt.readCharacteristic(display);
        display.setValue(4, BluetoothGattCharacteristic.FORMAT_UINT8, 0);
        mGatt.writeCharacteristic(display);
        mGatt.readCharacteristic(display);
    }

    public void onClickParameter(View view) {
        BluetoothGattCharacteristic display = Charactaristics.get(1);
        Log.i("onClickSend", display.getUuid().toString());
        Log.i("onClickSend", Integer.toString(getAssignedNumber(display.getUuid())));
        mGatt.readCharacteristic(display);
        display.setValue(4, BluetoothGattCharacteristic.FORMAT_UINT8, 0);
        mGatt.writeCharacteristic(display);
        mGatt.readCharacteristic(display);
    }



    @Override
    protected void onResume() {
        super.onResume();
        if (mBluetoothAdapter == null || !mBluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        } else {
            mLEScanner = mBluetoothAdapter.getBluetoothLeScanner();
            settings = new ScanSettings.Builder()
                    .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                    .build();
            ScanFilter scanFilter =
                    new ScanFilter.Builder()
                            .setDeviceName("Keg")
                            .build();
            ScanFilter scanFilter2 =
                    new ScanFilter.Builder()
                            .setDeviceName("Bluefruit52 HRM")
                            .build();
            filters = new ArrayList<ScanFilter>();
            filters.add(scanFilter);
            filters.add(scanFilter2);
            //scanLeDevice(true);
            Log.i("onResume", "We've got our filters");
        }
    }

    public void onClickSwitch(View view) {
        if (deviceConnected) {

            try {
                String sendText = "@";
                sendText += "y";
                sendText += "!";
                outputStream.write(sendText.getBytes());
            } catch (IOException e) {
                e.printStackTrace();
            }
            try {
                outputStream.close();
                inputStream.close();
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        Intent intent = new Intent(this, BasicActivity.class);
        startActivity(intent);
        finish();

    }

    public void onClickStart(View view) {

        Log.i("onClickStart", "Start clicked");
        scanLeDevice(true);
    }

    private void scanLeDevice(final boolean enable) {
        if (enable) {
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mLEScanner.stopScan(mScanCallback);
                }
            }, SCAN_PERIOD);

            mLEScanner.startScan(filters, settings, mScanCallback);
            Log.i("ScanLeDevice", "Scanning...");
        } else {
            mLEScanner.stopScan(mScanCallback);

        }
    }

    private ScanCallback mScanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            Log.i("callbackType", String.valueOf(callbackType));
            Log.i("result", result.toString());
            BluetoothDevice btDevice = result.getDevice();
            connectToDevice(btDevice);
        }

        @Override
        public void onBatchScanResults(List<ScanResult> results) {
            for (ScanResult sr : results) {
                Log.i("ScanResult - Results", sr.toString());
            }
        }

        @Override
        public void onScanFailed(int errorCode) {
            Log.e("Scan Failed", "Error Code: " + errorCode);
        }
    };

    public void connectToDevice(BluetoothDevice device) {
        if (mGatt == null) {
            mGatt = device.connectGatt(this, false, gattCallback);
            scanLeDevice(false);// will stop after first device detection
        }
    }

    private final BluetoothGattCallback gattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            //status of 0 if operation succeeds
            Log.i("onConnectionStateChange", "Status: " + status);
            switch (newState) {
                case BluetoothProfile.STATE_CONNECTED:
                    Log.i("gattCallback", "STATE_CONNECTED");
                    gatt.discoverServices();
                    break;
                case BluetoothProfile.STATE_DISCONNECTED:
                    Log.e("gattCallback", "STATE_DISCONNECTED");
                    break;
                default:
                    Log.e("gattCallback", "STATE_OTHER");
            }

        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            List<BluetoothGattService> services = gatt.getServices();
            int[] acceptable_services = getResources().getIntArray(R.array.Services);
            int[] acceptable_characteristics = getResources().getIntArray(R.array.Characteristics);
            for (BluetoothGattService service : services) {
                if (ArrayUtils.contains(acceptable_services, getAssignedNumber(service.getUuid()))) {
                    Log.i("Service found", service.getUuid().toString());
                    List<BluetoothGattCharacteristic> characteristics = service.getCharacteristics();
                    for (BluetoothGattCharacteristic characteristic : characteristics) {
                        if (ArrayUtils.contains(acceptable_characteristics, getAssignedNumber(characteristic.getUuid()))) {
                            Log.i("Characteristic found", characteristic.getUuid().toString());
                            mGatt.readCharacteristic(characteristic);
                            mGatt.setCharacteristicNotification(characteristic, true);
                            characteristic.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE);
                            Charactaristics.add(characteristic);
                            Descr= characteristic.getDescriptors();
                            for (BluetoothGattDescriptor d: Descr){
                                Log.i("Descriptors", d.getUuid().toString());
                                d.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                                mGatt.writeDescriptor(d);
                            }

                        }
                    }
                }
            }
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt,
                                         BluetoothGattCharacteristic
                                                 characteristic, int status) {
            Log.i("onCharacteristicReadV", characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, 0).toString());
            Log.i("onCharacteristicReadP1", Integer.toString(characteristic.getPermissions()));
            Log.i("onCharacteristicReadP2", Integer.toString(characteristic.getProperties()));
            Log.i("onCharacteristicReadS", Integer.toString(status));
            //gatt.disconnect();
        }

        @Override
        public void onCharacteristicChanged (BluetoothGatt gatt,
                                 BluetoothGattCharacteristic characteristic){
            Log.i("onCharacteristicChanged", characteristic.toString());
            Log.i("onCharacteristicChanged", characteristic.getIntValue(characteristic.FORMAT_UINT8, 0).toString());
            Log.i("onCharacteristicChanged", characteristic.getIntValue(characteristic.FORMAT_UINT8, 1).toString());
            Log.i("onCharacteristicChanged", Integer.toString(characteristic.getPermissions()));
            Log.i("onCharacteristicChanged", Integer.toString(characteristic.getProperties()));
        }

        @Override
        public void onDescriptorRead(BluetoothGatt gatt,
                                         BluetoothGattDescriptor
                                                 descriptor, int status) {
            String value = new String(descriptor.getValue());
            String permission = new String(descriptor.getValue());
            Log.i("onDescriptorReadV", value);
            Log.i("onDescriptorReadP1", permission);
            Log.i("onDescriptorReadS", Integer.toString(status));
        }

        @Override
        public void onDescriptorWrite (BluetoothGatt gatt,
                                        BluetoothGattDescriptor
                                                descriptor, int status) {
            String value = new String(descriptor.getValue());
            String permission = new String(descriptor.getValue());
            Log.i("onDescriptorWriteV", value);
            Log.i("onDescriptorWriteP1", permission);
            Log.i("onDescriptorWriteS", Integer.toString(status));
        }
    };

    private static int getAssignedNumber(UUID uuid) {
        // Keep only the significant bits of the UUID
        return (int) ((uuid.getMostSignificantBits() & 0x0000FFFF00000000L) >> 32);
    }
}