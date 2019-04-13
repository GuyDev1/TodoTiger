package com.example.guyerez.todotiger;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.provider.Telephony;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingRequest;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;

import java.util.List;

public class LocationReminderActivity extends AppCompatActivity implements OnMapReadyCallback, GoogleMap.OnMapClickListener, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

    //Constants to indicate the geofence creation phase (to update chooseGeofenceMarkerContainer)
    private final int CHOOSE_GEOFENCE_LOCATION=0;
    private final int CHOOSE_GEOFENCE_RADIUS=1;
    private final int CHOOSE_GEOFENCE_MESSAGE=2;
    private final int DELETE_PREVIOUS_GEOFENCE=3;

    private final int REQUEST_LOCATION_PERMISSIONS = 777;
    private final int UPDATE_LOCATION_INTERVAL= 3*60*1000; //3 minutes
    private final int FASTEST_LOCATION_INTERVAL=60*1000; //1 minute
    private final int GEOFENCE_REQ_CODE = 0;

    private GoogleMap mMap;
    private GoogleApiClient googleApiClient;
    private String chosenTaskListTitle;
    private String chosenTaskListId;
    private Location lastLocation;
    private Marker lastLocationMarker;
    private Marker geofenceLocationMarker;
    private FusedLocationProviderClient mFusedLocationClient;
    private LocationRequest locationRequest;
    private PendingIntent geoFencePendingIntent;
    
    private int geofenceCreationPhase;
    private Circle geoFenceBorder;
    private TextView currentInstructionTextView;
    private SeekBar chooseRadiusBar;
    private TextView radiusDescriptionTextView;
    private EditText locationReminderMessage;
    private Button continueButton;
    private float reminderGeofenceRadius;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.location_reminder_activity);
        setTitle("Location");

        initChooseGeofenceMarkerContainer();

        Intent intent=getIntent();
        getTaskListDetails(intent);

        //Initialize Google maps
        initGoogleMaps();


        // Initialize GoogleApiClient
        initGoogleApiClient();

        //Initialize LocationProviderClient
        initLocationProviderClient();

        setContinueButtonOnClickListener();

    }

    private void setContinueButtonOnClickListener() {
        continueButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                switch (geofenceCreationPhase){
                    case CHOOSE_GEOFENCE_LOCATION:
                        showChooseRadiusInsideContainer();
                        break;
                    case CHOOSE_GEOFENCE_RADIUS:
                        showChooseMessageInsideContainer();
                        break;
                    case CHOOSE_GEOFENCE_MESSAGE:
                        startGeofenceCreationProcess();
                        showDeleteGeofenceInsideContainer();
                        break;
                    case DELETE_PREVIOUS_GEOFENCE:
                        removeGeofence();
                        showChooseLocationInsideContainer();
                        break;
                }
            }
        });
    }

    private void initChooseGeofenceMarkerContainer() {
        currentInstructionTextView=findViewById(R.id.current_instruction_title);
        chooseRadiusBar=findViewById(R.id.radiusBar);
        radiusDescriptionTextView=findViewById(R.id.radiusDescription);
        locationReminderMessage=findViewById(R.id.location_reminder_message);
        continueButton=findViewById(R.id.continue_button);
    }

    private void showChooseLocationInsideContainer() {
        currentInstructionTextView.setText("Select where you want to be reminded");
        continueButton.setEnabled(false);
        continueButton.setText("Continue");
        chooseRadiusBar.setVisibility(View.GONE);
        radiusDescriptionTextView.setVisibility(View.GONE);
        locationReminderMessage.setVisibility(View.GONE);
        geofenceCreationPhase=CHOOSE_GEOFENCE_LOCATION;

    }

    private void showChooseRadiusInsideContainer() {
        currentInstructionTextView.setText("Choose the location Reminder's radius");
        continueButton.setEnabled(true);
        continueButton.setText("Continue");
        chooseRadiusBar.setVisibility(View.VISIBLE);
        radiusDescriptionTextView.setVisibility(View.VISIBLE);
        setDefaultRadiusValues();
        locationReminderMessage.setVisibility(View.GONE);
        geofenceCreationPhase=CHOOSE_GEOFENCE_RADIUS;
        //Set listener to update radius on UI according to user selection
        setRadiusBarProgressListener();

    }

    private void setDefaultRadiusValues() {
        radiusDescriptionTextView.setText("400 meters");
        reminderGeofenceRadius=400.0f;
        drawGeofenceBorderOnMap();
    }

    private void setRadiusBarProgressListener() {
        chooseRadiusBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                updateRadiusBarWithProgress(progress);
            }
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) { }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) { }
        });
    }

    private void updateRadiusBarWithProgress(int progress) {
        reminderGeofenceRadius = getRadiusValue(progress);
        radiusDescriptionTextView.setText(String.valueOf((int)reminderGeofenceRadius) +
                " meters");
        drawGeofenceBorderOnMap();


    }

    private float getRadiusValue(int progress) {
        return 100 + ((float)progress + 1) * 100;
    }

    private void showChooseMessageInsideContainer() {
        currentInstructionTextView.setText("Write the location reminder's message");
        continueButton.setEnabled(false);
        continueButton.setText("Create");
        chooseRadiusBar.setVisibility(View.GONE);
        radiusDescriptionTextView.setVisibility(View.GONE);
        locationReminderMessage.setVisibility(View.VISIBLE);
        geofenceCreationPhase=CHOOSE_GEOFENCE_MESSAGE;
        addOnTextChangedListenerToMessage();
    }

    private void showDeleteGeofenceInsideContainer() {
        currentInstructionTextView.setText("Do you want to delete this location reminder?");
        continueButton.setEnabled(true);
        continueButton.setText("Delete");
        chooseRadiusBar.setVisibility(View.GONE);
        radiusDescriptionTextView.setVisibility(View.GONE);
        locationReminderMessage.setVisibility(View.GONE);
        geofenceCreationPhase=DELETE_PREVIOUS_GEOFENCE;
    }

    private void notifyGeofenceCreated() {
        Toast.makeText(this, "Location reminder created!", Toast.LENGTH_SHORT).show();
    }

    private void notifyGeofenceRemoved() {
        Toast.makeText(this, "Location reminder Deleted!", Toast.LENGTH_SHORT).show();
    }

    private void addOnTextChangedListenerToMessage() {
        locationReminderMessage.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }
            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                if (charSequence.toString().trim().length() > 0)
                    continueButton.setEnabled(true);
                else
                    continueButton.setEnabled(false);
            }
            @Override
            public void afterTextChanged(Editable editable) {
            }
        });
    }

    private void getTaskListDetails(Intent intent) {
        Bundle extras = intent.getExtras();
        chosenTaskListTitle = extras.getString("taskListTitle");
        chosenTaskListId = extras.getString("taskListId");
    }


    private void initGoogleMaps() {
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
    }

    private void initGoogleApiClient() {
        if ( googleApiClient == null ) {
            googleApiClient = new GoogleApiClient.Builder( this )
                    .addConnectionCallbacks( this )
                    .addOnConnectionFailedListener( this )
                    .addApi( LocationServices.API )
                    .build();
        }
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        getLastKnownLocation();
        retrieveLastGeofenceMarker();
        if(geofenceLocationMarker==null)
            showChooseLocationInsideContainer();
        else
            showDeleteGeofenceInsideContainer();
    }


    @Override
    public void onConnectionSuspended(int i) {
        Log.d("connection suspended", "onConnectionSuspended: ");
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        Log.d("connection failed", "onConnectionFailed: ");
    }


    private void initLocationProviderClient() {
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
    }



    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        //Set a click listener for Map clicks
        //So the user can choose a location for his reminder
        mMap.setOnMapClickListener(this);
    }

    @Override
    public void onMapClick(LatLng latLng) {
        Log.d("map clicked", "onMapClick: ");
        setGeofenceMarkerLocation(latLng);
        // The user chose a location for his reminder.
        // Allow him to continue creating the reminder.
        if(geofenceCreationPhase==CHOOSE_GEOFENCE_LOCATION)
            continueButton.setEnabled(true);
    }

    private void setGeofenceMarkerLocation(LatLng latLng) {
        String markerTitle = "Location reminder for: " + chosenTaskListTitle;
        MarkerOptions markerOptions = new MarkerOptions()
                .position(latLng)
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE))
                .title(markerTitle);
        if(mMap!=null){
            //Remove previous geoFenceLocationMarker
            if(geofenceLocationMarker!=null)
                geofenceLocationMarker.remove();
            geofenceLocationMarker = mMap.addMarker(markerOptions);

            //jump to geofenceLocationMarker - so the user can see where the location reminder was set
            float zoom = 14f;
            CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngZoom(latLng, zoom);
            mMap.animateCamera(cameraUpdate);

        }
    }

    private void setLastLocationMarker(Location lastLocation) {
        LatLng coordinates=new LatLng(lastLocation.getLatitude(), lastLocation.getLongitude());
        MarkerOptions markerOptions = new MarkerOptions()
                .position(coordinates)
                .title("Last Known Location");
        if ( mMap!=null ) {
            if ( lastLocationMarker != null )
                lastLocationMarker.remove();
            lastLocationMarker = mMap.addMarker(markerOptions);
            if(geofenceLocationMarker==null){
                //Jump to current location only if the user hasn't set any location reminders yet
                float zoom = 14f;
                CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngZoom(coordinates, zoom);
                mMap.animateCamera(cameraUpdate);
            }

        }
    }

    private void getLastKnownLocation(){
        if (checkPermission()) {
            mFusedLocationClient.getLastLocation()
                    .addOnSuccessListener(this, new OnSuccessListener<Location>() {
                        @Override
                        public void onSuccess(Location location) {
                            // Got last known location. In some rare situations, this can be null.
                            if (location != null) {
                                lastLocation = location;
                                setLastLocationMarker(lastLocation);
                                requestLocationUpdates();

                            } else {
                                Log.d("no location found", "no location found");
                                requestLocationUpdates();
                            }
                        }
                    });
        }
        else
            askPermission();
    }

    // Request location updates
    private void requestLocationUpdates(){
        locationRequest = LocationRequest.create()
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                .setInterval(UPDATE_LOCATION_INTERVAL)
                .setFastestInterval(FASTEST_LOCATION_INTERVAL);

        if ( checkPermission() )
            mFusedLocationClient.requestLocationUpdates(locationRequest,mLocationCallback, Looper.myLooper());
    }

    //Define locationCallBack for location updates
    LocationCallback mLocationCallback = new LocationCallback() {
        @Override
        public void onLocationResult(LocationResult locationResult) {
            List<Location> locationList = locationResult.getLocations();
            if (locationList.size() > 0) {
                //The last location in the list is the newest
                Location location = locationList.get(locationList.size() - 1);
                lastLocation = location;
                setLastLocationMarker(lastLocation);
            }
        }
    };

    // Checks for permission to access user's location
    private boolean checkPermission() {
        // Ask for permission if it wasn't granted yet
        return (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED );
    }

    // Asks for permission to access location services
    private void askPermission() {
        ActivityCompat.requestPermissions(
                this, new String[] { Manifest.permission.ACCESS_FINE_LOCATION },
                REQUEST_LOCATION_PERMISSIONS
        );
    }

    // Verify user's response of the permission requested
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if(requestCode==REQUEST_LOCATION_PERMISSIONS){
            if ( grantResults.length > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED ){
                // Permission granted
                getLastKnownLocation();
            }
            else {
                // Permission denied
                permissionsDenied();
            }
        }
    }

    // App cannot work without the permissions
    private void permissionsDenied() {
        Log.d("permission denied", "permissionsDenied()");
        Toast.makeText(this, "app requires location permission to proceed", Toast.LENGTH_SHORT).show();
    }

    private void startGeofenceCreationProcess(){
        if(geofenceLocationMarker!=null)
        {
            Geofence currentGeofence = createNewGeofence(
                    geofenceLocationMarker.getPosition(),
                    reminderGeofenceRadius);
            GeofencingRequest currentGeofenceRequest = createNewGeofenceRequest(currentGeofence);
            addGeofenceRequest(currentGeofenceRequest);
        }
    }

    private Geofence createNewGeofence(LatLng position, float geofenceRadius) {
        String geofenceMessage = locationReminderMessage.getText().toString();
        return new Geofence.Builder()
                .setRequestId(geofenceMessage + "@" +chosenTaskListTitle
                        + "@" + chosenTaskListId)
                .setCircularRegion( position.latitude, position.longitude, geofenceRadius)
                .setExpirationDuration( Geofence.NEVER_EXPIRE )
                .setTransitionTypes( Geofence.GEOFENCE_TRANSITION_ENTER
                        | Geofence.GEOFENCE_TRANSITION_EXIT | Geofence.GEOFENCE_TRANSITION_DWELL )
                .setLoiteringDelay(UPDATE_LOCATION_INTERVAL)
                .build();
    }

    private void addGeofenceRequest(GeofencingRequest request) {
        if (checkPermission())
            LocationServices.getGeofencingClient(this)
            .addGeofences(request,createGeofencePendingIntent())
                    .addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void aVoid) {
                            notifyGeofenceCreated();
                            Log.d("wat - got here", "onSuccess: ");
                            saveGeofenceToSharedPref();
                            drawGeofenceBorderOnMap();
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            showCheckLocationSettingsToast();
                            Log.d("Geofence request failed", "onFailure: ");
                        }
                    });
    }

    private void showCheckLocationSettingsToast() {
        Toast.makeText(this, "Service unavailable," +
                " Go to Settings>Location>High Accuracy mode", Toast.LENGTH_LONG).show();
    }


    private void saveGeofenceToSharedPref() {
        SharedPreferences pref = getApplicationContext().getSharedPreferences("MyPref", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = pref.edit();

        editor.putLong( "LAT " + chosenTaskListId, Double.doubleToRawLongBits( geofenceLocationMarker.getPosition().latitude ));
        editor.putLong( "LON " + chosenTaskListId, Double.doubleToRawLongBits( geofenceLocationMarker.getPosition().longitude ));
        editor.putFloat( "RADIUS " + chosenTaskListId, reminderGeofenceRadius);
        editor.commit();
    }

    private void removeGeofenceFromSharedPref() {
        SharedPreferences pref = getApplicationContext().getSharedPreferences("MyPref", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = pref.edit();

        editor.remove("LAT " + chosenTaskListId);
        editor.remove("LON " + chosenTaskListId);
        editor.remove("RADIUS " + chosenTaskListId);
        editor.commit();
    }

    private void drawGeofenceBorderOnMap() {
        if ( geoFenceBorder != null )
            geoFenceBorder.remove();
        CircleOptions circleOptions = new CircleOptions()
                .center( geofenceLocationMarker.getPosition())
                .strokeColor(Color.argb(50, 70,70,70))
                .fillColor( Color.argb(100, 255,165,0) )
                .radius( reminderGeofenceRadius );
        geoFenceBorder = mMap.addCircle( circleOptions );
    }

    private void retrieveLastGeofenceMarker() {
        SharedPreferences pref = getApplicationContext().getSharedPreferences("MyPref", Context.MODE_PRIVATE);

        if (pref.contains("LAT " + chosenTaskListId)) {
            double lat = Double.longBitsToDouble(pref.getLong("LAT " + chosenTaskListId, -1));
            double lon = Double.longBitsToDouble(pref.getLong("LON " + chosenTaskListId, -1));
            reminderGeofenceRadius = pref.getFloat("RADIUS " + chosenTaskListId,400);
            LatLng latLng = new LatLng(lat, lon);
            setGeofenceMarkerLocation(latLng);
            drawGeofenceBorderOnMap();
        }
    }

    private PendingIntent createGeofencePendingIntent() {
        if (geoFencePendingIntent != null )
            return geoFencePendingIntent;
        Intent intent = new Intent( this, GeofenceTransitionService.class);
        return PendingIntent.getService(
                this, GEOFENCE_REQ_CODE, intent, PendingIntent.FLAG_UPDATE_CURRENT );
    }

    private GeofencingRequest createNewGeofenceRequest(Geofence currentGeofence) {
        return new GeofencingRequest.Builder()
                .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_DWELL)
                .addGeofence(currentGeofence)
                .build();
    }

    private void removeGeofence() {
        LocationServices.getGeofencingClient(this)
                .removeGeofences(createGeofencePendingIntent())
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        notifyGeofenceRemoved();
                        removeGeofenceMarkerAndBorder();
                        removeGeofenceFromSharedPref();
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        showCheckLocationSettingsToast();
                        Log.d("Geofence request failed", "onFailure: ");
                    }
                });
    }

    private void removeGeofenceMarkerAndBorder() {
        if ( geofenceLocationMarker != null)
            geofenceLocationMarker.remove();
        if ( geoFenceBorder != null )
            geoFenceBorder.remove();
    }

    public static Intent makeNotificationIntent(Context context,String geofenceRequestId) {
        Intent intent = new Intent( context, TaskActivity.class );
        updateCurrentTaskListForNotification(context,geofenceRequestId);
        return intent;
    }

    //Update current TaskList that the geofence belongs to
    //So when the Location Notification will trigger, the user can get to the
    //relevant TaskList
    private static void updateCurrentTaskListForNotification(Context context,String geofenceRequestId) {
        String[] geofenceRequestIdArray = geofenceRequestId.split("@");
        String tasklistTitle = geofenceRequestIdArray[1];
        String taskListId = geofenceRequestIdArray[2];

        SharedPreferences pref = context.getSharedPreferences("MyPref", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = pref.edit();

        Log.d("wat123", "updateCurrentTaskListForNotification: " + taskListId);

        editor.putString("currentTaskList", taskListId);
        editor.putString("currentTaskListTitle", tasklistTitle);
        editor.commit();
    }

    @Override
    protected void onStart() {
        super.onStart();
        // Call GoogleApiClient connection when starting the Activity
        googleApiClient.connect();
    }

    @Override
    protected void onStop() {
        super.onStop();
        // Disconnect GoogleApiClient when stopping Activity
        googleApiClient.disconnect();
    }
}