package com.example.chatapp;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.util.HashMap;

public class AddPostActivity extends AppCompatActivity {

    FirebaseAuth fAuth;
    DatabaseReference userDbRef;

    //permissions for external
    private static final int CAMERA_REQUEST_CODE = 100;
    private static final int STORAGE_REQUEST_CODE = 200;

    //image pick constants
    private static final int IMAGE_PICK_CAMERA_CODE = 300;
    private static final int IMAGE_PICK_GALLERY_CODE = 400;

    //permissions array
    String[] cameraPermissions;
    String[] storagePermissions;

    EditText titleET, descriptionET;
    ImageView imageIV;
    Button uploadBTN;

    //user info
    String name, email, uid, dp;

    //image picked will be named in this uri
    Uri image_uri = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_post);

        //initialize permissions array
        cameraPermissions = new String[]{Manifest.permission.CAMERA,
                Manifest.permission.WRITE_EXTERNAL_STORAGE};
        storagePermissions = new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE};

        //initialize
        fAuth = FirebaseAuth.getInstance();
        checkUserStatus();

        //get info from the currently signed in user
        userDbRef = FirebaseDatabase.getInstance().getReference("Users");
        Query query = userDbRef.orderByChild("email").equalTo(email);
        query.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                for (DataSnapshot ds: dataSnapshot.getChildren()){
                    name = "" + ds.child("name").getValue();
                    email = "" + ds.child("email").getValue();
                    dp = "" + ds.child("image").getValue();
                }

            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

        titleET = findViewById(R.id.postTitle);
        descriptionET = findViewById(R.id.postDescription);
        imageIV = findViewById(R.id.postImage);
        uploadBTN = findViewById(R.id.postUpload);

        //get image from camera and gallery on click
        imageIV.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //show image dialog
                showImagePickDialog();
            }
        });

        //button click listener
        uploadBTN.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //get data from edit Text
                String title = titleET.getText().toString().trim();
                String description = descriptionET.getText().toString().trim();

                //if user leave the form empty
                if (TextUtils.isEmpty(title)){
                    Toast.makeText(AddPostActivity.this, "Enter title",
                            Toast.LENGTH_SHORT).show();
                    return;
                }

                if (TextUtils.isEmpty(description)){
                    Toast.makeText(AddPostActivity.this, "Enter description", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (image_uri == null){
                    //post without image
                    uploadInfo(title, description, "noImage");
                }
                else {
                    //post with image
                    uploadInfo(title, description, String.valueOf(image_uri));
                }
            }
        });
    }

    private void uploadInfo(final String title, final String description, final String uri) {

        //detect time posting
        final String timeStamp = String.valueOf(System.currentTimeMillis());

        String filePathAndName = "Posts/" + "post_" + timeStamp;

        if (!uri.equals("noImage")){
            //if post with image
            StorageReference ref = FirebaseStorage.getInstance().getReference().child(filePathAndName);

            ref.putFile(Uri.parse(uri))
                    .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                        @Override
                        public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                            //when the image is successfully uploaded in firebase, get the url
                            Task<Uri> uriTask = taskSnapshot.getStorage().getDownloadUrl();
                            while (!uriTask.isSuccessful());

                            String downloadUri = uriTask.getResult().toString();

                            if (uriTask.isSuccessful()){

                                //after received the url, upload the info to firebase database
                                HashMap<Object, String> hashMap = new HashMap<>();

                                //info of the post
                                hashMap.put("uid", uid);
                                hashMap.put("userName", name);
                                hashMap.put("userEmail", email);
                                hashMap.put("userDp", dp);
                                hashMap.put("postId", timeStamp);
                                hashMap.put("postTitle", title);
                                hashMap.put("postDescription", description);
                                hashMap.put("postImage", downloadUri);
                                hashMap.put("postTime", timeStamp);

                                //path to store post data
                                DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Posts");

                                //put data in this reference
                                ref.child(timeStamp).setValue(hashMap)
                                        .addOnSuccessListener(new OnSuccessListener<Void>() {
                                            @Override
                                            public void onSuccess(Void aVoid) {
                                                //added in database
                                                Toast.makeText(AddPostActivity.this,
                                                        "Post published", Toast.LENGTH_SHORT).show();

                                                //reset to blank form again
                                                titleET.setText("");
                                                descriptionET.setText("");
                                                imageIV.setImageURI(null);
                                                image_uri = null;
                                            }
                                        })
                                        .addOnFailureListener(new OnFailureListener() {
                                            @Override
                                            public void onFailure(@NonNull Exception e) {
                                                //failed to add
                                                Toast.makeText(AddPostActivity.this,
                                                        ""+e.getMessage(), Toast.LENGTH_SHORT).show();

                                            }
                                        });
                            }
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            //failed to upload
                            Toast.makeText(AddPostActivity.this,
                                    ""+e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
        }
        else {
            //post without image
            //after received the url, upload the info to firebase database
            HashMap<Object, String> hashMap = new HashMap<>();

            //info of the post
            hashMap.put("userid", uid);
            hashMap.put("userName", name);
            hashMap.put("userEmail", email);
            hashMap.put("userDp", dp);
            hashMap.put("postId", timeStamp);
            hashMap.put("postTitle", title);
            hashMap.put("postDescription", description);
            hashMap.put("postImage", "noImage");
            hashMap.put("postTime", timeStamp);

            //path to store post data
            DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Posts");

            //put data in this reference
            ref.child(timeStamp).setValue(hashMap)
                    .addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void aVoid) {
                            //added in database
                            Toast.makeText(AddPostActivity.this,
                                    "Post published", Toast.LENGTH_SHORT).show();
                            //reset to blank form again
                            titleET.setText("");
                            descriptionET.setText("");
                            imageIV.setImageURI(null);
                            image_uri = null;
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            //failed to add
                            Toast.makeText(AddPostActivity.this,
                                    ""+e.getMessage(), Toast.LENGTH_SHORT).show();

                        }
                    });
        }
    }

    private void showImagePickDialog() {
        //options(camera, gallery) to show in dialog
        String[] options = {"Camera", "Gallery"};

        //dialog
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Choose image from");

        //set options
        builder.setItems(options,  new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                //handle item click
                if (i == 0) {
                    //when click camera

                    if (!checkCameraPermission()) {
                        requestCameraPermission();
                    } else {
                        pickFromCamera();
                    }
                }
                if (i == 1) {
                    //gallery clicked
                    if (!checkStoragePermission()) {
                        requestStoragePermission();
                    } else {
                        pickFromGallery();
                    }
                }
            }
        });

        //create dialog
        builder.create().show();

    }

    private void pickFromGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        startActivityForResult(intent, IMAGE_PICK_GALLERY_CODE);
    }

    private void pickFromCamera() {

        ContentValues cv = new ContentValues();
        cv.put(MediaStore.Images.Media.TITLE, "Temperory Pick");
        cv.put(MediaStore.Images.Media.DESCRIPTION, "Temporary Description");
        image_uri = getContentResolver()
                .insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, cv);

        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        intent.putExtra(MediaStore.EXTRA_OUTPUT, image_uri);
        startActivityForResult(intent, IMAGE_PICK_CAMERA_CODE);
    }

    //gallery/storage
    private boolean checkStoragePermission() {
        //enable storage permission
        boolean result = ContextCompat.checkSelfPermission(this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE)
                == (PackageManager.PERMISSION_GRANTED);

        return result;
    }

    private void requestStoragePermission() {
        //request runtime storage permission
        ActivityCompat.requestPermissions(this, storagePermissions, STORAGE_REQUEST_CODE);
    }


    //camera
    private boolean checkCameraPermission() {
        //enable camera permission
        //true and false

        boolean result = ContextCompat.checkSelfPermission(this,
                Manifest.permission.CAMERA) == (PackageManager.PERMISSION_GRANTED);
        boolean result1 = ContextCompat.checkSelfPermission(this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE)
                == (PackageManager.PERMISSION_GRANTED);
        return result && result1;
    }

    private void requestCameraPermission() {
        //request runtime camera permission
        ActivityCompat.requestPermissions(this, cameraPermissions, CAMERA_REQUEST_CODE);
    }


    private void checkUserStatus() {
        //get current user info
        FirebaseUser user = fAuth.getCurrentUser();
        if (user != null) {
            email = user.getEmail();
            uid = user.getUid();
        } else {
            //if already logged out, app will automatically go to main activity
            startActivity(new Intent
                    (AddPostActivity.this, MainActivity.class));
            finish();
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        checkUserStatus();
    }

    @Override
    protected void onResume() {
        super.onResume();
        checkUserStatus();
    }

    //inflate options menu

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {

        //get item id
        int id = item.getItemId();
        if (id == R.id.action_logout) {
            fAuth.signOut();
            checkUserStatus();
        }
        return super.onOptionsItemSelected(item);
    }

    //handle permission results
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        //this method is called when user press allow or deny from the dialog

        switch (requestCode) {
            case CAMERA_REQUEST_CODE: {
                if (grantResults.length > 0) {
                    boolean cameraAccepted = grantResults[0] == PackageManager.PERMISSION_GRANTED;
                    boolean storageAccepted = grantResults[1] == PackageManager.PERMISSION_GRANTED;

                    if (cameraAccepted && storageAccepted) {
                        //if both permission are granted
                        pickFromCamera();
                    }
                    else {
                        Toast.makeText(this, "Error. Need camera and storage permission",
                                Toast.LENGTH_SHORT).show();
                    }
                } else {

                }
            }

            break;
            case STORAGE_REQUEST_CODE: {
                if (grantResults.length > 0) {
                    boolean storageAccepted = grantResults[0] == PackageManager.PERMISSION_GRANTED;
                    if (storageAccepted) {
                        //if gallery permissions are granted
                        pickFromGallery();
                    } else {
                        Toast.makeText(this, "Error. Need storage permission",
                                Toast.LENGTH_SHORT).show();
                    }
                } else {

                }

            }
            break;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        //method is called after pick an image from camera or gallery
        if (resultCode == RESULT_OK){
            if (requestCode == IMAGE_PICK_GALLERY_CODE){
                //if pick gallery, get uri of image
                image_uri = data.getData();

                //set to imageview
                imageIV.setImageURI(image_uri);
            }
            else if (requestCode == IMAGE_PICK_CAMERA_CODE){
                //image is picked from camera, get uri of image

                imageIV.setImageURI(image_uri);
            }
        }
        super.onActivityResult(requestCode, resultCode, data);

    }
}




