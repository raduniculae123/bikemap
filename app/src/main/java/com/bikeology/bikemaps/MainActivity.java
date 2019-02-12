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
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.CardView;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

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
import java.util.List;

import static com.bikeology.bikemaps.Constants.ERROR_DIALOG_REQUEST;
import static com.bikeology.bikemaps.Constants.MAPVIEW_BUNDLE_KEY;
import static com.bikeology.bikemaps.Constants.PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION;
import static com.bikeology.bikemaps.Constants.PERMISSIONS_REQUEST_ENABLE_GPS;

public class MainActivity extends BaseActivity implements OnMapReadyCallback {

    private static final String TAG = "MainActivity";
    private boolean mLocationPermissionGranted = false;
    private MapView mMapView;
    private FusedLocationProviderClient mFusedLocationClient;
    private UserLocation mUserLocation;
    private FirebaseFirestore mDb;
    public static GoogleMap googleMap;
    private static final float DEFAULT_ZOOM = 15f;
    private Marker marker;
    private PlaceAutocompleteFragment autocompleteFragment;
    private Button button_recenter,button_fastrt,button_joyrt, button_website;
    private PlaceInfo Place;
    private GeoApiContext mGeoApiContext = null;
    private CardView infoCard;
    private TextView infoTextName;
    private TextView infoTextAddress;
    private CardView navCard;
    private TextView navText;
    private Button button_nav_yes, button_nav_cancel;
    private Place mPlace;
    private boolean isRouteCalculated = false;
    private Polyline mPolyline;
    private BroadcastReceiver locationReceiver;
    private boolean navYes = false;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setupDrawer();

        infoCard = findViewById(R.id.card_info);
        infoCard.setVisibility(View.GONE);
        infoTextName = findViewById(R.id.text_place_name);
        infoTextAddress = findViewById(R.id.text_place_address);
        button_website = findViewById(R.id.button_website);

        navCard = findViewById(R.id.card_navigate);
        navCard.setVisibility(View.GONE);
        navText = findViewById(R.id.text_nav_to);
        button_nav_yes = findViewById(R.id.button_nav_yes);
        button_nav_cancel = findViewById(R.id.button_nav_cancel);

        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {

            @Override
            public void run() {
                LatLng myLatLng = new LatLng(mUserLocation.getGeo_point().getLatitude(),
                        mUserLocation.getGeo_point().getLongitude());
                googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(myLatLng, 15));

            }

        }, 1000);


        button_fastrt = (Button) findViewById(R.id.btn_fastrt);
        button_fastrt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                calculateDirections(marker);
                infoCard.setVisibility(View.GONE);
                navCard.setVisibility(View.VISIBLE);
                navText.setText("Navigate to " + mPlace.getName() + "?");
                isRouteCalculated = true;
            }
        });


        button_joyrt = (Button) findViewById(R.id.btn_joyrt);
        button_joyrt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                /////////
                /////////
            }
        });
        button_nav_cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                infoCard.setVisibility(View.VISIBLE);
                navCard.setVisibility(View.GONE);
                navText.clearComposingText();
                googleMap.clear();
                LatLng latLng = mPlace.getLatLng();
                googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, DEFAULT_ZOOM));
                MarkerOptions options = new MarkerOptions()
                        .position(latLng)
                        .title(mPlace.getName().toString());
                marker = googleMap.addMarker(options);
                googleMap.addMarker(options);
                button_fastrt.setVisibility(View.VISIBLE);
                button_joyrt.setVisibility(View.VISIBLE);
                isRouteCalculated = false;
                navYes = false;
            }
        });

        button_nav_yes.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                LatLng myLatLng = new LatLng(mUserLocation.getGeo_point().getLatitude(),
                        mUserLocation.getGeo_point().getLongitude());

               navYes = true;

                mPolyline.setColor(Color.rgb(255, 0, 0));
                mPolyline.setWidth(30);
            }
        });




        button_joyrt.setVisibility(View.GONE);
        button_fastrt.setVisibility(View.GONE);
        button_recenter = (Button) findViewById(R.id.button_recenter);
        button_recenter.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {


                LatLng myLatLng = new LatLng(mUserLocation.getGeo_point().getLatitude(),
                        mUserLocation.getGeo_point().getLongitude());

                CameraPosition centeredCamera =new CameraPosition.Builder()
                        .target(myLatLng)
                        .zoom(DEFAULT_ZOOM)
                        .bearing(0)
                        .tilt(0)
                        .build();

                googleMap.animateCamera(CameraUpdateFactory.newCameraPosition(centeredCamera));

                if (button_fastrt.getVisibility() == View.VISIBLE) {
                    button_fastrt.setVisibility(View.VISIBLE);
                } else button_fastrt.setVisibility(View.GONE);

                if (button_joyrt.getVisibility() == View.VISIBLE) {
                    button_joyrt.setVisibility(View.VISIBLE);
                } else button_joyrt.setVisibility(View.GONE);

            }
        });


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
                CameraPosition povCamera = new CameraPosition.Builder()
                        .target(myLatLng)      // Sets the center of the map to Mountain View
                        .zoom(17)                   // Sets the zoom
                        .bearing(bearing)                // Sets the orientation of the camera to east
                        .tilt(30)                   // Sets the tilt of the camera to 30 degrees
                        .build();                   // Creates a CameraPosition from the builder
                googleMap.animateCamera(CameraUpdateFactory.newCameraPosition(povCamera));


            }
        };
        getApplicationContext().registerReceiver(locationReceiver, new IntentFilter("BM_Location"));


        initGoogleMap(savedInstanceState);
        mDb = FirebaseFirestore.getInstance();
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        autocompleteFragment = (PlaceAutocompleteFragment)
                getFragmentManager().findFragmentById(R.id.place_autocomplete_fragment);


        autocompleteFragment.setOnPlaceSelectedListener(new PlaceSelectionListener() {
            @Override
            public void onPlaceSelected(Place place) {

                // TODO: Get info about the selected place.
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
                infoCard.setVisibility(View.VISIBLE);
                button_fastrt.setVisibility(View.VISIBLE);
                button_joyrt.setVisibility(View.VISIBLE);

                final Uri website;
                website = place.getWebsiteUri();
                button_website.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        startActivity(new Intent(Intent.ACTION_VIEW, website));
                    }
                });
            }

            @Override
            public void onError(Status status) {
                // TODO: Handle the error.
                Log.i(TAG, "An error occurred: " + status);
            }
        });

        autocompleteFragment.getView().findViewById(R.id.place_autocomplete_clear_button)
                .setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
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
                        if(isRouteCalculated) {
                            LatLng myLatLng = new LatLng(mUserLocation.getGeo_point().getLatitude(),
                                    mUserLocation.getGeo_point().getLongitude());
                            googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(myLatLng, 15));
                        }

                    }
                });

    }


    private void addPolylinesToMap(final DirectionsResult result, final int shortestRoute){
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
                    for(com.google.maps.model.LatLng latLng: decodedPath){

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
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
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


        if (mGeoApiContext == null){

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

        if(googleMap == null || lstLatLngRoute == null || lstLatLngRoute.isEmpty()) return;

        LatLngBounds.Builder boundsBuilder = new LatLngBounds.Builder();
        for(LatLng latLngPoint : lstLatLngRoute)
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

    private void calculateDirections(Marker marker){
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

                    addPolylinesToMap(result, shortestRoute);
                }

            }

            @Override
            public void onFailure(Throwable e) {
                Log.e(TAG, "calculateDirections: Failed to get directions: " + e.getMessage() );

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
