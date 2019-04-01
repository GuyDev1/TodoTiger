package com.example.guyerez.todotiger;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.NavUtils;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.util.SparseArray;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ExpandableListView;
import android.widget.FrameLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;


public class SearchTask extends AppCompatActivity {

    final Context context = this;

    //The current TaskAdapter
    private ExpandableListAdapter adapter;
    // TextView that is displayed when the the search returned no results //
    private TextView mEmptyStateTextView;
    //The loading indicator //
    private View loadingIndicator;
    //Edit text for searching tasks
    private EditText mTaskSearchEditText;

    // Firebase instance variables
    private FirebaseDatabase mFirebaseDatabase;
    private DatabaseReference mTaskDatabaseReference;


    //Current user ID
    private String currentUser;

    //Indicate if Search is Active
    public static boolean SEARCH_ACTIVE;

    // More efficient than HashMap for mapping integers to objects
    private SparseArray<TaskGroup> taskGroups = new SparseArray<TaskGroup>();

    //The ExpandableListView - to show multiple TaskList groups with their tasks
    private ExpandableListView listView;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Set the content of the activity to use the search_activity.xml layout
        setContentView(R.layout.search_activity);

        this.setTitle("Search Tasks");

        //Indicate that Search is active
        SEARCH_ACTIVE = true;

        //Set up to allow Up navigation to parent activity
        this.getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        //Get current logged in user from SharedPreferences
        SharedPreferences currentData = this.getSharedPreferences("MyPref", Context.MODE_PRIVATE);
        currentUser = currentData.getString("userId", null);

        // Initialize Firebase components
        mFirebaseDatabase = FirebaseDatabase.getInstance();

        //Set FireBase DB references
        mTaskDatabaseReference = mFirebaseDatabase.getReference().child("users")
                .child(currentUser).child("allTasks");

        // Initialize references to views
        mTaskSearchEditText = (EditText) findViewById(R.id.task_search);


