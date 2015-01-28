package com.dev.siel.regaliasbt;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Set;
import java.util.UUID;


public class MainActivity extends ActionBarActivity {

    final int MESSAGE_READ = 9999; // its only identifier to tell to handler what to do with data you passed through.
    private static final int REQUEST_ENABLE_BT = 1;
    private static final String INFO_TAG = "Information" ;
    private static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    private boolean hasBluetooth = true;
    private BluetoothAdapter mBluetoothAdapter;//soy yo
    ArrayAdapter<String> mArrayAdapter;
    ArrayList<BluetoothDevice> devices;//array con todos los dispositivos que encuentre
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mArrayAdapter = new ArrayAdapter<String>(this,android.R.layout.simple_list_item_1);
        devices = new ArrayList<BluetoothDevice>();
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        setContentView(R.layout.activity_main);
        if(hasBluetooth()){
            if (!mBluetoothAdapter.isEnabled()) {
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
                Log.i(INFO_TAG,"Bluetooth Enabled");
            }
        }

    }

    private boolean hasBluetooth() {
        if(mBluetoothAdapter == null){
            //El dispositivo no tiene bluetooth y la aplicación se cerrará
            Toast.makeText(this,"Tu dispositivo no soporta bluetooth",Toast.LENGTH_SHORT).show();
            return false;
        }
        else
            return true;
    }

    public void discoverDevices(View v){
        if(hasBluetooth){
            IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
            devices.clear();
            mArrayAdapter.clear(); //inicializo de nuevo mArrayAdapter para reinicializar cada vez que presiono el boton
            registerReceiver(mReceiver, filter); // Don't forget to unregister during onDestroy
            mBluetoothAdapter.startDiscovery();//TENER CUIDADO CON ESTO, esto bloqueo el cel por 12 seg.
            Toast.makeText(this,"Searching for devices!",Toast.LENGTH_LONG).show();
        }else{
            Toast.makeText(this,"Tu dispositivo no soporta Bluetooth",Toast.LENGTH_LONG).show();
        }
    }

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            final ListView listaEncontrados = (ListView) findViewById(R.id.listView);
            listaEncontrados.setLongClickable(true);
            listaEncontrados.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
                @Override
                public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                    Log.i(INFO_TAG,"On Long CLick with item: "+position);
                    Log.i(INFO_TAG,"Data: ");
                    Log.i(INFO_TAG,"Name: "+devices.get(position).getName());
                    Log.i(INFO_TAG,"Address: "+devices.get(position).getAddress());
                    ConnectThread conectar = new ConnectThread(devices.get(position));
                    conectar.start();
                    return false;
                }
            });
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                devices.add(device);
                mArrayAdapter.add(device.getName() + "\n" + device.getAddress());
            }
            listaEncontrados.setAdapter(mArrayAdapter);
        }
    };


    @Override
    public void onDestroy(){
        super.onDestroy();
        try {
            unregisterReceiver(mReceiver);
        }catch (IllegalArgumentException e){
            e.printStackTrace();
        }
    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private class ConnectThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final BluetoothDevice mmDevice;

        public ConnectThread(BluetoothDevice device) {
            // Use a temporary object that is later assigned to mmSocket,
            // because mmSocket is final
            BluetoothSocket tmp = null;
            //mmDevice = device;
            mmDevice = BluetoothAdapter.getDefaultAdapter().getRemoteDevice(device.getAddress());

            // Get a BluetoothSocket to connect with the given BluetoothDevice
            try {
                // MY_UUID is the app's UUID string, also used by the server code
                Method m = mmDevice.getClass().getMethod("createRfcommSocket", new Class[] {int.class});
                tmp = (BluetoothSocket) m.invoke(device, 1);

                //tmp = mmDevice.createRfcommSocketToServiceRecord(MY_UUID);
                //tmp =(BluetoothSocket) mmDevice.getClass().getMethod("createRfcommSocket", new Class[] {int.class}).invoke(device,1);
            } catch (Exception e) {
                Log.e("Error: ", "Error creando el socket");
            }
            mmSocket = tmp;
        }

        public void run() {
            // Cancel discovery because it will slow down the connection

            mBluetoothAdapter.cancelDiscovery();
            Log.i("Posición","estoy en el run del thread");
            try {
                // Connect the device through the socket. This will block
                // until it succeeds or throws an exception
                mmSocket.connect();

                Log.e("","Connected");
            } catch (IOException connectException) {
                // Unable to connect; close the socket and get out
                Log.e("Error: ","Connection Error");
                connectException.printStackTrace();
                try {
                    mmSocket.close();
                } catch (IOException closeException) { }
                return;
            }

            // Do work to manage the connection (in a separate thread)
            manageConnectedSocket(mmSocket);
        }

        /** Will cancel an in-progress connection, and close the socket */
        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) { }
        }
    }

    private void manageConnectedSocket (BluetoothSocket mmSocket){
        Log.i(INFO_TAG,"Hablando desde el manager del socket");
        ConnectedThread coneccion = new ConnectedThread(mmSocket);

        coneccion.write("Hola Mundo!".getBytes());
        coneccion.run();
    }

    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            String address = null;
            switch (msg.what) {
                case MESSAGE_READ:
                    byte[] readBuf = (byte[]) msg.obj;
                    // construct a string from the valid bytes in the buffer
                    String readMessage = new String(readBuf, 0, msg.arg1);
                    Log.i("Recibí: ",readMessage);//aca recibo los datos del bluetooth como si fuese una interrupción
                    break;

            }
        }
    };
    private class ConnectedThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;

        public ConnectedThread(BluetoothSocket socket) {
            mmSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            // Get the input and output streams, using temp objects because
            // member streams are final
            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) { }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }

        public void run() {
            byte[] buffer = new byte[1024];  // buffer store for the stream
            int bytes; // bytes returned from read()

            // Keep listening to the InputStream until an exception occurs
            while (true) {
                try {
                    // Read from the InputStream
                    bytes = mmInStream.read(buffer);
                    // Send the obtained bytes to the UI activity
                    mHandler.obtainMessage(MESSAGE_READ, bytes, -1, buffer)
                           .sendToTarget();
                } catch (IOException e) {
                    break;
                }
            }
        }

        /* Call this from the main activity to send data to the remote device */
        public void write(byte[] bytes) {
            try {
                mmOutStream.write(bytes);
            } catch (IOException e) { }
        }

        /* Call this from the main activity to shutdown the connection */
        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) { }
        }
    }

}
