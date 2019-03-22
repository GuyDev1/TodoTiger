package com.example.guyerez.todotiger;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.icu.util.DateInterval;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.support.v7.view.menu.MenuBuilder;
import android.support.v7.view.menu.MenuPopupHelper;
import android.support.v7.widget.PopupMenu;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Period;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Comparator;
import java.util.Date;
import java.util.Locale;

import static com.example.guyerez.todotiger.TaskActivity.PRIORITY_URGENT;
import static com.example.guyerez.todotiger.TaskActivity.PRIORITY_HIGH;
import static com.example.guyerez.todotiger.TaskActivity.PRIORITY_DEFAULT;
/**
 * {@link TaskAdapter} is an {@link ArrayAdapter} that can provide the layout for each task item
 * based on a data source, which is a list of {@link Task} objects.
 */

public class TaskAdapter extends ArrayAdapter<Task> {

    //Define FireBase instance variables
    private DatabaseReference mTaskDatabaseReference;
    private FirebaseDatabase mFirebaseDatabase;
    private DatabaseReference mAllTasksDatabaseReference;

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
    private Activity activity;

    //Get a calendar instance for setting dates
    private Calendar calendar;
    private String currentUser;

    //Show created/due/completed on Task item UI
    public static boolean showCreated;
    public static boolean showDue;
    public static boolean showCompleted;

    //Define task priority menu components
    private MenuBuilder menuBuilder;
    private MenuPopupHelper optionsMenu;



    /**
     * Create a new {@link TaskAdapter} object.
     *
     * @param activity is the current activity that the adapter is being created in.
     * @param tasks is the list of {@link Task}s to be displayed.
     */


    public TaskAdapter(Activity activity, ArrayList<Task> tasks) {
        super(activity, 0, tasks);
        this.activity=activity;
        //Initialize sharedPreferences
        initSharedPreferences();
        //Initialize calendar for task's dates and for using the Calendar
        initializeCalendar();
        // Initialize Firebase DB
        mFirebaseDatabase = FirebaseDatabase.getInstance();
    }

    @SuppressLint("RestrictedApi")
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

        // Locate the TextView in the task_item.xml layout with the ID task_title.
        final TextView titleTextView = (TextView) listItemView.findViewById(R.id.task_title);
        // Get the task's title from the currentTask object and set it in the text view
        titleTextView.setText(currentTask.getTitle());
        //If the task is completed - title Strikethrough
        titleTextView.setBackgroundResource(AdapterUtil.strikeCompleted(currentTask.getCompleted()));
        //Set Title height to fit date TextView's appropriately
        if(showCreated || showDue || showCompleted){
            titleTextView.setHeight((int)TypedValue.applyDimension
                    (TypedValue.COMPLEX_UNIT_DIP, 59,
                            getContext().getResources().getDisplayMetrics()));
        }

        //Initialize the check box and check it if the task was completed.

        CheckBox checkBox = (CheckBox) listItemView.findViewById(R.id.check_box);
        checkBox.setOnCheckedChangeListener(null);
        checkBox.setChecked(currentTask.getCompleted());

        //Initialize the creation date TextView in the task_item.xml layout with the ID creation_date
        TextView creationDateTextView = (TextView) listItemView.findViewById(R.id.creation_date);
        //Get the task's creation date from the currentTask object and set it in the text view
        creationDateTextView.setText(AdapterUtil.getCreationDate(currentTask,calendar));
        if(!showCreated){
            creationDateTextView.setVisibility(View.GONE);
        }


        //Initialize the creation date TextView in the task_item.xml layout with the ID creation_date
         final TextView dueDateTextView = (TextView) listItemView.findViewById(R.id.due_date);
        //Get the task's creation date from the currentTask object and set it in the text view
        dueDateTextView.setText(AdapterUtil.getDueOrCompletedDate(currentTask,dueDateTextView,calendar,activity,null));
        if(!showDue){
            dueDateTextView.setVisibility(View.GONE);
        }



        // Get the Task's priorityImage and allow the user to change the task's priority
        // by clicking on it
        final ImageView priorityImage=listItemView.findViewById(R.id.imageView);
        priorityImage.setImageResource(AdapterUtil.getTaskPriorityImage(currentTask));
        priorityImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                initPriorityMenu(view);

                //Init the task DB reference to edit task completion status
                initDatabaseReferences(currentTask);

