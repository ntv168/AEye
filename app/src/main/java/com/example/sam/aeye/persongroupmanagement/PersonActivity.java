//
// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license.
//
// Microsoft Cognitive Services (formerly Project Oxford): https://www.microsoft.com/cognitive-services
//
// Microsoft Cognitive Services (formerly Project Oxford) GitHub:
// https://github.com/Microsoft/Cognitive-Face-Android
//
// Copyright (c) Microsoft Corporation
// All rights reserved.
//
// MIT License:
// Permission is hereby granted, free of charge, to any person obtaining
// a copy of this software and associated documentation files (the
// "Software"), to deal in the Software without restriction, including
// without limitation the rights to use, copy, modify, merge, publish,
// distribute, sublicense, and/or sell copies of the Software, and to
// permit persons to whom the Software is furnished to do so, subject to
// the following conditions:
//
// The above copyright notice and this permission notice shall be
// included in all copies or substantial portions of the Software.
//
// THE SOFTWARE IS PROVIDED ""AS IS"", WITHOUT WARRANTY OF ANY KIND,
// EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
// MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
// NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE
// LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
// OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
// WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
//
package com.example.sam.aeye.persongroupmanagement;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.ActionMode;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.sam.aeye.App;
import com.example.sam.aeye.R;
import com.example.sam.aeye.utils.SelectImageActivity;
import com.example.sam.aeye.utils.StorageHelper;
import com.example.sam.aeye.utils.VoiceUtils;
import com.example.sam.aeye.voice.ListeningActivity;
import com.example.sam.aeye.voice.VoiceRecognitionListener;
import com.microsoft.projectoxford.face.FaceServiceClient;
import com.microsoft.projectoxford.face.contract.CreatePersonResult;


import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;


public class PersonActivity extends ListeningActivity {
    // Background task of adding a person to person group.
    class AddPersonTask extends AsyncTask<String, String, String> {
        // Indicate the next step is to add face in this person, or finish editing this person.
        boolean mAddFace;
        String personName;
        AddPersonTask (boolean addFace, String personName) {
            mAddFace = addFace;
            try {
                this.personName = URLEncoder.encode(personName,"UTF-8");
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
        }

        @Override
        protected String doInBackground(String... params) {
            // Get an instance of face service client.
            FaceServiceClient faceServiceClient = App.getFaceServiceClient();
            try{
                publishProgress("Syncing with server to add person...");
                Log.d("-----------------", "doInBackground: " + params[0] + personName);

                // Start the request to creating person.
                CreatePersonResult createPersonResult = faceServiceClient.createPerson(
                        params[0],
                        personName,
                        getString(R.string.user_provided_description_data));

                return createPersonResult.personId.toString();
            } catch (Exception e) {
                publishProgress(e.getMessage());
                return null;
            }
        }

        @Override
        protected void onPreExecute() {
            setUiBeforeBackgroundTask();
        }

        @Override
        protected void onProgressUpdate(String... progress) {
            setUiDuringBackgroundTask(progress[0]);
        }

        @Override
        protected void onPostExecute(String result) {
            progressDialog.dismiss();

            if (result != null) {
                personId = result;
                setInfo("Successfully Synchronized!");

                if (mAddFace) {
                    addFace();
                } else {
                    doneAndSave();
                }
            }
        }
    }

    class UpdatePersonTask extends AsyncTask<String, String, String> {
        // Indicate the next step is to add face in this person, or finish editing this person.
        String personGroupId;
        String personName;
        UUID personId;