        //Add textChangedListener - when the user starts typing in the search box
        //the search begins automatically and shows the relevant results for his query
        mTaskSearchEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void afterTextChanged(Editable editable) {

                if (!editable.toString().isEmpty()) {
                    //The query is not empty, start a new search and update UI
                    showLoadingIndicator(true);
                    showEmptyStateView(false);
                    taskGroups.clear();
                    adapter.notifyDataSetChanged();
                    searchTask(editable.toString());
                } else {
                    //The query is empty - show a blank screen not emptyStateView
                    //because the user hasn't searched for anything yet
                    showEmptyStateView(false);
                    taskGroups.clear();
                    adapter.notifyDataSetChanged();
                }
            }
        });


        //Set the empty view
        mEmptyStateTextView = (TextView) findViewById(R.id.empty_view);

        //Initialize the loading indicator
        loadingIndicator = findViewById(R.id.loading_indicator);
        showLoadingIndicator(false);


        //Initialize the listView and Adapter
         listView = findViewById(R.id.expandable_list_view);
         adapter = new ExpandableListAdapter(this,
                taskGroups);
        listView.setAdapter(adapter);
        listView.setGroupIndicator(null);

    }

    @Override
    protected void onResume() {
        super.onResume();
        SEARCH_ACTIVE = true;
        this.setTitle("Search Tasks");

    }

    @Override
    protected void onPause() {
        super.onPause();
        taskGroups.clear();
        adapter.notifyDataSetChanged();
        SEARCH_ACTIVE = false;
    }


    //Search for the user input in the search box, update UI accordingly
    private void searchTask(final String s) {
        Query query = mTaskDatabaseReference.orderByChild("title");
        query.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                taskGroups.clear();
                adapter.notifyDataSetChanged();
                if (dataSnapshot.hasChildren()) {
                    for (DataSnapshot ds : dataSnapshot.getChildren()) {
                        Task task = ds.getValue(Task.class);
                        if(task.getTitle().contains(s) || (task.getNotes()!=null && task.getNotes().contains(s))){
                            updateExpandableListView(task);
                        }
                    }
                    if (taskGroups.size() != 0) {
                        Log.d("here2", "onDataChange: ");
                        showEmptyStateView(false);
                        showLoadingIndicator(false);
                    }
                    else {
                        //Found no tasks, stop loading and alert the user.
                        showLoadingIndicator(false);
                        showEmptyStateView(true);
                    }

                } else {
                    //Found no tasks, stop loading and alert the user.
                    Log.d("here3", "onDataChange: ");
                    showLoadingIndicator(false);
                    showEmptyStateView(true);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }


    //Show emptyStateView - no tasks found.
    private void showEmptyStateView(boolean b) {
        if (b) {
            mEmptyStateTextView.setVisibility(View.VISIBLE);
            mEmptyStateTextView.setText("No tasks found.");
        } else {
            mEmptyStateTextView.setVisibility(View.GONE);
        }
    }

    //Show loading indicator
    private void showLoadingIndicator(boolean b) {
        if (b) {
            loadingIndicator.setVisibility(View.VISIBLE);
        } else {
            loadingIndicator.setVisibility(View.GONE);
        }
    }


    //Open the TaskInfoFragment for this task
    public void getTaskInfo(Task currentTask) {
        TaskInfoFragment taskInfo = new TaskInfoFragment();
        taskInfo.setCurrentTask(currentTask);
        android.support.v4.app.FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.setCustomAnimations(R.anim.slide_in, 0, 0, R.anim.slide_out);
        // Replace whatever is in the fragment_container view with this fragment,
        // and add the transaction to the back stack
        transaction.replace(R.id.frag_container, taskInfo);
        transaction.addToBackStack(null);
        FrameLayout frameLayout = findViewById(R.id.frag_container);
        frameLayout.setClickable(true);

        // Commit the transaction
        transaction.commit();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Respond to the action bar's Up/Home button
        if (item.getItemId() == android.R.id.home) {
            //Check if the call came from the TaskInfoFragment or the activity
            Fragment currentFragment = getSupportFragmentManager().findFragmentById(R.id.frag_container);
            if (currentFragment != null && currentFragment.isVisible()) {
                if (NotesFragment.isAttached()) {
                    FrameLayout frameLayout = findViewById(R.id.frag_container);
                    frameLayout.setClickable(true);
                    this.onBackPressed();
                } else {
                    FrameLayout frameLayout = findViewById(R.id.frag_container);
                    frameLayout.setClickable(false);
                    this.onBackPressed();
                }
            } else {
                NavUtils.navigateUpFromSameTask(this);

            }
        }
        return true;
    }

    //Respond to onBackPressed according to whether the NotesFragment or TaskInfoFragment are attached
    @Override
    public void onBackPressed() {
        if (NotesFragment.isAttached()) {
            FragmentManager fm = getSupportFragmentManager();
            for (Fragment frag : fm.getFragments()) {
                if (frag.isVisible()) {
                    FragmentManager childFm = frag.getChildFragmentManager();
                    if (childFm.getBackStackEntryCount() > 0) {
                        childFm.popBackStack();
                        //setClickable to false to prevent inner fragment's frame from catching outer fragment clicks
                        FrameLayout frameLayout = findViewById(R.id.notes_fragment);
                        frameLayout.setClickable(false);
                        return;
                    }
                }
            }
        } else {
            //setClickable to false to prevent clicks being caught by the fragment's frame while we're viewing the tasks
            FrameLayout frameLayout = findViewById(R.id.frag_container);
            frameLayout.setClickable(false);
            this.setTitle("Search Tasks");
            super.onBackPressed();
        }
    }

    //Update ExpandableListView with the new tasks - if the task is in a TaskList currently not shown
    //Add that TaskList as a new group, and add this new task as it's child
    private void updateExpandableListView(Task currentTask){
        String taskListTitle=currentTask.getTaskListTitle();
        String taskListId=currentTask.getTaskListId();
        int index=findTaskList(taskListTitle);
        if(index==-1){
            //Need to add a new TaskList (taskGroup in listView)
            int position=taskGroups.size();
            TaskGroup tg=new TaskGroup(taskListTitle,taskListId);
            tg.tasks.add(currentTask);
            taskGroups.append(position,tg);
            adapter.notifyDataSetChanged();
            listView.expandGroup(position);
        }
        else{
            //Add task to relevant taskGroup already visible
            TaskGroup tg=taskGroups.get(index);
            tg.tasks.add(currentTask);
            adapter.notifyDataSetChanged();
            listView.expandGroup(index);
        }

    }

    //return the index of the requested taskList, if not found return -1
    private int findTaskList(String taskListTitle){
        for(int i=0;i<taskGroups.size();i++){
            TaskGroup tg=taskGroups.get(i);
            if(tg.taskListTitle.equals(taskListTitle)){
                return i;
            }
        }
        return -1;

    }

}
