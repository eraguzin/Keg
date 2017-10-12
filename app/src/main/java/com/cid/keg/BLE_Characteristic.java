package com.cid.keg;

import android.app.Activity;
import android.bluetooth.BluetoothGattCharacteristic;
import android.content.Context;
import android.util.Log;
import android.widget.TextView;

import java.util.UUID;

/**
 * Created by Eric on 9/7/2017.
 */

public class BLE_Characteristic {

    public int BLE_Short;
    public BluetoothGattCharacteristic BLE_Full;
    public TextView BLE_View;

    //constructor
    public BLE_Characteristic(int BLE_Short_in, BluetoothGattCharacteristic BLE_Full_in, TextView BLE_View_in) {
        BLE_Short = BLE_Short_in;
        BLE_Full = BLE_Full_in;
        BLE_View = BLE_View_in;
    }

    public static TextView getIdFromShortChar(Activity context, Integer BLE_Short_in){
        TextView corresponding_view = null;
        switch (BLE_Short_in){
            case (0xF001):
                corresponding_view = (TextView) (context.findViewById(R.id.receiveRefPressure));
                Log.i("Found", "BLE_Characteristic Class --> Found Reference Pressure");
                break;

            case (0xF000):
                corresponding_view = (TextView) (context.findViewById(R.id.receivePressure));
                Log.i("Found", "BLE_Characteristic Class --> Found Pressure");
                break;

            case (0xF002):
                corresponding_view = (TextView) (context.findViewById(R.id.receiveError));
                Log.i("Found", "BLE_Characteristic Class --> Found Error");
                break;

            case (0xD000):
                corresponding_view = (TextView) (context.findViewById(R.id.receivekP));
                Log.i("Found", "BLE_Characteristic Class --> Found kP");
                break;

            case (0xD001):
                corresponding_view = (TextView) (context.findViewById(R.id.receivekI));
                Log.i("Found", "BLE_Characteristic Class --> Found kI");
                break;

            case (0xD002):
                corresponding_view = (TextView) (context.findViewById(R.id.receivekD));
                Log.i("Found", "BLE_Characteristic Class --> Found kD");
                break;

            case (0xF003):
                corresponding_view = (TextView) (context.findViewById(R.id.receiveP));
                Log.i("Found", "BLE_Characteristic Class --> Found P");
                break;

            case (0xF004):
                corresponding_view = (TextView) (context.findViewById(R.id.receiveI));
                Log.i("Found", "BLE_Characteristic Class --> Found I");
                break;

            case (0xF005):
                corresponding_view = (TextView) (context.findViewById(R.id.receiveD));
                Log.i("Found", "BLE_Characteristic Class --> Found D");
                break;

            case (0xF006):
                corresponding_view = (TextView) (context.findViewById(R.id.receiveIntegral));
                Log.i("Found", "BLE_Characteristic Class --> Found Integral");
                break;

            case (0xE004):
                corresponding_view = (TextView) (context.findViewById(R.id.receivePump));
                Log.i("Found", "BLE_Characteristic Class --> Found Pump");
                break;

            case (0xF007):
                corresponding_view = (TextView) (context.findViewById(R.id.receiveVersion));
                Log.i("Found", "BLE_Characteristic Class --> Found Version");
                break;

            case (0xC001):
                corresponding_view = (TextView) (context.findViewById(R.id.receiveUpdate));
                Log.i("Found", "BLE_Characteristic Class --> Found Update Time");
                break;

            case (0xD004):
                corresponding_view = (TextView) (context.findViewById(R.id.receiveSettle));
                Log.i("Found", "BLE_Characteristic Class --> Found Settle Time");
                break;

            case (0xE003):
                corresponding_view = (TextView) (context.findViewById(R.id.receiveSize));
                Log.i("Found", "BLE_Characteristic Class --> Found Keg Size");
                break;

            case (0xF00B):
                corresponding_view = (TextView) (context.findViewById(R.id.receivebatt));
                Log.i("Found", "BLE_Characteristic Class --> Found Battery");
                break;

            case (0xF009):
                corresponding_view = (TextView) (context.findViewById(R.id.receivedP));
                Log.i("Found", "BLE_Characteristic Class --> Found dp");
                break;

            case (0xF00A):
                corresponding_view = (TextView) (context.findViewById(R.id.receivedT));
                Log.i("Found", "BLE_Characteristic Class --> Found dpdt");
                break;

            case (0xE000):
                corresponding_view = (TextView) (context.findViewById(R.id.receivemode));
                Log.i("Found", "BLE_Characteristic Class --> Found Mode");
                break;

            case (0xF008):
                corresponding_view = (TextView) (context.findViewById(R.id.receivestart_p));
                Log.i("Found", "BLE_Characteristic Class --> Found Start Pressure");
                break;

            case (0xE002):
                corresponding_view = (TextView) (context.findViewById(R.id.receiveBlow));
                Log.i("Found", "BLE_Characteristic Class --> Found Test Blow");
                break;

            case (0xD003):
                corresponding_view = (TextView) (context.findViewById(R.id.receiveTT));
                Log.i("Found", "BLE_Characteristic Class --> Found Test Time");
                break;

            case (0xC000):
                corresponding_view = (TextView) (context.findViewById(R.id.receiveTestWait));
                Log.i("Found", "BLE_Characteristic Class --> Found Test Wait");
                break;

            case (0xE001):
                corresponding_view = (TextView) (context.findViewById(R.id.receiveFullness));
                Log.i("Found", "BLE_Characteristic Class --> Found Fullness");
                break;

            default:
                Log.i("Error", "BLE_Characteristic Class --> Characteristic not found");
        }

        return corresponding_view;

    }

    public static int getAssignedNumber(UUID uuid) {
        // Keep only the significant bits of the UUID
        return (int) ((uuid.getMostSignificantBits() & 0x0000FFFF00000000L) >> 32);
    }

}