package com.example.sam.aeye;

import android.Manifest;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
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

import java.util.ArrayList;
import java.util.List;

import clarifai2.dto.input.ClarifaiInput;
import clarifai2.dto.model.output.ClarifaiOutput;
import clarifai2.dto.prediction.Concept;

public class MainActivity extends ListeningActivity {

    private static final float LOCATION_REFRESH_DISTANCE = 0;
    private static final long LOCATION_REFRESH_TIME = 0;
    private static Location mLocation;
    private List<ClarifaiOutput<Concept>> predictionResults = new ArrayList<>();

    private void showReply(String sentenceReply) {
        VoiceUtils.speak(sentenceReply);
    }

    private WebServer mWebServer;
    // Declaring a Location Manager
    protected LocationManager mLocationManager;

    @Override
    protected void onResume() {
        super.onResume();
        showReply("màng hình chính");
        // The following 3 lines are needed in every onCreate method of a ListeningActivity
        context = getApplicationContext(); // Needs to be set
        VoiceRecognitionListener.getInstance().setListener(this); // Here we set the current listener
        startListening(); // starts listening
    }


    private final LocationListener mLocationListener = new LocationListener() {
        @Override
        public void onLocationChanged(final Location location) {
            mLocation = location;
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {

        }

        @Override
        public void onProviderEnabled(String provider) {

        }

        @Override
        public void onProviderDisabled(String provider) {

        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mLocationManager = (LocationManager) getSystemService(LOCATION_SERVICE);

        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                predictionResults =
                        App.getClarifaiClient().getModelByID("VND").executeSync().get().asConceptModel().predict()
                                .withInputs(ClarifaiInput.forImage("http://hpsc.iwr.uni-heidelberg.de/HPSCHanoi2015/images/vnd100000.jpg"))
                                .executeSync().get();



            }
        };
        new Thread(runnable).start();

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                        != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, LOCATION_REFRESH_TIME,
                LOCATION_REFRESH_DISTANCE, mLocationListener);


    }


    public void startVoice(View view){
        startActivity(new Intent(this,TakePhotoActivity.class));
    }

    public void checkMoney(View view){
        //Start Server
//        final int port = 9090;
//        mWebServer = new WebServer(port,MainActivity.this);
//
//        startActivity(new Intent(this, com.example.sam.aeye.moneydetect.FaceTrackerActivity.class));
//
//        (new Thread(mWebServer)).start();
//        Toast.makeText(this, "Server Start", Toast.LENGTH_SHORT).show();

        AlertDialog.Builder builder = new AlertDialog.Builder(context);

        String messsss = "";

        for (int i = 0; i < predictionResults.get(0).data().size(); i++) {
            messsss = predictionResults.get(0).data().get(i).name() + "/ " + predictionResults.get(0).data().get(i).value();
            Log.e("-----------checkMoney" + predictionResults.size() , "checkMoney: " + messsss );
        }




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
