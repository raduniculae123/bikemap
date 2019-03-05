package com.bikeology.bikemaps;

import android.content.Intent;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import com.google.android.gms.maps.model.LatLng;

import com.bikeology.bikemaps.AccountActivity.AccountActivity;
import com.bikeology.bikemaps.AccountActivity.LoginActivity;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import static android.support.constraint.Constraints.TAG;

public class BaseActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    protected DrawerLayout mDrawerLayout;
    protected ActionBarDrawerToggle mToggle;
    protected FirebaseAuth auth;
    protected View headView;
    protected TextView headerTitle;
    protected TextView headerName;



    protected void setupDrawer(){

        setNavigationViewListener();
        updateHeader();
        mDrawerLayout = findViewById(R.id.drawerLayout);
        mToggle = new ActionBarDrawerToggle(this, mDrawerLayout, R.string.open, R.string.close);
        mDrawerLayout.addDrawerListener(mToggle);
        mToggle.syncState();
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }

    protected void updateHeader(){
        if(FirebaseAuth.getInstance().getCurrentUser() == null)
        {
            headerTitle.setText("Not signed in");
            headerName.clearComposingText();
        }
        else
        {
            headerTitle.setText(FirebaseAuth.getInstance().getCurrentUser().getEmail());
            final DocumentReference nameRef = FirebaseFirestore.getInstance()
                    .collection(getString(R.string.collection_user_details))
                    .document(FirebaseAuth.getInstance().getUid());
            nameRef.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                @Override
                public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                    if (task.isSuccessful()) {
                        DocumentSnapshot document = task.getResult();
                        if (document != null) {
                            UserDetails userDetails  = document.toObject(UserDetails.class);
                            if(userDetails == null){

                            }
                            else{
                                headerName.setText(userDetails.getFirstName() + " " + userDetails.getLastName());
                            }
                            return;
                        }

                    }
                }
            });
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        if(mToggle.onOptionsItemSelected(item)){
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    protected void setNavigationViewListener() {
        NavigationView navigationView = findViewById(R.id.navigationView);
        navigationView.setNavigationItemSelectedListener(this);
        headView = navigationView.getHeaderView(0);
        headerTitle = headView.findViewById(R.id.nav_header_title);
        headerName = headView.findViewById(R.id.nav_header_name);
    }


    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {

            case R.id.nav_settings: {
                Intent intent = new Intent(this, SettingsActivity.class);
                this.startActivity(intent);
                break;
            }
            case R.id.nav_map: {
                Intent intent = new Intent(this, MainActivity.class);
                this.startActivity(intent);
                break;
            }
            case R.id.nav_account: {
                Intent intent = new Intent(this, AccountActivity.class);
                this.startActivity(intent);
                break;
            }
        }
        //close navigation drawer
        mDrawerLayout.closeDrawer(GravityCompat.START);
        return true;
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateHeader();
    }
}
