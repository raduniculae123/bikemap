package com.bikeology.bikemaps;

import android.content.Context;
import android.location.Location;
import android.support.annotation.NonNull;
import android.util.Log;


import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import static android.support.constraint.Constraints.TAG;

public class TripUpdater {

    private UserTrips userTrips;
    private Context context;

    public TripUpdater(Context context) {
        this.context = context;
    }

    public TripUpdater() {

    }

    public UserTrips getUserTrips() {
        return userTrips;
    }

    public void setUserTrips(UserTrips userTrips) {
        this.userTrips = userTrips;
    }


    @Override
    public String toString() {
        return "TripUpdater{" +
                "userTrips=" + userTrips +
                '}';
    }

    public void saveUserTrips(Location location)
    {
        final DocumentReference tripsRef = FirebaseFirestore.getInstance()
                .collection(context.getString(R.string.collection_user_trips))
                .document(FirebaseAuth.getInstance().getUid());
        tripsRef.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                if (task.isSuccessful()) {
                    DocumentSnapshot document = task.getResult();
                    if (document != null) {
                        userTrips  = document.toObject(UserTrips.class);
                        if(userTrips == null){
                            userTrips = new UserTrips(20, 0, FirebaseAuth.getInstance().getCurrentUser().getEmail());
                            tripsRef.set(userTrips);
                            Log.d(TAG, "user,TripsInit " + userTrips.getAvgSpeed());

                        }
                        else{
                            Log.d(TAG, "userTripsUpdate");
                        }
                        return;
                    }

                } else {
                    Log.d("userTrips", "get failed with ", task.getException());
                }
            }
        });
        Log.d(TAG, "tripsRef = " + tripsRef);
    }

}
