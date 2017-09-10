package com.cid.keg;

import com.cid.keg.BLE_Characteristic;
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
import android.os.SystemClock;
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
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;
import android.os.Handler;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.apache.commons.lang.ArrayUtils;
import org.w3c.dom.Text;

public class BLE_Test extends AppCompatActivity {

    private static final String DEVICE_ADDRESS="20:14:10:30:05:38";
    private static final String DEVICE_NAME = "keg";
    private final UUID PORT_UUID = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb");//Serial Port Service ID
    private BluetoothDevice device;
    private BluetoothSocket socket;
    private OutputStream outputStream;
    private InputStream inputStream;

    boolean deviceConnected=false;

    //System Views
    Button startButton,stopButton,sendButton;
    TextView Status1,rawIn,Status2;
    EditText rawOut;

    TextView pressureRefReceive,pressureReceive,errorReceive,kPReceive,kIReceive,kDReceive,
            PReceive,IReceive,DReceive,IntegralReceive,PumpReceive,VersionReceive,UpdateReceive,
            SettleReceive,DebugReceive,BattReceive,dPReceive,dTReceive,ModeReceive,Start_pReceive,
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

    private List<BluetoothGattCharacteristic> confirmedCharacteristics = new ArrayList<>();
    private List<BluetoothGattDescriptor> Descr = new ArrayList<>();
    private Queue<BluetoothGattDescriptor> descriptorSetQueue = new ConcurrentLinkedQueue();
    private Object serviceLock = new Object();
    private Object initWriteLock = new Object();
    boolean BLEservicesRunning = false;
    setDescriptorThread q = new setDescriptorThread();
    initialReadThread init = new initialReadThread();
    List<BLE_Characteristic> allBLE = new ArrayList<>();

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