        UpdatePersonTask (String personGroupId, UUID personId,String personName) {
            try {
                this.personName = URLEncoder.encode(personName,"UTF-8");
                this.personGroupId = personGroupId;
                this.personId = personId;
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
        }

        @Override
        protected String doInBackground(String... params) {
            // Get an instance of face service client.
            FaceServiceClient faceServiceClient = App.getFaceServiceClient();
            try{
                publishProgress("Syncing with server to add person...");
                Log.d("-------------", "doInBackground: " + "----------------");
                // Start the request to creating person.

                faceServiceClient.updatePerson(
                        personGroupId,
                        personId,
                        personName,
                        getString(R.string.user_provided_description_data));

                Log.d("-------------", "doInBackground: " + "update");
                return "a";
            } catch (Exception e) {
                e.printStackTrace();
            }
            return "a";
        }

        @Override
        protected void onPreExecute() {
            setUiBeforeBackgroundTask();
        }

        @Override
        protected void onProgressUpdate(String... progress) {
            setUiDuringBackgroundTask(progress[0]);
        }

        @Override
        protected void onPostExecute(String result) {
            progressDialog.dismiss();

            if (result != null) {
                Log.d("----------", "onPostExecute: "+ "------------------");
            }
        }

    }

    class TrainPersonGroupTask extends AsyncTask<String, String, String> {

        private static final String TAG = "PPA";

        @Override
        protected String doInBackground(String... params) {

            // Get an instance of face service client.
            FaceServiceClient faceServiceClient = App.getFaceServiceClient();
            try{
                faceServiceClient.trainPersonGroup(params[0]);
                return params[0];
            } catch (Exception e) {
                publishProgress(e.getMessage());
                return null;
            }
        }

        @Override
        protected void onPreExecute() {
            setUiBeforeBackgroundTask();
        }

        @Override
        protected void onProgressUpdate(String... progress) {
            setUiDuringBackgroundTask(progress[0]);
        }

        @Override
        protected void onPostExecute(String result) {
            if (result != null) {
                progressDialog.dismiss();
                finish();
            }

        }
    }

    class DeleteFaceTask extends AsyncTask<String, String, String> {

        String mPersonGroupId;
        UUID mPersonId;

        DeleteFaceTask(String personGroupId, String personId) {
            mPersonGroupId = personGroupId;
            mPersonId = UUID.fromString(personId);
        }

        @Override
        protected String doInBackground(String... params) {
            // Get an instance of face service client.
            FaceServiceClient faceServiceClient = App.getFaceServiceClient();
            try{
                publishProgress("Deleting selected faces...");

                UUID faceId = UUID.fromString(params[0]);
                faceServiceClient.deletePersonFace(personGroupId, mPersonId, faceId);
                return params[0];
            } catch (Exception e) {
                publishProgress(e.getMessage());
                return null;
            }
        }

        @Override
        protected void onPreExecute() {
            setUiBeforeBackgroundTask();
        }

        @Override
        protected void onProgressUpdate(String... progress) {
            setUiDuringBackgroundTask(progress[0]);
        }

        @Override
        protected void onPostExecute(String result) {
            progressDialog.dismiss();

            if (result != null) {
                setInfo("Face " + result + " successfully deleted");
            }
        }
    }

    private void setUiBeforeBackgroundTask() {
        progressDialog.show();
    }

    // Show the status of background detection task on screen.
    private void setUiDuringBackgroundTask(String progress) {
        progressDialog.setMessage(progress);
        setInfo(progress);
    }

    boolean addNewPerson;
    String personId;
    String personGroupId;
    String oldPersonName;

    private static final int REQUEST_SELECT_IMAGE = 0;

    FaceGridViewAdapter faceGridViewAdapter;

    // Progress dialog popped up when communicating with server.
    ProgressDialog progressDialog;
    boolean read = false;
    private  void showReply(String sentenceReply){
        VoiceUtils.speak(sentenceReply);
        read = true;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_person);

        showReply("quản lý người quen");

        if (read) {
            context = getApplicationContext(); // Needs to be set
            VoiceRecognitionListener.getInstance().setListener(this); // Here we set the current listener
            startListening(); // starts listening
        }


        Bundle bundle = getIntent().getExtras();
        if (bundle != null) {
            addNewPerson = bundle.getBoolean("AddNewPerson");
            personGroupId = bundle.getString("PersonGroupId");
            oldPersonName = bundle.getString("PersonName");
            if (!addNewPerson) {
                personId = bundle.getString("PersonId");
                EditText editTextPersonName = (EditText)findViewById(R.id.edit_person_name);
                editTextPersonName.setText(oldPersonName);
            }

        }

