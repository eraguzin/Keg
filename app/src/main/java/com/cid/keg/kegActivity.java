package com.cid.keg;

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
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.NumberPicker;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import android.os.Handler;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.apache.commons.lang.ArrayUtils;

public class kegActivity extends AppCompatActivity {
    boolean deviceConnected=false;

    //System Views
    Button startButton;
    Switch switchButton;
    NumberPicker Digit1, Digit2, Digit3, Digit4;
    ImageView Num1, Num2, Num3, Num4, Fullness;

    private BluetoothAdapter mBluetoothAdapter;
    private Handler mHandler;
    private static final long SCAN_PERIOD = 10000;
    private BluetoothLeScanner mLEScanner;
    private ScanSettings settings;
    private List<ScanFilter> filters;
    private BluetoothGatt mGatt;
    private Queue<BluetoothGattDescriptor> descriptorSetQueue = new ConcurrentLinkedQueue<>();
    private final Object serviceLock = new Object();
    private final Object initWriteLock = new Object();
    boolean BLEservicesRunning = false;
    final setDescriptorThread q = new setDescriptorThread();
    initialReadThread init = new initialReadThread();
    List<BLE_Characteristic> allBLE = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_basic);
        mHandler = new Handler();
        //System Views
        startButton = (Button) findViewById(R.id.buttonStart);

        switchButton = (Switch) findViewById(R.id.buttonSwitch);
        switchButton.setChecked(false);
        Num1 = (ImageView) findViewById(R.id.Display1);
        Num2 = (ImageView) findViewById(R.id.Display2);
        Num3 = (ImageView) findViewById(R.id.Display3);
        Num4 = (ImageView) findViewById(R.id.Display4);
        Fullness = (ImageView) findViewById(R.id.Fullness);

        Digit1 = (NumberPicker) findViewById(R.id.Digit1);
        Digit2 = (NumberPicker) findViewById(R.id.Digit2);
        Digit3 = (NumberPicker) findViewById(R.id.Digit3);
        Digit4 = (NumberPicker) findViewById(R.id.Digit4);

        Digit1.setMinValue(1);
        Digit1.setMaxValue(1);
        Digit1.setWrapSelectorWheel(false);

        Digit2.setMinValue(4);
        Digit2.setMaxValue(9);
        Digit2.setWrapSelectorWheel(false);

        Digit3.setMinValue(0);
        Digit3.setMaxValue(10);
        Digit3.setWrapSelectorWheel(false);

        Digit4.setMinValue(0);
        Digit4.setMaxValue(10);
        Digit4.setWrapSelectorWheel(false);
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
        String Digit_1 = Integer.toHexString(Digit1.getValue());
        String Digit_2 = Integer.toHexString(Digit2.getValue());
        String Digit_3 = Integer.toHexString(Digit3.getValue());
        String Digit_4 = Integer.toHexString(Digit4.getValue());

        String total_string = Digit_1 + Digit_2 + "." + Digit_3 + Digit_4;

        Float valueToSend = Float.parseFloat(total_string);

        BluetoothGattCharacteristic toWriteTo = null;

        for (BLE_Characteristic temp : allBLE) {
            if (temp.BLE_Short == 0xF001) {
                toWriteTo = temp.BLE_Full;
                break;
            }
        }

        if (toWriteTo != null) {
            int bits = Float.floatToIntBits(valueToSend);
            //characteristic.setValue(mantissa, exponent, BluetoothGattCharacteristic.FORMAT_FLOAT, 0);
            toWriteTo.setValue(bits, BluetoothGattCharacteristic.FORMAT_UINT32, 0);

            Log.i("OnClickSend", "Sending " + total_string);
            BLEservicesRunning = true;
            boolean write = mGatt.writeCharacteristic(toWriteTo);
            String result = String.valueOf(write);
            Log.i("DidItSend", result);
            while (BLEservicesRunning) {
                try {
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
        else {
            Log.i("onClickSend", "No char found!");
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        int REQUEST_ENABLE_BT = 1;
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
                            .setDeviceName(getApplicationContext().getString(R.string.search_name))
                            .build();
            filters = new ArrayList<>();
            filters.add(scanFilter);
            //scanLeDevice(true);
            Log.i("onResume", "We've got our filters");
        }
    }


    public void onClickSwitch(View view) {
        if (deviceConnected) {
            mGatt.close();
        }
        mGatt = null;
        Intent intent = new Intent(this, BLE_Test.class);
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
            deviceConnected =true;
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
            int[] notification_characteristics = getResources().getIntArray(R.array.basicNotification);
            int[] write_characteristics = getResources().getIntArray(R.array.basicWrite);
            for (BluetoothGattService service : services) {
                if (ArrayUtils.contains(acceptable_services, BLE_Characteristic.getAssignedNumber(service.getUuid()))) {
                    Log.i("Service found", service.getUuid().toString());
                    allBLE.clear();
                    List<BluetoothGattCharacteristic> characteristics = service.getCharacteristics();
                    List<BluetoothGattDescriptor> Descr;
                    for (BluetoothGattCharacteristic characteristic : characteristics) {
                        Integer shorthandChar = BLE_Characteristic.getAssignedNumber(characteristic.getUuid());
                        if (ArrayUtils.contains(notification_characteristics, shorthandChar)) {
                            TextView corresponding_view = BLE_Characteristic.getIdFromShortChar(kegActivity.this, shorthandChar);
                            allBLE.add(new BLE_Characteristic(shorthandChar, characteristic, corresponding_view));
                            Log.i("Characteristic found", characteristic.getUuid().toString());
                            //mGatt.readCharacteristic(characteristic);

                            //characteristic.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE);
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
                            TextView corresponding_view = BLE_Characteristic.getIdFromShortChar(kegActivity.this, shorthandChar);
                            allBLE.add(new BLE_Characteristic(shorthandChar, characteristic, corresponding_view));
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
            //String value = new String(descriptor.getValue());
            //String permission = new String(descriptor.getValue());
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
            Runnable t = new updateViews(char_id, updateString);
            kegActivity.this.runOnUiThread(t);
        }
    }


    private class setDescriptorThread implements Runnable{

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
                while (BLEservicesRunning){
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
                if (write){
                    descriptorSetQueue.remove(nextToWrite);
                }
                Log.i("Queue size", Integer.toString(descriptorSetQueue.size()));
                //Log.i("Queue", descriptorSetQueue.toString());
            }
        }
    }

    private class updateViews implements Runnable {
        Integer charID;
        String stringToSet;
        private updateViews(Integer char_ID, String stringToSet) {
            this.charID = char_ID;
            this.stringToSet = stringToSet;
        }

        public void run() {
            if (charID == 0xF000){
                Log.i("Pressure is", stringToSet);
                Character Char1 = stringToSet.charAt(0);
                Log.i("Char1 is", Character.toString(Char1));
                int Digit_1 = Character.getNumericValue(Char1);
                final Integer ImageNum = doImages(Digit_1);
                Num1.setImageResource(ImageNum);

                Character Char2 = stringToSet.charAt(1);
                Log.i("Char2 is", Character.toString(Char2));
                int Digit_2 = Character.getNumericValue(Char2);
                final Integer ImageNum2 = doImages(Digit_2);
                Num2.setImageResource(ImageNum2);

                Character Char3 = stringToSet.charAt(3);
                Log.i("Char3 is", Character.toString(Char3));
                int Digit_3 = Character.getNumericValue(Char3);
                final Integer ImageNum3 = doImages(Digit_3);
                Num3.setImageResource(ImageNum3);

                Character Char4 = stringToSet.charAt(4);
                Log.i("Char4 is", Character.toString(Char4));
                int Digit_4 = Character.getNumericValue(Char4);
                final Integer ImageNum4 = doImages(Digit_4);
                Num4.setImageResource(ImageNum4);
            }
            else if (charID == 0xE001){
                Log.i("Fullness is", stringToSet);
                final Integer ImageNum = doFullness(Integer.parseInt(stringToSet));
                Fullness.setImageResource(ImageNum);
            }
            else if (charID == 0xF001){
                Log.i("Ref Pressure is", stringToSet);
                Character Char1 = stringToSet.charAt(0);
                Log.i("Char1 is", Character.toString(Char1));
                int Digit_1 = Character.getNumericValue(Char1);
                Digit1.setValue(Digit_1);

                Character Char2 = stringToSet.charAt(1);
                Log.i("Char2 is", Character.toString(Char2));
                int Digit_2 = Character.getNumericValue(Char2);
                Digit2.setValue(Digit_2);

                Character Char3 = stringToSet.charAt(3);
                Log.i("Char3 is", Character.toString(Char3));
                int Digit_3 = Character.getNumericValue(Char3);
                Digit3.setValue(Digit_3);

                try {
                    Character Char4 = stringToSet.charAt(4);
                    Log.i("Char4 is", Character.toString(Char4));
                    int Digit_4 = Character.getNumericValue(Char4);
                    Digit4.setValue(Digit_4);
                }
                catch (StringIndexOutOfBoundsException e){
                    Digit4.setValue(0);
                }
            }
        }
    }

    private class initialReadThread implements Runnable{

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
            int[] write_characteristics = getResources().getIntArray(R.array.basicWrite);
            for (int temp_char:write_characteristics){
                for (BLE_Characteristic temp_collection:allBLE){
                    if(temp_char == temp_collection.BLE_Short){
                        while (BLEservicesRunning){
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

    public Integer doImages (int num){
        Integer theID = null;
        switch (num) {
            case 0:
                theID = getResources().getIdentifier("zero", "drawable", getPackageName());
                break;

            case 1:
                theID = getResources().getIdentifier("one", "drawable", getPackageName());
                break;

            case 2:
                theID = getResources().getIdentifier("two", "drawable", getPackageName());
                break;

            case 3:
                theID = getResources().getIdentifier("three", "drawable", getPackageName());
                break;

            case 4:
                theID = getResources().getIdentifier("four", "drawable", getPackageName());
                break;

            case 5:
                theID = getResources().getIdentifier("five", "drawable", getPackageName());
                break;

            case 6:
                theID = getResources().getIdentifier("six", "drawable", getPackageName());
                break;

            case 7:
                theID = getResources().getIdentifier("seven", "drawable", getPackageName());
                break;

            case 8:
                theID = getResources().getIdentifier("eight", "drawable", getPackageName());
                break;

            case 9:
                theID = getResources().getIdentifier("nine", "drawable", getPackageName());
                break;

            default:
                break;
        }
        return theID;

    }

    public Integer doFullness (Integer num){
        Integer theID;
        switch (num) {
            case 100:
                theID = getResources().getIdentifier("full", "drawable", getPackageName());
                break;

            case 75:
                theID = getResources().getIdentifier("a34", "drawable", getPackageName());
                break;

            case 50:
                theID = getResources().getIdentifier("a12", "drawable", getPackageName());
                break;

            case 25:
                theID = getResources().getIdentifier("a14", "drawable", getPackageName());
                break;

            case 0:
                theID = getResources().getIdentifier("empty", "drawable", getPackageName());
                break;

            default:
                theID = getResources().getIdentifier("full", "drawable", getPackageName());
                break;
        }
        return theID;

    }

}