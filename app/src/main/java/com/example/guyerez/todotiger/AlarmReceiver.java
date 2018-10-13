package com.example.guyerez.todotiger;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.Calendar;


public class AlarmReceiver extends BroadcastReceiver  {

    private ChildEventListener mChildEventListener;
    private FirebaseDatabase mFirebaseDatabase;
    private DatabaseReference mTaskDatabaseReference;
    private Calendar calendar;



    @Override
    public void onReceive(final Context context, Intent intent) {
        mFirebaseDatabase = FirebaseDatabase.getInstance();

        //Get user currently logged in
        SharedPreferences sp=context.getSharedPreferences("MyPref", Context.MODE_PRIVATE);
        String currentUser=sp.getString("userId",null);

        mTaskDatabaseReference=mFirebaseDatabase.getReference().child("users").child(currentUser).child("allTasks");

        if (intent.getAction() != null && context != null) {
            if (intent.getAction().equals("android.intent.action.BOOT_COMPLETED")) {
                //Reinstate all active reminders.
                calendar=Calendar.getInstance();
                attachTaskDatabaseReadListener(context,currentUser);

            }
        }
        else{
            //Trigger the notification
            Bundle extras = intent.getExtras();
            String taskTitle = "Error, no task title!";
            int taskIntId=-1;
            String currentTaskListId=null;
            if (extras != null) {
                taskTitle = extras.getString("taskTitle");
                taskIntId=extras.getInt("taskIntId");
                currentTaskListId=extras.getString("taskList");
            }
            if(currentTaskListId!=null){
                //Update current TaskList in SharedPreferences
                SharedPreferences.Editor editor = sp.edit();
                editor.putString("currentTaskList", currentTaskListId);
                editor.commit();
            }

            TaskInfoFragment.showReminderNotification(context, TaskActivity.class, taskTitle,taskIntId);
            //Set the task's reminderDisplayed to true - the user presumably saw the reminder
            String taskId=extras.getString("taskId");
            mTaskDatabaseReference.child(taskId).child("reminderDisplayed").setValue(true);
        }






    }





    private void attachTaskDatabaseReadListener(final Context context,String userId) {
        mChildEventListener = new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot dataSnapshot, String s) {
                Task task = dataSnapshot.getValue(Task.class);
                //Check if the task has a reminder and if it had been displayed already
                if(task.getReminderDate()!=null && task.getReminderTime()!=null){
                    if(!task.getReminderDisplayed()){
                        TaskInfoFragment.setReminder(context,AlarmReceiver.class,task.getReminderDate(),
                                task.getReminderTime(),task,mTaskDatabaseReference.child(task.getId()));
                    }
                }
            }
            public void onChildChanged(DataSnapshot dataSnapshot, String s) {}
            public void onChildRemoved(DataSnapshot dataSnapshot) {
            }
            public void onChildMoved(DataSnapshot dataSnapshot, String s) {}
            public void onCancelled(DatabaseError databaseError) {}
        };

        mTaskDatabaseReference.addChildEventListener(mChildEventListener);

    }



}