        initializeGridView();





        progressDialog = new ProgressDialog(this);
        progressDialog.setTitle(getString(R.string.progress_dialog_title));
    }

    @Override
    public void processVoiceCommands(String... voiceCommands) {

    }

    private void initializeGridView() {
        GridView gridView = (GridView) findViewById(R.id.gridView_faces);

        gridView.setChoiceMode(GridView.CHOICE_MODE_MULTIPLE_MODAL);
        gridView.setMultiChoiceModeListener(new AbsListView.MultiChoiceModeListener() {
            @Override
            public void onItemCheckedStateChanged(
                    ActionMode mode, int position, long id, boolean checked) {
                faceGridViewAdapter.faceChecked.set(position, checked);

                GridView gridView = (GridView) findViewById(R.id.gridView_faces);
                gridView.setAdapter(faceGridViewAdapter);
            }

            @Override
            public boolean onCreateActionMode(ActionMode mode, Menu menu) {
                MenuInflater inflater = mode.getMenuInflater();
                inflater.inflate(R.menu.menu_delete_items, menu);

                faceGridViewAdapter.longPressed = true;

                GridView gridView = (GridView) findViewById(R.id.gridView_faces);
                gridView.setAdapter(faceGridViewAdapter);


                return true;
            }

            @Override
            public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
                return false;
            }

            @Override
            public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
                switch (item.getItemId()) {
                    case R.id.menu_delete_items:
                        deleteSelectedItems();
                        return true;
                    default:
                        return false;
                }
            }

            @Override
            public void onDestroyActionMode(ActionMode mode) {
                faceGridViewAdapter.longPressed = false;

                for (int i = 0; i < faceGridViewAdapter.faceChecked.size(); ++i) {
                    faceGridViewAdapter.faceChecked.set(i, false);
                }

                GridView gridView = (GridView) findViewById(R.id.gridView_faces);
                gridView.setAdapter(faceGridViewAdapter);

            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();

        faceGridViewAdapter = new FaceGridViewAdapter();
        GridView gridView = (GridView) findViewById(R.id.gridView_faces);
        gridView.setAdapter(faceGridViewAdapter);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putBoolean("AddNewPerson", addNewPerson);
        outState.putString("PersonId", personId);
        outState.putString("PersonGroupId", personGroupId);
        outState.putString("OldPersonName", oldPersonName);
    }

    @Override
    protected void onRestoreInstanceState(@NonNull Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);

        addNewPerson = savedInstanceState.getBoolean("AddNewPerson");
        personId = savedInstanceState.getString("PersonId");
        personGroupId = savedInstanceState.getString("PersonGroupId");
        oldPersonName = savedInstanceState.getString("OldPersonName");
    }


    public void doneAndSave(View view) {
        if (personId == null) {
            EditText editTextPersonName = (EditText)findViewById(R.id.edit_person_name);
            String newPersonName = editTextPersonName.getText().toString();
            new AddPersonTask(false,newPersonName).execute(personGroupId);
        } else {
            doneAndSave();
        }
    }

//    public void addFace(View view) {
//        if (personId == null) {
//            new AddPersonTask(true).execute(personGroupId);
//        } else {
//            addFace();
//        }
//    }


    private void doneAndSave() {
        TextView textWarning = (TextView)findViewById(R.id.info);
        EditText editTextPersonName = (EditText)findViewById(R.id.edit_person_name);
        String newPersonName = editTextPersonName.getText().toString();
        if (newPersonName.equals("")) {
            textWarning.setText(R.string.person_name_empty_warning_message);
            return;
        }

        StorageHelper.setPersonName(personId, newPersonName, personGroupId, PersonActivity.this);

        UUID id = UUID.fromString(personId);
        Log.d("---------------", "doneAndSave: " + id + "");

        new UpdatePersonTask(personGroupId,id,newPersonName).execute();

        new TrainPersonGroupTask().execute(personGroupId);

    }

