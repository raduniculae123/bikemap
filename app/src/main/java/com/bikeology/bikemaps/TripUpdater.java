package com.bikeology.bikemaps;

import android.content.Context;
import android.location.Location;
import android.support.annotation.NonNull;
import android.util.Log;
import android.widget.Toast;


import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.GeoPoint;

import java.lang.Math;
import java.text.DecimalFormat;


import static android.support.constraint.Constraints.TAG;

public class TripUpdater {

    private UserTrips userTrips;
    private Context context;

    private GeoPoint oldLocation;

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
                                double d = calculateDistance(oldLocation, newLocation);
                                double acc = location.getAccuracy();
                                if(acc < 0.0001){
                                    acc = 0.0001;
                                }
                                else{
                                    acc /= 1000;
                                }
                                if(d > acc && !isUserMoving)
                                {
                                    isUserMoving = true;
                                }

                                if(d <= acc && isUserMoving){
                                    stopInterval++;
                                    if(stopInterval == 5){
                                        isUserMoving = false;
                                        stopInterval = 0;
                                        stopCurrentTrip();
                                    }
                                }

                                if(isUserMoving) {
                                    if (moveInterval == 0) {
                                        startTime = System.currentTimeMillis() / 1000;
                                    }
                                    moveInterval++;
                                    intermediateDistance += d;
                                    oldLocation = newLocation;
                                    if (moveInterval % 50 == 0) {
                                        updateAverageSpeed();
                                        startTime = System.currentTimeMillis() / 1000;
                                    }
                                    tripsRef.set(userTrips);
                                }
                                DecimalFormat df2 = new DecimalFormat(".####");
                                Log.d(TAG, "moving: " + isUserMoving + " d=" + d + "m: " + moveInterval + "s: " + stopInterval + "acc: " + acc);
                                /*Toast.makeText(context.getApplicationContext(), "moving: " +
                                        isUserMoving + " d=" +
                                        df2.format(d) + " m: " +
                                        moveInterval + " s: " +
                                        stopInterval + " acc: " +
                                        df2.format(acc), Toast.LENGTH_LONG).show();*/
                            }
                        }
                        return;
                    }

                }
            }
        });
    }

    private void stopCurrentTrip() {
        updateAverageSpeed();
        intermediateDistance = 0;
        moveInterval = 0;
    }

    private void updateAverageSpeed() {
        endTime = System.currentTimeMillis()/1000;
        tripTime = endTime - startTime;
        avgSpeed = (intermediateDistance*1000)/(tripTime*3600);
        if(avgSpeed>=5 && avgSpeed<=50)
        {
            userTrips.setAvgSpeed((userTrips.getAvgSpeed() + avgSpeed)/2);
            updateTotalDistance();
        }
    }

    private void updateTotalDistance() {
        userTrips.setTotalDistance(userTrips.getTotalDistance() + intermediateDistance);
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

        return d;

    }

}
