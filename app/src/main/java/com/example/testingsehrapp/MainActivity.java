package com.example.testingsehrapp;
import android.app.Activity;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import androidx.annotation.RequiresApi;
import eu.interopehrate.md2de.D2DBluetoothConnector;
import eu.interopehrate.md2de.api.MD2DConnectionListener;
import eu.interopehrate.md2de.api.MD2DListener;
import eu.interopehrate.md2de.api.MD2DSecureConnectionFactory;
import eu.interopehrate.md2de.api.MD2D;
import eu.interopehrate.md2ds.MD2DSecurityUtilities;

import org.hl7.fhir.r4.model.Patient;
import java.io.IOException;

public class MainActivity extends Activity{

    TextView out;
    MD2D md2d;
    D2DBluetoothConnector btc;
    Button startConnectionButton;

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        out = (TextView) findViewById(R.id.out);
        startConnectionButton = findViewById(R.id.StartConnection);
        startConnectionButton.setOnClickListener(v -> {
            try {
                startConnection(this);
            } catch (Exception e) {
                Log.e("MD2D", e.getClass().getName());
                Log.e("MD2D", e.getMessage(),e);
                e.printStackTrace();
            }
        });

        Log.d("MD2D", "App started");
    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    public void startConnection(Context v) throws Exception {

        // Bluetooth Adapter MAC address (needs update based on BT adapter's MAC) 74:70:FD:24:E5:13#
        btc = new D2DBluetoothConnector();
//        String scanned="00:1A:7D:DA:71:13#" +
//                "EnmWJhtvsN6g7kyb3ejGkxiUf6etb556CcpQk3tQ4qkipV+kAsgijpnpqNnqBF/90KRwbl8tPcECTlb7KNsqorOCVQpPKN+DnVOP/HjBlq9BpVQ/LYK3KhYwSwpNAJODIbuH0GAb6XFh5aKqyiTJHhT8dpejWEBNaWJApmUnT3Q=";
        //kostis laptop
        String scanned = "34:7D:F6:E2:0D:9B#" + "9B#UWhBLXGgO0vISmpXpSfRTFg9e7Nqu1zBlyO6Sy85Yi+EyyCeuhZhfW9tn7hL9Y0I8kH5GgEiIkIq3Ai1gBWZ7p0obGNYftSQZ/+RuCIk+A5KvXAaadqomrnGEZlI7tt4zr7aExi4kCE3HS3Crn3J10m3baJ5pvWcn5mYWhjwrgo=";
        //charis lab
        //String scanned="00:1A:7D:DA:71:13#jyAUgbsDtS50lZ94w6dDJcCOOtmI7u7M4Z1sVyvpvt/dorwBJSddp/q4H2sGe1+C9tJ3usaX6u7HsK7hlBFhi2b0Gybw3iYRlKIS39bAG80a8pLELa9lx00OyUCsowaKheCW+YD063XgSdhzKPHLrV7t0LPltRQimPZQIoUbx/k=";
        //sofianna
        //String scanned="B0:35:9F:04:E4:CF#" +"AWxG1e0PqGOjMXa0yQDB/41LCv1Ifmk9NRlzwiDxi9TKJBRIKOjc6oio26QuQPI2+rvdcFP5Xdd1SVZSYCwWjLCl9D0Lw31tJ5MF01Dfbq2ohde3gexKnkfYomRsxR1WtudcBMFXXhtzQxQb6VoPLUeB1iHHkO/cqol64xlqa/U=";


        String[] parts = scanned.split("#");
        String address = parts[0];
        String signature = parts[1];

        //DummyD2DHRExchangeListeners listeners = new DummyD2DHRExchangeListeners(this);
        MD2DListener listener = new DummyResourceServerListener();
        MD2DConnectionListener listenersConnection = new DummyD2DConnectionListeners();

        // Start new version
        Log.d("MD2D", "Storing scanned infos...");
        MD2DSecurityUtilities.storeScannedCertificate(this, signature, address);

        // Starts bluetooth connection
        Log.d("MD2D", "Executing Bluetooth connection...");
        MD2DSecureConnectionFactory secureConnector = btc.broadcastConnection(address,
                listener, listenersConnection, this);

        FHIRObjectsFactory fhir = new FHIRObjectsFactory();
        Patient patient = fhir.buildPatient();

        // executes secure connection protocol
        Log.d("MD2D", "Executing D2D secure connection protocol...");
        MD2D md2d = secureConnector.createSecureConnection(patient, this);
        Log.d("MD2D", "Ready to receive requests from HCP App...");

        // End new version
        /*
        // Executes bluetooth connection with HCP App
        md2d = btc.newBroadcastConnection(address, listener, listenersConnection, this);
        // Must be moved in security utilities to check if the certificate exists
        md2d.fetchCertificate();

        // Must be part of the security handshake, don't know why it is here...
        md2d.sendSEHRCertificate();

        // Store Signature scanned from QR
        // move to MD2DSecurityUtilities
        md2d.storeScanned(this, signature, address);

        TimeUnit.SECONDS.sleep(6);

        // Must be part of the security handshake, must be moved
        sendPersonalIdentity();
         */
    }

    public void closeConnection(View v) {
        try {
            btc.closeConnection();
        } catch (IOException e) {
            Log.d("MD2D", "IOEXCEPTION");
        }
    }

    //check Sofianna
//    public void sendConsentAnswer(View v) {
//        Log.d("MD2D", "sendConsentAnswer");
//        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
//        String encodedConsent = settings.getString("consent", "defaultValue");
//        String signedConsent = settings.getString("signedConsent", "defaultValue");
//
//        Log.d("MD2D", "consent#" + encodedConsent + "#" + signedConsent);
//        mConnectedThread.sendConsentAnswer("consent#" + encodedConsent + "#" + signedConsent);
//    }
}
