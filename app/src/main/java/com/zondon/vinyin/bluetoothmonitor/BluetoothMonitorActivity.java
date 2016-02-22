package com.zondon.vinyin.bluetoothmonitor;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.util.UUID;

public class BluetoothMonitorActivity extends AppCompatActivity {
    String TAG = "BluetoothMonitorActivity";
    BluetoothAdapter btAdapt= null;//手机蓝牙适配器
    BluetoothDevice btMonitor = null;//监护仪蓝牙
    OutputStream outputStream = null;
    InputStream inputStream = null;
    boolean isShakingHandsSucess = false;//是否握手成功
    boolean isConnected = false;//是否握手成功
    int  bluetoothConnectStatus =  BluetoothDevice.BOND_NONE;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bluetooth_monitor);
        btAdapt = BluetoothAdapter.getDefaultAdapter();// 初始化本机蓝牙功能
        BluethoothCastReceiver receiver = new BluethoothCastReceiver();
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        registerReceiver(receiver, filter);
        filter = new IntentFilter(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
        registerReceiver(receiver, filter);
        if(!btAdapt.isEnabled())
        {
            btAdapt.enable();
        }
        if (btAdapt.isDiscovering()) {
            btAdapt.cancelDiscovery();
        }
        btAdapt.startDiscovery();
    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu){
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()){
            case R.id.connect:
                if(!btAdapt.isEnabled())
                {
                    btAdapt.enable();
                }
                if(bluetoothConnectStatus ==  BluetoothDevice.BOND_BONDED )
                {
                    new Thread(sendable).start();
                    isConnected = true;
                    Log.d(TAG,"BOND_BONDED");
                }else if(bluetoothConnectStatus ==  BluetoothDevice.BOND_NONE) {
                    btAdapt.startDiscovery();
                    Log.d(TAG, "BOND_NONE");
                }
                break;
            case R.id.close:
                if(btAdapt.isEnabled())
                {
                    btAdapt.disable();
                }
                break;
        }
        return  true;
    }
    //类中类 作蓝
    class BluethoothCastReceiver extends BroadcastReceiver {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            //找到设备
            if (BluetoothDevice.ACTION_FOUND.equals(action) || (BluetoothDevice.ACTION_BOND_STATE_CHANGED.equals(action))) {
                btMonitor = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                int deviceState = btMonitor.getBondState();
                switch (deviceState) {
                    case BluetoothDevice.BOND_NONE:///未配对进行配对
                        Log.d(TAG, "receiver BOND_NONE");
                        // 配对
                        try {
                            Method createBondMethod = BluetoothDevice.class.getMethod("createBond");
                            createBondMethod.invoke(btMonitor);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        break;
                    case BluetoothDevice.BOND_BONDED:///已配对进行连接
                        Log.d(TAG, "receiver BOND_BONDED");
                        btAdapt.cancelDiscovery();
                        bluetoothConnectStatus =  BluetoothDevice.BOND_BONDED;
                        break;
                    case BluetoothDevice.BOND_BONDING:///已配对进行连接
                        Log.d(TAG, "receiver BOND_BONDING");
                        break;
                }
            }
        }
    }
    Runnable sendable = new Runnable() {
        @Override
        public void run() {
            Log.d(TAG, "run ...");
            try {
                connect(btMonitor);
            } catch (IOException e) {
                Log.d(TAG, "connect failure");
                e.printStackTrace();
            }
            Log.d(TAG, "before while");
            if (inputStream == null || outputStream == null)
            {
                Log.d(TAG, "inputStream or inputStream is null");
                return;
            }
            byte[] buffer = new byte[1024];
            int i = 0;
            while(true)//接收数据
            {   i++;
                try {
                    int bytes = inputStream.read(buffer);
                    }
                catch(IOException e){
                            e.printStackTrace();
                }
                try {
                    Thread.sleep(100);
                }
                catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    };
    private void connect(BluetoothDevice device) throws IOException {
        // 固定的UUID
        String SPP_UUID = "00001101-0000-1000-8000-00805F9B34FB";
        UUID uuid = UUID.fromString(SPP_UUID);
        BluetoothSocket socket = device.createRfcommSocketToServiceRecord(uuid);
        Log.d(TAG,"will connect ");
        socket.connect();
        Log.d(TAG, "before outputStream = ");
        try {
            inputStream = socket.getInputStream();
            outputStream = socket.getOutputStream();
        } catch (IOException e) {
            Log.e(TAG, "temp sockets not created", e);
        }
        Log.d(TAG,"after outputStream =");
    }
}
