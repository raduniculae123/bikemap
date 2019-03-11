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
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

public class UserDetailsActivity extends BaseActivity {

    private static final String TAG = "UserDetailsActivity";

    private Button button_submit;
    private Button button_skip;

    private EditText firstName;
    private EditText lastName;

    private ProgressBar progressBar;
    private FirebaseAuth firebaseAuth;
    private FirebaseFirestore mDb;

    private UserDetails mUserDetails;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRequestedOrientation (ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        setContentView(R.layout.activity_user_details);
        firebaseAuth = FirebaseAuth.getInstance();
        mDb = FirebaseFirestore.getInstance();

        setNavigationViewListener();
        updateHeader();


        button_submit = findViewById(R.id.submit_button);
        button_skip = findViewById(R.id.skip_button);
        firstName = findViewById(R.id.firstName);
        lastName = findViewById(R.id.lastName);
        progressBar = findViewById(R.id.detailsProgressBar);

        button_submit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                String fName = firstName.getText().toString().trim();
                String lName = lastName.getText().toString().trim();

                if (TextUtils.isEmpty(fName)) {
                    Toast.makeText(getApplicationContext(), "Enter first name!", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (TextUtils.isEmpty(lName)) {
                    Toast.makeText(getApplicationContext(), "Enter last name!", Toast.LENGTH_SHORT).show();
                    return;
                }

                progressBar.setVisibility(View.VISIBLE);
                mUserDetails = new UserDetails(fName, lName);
                DocumentReference detailsRef = mDb.collection(getString(R.string.collection_user_details))
                        .document(FirebaseAuth.getInstance().getUid());
                detailsRef.set(mUserDetails).addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        Log.d(TAG, "saveUserDetails onComplete");
                        if (!task.isSuccessful()) {
                            Toast.makeText(UserDetailsActivity.this, "Authentication failed." + task.getException(),
                                    Toast.LENGTH_SHORT).show();
                        } else {
                            Log.d(TAG, "Full name saved");
                            startActivity(new Intent(UserDetailsActivity.this, LoginActivity.class));
                            finish();
                        }
                    }
                });
            }
        });

        button_skip.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(UserDetailsActivity.this, LoginActivity.class));
                finish();
            }
        });

    }
}
