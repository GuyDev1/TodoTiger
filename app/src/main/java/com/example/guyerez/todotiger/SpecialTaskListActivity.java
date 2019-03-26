package com.example.guyerez.todotiger;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.support.constraint.ConstraintLayout;
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
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ExpandableListView;
import android.widget.FrameLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.text.ParseException;
import java.util.Calendar;
import java.util.Date;

import static com.example.guyerez.todotiger.TaskActivity.PRIORITY_DEFAULT;
import static com.example.guyerez.todotiger.TaskActivity.TASKS_DUE_TODAY;
import static com.example.guyerez.todotiger.TaskActivity.TASKS_DUE_WEEK;

public class SpecialTaskListActivity extends AppCompatActivity {
    final Context context = this;

    //The current TaskAdapter
    private ExpandableListAdapter adapter;
    // TextView that is displayed when the the search returned no results //
    private TextView mEmptyStateTextView;
    //The loading indicator //
    private View loadingIndicator;
    //Edit text and button for adding tasks to Inbox
    private EditText mTaskEditText;
    private Button mTaskAddButton;


    // Firebase instance variables
    private FirebaseDatabase mFirebaseDatabase;
    private DatabaseReference mTaskDatabaseReference;
    private DatabaseReference mAllTasksDatabaseReference;
    private DatabaseReference mTaskNumDatabaseReferenceInbox;
    private DatabaseReference mTaskNumDatabaseReferenceGeneral;
    private ChildEventListener mChildEventListener;


    //Current user ID
    private String currentUser;

    //Current special TaskList ID
    private String currentTaskList;

    // More efficient than HashMap for mapping integers to objects
    private SparseArray<TaskGroup> taskGroups = new SparseArray<TaskGroup>();

    //The ExpandableListView - to show multiple TaskList groups with their tasks
    private ExpandableListView listView;

    //Task unique integer ID (for adding tasks)
    private int taskIdNumber;

    //Task Count in Inbox
    private int taskCountInbox;

    //Task Count field for a general TaskList
    private int taskCountGeneral;


    //SharedPreferences instance
    private SharedPreferences sharedPref;

    //Indicator whether the list is empty or not
    private boolean isEmpty;

    //Flag to indicate the user moved a task to another TaskList
    //Used in order to avoid task update bugs in the ExpandableListView
    protected static boolean changeTaskListFlag;

    //A temporary field to hold the latest task changed - get over Firebase offline-persistence bug
    private Task latestTaskChanged;

    //Flag to prevent infinite loop while updating TaskList taskCount when completing a task
    private boolean flagCompleted;

    //A flag to avoid incorrect use of onChildChanged when editing Task's priority
    private boolean priorityChangedFlag=false;


    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Set the content of the activity to use the search_activity.xml layout
        setContentView(R.layout.special_task_list_activity);

        //Set up to allow Up navigation to parent activity
        this.getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        //Get current logged in user and current TaskList from SharedPreferences
        SharedPreferences currentData = this.getSharedPreferences("MyPref", Context.MODE_PRIVATE);
        currentUser = currentData.getString("userId", null);
        currentTaskList=currentData.getString("currentTaskList",null);

        //Init sharedPreferences for getting Task ID number
        sharedPref = this.getPreferences(Context.MODE_PRIVATE);

        // Initialize Firebase components
        mFirebaseDatabase = FirebaseDatabase.getInstance();

        // Initialize references to views - relevant for DueToday only, so by default - view.GONE

        mTaskEditText = (EditText) findViewById(R.id.add_task);
        mTaskEditText.setVisibility(View.GONE);
        mTaskAddButton = (Button) findViewById(R.id.add_task_button);
        mTaskAddButton.setVisibility(View.GONE);
        //Set FireBase DB references
        mAllTasksDatabaseReference = mFirebaseDatabase.getReference().child("users")
                .child(currentUser).child("allTasks");
        mTaskDatabaseReference = mFirebaseDatabase.getReference().child("users")
                .child(currentUser).child("TaskLists").child("InboxID").child("tasks");
        mTaskNumDatabaseReferenceInbox = mFirebaseDatabase.getReference().child("users")
                .child(currentUser).child("TaskLists").child("InboxID");
        if(currentTaskList.equals("Due TodayID")) {

            // Set EditText and Add button as Visible - in case we're in DueToday

            mTaskEditText.setVisibility(View.VISIBLE);
            mTaskAddButton.setVisibility(View.VISIBLE);


            // Enable create button when input is not empty - relevant for DueToday only
            mTaskEditText.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                }

