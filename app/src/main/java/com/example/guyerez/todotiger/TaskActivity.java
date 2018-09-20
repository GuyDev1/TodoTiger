package com.example.guyerez.todotiger;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
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

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
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

        //Set up to allow Up navigation to parent activity
        this.getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        // Initialize Firebase components
        mFirebaseDatabase = FirebaseDatabase.getInstance();

        // Initialize references to views
        mTaskEditText = (EditText) findViewById(R.id.task_edit_text);
        mTaskCreateButton = (Button) findViewById(R.id.create_task_button);

        // Enable Send button when there's text to send
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
                String creationDate ="Created: " + new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
                String taskId = mTaskDatabaseReference.push().getKey();
                Task task = new Task(mTaskEditText.getText().toString(),false,taskId,creationDate);
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


        //Set and create the FAB and it's action listener
        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Get add_list.xml view
                LayoutInflater li = LayoutInflater.from(context);
                View addTaskView = li.inflate(R.layout.add_task, null);

                //Create the prompt to enable the user to create a new task list
                AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(
                        context);

                // Set add_task.xml as the layout for alertdialog builder
                alertDialogBuilder.setView(addTaskView);

                //Set the user input box
                final EditText userInput = (EditText) addTaskView
                        .findViewById(R.id.edit_task_name);

                // Set dialog message
                alertDialogBuilder
                        .setCancelable(false)
                        .setPositiveButton("Create",
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog,int id) {

                                        // Get task title from user and create a new task
                                        //Also fetch the FireBase ID and connect it to the new task.
                                        //And finally get the task's creation date
                                        String creationDate = "Created: " + new SimpleDateFormat
                                                ("yyyy-MM-dd", Locale.getDefault()).format(new Date());
                                        String taskId = mTaskDatabaseReference.push().getKey();
                                        Task task = new Task(userInput.getText().toString(),false,taskId,creationDate);
                                        mTaskDatabaseReference.child(taskId).setValue(task);
                                        //add that task to the list's task count
                                        mTaskNumDatabaseReference.child("taskNum").setValue(taskCount+1);




                                    }
                                })
                        .setNegativeButton("Cancel",
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog,int id) {
                                        dialog.cancel();
                                    }
                                });

                // Create the dialog
                AlertDialog alertDialog = alertDialogBuilder.create();

                // Show the dialog
                alertDialog.show();
            }
        });

        //add listener to get the current task count in this specific task list
        mTaskNumDatabaseReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                TaskList taskList = dataSnapshot.getValue(TaskList.class);
                taskCount=taskList.getTaskNum();
                Log.d("post count: ", "" + taskCount);
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
                @Override
                public void onChildAdded(DataSnapshot dataSnapshot, String s) {
                    Task task = dataSnapshot.getValue(Task.class);
                    mTaskAdapter.add(task);
                }
                public void onChildChanged(DataSnapshot dataSnapshot, String s) {}
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

            default:
                break;

        }
        return true;
    }


    //set up the back button - to navigate to the parent activity
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            // Respond to the action bar's Up/Home button
            case android.R.id.home:
                NavUtils.navigateUpFromSameTask(this);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }



}
