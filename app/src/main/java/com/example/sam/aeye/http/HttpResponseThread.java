package com.example.sam.aeye.http;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;


import com.example.sam.aeye.AppSingleton;
import com.example.sam.aeye.moneydetect.FaceTrackerActivity;
import com.example.sam.aeye.utils.MessageUtils;
import com.example.sam.aeye.utils.VoiceUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

import javax.xml.parsers.FactoryConfigurationError;


/**
 * Created by Thuans on 4/19/2017.
 */

public class HttpResponseThread extends Thread {
    private final String TAG = "HttpResponseThread";
    Socket socket;
    Context context;

    private  void showReply(String sentenceReply){
        VoiceUtils.speak(sentenceReply);
    }

    HttpResponseThread(Socket socket, Context context){
        this.socket = socket;
        this.context = context;
    }

    @Override
    public void run() {
        BufferedReader is;
        PrintWriter os;
        String request = "";


        try {
            is = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            request = is.readLine();
            Log.d(TAG,request+"");
            //kiem tra request url
            String response = "";
            os = new PrintWriter(socket.getOutputStream(), true);
            if (request != null ) {
                if (request.contains("/money/5k")) {
                    response = "5k";
                    showReply("5 ngàn đồng");
                    AppSingleton.getSingleton().setContent("năm ngàn đồng");
                    AppSingleton.getSingleton().setCheckToast(true);
                } else if (request.contains("/money/10k")) {
                    response = "10k";
                    showReply("10 ngàn đồng");
                    AppSingleton.getSingleton().setContent("mười ngàn đồng");
                    AppSingleton.getSingleton().setCheckToast(true);
                } else if (request.contains("/money/20k")) {
                    response = "20k";
                    showReply("20 ngàn đồng");
                    AppSingleton.getSingleton().setContent("hai mươi ngàn đồng");
                    AppSingleton.getSingleton().setCheckToast(true);
                } else if (request.contains("/money/50k")) {
                    response = "50k";
                    showReply("50 ngàn đồng");
                    AppSingleton.getSingleton().setContent("năm mươi ngàn đồng");
                    AppSingleton.getSingleton().setCheckToast(true);
                } else if (request.contains("/money/100k")) {
                    response = "100k";
                    showReply("100 ngàn đồng");
                    AppSingleton.getSingleton().setContent("một trăm ngàn đồng");
                    AppSingleton.getSingleton().setCheckToast(true);
                } else if (request.contains("/money/200k")) {
                    response = "200k";
                    showReply("200 ngàn đồng");
                    AppSingleton.getSingleton().setContent("hai trăm ngàn đồng");
                    AppSingleton.getSingleton().setCheckToast(true);
                }  else if (request.contains("/money/500k")) {
                    response = "500k";
                    showReply("500 ngàn đồng");
                    AppSingleton.getSingleton().setContent("năm trăm ngàn đồng");
                    AppSingleton.getSingleton().setCheckToast(true);
                }
            }
            os.print("HTTP/1.0 200" + "\r\n");
            os.print("Content type: text/html" + "\r\n");
            os.print("Content length: " + response.length() + "\r\n");
            os.print("\r\n");
            os.print(response + "\r\n");
            os.flush();
            socket.close();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return;
    }
}
