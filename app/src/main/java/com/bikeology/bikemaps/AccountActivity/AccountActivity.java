package com.bikeology.bikemaps.AccountActivity;

import com.bikeology.bikemaps.BaseActivity;
import com.bikeology.bikemaps.R;

import android.os.Bundle;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;


import com.bikeology.bikemaps.UserDetails;
import com.bikeology.bikemaps.UserTrips;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import static android.support.constraint.Constraints.TAG;

public class AccountActivity extends BaseActivity {

    private Button buttonEditDetails, buttonChangePassword, buttonSignOut;
    private TextView email;
    private TextView fullName;

    private EditText newPassword;
    private ProgressBar progressBar;
    private UserDetails userDetails = new UserDetails();
    private FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_account);
        setupDrawer();
//get firebase auth instance
        auth = FirebaseAuth.getInstance();
        email = findViewById(R.id.userEmail);
        fullName = findViewById(R.id.fullNameText);

        //get current user
       //final FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        setDataToView(user);
        authListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                FirebaseUser user = firebaseAuth.getCurrentUser();
                if (user == null) {
                    // user auth state is changed - user is null
                    // launch login activity
                    startActivity(new Intent(AccountActivity.this, LoginActivity.class));
                    finish();
                }
            }
        };

        buttonEditDetails = findViewById(R.id.button_edit_details);
        buttonChangePassword = findViewById(R.id.button_change_password);
        buttonSignOut = findViewById(R.id.button_sign_out);

        newPassword = findViewById(R.id.newPassword);

        newPassword.setVisibility(View.GONE);
        buttonChangePassword.setVisibility(View.GONE);

        progressBar = findViewById(R.id.accountProgressBar);

        if (progressBar != null) {
            progressBar.setVisibility(View.GONE);
        }


        buttonEditDetails.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                Intent i = new Intent(AccountActivity.this, EditDetailsActivity.class);
                i.putExtra("fName", userDetails.getFirstName());
                i.putExtra("lName", userDetails.getLastName());
                startActivity(i);
                finish();

            }
        });

        buttonChangePassword.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                progressBar.setVisibility(View.VISIBLE);
                if (user != null && !newPassword.getText().toString().trim().equals("")) {
                    if (newPassword.getText().toString().trim().length() < 6) {
                        newPassword.setError("Password too short, enter minimum 6 characters");
                        progressBar.setVisibility(View.GONE);
                    } else {
                        user.updatePassword(newPassword.getText().toString().trim())
                                .addOnCompleteListener(new OnCompleteListener<Void>() {
                                    @Override
                                    public void onComplete(@NonNull Task<Void> task) {
                                        if (task.isSuccessful()) {
                                            Toast.makeText(AccountActivity.this, "Password is updated, sign in with new password!", Toast.LENGTH_SHORT).show();
                                            signOut();
                                            progressBar.setVisibility(View.GONE);
                                        } else {
                                            Toast.makeText(AccountActivity.this, "Failed to update password!", Toast.LENGTH_SHORT).show();
                                            progressBar.setVisibility(View.GONE);
                                        }
                                    }
                                });
                    }
                } else if (newPassword.getText().toString().trim().equals("")) {
                    newPassword.setError("Enter password");
                    progressBar.setVisibility(View.GONE);
                }
            }
        });




        buttonSignOut.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                signOut();
            }
        });

    }

    @SuppressLint("SetTextI18n")
    private void setDataToView(final FirebaseUser user) {

        if(user != null)
            email.setText(user.getEmail());
        //fullName.setText(userDetails.getFirstName() + " " + userDetails.getLastName());

        final DocumentReference nameRef = FirebaseFirestore.getInstance()
                .collection(getString(R.string.collection_user_details))
                .document(FirebaseAuth.getInstance().getUid());
        nameRef.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                if (task.isSuccessful()) {
                    DocumentSnapshot document = task.getResult();
                    if (document != null) {
                        userDetails  = document.toObject(UserDetails.class);
                        if(userDetails == null){
                            Log.d(TAG, "Name null");

                        }
                        else{
                            fullName.setText(userDetails.getFirstName() + " " + userDetails.getLastName());
                            Log.d(TAG, "Name updated");
                        }
                        return;
                    }

                } else {
                    Log.d(TAG,"userDetails failed");
                }
            }
        });
    }

    // this listener will be called when there is change in firebase user session
    FirebaseAuth.AuthStateListener authListener = new FirebaseAuth.AuthStateListener() {
        @SuppressLint("SetTextI18n")
        @Override
        public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
            FirebaseUser user = firebaseAuth.getCurrentUser();
            if (user == null) {
                // user auth state is changed - user is null
                // launch login activity
                startActivity(new Intent(AccountActivity.this, LoginActivity.class));
                finish();
            } else {
                setDataToView(user);

            }
        }


    };

    //sign out method
    public void signOut() {
        auth.signOut();


// this listener will be called when there is change in firebase user session
        FirebaseAuth.AuthStateListener authListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                FirebaseUser user = firebaseAuth.getCurrentUser();
                if (user == null) {
                    // user auth state is changed - user is null
                    // launch login activity
                    startActivity(new Intent(AccountActivity.this, LoginActivity.class));
                    finish();
                }
            }
        };
    }

    @Override
    protected void onResume() {
        super.onResume();
        progressBar.setVisibility(View.GONE);
        setDataToView(user);
    }

    @Override
    public void onStart() {
        super.onStart();
        auth.addAuthStateListener(authListener);
        setDataToView(user);
    }

    @Override
    public void onStop() {
        super.onStop();
        if (authListener != null) {
            auth.removeAuthStateListener(authListener);
        }
    }
}
