package com.example.guyerez.todotiger;

import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.TaskStackBuilder;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.text.TextUtils;
import android.util.Log;

import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofenceStatusCodes;
import com.google.android.gms.location.GeofencingEvent;

import java.util.ArrayList;
import java.util.List;

public class GeofenceTransitionService extends IntentService {

    public GeofenceTransitionService() {
        super("GeofenceTransitionService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        GeofencingEvent geofencingEvent = GeofencingEvent.fromIntent(intent);
        // Handling errors
        if (geofencingEvent.hasError() ) {
            String errorMsg = getErrorString(geofencingEvent.getErrorCode() );
            Log.e( "Geofence error", errorMsg );
            return;
        }
        int geoFenceTransition = geofencingEvent.getGeofenceTransition();
        // Check if the transition type is ENTER/EXIT, otherwise it's irrelevant
        if (geoFenceTransition == Geofence.GEOFENCE_TRANSITION_ENTER ||
                geoFenceTransition == Geofence.GEOFENCE_TRANSITION_EXIT ) {
            // Get the geofences that were triggered
            List<Geofence> triggeringGeofences = geofencingEvent.getTriggeringGeofences();
            String geofenceRequestId = getGeofenceRequestId(triggeringGeofences);

            // Send geofence requestId details so we can retrieve the notification's message
            initGeofenceNotification(geofenceRequestId);
        }
    }


    private String getGeofenceRequestId(List<Geofence> triggeringGeofences) {
        // get the ID of each geofence triggered so we could retrieve
        // the notification's message - we will assume only 1 geofence is triggered,
        // so we would access the List's first index.
        ArrayList<String> geoFenceRequestIdList = new ArrayList<>();
        for (Geofence geofence : triggeringGeofences ) {
            geoFenceRequestIdList.add(geofence.getRequestId() );
        }

        return geoFenceRequestIdList.get(0);
    }

    private void initGeofenceNotification(String geofenceRequestId) {

        // Intent to start TaskActivity of the relevant TaskList
        Intent notificationIntent = LocationReminderActivity.makeNotificationIntent(
                getApplicationContext(),geofenceRequestId);

        TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
        stackBuilder.addParentStack(LocationReminderActivity.class);
        stackBuilder.addNextIntent(notificationIntent);
        PendingIntent notificationPendingIntent = stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);

        // Creating and sending Notification
        NotificationManager notificationManager =
                (NotificationManager) getSystemService( Context.NOTIFICATION_SERVICE );
        notificationManager.notify(
                0,
                createNotification(geofenceRequestId, notificationPendingIntent));

    }

    // Create the notification
    private Notification createNotification(String geofenceRequestId, PendingIntent notificationPendingIntent) {
        String notificationMessage = getGeofenceNotificationMessage(geofenceRequestId);

        //Build the notification's UI and info
        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(getApplicationContext(), "123")
                .setSmallIcon(R.drawable.ic_stat_name)
                .setLargeIcon(BitmapFactory.decodeResource(getApplicationContext().getResources(),
                        R.mipmap.ic_launcher_foreground))
                .setContentTitle("Location Reminder!")
                .setContentText(notificationMessage)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                // Set the intent that will fire when the user taps the notification
                .setContentIntent(notificationPendingIntent)
                .setAutoCancel(true);
        return mBuilder.build();
    }

    private String getGeofenceNotificationMessage(String geofenceRequestId) {
        String[] geofenceRequestIdArray = geofenceRequestId.split("@");
        String geofenceNotificationMessage = geofenceRequestIdArray[0];
        return geofenceNotificationMessage;
    }


    private static String getErrorString(int errorCode) {
        switch (errorCode) {
            case GeofenceStatusCodes.GEOFENCE_NOT_AVAILABLE:
                return "GeoFence not available";
            case GeofenceStatusCodes.GEOFENCE_TOO_MANY_GEOFENCES:
                return "Too many GeoFences";
            case GeofenceStatusCodes.GEOFENCE_TOO_MANY_PENDING_INTENTS:
                return "Too many pending intents";
            default:
                return "Unknown error.";
        }
    }
}
