/*
 * Copyright (C) The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.example.sam.aeye.moneydetect;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.example.sam.aeye.App;
import com.example.sam.aeye.AppSingleton;
import com.example.sam.aeye.MainActivity;
import com.example.sam.aeye.R;
import com.example.sam.aeye.camera.CameraConfig;
import com.example.sam.aeye.camera.CameraSourcePreview;
import com.example.sam.aeye.camera.GraphicOverlay;
import com.example.sam.aeye.utils.ImageHelper;
import com.example.sam.aeye.utils.StorageHelper;
import com.example.sam.aeye.utils.VoiceUtils;
import com.example.sam.aeye.voice.ListeningActivity;
import com.example.sam.aeye.voice.VoiceRecognitionListener;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.vision.CameraSource;
import com.google.android.gms.vision.MultiProcessor;
import com.google.android.gms.vision.Tracker;
import com.google.android.gms.vision.face.Face;
import com.google.android.gms.vision.face.FaceDetector;
import com.microsoft.projectoxford.face.FaceServiceClient;
import com.microsoft.projectoxford.face.contract.IdentifyResult;
import com.microsoft.projectoxford.face.contract.TrainingStatus;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * Activity for the face tracker app.  This app detects faces with the rear facing camera, and draws
 * overlay graphics to indicate the position, size, and ID of each face.
 */
public final class FaceTrackerActivity extends ListeningActivity {
    private static final String TAG = "FaceTracker";

    private CameraSource mCameraSource = null;

    private CameraSourcePreview mPreview;
    private GraphicOverlay mGraphicOverlay;
    private boolean DETECT_RUNNING = false;
    private int CURRENT_FACE_ID = 0;

    private static final int RC_HANDLE_GMS = 9001;
    // permission request codes need to be < 256
    private static final int RC_HANDLE_CAMERA_PERM = 2;
    private static float happy = 0.2f;

    //==============================================================================================
    // Activity Methods
    //==============================================================================================

    /**
     * Initializes the UI and initiates the creation of a face detector.
     */

    private static CameraSource.PictureCallback mCallBack;
    boolean detecting;
    private Bitmap mBitmap;
    String mPersonGroupId;
    ProgressDialog progressDialog;

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        setContentView(R.layout.activity_face_tracker);

        showReply("Sẵn sàng");

        if (read) {
            context = getApplicationContext(); // Needs to be set
            VoiceRecognitionListener.getInstance().setListener(this); // Here we set the current listener
            startListening(); // starts listening
        }


        mPersonGroupId = StorageHelper.getPersonGroupId("nguoinha", FaceTrackerActivity.this);
        detecting= false;

        progressDialog = new ProgressDialog(this);
        progressDialog.setTitle(getString(R.string.progress_dialog_title));

        mPreview = (CameraSourcePreview) findViewById(R.id.preview);
        mGraphicOverlay = (GraphicOverlay) findViewById(R.id.faceOverlay);

