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

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.sam.aeye.App;
import com.example.sam.aeye.R;
import com.example.sam.aeye.utils.StorageHelper;
import com.example.sam.aeye.utils.VoiceUtils;
import com.example.sam.aeye.voice.ListeningActivity;
import com.example.sam.aeye.voice.VoiceRecognitionListener;
import com.microsoft.projectoxford.face.FaceServiceClient;
import com.microsoft.projectoxford.face.contract.Person;


import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.UUID;


public class PersonGroupActivity extends ListeningActivity {
    // Background task of adding a person group.
    class AddPersonGroupTask extends AsyncTask<String, String, String> {
        // Indicate the next step is to add person in this group, or finish editing this group.
        boolean mAddPerson;

        AddPersonGroupTask(boolean addPerson) {
            mAddPerson = addPerson;
        }

        @Override
        protected String doInBackground(String... params) {

            // Get an instance of face service client.
            FaceServiceClient faceServiceClient = App.getFaceServiceClient();
            try{
                publishProgress("Syncing with server to add person group...");

                // Start creating person group in server.
                faceServiceClient.createPersonGroup(
                        params[0],
                        getString(R.string.user_provided_person_group_name),
                        getString(R.string.user_provided_person_group_description_data));

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

                personGroupExists = true;
                GridView gridView = (GridView) findViewById(R.id.gridView_persons);
                personGridViewAdapter = new PersonGridViewAdapter();
                gridView.setAdapter(personGridViewAdapter);

                setInfo("Success. Group " + result + " created");

                if (mAddPerson) {
                    addPerson();
                } else {
                    doneAndSave(false);
                }
            }
        }
    }

    class TrainPersonGroupTask extends AsyncTask<String, String, String> {

        @Override
        protected String doInBackground(String... params) {

            // Get an instance of face service client.
            FaceServiceClient faceServiceClient = App.getFaceServiceClient();
            try{
                publishProgress("Training person group...");

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
            progressDialog.dismiss();

            if (result != null) {

            }
        }
    }



