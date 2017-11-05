package com.example.sam.aeye;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import com.example.sam.aeye.facetracker.FaceTrackerActivity;
import com.example.sam.aeye.http.WebServer;
import com.example.sam.aeye.persongroupmanagement.PersonGroupActivity;
import com.example.sam.aeye.photo.TakePhotoActivity;
import com.example.sam.aeye.streetmode.FaceTrackerStreetModeActivity;
import com.example.sam.aeye.utils.VoiceUtils;
import com.example.sam.aeye.voice.ListeningActivity;
import com.example.sam.aeye.voice.VoiceRecognitionListener;

public class MainActivity extends ListeningActivity{

    private  void showReply(String sentenceReply){
        VoiceUtils.speak(sentenceReply);
    }
    private WebServer mWebServer;

    @Override
    protected void onResume() {
        super.onResume();
        showReply("màng hình chính");
        // The following 3 lines are needed in every onCreate method of a ListeningActivity
        context = getApplicationContext(); // Needs to be set
        VoiceRecognitionListener.getInstance().setListener(this); // Here we set the current listener
        startListening(); // starts listening
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


    }

    public void startVoice(View view){
        startActivity(new Intent(this,TakePhotoActivity.class));
    }

    public void stopVoice(View view){
        //Start Server
        final int port = 9090;
        mWebServer = new WebServer(port,MainActivity.this);

        startActivity(new Intent(this, com.example.sam.aeye.moneydetect.FaceTrackerActivity.class));

        (new Thread(mWebServer)).start();
        Toast.makeText(this, "Server Start", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void processVoiceCommands(String... voiceCommands) {
        Toast.makeText(context, voiceCommands[0], Toast.LENGTH_SHORT).show();
        if (voiceCommands[0].contains("người")) {
            startActivity(new Intent(this, PersonGroupActivity.class));
        } if (voiceCommands[0].contains("nhận ")) {
            startActivity(new Intent(this, FaceTrackerActivity.class));
        } if (voiceCommands[0].contains("lưu lại")) {
            startActivity(new Intent(this, TakePhotoActivity.class));
        } if (voiceCommands[0].contains("đường") && voiceCommands[0].contains("đi")) {
            startActivity(new Intent(this, FaceTrackerStreetModeActivity.class));
        } if (voiceCommands[0].contains("kiểm tra")) {
            final int port = 9090;
            mWebServer = new WebServer(port,MainActivity.this);

            startActivity(new Intent(this, com.example.sam.aeye.moneydetect.FaceTrackerActivity.class));

            (new Thread(mWebServer)).start();
            Toast.makeText(this, "Server Start", Toast.LENGTH_SHORT).show();
        }

        restartListeningService();
    }

    public void start(View view){
        startActivity(new Intent(this, PersonGroupActivity.class));
    }

    public void tracker(View view){
        startActivity(new Intent(this, FaceTrackerActivity.class));
    }

    public void streetmode(View view) {
        startActivity(new Intent(this, FaceTrackerStreetModeActivity.class));
    }

    public void save(View view) {
        startActivity(new Intent(this, TakePhotoActivity.class));
    }

}
