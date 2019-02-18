package com.example.guyerez.todotiger;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.v4.content.ContextCompat;
import android.support.v7.view.menu.MenuBuilder;
import android.support.v7.view.menu.MenuPopupHelper;
import android.util.Log;
import android.util.SparseArray;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.CheckBox;
import android.widget.CheckedTextView;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import org.w3c.dom.Text;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

import static com.example.guyerez.todotiger.TaskActivity.PRIORITY_DEFAULT;
import static com.example.guyerez.todotiger.TaskActivity.PRIORITY_HIGH;
import static com.example.guyerez.todotiger.TaskActivity.PRIORITY_URGENT;

public class ExpandableListAdapter extends BaseExpandableListAdapter {

    private final SparseArray<TaskGroup> taskGroups;
    private  String currentUser;
    private  boolean showCreated;
    private  boolean showDue;
    private  boolean showCompleted;
    public LayoutInflater inflater;
    public Activity activity;

    //Get a calendar instance for setting dates
    private Calendar calendar;

    //Current completion date
    private Date completionDate;

    //Define FireBase instance variables
    private DatabaseReference mTaskDatabaseReference;
    private FirebaseDatabase mFirebaseDatabase;
    private DatabaseReference mAllTasksDatabaseReference;

    public ExpandableListAdapter(Activity act, SparseArray<TaskGroup> taskGroups) {
        activity = act;
        this.taskGroups = taskGroups;
        inflater = act.getLayoutInflater();
        //Get Settings for Task UI preferences
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(activity);
        showCreated=settings.getBoolean("show_created_date",true);
        showDue=settings.getBoolean("show_due_date",true);
        showCompleted=settings.getBoolean("show_completed_date",true);
        //Get current logged in user from SharedPreferences
        SharedPreferences currentData = activity.getSharedPreferences("MyPref", Context.MODE_PRIVATE);
        currentUser = currentData.getString("userId", null);

        //Get SimpleDateFormat to format task's dates and Calendar instance:
        final String myFormat = "dd/MM/yyyy";
        final SimpleDateFormat sdf = new SimpleDateFormat(myFormat, Locale.getDefault());
        calendar=Calendar.getInstance();

        // Initialize Firebase DB
        mFirebaseDatabase = FirebaseDatabase.getInstance();

    }

    @Override
    public Object getChild(int groupPosition, int childPosition) {
        return taskGroups.get(groupPosition).tasks.get(childPosition);
    }

    @Override
    public long getChildId(int groupPosition, int childPosition) {
        return 0;
    }