    class DeletePersonTask extends AsyncTask<String, String, String> {
        String mPersonGroupId;
        DeletePersonTask(String personGroupId) {
            mPersonGroupId = personGroupId;
        }
        @Override
        protected String doInBackground(String... params) {
            // Get an instance of face service client.
            FaceServiceClient faceServiceClient = App.getFaceServiceClient();
            try{
                publishProgress("Deleting selected persons...");

                UUID personId = UUID.fromString(params[0]);
                faceServiceClient.deletePerson(mPersonGroupId, personId);
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
                setInfo("Person " + result + " successfully deleted");
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


    private void addPerson() {
        setInfo("");

        Intent intent = new Intent(this, PersonActivity.class);
        intent.putExtra("AddNewPerson", true);
        intent.putExtra("PersonName", "");
        intent.putExtra("PersonGroupId", personGroupId);
        startActivity(intent);
    }

    boolean addNewPersonGroup;
    boolean personGroupExists;
    String personGroupId;
    TextView persongroupid;
    String oldPersonGroupName;

    PersonGridViewAdapter personGridViewAdapter;

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
        setContentView(R.layout.activity_person_group);


        showReply("quản lý người quen");

        if (read) {
            context = getApplicationContext(); // Needs to be set
            VoiceRecognitionListener.getInstance().setListener(this); // Here we set the current listener
            startListening(); // starts listening
        }


        progressDialog = new ProgressDialog(this);
        progressDialog.setTitle(getString(R.string.progress_dialog_title));

        persongroupid = (TextView) findViewById(R.id.person_group_id);


        personGroupId = StorageHelper.getPersonGroupId("nguoinha", this);

        if (!personGroupId.equals("")) {
            oldPersonGroupName = "nguoinha";
            addNewPersonGroup = false;
            personGroupExists = true;
            persongroupid.setText(personGroupId);

        } else {
            personGroupId = "29f1ccf6-16a3-4e09-95c7-24e5e31a2acf";
            StorageHelper.setPersonGroupId(personGroupId, "nguoinha", this);
            addNewPersonGroup = true;
            personGroupExists = false;
            oldPersonGroupName = "nguoinha";
            persongroupid.setText(personGroupId);
            new AddPersonGroupTask(false).execute(personGroupId);
            new GetPersonIdsTask().execute(personGroupId);
        }

        Log.d("---------", "onCreate: " + personGroupId);

        initializeListView();


    }

    @Override
    public void processVoiceCommands(String... voiceCommands) {
        Toast.makeText(context, voiceCommands[0], Toast.LENGTH_SHORT).show();
        if (voiceCommands[0].toLowerCase().contains("về")) {
            finish();
        }
        restartListeningService();
    }

    private void initializeListView() {
        GridView gridView = (GridView) findViewById(R.id.gridView_persons);
        gridView.setAdapter(personGridViewAdapter);

        gridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    String personId = personGridViewAdapter.personIdList.get(position);
                    String personName = StorageHelper.getPersonName(
                            personId, personGroupId, PersonGroupActivity.this);

                    Intent intent = new Intent(PersonGroupActivity.this, PersonActivity.class);
                    intent.putExtra("AddNewPerson", false);
                    intent.putExtra("PersonName", personName);
                    intent.putExtra("PersonId", personId);
                    intent.putExtra("PersonGroupId", personGroupId);
                    startActivity(intent);
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (personGroupExists) {
            GridView gridView = (GridView) findViewById(R.id.gridView_persons);
            personGridViewAdapter = new PersonGridViewAdapter();
            gridView.setAdapter(personGridViewAdapter);
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putBoolean("AddNewPersonGroup", addNewPersonGroup);
        outState.putString("OldPersonGroupName", oldPersonGroupName);
        outState.putString("PersonGroupId", personGroupId);
        outState.putBoolean("PersonGroupExists", personGroupExists);
    }

    @Override
    protected void onRestoreInstanceState(@NonNull Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);

        addNewPersonGroup = savedInstanceState.getBoolean("AddNewPersonGroup");
        personGroupId = savedInstanceState.getString("PersonGroupId");
        oldPersonGroupName = savedInstanceState.getString("OldPersonGroupName");
        personGroupExists = savedInstanceState.getBoolean("PersonGroupExists");
    }

    public void doneAndSave(View view) {
        if (!personGroupExists) {
            new AddPersonGroupTask(false).execute(personGroupId);
        } else {
            doneAndSave(true);
        }
    }

    private void doneAndSave(boolean trainPersonGroup) {

        new TrainPersonGroupTask().execute(personGroupId);

    }

    private void deleteSelectedItems(String personGroupId, String personId) {
        List<String> newPersonIdList = new ArrayList<>();

        new DeletePersonTask(personGroupId).execute(personId);

        StorageHelper.deleteAPerson(personId, personGroupId, this);
        Set<String> personIdSet = StorageHelper.getAllPersonIds(personGroupId, PersonGroupActivity.this);
        for (String pId : personIdSet) {
            newPersonIdList.add(pId);
        }

        personGridViewAdapter.personIdList = newPersonIdList;
        new TrainPersonGroupTask().execute(personGroupId);
        progressDialog.dismiss();
        personGridViewAdapter.notifyDataSetChanged();
    }
    // Set the information panel on screen.
    private void setInfo(String info) {
        TextView textView = (TextView) findViewById(R.id.info);
        textView.setText(info);
    }

    private class PersonGridViewAdapter extends BaseAdapter {

        List<String> personIdList;

        PersonGridViewAdapter() {
            personIdList = new ArrayList<>();

            Set<String> personIdSet = StorageHelper.getAllPersonIds(personGroupId, PersonGroupActivity.this);
            for (String personId: personIdSet) {
                personIdList.add(personId);
            }
        }

        @Override
        public int getCount() {
            return personIdList.size();
        }

        @Override
        public Object getItem(int position) {
            return personIdList.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(final int position, View convertView, ViewGroup parent) {
            // set the item view
            if (convertView == null) {
                LayoutInflater layoutInflater = (LayoutInflater)getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                convertView = layoutInflater.inflate(R.layout.item_person, parent, false);
            }
            convertView.setId(position);

            final String personId = personIdList.get(position);
            Set<String> faceIdSet = StorageHelper.getAllFaceIds(personId, PersonGroupActivity.this);
            if (!faceIdSet.isEmpty()) {
                Iterator<String> it = faceIdSet.iterator();
                Uri uri = Uri.parse(StorageHelper.getFaceUri(it.next(), PersonGroupActivity.this));
                ((ImageView)convertView.findViewById(R.id.image_person)).setImageURI(uri);
                ((TextView)convertView.findViewById(R.id.number_picture)).setText("Có " + faceIdSet.size() + " ảnh");
            } else {
                Drawable drawable = getResources().getDrawable(R.drawable.select_image);
                ((ImageView)convertView.findViewById(R.id.image_person)).setImageDrawable(drawable);
                ((TextView)convertView.findViewById(R.id.number_picture)).setText("Không có ảnh");
            }

            // set the text of the item
            String personName = StorageHelper.getPersonName(personId, personGroupId, PersonGroupActivity.this);
            ((TextView)convertView.findViewById(R.id.text_person)).setText(personName.split("-")[0]);

            TextView txtDelete = (TextView) convertView.findViewById(R.id.txt_delete_person);
            txtDelete.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    new AlertDialog.Builder(PersonGroupActivity.this)
                            .setTitle("Xác nhận")
                            .setMessage("Bạn có chắc là muốn xóa người này ra khỏi danh sách?")
                            .setIcon(android.R.drawable.ic_dialog_alert)
                            .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {

                                public void onClick(DialogInterface dialog, int whichButton) {
                                    Toast.makeText(PersonGroupActivity.this, "Xóa Thành Công", Toast.LENGTH_SHORT).show();
                                    deleteSelectedItems(personGroupId ,personId);
                                }})
                            .setNegativeButton(android.R.string.no, null).show();
                }
            });

