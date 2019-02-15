package com.bikeology.bikemaps;

import android.location.Location;
import android.os.Parcelable;

import com.google.android.gms.maps.model.LatLng;
import com.google.firebase.firestore.GeoPoint;
import com.google.firebase.firestore.ServerTimestamp;
import com.google.firebase.firestore.auth.User;

import java.util.Date;

public class UserLocation {

    private GeoPoint geo_point;
    private @ServerTimestamp Date timestamp;
    private String userId;
    private String userEmail;
    private float bearing;
    private long avgSpeed = 20;

    public UserLocation(GeoPoint geo_point, Date timestamp, String userId, String userEmail, long avgSpeed) {
        this.geo_point = geo_point;
        this.timestamp = timestamp;
        this.userId = userId;
        this.userEmail = userEmail;
        this.avgSpeed = avgSpeed;
    }

    public UserLocation() {

    }



    public GeoPoint getGeo_point() {
        return geo_point;
    }

    public void setGeo_point(GeoPoint geo_point) {
        this.geo_point = geo_point;
    }

    public Date getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Date timestamp) {
        this.timestamp = timestamp;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getUserEmail() {
        return userEmail;
    }

    public void setUserEmail(String userEmail) {
        this.userEmail = userEmail;
    }

    public float getBearing() {
        return bearing;
    }

    public void setBearing(float bearing) {
        this.bearing = bearing;
    }

    public long getAvgSpeed() {
        return avgSpeed;
    }

    public void setAvgSpeed(long avgSpeed) {
        this.avgSpeed = avgSpeed;
    }

    @Override
    public String toString() {
        return "UserLocation{" +
                "geo_point=" + geo_point +
                ", timestamp=" + timestamp +
                ", userId=" + userId +
                ", userEmail=" + userEmail +
                ", bearing=" + bearing +
                ", avgSpeed=" + avgSpeed +
                '}';
    }
}
