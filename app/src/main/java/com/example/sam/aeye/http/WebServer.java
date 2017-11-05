package com.example.sam.aeye.http;

import android.content.Context;
import android.util.Log;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * Created by Thuans on 4/19/2017.
 */

public class WebServer implements Runnable {
    private static final String TAG = "SimpleWebServer";

    private final int mPort;
    private Context mContext;

    private boolean mIsRunning;

    private ServerSocket mServerSocket;
    private String message;

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public WebServer(int port, Context context) {
        mPort = port;
        mContext = context;
    }

    public void stop() {
        try {
            mIsRunning = false;
            if (null != mServerSocket) {
                mServerSocket.close();
                mServerSocket = null;
            }
        } catch (IOException e) {
            Log.e(TAG, "Error closing the server socket.", e);
        }
    }

    public int getPort() {
        return mPort;
    }

    @Override
    public void run() {
        Log.d(TAG,"Start thread");
        mIsRunning = true;
        Socket socket = null;
        try {
            mServerSocket = new ServerSocket(getPort());

            while(true){
                socket = mServerSocket.accept();

                HttpResponseThread httpResponseThread =
                        new HttpResponseThread(
                                socket,mContext);
                httpResponseThread.start();
            }
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

}
