package com.bikeology.bikemaps;

import android.location.Location;
import android.util.Log;

import com.google.maps.model.DirectionsResult;
import com.google.maps.model.DirectionsRoute;

import static java.lang.Math.acos;
import static java.lang.Math.asin;
import static java.lang.Math.atan2;
import static java.lang.Math.cos;
import static java.lang.Math.sin;

public class StepInfo {
    private final static String TAG = "StepInfo";
    private int step;
    private double distance;
    private double bearing;

    public StepInfo(int step, double distance, double brng) {
        this.step = step;
        this.distance = distance;
        this.bearing = brng;
    }

    public int getStep() {
        return step;
    }

    public void setStep(int step) {
        this.step = step;
    }

    public double getDistance() {
        return distance;
    }

    public void setDistance(double distance) {
        this.distance = distance;
    }

    public double getBearing() {
        return bearing;
    }

    public void setBearing(double bearing) {
        this.bearing = bearing;
    }

    public static StepInfo getCurrentStep(Location location, DirectionsRoute directionsResult, UserLocation userLocation) {
        double perpDst = 99999999;
        int iStep=0;
        Log.d(TAG, "update camera valid : " + location);
        for(int i=0; i < directionsResult.legs[0].steps.length;i++){
            double lat1 = directionsResult.legs[0].steps[i].startLocation.lat; // point A from leg lat
            double lon1 = directionsResult.legs[0].steps[i].startLocation.lng;  // point A from leg lng

            double lat2 = directionsResult.legs[0].steps[i].endLocation.lat;   // point B from leg lat
            double lon2 = directionsResult.legs[0].steps[i].endLocation.lng;   // point B from leg lng

            double lat3 = userLocation.getGeo_point().getLatitude();
            double lon3 = userLocation.getGeo_point().getLongitude();

            double y = sin(lon3 - lon1) * cos(lat3);
            double x = cos(lat1) * sin(lat3) - sin(lat1) * cos(lat3) * cos(lat3 - lat1);
            double bearing1 = Math.toDegrees(atan2(y, x));
            bearing1 = 360 - ((bearing1 + 360) % 360);


            double y2 = sin(lon2 - lon1) * cos(lat2);
            double x2 = cos(lat1) * sin(lat2) - sin(lat1) * cos(lat2) * cos(lat2 - lat1);
            double bearing2 = Math.toDegrees(atan2(y2, x2));
            bearing2 = 360 - ((bearing2 + 360) % 360);

            double lat1Rads = Math.toRadians(lat1);
            double lat3Rads = Math.toRadians(lat3);
            double dLon = Math.toRadians(lon3 - lon1);

            double distanceAC = acos(sin(lat1Rads) * sin(lat3Rads)+cos(lat1Rads)*cos(lat3Rads)*cos(dLon)) * 6371;
            double minDst = Math.abs(asin(sin(distanceAC/6371)*sin(Math.toRadians(bearing1)-Math.toRadians(bearing2))) * 6371);
            if(minDst < perpDst ){
                perpDst = minDst;
                iStep = i;
            }
        }
        double lat1 = Math.toRadians(directionsResult.legs[0].steps[iStep].startLocation.lat); // point A from leg lat
        double lon1 = Math.toRadians(directionsResult.legs[0].steps[iStep].startLocation.lng);  // point A from leg lng

        double lat2 = Math.toRadians(directionsResult.legs[0].steps[iStep].endLocation.lat);   // point B from leg lat
        double lon2 = Math.toRadians(directionsResult.legs[0].steps[iStep].endLocation.lng);   // point B from leg lng

        Log.i(TAG, "lat1: " + lat1);
        Log.i(TAG, "lat2: " + lat2);
        Log.i(TAG, "long1: " + lon1);
        Log.i(TAG, "long2: " + lon2);

        double y = Math.sin(lon2 - lon1) * Math.cos(lat2);
        double x = Math.cos(lat1) * Math.sin(lat2) -
                Math.sin(lat1) * Math.cos(lat2) * Math.cos(lon2 - lon1);
        double brng = Math.toDegrees(atan2(y, x));
        Log.i(TAG, "step bearing: " + brng);
        Log.i(TAG, "distance to step: " + perpDst);
        return new StepInfo(iStep, perpDst, brng);
    }

}