        switchButton = (Switch) findViewById(R.id.buttonSwitch);
        switchButton.setChecked(false);
        Status2.setText("BLE test yo");

        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, "Bluetooth Low Energy Not Supported",
                    Toast.LENGTH_SHORT).show();
            finish();
        }


        new Thread(q).start();

        final BluetoothManager bluetoothManager =
                (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();

    }

    public void onClickSend(View view) {
        BluetoothGattCharacteristic display = confirmedCharacteristics.get(0);
        Log.i("onClickSend", display.getUuid().toString());
        Log.i("onClickSend", Integer.toString(BLE_Characteristic.getAssignedNumber(display.getUuid())));
        mGatt.readCharacteristic(display);
        display.setValue(4, BluetoothGattCharacteristic.FORMAT_UINT8, 0);
        mGatt.writeCharacteristic(display);
        mGatt.readCharacteristic(display);
    }

    public void onClickParameter(View view) {
        int rowId = ((View) view.getParent()).getId();
        TableRow row = (TableRow)findViewById(rowId);
        EditText grabFrom = (EditText)row.getChildAt(2);
        String valueString = grabFrom.getText().toString();
        TextView reference = (TextView)row.getChildAt(1);
        BluetoothGattCharacteristic toWriteTo = null;
        Integer shorthand = null;
        for (BLE_Characteristic temp : allBLE){
            if (temp.BLE_View == reference){
                toWriteTo = temp.BLE_Full;
                shorthand = temp.BLE_Short;
                break;
            }
        }
        if (toWriteTo != null){
            if (shorthand >= 0xF000) {
                Float floatToWrite = Float.parseFloat(valueString);
                int bits = Float.floatToIntBits(floatToWrite);
                //characteristic.setValue(mantissa, exponent, BluetoothGattCharacteristic.FORMAT_FLOAT, 0);
                toWriteTo.setValue(bits, BluetoothGattCharacteristic.FORMAT_UINT32, 0);
            }
            else if ((shorthand < 0xF000) & (shorthand >= 0xE000)) {
                Integer intToWrite = Integer.parseInt(valueString);
                toWriteTo.setValue(intToWrite, BluetoothGattCharacteristic.FORMAT_UINT8, 0);
            }
            else if ((shorthand < 0xE000) & (shorthand >= 0xD000)) {
                Integer intToWrite = Integer.parseInt(valueString);
                toWriteTo.setValue(intToWrite, BluetoothGattCharacteristic.FORMAT_UINT16, 0);
            }
            else if ((shorthand < 0xD000) & (shorthand >= 0xC000)) {
                Integer intToWrite = Integer.parseInt(valueString);
                toWriteTo.setValue(intToWrite, BluetoothGattCharacteristic.FORMAT_UINT32, 0);
            }
            else {
                Log.i("OnClickSend", "Unknown Characteristic");
            }
            Log.i("OnClickSend", "Sending " + valueString);
            Log.i("OnClickSend", "To " + Integer.toHexString(shorthand));
            BLEservicesRunning = true;
            boolean write = mGatt.writeCharacteristic(toWriteTo);
            String result = String.valueOf(write);
            Log.i("DidItSend", result);
            while (BLEservicesRunning == true){
                try{
                    Log.i("Write Thread", "BLE Services being used, will wait");
                    synchronized (serviceLock) {
                        serviceLock.wait();
                    }
                } catch (InterruptedException iex) {
                    Log.i("Write Thread: ", iex.getMessage());
                }
            }
            mGatt.readCharacteristic(toWriteTo);
        }
        else{
            Log.i("onClickSend", "No view found!");
        }
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
                            .setDeviceName("Keg Pump!")
                            .build();
            ScanFilter scanFilter2 =
                    new ScanFilter.Builder()
                            .setDeviceName("Bluefruit52 HRM")
                            .build();
            filters = new ArrayList<>();
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
            Log.i("Address", btDevice.getAddress());
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
                    android.os.SystemClock.sleep(600);
                    gatt.discoverServices();
                    break;
                case BluetoothProfile.STATE_DISCONNECTED:
                    Log.e("gattCallback", "STATE_DISCONNECTED");
                    mGatt.close();
                    break;
                default:
                    Log.e("gattCallback", "STATE_OTHER");
            }

        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            List<BluetoothGattService> services = gatt.getServices();
            int[] acceptable_services = getResources().getIntArray(R.array.Services);
            int[] notification_characteristics = getResources().getIntArray(R.array.notificationCharacteristics);
            int[] write_characteristics = getResources().getIntArray(R.array.writeCharacteristics);
            for (BluetoothGattService service : services) {
                if (ArrayUtils.contains(acceptable_services, BLE_Characteristic.getAssignedNumber(service.getUuid()))) {
                    Log.i("Service found", service.getUuid().toString());
                    allBLE.clear();
                    List<BluetoothGattCharacteristic> characteristics = service.getCharacteristics();
                    for (BluetoothGattCharacteristic characteristic : characteristics) {
                        Integer shorthandChar = BLE_Characteristic.getAssignedNumber(characteristic.getUuid());

                        TextView corresponding_view = BLE_Characteristic.getIdFromShortChar(BLE_Test.this, shorthandChar);
                        allBLE.add(new BLE_Characteristic(shorthandChar, characteristic, corresponding_view));
                        if (ArrayUtils.contains(notification_characteristics, shorthandChar)) {
                            Log.i("Characteristic found", characteristic.getUuid().toString());
                            //mGatt.readCharacteristic(characteristic);

                            //characteristic.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE);
                            confirmedCharacteristics.add(characteristic);
                            Descr = characteristic.getDescriptors();

                            for (BluetoothGattDescriptor d : Descr) {
                                //Log.i("Descriptors", d.getUuid().toString());
                                boolean set = d.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                                String result = String.valueOf(set);
                                Log.i("DidItSet", result);
                                if (descriptorSetQueue.isEmpty()) {
                                    descriptorSetQueue.add(d);
                                    synchronized (q) {
                                        q.notify();
                                    }
                                } else {
                                    descriptorSetQueue.add(d);
                                }
                                //Log.i("Queue size after add", Integer.toString(descriptorSetQueue.size()));
                                //Log.i("Queue after add", descriptorSetQueue.toString());

                            }

                            mGatt.setCharacteristicNotification(characteristic, true);

                        } else if (ArrayUtils.contains(write_characteristics, shorthandChar)) {
                            Log.i("Read Characteristic:", characteristic.getUuid().toString());
                        }
                    }
                    new Thread(init).start();
                }
            }
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            Log.i("onCharacteristicReadV", characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, 0).toString());
            //Log.i("onCharacteristicReadP1", Integer.toString(characteristic.getPermissions()));
            //Log.i("onCharacteristicReadP2", Integer.toString(characteristic.getProperties()));
            Log.i("onCharacteristicReadS", Integer.toString(status));
            //gatt.disconnect();
            updateCharacteristic((characteristic));
            BLEservicesRunning = false;
            synchronized (serviceLock) {
                serviceLock.notify();
            }
        }

        @Override
        public void onCharacteristicChanged (BluetoothGatt gatt, BluetoothGattCharacteristic characteristic){
            //Log.i("onCharacteristicChanged", characteristic.toString());
            //Log.i("onCharacteristicChanged", characteristic.getUuid().toString());
            //Log.i("onCharacteristicChanged", Integer.toHexString(BLE_Characteristic.getAssignedNumber(characteristic.getUuid())));

            if(descriptorSetQueue.isEmpty()) {
                updateCharacteristic(characteristic);
            }

            else {
                Log.i("Characteristic changed", "Waiting for all descriptors to be set");
            }

        }

        @Override
        public void onCharacteristicWrite (BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            //Log.i("Written", characteristic.toString());
            //Log.i("Written", characteristic.getUuid().toString());
            Log.i("Written", Integer.toHexString(BLE_Characteristic.getAssignedNumber(characteristic.getUuid())));
            Log.i("WrittenStatus", Integer.toString(status));

            BLEservicesRunning = false;
            synchronized (serviceLock) {
                serviceLock.notify();
            }
        }

        @Override
        public void onDescriptorRead(BluetoothGatt gatt,BluetoothGattDescriptor descriptor, int status) {
            String value = new String(descriptor.getValue());
            String permission = new String(descriptor.getValue());
            //Log.i("onDescriptorReadV", value);
            //Log.i("onDescriptorReadP1", permission);
            //Log.i("onDescriptorReadS", Integer.toString(status));
        }

        @Override
        public void onDescriptorWrite (BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            //String value = new String(descriptor.getValue());
            //String permission = new String(descriptor.getValue());
            //Log.i("onDescriptorWriteV", value);
            //Log.i("onDescriptorWriteP1", permission);
            //Log.i("onDescriptorWriteS", Integer.toString(status));
            BLEservicesRunning = false;
            synchronized (serviceLock) {
                serviceLock.notify();
            }
        }
    };

    private void updateCharacteristic(BluetoothGattCharacteristic characteristic){
        TextView viewToUpdate = null;
        for (BLE_Characteristic test_collection : allBLE) {
            if (test_collection.BLE_Full == characteristic) {
                viewToUpdate = test_collection.BLE_View;
                break;
            }
        }
        Integer char_id = BLE_Characteristic.getAssignedNumber(characteristic.getUuid());
        String updateString = null;
        if (char_id >= 0xF000) {
            byte[] bytes = characteristic.getValue();
            ByteBuffer so1 = ByteBuffer.wrap(Arrays.copyOfRange(bytes, 0, 4)).order(ByteOrder.LITTLE_ENDIAN);
            Float so1a = so1.getFloat();
            updateString = so1a.toString();
            Log.i("Received characteristic", Integer.toHexString(char_id));
            Log.i("Float Method", so1a.toString());
        } else if ((char_id < 0xF000) & (char_id >= 0xE000)) {
            updateString = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, 0).toString();
            Log.i("Received characteristic", Integer.toHexString(char_id));
            Log.i("Int 8", updateString);
        } else if ((char_id < 0xE000) & (char_id >= 0xD000)) {
            updateString = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT16, 0).toString();
            Log.i("Received characteristic", Integer.toHexString(char_id));
            Log.i("Int 16", updateString);
        } else if ((char_id < 0xD000) & (char_id >= 0xC000)) {
            updateString = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT32, 0).toString();
            Log.i("Received characteristic", Integer.toHexString(char_id));
            Log.i("Int 32", updateString);
        }else {
            Log.i("Unknown characteristic", Integer.toHexString(char_id));
        }
        if (updateString != null) {
            Runnable t = new updateViews(viewToUpdate, updateString);
            BLE_Test.this.runOnUiThread(t);
        }
    }


    class setDescriptorThread implements Runnable{

        setDescriptorThread(){

        }
        public void run() {
            Log.i("Descriptor Thread", "Thread Started");
            while(!Thread.currentThread().isInterrupted()){
                while (descriptorSetQueue.isEmpty())
                    try{
                        Log.i("Descriptor Thread", "Queue has no objects, will wait");
                        synchronized (initWriteLock){
                            initWriteLock.notify();
                        }
                        synchronized (q) {
                            q.wait();
                        }
                    } catch (InterruptedException iex) {
                        Log.i("Descriptor Thread: ", iex.getMessage());
                    }
                Log.i("Descriptor Thread", "Must be something in queue");
                while (BLEservicesRunning == true){
                    try{
                        Log.i("Descriptor Thread", "BLE Services being used, will wait");
                        synchronized (serviceLock) {
                            serviceLock.wait();
                        }
                    } catch (InterruptedException iex) {
                        Log.i("Descriptor Thread: ", iex.getMessage());
                    }
                }
                BluetoothGattDescriptor nextToWrite = descriptorSetQueue.peek();
                BLEservicesRunning = true;
                boolean write = mGatt.writeDescriptor(nextToWrite);
                String resultW = String.valueOf(write);
                Log.i("DidItWrite", resultW);
                if (write == true){
                    descriptorSetQueue.remove(nextToWrite);
                }
                Log.i("Queue size", Integer.toString(descriptorSetQueue.size()));
                //Log.i("Queue", descriptorSetQueue.toString());
            }
        }
    }

    class updateViews implements Runnable {
        TextView viewToChange;
        String stringToSet;
        public updateViews(TextView viewToChange, String stringToSet) {
            this.viewToChange = viewToChange;
            this.stringToSet = stringToSet;
        }

        public void run() {
            viewToChange.setText(stringToSet);
        }
    }

    class initialReadThread implements Runnable{

        initialReadThread(){

        }
        public void run() {
            Log.i("Initial Read Thread", "Thread Started");
            while (!descriptorSetQueue.isEmpty())
                try{
                    Log.i("Initial Read Thread", "Queue has objects, will wait");
                    synchronized (initWriteLock) {
                        initWriteLock.wait();
                    }
                } catch (InterruptedException iex) {
                    Log.i("Initial Write Thread: ", iex.getMessage());
                }
            Log.i("Initial Write Thread", "Queue must be empty");
            int[] write_characteristics = getResources().getIntArray(R.array.writeCharacteristics);
            for (int i=0; i < write_characteristics.length; i++){
                int temp_char = write_characteristics[i];
                for (BLE_Characteristic temp_collection:allBLE){
                    if(temp_char == temp_collection.BLE_Short){
                        while (BLEservicesRunning == true){
                            try{
                                Log.i("Initial Write Thread", "BLE Services being used, will wait");
                                synchronized (serviceLock) {
                                    serviceLock.wait();
                                }
                            } catch (InterruptedException iex) {
                                Log.i("Initial Write Thread: ", iex.getMessage());
                            }
                        }
                        Log.i("Match of", Integer.toHexString(temp_collection.BLE_Short));
                        BLEservicesRunning = true;
                        Boolean resp = mGatt.readCharacteristic(temp_collection.BLE_Full);
                        Log.i("Successful?", resp.toString());
                        break;
                    }
                }
            }

            Log.i("Initial Write Thread", "Thread Done");

        }
    }

}