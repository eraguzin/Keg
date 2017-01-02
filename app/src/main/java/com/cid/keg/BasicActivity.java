package com.cid.keg;

import android.media.Image;
import android.support.annotation.RequiresPermission;
import android.util.Log;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.NumberPicker;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.ImageView;
import android.widget.Toast;
import android.os.Handler;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Set;
import java.util.UUID;

public class BasicActivity extends Activity {
    //    private final String DEVICE_NAME="MyBTBee";
    private static final String DEVICE_ADDRESS = "20:14:10:30:05:38";
    private final UUID PORT_UUID = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb");//Serial Port Service ID
    private BluetoothDevice device;
    private BluetoothSocket socket;
    private OutputStream outputStream;
    private InputStream inputStream;

    boolean deviceConnected = false;
    byte buffer[];
    boolean stopThread = true;
    boolean stopStartThread;
    String fullString = "";
    String singleCommand = "";
    boolean saving = false;
    boolean iteration_done = false;
    char beginning = '@';
    char ending = '!';


    //System Views
    Button startButton;
    Switch switchButton;
    TextView pressureRefReceive, pressureReceive;
    SeekBar barBrightness;
    NumberPicker Digit1, Digit2, Digit3, Digit4;
    ImageView Num1, Num2, Num3, Num4;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_basic);

        //System Views
        startButton = (Button) findViewById(R.id.buttonStart);

        pressureRefReceive = (TextView) findViewById(R.id.receiveRefPressure);
        pressureReceive = (TextView) findViewById(R.id.receivePressure);

        switchButton = (Switch) findViewById(R.id.buttonSwitch);
        switchButton.setChecked(false);
        beginConnection();
        beginListenForData();

        barBrightness = (SeekBar) findViewById(R.id.BrightnessSeeker);

        barBrightness.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
            Integer progressChanged = 0;

            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser){
                progressChanged = progress;
            }

            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            public void onStopTrackingTouch(SeekBar seekBar) {
                String string = "@g";
                string = string.concat(Integer.toString(progressChanged));
                string = string.concat("!");
                try {
                    outputStream.write(string.getBytes());
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
        Num1 = (ImageView) findViewById(R.id.Display1);
        Num2 = (ImageView) findViewById(R.id.Display2);
        Num3 = (ImageView) findViewById(R.id.Display3);
        Num4 = (ImageView) findViewById(R.id.Display4);

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

    }

    public boolean BTinit() {
        boolean found = false;
        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter == null) {
            Toast.makeText(getApplicationContext(), "Device doesnt Support Bluetooth", Toast.LENGTH_SHORT).show();
        }
        if (bluetoothAdapter != null) {
            if (!bluetoothAdapter.isEnabled()) {
                Intent enableAdapter = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableAdapter, 0);
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            Set<BluetoothDevice> bondedDevices = bluetoothAdapter.getBondedDevices();
            if (bondedDevices.isEmpty()) {
                Toast.makeText(getApplicationContext(), "Please Pair the Device first", Toast.LENGTH_SHORT).show();
            } else {
                for (BluetoothDevice iterator : bondedDevices) {
                    Log.d("ADD", DEVICE_ADDRESS);
                    Log.d("GET", iterator.getAddress());
                    if (iterator.getAddress().equals(DEVICE_ADDRESS)) {
                        device = iterator;
                        found = true;
                        break;
                    }
                }
            }

        }
        return found;
    }

    public boolean BTconnect() {
        boolean connected = true;
        try {
            socket = device.createRfcommSocketToServiceRecord(PORT_UUID);
            socket.connect();
        } catch (IOException e) {
            e.printStackTrace();
            connected = false;
        }
        if (connected) {
            try {
                outputStream = socket.getOutputStream();
            } catch (IOException e) {
                e.printStackTrace();
            }
            try {
                inputStream = socket.getInputStream();
            } catch (IOException e) {
                e.printStackTrace();
            }

        }


        return connected;
    }

    public void onClickStart(View view) {
        if (BTinit()) {
            Log.d("", "Initialized");
            if (BTconnect()) {
                Log.d("", "Connected");
                deviceConnected = true;
                beginListenForData();
            }
        }
    }

    void beginConnection() {
        stopStartThread = false;
        buffer = new byte[1024];
        final Thread thread = new Thread(new Runnable() {
            public void run() {
                while (!Thread.currentThread().isInterrupted() && !stopStartThread) {
                    if (BTinit()) {
                        Log.d("", "Initialized");
                        try {
                            Thread.sleep(50);
                        } catch (InterruptedException e) {
                            Log.d("d", "Interrupted");
                        }
                        if (BTconnect()) {
                            Log.d("", "Connected");
                            deviceConnected = true;
                            stopStartThread = true;
                            stopThread = false;
                            Log.d("d", "Communication Opened");
                        } else {
                            Log.d("d", "No Communication");
                        }

                    } else {
                        Log.d("d", "No Connection");
                    }
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        Log.d("d", "Interrupted");
                    }
                }
            }
        }
        );

        thread.start();
    }

    void beginListenForData() {
        final Handler handler = new Handler();
        buffer = new byte[1024];
        final Thread thread = new Thread(new Runnable() {
            public void run() {
                Log.d("d", "Thread Opened");
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                    Log.d("d", "Interrupted");
                }
                while (!Thread.currentThread().isInterrupted() && !stopThread) {
                    try {
                        int byteCount = inputStream.available();
                        if (byteCount > 0) {
                            byte[] rawBytes = new byte[byteCount];
                            int bytesread = inputStream.read(rawBytes);
                            final String receivedString = new String(rawBytes, "UTF-8");
                            fullString = receivedString;
                            iteration_done = false;
                            while (!iteration_done) {
                                final String toSend = ProcessIncoming();
                                if (!saving) {
                                    Integer ViewID = SendToProper(toSend);
                                    if (ViewID != null) {
                                        if (ViewID == 2) {
                                            Character Char1 = toSend.charAt(1);
                                            int Digit1 = Character.getNumericValue(Char1);
                                            final Integer ImageNum = doImages(Digit1);
                                            handler.post(new Runnable() {
                                                             public void run() {
                                                                 Num1.setImageResource(ImageNum);
                                                             }
                                                         }
                                            );
                                            Character Char2 = toSend.charAt(2);
                                            int Digit2 = Character.getNumericValue(Char2);
                                            final Integer ImageNum2 = doImages(Digit2);
                                            handler.post(new Runnable() {
                                                             public void run() {
                                                                 Num2.setImageResource(ImageNum2);
                                                             }
                                                         }
                                            );
                                            Character Char3 = toSend.charAt(4);
                                            int Digit3 = Character.getNumericValue(Char3);
                                            final Integer ImageNum3 = doImages(Digit3);
                                            handler.post(new Runnable() {
                                                             public void run() {
                                                                 Num3.setImageResource(ImageNum3);
                                                             }
                                                         }
                                            );
                                            Character Char4 = toSend.charAt(5);
                                            int Digit4 = Character.getNumericValue(Char4);
                                            final Integer ImageNum4 = doImages(Digit4);
                                            handler.post(new Runnable() {
                                                             public void run() {
                                                                 Num4.setImageResource(ImageNum4);
                                                             }
                                                         }
                                            );
                                        }
                                        if (ViewID == barBrightness.getId()) {
                                            final Integer Brightness = Integer.parseInt(toSend.substring(1));
                                            handler.post(new Runnable() {
                                                             public void run() {
                                                                 barBrightness.setProgress(Brightness);
                                                             }
                                                         }
                                            );
                                        }
                                    }

                                }
                            }

                        }
                    } catch (IOException ex) {
                        stopThread = true;
                    }
                    try {
                        Thread.sleep(10);
                    } catch (InterruptedException e) {
                        Log.d("d", "Interrupted");
                    }
                }
            }
        }
        );

        thread.start();
    }

    public void onClickSwitch(View view) {
        if (deviceConnected) {

            try {
                String sendText = "@";
                sendText += "z";
                sendText += "!";
                outputStream.write(sendText.getBytes());
                Log.d("sent", sendText);
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
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
    }

    public String ProcessIncoming() {
        if (!saving) {
            int start_pos = fullString.indexOf(beginning);
            if ((start_pos != -1) && (start_pos != 0)) {
                fullString = fullString.substring(start_pos);
                start_pos = fullString.indexOf(beginning);
            }
            if (start_pos != -1) {
                int end_pos = fullString.indexOf(ending);
                if (end_pos != -1) {
                    singleCommand = fullString.substring(start_pos + 1, end_pos);
                    fullString = fullString.substring(end_pos + 1);
                    return singleCommand;
                } else {
                    singleCommand = fullString.substring(start_pos + 1);
                    iteration_done = true;
                    saving = true;
                    return "0";
                }
            } else {
                iteration_done = true;
                return "0";
            }
        } else {
            int end_pos = fullString.indexOf(ending);
            if (end_pos != -1) {
                singleCommand += fullString.substring(0, end_pos);
                fullString = fullString.substring(end_pos + 1);
                saving = false;
                return singleCommand;
            } else {
                singleCommand += fullString;
                iteration_done = true;
                return "0";
            }
        }

    }

    public Integer SendToProper(String processedString) {
        char indicator = processedString.charAt(0);
        Integer view;
        switch (indicator) {
            case 'a':
                view = 1;
                break;

            case 'b':
                view = 2;
                break;

            case 'g':
                view = barBrightness.getId();
                break;

            case 'y':
                view = null;
                break;

            case '0':
                view = null;
                break;

            default:
                Log.d("received", processedString);
                try {
                    String sendText = "@";
                    sendText += "y";
                    sendText += "!";
                    outputStream.write(sendText.getBytes());
                } catch (IOException e) {
                    e.printStackTrace();
                }
                view = null;
        }
        return view;
    }

    public void onClickSend(View v) {
        Integer Num1 = Digit1.getValue();
        Integer Num2 = Digit2.getValue();
        Integer Num3 = Digit3.getValue();
        Integer Num4 = Digit4.getValue();

        String sendText = "@a";
        sendText += Integer.toString(Num1);
        sendText += Integer.toString(Num2);
        sendText += ".";
        sendText += Integer.toString(Num3);
        sendText += Integer.toString(Num4);
        sendText += "!";
        try {
            outputStream.write(sendText.getBytes());
        }
        catch (IOException e) {
            e.printStackTrace();
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
}