                // Set Item Click Listener
                menuBuilder.setCallback(new MenuBuilder.Callback() {
                    @Override
                    public boolean onMenuItemSelected(MenuBuilder menu, MenuItem item) {
                        switch (item.getItemId()) {
                            case R.id.priority_urgent:
                                AdapterUtil.setPriority(PRIORITY_URGENT,currentTask,mTaskDatabaseReference,mAllTasksDatabaseReference,priorityImage);
                                return true;
                            case R.id.priority_high:
                                AdapterUtil.setPriority(PRIORITY_HIGH,currentTask,mTaskDatabaseReference,mAllTasksDatabaseReference,priorityImage);
                                return true;
                            case R.id.priority_default:
                                AdapterUtil.setPriority(PRIORITY_DEFAULT,currentTask,mTaskDatabaseReference,mAllTasksDatabaseReference,priorityImage);
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

        // Find the CheckBox in the task_item.xml layout with the ID check_box.

        checkBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView,boolean isChecked) {
                //Get the task DB reference to edit task completion status
                initDatabaseReferences(currentTask);
                    if (isChecked) {
                        //Update the Task as Checked (completed) in the DB and UI
                        AdapterUtil.updateTaskChecked(titleTextView,dueDateTextView,currentTask,calendar,mTaskDatabaseReference,mAllTasksDatabaseReference,activity,showCompleted);
                        //cancel task's reminder if it had one, since it's completed
                        AdapterUtil.cancelTaskReminder(currentTask,activity);
                        //After a small delay for better animation effect
                        //Remove the task from current adapter if it's not in SHOW_ALL_TASKs
                        // Since the user completed the task
                        if(TaskActivity.tasksToShow!=SHOW_ALL_TASKS){
                            removeCompletedTaskAnim(currentTask);
                        }

                    } else {
                        //Update the Task as unChecked (not completed) in the DB and UI
                        AdapterUtil.updateTaskUnchecked(titleTextView,dueDateTextView,currentTask,mTaskDatabaseReference,mAllTasksDatabaseReference,calendar,activity);
                        //Reset the task's reminder if it had any, and it wasn't displayed yet
                        if(!currentTask.getReminderDisplayed()){
                            AdapterUtil.resetTaskReminder(currentTask,activity,mAllTasksDatabaseReference);
                        }
                        //If we're not in ALL_TASKS, we must be in SHOW_COMPLETED if we can un-complete the task
                        if(TaskActivity.tasksToShow!=SHOW_ALL_TASKS){
                            removeCompletedTaskAnim(currentTask);
                        }
                    }
            }
            }

        );
        //When the user clicks the task - enter the taskInfoFragment
        titleTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(activity instanceof TaskActivity){
                    //Use the TaskActivity getTaskInfo method to start TaskInfoFragment
                    ((TaskActivity)activity).getTaskInfo(currentTask);
                }
                if(activity instanceof SearchTask){
                    ((SearchTask)activity).getTaskInfo(currentTask);
                }

            }
        });

        // Return the whole list item layout (containing 3 text views and 1 checkbox) so that it can be shown in the ListView.
        return listItemView;

    }

    @Override
    public void add(@Nullable Task object) {
        super.add(object);
        if(SearchTask.SEARCH_ACTIVE){
            return;
        }
        //Sort the tasks according to relevant parameters
        this.sort(new Comparator<Task>() {
            @Override
            public int compare(Task o1, Task o2) {

                if(o1.getCreationDate()==null || o2.getCreationDate()==null){
                    return 0;
                }
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

    private void initDatabaseReferences(Task task){
        mTaskDatabaseReference=mFirebaseDatabase.getReference()
                .child("users").child(currentUser).child("TaskLists")
                .child(task.getTaskListId()).child("tasks").child(task.getId());
        mAllTasksDatabaseReference=mFirebaseDatabase.getReference()
                .child("users").child(currentUser)
                .child("allTasks").child(task.getId());
    }



    //After a small delay for better animation effect
    //Remove the task from current adapter if it's not in SHOW_ALL_TASKs
    // Since the user completed the task or un-completed the task and is in SHOW_COMPLETED
    private void removeCompletedTaskAnim(final Task currentTask){
            final Handler handler = new Handler();
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    remove(currentTask);
                }}, 500);

    }



    private void initSharedPreferences(){
        //Get Settings for Task UI preferences
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(getContext());
        showCreated=settings.getBoolean("show_created_date",true);
        showDue=settings.getBoolean("show_due_date",true);
        showCompleted=settings.getBoolean("show_completed_date",true);
        //Get current logged in user and the current TaskList from SharedPreferences
        final SharedPreferences currentData=getContext().getSharedPreferences("MyPref", Context.MODE_PRIVATE);
        currentUser=currentData.getString("userId",null);
    }

    private void initializeCalendar() {
        //Get SimpleDateFormat to format task's dates and Calendar instance:
        final String myFormat = "dd/MM/yyyy";
        final SimpleDateFormat sdf = new SimpleDateFormat(myFormat, Locale.getDefault());
        calendar=Calendar.getInstance();
    }

    @SuppressLint("RestrictedApi")
    private void initPriorityMenu(View view) {
        menuBuilder =new MenuBuilder(getContext());
        MenuInflater inflater = new MenuInflater(getContext());
        inflater.inflate(R.menu.priority_menu, menuBuilder);
        optionsMenu = new MenuPopupHelper(getContext(), menuBuilder, view);
        optionsMenu.setForceShowIcon(true);
    }


}
