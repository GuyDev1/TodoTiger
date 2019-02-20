package com.example.guyerez.todotiger;

import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.NavUtils;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.util.SparseArray;
import android.view.ContextMenu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
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

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;

public class SearchTask extends AppCompatActivity {

    final Context context = this;

    //The current TaskAdapter
    private TaskAdapter mTaskAdapter;
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

    private ExpandableListView listView;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Set the content of the activity to use the search_activity.xml layout
        setContentView(R.layout.search_activity);

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
                    showLoadingIndicator(true);
                    showEmptyStateView(false);
                    taskGroups.clear();
                    adapter.notifyDataSetChanged();
                    searchTask(editable.toString());
                } else {
                    showEmptyStateView(false);
                    taskGroups.clear();
                    adapter.notifyDataSetChanged();
                }
            }
        });

//        //Initialize task Array, ListView and Adapter.
//        final ArrayList<Task> tasks = new ArrayList<Task>();
//        // Create an {@link TaskAdapter}, whose data source is a list of {@link Task}s.
//        mTaskAdapter = new TaskAdapter(this, tasks);
//
//        // Locate the {@link ListView} object in the view hierarchy of the {@link Activity}.
//        ListView listView = (ListView) findViewById(R.id.search_task_list_view);

        //Set the empty view
        mEmptyStateTextView = (TextView) findViewById(R.id.empty_view);

        //Initialize the loading indicator
        loadingIndicator = findViewById(R.id.loading_indicator);
        showLoadingIndicator(false);


         listView = findViewById(R.id.expandable_list_view);
         adapter = new ExpandableListAdapter(this,
                taskGroups);
        listView.setAdapter(adapter);
        listView.setGroupIndicator(null);

        //Set context menu for ListView
        listView.setLongClickable(true);
        registerForContextMenu(listView);
//        adapter.add(0,new TaskGroup("12345"));

//        taskGroups.append(0,new TaskGroup("fff"));

