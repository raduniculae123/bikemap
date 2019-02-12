package com.bikeology.bikemaps;

import android.location.Location;

import com.google.android.gms.maps.model.LatLng;
import com.google.firebase.firestore.GeoPoint;
import com.google.firebase.firestore.ServerTimestamp;
import com.google.firebase.firestore.auth.User;

import java.util.Date;

public class UserLocation {

    private GeoPoint geo_point;
    private @ServerTimestamp
    Date timestamp;
    private String userId;
    private String userEmail;
    private float bearing;
    private int avgspeed=20;

    public UserLocation(GeoPoint geo_point, Date timestamp, String userId, String userEmail, int avgspeed) {
        this.geo_point = geo_point;
        this.timestamp = timestamp;
        this.userId = userId;
        this.userEmail = userEmail;
        this.avgspeed = avgspeed;

    }

    public UserLocation() {

    }

    public int getavgspeed() {
        return avgspeed;
    }

    public void setavgspeed(int avgspeed) {
        this.avgspeed = avgspeed;
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

    @Override
    public String toString() {
        return "UserLocation{" +
                "geo_point=" + geo_point +
                ", timestamp=" + timestamp +
                ", userId=" + userId +
                ", userEmail=" + userEmail +
                ", bearing=" + bearing +
                ", avgspeed=" + avgspeed +
                '}';
    }
}
