package com.example.chatapp;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.HashMap;

public class RegisterActivity extends AppCompatActivity {

    //declare the object
    EditText mEmailEt, mPasswordEt;
    Button mRegisterBtn;
    TextView mHaveAccountTv;

    ProgressDialog pdialog;

    //declare firebase authentication function
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        mEmailEt = findViewById(R.id.emailEt);
        mPasswordEt = findViewById(R.id.passwordEt);
        mRegisterBtn = findViewById(R.id.registerNowBtn);
        mHaveAccountTv = findViewById(R.id.have_accountTv);

        //initialize firebase authentication
        mAuth = FirebaseAuth.getInstance();

        //progress dialog
        pdialog = new ProgressDialog(this);
        pdialog.setMessage("Registering user...");

        //on click button for register
        mRegisterBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //input from user
                String email = mEmailEt.getText().toString().trim();
                String password = mPasswordEt.getText().toString().trim();

                //validate if have issue
                if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()){
                    mEmailEt.setError("Invalid email format");
                    mEmailEt.setFocusable(true);
                }
                else if (password.length()<6){
                    mPasswordEt.setError("Password length must be at least 6 and above");
                    mPasswordEt.setFocusable(true);
                }

                //if no issue
                else {
                    registerUser(email, password);
                }
            }
        });

        //on-click listener
        mHaveAccountTv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent
                        (RegisterActivity.this, LoginActivity.class));
            }
        });
    }

    private void registerUser(String email, String password) {
            mAuth.createUserWithEmailAndPassword(email,password)
                    .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                        @Override
                        public void onComplete(@NonNull Task<AuthResult> task) {
                            if (task.isSuccessful()){
                                //if sign in successful, update UI with their information
                                FirebaseUser user = mAuth.getCurrentUser();

                                //get uid from auth
                                String email = user.getEmail();
                                String uid = user.getUid();
                                //store in realtime db using hashmap
                                HashMap<Object, String> hashMap = new HashMap<>();
                                hashMap.put("email", email);
                                hashMap.put("uid", uid);
                                hashMap.put("name", "");
                                hashMap.put("phone", "");
                                hashMap.put("image", "");

                                FirebaseDatabase database = FirebaseDatabase.getInstance();
                                DatabaseReference reference = database.getReference("Users");
                                reference.child(uid).setValue(hashMap);

                                Toast.makeText(RegisterActivity.this, "Successfully registered.." +
                                        user.getEmail(), Toast.LENGTH_SHORT).show();
                                startActivity(new Intent(RegisterActivity.this, DashboardActivity.class));
                                finish();
                            }
                            else {
                                //if sign in failed, display error message
                                Toast.makeText(RegisterActivity.this,
                                        "Authentication failed", Toast.LENGTH_SHORT).show();
                            }
                        }
                    }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    Toast.makeText(RegisterActivity.this,
                            ""+e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
    }
}