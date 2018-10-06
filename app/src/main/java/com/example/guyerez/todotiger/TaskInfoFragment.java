package com.example.guyerez.todotiger;

import android.app.AlarmManager;
import android.app.DatePickerDialog;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.TimePickerDialog;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.design.widget.TextInputEditText;
import android.support.v4.app.Fragment;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v4.app.TaskStackBuilder;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.TimePicker;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

import static android.content.Context.ALARM_SERVICE;


public class TaskInfoFragment extends Fragment {

    //Current task reference
    private Task currentTask;

    //View references - TextEdit, buttons, etc.
    private EditText mTaskTitle;
    private EditText dueDate;
    private EditText reminderDate;
    private EditText reminderTime;
    private EditText mTaskNotes;
    private Button mSaveChangesButton;
    private Button mCancelButton;

    //FireBase DB references to change task info
    private FirebaseDatabase mFirebaseDatabase;
    private DatabaseReference mTaskDatabaseReference;
    private DatabaseReference mAllTasksDatabaseReference;

    //References for dueDatePicker
    private DatePickerDialog.OnDateSetListener dateDue;
    private Calendar dueCalendar;

    //References for Reminder
    private DatePickerDialog.OnDateSetListener dateReminder;
    private Calendar remindDateCalendar;
    private TimePickerDialog.OnTimeSetListener timeReminder;
    private Calendar remindTimeCalendar;

    //Flags to indicate date changes - prevent bugs
    private boolean dueFlag=false;
    private boolean remindDateFlag=false;
    private boolean remindTimeFlag=false;



    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        //Inflate the layout for this fragment
        final View rootView = inflater.inflate(R.layout.task_info, container, false);

        // Initialize Firebase components
        mFirebaseDatabase = FirebaseDatabase.getInstance();

        //set up Task DB references
        mTaskDatabaseReference = mFirebaseDatabase.getReference().child("users")
                .child(MainActivity.getCurrentUserId()).child("TaskLists")
                .child(MainActivity.getCurrentTaskListId()).child("tasks").child(currentTask.getId());
        mAllTasksDatabaseReference=mFirebaseDatabase.getReference().child("users")
                .child(MainActivity.getCurrentUserId()).child("allTasks").child(currentTask.getId());

        //Get SimpleDateFormat to format task's dates and Calendar instance:
        String myFormat = "dd/MM/yyyy";
        final SimpleDateFormat sdf = new SimpleDateFormat(myFormat, Locale.getDefault());
        String myFormatTime = "HH:mm";
        final SimpleDateFormat sdfTime = new SimpleDateFormat(myFormatTime, Locale.getDefault());

        // Initialize references to views
        mTaskTitle = rootView.findViewById(R.id.input_task_title);
        mTaskTitle.setText(currentTask.getTitle());
        mSaveChangesButton = rootView.findViewById(R.id.save_button);
        mSaveChangesButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Save edited task info
                mTaskDatabaseReference.child("title").setValue(mTaskTitle.getText().toString());
                mAllTasksDatabaseReference.child("title").setValue(mTaskTitle.getText().toString());
                if(dueFlag){
                    mTaskDatabaseReference.child("dueDate").setValue(dueCalendar.getTime());
                    mAllTasksDatabaseReference.child("dueDate").setValue(dueCalendar.getTime());
                }
                if(remindDateFlag) {
                    mTaskDatabaseReference.child("reminderDate").setValue(remindDateCalendar.getTime());
                    mAllTasksDatabaseReference.child("reminderDate").setValue(remindDateCalendar.getTime());
                }
                if(remindTimeFlag){
                    mTaskDatabaseReference.child("reminderTime").setValue(remindTimeCalendar.getTime());
                    mAllTasksDatabaseReference.child("reminderTime").setValue(remindTimeCalendar.getTime());
                }
                mTaskDatabaseReference.child("notes").setValue(mTaskNotes.getText().toString());
                mAllTasksDatabaseReference.child("notes").setValue(mTaskNotes.getText().toString());

