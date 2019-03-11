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
import com.google.firebase.firestore.GeoPoint;

import java.lang.Math;


import static android.support.constraint.Constraints.TAG;

public class TripUpdater {

    private UserTrips userTrips;
    private Context context;

    private GeoPoint oldLocation;

    private int refreshInterval = 0;
    private int stopInterval = 0;
    private int moveInterval = 0;

    private boolean isUserMoving = false;

    private final double minimumRefreshDistance = 0.01; // in km
    private double intermediateDistance = 0;

    private double avgSpeed;

    private double startTime;
    private double endTime;
    public double tripTime;
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

    public void saveUserTrips(final Location location)
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
                            if(oldLocation == null){
                                oldLocation = new GeoPoint(location.getLatitude(), location.getLongitude());
                            }
                            else {
                                GeoPoint newLocation = new GeoPoint(location.getLatitude(), location.getLongitude());
                                refreshInterval ++;
                                    double d = calculateDistance(oldLocation, newLocation);
                                    if(isUserMoving){
                                        stopInterval = 0;
                                        if(moveInterval == 0)
                                        {
                                            startTime = System.currentTimeMillis()/1000;
                                        }
                                        moveInterval ++;
                                        userTrips.setTotalDistance(userTrips.getTotalDistance() + d);
                                        intermediateDistance += d;
                                    }
                                    else{
                                        if(moveInterval>20){
                                            stopInterval ++;
                                        }
                                        if(stopInterval == 5){
                                            stopCurrentTrip();
                                        }
                                    }
                                if(refreshInterval == 50) {
                                    stopCurrentTrip();
                                }

                                tripsRef.set(userTrips);
                            }
                        }
                        return;
                    }

                } else {
                }
            }
        });
    }

    private void stopCurrentTrip() {
        refreshInterval = 0;
        endTime = System.currentTimeMillis()/1000;
        tripTime = endTime - startTime;
        avgSpeed = (intermediateDistance*1000)/(tripTime*3600);
        intermediateDistance = 0;
        stopInterval = 0;
        moveInterval = 0;

    }

    private double calculateDistance(GeoPoint oldLocation, GeoPoint newLocation){
        double R = 6371;

        double lat1 = Math.toRadians(oldLocation.getLatitude());
        double lat2 = Math.toRadians(newLocation.getLatitude());

        double long1 = Math.toRadians(oldLocation.getLongitude());
        double long2 = Math.toRadians(newLocation.getLongitude());

        double dLat = lat2 - lat1;
        double dLong = long2 - long1;

        double a = Math.sin(dLat/2) * Math.sin(dLat/2) +
                Math.cos(lat1) * Math.cos(lat2) *
                        Math.sin(dLong/2) * Math.sin(dLong/2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));
        double d = R * c;

        if(d > minimumRefreshDistance)
        {
            isUserMoving = true;
        }
        return d;

    }

}
