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
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
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
import android.widget.ImageView;
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
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.GeoPoint;
import com.google.maps.DirectionsApiRequest;
import com.google.maps.GeoApiContext;
import com.google.maps.PendingResult;
import com.google.maps.internal.PolylineEncoding;
import com.google.maps.model.DirectionsResult;
import com.google.maps.model.DirectionsRoute;


import java.util.ArrayList;
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
    private Polyline mPolyline;
    private Button button_recenter;


    //FIREBASE
    private UserLocation mUserLocation;
    private FirebaseFirestore mDb;

    //PLACE
    private PlaceInfo Place;
    private Place mPlace;

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
    private RatingBar ratingBar;

    //NAV CARD
    private CardView navCard;

    private TextView navText;

    private Button button_nav_yes;
    private Button button_nav_cancel;

    private ProgressBar calculateRouteProgressBar;

    //NAVIGATION MODE
    private BroadcastReceiver locationReceiver;
    private Button button_endtrip;

    //BOOLEANS
    private boolean isRouteCalculated = false;
    private boolean navYes = false;

    // avg speed calculator

    private long durationLong;
    private long shrtDst;
    private TextView durationTextView;
    private long tripDuration;


    //avg speed updater
    private long startTime;
    private long endTime;
    private long tripSpeed;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setupDrawer();

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
        button_website = findViewById(R.id.button_phone);
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

        // recenter buttona
        button_recenter = findViewById(R.id.button_recenter);

        // end trip button
        button_endtrip = findViewById(R.id.endtrip);
        button_endtrip.setVisibility(View.GONE);

        // trip duration
        durationTextView = findViewById(R.id.durationTextView);
        durationTextView.setVisibility(View.GONE);



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

                calculateDirections(marker);
                infoCard.setVisibility(View.GONE);
                navCard.setVisibility(View.VISIBLE);
                searchCard.setVisibility(View.GONE);

                navText.setText("Navigate to " + mPlace.getName() + "?");
                isRouteCalculated = true;

            }
        });

        // joy route button
        button_joyrt.setVisibility(View.GONE);

        // nav yes button
        button_nav_yes.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                button_endtrip.setVisibility(View.VISIBLE);
                button_recenter.setVisibility(View.GONE);
                LatLng myLatLng = new LatLng(mUserLocation.getGeo_point().getLatitude(),
                        mUserLocation.getGeo_point().getLongitude());

                navYes = true;
                Log.d(TAG, "navYes true");

                startTime = System.currentTimeMillis()/1000;

                mPolyline.setColor(Color.rgb(255, 0, 0));
                mPolyline.setWidth(30);

                navCard.setVisibility(View.GONE);
                searchCard.setVisibility(View.GONE);

                CameraPosition povCamera = new CameraPosition.Builder()
                        .target(myLatLng)
                        .zoom(25)
                        .bearing(210)
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

                button_nav_yes.setVisibility(View.GONE);
                button_nav_cancel.setVisibility(View.GONE);
                searchCard.setVisibility(View.VISIBLE);
                navText.clearComposingText();
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


                LatLng myLatLng = new LatLng(mUserLocation.getGeo_point().getLatitude(),
                        mUserLocation.getGeo_point().getLongitude());
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
                            .target(myLatLng)
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
                durationTextView.setVisibility(View.GONE);
                searchCard.setVisibility(View.VISIBLE);
                navText.clearComposingText();
                durationTextView.setVisibility(View.GONE);
                googleMap.clear();
                LatLng latLng = mPlace.getLatLng();
                CameraPosition centeredCamera = new CameraPosition.Builder()
                        .target(latLng)
                        .zoom(DEFAULT_ZOOM)
                        .bearing(0)
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
        });

        // navigation mode camera updater
        locationReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if(!navYes)
                    return;
                Location location = intent.getExtras().getParcelable("location");
                float bearing = intent.getExtras().getFloat("bearing");
                Log.d(TAG, "update camera valid : " + location);
                LatLng myLatLng = new LatLng(location.getLatitude(),
                        location.getLongitude());
                durationLong = shrtDst/((mUserLocation.getAvgSpeed()*1000)/60);
                durationTextView.setText("Trip duration: " + durationLong + " minutes");


                CameraPosition povCamera = new CameraPosition.Builder()
                        .target(myLatLng)
                        .zoom(25)
                        .bearing(210)
                        .tilt(60)
                        .build();
                googleMap.animateCamera(CameraUpdateFactory.newCameraPosition(povCamera));


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

                // TODO: Get info about the selected place.
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
                infoTextName.setText(mPlace.getName());
                infoTextAddress.setText(mPlace.getAddress());
                ratingBar.setRating(mPlace.getRating());
                ratingValue.setText(String.valueOf(mPlace.getRating()));
                Log.d(TAG, "rating: " + mPlace.getRating());
                infoCard.setVisibility(View.VISIBLE);
                button_fastrt.setVisibility(View.VISIBLE);
                // button_joyrt.setVisibility(View.VISIBLE);

                final Uri website;
                website = place.getWebsiteUri();

                    button_website.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            startActivity(new Intent(Intent.ACTION_VIEW, website));
                        }
                    });
                    final String phone;
                    phone = place.getPhoneNumber().toString();
                    button_phone.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        /*Intent callIntent = new Intent(Intent.ACTION_CALL);
                        callIntent.setData(Uri.parse(phone));
                        startActivity(callIntent);*/
                    }
                });


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
                        button_endtrip.setVisibility(View.GONE);
                        googleMap.clear();
                        marker.remove();
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
                mPolyline = googleMap.addPolyline(new PolylineOptions().addAll(newDecodedPath));
                mPolyline.setColor(-7829368);
                mPolyline.setClickable(false);
                zoomRoute(mPolyline.getPoints());
                calculateRouteProgressBar.setVisibility(View.GONE);
                button_nav_yes.setVisibility(View.VISIBLE);
                button_nav_cancel.setVisibility(View.VISIBLE);
                navText.setVisibility(View.VISIBLE);

                durationLong = shrtDst/((mUserLocation.getAvgSpeed()*1000)/60);
                durationTextView.setText("Trip duration: " + durationLong + " minutes");
                durationTextView.setVisibility(View.VISIBLE);
                durationTextView.bringToFront();

                Log.d(TAG, "shrtDst: " + shrtDst);
                Log.d(TAG, "avgSpeed: " + mUserLocation.getAvgSpeed());

                button_fastrt.setVisibility(View.GONE);
                button_joyrt.setVisibility(View.GONE);

                //}
            }
        });
    }


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
                    addPolylinesToMap(result, shortestRoute);
                }

            }

            @Override
            public void onFailure(Throwable e) {
                Log.e(TAG, "calculateDirections: Failed to get directions: " + e.getMessage());

            }
        });
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
