package com.example.guyerez.todotiger;

import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.RequiresApi;
import android.support.v4.app.Fragment;
import android.support.v4.app.NavUtils;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.firebase.ui.auth.AuthUI;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.MutableData;
import com.google.firebase.database.Transaction;
import com.google.firebase.database.ValueEventListener;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class TaskActivity extends AppCompatActivity {
    final Context context = this;
    private TaskAdapter mTaskAdapter;
    private int taskCount;
    // TextView that is displayed when the list is empty //
    private TextView mEmptyStateTextView;
    //The loading indicator //
    private View loadingIndicator;
    //Edit text and button for creating new tasks quickly
    private EditText mTaskEditText;
    private Button mTaskCreateButton;
    //Show completed tasks boolean
    private int tasksToShow;
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



    // Firebase instance variables
    private FirebaseDatabase mFirebaseDatabase;
    private DatabaseReference mTaskDatabaseReference;
    private DatabaseReference mTaskNumDatabaseReference;
    private ChildEventListener mChildEventListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Set the content of the activity to use the activity_main.xml layout file - the task lists
        setContentView(R.layout.task_activity);

        //Get task preferences - which tasks to show - show all tasks by default
        SharedPreferences sharedPref = this.getPreferences(Context.MODE_PRIVATE);
        tasksToShow=sharedPref.getInt("tasksToShow",SHOW_ALL_TASKS);

        //Set up to allow Up navigation to parent activity
        this.getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        // Initialize Firebase components
        mFirebaseDatabase = FirebaseDatabase.getInstance();

        // Initialize references to views

        mTaskEditText = (EditText) findViewById(R.id.task_edit_text);
        mTaskCreateButton = (Button) findViewById(R.id.create_task_button);

        // Enable create button when input is not empty
        mTaskEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                if (charSequence.toString().trim().length() > 0) {
                    mTaskCreateButton.setEnabled(true);
                } else {
                    mTaskCreateButton.setEnabled(false);
                }
            }

            @Override
            public void afterTextChanged(Editable editable) {
            }
        });

        // Create button creates a new task and clears the EditText
        mTaskCreateButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Get task title from user and create a new task
                //Also fetch the FireBase ID and connect it to the new task.
                //And finally get the task's creation date
                String creationDate ="Created: " + new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(new Date());
                String taskId = mTaskDatabaseReference.push().getKey();
                Task task = new Task(mTaskEditText.getText().toString(),false,taskId,creationDate,"Due: --/--/----",null);
                mTaskDatabaseReference.child(taskId).setValue(task);

                //add that task to the list's task count
                mTaskNumDatabaseReference.child("taskNum").setValue(taskCount+1);



                // Clear input box
                mTaskEditText.setText("");
            }
        });


        //Initialize task Array, ListView and Adapter.
        final ArrayList<Task> tasks = new ArrayList<Task>();

        // Create an {@link TaskAdapter}, whose data source is a list of {@link Task}s.
        mTaskAdapter = new TaskAdapter(this, tasks);

        // Locate the {@link ListView} object in the view hierarchy of the {@link Activity}.
        ListView listView = (ListView) findViewById(R.id.task_list_view);

        //Set the empty view
        mEmptyStateTextView = (TextView) findViewById(R.id.empty_view);
        listView.setEmptyView(mEmptyStateTextView);

        //Initialize the loading indicator
        loadingIndicator = findViewById(R.id.loading_indicator);
        loadingIndicator.setVisibility(View.INVISIBLE);

        // Make the {@link ListView} use the {@link TaskAdapter} defined above, so that the
        // {@link ListView} will display list items for each {@link Task} in the list.
        listView.setAdapter(mTaskAdapter);


        //Set context menu for ListView
        listView.setLongClickable(true);
        registerForContextMenu(listView);



        //Get reference for the task list for the logged in user and attach the database listener
        mTaskDatabaseReference=mFirebaseDatabase.getReference().child("users")
                .child(MainActivity.getCurrentUserId())
                .child(MainActivity.getCurrentTaskListId()).child("tasks");
        mTaskNumDatabaseReference=mFirebaseDatabase.getReference().child("users")
                .child(MainActivity.getCurrentUserId())
                .child(MainActivity.getCurrentTaskListId());


        //add listener to get the current task count in this specific task list
        mTaskNumDatabaseReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                TaskList taskList = dataSnapshot.getValue(TaskList.class);
                if(taskList!=null){
                    taskCount=taskList.getTaskNum();
                    Log.d("post count: ", "" + taskCount);
                }

            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                System.out.println("The read failed: " + databaseError.getCode());
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadingIndicator.setVisibility(View.VISIBLE);
        attachDatabaseReadListener();
        mEmptyStateTextView.setText("No tasks, add a new one!");
        loadingIndicator.setVisibility(View.GONE);

    }
    @Override
    protected void onPause() {
        super.onPause();

        mTaskAdapter.clear();
        detachDatabaseReadListener();

    }

    private void attachDatabaseReadListener() {
        if (mChildEventListener == null) {
            mChildEventListener = new ChildEventListener() {
                @TargetApi(Build.VERSION_CODES.N)
                @RequiresApi(api = Build.VERSION_CODES.N)
                @Override
                public void onChildAdded(DataSnapshot dataSnapshot, String s) {
                    Task task = dataSnapshot.getValue(Task.class);
                    switch (tasksToShow){
                        case SHOW_ALL_TASKS:
                            mTaskAdapter.add(task);
                            break;

                        case SHOW_OPEN_TASKS:
                            if(!task.getCompleted()){
                                mTaskAdapter.add(task);
                            }
                            break;
                        case SHOW_COMPLETED_TASKS:
                            if(task.getCompleted()){
                                mTaskAdapter.add(task);
                            }
                            break;

                        case TASKS_DUE_TODAY:
                            try {
                                if(!task.getCompleted() && checkDueDate(TASKS_DUE_TODAY,task.getDueDate())){
                                    mTaskAdapter.add(task);

                                }
                            } catch (ParseException e) {
                                e.printStackTrace();
                            }
                            break;
                        case TASKS_DUE_WEEK:
                            try {
                                if(!task.getCompleted() && checkDueDate(TASKS_DUE_WEEK,task.getDueDate())){
                                    mTaskAdapter.add(task);

                                }
                            } catch (ParseException e) {
                                e.printStackTrace();
                            }
                            break;

                        case TASKS_DUE_MONTH:
                            try {
                                if(!task.getCompleted() && checkDueDate(TASKS_DUE_MONTH,task.getDueDate())){
                                    mTaskAdapter.add(task);

                                }
                            } catch (ParseException e) {
                                e.printStackTrace();
                            }
                            break;
                        case TASKS_COMPLETED_TODAY:
                            try {
                                if(task.getCompleted() && checkCompletionDate(TASKS_COMPLETED_TODAY,task.getCompletionDate())){
                                    mTaskAdapter.add(task);

                                }
                            } catch (ParseException e) {
                                e.printStackTrace();
                            }
                            break;

                        case TASKS_COMPLETED_WEEK:
                            try {
                                if(task.getCompleted() && checkCompletionDate(TASKS_COMPLETED_WEEK,task.getCompletionDate())){
                                    mTaskAdapter.add(task);

                                }
                            } catch (ParseException e) {
                                e.printStackTrace();
                            }
                            break;

                        case TASKS_COMPLETED_MONTH:
                            try {
                                if(task.getCompleted() && checkCompletionDate(TASKS_COMPLETED_MONTH,task.getCompletionDate())){
                                    mTaskAdapter.add(task);

                                }
                            } catch (ParseException e) {
                                e.printStackTrace();
                            }
                            break;



                    }

                }
                public void onChildChanged(DataSnapshot dataSnapshot, String s) { ;
                }
                public void onChildRemoved(DataSnapshot dataSnapshot) {
                    mTaskNumDatabaseReference.child("taskNum").setValue(taskCount-1);
                }
                public void onChildMoved(DataSnapshot dataSnapshot, String s) {}
                public void onCancelled(DatabaseError databaseError) {}
            };

        }
        mTaskDatabaseReference.addChildEventListener(mChildEventListener);

    }
    private void detachDatabaseReadListener() {
        if (mChildEventListener != null) {
            mTaskDatabaseReference.removeEventListener(mChildEventListener);
            mChildEventListener = null;
        }
    }

    /**
     * MENU
     */

    @Override
    public void onCreateContextMenu(ContextMenu menu,View v, ContextMenu.ContextMenuInfo menuInfo){
        if (v.getId() == R.id.task_list_view){
            AdapterView.AdapterContextMenuInfo info =(AdapterView.AdapterContextMenuInfo)menuInfo;
            menu.add(0,0,0,"Delete");
            menu.add(0,1,1,"info");
        }
    }

    @Override
    public boolean onContextItemSelected(MenuItem menuItem){
        AdapterView.AdapterContextMenuInfo info=(AdapterView.AdapterContextMenuInfo)menuItem.getMenuInfo();
        Task taskClicked=mTaskAdapter.getItem(info.position);
        Log.d("check","" +taskClicked.getTitle());
        switch (menuItem.getItemId()) {
            case 0:
                mTaskDatabaseReference.child(taskClicked.getId()).removeValue();
                mTaskAdapter.remove(taskClicked);
                Toast.makeText(this, "Task deleted!", Toast.LENGTH_LONG).show();
                break;

            case 1:
                //Open the TaskInfoFragment for this task
                getTaskInfo(taskClicked);
                break;

            default:
                break;

        }
        return true;
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.task_menu,menu);
        return true;
    }

    //set up the back button - to navigate to the parent activity
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            // Respond to the action bar's Up/Home button
            case android.R.id.home:
                //Check if the call came from the TaskInfoFragment or the activity
                Fragment currentFragment = getSupportFragmentManager().findFragmentById(R.id.frag_container);
                if(currentFragment!=null && currentFragment.isVisible()){
                    this.onBackPressed();
                }
                else{
                    NavUtils.navigateUpFromSameTask(this);

                }
                return true;

            case R.id.show_all:
                setTasksToShow(SHOW_ALL_TASKS);
                recreate();
                return true;

            case R.id.show_open:
                setTasksToShow(SHOW_OPEN_TASKS);
                recreate();
                return true;


            case R.id.due_today:
                setTasksToShow(TASKS_DUE_TODAY);
                recreate();
                return true;

            case R.id.due_week:
                setTasksToShow(TASKS_DUE_WEEK);
                recreate();
                return true;

            case R.id.due_month:
                setTasksToShow(TASKS_DUE_MONTH);
                recreate();
                return true;

            case R.id.completed_today:
                setTasksToShow(TASKS_COMPLETED_TODAY);
                recreate();
                return true;

            case R.id.completed_week:
                setTasksToShow(TASKS_COMPLETED_WEEK);
                recreate();
                return true;

            case R.id.completed_month:
                setTasksToShow(TASKS_COMPLETED_MONTH);
                recreate();
                return true;

            case R.id.completed_all:
                setTasksToShow(SHOW_COMPLETED_TASKS);
                recreate();
                return true;




        }
        return super.onOptionsItemSelected(item);
    }

    public void getTaskInfo(Task currentTask){
        //Open the TaskInfoFragment for this task
        TaskInfoFragment taskInfo = new TaskInfoFragment();
        taskInfo.setCurrentTask(currentTask);
        android.support.v4.app.FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        // Replace whatever is in the fragment_container view with this fragment,
        // and add the transaction to the back stack
        transaction.replace(R.id.frag_container, taskInfo);
        transaction.addToBackStack(null);

        // Commit the transaction
        transaction.commit();
    }

    private void setTasksToShow(int tasksToShow){
        //Get's which tasks to show - and sets the preferences accordingly
        SharedPreferences sharedPref = this.getPreferences(Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putInt("tasksToShow",tasksToShow);
        editor.commit();
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    private boolean checkDueDate(int dueWhen, String dueDate) throws ParseException {
        //Get's duedate filter (today/week/month) and check if current task's due date fits.
        Calendar currentCalendar = Calendar.getInstance();
        Calendar dueCalendar = Calendar.getInstance();
        String myFormat = "dd/MM/yyyy";
        SimpleDateFormat sdf = new SimpleDateFormat(myFormat, Locale.getDefault());
        String currentDate=sdf.format(currentCalendar.getTime());
        Date taskDueDate=sdf.parse(dueDate.substring(5));
        currentCalendar.setTime(currentCalendar.getTime());
        dueCalendar.setTime(taskDueDate);
        int currentYear=currentCalendar.getWeekYear();
        int currentWeek=currentCalendar.get(Calendar.WEEK_OF_YEAR);
        int currentMonth=currentCalendar.get(Calendar.MONTH);
        int dueYear=dueCalendar.getWeekYear();
        int dueWeek=dueCalendar.get(Calendar.WEEK_OF_YEAR);
        int dueMonth=dueCalendar.get(Calendar.MONTH);

        switch (dueWhen){
            case TASKS_DUE_TODAY:
                if (currentDate.equals(dueDate.substring(5))){
                    return true;
                }
                break;
            case TASKS_DUE_WEEK:
                if(currentYear==dueYear && currentWeek==dueWeek){
                    return true;
                }
                break;
            case TASKS_DUE_MONTH:
                if(currentMonth==dueMonth){
                    return true;
                }
                break;



                }
                return false;
        }

    @RequiresApi(api = Build.VERSION_CODES.N)
    private boolean checkCompletionDate(int completedWhen, String completedDate) throws ParseException {
        //Get's completedDate filter (today/week/month) and check if current task's completion date fits.
        Calendar currentCalendar = Calendar.getInstance();
        Calendar completedCalendar = Calendar.getInstance();
        String myFormat = "dd/MM/yyyy";
        SimpleDateFormat sdf = new SimpleDateFormat(myFormat, Locale.getDefault());
        String currentDate=sdf.format(currentCalendar.getTime());
        Date taskCompletionDate=sdf.parse(completedDate.substring(11));
        currentCalendar.setTime(currentCalendar.getTime());
        completedCalendar.setTime(taskCompletionDate);
        int currentYear=currentCalendar.getWeekYear();
        int currentWeek=currentCalendar.get(Calendar.WEEK_OF_YEAR);
        int currentMonth=currentCalendar.get(Calendar.MONTH);
        int completedYear=completedCalendar.getWeekYear();
        int completedWeek=completedCalendar.get(Calendar.WEEK_OF_YEAR);
        int completedMonth=completedCalendar.get(Calendar.MONTH);

        switch (completedWhen){
            case TASKS_COMPLETED_TODAY:
                if (currentDate.equals(completedDate.substring(11))){
                    return true;
                }
                break;
            case TASKS_COMPLETED_WEEK:
                if(currentYear==completedYear && currentWeek==completedWeek){
                    return true;
                }
                break;
            case TASKS_COMPLETED_MONTH:
                if(currentMonth==completedMonth){
                    return true;
                }
                break;



        }
        return false;
    }

    }




