package com.example.guyerez.todotiger;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.EditText;
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
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Arrays;


public class MainActivity extends AppCompatActivity {

    final Context context = this;
    public static final int SIGN_IN = 1;
    public static String currentTaskListId;
    public static String currentUserId;
    private TaskListAdapter mTaskListAdapter;
    //TextView that is displayed when the list is empty//
    private TextView mEmptyStateTextView;
    //TextView that is displayed when the list is empty//
    private View loadingIndicator;


    // Firebase instance variables

    private FirebaseAuth mFirebaseAuth;
    private FirebaseAuth.AuthStateListener mAuthStateListener;
    private FirebaseDatabase mFirebaseDatabase;
    private DatabaseReference mTaskListDatabaseReference;
    private ChildEventListener mChildEventListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Set the content of the activity to use the activity_main.xml layout file - the task lists
        setContentView(R.layout.activity_main);

        // Initialize Firebase components
        mFirebaseAuth = FirebaseAuth.getInstance();
        mFirebaseDatabase = FirebaseDatabase.getInstance();




        //Initialize firebase authentication
        mAuthStateListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                FirebaseUser user = firebaseAuth.getCurrentUser();
                if (user != null) {
                    // user is signed in
                    currentUserId=user.getUid();
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


        //Initialize task list Array, ListView and Adapter.
        final ArrayList<TaskList> taskLists = new ArrayList<TaskList>();

        // Create an {@link TaskListAdapter}, whose data source is a list of {@link TaskList}s.
        mTaskListAdapter = new TaskListAdapter(this, taskLists);

        // Locate the {@link ListView} object in the view hierarchy of the {@link Activity}.
        ListView listView = (ListView) findViewById(R.id.task_list_view);

        //Set the empty view
        mEmptyStateTextView = (TextView) findViewById(R.id.empty_view);
        listView.setEmptyView(mEmptyStateTextView);

        //Initialize the loading indicator
        loadingIndicator = findViewById(R.id.loading_indicator);
        loadingIndicator.setVisibility(View.INVISIBLE);

        // Make the {@link ListView} use the {@link TaskListAdapter} defined above, so that the
        // {@link ListView} will display list items for each {@link TaskList} in the list.
        listView.setAdapter(mTaskListAdapter);

        //Set and create the FAB and it's action listener
        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
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
                        .setPositiveButton("Create",
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog,int id) {
                                        // Get list title from user and create a new task list
                                        //Also fetch the FireBase ID and connect it to the new task list.
                                        String mTaskListId = mTaskListDatabaseReference.push().getKey();
                                        TaskList taskList = new TaskList(userInput.getText().toString(),mTaskListId);
                                        mTaskListDatabaseReference.child(mTaskListId).setValue(taskList);
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
        // Set an item click listener on the ListView, which creates an intent to open
        //the relevant task list and show the tasks inside.
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long l) {
                // Find the current task list that was clicked on
                TaskList currentTaskList = mTaskListAdapter.getItem(position);

                //get the current task list's ID
                currentTaskListId=currentTaskList.getId();


                // Create a new intent to view the tasks in the chosen list
                Intent taskIntent = new Intent(MainActivity.this, TaskActivity.class);

                // Send the intent to launch a new activity
                startActivity(taskIntent);
            }
        });

    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == SIGN_IN) {
            if (resultCode == RESULT_OK) {
                // Sign-in succeeded, set up the UI
                Toast.makeText(this, "Signed in!", Toast.LENGTH_SHORT).show();
            } else if (resultCode == RESULT_CANCELED) {
                // Sign in was canceled by the user, finish the activity
                Toast.makeText(this, "Sign in canceled", Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.mini_menu,menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.sign_out:
                AuthUI.getInstance().signOut(this);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }

    }

    @Override
    protected void onResume() {
        super.onResume();
        mFirebaseAuth.addAuthStateListener(mAuthStateListener);
    }
    @Override
    protected void onPause() {
        super.onPause();
        if (mAuthStateListener != null) {
            mFirebaseAuth.removeAuthStateListener(mAuthStateListener);
        }
        mTaskListAdapter.clear();
        detachDatabaseReadListener();

    }

    private void onSignedInInitialize(final String userId) {

        //Get reference for the task list for the logged in user and attach the database listener
        mTaskListDatabaseReference=mFirebaseDatabase.getReference().child("users").child(userId);
        loadingIndicator.setVisibility(View.VISIBLE);
        attachDatabaseReadListener();
        mEmptyStateTextView.setText("No task lists, add a new one!");
        loadingIndicator.setVisibility(View.GONE);


    }

    private void onSignedOutCleanup() {
        mTaskListAdapter.clear();
        detachDatabaseReadListener();
    }
    private void attachDatabaseReadListener() {
        if (mChildEventListener == null) {
            mChildEventListener = new ChildEventListener() {
                @Override
                public void onChildAdded(DataSnapshot dataSnapshot, String s) {
                    TaskList taskList = dataSnapshot.getValue(TaskList.class);
                    mTaskListAdapter.add(taskList);
                }
                public void onChildChanged(DataSnapshot dataSnapshot, String s) {}
                public void onChildRemoved(DataSnapshot dataSnapshot) {}
                public void onChildMoved(DataSnapshot dataSnapshot, String s) {}
                public void onCancelled(DatabaseError databaseError) {}
            };

        }
        mTaskListDatabaseReference.addChildEventListener(mChildEventListener);

    }
    private void detachDatabaseReadListener() {
        if (mChildEventListener != null) {
            mTaskListDatabaseReference.removeEventListener(mChildEventListener);
            mChildEventListener = null;
        }
    }

    public static String getCurrentTaskListId() {
        return currentTaskListId;
    }
    public static String getCurrentUserId() {
        return currentUserId;
    }


}
