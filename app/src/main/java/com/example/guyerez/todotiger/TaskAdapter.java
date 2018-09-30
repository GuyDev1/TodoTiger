package com.example.guyerez.todotiger;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.TextView;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Comparator;
import java.util.Date;
import java.util.Locale;

/**
 * {@link TaskAdapter} is an {@link ArrayAdapter} that can provide the layout for each task item
 * based on a data source, which is a list of {@link Task} objects.
 */

public class TaskAdapter extends ArrayAdapter<Task> {

    //Define FireBase instance variables
    private DatabaseReference mTaskDatabaseReference;
    private FirebaseDatabase mFirebaseDatabase;

    //Variables indicating which tasks to show in the ListView
    public static final int SHOW_ALL_TASKS = 0;
    public static final int SHOW_OPEN_TASKS = 1;
    public static final int SHOW_COMPLETED_TASKS = 2;
    public static final int TASKS_DUE_TODAY = 3;
    public static final int TASKS_DUE_WEEK = 4;
    public static final int TASKS_DUE_MONTH = 5;
    public static final int TASKS_COMPLETED_TODAY = 6;
    public static final int TASKS_COMPLETED_WEEK = 7;
    public static final int TASKS_COMPLETED_MONTH = 8;

    //Context to access TaskActivity method
    private Context mContext;

    //Get a calendar instance for setting dates
    private Calendar calendar;
    /**
     * Create a new {@link TaskAdapter} object.
     *
     * @param context is the current context (i.e. Activity) that the adapter is being created in.
     * @param tasks is the list of {@link Task}s to be displayed.
     */


    public TaskAdapter(Context context, ArrayList<Task> tasks) {
        super(context, 0, tasks);
        this.mContext=context;
    }

