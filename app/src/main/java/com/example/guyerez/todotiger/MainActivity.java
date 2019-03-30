package com.example.guyerez.todotiger;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.PorterDuff;
import android.os.Build;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.ContextMenu;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
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
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;


import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import static com.example.guyerez.todotiger.TaskActivity.TASKS_DUE_TODAY;
import static com.example.guyerez.todotiger.TaskActivity.TASKS_DUE_WEEK;


public class MainActivity extends AppCompatActivity {

    //Channel_ID for android notifications
    private static final String CHANNEL_ID = "123";

    //constants used for initializing default TaskList's
    private static final int DEFAULT_INBOX = 0;
    private static final int DEFAULT_DUE_TODAY = 1;
    private static final int DEFAULT_DUE_WEEK = 2;

    final Context context = this;

    //Constant to indicate the user signed in
    public static final int SIGN_IN = 1;

    //The current user ID in FireBase
    private String currentUserId;
    private TaskListAdapter mTaskListAdapter;
    //TextView that is displayed when the list is empty//
    private TextView mEmptyStateTextView;
    //The loading indicator //
    private View loadingIndicator;


    // Firebase instance variables

    private FirebaseAuth mFirebaseAuth;
    private FirebaseAuth.AuthStateListener mAuthStateListener;
    private FirebaseDatabase mFirebaseDatabase;
    private DatabaseReference mTaskListDatabaseReference;
    private DatabaseReference mAllTasksDatabaseReference;
    private DatabaseReference mTaskNumDatabaseReferenceDueToday;
    private DatabaseReference mTaskNumDatabaseReferenceDueWeek;
    private ChildEventListener mChildEventListener;

    //SharedPreferences instance
    private SharedPreferences sharedPref;

    //Indicators - Show default TaskLists in ListView
    private boolean showDueToday;
    private boolean showDueWeek;

    //Task Counters for Default TaskList's - DueToday and DueWeek
    private int countDueToday;
    private int countDueWeek;

    //Custom ImageView button for adding new TaskList's
    private ImageView createTaskListButton;


    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Set the content of the activity to use the activity_main.xml layout file - the task lists
        setContentView(R.layout.activity_main);


        //Create the notification channel
        createNotificationChannel();

//        //Setup app logo
//        getSupportActionBar().setDisplayShowHomeEnabled(true);
//        getSupportActionBar().setTitle("");
//        getSupportActionBar().setLogo(R.drawable.todo_tiger);
//        getSupportActionBar().setDisplayUseLogoEnabled(true);

        //Customize the ActionBar - setup app logo in the middle
         ActionBar abar = getSupportActionBar();
        View viewActionBar = getLayoutInflater().inflate(R.layout.custom_action_bar, null);
        ActionBar.LayoutParams params = new ActionBar.LayoutParams(//Center the textview in the ActionBar !
                ActionBar.LayoutParams.WRAP_CONTENT,
                ActionBar.LayoutParams.MATCH_PARENT,
                Gravity.CENTER);
        abar.setCustomView(viewActionBar, params);
        abar.setDisplayShowCustomEnabled(true);
        abar.setDisplayShowTitleEnabled(false);
        abar.setIcon(R.drawable.todo_tiger);


        // Initialize Firebase components
        mFirebaseAuth = FirebaseAuth.getInstance();
        mFirebaseDatabase = FirebaseDatabase.getInstance();

