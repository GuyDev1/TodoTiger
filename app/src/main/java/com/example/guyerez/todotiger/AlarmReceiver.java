package com.example.guyerez.todotiger;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

public class AlarmReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {

        if (intent.getAction() != null && context != null) {
            if (intent.getAction().equals("android.intent.action.BOOT_COMPLETED")) {
                //TODO - HANDLE DEVICE REBOOT
            }
        }


        //Trigger the notification
        Bundle extras = intent.getExtras();
        String taskTitle = "Error, no task title!";
        int taskIntId=-1;
        if (extras != null) {
            taskTitle = extras.getString("taskTitle");
            taskIntId=extras.getInt("taskIntId");
        }

        TaskInfoFragment.showReminderNotification(context, MainActivity.class, taskTitle,taskIntId);

    }
}
