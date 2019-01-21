package com.bikeology.bikemaps;

import com.google.firebase.firestore.GeoPoint;
import com.google.firebase.firestore.ServerTimestamp;

import java.util.Date;

public class UserLocation {

    private GeoPoint geo_point;
    private @ServerTimestamp Date timestamp;
    private String userId;

    public UserLocation(GeoPoint geo_point, Date timestamp, String userId) {
        this.geo_point = geo_point;
        this.timestamp = timestamp;
        this.userId = userId;
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

    @Override
    public String toString() {
        return "UserLocation{" +
                "geo_point=" + geo_point +
                ", timestamp=" + timestamp +
                ", userId=" + userId +
                '}';
    }
}