        // Check for the camera permission before accessing the camera.  If the
        // permission is not granted yet, request permission.
        int rc = ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA);
        if (rc == PackageManager.PERMISSION_GRANTED) {
            createCameraSource();
        } else {
            requestCameraPermission();
        }


        mCallBack = new CameraSource.PictureCallback() {
            @Override
            public void onPictureTaken(byte[] data) {
//                Toast.makeText(FaceTrackerStreetModeActivity.this, "take a picture", Toast.LENGTH_SHORT).show();

                FaceTrackerActivity.this.DETECT_RUNNING = true;
                showMessage("camera data length: "+ data.length);

                //Rezise bitmap
                BitmapFactory.Options options = new BitmapFactory.Options();
                options.inPurgeable = true;

                Bitmap tmp = BitmapFactory.decodeByteArray(data, 0, data.length, options);

                if(tmp.getHeight() > 300 && tmp.getWidth() > 300){
                    tmp = Bitmap.createScaledBitmap(tmp, 300, 300, false);

                }


                //respone image base64
                CameraConfig camera = CameraConfig.getInstance();
                ByteArrayOutputStream os = new ByteArrayOutputStream();
                tmp.compress(Bitmap.CompressFormat.JPEG, 100, os);
                camera.setFaceDetected(1);
                camera.setFaceImage(os.toByteArray());

                String root = Environment.getExternalStorageDirectory().toString();
                File myDir = new File(root + "/saved_images");
                myDir.mkdirs();
                String nameFile = "testSelf.jpg";
                File file = new File(myDir, nameFile);
                if (file.exists ()) file.delete();
                try {
                    FileOutputStream out = new FileOutputStream(file);
                    tmp.compress(Bitmap.CompressFormat.JPEG, 100, out);
//
                    out.close();
                    showMessage("Saved picture......."+file.getCanonicalPath());
                } catch (IOException e){
                    Log.d("CAMERA file", e.getMessage());
                }



//                detecting = false;
                // If image is selected successfully, set the image URI and bitmap.
                Uri uri = Uri.fromFile(file);

                mBitmap = ImageHelper.loadSizeLimitedBitmapFromUri(
                        uri, getContentResolver());

                detect(mBitmap);


            }
        };

    }



    private void detect(Bitmap bitmap) {
        // Put the image into an input stream for detection.
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, output);
        ByteArrayInputStream inputStream = new ByteArrayInputStream(output.toByteArray());


        // Start a background task to detect faces in the image.
        new DetectionTask().execute(inputStream);
    }

    com.microsoft.projectoxford.face.contract.Face emotionFace = null;

    private class DetectionTask extends AsyncTask<InputStream, String, com.microsoft.projectoxford.face.contract.Face[]> {
        long startdetect = System.currentTimeMillis();
        @Override
        protected com.microsoft.projectoxford.face.contract.Face[] doInBackground(InputStream... params) {
            // Get an instance of face service client to detect faces in image.
            FaceServiceClient faceServiceClient = App.getFaceServiceClient();

            try{
                publishProgress("Detecting...");

                // Start detection.
                return faceServiceClient.detect(
                        params[0],  /* Input stream of image to detect */
                        true,       /* Whether to return face ID */
                        false,       /* Whether to return face landmarks */
                        /* Which face attributes to analyze, currently we support:
                           age,gender,headPose,smile,facialHair */
                        new FaceServiceClient.FaceAttributeType[] {
                                FaceServiceClient.FaceAttributeType.Age,
                                FaceServiceClient.FaceAttributeType.Gender,
                                FaceServiceClient.FaceAttributeType.Smile,
                                FaceServiceClient.FaceAttributeType.Glasses,
                                FaceServiceClient.FaceAttributeType.FacialHair,
                                FaceServiceClient.FaceAttributeType.Emotion,
                                FaceServiceClient.FaceAttributeType.HeadPose,
                                FaceServiceClient.FaceAttributeType.Accessories,
                                FaceServiceClient.FaceAttributeType.Blur,
                                FaceServiceClient.FaceAttributeType.Exposure,
                                FaceServiceClient.FaceAttributeType.Hair,
                                FaceServiceClient.FaceAttributeType.Makeup,
                                FaceServiceClient.FaceAttributeType.Noise,
                                FaceServiceClient.FaceAttributeType.Occlusion});
            }  catch (Exception e) {
                publishProgress(e.getMessage());
                return null;
            }
        }

        @Override
        protected void onPreExecute() {

        }

        @Override
        protected void onProgressUpdate(String... values) {
            // Show the status of background detection task on screen.

        }

        @Override
        protected void onPostExecute(com.microsoft.projectoxford.face.contract.Face[] result) {
            progressDialog.dismiss();
            long enddetect= System.currentTimeMillis();
            Log.d("--------------", "time detect --------------- " + (enddetect - startdetect));

            if (result != null) {
                if (  result.length == 1) {
                    emotionFace = result[0];
                } else {
                    emotionFace = null;
                }
            }


            if (result != null) {
                // Set the adapter of the ListView which contains the details of detectingfaces.
                List<com.microsoft.projectoxford.face.contract.Face> faces = Arrays.asList(result);

                if (result.length == 0) {
                    detecting= false;
                    setInfo("No faces detected!");
                    FaceTrackerActivity.this.DETECT_RUNNING = false;
                } else {
                    detecting= true;


                    // Called identify after detection.
                    if (detecting&& mPersonGroupId != null) {
                        // Start a background task to identify faces in the image.
                        List<UUID> faceIds = new ArrayList<>();
                        for (com.microsoft.projectoxford.face.contract.Face face:  faces) {
                            faceIds.add(face.faceId);

                            Log.d(TAG, "------------------------: " + face.faceId.toString());
                        }
                        new IdentificationTask(mPersonGroupId).execute(
                                faceIds.toArray(new UUID[faceIds.size()]));
                        Log.d("-------", "identify: facezise" + faceIds.size());
                        FaceTrackerActivity.this.DETECT_RUNNING = false;
                    } else {
                        // Not detectingor person group exists.
                        FaceTrackerActivity.this.DETECT_RUNNING = false;
                        setInfo("Please select an image and create a person group first.");
                    }
                }
            } else {
                detecting= false;
                ;
            }

        }

    }

    private class IdentificationTask extends AsyncTask<UUID, String, IdentifyResult[]> {
        String mPersonGroupId;
        long startidentify = System.currentTimeMillis();
        IdentificationTask(String personGroupId) {
            this.mPersonGroupId = personGroupId;
            Log.d("--------", "IdentificationTask: " + personGroupId);
        }

        @Override
        protected IdentifyResult[] doInBackground(UUID... params) {
            String logString = "Request: Identifying faces ";
            for (UUID faceId: params) {
                logString += faceId.toString() + ", ";
            }
            logString += " in group " + mPersonGroupId;
            Log.d("--------", "IdentificationTask: " + mPersonGroupId);
            // Get an instance of face service client to detect faces in image.
            FaceServiceClient faceServiceClient = App.getFaceServiceClient();
            try{
                publishProgress("Getting person group status...");

                TrainingStatus trainingStatus = faceServiceClient.getPersonGroupTrainingStatus(
                        this.mPersonGroupId);     /* personGroupId */

                Log.d("--------", "trainingStatus: " + trainingStatus);

                if (trainingStatus.status != TrainingStatus.Status.Succeeded) {
                    publishProgress("Person group training status is " + trainingStatus.status);
                    return null;
                }

                publishProgress("Identifying...");

                // Start identification.
                return faceServiceClient.identity(
                        this.mPersonGroupId,   /* personGroupId */
                        params,                  /* faceIds */
                        1);  /* maxNumOfCandidatesReturned */
            }  catch (Exception e) {

                publishProgress(e.getMessage());
                return null;
            }
        }

        @Override
        protected void onPreExecute() {
//
        }

        @Override
        protected void onProgressUpdate(String... values) {
            // Show the status of background detection task on screen.a

        }

        @Override
        protected void onPostExecute(IdentifyResult[] result) {
            long endidentify= System.currentTimeMillis();
            Log.d("----------------", "time identity: ------------- " + (endidentify - startidentify));
            String message = "";
            // Show the result on screen when detection is done.
            // Set the information about the detection result.
            if (result != null) {
                Toast.makeText(FaceTrackerActivity.this, ""+ (endidentify - startidentify), Toast.LENGTH_LONG).show();

                Boolean hasAqua = false;
                int stranger = 0;

                for (IdentifyResult identifyResult: result) {
                    if (identifyResult.candidates.size() > 0) {
                        if (identifyResult.candidates.get(0).confidence > 0.65) {
                            String personId = identifyResult.candidates.get(0).personId.toString();
                            String personName = StorageHelper.getPersonName(
                                    personId, mPersonGroupId, FaceTrackerActivity.this);
                            try {
                                personName = URLDecoder.decode(personName,"UTF-8");
                            } catch (UnsupportedEncodingException e) {
                                e.printStackTrace();
                            }
                            message  += "Xin chào,"   + personName;
                            hasAqua = true;
                        } else {
                            stranger++;
                        }
                    } else {
                        stranger++;
                    }
                }
//                if (stranger > 0 && hasAqua) {
//                    message += " và " + stranger + "người lạ";}
                 if (stranger > 0 && !hasAqua) {
                    message = "nhận diện hoàn tất";
                }

            }

            showReply(message);
            Toast.makeText(FaceTrackerActivity.this, message, Toast.LENGTH_SHORT).show();
        }

    }


    private void setInfo(String info) {
        TextView textView = (TextView) findViewById(R.id.info);
        textView.setText(info);
    }

    boolean read = false;
    private  void showReply(String sentenceReply){
        VoiceUtils.speak(sentenceReply);
        read = true;
    }

    /**
     * Handles the requesting of the camera permission.  This includes
     * showing a "Snackbar" message of why the permission is needed then
     * sending the request.
     */
    private void requestCameraPermission() {
        Log.w(TAG, "Camera permission is not granted. Requesting permission");

        final String[] permissions = new String[]{Manifest.permission.CAMERA};

        if (!ActivityCompat.shouldShowRequestPermissionRationale(this,
                Manifest.permission.CAMERA)) {
            ActivityCompat.requestPermissions(this, permissions, RC_HANDLE_CAMERA_PERM);
            return;
        }

        final Activity thisActivity = this;

        View.OnClickListener listener = new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ActivityCompat.requestPermissions(thisActivity, permissions,
                        RC_HANDLE_CAMERA_PERM);
            }
        };

        Snackbar.make(mGraphicOverlay, R.string.permission_camera_rationale,
                Snackbar.LENGTH_INDEFINITE)
                .setAction(R.string.ok, listener)
                .show();
    }

    /**
     * Creates and starts the camera.  Note that this uses a higher resolution in comparison
     * to other detection examples to enable the barcode detector to detect small barcodes
     * at long distances.
     */
    private void createCameraSource() {

        Context context = getApplicationContext();
        FaceDetector detector = new FaceDetector.Builder(context)
                .setClassificationType(FaceDetector.ALL_CLASSIFICATIONS)
                .build();

        detector.setProcessor(
                new MultiProcessor.Builder<>(new GraphicFaceTrackerFactory())
                        .build());

        if (!detector.isOperational()) {
            // Note: The first time that an app using face API is installed on a device, GMS will
            // download a native library to the device in order to do detection.  Usually this
            // completes before the app is run for the first time.  But if that download has not yet
            // completed, then the above call will not detect any faces.
            //
            // isOperational() can be used to check if the required native library is currently
            // available.  The detector will automatically become operational once the library
            // download completes on device.
            Log.w(TAG, "Face detector dependencies are not yet available.");
        }

        mCameraSource = new CameraSource.Builder(context, detector)
                .setRequestedPreviewSize(640, 480)
                .setFacing(CameraSource.CAMERA_FACING_BACK)
                .setRequestedFps(30.0f)
                .build();
    }

    /**
     * Restarts the camera.
     */
    @Override
    protected void onResume() {
        super.onResume();
        startCameraSource();
    }

    /**
     * Stops the camera.
     */
    @Override
    protected void onPause() {
        super.onPause();
        mPreview.stop();
    }

    @Override
    public void processVoiceCommands(String... voiceCommands) {
        if (AppSingleton.getSingleton().getCheckToast()) {
            Toast.makeText(context, AppSingleton.getSingleton().getContent(), Toast.LENGTH_SHORT).show();
            AppSingleton.getSingleton().setCheckToast(false);
        }
        if (voiceCommands[0].toLowerCase().contains("về")) {
            finish();
        }
        if (voiceCommands[0].toLowerCase().contains("cảm")) {
            if (emotionFace == null) {
                showReply("có quá nhiều khuôn mặt để nhận diện, xin vui lòng thử lại");
            } else {
                if (emotionFace.faceAttributes.smile > 0.55) {
                    showReply("người này có vẻ đang vui");
                } else if (emotionFace.faceAttributes.smile < 0.35) {
                    showReply("người này có vẻ buồn");
                } else {
                    showReply("người này có vẻ mặt bình thường");
                }
            }

        }
        if (voiceCommands[0].toLowerCase().contains("bao nhiêu")) {
            if (emotionFace == null) {
                showReply("không xác định được, xin vui lòng thử lại");
            } else {
                showReply("người này khoảng " + emotionFace.faceAttributes.age + " tuổi");
            }

        }
        restartListeningService();
    }

    /**
     * Releases the resources associated with the camera source, the associated detector, and the
     * rest of the processing pipeline.
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mCameraSource != null) {
            mCameraSource.release();
        }
    }

    /**
     * Callback for the result from requesting permissions. This method
     * is invoked for every call on {@link #requestPermissions(String[], int)}.
     * <p>
     * <strong>Note:</strong> It is possible that the permissions request interaction
     * with the user is interrupted. In this case you will receive empty permissions
     * and results arrays which should be treated as a cancellation.
     * </p>
     *
     * @param requestCode  The request code passed in {@link #requestPermissions(String[], int)}.
     * @param permissions  The requested permissions. Never null.
     * @param grantResults The grant results for the corresponding permissions
     *                     which is either {@link PackageManager#PERMISSION_GRANTED}
     *                     or {@link PackageManager#PERMISSION_DENIED}. Never null.
     * @see #requestPermissions(String[], int)
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode != RC_HANDLE_CAMERA_PERM) {
            Log.d(TAG, "Got unexpected permission result: " + requestCode);
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
            return;
        }

        if (grantResults.length != 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            Log.d(TAG, "Camera permission granted - initialize the camera source");
            // we have permission, so create the camerasource
            createCameraSource();
            return;
        }

        Log.e(TAG, "Permission not granted: results len = " + grantResults.length +
                " Result code = " + (grantResults.length > 0 ? grantResults[0] : "(empty)"));

        DialogInterface.OnClickListener listener = new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                finish();
            }
        };

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Face Tracker sample")
                .setMessage(R.string.no_camera_permission)
                .setPositiveButton(R.string.ok, listener)
                .show();
    }

    //==============================================================================================
    // Camera Source Preview
    //==============================================================================================

    /**
     * Starts or restarts the camera source, if it exists.  If the camera source doesn't exist yet
     * (e.g., because onResume was called before the camera source was created), this will be called
     * again when the camera source is created.
     */
    private void startCameraSource() {

        // check that the device has play services available.
        int code = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(
                getApplicationContext());
        if (code != ConnectionResult.SUCCESS) {
            Dialog dlg =
                    GoogleApiAvailability.getInstance().getErrorDialog(this, code, RC_HANDLE_GMS);
            dlg.show();
        }

        if (mCameraSource != null) {
            try {
                mPreview.start(mCameraSource, mGraphicOverlay);
            } catch (IOException e) {
                Log.e(TAG, "Unable to start camera source.", e);
                mCameraSource.release();
                mCameraSource = null;
            }
        }
    }

    //==============================================================================================
    // Graphic Face Tracker
    //==============================================================================================

    /**
     * Factory for creating a face tracker to be associated with a new face.  The multiprocessor
     * uses this factory to create face trackers as needed -- one for each individual.
     */
    private class GraphicFaceTrackerFactory implements MultiProcessor.Factory<Face> {
        @Override
        public Tracker<Face> create(Face face) {
            return new GraphicFaceTracker(mGraphicOverlay);
        }
    }

    /**
     * Face tracker for each detectingindividual. This maintains a face graphic within the app's
     * associated face overlay.
     */
    private class GraphicFaceTracker extends Tracker<Face> {
        private GraphicOverlay mOverlay;
        private FaceGraphic mFaceGraphic;

        GraphicFaceTracker(GraphicOverlay overlay) {
            mOverlay = overlay;
            mFaceGraphic = new FaceGraphic(overlay);
        }

        /**
         * Start tracking the detectingface instance within the face overlay.
         */
        @Override
        public void onNewItem(int faceId, Face item) {

        }

        /**
         * Update the position/characteristics of the face within the overlay.
         */
        @Override
        public void onUpdate(FaceDetector.Detections<Face> detectionResults, Face face) {
            mOverlay.add(mFaceGraphic);
            mFaceGraphic.updateFace(face);
        }

        /**
         * Hide the graphic when the corresponding face was not detected.  This can happen for
         * intermediate frames temporarily (e.g., if the face was momentarily blocked from
         * view).
         */
        @Override
        public void onMissing(FaceDetector.Detections<Face> detectionResults) {
            mOverlay.remove(mFaceGraphic);
        }

        /**
         * Called when the face is assumed to be gone for good. Remove the graphic annotation from
         * the overlay.
         */
        @Override
        public void onDone() {
            mOverlay.remove(mFaceGraphic);
        }
    }

    public void showMessage(String message) {
        Toast.makeText(getApplicationContext(),message,Toast.LENGTH_SHORT).show();
    }


}
