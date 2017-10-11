package com.example.sam.aeye;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import com.example.sam.aeye.facetracker.FaceTrackerActivity;
import com.example.sam.aeye.persongroupmanagement.PersonGroupActivity;
import com.example.sam.aeye.utils.VoiceUtils;
import com.example.sam.aeye.voice.ListeningActivity;
import com.example.sam.aeye.voice.VoiceRecognitionListener;

public class MainActivity extends ListeningActivity{

    private  void showReply(String sentenceReply){
        VoiceUtils.speak(sentenceReply);
    }

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
        context = getApplicationContext(); // Needs to be set
        VoiceRecognitionListener.getInstance().setListener(this); // Here we set the current listener
        startListening(); // starts listening
    }



    public void stopVoice(View view){
        stopListening(); // starts listening
    }

    @Override
    public void processVoiceCommands(String... voiceCommands) {
        Toast.makeText(context, voiceCommands[0], Toast.LENGTH_SHORT).show();
        if (voiceCommands[0].contains("người")) {
            startActivity(new Intent(this, PersonGroupActivity.class));
        }
        if (voiceCommands[0].contains("nhận ")) {
            startActivity(new Intent(this, FaceTrackerActivity.class));
        }
        restartListeningService();
    }

    public void start(View view){
        startActivity(new Intent(this, PersonGroupActivity.class));
    }

    public void tracker(View view){
        startActivity(new Intent(this, FaceTrackerActivity.class));
    }

}