//        // Make the {@link ListView} use the {@link TaskAdapter} defined above, so that the
//        // {@link ListView} will display list items for each {@link Task} in the list.
//        listView.setAdapter(mTaskAdapter);


    }

    @Override
    protected void onResume() {
        super.onResume();
        SEARCH_ACTIVE = true;

    }

    @Override
    protected void onPause() {
        super.onPause();
        taskGroups.clear();
        adapter.notifyDataSetChanged();
        SEARCH_ACTIVE = false;
    }


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


    private void showEmptyStateView(boolean b) {
        if (b) {
            mEmptyStateTextView.setVisibility(View.VISIBLE);
            mEmptyStateTextView.setText("No tasks found.");
        } else {
            mEmptyStateTextView.setVisibility(View.GONE);
        }
    }

    private void showLoadingIndicator(boolean b) {
        if (b) {
            loadingIndicator.setVisibility(View.VISIBLE);
        } else {
            loadingIndicator.setVisibility(View.GONE);
        }
    }


    public void getTaskInfo(Task currentTask) {
        //Open the TaskInfoFragment for this task
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
            this.setTitle("TodoTiger");
            super.onBackPressed();
        }
    }

    private void updateExpandableListView(Task currentTask){
        String taskListTitle=currentTask.getTaskListTitle();
        String taskListId=currentTask.getTaskListId();
        int index=findTaskList(taskListTitle);
        if(index==-1){
            int position=taskGroups.size();
            TaskGroup tg=new TaskGroup(taskListTitle,taskListId);
            tg.tasks.add(currentTask);
            taskGroups.append(position,tg);
            adapter.notifyDataSetChanged();
            listView.expandGroup(position);
        }
        else{
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

//    @Override
//    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo){
//        if (v.getId() == R.id.task_list_view){
//            AdapterView.AdapterContextMenuInfo info =(AdapterView.AdapterContextMenuInfo)menuInfo;
//
//            menu.add(0,0,0,"info");
//            menu.add(0,1,1,"Move to");
//            menu.add(0,2,2,"Delete");
//        }
//    }
//
//    @Override
//    public boolean onContextItemSelected(MenuItem menuItem){
//        AdapterView.AdapterContextMenuInfo info=(AdapterView.AdapterContextMenuInfo)menuItem.getMenuInfo();
//        Task taskClicked = mTaskAdapter.getItem(info.position);
//        switch (menuItem.getItemId()) {
//
//            case 0:
//                //Open the TaskInfoFragment for this task
//                getTaskInfo(taskClicked);
//                break;
//
//
//            case 1:
//                //Pop a dialog and allow the user to choose a TaskList to move the current task to
//                getTaskLists(taskClicked);
//                setTaskMoveDialog();
//                dialog.show();
//                moveTaskToSelectedList(taskClicked);
//                break;
//
//            case 2:
//                //Confirm delete and perform the task's deletion
//                confirmDeleteDialog(taskClicked);
//                break;
//
//
//            default:
//                break;
//
//        }
//        return true;
//    }
//
//    private void getTaskLists(final Task taskClicked){
//        //Starts a childEventListener to get the list of TaskLists
//        mChildEventListener2 = new ChildEventListener() {
//            @Override
//            public void onChildAdded(DataSnapshot dataSnapshot, String s) {
//
//                TaskList taskList = dataSnapshot.getValue(TaskList.class);
//                //Don't show current TaskList in the move-to ListView (you're already there)
//                if(!taskList.getId().equals(taskClicked.getTaskListId())) {
//                    mTaskListAdapter.add(taskList);
//                }
//            }
//            public void onChildChanged(DataSnapshot dataSnapshot, String s) {}
//            public void onChildRemoved(DataSnapshot dataSnapshot) {
//
//            }
//            public void onChildMoved(DataSnapshot dataSnapshot, String s) {}
//            public void onCancelled(DatabaseError databaseError) {}
//        };
//        mTaskListDatabaseReference.addChildEventListener(mChildEventListener2);
//    }
//
//    private void setTaskMoveDialog(){
//        //Initialize TaskList Array, ListView and Adapter for the popup dialog ListView
//        final ArrayList<TaskList> taskLists = new ArrayList<TaskList>();
//        dialog = new Dialog(TaskActivity.this,R.style.CustomDialog);
//        dialog.setContentView(R.layout.move_task_dialog);
//        dialog.setTitle("Choose a TaskList");
//        dialog.getWindow().getAttributes().windowAnimations = R.style.DialogTheme;
//        taskListsView= (ListView) dialog.findViewById(R.id.List);
//        // Create an {@link TaskListAdapter}, whose data source is a list of {@link TaskList}s.
//        mTaskListAdapter = new TaskListAdapter(this, taskLists);
//        taskListsView.setAdapter(mTaskListAdapter);
//    }
//    private void moveTaskToSelectedList(final Task task){
//        //Initialize an onItemClickListener to allow the user to choose a TaskList
//        //And then move the chosen task into that TaskList
//        taskListsView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
//            @Override
//            public void onItemClick(AdapterView<?> adapterView, View view, int position, long l) {
//                // Find the current task list that was clicked on
//                TaskList currentTaskList = mTaskListAdapter.getItem(position);
//
//                //get the current task list's ID and title
//                currentTaskListId=currentTaskList.getId();
//                currentTaskListTitle=currentTaskList.getTitle();
//                //Get references for that specific TaskList and the number of tasks in it
//                mTaskDatabaseReference2=mFirebaseDatabase.getReference().child("users")
//                        .child(currentUser).child("TaskLists")
//                        .child(currentTaskListId).child("tasks");
//                mTaskNumDatabaseReference2=mFirebaseDatabase.getReference().child("users")
//                        .child(currentUser).child("TaskLists")
//                        .child(currentTaskListId);
//
//                //Move the task inside the DB to another TaskList
//                moveTaskFireBase(mTaskDatabaseReference,mTaskDatabaseReference2,task.getId());
//                //Update the task's current TaskList ID and title
//                mTaskDatabaseReference2.child(task.getId()).child("taskListId").setValue(currentTaskListId);
//                mAllTasksDatabaseReference.child(task.getId()).child("taskListId").setValue(currentTaskListId);
//                mTaskDatabaseReference2.child(task.getId()).child("taskListTitle").setValue(currentTaskListTitle);
//                mAllTasksDatabaseReference.child(task.getId()).child("taskListTitle").setValue(currentTaskListTitle);
//
//                //Set flag to true to avoid an infinite loop while updating the taskNum for that TaskList
//                flag=true;
//                mTaskNumDatabaseReference2.addValueEventListener(new ValueEventListener() {
//                    @Override
//                    public void onDataChange(DataSnapshot dataSnapshot) {
//                        taskCount2 =dataSnapshot.getValue(TaskList.class).getTaskNum();
//                        if(flag) {
//                            flag=false;
//                            mTaskNumDatabaseReference2.child("taskNum").setValue(taskCount2 + 1);
//                        }
//
//
//
//
//                    }
//
//                    @Override
//                    public void onCancelled(DatabaseError databaseError) {
//                        System.out.println("The read failed: " + databaseError.getCode());
//                    }
//                });
//
//                //Remove the task from the current TaskAdapter and dismiss the dialog
//                mTaskAdapter.remove(task);
//                dialog.dismiss();
//                Toast.makeText(context,"Task moved!", Toast.LENGTH_LONG).show();
//
//            }
//        });
//
//    }
//
//    private void confirmDeleteDialog(final Task taskClicked){
//        AlertDialog.Builder builder = new AlertDialog.Builder(this);
//        //Set the title
//        builder.setTitle("Delete this task?");
//        // Add the buttons
//        builder.setPositiveButton("Delete", new DialogInterface.OnClickListener() {
//            public void onClick(DialogInterface dialog, int id) {
//                //Delete the selected task and cancel the reminder if it had one
//                mTaskDatabaseReference.child(taskClicked.getId()).removeValue();
//                mTaskAdapter.remove(taskClicked);
//                if(taskClicked.getReminderDate()!=null){
//                    TaskInfoFragment.cancelReminder(context,AlarmReceiver.class,taskClicked.getIntId());
//                }
//                Toast.makeText(context, "Task deleted!", Toast.LENGTH_LONG).show();
//            }
//        });
//        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
//            public void onClick(DialogInterface dialog, int id) {
//                // User cancelled the dialog
//                dialog.cancel();
//            }
//        });
//
//        // Create the AlertDialog
//        AlertDialog deleteDialog = builder.create();
//        deleteDialog.getWindow().getAttributes().windowAnimations = R.style.DialogTheme;
//        deleteDialog.show();
//    }




}