    @Override
    public View getChildView(int groupPosition, final int childPosition,
                             boolean isLastChild, View convertView, ViewGroup parent) {
        final Task task = (Task) getChild(groupPosition, childPosition);
        if (convertView == null) {
            convertView = inflater.inflate(R.layout.task_item, null);
        }
        final TextView title=convertView.findViewById(R.id.task_title);
        title.setText(task.getTitle());

        //If the task is completed - title Strikethrough
        title.setBackgroundResource(strikeCompleted(task.getCompleted()));

        //Set title onClickListener
        title.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(activity instanceof TaskActivity){
                    //Use the TaskActivity getTaskInfo method to start TaskInfoFragment
                    ((TaskActivity)activity).getTaskInfo(task);
                }
                if(activity instanceof SearchTask){
                    ((SearchTask)activity).getTaskInfo(task);
                }

            }
        });
        //Set Title height to fit date TextView's appropriately
        if(showCreated || showDue || showCompleted){
            title.setHeight((int) TypedValue.applyDimension
                    (TypedValue.COMPLEX_UNIT_DIP, 59,
                            activity.getResources().getDisplayMetrics()));
        }

        //Initialize the creation date TextView in the task_item.xml layout with the ID creation_date
        TextView creationDateTextView = (TextView) convertView.findViewById(R.id.creation_date);
        //Get the task's creation date from the currentTask object and set it in the text view
        creationDateTextView.setText(getCreationDate(task));
        if(!showCreated){
            creationDateTextView.setVisibility(View.GONE);
        }


        //Initialize the creation date TextView in the task_item.xml layout with the ID creation_date
        final TextView dueDateTextView = (TextView) convertView.findViewById(R.id.due_date);
        //Get the task's creation date from the currentTask object and set it in the text view
        dueDateTextView.setText(getDueOrCompletedDate(task,dueDateTextView,null));
        if(!showDue){
            dueDateTextView.setVisibility(View.GONE);
        }

        //Initialize the check box and check it if the task was completed.

        CheckBox checkBox = (CheckBox) convertView.findViewById(R.id.check_box);
        checkBox.setOnCheckedChangeListener(null);
        checkBox.setChecked(task.getCompleted());

        checkBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView,boolean isChecked) {
                //Get the task DB reference to edit task completion status
                mTaskDatabaseReference=mFirebaseDatabase.getReference()
                        .child("users").child(currentUser).child("TaskLists")
                        .child(task.getTaskListId()).child("tasks").child(task.getId());
                mAllTasksDatabaseReference=mFirebaseDatabase.getReference()
                        .child("users").child(currentUser)
                        .child("allTasks").child(task.getId());
                    if (isChecked) {
                        completionDate =calendar.getTime();
                        title.setBackgroundResource(R.drawable.strike_through);
                        mTaskDatabaseReference.child("completed").setValue(true);
                        mTaskDatabaseReference.child("completionDate").setValue(completionDate);
                        mAllTasksDatabaseReference.child("completed").setValue(true);
                        mAllTasksDatabaseReference.child("completionDate").setValue(completionDate);
                        if(showCompleted){
                            dueDateTextView.setText(getDueOrCompletedDate(task,dueDateTextView,Boolean.TRUE));
                            dueDateTextView.setVisibility(View.VISIBLE);
                        }
                        //cancel reminder if it had one
                        if(task.getReminderDate()!=null){
                            TaskInfoFragment.cancelReminder(activity,AlarmReceiver.class,task.getIntId());
                        }


                    } else {
                        title.setBackgroundResource(R.drawable.task_clicked);
                        mTaskDatabaseReference.child("completed").setValue(false);
                        mTaskDatabaseReference.child("completionDate").setValue(null);
                        mAllTasksDatabaseReference.child("completed").setValue(false);
                        mAllTasksDatabaseReference.child("completionDate").setValue(null);
                        dueDateTextView.setText(getDueOrCompletedDate(task,dueDateTextView,Boolean.FALSE));

                        //Reset the reminder if it had any
                        if(task.getReminderDate()!=null && task.getReminderTime()!=null){
                            TaskInfoFragment.setReminder(activity,AlarmReceiver.class,task.getReminderDate(),
                                    task.getReminderTime(),task,mAllTasksDatabaseReference);
                        }


                    }
            }
            });


        final ImageView priorityImage=convertView.findViewById(R.id.imageView);
        priorityImage.setImageResource(getTaskPriorityImage(task));
        priorityImage.setOnClickListener(new View.OnClickListener() {
            @SuppressLint("RestrictedApi")
            @Override
            public void onClick(View view) {
                @SuppressLint("RestrictedApi") MenuBuilder menuBuilder =new MenuBuilder(activity);
                MenuInflater inflater = new MenuInflater(activity);
                inflater.inflate(R.menu.priority_menu, menuBuilder);
                @SuppressLint("RestrictedApi") MenuPopupHelper optionsMenu = new MenuPopupHelper(activity, menuBuilder, view);
                optionsMenu.setForceShowIcon(true);
                //Get the task DB reference to edit task completion status
                mTaskDatabaseReference=mFirebaseDatabase.getReference()
                        .child("users").child(currentUser).child("TaskLists")
                        .child(task.getTaskListId()).child("tasks").child(task.getId());
                mAllTasksDatabaseReference=mFirebaseDatabase.getReference()
                        .child("users").child(currentUser)
                        .child("allTasks").child(task.getId());

// Set Item Click Listener
                menuBuilder.setCallback(new MenuBuilder.Callback() {
                    @Override
                    public boolean onMenuItemSelected(MenuBuilder menu, MenuItem item) {
                        switch (item.getItemId()) {
                            case R.id.priority_urgent:
                                mTaskDatabaseReference.child("priority").setValue(PRIORITY_URGENT);
                                mAllTasksDatabaseReference.child("priority").setValue(PRIORITY_URGENT);
                                task.setPriority(PRIORITY_URGENT); //Update currentTask - keeping taskInfoFragment up to date
                                priorityImage.setImageResource(R.mipmap.ic_launcher); //Updating the image to show instant change of priority
                                return true;
                            case R.id.priority_high:
                                mTaskDatabaseReference.child("priority").setValue(PRIORITY_HIGH);
                                mAllTasksDatabaseReference.child("priority").setValue(PRIORITY_HIGH);
                                task.setPriority(PRIORITY_HIGH); //Update currentTask - keeping taskInfoFragment up to date
                                priorityImage.setImageResource(R.mipmap.ic_launcher_foreground); //Updating the image to show instant change of priority
                                return true;
                            case R.id.priority_default:
                                mTaskDatabaseReference.child("priority").setValue(PRIORITY_DEFAULT);
                                mAllTasksDatabaseReference.child("priority").setValue(PRIORITY_DEFAULT);
                                task.setPriority(PRIORITY_DEFAULT); //Update currentTask - keeping taskInfoFragment up to date
                                return true;
                            default:
                                return false;
                        }
                    }

                    @Override
                    public void onMenuModeChange(MenuBuilder menu) {}
                });

                optionsMenu.show();
            }
        });


        return convertView;
    }

    @Override
    public int getChildrenCount(int groupPosition) {
        return taskGroups.get(groupPosition).tasks.size();
    }

    @Override
    public Object getGroup(int groupPosition) {
        return taskGroups.get(groupPosition);
    }

    @Override
    public int getGroupCount() {
        return taskGroups.size();
    }

    @Override
    public void onGroupCollapsed(int groupPosition) {
        super.onGroupCollapsed(groupPosition);
    }

    @Override
    public void onGroupExpanded(int groupPosition) {
        super.onGroupExpanded(groupPosition);
    }

    @Override
    public long getGroupId(int groupPosition) {
        return 0;
    }

    @Override
    public View getGroupView(int groupPosition, boolean isExpanded,
                             View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = inflater.inflate(R.layout.task_group_row, null);
        }
        final TaskGroup taskGroup = (TaskGroup) getGroup(groupPosition);
        CheckedTextView taskListTitleTextView=convertView.findViewById(R.id.task_list_group_title);
        //onClick - go to relevant group's TaskList
        taskListTitleTextView.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                //Update current TaskList in SharedPreferences
                SharedPreferences pref = activity.getApplicationContext().getSharedPreferences("MyPref", Context.MODE_PRIVATE);
                SharedPreferences.Editor editor = pref.edit();
                editor.putString("currentTaskList", taskGroup.taskListId);
                editor.putString("currentTaskListTitle", taskGroup.taskListTitle);
                editor.commit();


                // Create a new intent to view the tasks in the chosen list
                Intent taskIntent = new Intent(activity, TaskActivity.class);

                // Send the intent to launch a new activity
                activity.startActivity(taskIntent);
            }
        });
        ((CheckedTextView) convertView).setText(taskGroup.taskListTitle);
        ((CheckedTextView) convertView).setChecked(isExpanded);
        return convertView;
    }

    @Override
    public boolean hasStableIds() {
        return false;
    }

    @Override
    public boolean isChildSelectable(int groupPosition, int childPosition) {
        return false;
    }

    private int strikeCompleted(boolean completed){
        if (completed){
            return R.drawable.strike_through;
        }
        else{
            return R.drawable.task_clicked;
        }
    }

    private String getDueOrCompletedDate(Task currentTask,TextView dueDateTextView, Boolean... currentlyCompleted){
        Boolean currentlyComplete=null;
        String myFormat = "dd/MM/yyyy";
        SimpleDateFormat sdf = new SimpleDateFormat(myFormat, Locale.getDefault());
        if(currentlyCompleted!=null){
            currentlyComplete=currentlyCompleted[0];
        }

        if(currentlyComplete!=null){
            if(currentlyComplete==Boolean.TRUE){
                dueDateTextView.setTextColor(Color.parseColor("#000000"));
                dueDateTextView.setAlpha(0.54f);
                return "Completed: "+sdf.format(calendar.getTime());
            }
            else{
                return getDueDate(currentTask.getDueDate(),dueDateTextView);
            }

        }
        if(currentTask.getCompleted()){
            dueDateTextView.setTextColor(Color.parseColor("#000000"));
            dueDateTextView.setAlpha(0.54f);
            //If we completed the task through search, set the current date to prevent a NullPointerException
            if(currentTask.getCompletionDate()==null){
                return "Completed: "+sdf.format(completionDate);
            }
            return "Completed: "+sdf.format(currentTask.getCompletionDate());
        }
        else{
            return getDueDate(currentTask.getDueDate(),dueDateTextView);

        }
    }

    public int getTaskPriorityImage(Task currentTask){
        switch (currentTask.getPriority()){
            case PRIORITY_URGENT:
                return R.mipmap.ic_launcher_round;
            case PRIORITY_HIGH:
                return R.mipmap.ic_launcher_foreground;
            default:
                break;
        }
        return R.mipmap.ic_launcher; //Default priority icon
    }

    private String getDueDate(Date dueDate, TextView dueDateTextView){
        if(dueDate!=null){
            int dayDifference=
                    ((int)((dueDate.getTime()/(24*60*60*1000))
                            -(int)(calendar.getTime().getTime()/(24*60*60*1000))));
            if(dayDifference>=0){
                switch (dayDifference) {
                    case 0:
                        dueDateTextView.setAlpha(1);
                        dueDateTextView.setTextColor(ContextCompat.getColor(activity,R.color.green));
                        return "Due today";
                    case 1:
                        dueDateTextView.setAlpha(1);
                        return "Due tomorrow";
                    default:
                        return String.format(Locale.getDefault(), "Due in %d days", dayDifference);
                }
            }
            else{
                dueDateTextView.setTextColor(ContextCompat.getColor(activity,R.color.red));
                dueDateTextView.setAlpha(1);
                if(dayDifference==-1){
                    return "Due yesterday";
                }
                else{
                    return String.format(Locale.getDefault(), "Due %d days ago", -dayDifference);
                }

            }

        }
        else{
            dueDateTextView.setTextColor(Color.parseColor("#000000"));
            dueDateTextView.setAlpha(0.54f);
            return "Due: ";
        }


    }

    private String getCreationDate(Task currentTask){
        int dayDifference=
                ((int)((calendar.getTime().getTime()/(24*60*60*1000))
                        -(int)(currentTask.getCreationDate().getTime()/(24*60*60*1000))));
        switch (dayDifference){
            case 0:
                return "Created today";
            case 1:
                return "Created yesterday";
            default:
                return String.format(Locale.getDefault(),"Created %d days ago",dayDifference);

        }

    }

}

