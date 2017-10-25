package com.example.sam.aeye;

import android.app.Application;
import android.content.Context;
import android.util.Log;

import com.example.sam.aeye.utils.VoiceUtils;
import com.microsoft.projectoxford.emotion.EmotionServiceClient;
import com.microsoft.projectoxford.emotion.EmotionServiceRestClient;
import com.microsoft.projectoxford.face.FaceServiceClient;
import com.microsoft.projectoxford.face.FaceServiceRestClient;


/**
 * Created by Sam on 10/10/2017.
 */

public class App extends Application {
    private static Context context;

    public static FaceServiceClient getFaceServiceClient() {
        return sFaceServiceClient;
    }

    private static FaceServiceClient sFaceServiceClient;

    private static EmotionServiceClient client;

    public static EmotionServiceClient getClient() {
        return client;
    }

    public void setClient(EmotionServiceClient client) {
        this.client = client;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        context = getApplicationContext();
        sFaceServiceClient = new FaceServiceRestClient(getString(R.string.endpoint), getString(R.string.subscription_key));
        client = new EmotionServiceRestClient(getString(R.string.subscription_key_emotion));

        context = this.getApplicationContext();
        VoiceUtils.initializeInstance(context);

        Log.d("App context","initiating .....");
    }

    public static Context getContext() {
        return context;
    }
}
