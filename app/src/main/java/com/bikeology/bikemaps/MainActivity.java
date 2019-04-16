package com.bikeology.bikemaps;

import android.Manifest;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.constraint.ConstraintLayout;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.CardView;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import com.bikeology.bikemaps.AccountActivity.LoginActivity;
import com.bikeology.bikemaps.services.LocationService;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.ui.PlaceAutocompleteFragment;
import com.google.android.gms.location.places.ui.PlaceSelectionListener;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.GeoPoint;
import com.google.maps.DirectionsApiRequest;
import com.google.maps.GeoApiContext;
import com.google.maps.PendingResult;
import com.google.maps.internal.PolylineEncoding;
import com.google.maps.model.DirectionsResult;
import com.google.maps.model.DirectionsRoute;


import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import static com.bikeology.bikemaps.Constants.ERROR_DIALOG_REQUEST;
import static com.bikeology.bikemaps.Constants.MAPVIEW_BUNDLE_KEY;
import static com.bikeology.bikemaps.Constants.PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION;
import static com.bikeology.bikemaps.Constants.PERMISSIONS_REQUEST_ENABLE_GPS;

public class MainActivity extends BaseActivity implements OnMapReadyCallback {

    //FINAL VALUES
    private static final String TAG = "MainActivity";
    private static final float DEFAULT_ZOOM = 15f;


    //LOCATION
    private boolean mLocationPermissionGranted = false;
    private FusedLocationProviderClient mFusedLocationClient;
    private GeoApiContext mGeoApiContext = null;


    //MAP
    private MapView mMapView;
    public static GoogleMap googleMap;
    private Marker marker;
    private Polyline mOldPolyline;
    private Polyline mNewPolyline;
    private Polyline m1Polyline;
    private Button button_recenter;
    private LatLngBounds mRouteBounds;


    //FIREBASE
    private UserLocation mUserLocation;
    private UserTrips mUserTrips;
    private FirebaseFirestore mDb;

    //PLACE
    private PlaceInfo Place;
    private Place mPlace;
    boolean isWebsiteValid = true;

    //SEARCH BAR
    private PlaceAutocompleteFragment autocompleteFragment;
    private CardView searchCard;

    //INFO CARD
    private CardView infoCard;

    private TextView infoTextName;
    private TextView infoTextAddress;
    private TextView ratingText;
    private TextView ratingValue;

    private Button button_fastrt;
    private Button button_joyrt;
    private Button button_website;
    private Button button_phone;

    //NAV CARD
    private ConstraintLayout navCardLayout;
    private CardView navCard;
    private RatingBar ratingBar;
    private TextView navText;

    private Button button_nav_yes;
    private Button button_nav_cancel;

    private ProgressBar calculateRouteProgressBar;

    //NAVIGATION MODE
    private CardView etaCard;
    private BroadcastReceiver locationReceiver;
    private Button button_endtrip;
    private TextView avgSpeedTxt;
    private TextView durationTxt;
    private int currentStep = -1;

    //BOOLEANS
    private boolean isRouteCalculated = false;
    private boolean navYes = false;
    private boolean focusOnMarker = true;
    private boolean refreshRoute = false;


    // avg speed calculator

    private long durationLong;
    private double shrtDst;
    private TextView distanceTextView;
    private TextView durationTextView;
    private long tripDuration;

    // leg info
    DirectionsResult directionsResult;
    int resultShortestRoute;

    //avg speed updater
    private long startTime;
    private long endTime;
    private long tripSpeed;

