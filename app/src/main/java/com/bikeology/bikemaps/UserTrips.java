package com.bikeology.bikemaps;

public class UserTrips {

    private long avgSpeed;
    private double totalDistance;
    private String userEmail;


    public UserTrips(long avgSpeed, long totalDistance, String userEmail) {
        this.avgSpeed = avgSpeed;
        this.totalDistance = totalDistance;
        this.userEmail = userEmail;
    }

    public UserTrips() {

    }

    public long getAvgSpeed() {
        return avgSpeed;
    }

    public void setAvgSpeed(long avgSpeed) {
        this.avgSpeed = avgSpeed;
    }

    public double getTotalDistance() {
        return totalDistance;
    }

    public void setTotalDistance(double totalDistance) {
        this.totalDistance = totalDistance;
    }

    public String getUserEmail() {
        return userEmail;
    }

    public void setUserEmail(String userEmail) {
        this.userEmail = userEmail;
    }

    @Override
    public String toString() {
        return "UserTrips{" +
                "avgSpeed=" + avgSpeed +
                ", totalDistance=" + totalDistance +
                ", userEmail='" + userEmail + '\'' +
                '}';
    }
}