        //Check if this user has TaskList's if not - show EmptyStateTextView
        DatabaseReference rootRef = FirebaseDatabase.getInstance().getReference().child("users");
        rootRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                if (snapshot != null) {
                    if (getCurrentUserId() != null && !snapshot.hasChild(getCurrentUserId())) {
                        mEmptyStateTextView.setVisibility(View.VISIBLE);
                        mEmptyStateTextView.setText("No task lists, add a new one!");
                        loadingIndicator.setVisibility(View.GONE);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

        //Initialize task list Array, ListView and Adapter.
        final ArrayList<TaskList> taskLists = new ArrayList<TaskList>();

        // Create an {@link TaskListAdapter}, whose data source is a list of {@link TaskList}s.
        mTaskListAdapter = new TaskListAdapter(this, taskLists);

        // Locate the {@link ListView} object in the view hierarchy of the {@link Activity}.
        ListView listView = (ListView) findViewById(R.id.task_list_view);

        //Set the empty view
        mEmptyStateTextView = (TextView) findViewById(R.id.empty_view);

        //Initialize the loading indicator
        loadingIndicator = findViewById(R.id.loading_indicator);
        loadingIndicator.setVisibility(View.VISIBLE);

        // Make the {@link ListView} use the {@link TaskListAdapter} defined above, so that the
        // {@link ListView} will display list items for each {@link TaskList} in the list.
        listView.setAdapter(mTaskListAdapter);

        //Initialize firebase authentication
        mAuthStateListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                FirebaseUser user = firebaseAuth.getCurrentUser();
                if (user != null) {
                    // user is signed in

                    currentUserId = user.getUid();
                    onSignedInInitialize(user.getUid());

                } else {
                    // user is signed out
                    onSignedOutCleanup();
                    startActivityForResult(
                            AuthUI.getInstance()
                                    .createSignInIntentBuilder()
                                    .setIsSmartLockEnabled(false)
                                    .setAvailableProviders(Arrays.asList(
                                            new AuthUI.IdpConfig.EmailBuilder().build(),
                                            new AuthUI.IdpConfig.GoogleBuilder().build()))
                                    .setTosAndPrivacyPolicyUrls("https://superapp.example.com/terms-of-service.html",
                                            "https://superapp.example.com/privacy-policy.html")
                                    .build(),
                            SIGN_IN);
                }
            }
        };

        //Define custom "FAB" and it's onClickListener - to create a new TaskList
        createTaskListButton=findViewById(R.id.add_tasklist_button);
        //set onTouch listener for proper animation
        AdapterUtil.setImageViewClickAnimation(createTaskListButton);

        //Set onClickListener - to respond and enable the user to create a new TaskList
        createTaskListButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Get add_list.xml view
                LayoutInflater li = LayoutInflater.from(context);
                View addTaskListView = li.inflate(R.layout.add_list, null);

                //Create the prompt to enable the user to create a new task list
                AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(
                        context);

                // Set add_list.xml as the layout for alertdialog builder
                alertDialogBuilder.setView(addTaskListView);

                //Set the user input box
                final EditText userInput = (EditText) addTaskListView
                        .findViewById(R.id.edit_list_name);