    //DEBUG
    private TextView debugText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setupDrawer();
        setRequestedOrientation (ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        // FIND BY ID

        // SEARCH BAR
        autocompleteFragment = (PlaceAutocompleteFragment)
                getFragmentManager().findFragmentById(R.id.place_autocomplete_fragment);
        searchCard = findViewById(R.id.searchCard);

        // INFO CARD -------------------------------
        infoCard = findViewById(R.id.card_info);
        infoCard.setVisibility(View.GONE);

        // info text
        infoTextName = findViewById(R.id.text_place_name);
        infoTextAddress = findViewById(R.id.text_place_address);
        ratingText = findViewById(R.id.ratingText);
        ratingValue = findViewById(R.id.ratingValue);

        // info buttons
        button_website = findViewById(R.id.button_website);
        button_phone = findViewById(R.id.button_phone);
        button_fastrt = findViewById(R.id.btn_fastrt);
        button_joyrt = findViewById(R.id.btn_joyrt);

        // info rating
        ratingBar = findViewById(R.id.ratingBar);
        ratingBar.setNumStars(5);
        ratingBar.setIsIndicator(true);
        ratingBar.setMax(5);
        ratingBar.setStepSize(0.1f);

        //------------------------------------------


        // NAV CARD --------------------------------
        navCard = findViewById(R.id.card_navigate);
        navCard.setVisibility(View.GONE);

        navCardLayout = findViewById(R.id.navCardLayout);

        // nav text
        navText = findViewById(R.id.text_nav_to);
        navText.setVisibility(View.GONE);

        //nav buttons
        button_nav_yes = findViewById(R.id.button_nav_yes);
        button_nav_cancel = findViewById(R.id.button_nav_cancel);
        button_nav_yes.setVisibility(View.GONE);
        button_nav_cancel.setVisibility(View.GONE);


        // progress bar
        calculateRouteProgressBar = findViewById(R.id.calculateRoutePregressBar);
        calculateRouteProgressBar.setVisibility(View.GONE);
        //------------------------------------------

        // recenter button
        button_recenter = findViewById(R.id.button_recenter);

        // eta card
        etaCard = findViewById(R.id.card_eta);
        // end trip button
        button_endtrip = findViewById(R.id.endtrip);
        button_endtrip.setVisibility(View.GONE);

        //trip distance
        distanceTextView = findViewById(R.id.distanceTextView);
        distanceTextView.setVisibility(View.GONE);

        // trip duration
        durationTextView = findViewById(R.id.durationTextView);
        durationTextView.setVisibility(View.GONE);

        // avg speed textview
        avgSpeedTxt = findViewById(R.id.average_speed1);
        avgSpeedTxt.setVisibility(View.GONE);

        // duration textview
        durationTxt = findViewById(R.id.durationTextView1);
        durationTxt.setVisibility(View.GONE);

        //DEBUG
        debugText = findViewById(R.id.debugText);


        // delay of 2 seconds at onCreate
        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {

            @Override
            public void run() {

                LatLng myLatLng = new LatLng(mUserLocation.getGeo_point().getLatitude(),
                        mUserLocation.getGeo_point().getLongitude());
                googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(myLatLng, 15));

            }

        }, 2000);


        // calculate route button
        button_fastrt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                calculateRouteProgressBar.setVisibility(View.VISIBLE);
                refreshRoute = true;
                infoCard.setVisibility(View.GONE);
                button_recenter.setVisibility(View.GONE);
                navCard.setVisibility(View.VISIBLE);
                searchCard.setVisibility(View.GONE);
                navText.setText("Navigate to " + mPlace.getName() + "?");
                isRouteCalculated = true;
              //  googleMap.animateCamera(CameraUpdateFactory.newLatLngBounds(mRouteBounds, 100));


            }
        });

        // joy route button
        button_joyrt.setVisibility(View.GONE);

        // nav yes button
        button_nav_yes.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                etaCard.setVisibility(View.VISIBLE);
                button_endtrip.setVisibility(View.VISIBLE);
                button_recenter.setVisibility(View.GONE);
                LatLng myLatLng = new LatLng(mUserLocation.getGeo_point().getLatitude(),
                        mUserLocation.getGeo_point().getLongitude());
                navYes = true;
                Log.d(TAG, "navYes true");

                startTime = System.currentTimeMillis()/1000;

                mNewPolyline.setColor(Color.rgb(2, 113, 102));
                mNewPolyline.setWidth(30);

                navCard.setVisibility(View.GONE);
                searchCard.setVisibility(View.GONE);

                CameraPosition povCamera = new CameraPosition.Builder()
                        .target(myLatLng)
                        .zoom(25)
                        .bearing(mUserLocation.getBearing())
                        .tilt(60)
                        .build();
              googleMap.animateCamera(CameraUpdateFactory.newCameraPosition(povCamera));


            }
        });

        // nav cancel button
        button_nav_cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                infoCard.setVisibility(View.VISIBLE);
                navCard.setVisibility(View.GONE);
                button_recenter.setVisibility(View.VISIBLE);

                button_nav_yes.setVisibility(View.GONE);
                button_nav_cancel.setVisibility(View.GONE);
                searchCard.setVisibility(View.VISIBLE);
                navText.clearComposingText();
                navText.setVisibility(View.GONE);
                distanceTextView.setVisibility(View.GONE);
                durationTextView.setVisibility(View.GONE);
                googleMap.clear();
                LatLng latLng = mPlace.getLatLng();
                googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, DEFAULT_ZOOM));
                MarkerOptions options = new MarkerOptions()
                        .position(latLng)
                        .title(mPlace.getName().toString());
                marker = googleMap.addMarker(options);
                googleMap.addMarker(options);
                button_fastrt.setVisibility(View.VISIBLE);
                isRouteCalculated = false;
                navYes = false;
            }
        });

        // recenter button
        button_recenter.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                LatLng focusLatLng;
                LatLng myLatLng = new LatLng(mUserLocation.getGeo_point().getLatitude(),
                        mUserLocation.getGeo_point().getLongitude());
                if(marker != null){
                    LatLng markerLatLng = marker.getPosition();
                    if(focusOnMarker){
                        focusOnMarker = false;
                        focusLatLng = myLatLng;
                        button_recenter.setBackgroundResource(R.drawable.marker_center);
                    }
                    else{
                        focusOnMarker = true;
                        focusLatLng = markerLatLng;
                        button_recenter.setBackgroundResource(R.drawable.location_center);
                    }

                }
                else{
                    focusLatLng = myLatLng;
                }
                if (navYes) {
                    CameraPosition navigationCamera = new CameraPosition.Builder()
                            .target(myLatLng)
                            .zoom(25)
                            .bearing(mUserLocation.getBearing())
                            .tilt(30)
                            .build();

                    googleMap.animateCamera(CameraUpdateFactory.newCameraPosition(navigationCamera));
                }
                else {
                    CameraPosition centeredCamera = new CameraPosition.Builder()
                            .target(focusLatLng)
                            .zoom(DEFAULT_ZOOM)
                            .bearing(0)
                            .tilt(0)
                            .build();

                    googleMap.animateCamera(CameraUpdateFactory.newCameraPosition(centeredCamera));
                }
                if (button_fastrt.getVisibility() == View.VISIBLE) {
                    button_fastrt.setVisibility(View.VISIBLE);
                } else button_fastrt.setVisibility(View.GONE);

            }
        });

        // end trip button
        button_endtrip.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                infoCard.setVisibility(View.VISIBLE);
                navCard.setVisibility(View.GONE);
                distanceTextView.setVisibility(View.GONE);
                durationTextView.setVisibility(View.GONE);
                searchCard.setVisibility(View.VISIBLE);
                navText.clearComposingText();
                googleMap.clear();
                mUserLocation.setBearing(0);
                LatLng latLng = mPlace.getLatLng();
                CameraPosition centeredCamera = new CameraPosition.Builder()
                        .target(latLng)
                        .zoom(DEFAULT_ZOOM)
                        .bearing(mUserLocation.getBearing())
                        .tilt(0)
                        .build();

                googleMap.animateCamera(CameraUpdateFactory.newCameraPosition(centeredCamera));

                endTime = System.currentTimeMillis()/1000;
                tripDuration = (endTime - startTime);

                Log.d(TAG, "aaatrip duration" + tripDuration);

                Log.d(TAG, "aaashrtdst" + shrtDst);

                tripSpeed=(long)(shrtDst*1000)/(tripDuration*3600);


                Log.d(TAG, "aaatrip speed" + tripSpeed);

                Log.d(TAG, "aaaavgspd speed" + mUserLocation.getAvgSpeed());
                mUserLocation.setAvgSpeed((mUserLocation.getAvgSpeed()+tripSpeed)/2);

                //UPLOAD FIREBASE AVGSPEED

                Log.d(TAG, "aaasetavgspd" + (mUserLocation.getAvgSpeed()+tripSpeed)/2);


                MarkerOptions options = new MarkerOptions()
                        .position(latLng)
                        .title(mPlace.getName().toString());
                marker = googleMap.addMarker(options);
                googleMap.addMarker(options);
                button_fastrt.setVisibility(View.VISIBLE);
                etaCard.setVisibility(View.VISIBLE);
                button_endtrip.setVisibility(View.GONE);
                button_recenter.setVisibility(View.VISIBLE);
                distanceTextView.setVisibility(View.GONE);
                durationTextView.setVisibility(View.GONE);
                isRouteCalculated = false;
                navYes = false;

            }
        });

        // navigation mode camera updater
        locationReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {

                Log.d(TAG, "refreshRoute: " + refreshRoute);
                if(refreshRoute){
                    calculateDirections(marker);
                    refreshRoute = false;
                }

                if(!navYes)
                    return;
                Location location = intent.getExtras().getParcelable("location");
                LatLng myLatLng = new LatLng(location.getLatitude(),
                        location.getLongitude());
                GeoPoint geoPoint = new GeoPoint(location.getLatitude(), location.getLongitude());
                mUserLocation.setGeo_point(geoPoint);
                StepInfo step = StepInfo.getCurrentStep(location, directionsResult.routes[resultShortestRoute], mUserLocation);
                double brng = step.getBearing();

                if(currentStep != -1 && currentStep != step.getStep()){
                    refreshRoute = true;
                }
                if(step.getDistance() >= 50){
                    refreshRoute = true;
                }
                currentStep = step.getStep();
                debugText.setText("Current step: " + currentStep);

                mUserLocation.setBearing((float)brng);

                durationLong = (long)shrtDst/(((long)mUserTrips.getAvgSpeed()*1000)/60);
                distanceTextView.setText("Trip distance: " + String.format("%.2f",shrtDst/1000) + " km");
                int hours = 0;
                while(durationLong > 60){
                    hours++;
                    durationLong -= 60;
                }
                if(hours > 0){
                    durationTextView.setText("Trip duration: " + hours + "h" + durationLong + " minutes");
                }
                else {
                    durationTextView.setText("Trip duration: " + durationLong + " minutes");
                }
                SimpleDateFormat sdf = new SimpleDateFormat("HH:mm");
                Date eta = new Date();
                Calendar calendar = Calendar.getInstance();
                calendar.setTime(eta);
                calendar.add(Calendar.MINUTE, (int)durationLong);


                avgSpeedTxt.setText(String.format("%.0f", mUserTrips.getAvgSpeed()) + " km/h");
                avgSpeedTxt.setVisibility(View.VISIBLE);
                durationTxt.setText("ETA \n" + sdf.format(calendar.getTime()));
                durationTxt.setVisibility(View.VISIBLE);

                CameraPosition povCamera = new CameraPosition.Builder()
                        .target(myLatLng)
                        .zoom(25)
                        .bearing((float)brng)
                        .tilt(60)
                        .build();
                googleMap.animateCamera(CameraUpdateFactory.newCameraPosition(povCamera));
                Log.i(TAG, "bearing=" + mUserLocation.getBearing());


                /*calculateDirections1(marker);
                mOldPolyline.setVisible(false);
                calculateDirections2(marker);
                m1Polyline.setVisible(false);*/


            }
        };
        getApplicationContext().registerReceiver(locationReceiver, new IntentFilter("BM_Location"));


        initGoogleMap(savedInstanceState);
        mDb = FirebaseFirestore.getInstance();
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        if(FirebaseAuth.getInstance().getCurrentUser() == null) {
            Intent intent = new Intent(MainActivity.this, LoginActivity.class);
        startActivity(intent);
        }


        // SEARCH BAR

        autocompleteFragment.setOnPlaceSelectedListener(new PlaceSelectionListener() {
            @Override
            public void onPlaceSelected(Place place) {

                googleMap.clear();
                mPlace = place;
                Log.i(TAG, "Place: " + mPlace.getName());
                googleMap.setInfoWindowAdapter(new CustomInfoWindowAdapter(MainActivity.this));
                LatLng latLng = mPlace.getLatLng();
                googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, DEFAULT_ZOOM));
                String snippet = "Address: " + mPlace.getAddress() + "\n" +
                        "Phone Number: " + mPlace.getPhoneNumber() + "\n" +
                        "Website: " + mPlace.getWebsiteUri() + "\n" +
                        "Price Rating: " + mPlace.getRating() + "\n";
                MarkerOptions options = new MarkerOptions()
                        .position(latLng)
                        .title(mPlace.getName().toString())
                        .snippet(snippet);
                marker = googleMap.addMarker(options);
                googleMap.addMarker(options);
                isRouteCalculated = false;
                mOldPolyline = null;
                mNewPolyline = null;
                infoTextName.setText(mPlace.getName());
                infoTextAddress.setText(mPlace.getAddress());
                if(mPlace.getRating()<0)
                {
                    ratingBar.setVisibility(View.GONE);
                    ratingValue.setVisibility(View.GONE);
                    ratingText.setVisibility(View.GONE);
                    button_website.setVisibility(View.GONE);
                    button_phone.setVisibility(View.GONE);
                }
                else{
                    ratingBar.setVisibility(View.VISIBLE);
                    ratingValue.setVisibility(View.VISIBLE);
                    ratingText.setVisibility(View.VISIBLE);
                    button_website.setVisibility(View.VISIBLE);
                    button_phone.setVisibility(View.VISIBLE);

                    ratingBar.setRating(mPlace.getRating());
                    ratingValue.setText(String.valueOf(mPlace.getRating()));
                    Log.d(TAG, "rating: " + mPlace.getRating());

                }
                infoCard.setVisibility(View.VISIBLE);
                button_fastrt.setVisibility(View.VISIBLE);
                // button_joyrt.setVisibility(View.VISIBLE);

                /*final Uri website;
                website = place.getWebsiteUri();
                isWebsiteValid = true;
                try {
                    String web_string = website.toString();
                    Log.d(TAG, "website uri string: " + web_string);
                }
                catch (NullPointerException e){
                    Log.d(TAG, "website uri string: null");
                    isWebsiteValid = false;

                }

                    button_website.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            if(isWebsiteValid){
                                startActivity(new Intent(Intent.ACTION_VIEW, website));
                            }
                        }
                    });
                    button_phone.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        String phone;
                        phone = mPlace.getPhoneNumber().toString();
                        Intent phoneIntent = new Intent(Intent.ACTION_DIAL);
                        phoneIntent.setData(Uri.parse("tel:"+phone));
                        startActivity(phoneIntent);
                    }
                });*/


            }

            @Override
            public void onError(Status status) {
                // TODO: Handle the error.
                Log.i(TAG, "An error occurred: " + status);
            }
        });

        // clear button
        autocompleteFragment.getView().findViewById(R.id.place_autocomplete_clear_button)
                .setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        etaCard.setVisibility(View.GONE);
                        button_endtrip.setVisibility(View.GONE);
                        googleMap.clear();
                        marker.remove();
                        marker = null;
                        button_recenter.setBackgroundResource(R.drawable.location_center);
                        infoTextName.clearComposingText();
                        infoTextAddress.clearComposingText();
                        infoCard.setVisibility(View.GONE);
                        autocompleteFragment.setText("");
                        button_joyrt.setVisibility(View.GONE);
                        button_fastrt.setVisibility(View.GONE);
                        navCard.setVisibility(View.GONE);
                        navText.clearComposingText();
                        if (isRouteCalculated) {
                            LatLng myLatLng = new LatLng(mUserLocation.getGeo_point().getLatitude(),
                                    mUserLocation.getGeo_point().getLongitude());
                            googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(myLatLng, 15));
                        }
                        navYes = false;

                    }
                });

    }

    private void addPolylinesToMap(final DirectionsResult result, final int shortestRoute) {
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                Log.d(TAG, "run: result routes: " + result.routes.length);

                if (shortestRoute == -1) {
                    // Log error
                    return;
                }
                DirectionsRoute route = result.routes[shortestRoute];
                //for(DirectionsRoute route: result.routes){
                Log.d(TAG, "run: leg: " + route.legs[0].toString());
                List<com.google.maps.model.LatLng> decodedPath = PolylineEncoding.decode(route.overviewPolyline.getEncodedPath());

                List<LatLng> newDecodedPath = new ArrayList<>();

                // This loops through all the LatLng coordinates of ONE polyline.
                for (com.google.maps.model.LatLng latLng : decodedPath) {

//                        Log.d(TAG, "run: latlng: " + latLng.toString());

                    newDecodedPath.add(new LatLng(
                            latLng.lat,
                            latLng.lng
                    ));
                }
                mNewPolyline = googleMap.addPolyline(new PolylineOptions().addAll(newDecodedPath));
                mNewPolyline.setColor(-7829368);
                mNewPolyline.setClickable(false);

                final DocumentReference tripsRef = FirebaseFirestore.getInstance()
                        .collection(getString(R.string.collection_user_trips))
                        .document(FirebaseAuth.getInstance().getUid());
                tripsRef.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                        if (task.isSuccessful()) {
                            DocumentSnapshot document = task.getResult();
                            if (document != null) {
                                calculateRouteProgressBar.setVisibility(View.GONE);
                                button_nav_yes.setVisibility(View.VISIBLE);
                                button_nav_cancel.setVisibility(View.VISIBLE);
                                navText.setVisibility(View.VISIBLE);

                                mUserTrips = document.toObject(UserTrips.class);
                                durationLong = (long)shrtDst/(long)((mUserTrips.getAvgSpeed()*1000)/60);
                                distanceTextView.setText("Trip distance: " + shrtDst/1000 + " km");
                                distanceTextView.setVisibility(View.VISIBLE);
                                distanceTextView.bringToFront();

                                int hours = 0;
                                while(durationLong > 60){
                                    hours++;
                                    durationLong -= 60;
                                }
                                if(hours > 0){
                                    durationTextView.setText("Trip duration: " + hours + "h" + durationLong + " minutes");
                                }
                                else {
                                    durationTextView.setText("Trip duration: " + durationLong + " minutes");
                                }                                durationTextView.setVisibility(View.VISIBLE);
                                durationTextView.bringToFront();
                                mRouteBounds = getRouteBounds(result, shortestRoute);
                                googleMap.animateCamera(CameraUpdateFactory.newLatLngBounds(mRouteBounds, 100));


                                button_fastrt.setVisibility(View.GONE);
                                button_joyrt.setVisibility(View.GONE);


                            }
                        }
                    }

                });
                Log.d(TAG, "shrtDst: " + shrtDst);
                Log.d(TAG, "avgSpeed: " + mUserLocation.getAvgSpeed());


                if(shrtDst<200)
                {
                    infoCard.setVisibility(View.VISIBLE);
                    navCard.setVisibility(View.GONE);
                    durationTextView.setVisibility(View.GONE);
                    distanceTextView.setVisibility(View.GONE);
                    searchCard.setVisibility(View.VISIBLE);
                    navText.clearComposingText();
                    googleMap.clear();
                    mUserLocation.setBearing(0);
                    LatLng latLng = mPlace.getLatLng();
                    CameraPosition centeredCamera = new CameraPosition.Builder()
                            .target(latLng)
                            .zoom(DEFAULT_ZOOM)
                            .bearing(mUserLocation.getBearing())
                            .tilt(0)
                            .build();

                    googleMap.animateCamera(CameraUpdateFactory.newCameraPosition(centeredCamera));

                    endTime = System.currentTimeMillis()/1000;
                    tripDuration = (endTime - startTime);

                    Log.d(TAG, "aaatrip duration" + tripDuration);

                    Log.d(TAG, "aaashrtdst" + shrtDst);

                    tripSpeed=(long)(shrtDst*1000)/(tripDuration*3600);


                    Log.d(TAG, "aaatrip speed" + tripSpeed);

                    Log.d(TAG, "aaaavgspd speed" + mUserLocation.getAvgSpeed());
                    mUserLocation.setAvgSpeed((mUserLocation.getAvgSpeed()+tripSpeed)/2);

                    //UPLOAD FIREBASE AVGSPEED

                    Log.d(TAG, "aaasetavgspd" + (mUserLocation.getAvgSpeed()+tripSpeed)/2);


                    MarkerOptions options = new MarkerOptions()
                            .position(latLng)
                            .title(mPlace.getName().toString());
                    marker = googleMap.addMarker(options);
                    googleMap.addMarker(options);
                    button_fastrt.setVisibility(View.VISIBLE);
                    etaCard.setVisibility(View.GONE);
                    button_endtrip.setVisibility(View.GONE);
                    button_recenter.setVisibility(View.VISIBLE);
                    durationTextView.setVisibility(View.GONE);
                    distanceTextView.setVisibility(View.GONE);
                    isRouteCalculated = false;
                    navYes = false;

                }


            }
        });
    }


    private void refreshPolylinesToMap(final DirectionsResult result, final int shortestRoute) {
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                Log.d(TAG, "run: result routes: " + result.routes.length);

                if (shortestRoute == -1) {
                    // Log error
                    return;
                }
                DirectionsRoute route = result.routes[shortestRoute];
                //for(DirectionsRoute route: result.routes){
                Log.d(TAG, "run: leg: " + route.legs[0].toString());
                List<com.google.maps.model.LatLng> decodedPath = PolylineEncoding.decode(route.overviewPolyline.getEncodedPath());

                List<LatLng> newDecodedPath = new ArrayList<>();

                // This loops through all the LatLng coordinates of ONE polyline.
                for (com.google.maps.model.LatLng latLng : decodedPath) {

//                        Log.d(TAG, "run: latlng: " + latLng.toString());

                    newDecodedPath.add(new LatLng(
                            latLng.lat,
                            latLng.lng
                    ));
                }

                mOldPolyline = mNewPolyline;
                mNewPolyline = googleMap.addPolyline(new PolylineOptions().addAll(newDecodedPath));
                mNewPolyline.setColor(Color.rgb(2, 113, 102));
                mNewPolyline.setWidth(30);
                mNewPolyline.setClickable(false);
                if(mOldPolyline != null){
                    mOldPolyline.setVisible(false);
                }

                if (shrtDst < 200) {
                    infoCard.setVisibility(View.VISIBLE);
                    navCard.setVisibility(View.GONE);
                    durationTextView.setVisibility(View.GONE);
                    searchCard.setVisibility(View.VISIBLE);
                    navText.clearComposingText();
                    durationTextView.setVisibility(View.GONE);
                    googleMap.clear();
                    mUserLocation.setBearing(0);
                    LatLng latLng = mPlace.getLatLng();
                    CameraPosition centeredCamera = new CameraPosition.Builder()
                            .target(latLng)
                            .zoom(DEFAULT_ZOOM)
                            .bearing(mUserLocation.getBearing())
                            .tilt(0)
                            .build();

                    googleMap.animateCamera(CameraUpdateFactory.newCameraPosition(centeredCamera));

                    endTime = System.currentTimeMillis() / 1000;
                    tripDuration = (endTime - startTime);
                    MarkerOptions options = new MarkerOptions()
                            .position(latLng)
                            .title(mPlace.getName().toString());
                    marker = googleMap.addMarker(options);
                    googleMap.addMarker(options);
                    button_fastrt.setVisibility(View.VISIBLE);
                    etaCard.setVisibility(View.GONE);
                    button_endtrip.setVisibility(View.GONE);
                    button_recenter.setVisibility(View.VISIBLE);
                    durationTextView.setVisibility(View.GONE);
                    isRouteCalculated = false;
                    navYes = false;


                }
            }
        });
    }
    /*

    private void addPolylinesToMap2(final DirectionsResult result, final int shortestRoute) {
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                Log.d(TAG, "run: result routes: " + result.routes.length);

                if (shortestRoute == -1) {
                    // Log error
                    return;
                }
                DirectionsRoute route = result.routes[shortestRoute];
                //for(DirectionsRoute route: result.routes){
                Log.d(TAG, "run: leg: " + route.legs[0].toString());
                List<com.google.maps.model.LatLng> decodedPath = PolylineEncoding.decode(route.overviewPolyline.getEncodedPath());

                List<LatLng> newDecodedPath = new ArrayList<>();

                // This loops through all the LatLng coordinates of ONE polyline.
                for (com.google.maps.model.LatLng latLng : decodedPath) {

//                        Log.d(TAG, "run: latlng: " + latLng.toString());

                    newDecodedPath.add(new LatLng(
                            latLng.lat,
                            latLng.lng
                    ));
                }

                mNewPolyline.setVisible(true);
                mNewPolyline = googleMap.addPolyline(new PolylineOptions().addAll(newDecodedPath));
                mNewPolyline.setColor(Color.rgb(2, 113, 102));
                mNewPolyline.setWidth(30);
                mNewPolyline.setClickable(false);

                durationLong = shrtDst/((mUserLocation.getAvgSpeed()*1000)/60);

                Log.d(TAG, "shrtDst: " + shrtDst);
                Log.d(TAG, "avgSpeed: " + mUserLocation.getAvgSpeed());

                if(shrtDst<2000)
                {
                    infoCard.setVisibility(View.VISIBLE);
                    navCard.setVisibility(View.GONE);
                    durationTextView.setVisibility(View.GONE);
                    searchCard.setVisibility(View.VISIBLE);
                    navText.clearComposingText();
                    durationTextView.setVisibility(View.GONE);
                    googleMap.clear();
                    mUserLocation.setBearing(0);
                    LatLng latLng = mPlace.getLatLng();
                    CameraPosition centeredCamera = new CameraPosition.Builder()
                            .target(latLng)
                            .zoom(DEFAULT_ZOOM)
                            .bearing(mUserLocation.getBearing())
                            .tilt(0)
                            .build();

                    googleMap.animateCamera(CameraUpdateFactory.newCameraPosition(centeredCamera));

                    endTime = System.currentTimeMillis()/1000;
                    tripDuration = (endTime - startTime);

                    Log.d(TAG, "aaatrip duration" + tripDuration);

                    Log.d(TAG, "aaashrtdst" + shrtDst);

                    tripSpeed=(shrtDst*1000)/(tripDuration*3600);


                    Log.d(TAG, "aaatrip speed" + tripSpeed);

                    Log.d(TAG, "aaaavgspd speed" + mUserLocation.getAvgSpeed());
                    mUserLocation.setAvgSpeed((mUserLocation.getAvgSpeed()+tripSpeed)/2);

                    //UPLOAD FIREBASE AVGSPEED

                    Log.d(TAG, "aaasetavgspd" + (mUserLocation.getAvgSpeed()+tripSpeed)/2);


                    MarkerOptions options = new MarkerOptions()
                            .position(latLng)
                            .title(mPlace.getName().toString());
                    marker = googleMap.addMarker(options);
                    googleMap.addMarker(options);
                    button_fastrt.setVisibility(View.VISIBLE);
                    button_endtrip.setVisibility(View.GONE);
                    button_recenter.setVisibility(View.VISIBLE);
                    durationTextView.setVisibility(View.GONE);
                    isRouteCalculated = false;
                    navYes = false;

                }

            }
        });
    }

*/

    private void saveUserLocation() {

        if (FirebaseAuth.getInstance().getUid() == null) {
            return;
        }

        DocumentReference locationRef = mDb.collection(getString(R.string.collection_user_locations))
                .document(FirebaseAuth.getInstance().getUid());
        locationRef.set(mUserLocation).addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                if (task.isSuccessful()) {
                    Log.d(TAG, "saveUserLocation: \ninserted user location into database." +
                            "\n latitude: " + mUserLocation.getGeo_point().getLatitude() +
                            "\n longitude: " + mUserLocation.getGeo_point().getLongitude());
                }
            }
        });
    }

    private void getLastKnownLocation() {
        Log.d(TAG, "getLastKnownLocation: called.");
        if (ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        mFusedLocationClient.getLastLocation().addOnCompleteListener(new OnCompleteListener<Location>() {
            @Override
            public void onComplete(@NonNull Task<Location> task) {
                if (task.isSuccessful()) {
                    Location location = task.getResult();
                    GeoPoint geoPoint = new GeoPoint(location.getLatitude(), location.getLongitude());
                    Log.d(TAG, "onComplete: latitude" + geoPoint.getLatitude());
                    Log.d(TAG, "onComplete: longitude" + geoPoint.getLongitude());

                    if (mUserLocation == null) {
                        mUserLocation = new UserLocation();
                        mUserLocation.setUserId(FirebaseAuth.getInstance().getUid());
                    }
                    if (FirebaseAuth.getInstance().getCurrentUser() != null) {
                        mUserLocation.setUserEmail(FirebaseAuth.getInstance().getCurrentUser().getEmail());
                    }
                    mUserLocation.setGeo_point(geoPoint);
                    mUserLocation.setTimestamp(null);
                    mUserLocation.setBearing(location.getBearing());
                    mUserLocation.setAvgSpeed(mUserLocation.getAvgSpeed());
                    Log.d(TAG, "avgspped" + mUserLocation.getAvgSpeed());


                    saveUserLocation();
                    startLocationService();
                }
            }
        });
    }

    private void initGoogleMap(Bundle savedInstanceState) {
        // *** IMPORTANT ***
        // MapView requires that the Bundle you pass contain _ONLY_ MapView SDK
        // objects or sub-Bundles.
        Bundle mapViewBundle = null;
        if (savedInstanceState != null) {
            mapViewBundle = savedInstanceState.getBundle(MAPVIEW_BUNDLE_KEY);
        }
        mMapView = findViewById(R.id.mapView);
        mMapView.onCreate(mapViewBundle);

        mMapView.getMapAsync(this);


        if (mGeoApiContext == null) {

            mGeoApiContext = new GeoApiContext.Builder()
                    .apiKey(getString(R.string.google_maps_api_key))
                    .build();

        }




    }

    private boolean checkMapServices() {
        if (isServicesOK()) {
            return isMapsEnabled();
        }
        return false;
    }

    private void buildAlertMessageNoGps() {
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage("This application requires GPS to work properly, do you want to enable it?")
                .setCancelable(false)
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    public void onClick(@SuppressWarnings("unused") final DialogInterface dialog, @SuppressWarnings("unused") final int id) {
                        Intent enableGpsIntent = new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                        startActivityForResult(enableGpsIntent, PERMISSIONS_REQUEST_ENABLE_GPS);
                    }
                });
        final AlertDialog alert = builder.create();
        alert.show();
    }

    public boolean isMapsEnabled() {
        final LocationManager manager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        if (!manager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            buildAlertMessageNoGps();
            return false;
        }
        return true;
    }
    private void getLocationPermission() {
        /*
         * Request location permission, so that we can get the location of the
         * device. The result of the permission request is handled by a callback,
         * onRequestPermissionsResult.
         */
        if (ContextCompat.checkSelfPermission(this.getApplicationContext(),
                android.Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            mLocationPermissionGranted = true;
            getLastKnownLocation();
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},
                    PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION);
        }
    }

    public boolean isServicesOK() {
        Log.d(TAG, "isServicesOK: checking google services version");

        int available = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(MainActivity.this);

        if (available == ConnectionResult.SUCCESS) {
            //everything is fine and the user can make map requests
            Log.d(TAG, "isServicesOK: Google Play Services is working");
            return true;
        } else if (GoogleApiAvailability.getInstance().isUserResolvableError(available)) {
            //an error occured but we can resolve it
            Log.d(TAG, "isServicesOK: an error occured but we can fix it");
            Dialog dialog = GoogleApiAvailability.getInstance().getErrorDialog(MainActivity.this, available, ERROR_DIALOG_REQUEST);
            dialog.show();
        } else {
            Toast.makeText(this, "You can't make map requests", Toast.LENGTH_SHORT).show();
        }
        return false;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String permissions[],
                                           @NonNull int[] grantResults) {
        mLocationPermissionGranted = false;
        switch (requestCode) {
            case PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    mLocationPermissionGranted = true;
                }
            }
        }
    }

    private void startLocationService() {
        if (!isLocationServiceRunning()) {
            Intent serviceIntent = new Intent(this, LocationService.class);
//        this.startService(serviceIntent);

            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {

                MainActivity.this.startForegroundService(serviceIntent);
            } else {
                startService(serviceIntent);
            }
        }
    }

    private boolean isLocationServiceRunning() {
        ActivityManager manager = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if ("com.bikeology.bikemaps.services.LocationService".equals(service.service.getClassName())) {
                Log.d(TAG, "isLocationServiceRunning: location service is already running.");
                return true;
            }
        }
        Log.d(TAG, "isLocationServiceRunning: location service is not running.");
        return false;
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Log.d(TAG, "onActivityResult: called.");
        switch (requestCode) {
            case PERMISSIONS_REQUEST_ENABLE_GPS: {
                if (mLocationPermissionGranted) {
                    getLastKnownLocation();
                } else {
                    getLocationPermission();
                }
            }
        }

    }

    private void zoomRoute(List<LatLng> lstLatLngRoute) {

        if (googleMap == null || lstLatLngRoute == null || lstLatLngRoute.isEmpty()) return;

        LatLngBounds.Builder boundsBuilder = new LatLngBounds.Builder();
        for (LatLng latLngPoint : lstLatLngRoute)
            boundsBuilder.include(latLngPoint);

        int routePadding=450;
        LatLngBounds latLngBounds = boundsBuilder.build();

        googleMap.animateCamera(
                CameraUpdateFactory.newLatLngBounds(latLngBounds, routePadding),
                650,
                null
        );


    }




    private void movemyCamera(LatLng latLng, float zoom, String title) {
        Log.d(TAG, "moveCamera: moving the camera to: lat: " + latLng.latitude + ", lng: " + latLng.longitude);
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, zoom));
    }


    private void moveCamera(LatLng latLng, float zoom, String title) {
        Log.d(TAG, "moveCamera: moving the camera to: lat: " + latLng.latitude + ", lng: " + latLng.longitude);
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, zoom));
        googleMap.clear();
    }

    private void calculateDirections(Marker marker) {

        Log.d(TAG, "calculateDirections: calculating directions.");


        com.google.maps.model.LatLng destination = new com.google.maps.model.LatLng(
                marker.getPosition().latitude,
                marker.getPosition().longitude
        );
        final DirectionsApiRequest directions = new DirectionsApiRequest(mGeoApiContext);

        directions.alternatives(true);
        directions.origin(
                new com.google.maps.model.LatLng(
                        mUserLocation.getGeo_point().getLatitude(),
                        mUserLocation.getGeo_point().getLongitude()
                )
        );
        Log.d(TAG, "calculateDirections: destination: " + destination.toString());
        directions.destination(destination).setCallback(new PendingResult.Callback<DirectionsResult>() {
            @Override
            public void onResult(DirectionsResult result) {
                Log.d(TAG, "calculateDirections:onResult 1");

                int shortestRoute = -1;
                long shortestDistance = -1;
                for (int i = 0; i < result.routes.length; i++) {
                    long routeDistance = 0;
                    for (int j = 0; j < result.routes[i].legs[0].steps.length; j++) {
                        routeDistance += result.routes[i].legs[0].steps[j].distance.inMeters;
                    }
                    if (shortestRoute == -1 || routeDistance < shortestDistance) {
                        shortestRoute = i;
                        shortestDistance = routeDistance;
                    }
                }
                Log.d(TAG, "calculateDirections:onResult 2");

                Log.d(TAG, "shortestRoute: " + shortestRoute);
                Log.d(TAG, "route length: " + result.routes.length);
                if (shortestRoute == -1) {
                    // no routes
                } else {
                    Log.d(TAG, "calculateDirections: routes: " + result.routes[shortestRoute].toString());
                    Log.d(TAG, "calculateDirections: duration: " + result.routes[shortestRoute].legs[0].duration);
                    Log.d(TAG, "calculateDirections: distance: " + result.routes[shortestRoute].legs[0].distance);
                    //Log.d(TAG, "calculateDirections: geocodedWayPoints: " + result.geocodedWaypoints[shortestRoute].toString());

                    shrtDst = result.routes[shortestRoute].legs[0].distance.inMeters;
                    Log.i(TAG, "legs = " + result.routes[shortestRoute].legs.length);
                    //directionsResult = result.routes[shortestRoute];
                    directionsResult = result;
                    resultShortestRoute = shortestRoute;
                    if(!navYes){
                        addPolylinesToMap(directionsResult, resultShortestRoute);
                    }
                    else
                    {
                        refreshPolylinesToMap(directionsResult, resultShortestRoute);
                    }
                }

            }

            @Override
            public void onFailure(Throwable e) {
                Log.e(TAG, "calculateDirections: Failed to get directions: " + e.getMessage());
                //TODO
                //message box
            }
        });
    }



    /*private void calculateDirections1(Marker marker) {

        Log.d(TAG, "calculateDirections: calculating directions.");


        com.google.maps.model.LatLng destination = new com.google.maps.model.LatLng(
                marker.getPosition().latitude,
                marker.getPosition().longitude
        );
        DirectionsApiRequest directions = new DirectionsApiRequest(mGeoApiContext);

        directions.alternatives(true);
        directions.origin(
                new com.google.maps.model.LatLng(
                        mUserLocation.getGeo_point().getLatitude(),
                        mUserLocation.getGeo_point().getLongitude()
                )
        );
        Log.d(TAG, "calculateDirections: destination: " + destination.toString());
        directions.destination(destination).setCallback(new PendingResult.Callback<DirectionsResult>() {
            @Override
            public void onResult(DirectionsResult result) {
                int shortestRoute = -1;
                long shortestDistance = -1;
                for (int i = 0; i < result.routes.length; i++) {
                    long routeDistance = 0;
                    for (int j = 0; j < result.routes[i].legs.length; j++) {
                        routeDistance += result.routes[i].legs[j].distance.inMeters;
                    }
                    if (shortestRoute == -1 || routeDistance < shortestDistance) {
                        shortestRoute = i;
                        shortestDistance = routeDistance;
                    }
                }
                Log.d(TAG, "shortestRoute: " + shortestRoute);
                Log.d(TAG, "route length: " + result.routes.length);
                if (shortestRoute == -1) {
                    // no routes
                } else {
                    Log.d(TAG, "calculateDirections: routes: " + result.routes[shortestRoute].toString());
                    Log.d(TAG, "calculateDirections: duration: " + result.routes[shortestRoute].legs[0].duration);
                    Log.d(TAG, "calculateDirections: distance: " + result.routes[shortestRoute].legs[0].distance);
                    //Log.d(TAG, "calculateDirections: geocodedWayPoints: " + result.geocodedWaypoints[shortestRoute].toString());

                    shrtDst = result.routes[shortestRoute].legs[0].distance.inMeters;
                    Log.i(TAG, "legs = " + result.routes[shortestRoute].legs.length);
                    directionsResult = result.routes[shortestRoute];
                    mRouteBounds = getRouteBounds(result, shortestRoute);
                    addPolylinesToMap1(result, shortestRoute);
                }

            }


            @Override
            public void onFailure(Throwable e) {
                Log.e(TAG, "calculateDirections: Failed to get directions: " + e.getMessage());
                //TODO
                //message box
            }
        });
    }

    private void calculateDirections2(Marker marker) {

        Log.d(TAG, "calculateDirections: calculating directions.");


        com.google.maps.model.LatLng destination = new com.google.maps.model.LatLng(
                marker.getPosition().latitude,
                marker.getPosition().longitude
        );
        DirectionsApiRequest directions = new DirectionsApiRequest(mGeoApiContext);

        directions.alternatives(true);
        directions.origin(
                new com.google.maps.model.LatLng(
                        mUserLocation.getGeo_point().getLatitude(),
                        mUserLocation.getGeo_point().getLongitude()
                )
        );
        Log.d(TAG, "calculateDirections: destination: " + destination.toString());
        directions.destination(destination).setCallback(new PendingResult.Callback<DirectionsResult>() {
            @Override
            public void onResult(DirectionsResult result) {
                int shortestRoute = -1;
                long shortestDistance = -1;
                for (int i = 0; i < result.routes.length; i++) {
                    long routeDistance = 0;
                    for (int j = 0; j < result.routes[i].legs.length; j++) {
                        routeDistance += result.routes[i].legs[j].distance.inMeters;
                    }
                    if (shortestRoute == -1 || routeDistance < shortestDistance) {
                        shortestRoute = i;
                        shortestDistance = routeDistance;
                    }
                }
                Log.d(TAG, "shortestRoute: " + shortestRoute);
                Log.d(TAG, "route length: " + result.routes.length);
                if (shortestRoute == -1) {
                    // no routes
                } else {
                    Log.d(TAG, "calculateDirections: routes: " + result.routes[shortestRoute].toString());
                    Log.d(TAG, "calculateDirections: duration: " + result.routes[shortestRoute].legs[0].duration);
                    Log.d(TAG, "calculateDirections: distance: " + result.routes[shortestRoute].legs[0].distance);
                    //Log.d(TAG, "calculateDirections: geocodedWayPoints: " + result.geocodedWaypoints[shortestRoute].toString());

                    shrtDst = result.routes[shortestRoute].legs[0].distance.inMeters;
                    Log.i(TAG, "legs = " + result.routes[shortestRoute].legs.length);
                    directionsResult = result.routes[shortestRoute];
                    mRouteBounds = getRouteBounds(result, shortestRoute);
                    addPolylinesToMap2(result, shortestRoute);
                }

            }

            @Override
            public void onFailure(Throwable e) {
                Log.e(TAG, "calculateDirections: Failed to get directions: " + e.getMessage());
                //TODO
                //message box
            }
        });
    }
*/
    public LatLngBounds getRouteBounds (DirectionsResult result, int x){
        double nMax=mUserLocation.getGeo_point().getLatitude(), eMax=mUserLocation.getGeo_point().getLongitude(),
                sMax=mUserLocation.getGeo_point().getLatitude(), wMax=mUserLocation.getGeo_point().getLongitude();

        for(int i=0; i<result.routes[x].legs[0].steps.length; i++){
            double lat = result.routes[x].legs[0].steps[i].endLocation.lat;
            double lng = result.routes[x].legs[0].steps[i].endLocation.lng;
            if(lat > nMax){
                nMax = lat;
            }
            if(lat < sMax){
                sMax = lat;
            }
            if(lng > eMax){
                eMax = lng;
            }
            if(lng < wMax){
                wMax = lng;
            }
        }
        double mapHeight = mMapView.getHeight();
        navCard.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
        double navCardHeight = navCard.getMeasuredHeight();
        Log.d(TAG, "nav Height: " + navCardHeight);
        Log.d(TAG, "map Height: " + mapHeight);

        double perc = navCardHeight/mapHeight;
        double s = sMax - perc*1.5*(nMax-sMax);
        double n = nMax + perc*0.1*(nMax-sMax);
        LatLngBounds routeBounds = new LatLngBounds(new LatLng(s, wMax), new LatLng(n, eMax));
        return routeBounds;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        Bundle mapViewBundle = outState.getBundle(MAPVIEW_BUNDLE_KEY);
        if (mapViewBundle == null) {
            mapViewBundle = new Bundle();
            outState.putBundle(MAPVIEW_BUNDLE_KEY, mapViewBundle);
        }

        mMapView.onSaveInstanceState(mapViewBundle);
    }

    @Override
    protected void onResume() {
        super.onResume();
        mMapView.onResume();
        navYes = false;
        if (checkMapServices()) {
            if (mLocationPermissionGranted) {
                getLastKnownLocation();
            } else {
                getLocationPermission();
            }
        }
        if(FirebaseAuth.getInstance().getCurrentUser() == null) {
            Intent intent = new Intent(MainActivity.this, LoginActivity.class);
            startActivity(intent);
        }

    }

    @Override
    public void onStart() {
        super.onStart();
        mMapView.onStart();
    }


    @Override
    public void onStop() {
        super.onStop();
        mMapView.onStop();
    }

    @Override
    public void onMapReady(GoogleMap map) {
        googleMap = map;
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                        != PackageManager.PERMISSION_GRANTED) {

            return;
        }
        map.setMyLocationEnabled(true);
    }

    @Override
    public void onPause() {
        mMapView.onPause();
        super.onPause();
    }

    @Override
    public void onDestroy() {
        mMapView.onDestroy();
        super.onDestroy();
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        mMapView.onLowMemory();
    }
}