            // set the checked status of the ite

            return convertView;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater menuInflater = getMenuInflater();
        menuInflater.inflate(R.menu.menu_add_person, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        addPerson();
        return super.onOptionsItemSelected(item);
    }


    class GetPersonIdsTask extends AsyncTask<String, String, Person[]> {

        String groupid = "";

        @Override
        protected Person[] doInBackground(String... params) {


            // Get an instance of face service client.
            FaceServiceClient faceServiceClient = App.getFaceServiceClient();
            try{
                publishProgress("Training person group...");


                groupid = params[0];
                Log.d("", "doInBackground: " + groupid);

                return faceServiceClient.listPersons(params[0]);

            } catch (Exception e) {
                return null;
            }
        }

        @Override
        protected void onPreExecute() {

        }

        @Override
        protected void onProgressUpdate(String... progress) {

        }

        @Override
        protected void onPostExecute(Person[] result) {
            List<String> newPersonIdList = new ArrayList<>();
            if(result != null){
                for (Person person : result) {
                    try {
                        String name = URLDecoder.decode(person.name, "UTF-8");
                        StorageHelper.setPersonName(person.personId.toString(),name, groupid, PersonGroupActivity.this);
                    } catch (UnsupportedEncodingException e) {
                        e.printStackTrace();
                    }


                }

                Set<String> personIdSet = StorageHelper.getAllPersonIds(personGroupId, PersonGroupActivity.this);
                for (String pId : personIdSet) {
                    newPersonIdList.add(pId);
                }

                GridView gridView = (GridView) findViewById(R.id.gridView_persons);
                personGridViewAdapter = new PersonGridViewAdapter();
                gridView.setAdapter(personGridViewAdapter);
            }


        }
    }
}
