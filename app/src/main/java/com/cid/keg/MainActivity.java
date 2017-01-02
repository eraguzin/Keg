package com.cid.keg;

//things have changed!

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
import android.widget.EditText;
import android.widget.Switch;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;
import android.os.Handler;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Set;
import java.util.UUID;

public class MainActivity extends Activity {
    //    private final String DEVICE_NAME="MyBTBee";
    private static final String DEVICE_ADDRESS="20:14:10:30:05:38";
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
            SetReceive,DispReceive;

    Switch switchButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

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
        switchButton = (Switch) findViewById(R.id.buttonSwitch);
        switchButton.setChecked(false);
    }

    public boolean BTinit()
    {
        boolean found=false;
        BluetoothAdapter bluetoothAdapter=BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter == null) {
            Toast.makeText(getApplicationContext(),"Device doesnt Support Bluetooth",Toast.LENGTH_SHORT).show();
        }
        if(bluetoothAdapter != null) {
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
                    rawIn.append(DEVICE_ADDRESS);
                    rawIn.append(iterator.getAddress());
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

    public boolean BTconnect()
    {
        boolean connected=true;
        try {
            socket = device.createRfcommSocketToServiceRecord(PORT_UUID);
            socket.connect();
        } catch (IOException e) {
            e.printStackTrace();
            connected=false;
        }
        if(connected)
        {
            try {
                outputStream=socket.getOutputStream();
            } catch (IOException e) {
                e.printStackTrace();
            }
            try {
                inputStream=socket.getInputStream();
            } catch (IOException e) {
                e.printStackTrace();
            }

        }


        return connected;
    }

    public void onClickStart(View view) {
        if(BTinit())
        {
            Log.d("", "Initialized");
            if(BTconnect())
            {
                Log.d("", "Connected");
                deviceConnected=true;
                beginListenForData();
                rawIn.setText("\nConnection Opened!\n");
            }
            else
            {
                rawIn.setText("\nCannot Communicate With Device\n");
            }

        }
        else
        {
            rawIn.setText("\nCannot Initialize Connection\n");
        }
    }

    void beginListenForData()
    {
        final Handler handler = new Handler();
        stopThread = false;
        buffer = new byte[1024];
        final Thread thread  = new Thread(new Runnable()
        {
            public void run()
            {
                while(!Thread.currentThread().isInterrupted() && !stopThread)
                {
                    try
                    {
                        int byteCount = inputStream.available();
                        if(byteCount > 0)
                        {
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

                                        final TextView SendItTo = (TextView) findViewById(ViewID);
                                        handler.post(new Runnable() {
                                                         public void run() {
                                                             //rawIn.setText(receivedString);
                                                             SendItTo.setText(toSend.substring(1));
                                                         }
                                                     }
                                        );
                                    }

                                }
                                else {
                                    handler.post(new Runnable() {
                                                     public void run() {
                                                         Status1.setText(getString(R.string.waiting));
                                                     }
                                                 }
                                    );
                                }
                            }

                        }
                    }
                    catch (IOException ex) {
                        stopThread = true;
                    }
                    try{
                        Thread.sleep(10);
                    }catch(InterruptedException e){
                        Log.d("d", "Interrupted");
                    }
                }
            }
        }
        );

        thread.start();
    }

    public void onClickSend(View view) {
        String string = rawOut.getText().toString();
        string = string.concat("\n");
        try {
            outputStream.write(string.getBytes());
        } catch (IOException e) {
            e.printStackTrace();
        }
        Status1.setText(string);

    }

    public void onClickStop(View view) throws IOException {
        stopThread = true;
        outputStream.close();
        inputStream.close();
        socket.close();deviceConnected=false;
        rawIn.setText("\nConnection Closed!");
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
                    fullString = fullString.substring(end_pos+1);
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
        }
        else{
            int end_pos = fullString.indexOf(ending);
            if (end_pos != -1) {
                singleCommand += fullString.substring(0,end_pos);
                fullString = fullString.substring(end_pos+1);
                saving = false;
                return singleCommand;
            }
            else{
                singleCommand += fullString;
                iteration_done = true;
                return "0";
            }
        }

    }

    public Integer SendToProper(String processedString){
        char indicator = processedString.charAt(0);
        Integer view;
        switch(indicator) {
            case 'a':
                view = pressureRefReceive.getId();
                break;

            case 'b':
                view = pressureReceive.getId();
                break;

            case 'c':
                view = errorReceive.getId();
                break;

            case 'p':
                view = kPReceive.getId();
                break;

            case 'i':
                view = kIReceive.getId();
                break;

            case 'd':
                view = kDReceive.getId();
                break;

            case 'P':
                view = PReceive.getId();
                break;

            case 'I':
                view = IReceive.getId();
                break;

            case 'D':
                view = DReceive.getId();
                break;

            case 'u':
                view = UpdateReceive.getId();
                break;

            case 'm':
                view = SetReceive.getId();
                break;

            case 's':
                view = DispReceive.getId();
                break;

            case 'g':
                view = BrightnessReceive.getId();
                break;

            case 'n':
                view = IntegralReceive.getId();
                break;

            case 'x':
                view = PumpReceive.getId();
                break;

            case '0':
                view = PumpReceive.getId();
                break;

            default:
                view = null;
        }
        return view;
    }

    public void onClickParameter(View v) {
        int rowId = ((View) v.getParent()).getId();
        TableRow row = (TableRow)findViewById(rowId);
        EditText grabFrom = (EditText)row.getChildAt(2);
        String sendText = "@";
        sendText += grabFrom.getTag().toString();
        sendText += grabFrom.getText().toString();
        sendText += "!";
        try {
            outputStream.write(sendText.getBytes());
        }
        catch (IOException e) {
            e.printStackTrace();
        }
        Status2.setText(sendText);
    }
}