                // Set dialog message
                alertDialogBuilder
                        .setCancelable(false)
                        .setPositiveButton("Add",
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int id) {
                                        // Get list title from user and create a new task list
                                        //Also fetch the FireBase ID and connect it to the new task list.
                                        //And finally get the TaskList's creation date
                                        createNewTaskList(userInput.getText().toString());

                                    }
                                })
                        .setNegativeButton("Cancel",
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int id) {
                                        dialog.cancel();
                                    }
                                });

                // Create the dialog
                final AlertDialog alertDialog = alertDialogBuilder.create();
                alertDialog.getWindow().getAttributes().windowAnimations = R.style.DialogTheme;

                // Enable create button when input is not empty
                userInput.addTextChangedListener(new TextWatcher() {
                    @Override
                    public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                    }

                    @Override
                    public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                        if (charSequence.toString().trim().length() > 0) {
                            alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(true);
                        } else {
                            alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);
                        }
                    }

                    @Override
                    public void afterTextChanged(Editable editable) {
                    }
                });

                // Show the dialog
                alertDialog.show();
                alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);
            }
        });

        // Set an item click listener on the ListView, which creates an intent to open
        //the relevant task list and show the tasks inside.
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long l) {
                // Find the current task list that was clicked on
                TaskList currentTaskList = mTaskListAdapter.getItem(position);

                //Open the new TaskList as a new TaskActivity
                openTaskList(currentTaskList);
            }
        });

        listView.setLongClickable(true);
        registerForContextMenu(listView);

        //init default TaskList preferences
        initDefaultTaskListsPrefs();


    }


    //Check if user signed-in successfully and react accordingly
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == SIGN_IN) {
            if (resultCode == RESULT_OK) {
                // Sign-in succeeded, set up the UI
                recreate();
                Toast.makeText(this, "Signed in!", Toast.LENGTH_SHORT).show();
            } else if (resultCode == RESULT_CANCELED) {
                // Sign in was canceled by the user, finish the activity
                Toast.makeText(this, "Sign in canceled", Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }

    //Create the optionsMenu
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.mini_menu, menu);
        return true;
    }

    //Set optionMenu responses - sign_out, settings and search
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.sign_out:
                AuthUI.getInstance().signOut(this);
                return true;
            case R.id.settings:
                Intent settingsIntent = new Intent(this, SettingsActivity.class);
                startActivity(settingsIntent);
                return true;
            case R.id.search:
                // Create a new intent to enter the Search activity
                Intent searchIntent = new Intent(MainActivity.this, SearchTask.class);
                // Send the intent to launch a new activity
                startActivity(searchIntent);
            default:
                return super.onOptionsItemSelected(item);
        }

    }

    @Override
    protected void onResume() {
        super.onResume();
        //Start authentication listener when app is resumed.
        mFirebaseAuth.addAuthStateListener(mAuthStateListener);

    }

    @Override
    protected void onPause() {
        super.onPause();
        //When app is paused - remove auth listener, clear the adapter
        //and detach the databaseReadListener
        if (mAuthStateListener != null) {
            mFirebaseAuth.removeAuthStateListener(mAuthStateListener);
        }
        mTaskListAdapter.clear();
        detachDatabaseReadListener();

    }

    private void onSignedInInitialize(final String userId) {

        //Get reference for the task list for the logged in user and attach the database listener
        mTaskListDatabaseReference = mFirebaseDatabase.getReference().child("users").child(userId).child("TaskLists");
        mAllTasksDatabaseReference = mFirebaseDatabase.getReference().child("users")
                .child(userId).child("allTasks");
        mTaskNumDatabaseReferenceDueToday = mFirebaseDatabase.getReference().child("users")
                .child(userId).child("TaskLists").child("Due TodayID");
        mTaskNumDatabaseReferenceDueWeek = mFirebaseDatabase.getReference().child("users")
                .child(userId).child("TaskLists").child("Due This WeekID");
        attachDatabaseReadListener();
        attachDatabaseReadListenerDue();

        if (!defaultTasksCreated(userId)) {
            initDefaultTaskLists(userId);
        }


        //Update current user in SharedPreferences
        SharedPreferences pref = getApplicationContext().getSharedPreferences("MyPref", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = pref.edit();
        editor.putString("userId", userId); // Storing boolean - true/false
        editor.commit();

    }


    //When the user signs out, clear the TaskListAdapter and detach the databaseReadListener
    private void onSignedOutCleanup() {
        mTaskListAdapter.clear();
        detachDatabaseReadListener();
    }

    //Attach the dataBaseReadListener - to get relevant TaskLists and update the UI
    private void attachDatabaseReadListener() {
        if (mChildEventListener == null) {
            mChildEventListener = new ChildEventListener() {
                @Override
                public void onChildAdded(DataSnapshot dataSnapshot, String s) {
                    mEmptyStateTextView.setVisibility(View.GONE);
                    loadingIndicator.setVisibility(View.GONE);
                    TaskList taskList = dataSnapshot.getValue(TaskList.class);
                    if(taskList.getId()!=null){
                        if ((taskList.getId().equals("Due TodayID") && (!showDueToday))
                                || (taskList.getId().equals("Due This WeekID") && (!showDueWeek))) {
                            //Don't add Default TaskList to Adapter
                            return;
                        }
                        mTaskListAdapter.add(taskList);
                    }

                }

                public void onChildChanged(DataSnapshot dataSnapshot, String s) {
                }

                public void onChildRemoved(DataSnapshot dataSnapshot) {
                    if (mTaskListAdapter.isEmpty()) {
                        mEmptyStateTextView.setVisibility(View.VISIBLE);
                        mEmptyStateTextView.setText("No task lists, add a new one!");
                    }
                }

                public void onChildMoved(DataSnapshot dataSnapshot, String s) {
                }

                public void onCancelled(DatabaseError databaseError) {
                }
            };

        }
        mTaskListDatabaseReference.addChildEventListener(mChildEventListener);

    }
    //Remove the databaseReadListener so we don't listen for new TaskLists while the app is paused
    private void detachDatabaseReadListener() {
        if (mChildEventListener != null) {
            mTaskListDatabaseReference.removeEventListener(mChildEventListener);
            mChildEventListener = null;
        }
    }

    //Return the current logged in user
    private String getCurrentUserId() {
        return currentUserId;
    }

    //Create contextMenu - when TaskLists are long clicked, enables the user to delete them
    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        if (v.getId() == R.id.task_list_view) {
            AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) menuInfo;
            menu.add(0, 0, 0, "Delete");
        }
    }

    @Override
    public boolean onContextItemSelected(MenuItem menuItem) {
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) menuItem.getMenuInfo();
        TaskList taskListClicked = mTaskListAdapter.getItem(info.position);
        Log.d("check", "" + taskListClicked.getTitle());
        switch (menuItem.getItemId()) {
            case 0:
                //The user chose to delete this TaskList - react appropriately
                //This option will also delete all the tasks related to this TaskList
                if (taskListClicked.getId().equals("InboxID") || taskListClicked.getId().equals("Due TodayID")
                        || taskListClicked.getId().equals("Due This WeekID")) {
                    Toast.makeText(this, "Can't delete default Task List", Toast.LENGTH_LONG).show();
                    break;
                }
                mTaskListDatabaseReference.child(taskListClicked.getId()).removeValue();
                removeFromAllTasks(taskListClicked.getId());
                mTaskListAdapter.remove(taskListClicked);
                Toast.makeText(this, "Task List deleted!", Toast.LENGTH_LONG).show();
                break;

            default:
                break;

        }
        return true;
    }

    //When a taskList is deleted - delete all it's tasks (from allTasks list, direct descendants
    //are deleted automatically
    private void removeFromAllTasks(final String taskListId) {
        final List<Task>tasksToDelete=new ArrayList<>();
        mAllTasksDatabaseReference.addValueEventListener(new ValueEventListener() {
            public void onDataChange(DataSnapshot dataSnapshot) {
                for(DataSnapshot snapshot : dataSnapshot.getChildren()){
                    Task task = snapshot.getValue(Task.class);
                    Log.d("wat1", "onDataChange: "+task.getTitle());
                    if (task.getTaskListId().equals(taskListId)) {
                        Log.d("wat2", "onDataChange: "+task.getTitle());
                        tasksToDelete.add(task);
                    }

                }
                for(Task t:tasksToDelete){
                    Log.d("wat4", "onDataChange: "+t.getTitle());
                    mAllTasksDatabaseReference.child(t.getId()).removeValue();
                }

            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });


    }


    private void createNotificationChannel() {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = getString(R.string.channel_name);
            String description = getString(R.string.channel_description);
            int importance = NotificationManager.IMPORTANCE_HIGH;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);
            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }

    //Create a new TaskList, update the DataBase, and open the TaskList so the user
    //could enter new tasks immediately.
    private void createNewTaskList(String title) {
        Calendar calendar = Calendar.getInstance();
        Date creationDate = calendar.getTime();
        String mTaskListId = mTaskListDatabaseReference.push().getKey();
        TaskList taskList = new TaskList(title, mTaskListId, creationDate);
        mTaskListDatabaseReference.child(mTaskListId).setValue(taskList);
        openTaskList(taskList);
    }

    //Start TaskActivity for the taskList the user chose to open
    private void openTaskList(TaskList currentTaskList) {
        //Update current TaskList in SharedPreferences
        SharedPreferences pref = getApplicationContext().getSharedPreferences("MyPref", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = pref.edit();
        editor.putString("currentTaskList", currentTaskList.getId());
        editor.putString("currentTaskListTitle", currentTaskList.getTitle());
        editor.commit();


        if (currentTaskList.getId().equals("Due TodayID") || currentTaskList.getId().equals("Due This WeekID")) {
            Intent taskIntent = new Intent(MainActivity.this, SpecialTaskListActivity.class);
            // Send the intent to launch a new activity
            startActivity(taskIntent);
        } else {
            // Create a new intent to view the tasks in the chosen list
            Intent taskIntent = new Intent(MainActivity.this, TaskActivity.class);
            // Send the intent to launch a new activity
            startActivity(taskIntent);
        }


    }

    //Initialize default TaskLists
    private void initDefaultTaskLists(String currentUserId) {
        createDefaultTaskList("Inbox", DEFAULT_INBOX);
        createDefaultTaskList("Due Today", DEFAULT_DUE_TODAY);
        createDefaultTaskList("Due This Week", DEFAULT_DUE_WEEK);

        //Update that default TaskLists was created for this user
        SharedPreferences pref = getApplicationContext().getSharedPreferences("MyPref", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = pref.edit();
        editor.putBoolean("defaultTasksCreated" + currentUserId, true); // Storing boolean - true/false
        editor.commit();
    }

    //Check whether we already created the default TaskLists for this user
    private boolean defaultTasksCreated(String currentUserId) {
        SharedPreferences currentData = context.getSharedPreferences("MyPref", Context.MODE_PRIVATE);
        return currentData.getBoolean("defaultTasksCreated" + currentUserId, false);
    }

    //Create the default TaskLists with an early date so they always show on top
    private void createDefaultTaskList(String taskListName, int defaultTaskName) {
        Log.d("wat", "createDefaultTaskList: ");
        Calendar calendar = Calendar.getInstance();
        calendar.set(2000, 1, 1, 0, defaultTaskName);
        Date creationDate = calendar.getTime();
        TaskList taskList = new TaskList(taskListName, taskListName + "ID", creationDate);
        mTaskListDatabaseReference.child(taskListName + "ID").setValue(taskList);
    }

    //Get Settings to indicate whether the user wants to see the default TaskLists
    private void initDefaultTaskListsPrefs() {
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
        showDueToday = settings.getBoolean("show_due_today", true);
        showDueWeek = settings.getBoolean("show_due_week", true);

    }

    //Attach a databaseReadListener to check the current number of tasks associated with the
    //default TaskLists - that is, update their task number in the UI
    private void attachDatabaseReadListenerDue() {
        mAllTasksDatabaseReference.addValueEventListener(new ValueEventListener() {
            @SuppressLint("NewApi")
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                countDueToday=0;
                countDueWeek=0;
                for(DataSnapshot snapshot : dataSnapshot.getChildren()){
                    Task task = snapshot.getValue(Task.class);
                    if (task != null && task.getDueDate()!=null) {
                        try {
                            if (checkDueDate(TASKS_DUE_TODAY, task.getDueDate())
                                &&(!task.getCompleted())) {
                                countDueToday++;
                                countDueWeek++;
                            }
                            else if(checkDueDate(TASKS_DUE_WEEK, task.getDueDate())
                                    &&(!task.getCompleted())){
                                countDueWeek++;
                            }
                        } catch (ParseException e) {
                            e.printStackTrace();
                        }

                    };

                }

                mTaskNumDatabaseReferenceDueToday.child("taskNum").setValue(countDueToday);
                mTaskNumDatabaseReferenceDueWeek.child("taskNum").setValue(countDueWeek);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

    //Get's duedate filter (today/week/month) and check if current task's due date fits.
    @RequiresApi(api = Build.VERSION_CODES.N)
    private boolean checkDueDate(int dueWhen, Date dueDate) throws ParseException {
        Calendar currentCalendar = Calendar.getInstance();
        Calendar dueCalendar = Calendar.getInstance();
        currentCalendar.setTime(currentCalendar.getTime());
        dueCalendar.setTime(dueDate);
        int currentDay=currentCalendar.get(Calendar.DAY_OF_YEAR);
        int currentYear=currentCalendar.getWeekYear();
        int currentWeek=currentCalendar.get(Calendar.WEEK_OF_YEAR);
//        int currentMonth=currentCalendar.get(Calendar.MONTH);
        int dueDay=dueCalendar.get(Calendar.DAY_OF_YEAR);
        int dueYear=dueCalendar.getWeekYear();
        int dueWeek=dueCalendar.get(Calendar.WEEK_OF_YEAR);
//        int dueMonth=dueCalendar.get(Calendar.MONTH);

        switch (dueWhen){
            case TASKS_DUE_TODAY:
                if (currentYear>=dueYear && currentDay>=dueDay
                        && currentWeek>=dueWeek){
                    return true;
                }
                break;
            case TASKS_DUE_WEEK:
                if(currentYear==dueYear && currentWeek==dueWeek){
                    return true;
                }
                break;
//            case TASKS_DUE_MONTH:
//                if(currentMonth==dueMonth){
//                    return true;
//                }
//                break;



        }
        return false;
    }
}