    @Override
    public View getView(int position, View convertView, final ViewGroup parent) {
        // Check if an existing view is being reused, otherwise inflate the view
        View listItemView = convertView;
        if (listItemView == null) {
            listItemView = LayoutInflater.from(getContext()).inflate(
                    R.layout.task_item, parent, false);
        }

        // Get the {@link Task} object located at this position in the list
        final Task currentTask = getItem(position);

        //Get SimpleDateFormat to format task's dates and Calendar instance:
        String myFormat = "dd/MM/yyyy";
        final SimpleDateFormat sdf = new SimpleDateFormat(myFormat, Locale.getDefault());
        calendar=Calendar.getInstance();

        // Locate the TextView in the task_item.xml layout with the ID task_title.
        final TextView titleTextView = (TextView) listItemView.findViewById(R.id.task_title);
        // Get the task's title from the currentTask object and set it in the text view
        titleTextView.setText(currentTask.getTitle());
        //If the task is completed - title Strikethrough
        titleTextView.setBackgroundResource(strikeCompleted(currentTask.getCompleted()));

        //Initialize the check box and check it if the task was completed.

        CheckBox checkBox = (CheckBox) listItemView.findViewById(R.id.check_box);
        checkBox.setOnCheckedChangeListener(null);
        checkBox.setChecked(currentTask.getCompleted());

        //Initialize the creation date TextView in the task_item.xml layout with the ID creation_date
        TextView creationDateTextView = (TextView) listItemView.findViewById(R.id.creation_date);
        //Get the task's creation date from the currentTask object and set it in the text view

        creationDateTextView.setText("Created: "+sdf.format(currentTask.getCreationDate()));

        //Initialize the creation date TextView in the task_item.xml layout with the ID creation_date
        final TextView dueDateTextView = (TextView) listItemView.findViewById(R.id.due_date);
        //Get the task's creation date from the currentTask object and set it in the text view
        dueDateTextView.setText(getDueOrCompletedDate(currentTask));

        // Initialize Firebase DB
        mFirebaseDatabase = FirebaseDatabase.getInstance();


        // Find the CheckBox in the task_item.xml layout with the ID check_box.

        checkBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView,boolean isChecked) {
                //Get the task DB reference to edit task completion status
                mTaskDatabaseReference=mFirebaseDatabase.getReference()
                        .child("users").child(MainActivity.getCurrentUserId())
                        .child(MainActivity.getCurrentTaskListId()).child("tasks").child(currentTask.getId());
                    if (isChecked) {
                        Date completionDate =calendar.getTime();
                        titleTextView.setBackgroundResource(R.drawable.strike_through);
                        mTaskDatabaseReference.child("completed").setValue(true);
                        mTaskDatabaseReference.child("completionDate").setValue(completionDate);
                        dueDateTextView.setText("Completed: "+sdf.format(completionDate));
                        //After a small delay for better animation effect
                        //Remove the task from current adapter if it's not in SHOW_ALL_TASKs
                        // Since the user completed the task
                        if(TaskActivity.tasksToShow!=SHOW_ALL_TASKS){
                            final Handler handler = new Handler();
                            handler.postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                        remove(currentTask);
                                }}, 500);
                        }


                    } else {
                        titleTextView.setBackgroundResource(R.drawable.task_clicked);
                        mTaskDatabaseReference.child("completed").setValue(false);
                        mTaskDatabaseReference.child("completionDate").setValue(null);
                        if(currentTask.getDueDate()!=null){
                            dueDateTextView.setText("Due: "+sdf.format(currentTask.getDueDate()));
                        }
                        else{
                            dueDateTextView.setText("Due: ");
                        }
                        if(TaskActivity.tasksToShow!=SHOW_ALL_TASKS){
                            final Handler handler = new Handler();
                            handler.postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    remove(currentTask);
                                }}, 500);
                        }

                    }
            }
            }

        );
        titleTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(mContext instanceof TaskActivity){
                    //Use the TaskActivity getTaskInfo method to start TaskInfoFragment
                    ((TaskActivity)mContext).getTaskInfo(currentTask);
                }

            }
        });

        // Return the whole list item layout (containing 3 text views and 1 checkbox) so that it can be shown in the ListView.
        return listItemView;

    }
    private int strikeCompleted(boolean completed){
        if (completed){
            return R.drawable.strike_through;
        }
        else{
            return R.drawable.task_clicked;
        }
    }

    private String getDueOrCompletedDate(Task currentTask){
        String myFormat = "dd/MM/yyyy";
        SimpleDateFormat sdf = new SimpleDateFormat(myFormat, Locale.getDefault());
        if(currentTask.getCompleted()){
            return "Completed: "+sdf.format(currentTask.getCompletionDate());
        }
        else{
            if(currentTask.getDueDate()!=null){
                return "Due: "+sdf.format(currentTask.getDueDate());
            }
            else{
                return "Due: ";
            }

        }
    }

    @Override
    public void add(@Nullable Task object) {
        super.add(object);
        //Sort the tasks according to relevant parameters
        this.sort(new Comparator<Task>() {
            @Override
            public int compare(Task o1, Task o2) {

                return o1.getCreationDate().compareTo(o2.getCreationDate());
            }
        });
        if(TaskActivity.tasksToShow!=TASKS_COMPLETED_TODAY && TaskActivity.tasksToShow!=TASKS_DUE_TODAY){
            this.sort(new Comparator<Task>() {
                @Override
                public int compare(Task o1, Task o2) {

                    return sortByCategories(o1,o2);
                }
            });
        }

    }

    //Apply sorting based on the task categories chosen by the user
    private int sortByCategories(Task task1,Task task2){
        switch (TaskActivity.tasksToShow){
            case SHOW_ALL_TASKS:
                if(task1.getCompleted() && task2.getCompleted())
                {
                    return task1.getCompletionDate().compareTo(task2.getCompletionDate());
                }
                else if(task1.getDueDate()!=null && task2.getDueDate()!=null){
                    return task1.getDueDate().compareTo(task2.getDueDate());
                }
                else {
                    return 0;
                }

            case SHOW_OPEN_TASKS:
                if(task1.getDueDate()!=null && task2.getDueDate()!=null){
                    return task1.getDueDate().compareTo(task2.getDueDate());
                }
                else{
                    return 0;
                }
            case TASKS_DUE_WEEK:
            case TASKS_DUE_MONTH:
                return task1.getDueDate().compareTo(task2.getDueDate());
            case SHOW_COMPLETED_TASKS:
            case TASKS_COMPLETED_WEEK:
            case TASKS_COMPLETED_MONTH:
                return task1.getCompletionDate().compareTo(task2.getCompletionDate());


        }
        return 0;

    }
}
