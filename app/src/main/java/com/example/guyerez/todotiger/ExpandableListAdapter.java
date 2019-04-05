package com.example.guyerez.todotiger;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Paint;
import android.preference.PreferenceManager;
import android.support.v7.view.menu.MenuBuilder;
import android.support.v7.view.menu.MenuPopupHelper;
import android.util.SparseArray;
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
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

import static com.example.guyerez.todotiger.TaskActivity.PRIORITY_DEFAULT;
import static com.example.guyerez.todotiger.TaskActivity.PRIORITY_HIGH;
import static com.example.guyerez.todotiger.TaskActivity.PRIORITY_URGENT;

public class ExpandableListAdapter extends BaseExpandableListAdapter {

    //Variables indicating task LongClick options
    public static final int ACTION_INFO = 0;
    public static final int ACTION_MOVE_TO = 1;
    public static final int ACTION_DELETE = 2;

    //SparseArray to hold the taskGroups
    private final SparseArray<TaskGroup> taskGroups;

    //SharedPreference fields
    private  String currentUser;


    private LayoutInflater inflater;
    private Activity activity;

    //Get a calendar instance for setting dates
    private Calendar calendar;

    //Define FireBase instance variables
    private DatabaseReference mTaskDatabaseReference;
    private FirebaseDatabase mFirebaseDatabase;
    private DatabaseReference mAllTasksDatabaseReference;
    private DatabaseReference mTaskListDatabaseReference;
    private DatabaseReference mTaskNumDatabaseReference2;
    private ChildEventListener mChildEventListener;

    //Adapter reference for confirm delete dialog behavior in AdapterUtil
    private ExpandableListAdapter adapter;

    //Define task priority menu components
    private MenuBuilder menuBuilder;
    private MenuPopupHelper optionsMenu;