    private void addFace() {
        setInfo("");
        Intent intent = new Intent(this, SelectImageActivity.class);
        startActivityForResult(intent, REQUEST_SELECT_IMAGE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode)
        {
            case REQUEST_SELECT_IMAGE:
                if (resultCode == RESULT_OK) {
                    Uri uriImagePicked = data.getData();
                    Intent intent = new Intent(this, AddFaceToPersonActivity.class);
                    intent.putExtra("PersonId", personId);
                    intent.putExtra("PersonGroupId", personGroupId);
                    intent.putExtra("ImageUriStr", uriImagePicked.toString());
                    startActivity(intent);
                }
                break;
            default:
                break;
        }
    }

    private void deleteSelectedItems() {
        List<String> newFaceIdList = new ArrayList<>();
        List<Boolean> newFaceChecked = new ArrayList<>();
        List<String> faceIdsToDelete = new ArrayList<>();
        for (int i = 0; i < faceGridViewAdapter.faceChecked.size(); ++i) {
            boolean checked = faceGridViewAdapter.faceChecked.get(i);
            if (checked) {
                String faceId = faceGridViewAdapter.faceIdList.get(i);
                faceIdsToDelete.add(faceId);
                new DeleteFaceTask(personGroupId, personId).execute(faceId);
            } else {
                newFaceIdList.add(faceGridViewAdapter.faceIdList.get(i));
                newFaceChecked.add(false);
            }
        }

        StorageHelper.deleteFaces(faceIdsToDelete, personId, this);

        faceGridViewAdapter.faceIdList = newFaceIdList;
        faceGridViewAdapter.faceChecked = newFaceChecked;
        new TrainPersonGroupTask().execute(personGroupId);

        faceGridViewAdapter.notifyDataSetChanged();

    }


    // Set the information panel on screen.
    private void setInfo(String info) {
        TextView textView = (TextView) findViewById(R.id.info);
        textView.setText(info);
    }

    private class FaceGridViewAdapter extends BaseAdapter {
        List<String> faceIdList;
        List<Boolean> faceChecked;
        boolean longPressed;

        FaceGridViewAdapter() {
            longPressed = false;
            faceIdList = new ArrayList<>();
            faceChecked = new ArrayList<>();

            Set<String> faceIdSet = StorageHelper.getAllFaceIds(personId, PersonActivity.this);
            for (String faceId: faceIdSet) {
                faceIdList.add(faceId);
                faceChecked.add(false);
            }
        }

        @Override
        public int getCount() {
            return faceIdList.size();
        }

        @Override
        public Object getItem(int position) {
            return faceIdList.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(final int position, View convertView, ViewGroup parent) {
            // set the item view
            if (convertView == null) {
                LayoutInflater layoutInflater
                        = (LayoutInflater)getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                convertView = layoutInflater.inflate(
                        R.layout.item_face_with_checkbox, parent, false);
            }
            convertView.setId(position);

            Uri uri = Uri.parse(StorageHelper.getFaceUri(
                    faceIdList.get(position), PersonActivity.this));
            ((ImageView)convertView.findViewById(R.id.image_face)).setImageURI(uri);

            // set the checked status of the item
            CheckBox checkBox = (CheckBox) convertView.findViewById(R.id.checkbox_face);
            if (longPressed) {
                checkBox.setVisibility(View.VISIBLE);

                checkBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                        faceChecked.set(position, isChecked);
                    }
                });
                checkBox.setChecked(faceChecked.get(position));
            } else {
                checkBox.setVisibility(View.INVISIBLE);
            }

            return convertView;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater menuInflater = getMenuInflater();
        menuInflater.inflate(R.menu.menu_add_person,menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_add_person :
                if (personId == null) {
                    EditText editTextPersonName = (EditText)findViewById(R.id.edit_person_name);
                    String newPersonName = editTextPersonName.getText().toString();
                    new AddPersonTask(true, newPersonName).execute(personGroupId);
                } else {
                    addFace();
                }
                break;
        }
        return super.onOptionsItemSelected(item);
    }
}
