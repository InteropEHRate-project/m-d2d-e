package com.example.testingsehrapp;

import android.util.Log;

import eu.interopehrate.md2de.api.MD2DConnectionListener;

public class DummyD2DConnectionListeners implements MD2DConnectionListener {

    public void onConnectionClosure(){
        Log.d("MD2D","onConnectionClosure()");
    }
}