                @Override
                public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                    if (charSequence.toString().trim().length() > 0) {
                        mTaskAddButton.setEnabled(true);
                    } else {
                        mTaskAddButton.setEnabled(false);
                    }
                }

                @Override
                public void afterTextChanged(Editable editable) {
                }
            });

            // Create button creates a new task and clears the EditText - relevant for DueToday only
            mTaskAddButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    // Get task title from user and create a new task
                    //Also fetch the FireBase ID and SharedPreferences ID
                    //And finally get the task's creation date
                    taskIdNumber = sharedPref.getInt("taskIdNumber", 1);
                    Calendar calendar = Calendar.getInstance();
                    Date creationDate = calendar.getTime();
                    String taskId = mTaskDatabaseReference.push().getKey();
                    Task task = new Task
                            (mTaskEditText.getText().toString(), false, taskId, taskIdNumber,
                                    "InboxID", "Inbox", creationDate, creationDate, null, PRIORITY_DEFAULT);
                    mTaskDatabaseReference.child(taskId).setValue(task);

                    //Create a copy of that Task under "AllTasks" in DB
                    mAllTasksDatabaseReference.child(taskId).setValue(task);

                    //add that task to Inbox (if we're in DueToday TaskList)
                    mTaskNumDatabaseReferenceInbox.child("taskNum").setValue(taskCountInbox + 1);
                    //Increase TaskIdNumber by 1
                    setNewTaskId(taskIdNumber + 1);


                    // Clear input box
                    mTaskEditText.setText("");
                }
            });



        }

        //add listener to get the current task count in Inbox TaskList (because the user may have
        // added a new task to it from DueToday TaskList
        addTaskNumListenerInbox();

        //Set the empty view
        mEmptyStateTextView = (TextView) findViewById(R.id.empty_view);
        showEmptyStateView(false);

        //Initialize the loading indicator
        loadingIndicator = findViewById(R.id.loading_indicator);
        showLoadingIndicator(true);


        //Set the listView and adapter - if we're not in Due Today TaskList,
        //then there is no EditText for creating a new task, so we can expand the listView
        listView = findViewById(R.id.expandable_list_view);
        if(!currentTaskList.equals("Due TodayID")){
            ViewGroup.LayoutParams params = listView.getLayoutParams();
            params.height+=200;
            listView.setLayoutParams(params);
        }

        adapter = new ExpandableListAdapter(this,
                taskGroups);
        listView.setAdapter(adapter);
        listView.setGroupIndicator(null);

        //Check for tasks - react appropriately if the TaskList is empty.
        checkForTasks();
    }

    @Override
    protected void onResume() {
        super.onResume();
        showEmptyStateView(false);
        showLoadingIndicator(true);
        attachDatabaseReadListener();

    }

    @Override
    protected void onPause() {
        super.onPause();
        taskGroups.clear();
        adapter.notifyDataSetChanged();
        detachDatabaseReadListener();
    }

    //Attach the dataBaseReadListener - to get relevant tasks and update the UI
    private void attachDatabaseReadListener() {
        if (mChildEventListener == null) {
            mChildEventListener = new ChildEventListener() {
                @SuppressLint("NewApi")
                @Override
                public void onChildAdded(DataSnapshot dataSnapshot, String s) {
                    //Show relevant tasks according to the default TaskList we're in - today/week
                    Log.d("wat", "onChildAdded: ");
                    Task task = dataSnapshot.getValue(Task.class);
                    if(task!=null && task.getDueDate()!=null){
                        if(currentTaskList.equals("Due TodayID")){
                                try {
                                    if(checkDueDate(TASKS_DUE_TODAY,task.getDueDate())
                                        &&(!task.getCompleted())){
                                        Log.d("triggered", "onChildAdded: ");
                                        showLoadingIndicator(false);
                                        showEmptyStateView(false);
                                        updateExpandableListView(task);
                                    }
                                    else if(taskGroups.size()==0){
                                        //all tasks due today are completed
                                        showLoadingIndicator(false);
                                        showEmptyStateView(true);
                                    }
                                } catch (ParseException e) {
                                    e.printStackTrace();
                                }

                        }
                        else{
                            try {
                                if((checkDueDate(TASKS_DUE_TODAY,task.getDueDate())||
                                        checkDueDate(TASKS_DUE_WEEK,task.getDueDate()))
                                        &&(!task.getCompleted())){
                                    showLoadingIndicator(false);
                                    showEmptyStateView(false);
                                    updateExpandableListView(task);
                                }
                                else if(taskGroups.size()==0){
                                    //all tasks due this week are completed
                                    showLoadingIndicator(false);
                                    showEmptyStateView(true);
                                }
                            } catch (ParseException e) {
                                e.printStackTrace();
                            }
                        }
                    }

                }
                public void onChildChanged(DataSnapshot dataSnapshot, String s) {
                    Task task = dataSnapshot.getValue(Task.class);
                    //Check if we moved the task
                    if(task!=null &&changeTaskListFlag){
                        //If we moved the task from one TaskList to another, update the UI
                        updateMovedExpandableListView(task);
                    }
                    else if(priorityChangedFlag){
                        priorityChangedFlag=false;
                    }
                    else{
                        //In case we completed the task
                        // if it's just the offline-persistence bug causing a double trigger, reset latestTaskChanged
                        if(latestTaskChanged!=null && latestTaskChanged.getCompleted()==task.getCompleted()){
                            latestTaskChanged=null;
                        }
                        else {
                            //Initialize the reference for this task's TaskList
                            mTaskNumDatabaseReferenceGeneral = mFirebaseDatabase.getReference().child("users")
                                    .child(currentUser).child("TaskLists").child(task.getTaskListId());
                            //Get the task count in this Task's TaskList
                            //And remove this task from the task count (it has been completed)
                            addTaskNumListenerGeneral();
                            latestTaskChanged = task;
                            //Remove the completed task from the UI, it's irrelevant now
                            removeCompletedTaskAnim(task);
                        }
                    }

                }
                @SuppressLint("NewApi")
                public void onChildRemoved(DataSnapshot dataSnapshot) {
                    Task task = dataSnapshot.getValue(Task.class);
                    if(task!=null){
                        //if we deleted the task - update the UI
                        Log.d("wat", "onChildRemoved: "+task.getTitle());
                        removeFromExpandableListView(task,false);
                    }

                }
                public void onChildMoved(DataSnapshot dataSnapshot, String s) {
                    Log.d("wat", "onChildMoved2: ");
                }
                public void onCancelled(DatabaseError databaseError) {}
            };

        }

        mAllTasksDatabaseReference.addChildEventListener(mChildEventListener);

    }



    //Remove the databaseReadListener so we don't listen for new tasks while the app is paused
    private void detachDatabaseReadListener() {
        if (mChildEventListener != null) {
            mAllTasksDatabaseReference.removeEventListener(mChildEventListener);
            mChildEventListener = null;
        }
    }



    //Show emptyStateView - no relevant tasks due for that relevant TaskList period (today/week)
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
            this.setTitle("TodoTiger");
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

    //Remove the task from the ExpandableListView and update UI accordingly
    private void removeFromExpandableListView(Task currentTask, boolean taskMoved) {
        String taskListTitle=currentTask.getTaskListTitle();
        int index=findTaskList(taskListTitle);
        TaskGroup tg=taskGroups.get(index);
        //TaskIndex won't be -1 because the task exists.
        int taskIndex=findTaskInTaskGroup(currentTask,tg);
        tg.tasks.remove(taskIndex);
        if(tg.tasks.size()==0){ //If taskGroup is now empty, delete it.
            if(index==0 && taskGroups.size()>1){
                //If it's the upper taskGroup, change positions to prevent bug.
                TaskGroup tg2=taskGroups.get(1);
                listView.collapseGroup(0);
                taskGroups.remove(0);
                listView.collapseGroup(1);
                taskGroups.remove(1);
                taskGroups.append(0,tg2);
                listView.expandGroup(0);
                adapter.notifyDataSetChanged();
                return;
            }
            listView.collapseGroup(index);
            taskGroups.remove(index);
        }
        adapter.notifyDataSetChanged();
        if(taskGroups.size()==0 && (!taskMoved)){
            showEmptyStateView(true);
            showLoadingIndicator(false);
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


    //Sets the new current TaskId number, for creating new tasks (it's an id that tracks the number
    //of tasks created.
    private void setNewTaskId(int newTaskId){
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putInt("taskIdNumber",newTaskId);
        editor.commit();
    }

    //Update ExpandableListView when a task is moved from one TaskList to another
    //update the UI accordingly
    private void updateMovedExpandableListView(Task currentTask) {
        String taskListTitle=currentTask.getTaskListTitle();
        String taskListId=currentTask.getTaskListId();
        int index=findTaskList(taskListTitle);
        //Definitely Wasn't in that TaskList before
        if(index==-1){
            changeTaskListFlag=false;
            int position=taskGroups.size();
            TaskGroup tg=new TaskGroup(taskListTitle,taskListId);
            tg.tasks.add(currentTask);
            taskGroups.append(position,tg);
            adapter.notifyDataSetChanged();
            listView.expandGroup(position);
        }
        //Maybe the existing TaskList - check before updating
        else{
            TaskGroup tg=taskGroups.get(index);
            //Still in previous TaskList - remove it from here
            if(findTaskInTaskGroup(currentTask,tg)!=-1){
                removeFromExpandableListView(currentTask,true);
                return;
            }
            tg.tasks.add(currentTask);
            adapter.notifyDataSetChanged();
            listView.expandGroup(index);
            changeTaskListFlag=false;
        }
    }

    //Find this task's index in a specific taskGroup
    private int findTaskInTaskGroup(Task currentTask,TaskGroup tg){
        for (int i=0;i<tg.tasks.size();i++){
            if(tg.tasks.get(i).getId().equals(currentTask.getId())){
                return i;
            }
        }
        return -1;
    }

    //Add a listener to the number of tasks in Inbox - when we create a new task
    //due for today in Due Today TaskList, update Inbox's number of tasks
    private void addTaskNumListenerInbox(){
        mTaskNumDatabaseReferenceInbox.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                TaskList taskList = dataSnapshot.getValue(TaskList.class);
                if (taskList != null) {
                    taskCountInbox = taskList.getTaskNum();
                }

            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                System.out.println("The read failed: " + databaseError.getCode());
            }
        });
    }

    //Add a listener to the number of tasks in the TaskList that this task belongs to
    private void addTaskNumListenerGeneral(){
        //Set flag to true to avoid an infinite loop while updating the taskNum for that TaskList
        flagCompleted=true;
        mTaskNumDatabaseReferenceGeneral.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                TaskList taskList = dataSnapshot.getValue(TaskList.class);
                if (taskList != null) {
                    taskCountGeneral = taskList.getTaskNum();
                    if(flagCompleted) {
                        flagCompleted=false;
                        mTaskNumDatabaseReferenceGeneral.child("taskNum").setValue(taskCountGeneral - 1);
                    }
                }

            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                System.out.println("The read failed: " + databaseError.getCode());
            }
        });
    }
    //Check for tasks, in order to correctly show the loading indicator
    // and the EmptyStateTextView
    private void checkForTasks(){
        showLoadingIndicator(true);
        mAllTasksDatabaseReference.addValueEventListener(new ValueEventListener() {
            @SuppressLint("NewApi")
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                isEmpty=true;
                for(DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    Task task = snapshot.getValue(Task.class);
                    if (task != null && task.getDueDate() != null) {
                        if (currentTaskList.equals("Due TodayID")) {
                            try {
                                if (checkDueDate(TASKS_DUE_TODAY, task.getDueDate())) {
                                    isEmpty = false;
                                }
                            } catch (ParseException e) {
                                e.printStackTrace();
                            }
                        } else {
                            try {
                                if (checkDueDate(TASKS_DUE_TODAY, task.getDueDate()) ||
                                        checkDueDate(TASKS_DUE_WEEK, task.getDueDate())) {
                                    isEmpty = false;
                                }
                            } catch (ParseException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                }
                if(isEmpty){
                    showEmptyStateView(true);
                    showLoadingIndicator(false);
                }


            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                System.out.println("The read failed: " + databaseError.getCode());
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

    //After a small delay for better animation effect
    //Remove the task from current adapter since the user completed the task
    private void removeCompletedTaskAnim(final Task currentTask){
        final Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                removeFromExpandableListView(currentTask,false);
            }}, 500);

    }

    public void setPriorityChangedFlag(boolean b){
        priorityChangedFlag=b;
    }
}
