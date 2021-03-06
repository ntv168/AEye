package com.example.sam.aeye;

import android.Manifest;
import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
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
import com.example.sam.aeye.utils.ImageHelper;
import com.example.sam.aeye.utils.SelectImageActivity;
import com.example.sam.aeye.utils.VoiceUtils;
import com.example.sam.aeye.voice.ListeningActivity;
import com.example.sam.aeye.voice.VoiceRecognitionListener;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.drive.CreateFileActivityOptions;
import com.google.android.gms.drive.Drive;
import com.google.android.gms.drive.DriveClient;
import com.google.android.gms.drive.DriveResourceClient;

import java.util.ArrayList;
import java.util.List;

import clarifai2.api.ClarifaiResponse;
import clarifai2.dto.input.ClarifaiImage;
import clarifai2.dto.input.ClarifaiInput;
import clarifai2.dto.model.output.ClarifaiOutput;
import clarifai2.dto.prediction.Concept;

public class MainActivity extends ListeningActivity {

    private static final String TAG = "drive-quickstart";
    private static final int REQUEST_CODE_SIGN_IN = 3;


    private static final int REQUEST_SELECT_IMAGE = 0;
    private List<ClarifaiOutput<Concept>> predictionResults = new ArrayList<>();
    private ProgressDialog progressDialog;

    private void showReply(String sentenceReply) {
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
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode)
        {
            case REQUEST_SELECT_IMAGE:
                if (resultCode == RESULT_OK) {
                    final byte[] imageBytes = ImageHelper.retrieveSelectedImage(this,data);
                    onImagePicked(imageBytes);
                    progressDialog.show();
                }
                break;
            default:
                break;
        }
    }


    private void onImagePicked(final byte[] imageBytes) {

        new AsyncTask<Void, Void, ClarifaiResponse<List<ClarifaiOutput<Concept>>>>() {
            @Override protected ClarifaiResponse<List<ClarifaiOutput<Concept>>> doInBackground(Void... params) {
                // The default Clarifai model that identifies concepts in images
                // Use this model to predict, with the image that the user just selected as the input
                return App.getClarifaiClient().getModelByID("VND").executeSync().get().asConceptModel().predict()
                        .withInputs(ClarifaiInput.forImage(ClarifaiImage.of(imageBytes)))
                        .executeSync();

            }
            @Override protected void onPostExecute(ClarifaiResponse<List<ClarifaiOutput<Concept>>> response) {
                progressDialog.dismiss();
                if (!response.isSuccessful()) {
                    Toast.makeText(context, R.string.error_while_contacting_api, Toast.LENGTH_SHORT).show();
                    return;
                }
                final List<ClarifaiOutput<Concept>> predictions = response.get();
                if (predictions.isEmpty()) {
                    Toast.makeText(context, R.string.no_results_from_api, Toast.LENGTH_SHORT).show();
                    return;
                }

                String mess = "";

                for (int i = 0; i < predictions.get(0).data().size(); i++) {
                    mess += predictions.get(0).data().get(i).name() + ": "
                            + predictions.get(0).data().get(i).value() + " -- ";
                }
                Toast.makeText(context, mess, Toast.LENGTH_LONG).show();
                showReply(predictions.get(0).data().get(0).name());
            }
        }.execute();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Đang xử lí...");

    }


    public void uploadDrive(View view){
        startActivity(new Intent(this, com.example.sam.aeye.drive.TakePhotoActivity.class));
    }

    public void checkMoney(View view){

        Intent intent = new Intent(this, com.example.sam.aeye.moneydetect.FaceTrackerActivity.class);
        startActivity(intent);
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
            Intent intent = new Intent(this, com.example.sam.aeye.moneydetect.FaceTrackerActivity.class);
            startActivity(intent);
        } if (voiceCommands[0].contains("tải")) {
            Intent intent = new Intent(this, com.example.sam.aeye.drive.TakePhotoActivity.class);
            startActivity(intent);
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