    public ExpandableListAdapter(Activity act, SparseArray<TaskGroup> taskGroups) {
        activity = act;
        this.taskGroups = taskGroups;
        inflater = act.getLayoutInflater();

        //Initialize sharedPreferences
        initSharedPreferences();

        //Initialize calendar for task's dates and for using the Calendar
        initializeCalendar();

        // Initialize Firebase DB
        mFirebaseDatabase = FirebaseDatabase.getInstance();

        adapter=this;


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
        //Get task's title
        final TextView title=convertView.findViewById(R.id.task_title);
        title.setText(task.getTitle());

        //Set onClick animation
        title.setBackgroundResource(R.drawable.task_clicked);

        //If the task is completed - title Strikethrough
        if(task.getCompleted())
            title.setPaintFlags(title.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
//        title.setBackgroundResource(AdapterUtil.strikeCompleted(task.getCompleted()));

        //When the user clicks the task - enter the taskInfoFragment
        title.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(activity instanceof SpecialTaskListActivity){
                    //Use the TaskActivity getTaskInfo method to start TaskInfoFragment
                    ((SpecialTaskListActivity)activity).getTaskInfo(task);
                }
                if(activity instanceof SearchTask){
                    ((SearchTask)activity).getTaskInfo(task);
                }

            }
        });
        title.setLongClickable(true);
        //Set onLongClickListener - similar to that of TaskActivity - allowing
        //the user to see the task's Info, Move the task to a different TaskList and delete it.
        title.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                final CharSequence[] options = {"Info","Move to","Delete"};

                AlertDialog.Builder builder = new AlertDialog.Builder(activity);

                builder.setTitle("Choose Action:");
                builder.setItems(options, new DialogInterface.OnClickListener() {

                    public void onClick(DialogInterface dialog, int item) {
                        switch(item){
                            case ACTION_INFO:
                                //Open the TaskInfoFragment for this task
                                if(activity instanceof SearchTask){
                                    ((SearchTask)activity).getTaskInfo(task);
                                }
                                else{
                                    ((SpecialTaskListActivity)activity).getTaskInfo(task);
                                }
                                break;
                            case ACTION_MOVE_TO:
                                //Open a dialog and allow the user to choose a TaskList to move the current task to
                                initDatabaseReferencesForTaskOptions(task);
                                AdapterUtil.getTaskLists(task, mTaskListDatabaseReference);
                                AdapterUtil.setTaskMoveDialog(activity);

                                AdapterUtil.moveTaskToSelectedList(task,currentUser,
                                        mTaskDatabaseReference,mFirebaseDatabase,mAllTasksDatabaseReference,activity,this);
                                notifyDataSetChanged();
                                break;
                            case ACTION_DELETE:
                                //Confirm delete and perform the task's deletion
                                initDatabaseReferencesForTaskOptions(task);
                                AdapterUtil.confirmDeleteDialog(task,activity,mTaskDatabaseReference,mAllTasksDatabaseReference,adapter);

                                break;

                        }
                    }
                });
                AlertDialog taskOptions = builder.create();

                taskOptions.show();
                return false;
            }
        });


        //Initialize the due date TextView in the task_item.xml layout with the ID due_date
        final TextView dueDateTextView = (TextView) convertView.findViewById(R.id.due_date);
        //Get the task's due date from the currentTask object and set it in the text view
        dueDateTextView.setText(AdapterUtil.getDueOrCompletedDate(task,dueDateTextView,calendar,activity,null));

        //Initialize the check box and check it if the task was completed.
        CheckBox checkBox = (CheckBox) convertView.findViewById(R.id.check_box);
        checkBox.setOnCheckedChangeListener(null);
        checkBox.setChecked(task.getCompleted());

        checkBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView,boolean isChecked) {
                //Get the task DB reference to edit task completion status
                initDatabaseReferences(task);
                    if (isChecked) {
                        //Update the Task as Checked (completed) in the DB and UI
                        AdapterUtil.updateTaskChecked(title,dueDateTextView,task,mTaskDatabaseReference,mAllTasksDatabaseReference,calendar,activity);
                        //cancel task's reminder if it had one, since it's completed
                        AdapterUtil.cancelTaskReminder(task,activity);

                    } else {
                        //Update the Task as unChecked (not completed) in the DB and UI
                        AdapterUtil.updateTaskUnchecked(title,dueDateTextView,task,mTaskDatabaseReference,mAllTasksDatabaseReference,calendar,activity);
                        //Reset the reminder if it had any
                        AdapterUtil.resetTaskReminder(task,activity,mAllTasksDatabaseReference);
                    }
            }
            });


        // Get the Task's priorityImage and allow the user to change the task's priority
        // by clicking on it
        final ImageView priorityImage=convertView.findViewById(R.id.imageView);
        priorityImage.setImageResource(AdapterUtil.getTaskPriorityImage(task));
        priorityImage.setOnClickListener(new View.OnClickListener() {
            @SuppressLint("RestrictedApi")
            @Override
            public void onClick(View view) {
                initPriorityMenu(view);

                //Init the task DB reference to edit task completion status
                initDatabaseReferences(task);

                // Set Item Click Listener
                menuBuilder.setCallback(new MenuBuilder.Callback() {
                    @Override
                    public boolean onMenuItemSelected(MenuBuilder menu, MenuItem item) {
                        switch (item.getItemId()) {
                            case R.id.priority_urgent:
                                AdapterUtil.setPriority(PRIORITY_URGENT,task,mTaskDatabaseReference,mAllTasksDatabaseReference,priorityImage);
                                return true;
                            case R.id.priority_high:
                                AdapterUtil.setPriority(PRIORITY_HIGH,task,mTaskDatabaseReference,mAllTasksDatabaseReference,priorityImage);
                                return true;
                            case R.id.priority_default:
                                AdapterUtil.setPriority(PRIORITY_DEFAULT,task,mTaskDatabaseReference,mAllTasksDatabaseReference,priorityImage);
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
        //Set the TaskGroup - that is the TaskList title those Tasks belong to
        final TaskGroup taskGroup = (TaskGroup) getGroup(groupPosition);
        CheckedTextView taskListTitleTextView=convertView.findViewById(R.id.task_list_group_title);
        //onClick - go to relevant group's TaskList
        taskListTitleTextView.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                //Update current TaskList in SharedPreferences
                updateTaskListSP(taskGroup);
                // Create a new intent to view the tasks in the chosen list
                Intent taskIntent = new Intent(activity, TaskActivity.class);
                // Send the intent to launch the relevant TaskActivity
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
        return true;
    }


    private void initSharedPreferences(){
        //Get Settings for Task UI preferences
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(activity);
        //Get current logged in user from SharedPreferences
        SharedPreferences currentData = activity.getSharedPreferences("MyPref", Context.MODE_PRIVATE);
        currentUser = currentData.getString("userId", null);
    }

    private void initializeCalendar() {
        //Get SimpleDateFormat to format task's dates and Calendar instance:
        final String myFormat = "dd/MM/yyyy";
        final SimpleDateFormat sdf = new SimpleDateFormat(myFormat, Locale.getDefault());
        calendar=Calendar.getInstance();
    }

    private void updateTaskListSP(TaskGroup taskGroup){
        //Update current TaskList in SharedPreferences
        SharedPreferences pref = activity.getApplicationContext().getSharedPreferences("MyPref", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = pref.edit();
        editor.putString("currentTaskList", taskGroup.taskListId);
        editor.putString("currentTaskListTitle", taskGroup.taskListTitle);
        editor.commit();

    }

    private void initDatabaseReferences(Task task){
        mTaskDatabaseReference=mFirebaseDatabase.getReference()
                .child("users").child(currentUser).child("TaskLists")
                .child(task.getTaskListId()).child("tasks").child(task.getId());
        mAllTasksDatabaseReference=mFirebaseDatabase.getReference()
                .child("users").child(currentUser)
                .child("allTasks").child(task.getId());
        mTaskListDatabaseReference=mFirebaseDatabase.getReference().child("users")
                .child(currentUser).child("TaskLists");
    }

    private void initDatabaseReferencesForTaskOptions(Task task){
        mTaskDatabaseReference=mFirebaseDatabase.getReference()
                .child("users").child(currentUser).child("TaskLists")
                .child(task.getTaskListId()).child("tasks");
        mAllTasksDatabaseReference=mFirebaseDatabase.getReference()
                .child("users").child(currentUser)
                .child("allTasks");
        mTaskListDatabaseReference=mFirebaseDatabase.getReference().child("users")
                .child(currentUser).child("TaskLists");
    }


    @SuppressLint("RestrictedApi")
    private void initPriorityMenu(View view) {
        menuBuilder =new MenuBuilder(activity);
        MenuInflater inflater = new MenuInflater(activity);
        inflater.inflate(R.menu.priority_menu, menuBuilder);
        optionsMenu = new MenuPopupHelper(activity, menuBuilder, view);
        optionsMenu.setForceShowIcon(true);
    }








}

