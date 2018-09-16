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
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import com.firebase.ui.auth.AuthUI;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.ArrayList;
import java.util.Arrays;


public class MainActivity extends AppCompatActivity {
    final Context context = this;
    public static final int SIGN_IN = 1;
    private String mUserName;

    // Firebase instance variables

    private FirebaseAuth mFirebaseAuth;
    private FirebaseAuth.AuthStateListener mAuthStateListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Set the content of the activity to use the activity_main.xml layout file - the task lists
        setContentView(R.layout.activity_main);

        // Initialize Firebase components
        mFirebaseAuth = FirebaseAuth.getInstance();

        //Initialize firebase authentication
        mAuthStateListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                FirebaseUser user = firebaseAuth.getCurrentUser();
                if (user != null) {
                    // user is signed in
                    mUserName=user.getDisplayName().toString();
                } else {
                    // user is signed out
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


        //create a temporary list of task lists.
        final ArrayList<TaskList> taskLists = new ArrayList<TaskList>();
        taskLists.add(new TaskList("Inbox"));
        taskLists.add(new TaskList("Homework"));
        taskLists.add(new TaskList("Legendary"));
        taskLists.add(new TaskList("Maybe/Someday"));
        taskLists.add(new TaskList("ASAP"));


        // Create an {@link TaskListAdapter}, whose data source is a list of {@link TaskList}s.
        TaskListAdapter adapter = new TaskListAdapter(this, taskLists);

        // Locate the {@link ListView} object in the view hierarchy of the {@link Activity}.
        ListView listView = (ListView) findViewById(R.id.task_list_view);

        // Make the {@link ListView} use the {@link TaskListAdapter} defined above, so that the
        // {@link ListView} will display list items for each {@link TaskList} in the list.
        listView.setAdapter(adapter);

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
                                        taskLists.add(new TaskList(userInput.getText().toString()));
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

    }

}
