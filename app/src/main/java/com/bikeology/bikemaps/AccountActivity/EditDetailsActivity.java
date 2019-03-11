package com.bikeology.bikemaps.AccountActivity;

import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.bikeology.bikemaps.BaseActivity;
import com.bikeology.bikemaps.R;
import com.bikeology.bikemaps.UserDetails;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import static android.support.constraint.Constraints.TAG;

public class EditDetailsActivity extends BaseActivity {

    private static final String TAG = "EditDetailsActivity";

    private Button button_submit;
    private Button button_cancel;

    private EditText firstName;
    private EditText lastName;
    private EditText changePassword;

    private ProgressBar progressBar;
    private FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
    private FirebaseFirestore mDb;
    private UserDetails mUserDetails = new UserDetails();

    private boolean isPasswordUpdated;

    private String newFirstName;
    private String newLastName;

    private String fNameHint;
    private String lNameHint;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setRequestedOrientation (ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_details);

        mDb = FirebaseFirestore.getInstance();

        setNavigationViewListener();
        updateHeader();

        Intent intent = getIntent();
        fNameHint = intent.getStringExtra("fName");
        lNameHint = intent.getStringExtra("lName");

        button_submit = findViewById(R.id.submit_button);
        button_cancel = findViewById(R.id.cancel_button);
        firstName = findViewById(R.id.firstName);
        lastName = findViewById(R.id.lastName);
        changePassword = findViewById(R.id.changePassword);
        progressBar = findViewById(R.id.detailsProgressBar);

        firstName.setHint(fNameHint);
        lastName.setHint(lNameHint);

        isPasswordUpdated = false;

        button_submit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                final String fName = firstName.getText().toString().trim();
                final String lName = lastName.getText().toString().trim();

                if (user != null && !changePassword.getText().toString().trim().equals("")) {
                    if (changePassword.getText().toString().trim().length() < 6) {
                        changePassword.setError("Password too short, enter minimum 6 characters");
                        progressBar.setVisibility(View.GONE);
                        return;
                    } else {
                        user.updatePassword(changePassword.getText().toString().trim())
                                .addOnCompleteListener(new OnCompleteListener<Void>() {
                                    @Override
                                    public void onComplete(@NonNull Task<Void> task) {
                                        if (task.isSuccessful()) {
                                            Toast.makeText(EditDetailsActivity.this, "Password is updated, sign in with new password!", Toast.LENGTH_SHORT).show();
                                            isPasswordUpdated = true;
                                            progressBar.setVisibility(View.GONE);
                                        } else {
                                            Toast.makeText(EditDetailsActivity.this, "Failed to update password!", Toast.LENGTH_SHORT).show();
                                            progressBar.setVisibility(View.GONE);
                                            return;
                                        }
                                    }
                                });
                    }
                }

                progressBar.setVisibility(View.VISIBLE);

                final DocumentReference nameRef = FirebaseFirestore.getInstance()
                        .collection(getString(R.string.collection_user_details))
                        .document(FirebaseAuth.getInstance().getUid());
                nameRef.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                        if (task.isSuccessful()) {
                            DocumentSnapshot document = task.getResult();
                            if (document != null) {
                                mUserDetails  = document.toObject(UserDetails.class);
                                if(mUserDetails == null){
                                    Log.d(TAG, "Name null");
                                }
                                else{
                                    Log.d(TAG, "db first name: " + mUserDetails.getFirstName());
                                    Log.d(TAG, "db last name: " + mUserDetails.getLastName());

                                    Log.d(TAG, "Name updated");

                                    if(!fName.equals(mUserDetails.getFirstName()) && !fName.equals("")){
                                        newFirstName = fName;
                                    }
                                    else if(fName.equals("")){
                                        newFirstName = mUserDetails.getFirstName();
                                    }

                                    if(!lName.equals(mUserDetails.getLastName()) && !lName.equals("")){
                                        newLastName = lName;
                                    }
                                    else if(lName.equals("")){
                                        newLastName = mUserDetails.getLastName();
                                    }
                                    Log.d(TAG, "oldFirstName: " + mUserDetails.getFirstName());
                                    Log.d(TAG, "oldLastName: " + mUserDetails.getLastName());
                                    Log.d(TAG, "newFirstName: " + newFirstName);
                                    Log.d(TAG, "newLastName: " + newLastName);

                                    UserDetails newUserDetails= new UserDetails(newFirstName, newLastName);
                                    DocumentReference detailsRef = mDb.collection(getString(R.string.collection_user_details))
                                            .document(FirebaseAuth.getInstance().getUid());
                                    detailsRef.set(newUserDetails).addOnCompleteListener(new OnCompleteListener<Void>() {
                                        @Override
                                        public void onComplete(@NonNull Task<Void> task) {
                                            Log.d(TAG, "saveUserDetails onComplete");
                                            if (!task.isSuccessful()) {
                                                Toast.makeText(EditDetailsActivity.this, "Authentication failed." + task.getException(),
                                                        Toast.LENGTH_SHORT).show();
                                            } else {
                                                Log.d(TAG, "Full name saved");
                                                if (isPasswordUpdated) {
                                                    auth.signOut();
                                                }
                                                startActivity(new Intent(EditDetailsActivity.this, LoginActivity.class));
                                                finish();

                                            }
                                        }
                                    });

                                }
                            }

                        } else {
                            Log.d(TAG,"userDetails failed");
                        }
                    }
                });
            }
        });

        button_cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(EditDetailsActivity.this, LoginActivity.class));
                finish();
            }
        });
    }
}
