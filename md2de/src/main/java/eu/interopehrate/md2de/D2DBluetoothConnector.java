package eu.interopehrate.md2de;

import java.io.IOException;
import java.util.UUID;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.util.Log;

import eu.interopehrate.md2de.api.MD2DConnectionFactory;
import eu.interopehrate.md2de.api.MD2DConnectionListener;
import eu.interopehrate.md2de.api.MD2DListener;
import eu.interopehrate.md2de.api.MD2DSecureConnectionFactory;

public class D2DBluetoothConnector implements MD2DConnectionFactory {

    public BluetoothAdapter btAdapter = null;
    public BluetoothSocket btSocket = null;
    ConnectedThread mConnectedThread;

    // UUID service of bluetooth device
    public static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    // Bluetooth Adapter MAC address
    public static String address;
    public static String signature;
    public static Context context;

    /*
    public ConnectedThread broadcastConnection(String address, D2DHRExchangeListeners listeners, D2DConnectionListener listenersConnection, Context context){

        this.context = context;
        if (btSocket != null) {
            try {btSocket.close();
            } catch (Exception e) {

            }
            Log.d("MD2D","Closed the old connection");
            btSocket = null;
//            Log.d("MD2D","...socket"+btSocket+"streams"+mConnectedThread.mmInStream+mConnectedThread.mmOutStream);
        }

        btAdapter = BluetoothAdapter.getDefaultAdapter();
        CheckBTState();

        // Set up a pointer to the remote node using its address.
        this.address=address;

        this.signature = signature;
        BluetoothDevice device = btAdapter.getRemoteDevice(address);

        try {
            btSocket = device.createRfcommSocketToServiceRecord(MY_UUID);
        } catch (IOException e) {
            Log.d("MD2D","In onResume() and socket create failed: " + e.getMessage() + ".");
        }

        // Discovery is resource intensive.  Make sure it isn't going on when you attempt to connect and pass your message.
        btAdapter.cancelDiscovery();

        // Establish the connection.  This will block until it connects.
        try {
            btSocket.connect();
            Log.d("MD2D","\n...Connection established and data link opened...");
        } catch (IOException e) {
            try {
                btSocket.close();
            } catch (IOException e2) {
                Log.d("MD2D","Unable to close socket during connection failure" + e2.getMessage() + ".");
            }
        }

        mConnectedThread = new ConnectedThread(btSocket,listeners,listenersConnection, context);
        mConnectedThread.start();

        return mConnectedThread;
    }

    public MD2D newBroadcastConnection(String address,
                                       D2DListener listener,
                                       D2DConnectionListener listenersConnection,
                                       Context context){
        this.context = context;
        if (btSocket != null) {
            try {btSocket.close();
            } catch (Exception e) {

            }
            Log.d("MD2D","Closed the old connection");
            btSocket = null;
//            Log.d("MD2D","...socket"+btSocket+"streams"+mConnectedThread.mmInStream+mConnectedThread.mmOutStream);
        }

        btAdapter = BluetoothAdapter.getDefaultAdapter();
        CheckBTState();

        // Set up a pointer to the remote node using its address.
        this.address=address;

        this.signature = signature;
        BluetoothDevice device = btAdapter.getRemoteDevice(address);

        try {
            btSocket = device.createRfcommSocketToServiceRecord(MY_UUID);
        } catch (IOException e) {
            Log.d("MD2D","In onResume() and socket create failed: " + e.getMessage() + ".");
        }

        // Discovery is resource intensive.  Make sure it isn't going on when you attempt to connect and pass your message.
        btAdapter.cancelDiscovery();

        // Establish the connection.  This will block until it connects.
        try {
            btSocket.connect();
            Log.d("MD2D","\n...Connection established and data link opened...");
        } catch (IOException e) {
            try {
                btSocket.close();
            } catch (IOException e2) {
                Log.d("MD2D","Unable to close socket during connection failure" + e2.getMessage() + ".");
            }
        }

        return new MD2DImplFirstVersion(btSocket,listener,listenersConnection, context);
    }
    */

    public MD2DSecureConnectionFactory broadcastConnection(String address,
                                                           MD2DListener listener,
                                                           MD2DConnectionListener listenersConnection,
                                                           Context context) throws Exception {
        this.context = context;
        if (btSocket != null) {
            try {btSocket.close();
            } catch (Exception e) { }
            Log.d("MD2D","Closed the old connection");
            btSocket = null;
//            Log.d("MD2D","...socket"+btSocket+"streams"+mConnectedThread.mmInStream+mConnectedThread.mmOutStream);
        }

        btAdapter = BluetoothAdapter.getDefaultAdapter();
        CheckBTState();

        // Set up a pointer to the remote node using its address.
        this.address = address;
        this.signature = signature;
        BluetoothDevice device = btAdapter.getRemoteDevice(address);

        try {
            btSocket = device.createRfcommSocketToServiceRecord(MY_UUID);
        } catch (IOException e) {
            Log.d("MD2D","In onResume() and socket create failed: " + e.getMessage() + ".");
        }

        // Discovery is resource intensive.  Make sure it isn't going on when you attempt to connect and pass your message.
        btAdapter.cancelDiscovery();

        // Establish the connection.  This will block until it connects.
        btSocket.connect();
        Log.d("MD2D","...Connection established, data link opened, starting secure connection protocol...");

        return new MD2DSecureConnectionFactoryImpl(listener,
                btSocket.getInputStream(),
                btSocket.getOutputStream());

    }

    public void CheckBTState() {
        if(btAdapter == null) {
            Log.d("MD2D","...Bluetooth Not supported. Aborting....");
        } else {
            if (btAdapter.isEnabled()) {
                Log.d("MD2D","...Bluetooth is enabled...");
            }
        }
    }

    public void closeConnection() throws IOException {
        Log.d("MD2D","Closing connection with HCP App...");
        btSocket.close();
        btSocket=null;
    }

    public boolean checkProvenance(String data) {
        if (data.contains("signature") && data.contains("author"))
        {
            return true;
        }
        else
        {
            return false;
        }
    }

}
