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
    private Context mContext;

    //Get a calendar instance for setting dates
    private Calendar calendar;
    private String currentUser;
    private String thisTaskList;

    //Current completion date
    private Date completionDate;

    //Show created/due/completed on Task item UI
    public static boolean showCreated;
    public static boolean showDue;
    public static boolean showCompleted;

    //Define task priority menu components
    private MenuBuilder menuBuilder;
    private MenuPopupHelper optionsMenu;

    //Define relevant UI components
    private ImageView priorityImage;


    /**
     * Create a new {@link TaskAdapter} object.
     *
     * @param context is the current context (i.e. Activity) that the adapter is being created in.
     * @param tasks is the list of {@link Task}s to be displayed.
     */


    public TaskAdapter(Context context, ArrayList<Task> tasks) {
        super(context, 0, tasks);
        this.mContext=context;
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
        titleTextView.setBackgroundResource(strikeCompleted(currentTask.getCompleted()));
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
        creationDateTextView.setText(getCreationDate(currentTask));
        if(!showCreated){
            creationDateTextView.setVisibility(View.GONE);
        }


        //Initialize the creation date TextView in the task_item.xml layout with the ID creation_date
         final TextView dueDateTextView = (TextView) listItemView.findViewById(R.id.due_date);
        //Get the task's creation date from the currentTask object and set it in the text view
        dueDateTextView.setText(getDueOrCompletedDate(currentTask,dueDateTextView,null));
        if(!showDue){
            dueDateTextView.setVisibility(View.GONE);
        }


        final ImageView priorityImage=listItemView.findViewById(R.id.imageView);
        priorityImage.setImageResource(getTaskPriorityImage(currentTask));
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
                                setPriority(PRIORITY_URGENT,currentTask);
                                return true;
                            case R.id.priority_high:
                                setPriority(PRIORITY_HIGH,currentTask);
                                return true;
                            case R.id.priority_default:
                                setPriority(PRIORITY_DEFAULT,currentTask);
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
                        updateTaskChecked(titleTextView,dueDateTextView,currentTask);
                        //cancel task's reminder if it had one, since it's completed
                        cancelTaskReminder(currentTask);
                        //After a small delay for better animation effect
                        //Remove the task from current adapter if it's not in SHOW_ALL_TASKs
                        // Since the user completed the task
                        if(TaskActivity.tasksToShow!=SHOW_ALL_TASKS){
                            removeCompletedTaskAnim(currentTask);
                        }

                    } else {
                        //Update the Task as unChecked (not completed) in the DB and UI
                        updateTaskUnchecked(titleTextView,dueDateTextView,currentTask);
                        //Reset the reminder if it had any
                        resetTaskReminder(currentTask);
                        //If we're not in ALL_TASKS, we must be in SHOW_COMPLETED if we can un-complete the task
                        if(TaskActivity.tasksToShow!=SHOW_ALL_TASKS){
                            removeCompletedTaskAnim(currentTask);
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
                if(mContext instanceof SearchTask){
                    ((SearchTask)mContext).getTaskInfo(currentTask);
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
    private String getDueDate(Date dueDate,TextView dueDateTextView){
        if(dueDate!=null){
            int dayDifference=
                    ((int)((dueDate.getTime()/(24*60*60*1000))
                            -(int)(calendar.getTime().getTime()/(24*60*60*1000))));
            if(dayDifference>=0){
                switch (dayDifference) {
                    case 0:
                        dueDateTextView.setAlpha(1);
                        dueDateTextView.setTextColor(ContextCompat.getColor(getContext(),R.color.green));
                        return "Due today";
                    case 1:
                        dueDateTextView.setAlpha(1);
                        return "Due tomorrow";
                    default:
                        return String.format(Locale.getDefault(), "Due in %d days", dayDifference);
                }
            }
            else{
                 dueDateTextView.setTextColor(ContextCompat.getColor(getContext(),R.color.red));
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

    private void initDatabaseReferences(Task task){
        mTaskDatabaseReference=mFirebaseDatabase.getReference()
                .child("users").child(currentUser).child("TaskLists")
                .child(task.getTaskListId()).child("tasks").child(task.getId());
        mAllTasksDatabaseReference=mFirebaseDatabase.getReference()
                .child("users").child(currentUser)
                .child("allTasks").child(task.getId());
    }

    private void updateTaskChecked(TextView title,TextView dueDateTextView,Task task) {
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
    }

    private void cancelTaskReminder(Task task) {
        if(task.getReminderDate()!=null){
            TaskInfoFragment.cancelReminder(getContext(),AlarmReceiver.class,task.getIntId());
        }
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

    private void updateTaskUnchecked(TextView title, TextView dueDateTextView, Task task) {
        title.setBackgroundResource(R.drawable.task_clicked);
        mTaskDatabaseReference.child("completed").setValue(false);
        mTaskDatabaseReference.child("completionDate").setValue(null);
        mAllTasksDatabaseReference.child("completed").setValue(false);
        mAllTasksDatabaseReference.child("completionDate").setValue(null);
        dueDateTextView.setText(getDueOrCompletedDate(task,dueDateTextView,Boolean.FALSE));
    }

    private void resetTaskReminder(Task task) {
        if(task.getReminderDate()!=null && task.getReminderTime()!=null){
            TaskInfoFragment.setReminder(getContext(),AlarmReceiver.class,task.getReminderDate(),
                    task.getReminderTime(),task,mAllTasksDatabaseReference);
        }
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
        thisTaskList=currentData.getString("currentTaskList",null);
    }

    private void initializeCalendar() {
        //Get SimpleDateFormat to format task's dates and Calendar instance:
        final String myFormat = "dd/MM/yyyy";
        final SimpleDateFormat sdf = new SimpleDateFormat(myFormat, Locale.getDefault());
        calendar=Calendar.getInstance();
    }

    @SuppressLint("RestrictedApi")
    private void initPriorityMenu(View view) {
        @SuppressLint("RestrictedApi") MenuBuilder menuBuilder =new MenuBuilder(getContext());
        MenuInflater inflater = new MenuInflater(getContext());
        inflater.inflate(R.menu.priority_menu, menuBuilder);
        @SuppressLint("RestrictedApi") MenuPopupHelper optionsMenu = new MenuPopupHelper(getContext(), menuBuilder, view);
        optionsMenu.setForceShowIcon(true);
    }

    private void setPriority(int priorityLevel,Task task) {
        mTaskDatabaseReference.child("priority").setValue(priorityLevel);
        mAllTasksDatabaseReference.child("priority").setValue(priorityLevel);
        task.setPriority(priorityLevel); //Update currentTask - keeping taskInfoFragment up to date
        setPriorityImage(priorityLevel);//Updating the image to show instant change of priority

    }

    private void setPriorityImage(int priorityLevel){
        switch(priorityLevel){
            case PRIORITY_URGENT:
                priorityImage.setImageResource(R.mipmap.ic_launcher);
                break;
            case PRIORITY_HIGH:
                priorityImage.setImageResource(R.mipmap.ic_launcher_foreground);
                break;
            case PRIORITY_DEFAULT:
                priorityImage.setImageResource(R.mipmap.ic_launcher);
                break;
        }

    }
}
