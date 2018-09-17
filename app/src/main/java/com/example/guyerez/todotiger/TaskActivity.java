package com.example.guyerez.todotiger;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
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

import java.util.ArrayList;
import java.util.Arrays;

public class TaskActivity extends AppCompatActivity {
    final Context context = this;
    private TaskAdapter mTaskAdapter;
    private int taskCount;
    /** TextView that is displayed when the list is empty */
    private TextView mEmptyStateTextView;

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

        // Initialize Firebase components
        mFirebaseDatabase = FirebaseDatabase.getInstance();

        //Initialize task Array, ListView and Adapter.
        final ArrayList<Task> tasks = new ArrayList<Task>();

        // Create an {@link TaskAdapter}, whose data source is a list of {@link Task}s.
        mTaskAdapter = new TaskAdapter(this, tasks);

        // Locate the {@link ListView} object in the view hierarchy of the {@link Activity}.
        ListView listView = (ListView) findViewById(R.id.task_list_view);
        mEmptyStateTextView = (TextView) findViewById(R.id.empty_view);
        listView.setEmptyView(mEmptyStateTextView);

        // Make the {@link ListView} use the {@link TaskAdapter} defined above, so that the
        // {@link ListView} will display list items for each {@link Task} in the list.
        listView.setAdapter(mTaskAdapter);

        //Get reference for the task list for the logged in user and attach the database listener
        mTaskDatabaseReference=mFirebaseDatabase.getReference().child("users")
                .child(MainActivity.getCurrentUserId())
                .child(MainActivity.getCurrentTaskListId()).child("tasks");
        mTaskNumDatabaseReference=mFirebaseDatabase.getReference().child("users")
                .child(MainActivity.getCurrentUserId())
                .child(MainActivity.getCurrentTaskListId());
        View loadingIndicator = findViewById(R.id.loading_indicator);
        loadingIndicator.setVisibility(View.GONE);
        mEmptyStateTextView.setText("No tasks, add a new one!");

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
                                        String taskId = mTaskDatabaseReference.push().getKey();
                                        Task task = new Task(userInput.getText().toString(),false,taskId);
                                        mTaskDatabaseReference.child(taskId).setValue(task);

                                        //Count that task in the list's task count
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
        attachDatabaseReadListener();
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
                public void onChildRemoved(DataSnapshot dataSnapshot) {}
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



}