                //Check if user scheduled a reminder and if so - set a reminder
                if(!reminderDate.getText().toString().equals("") && !reminderTime.getText().toString().equals(""))
                {
                    setReminder(getContext(),AlarmReceiver.class,remindDateCalendar.getTime(),remindTimeCalendar.getTime()
                            ,currentTask,mAllTasksDatabaseReference);
                }

                //Recreate activity to update changes and go back to the TaskActivity
                getActivity().recreate();
                getActivity().onBackPressed();


            }
        });

        mCancelButton = rootView.findViewById(R.id.cancel_button);
        mCancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //Cancel changes and go back to task list
                Log.d("clicked back!","clicked back");
                FrameLayout frameLayout=getActivity().findViewById(R.id.frag_container);
                frameLayout.setClickable(false);
                getActivity().onBackPressed();


            }
        });


        //Initialize dueDatePicker related variables
        dueCalendar = Calendar.getInstance();
        dueDate = (EditText) rootView.findViewById(R.id.due_date_picker);
        if(currentTask.getDueDate()!=null){
            dueDate.setText(sdf.format(currentTask.getDueDate()));
        }

        dateDue = new DatePickerDialog.OnDateSetListener() {

            //Set the DatePicker

            @Override
            public void onDateSet(DatePicker view, int year, int monthOfYear,
                                  int dayOfMonth) {
                dueCalendar.set(Calendar.YEAR, year);
                dueCalendar.set(Calendar.MONTH, monthOfYear);
                dueCalendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                updateDueDateLabel();
                dueFlag=true;
            }

        };

        dueDate.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                new DatePickerDialog(getActivity(), dateDue, dueCalendar
                        .get(Calendar.YEAR), dueCalendar.get(Calendar.MONTH),
                        dueCalendar.get(Calendar.DAY_OF_MONTH)).show();
            }
        });

        //Initialize ReminderDate related variables
        remindDateCalendar = Calendar.getInstance();
        reminderDate = (EditText) rootView.findViewById(R.id.remind_date_picker);
        if(currentTask.getReminderDate()!=null){
            reminderDate.setText(sdf.format(currentTask.getReminderDate()));
        }
        dateReminder = new DatePickerDialog.OnDateSetListener() {
            //Set the DatePicker

            @Override
            public void onDateSet(DatePicker view, int year, int monthOfYear,
                                  int dayOfMonth) {
                remindDateCalendar.set(Calendar.YEAR, year);
                remindDateCalendar.set(Calendar.MONTH, monthOfYear);
                remindDateCalendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                updateReminderDateLabel();
                remindDateFlag=true;
            }

        };

        reminderDate.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                new DatePickerDialog(getActivity(), dateReminder, remindDateCalendar
                        .get(Calendar.YEAR), remindDateCalendar.get(Calendar.MONTH),
                        remindDateCalendar.get(Calendar.DAY_OF_MONTH)).show();
            }
        });

        //Initialize ReminderTime related variables
        remindTimeCalendar = Calendar.getInstance();
        reminderTime = (EditText) rootView.findViewById(R.id.remind_time_picker);
        if(currentTask.getReminderTime()!=null){

            reminderTime.setText(sdfTime.format(currentTask.getReminderTime()));
        }
        timeReminder = new TimePickerDialog.OnTimeSetListener() {
            @Override
            public void onTimeSet(TimePicker view, int hour, int min) {
                remindTimeCalendar.set(Calendar.HOUR_OF_DAY, hour);
                remindTimeCalendar.set(Calendar.MINUTE, min);
                updateReminderTimeLabel();
                remindTimeFlag=true;


            }
        };

        reminderTime.setOnClickListener(new View.OnClickListener()
            {
                @Override
                public void onClick (View v){
                    new TimePickerDialog(getActivity(),timeReminder,remindTimeCalendar.get
                            (Calendar.HOUR_OF_DAY),remindTimeCalendar.get(Calendar.MINUTE),true).show();
            }
            });

        mTaskNotes =rootView.findViewById(R.id.notes);
        mTaskNotes.setText(currentTask.getNotes());

        return rootView;

        }




    //Update the dueDate with the date the user selected
    private void updateDueDateLabel() {
        String myFormat = "dd/MM/yyyy";
        SimpleDateFormat sdf = new SimpleDateFormat(myFormat, Locale.getDefault());

        dueDate.setText(sdf.format(dueCalendar.getTime()));
    }

    private void updateReminderDateLabel() {
        String myFormat = "dd/MM/yyyy";
        SimpleDateFormat sdf = new SimpleDateFormat(myFormat, Locale.getDefault());

        reminderDate.setText(sdf.format(remindDateCalendar.getTime()));
    }

    private void updateReminderTimeLabel() {
        String myFormat = "HH:mm";
        SimpleDateFormat sdf = new SimpleDateFormat(myFormat, Locale.getDefault());

        reminderTime.setText(sdf.format(remindTimeCalendar.getTime()));
    }

    public void setCurrentTask(Task task) {
        this.currentTask = task;
    }
    public static void setReminder(Context context, Class<?> cls, Date remindDate, Date remindTime, Task task, DatabaseReference ref)
    {
        //Set the remindCalendar
        Calendar remindCalendar=Calendar.getInstance();
        Calendar remindDateCal=Calendar.getInstance();
        remindDateCal.setTime(remindDate);
        Calendar remindTimeCal=Calendar.getInstance();
        remindTimeCal.setTime(remindTime);
        remindCalendar.set(Calendar.YEAR,remindDateCal.get(Calendar.YEAR));
        remindCalendar.set(Calendar.MONTH,remindDateCal.get(Calendar.MONTH));
        remindCalendar.set(Calendar.DAY_OF_MONTH,remindDateCal.get(Calendar.DAY_OF_MONTH));
        remindCalendar.set(Calendar.HOUR_OF_DAY,remindTimeCal.get(Calendar.HOUR_OF_DAY));
        remindCalendar.set(Calendar.MINUTE,remindTimeCal.get(Calendar.MINUTE));
        remindCalendar.set(Calendar.SECOND,0);


        // Enable a receiver

        ComponentName receiver = new ComponentName(context, cls);
        PackageManager pm = context.getPackageManager();

        pm.setComponentEnabledSetting(receiver,
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                PackageManager.DONT_KILL_APP);

        //Set reminderDisplayed to false - this is a new reminder
        ref.child("reminderDisplayed").setValue(false);


        Intent intent = new Intent(context, cls);
        intent.putExtra("taskTitle",task.getTitle());
        intent.putExtra("taskIntId",task.getIntId());
        intent.putExtra("taskId",task.getId());
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, task.getIntId(), intent, PendingIntent.FLAG_UPDATE_CURRENT);
        AlarmManager am = (AlarmManager) context.getSystemService(ALARM_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            am.setExactAndAllowWhileIdle (AlarmManager.RTC_WAKEUP, remindCalendar.getTimeInMillis(), pendingIntent);
        }
        else{
            am.setExact (AlarmManager.RTC_WAKEUP, remindCalendar.getTimeInMillis(), pendingIntent);
        }

    }

    public static void showReminderNotification(Context context,Class<?> cls,String title,int taskIntId)
    {

        Intent notificationIntent = new Intent(context, cls);
        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, taskIntId, notificationIntent, 0);


        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(context,"123")
                .setSmallIcon(R.drawable.fui_ic_googleg_color_24dp)
                .setContentTitle("Reminder: "+title)
                .setContentText("Go do it TIGER!")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                // Set the intent that will fire when the user taps the notification
                .setContentIntent(pendingIntent)
                .setAutoCancel(true);

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
        notificationManager.notify(taskIntId, mBuilder.build());

    }

    public static void cancelReminder(Context context,Class<?> cls,int taskIntId)
    {
        // Disable a receiver

        ComponentName receiver = new ComponentName(context, cls);
        PackageManager pm = context.getPackageManager();

        pm.setComponentEnabledSetting(receiver,
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                PackageManager.DONT_KILL_APP);

        Intent intent = new Intent(context, cls);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, taskIntId, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        AlarmManager am = (AlarmManager) context.getSystemService(ALARM_SERVICE);
        am.cancel(pendingIntent);
        pendingIntent.cancel();
    }
}

