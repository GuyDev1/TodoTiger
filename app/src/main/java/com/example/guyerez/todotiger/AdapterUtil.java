package com.example.guyerez.todotiger;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

import static com.example.guyerez.todotiger.TaskActivity.PRIORITY_DEFAULT;
import static com.example.guyerez.todotiger.TaskActivity.PRIORITY_HIGH;
import static com.example.guyerez.todotiger.TaskActivity.PRIORITY_URGENT;

public class AdapterUtil {

    //Variables for moving Tasks between TaskLists - defined here to prevent inner class access problem
    private static boolean flagAdd;
    private static boolean flagDelete;
    private static int taskCountAdd;
    private static int taskCountDelete;
    private static ListView taskListsView;
    private static TaskListAdapter mTaskListAdapter;
    private static Dialog moveTaskDialog;

    //Check whether the task was completed or not - and show the DueDate or CompletedDate accordingly
    public static String getDueOrCompletedDate(Task currentTask, TextView dueDateTextView, Calendar calendar,
                                         Activity activity,Boolean... currentlyCompleted){
        Date currentDate =calendar.getTime();
        Boolean currentlyComplete=null;
        String myFormat = "dd/MM/yyyy";
        SimpleDateFormat sdf = new SimpleDateFormat(myFormat, Locale.getDefault());
        if(currentlyCompleted!=null){
            currentlyComplete=currentlyCompleted[0];
        }

        if(currentlyComplete!=null){
            if(currentlyComplete==Boolean.TRUE){
                dueDateTextView.setTextColor(Color.parseColor("#000000"));
                dueDateTextView.setAlpha(0.54f);
                return "Completed: "+sdf.format(calendar.getTime());
            }
            else{
                return getDueDate(currentTask.getDueDate(),dueDateTextView,calendar,activity);
            }

        }
        if(currentTask.getCompleted()){
            dueDateTextView.setTextColor(Color.parseColor("#000000"));
            dueDateTextView.setAlpha(0.54f);
            //If we completed the task through search, set the current date to prevent a NullPointerException
            if(currentTask.getCompletionDate()==null){
                return "Completed: "+sdf.format(currentDate);
            }
            return "Completed: "+sdf.format(currentTask.getCompletionDate());
        }
        else{
            return getDueDate(currentTask.getDueDate(),dueDateTextView,calendar,activity);

        }
    }

    //Get the task's dueDate and color it accordingly (e.g: if it's past it's dueDate, color it red)
    public static String getDueDate(Date dueDate, TextView dueDateTextView, Calendar calendar, Activity activity){
        if(dueDate!=null){
            int dayDifference=
                    ((int)((dueDate.getTime()/(24*60*60*1000))
                            -(int)(calendar.getTime().getTime()/(24*60*60*1000))));
            if(dayDifference>=0){
                switch (dayDifference) {
                    case 0:
                        dueDateTextView.setAlpha(1);
                        dueDateTextView.setTextColor(ContextCompat.getColor(activity,R.color.green));
                        return "Due today";
                    case 1:
                        dueDateTextView.setAlpha(1);
                        return "Due tomorrow";
                    default:
                        dueDateTextView.setTextColor(Color.parseColor("#000000"));
                        dueDateTextView.setAlpha(0.54f);
                        return String.format(Locale.getDefault(), "Due in %d days", dayDifference);
                }
            }
            else{
                dueDateTextView.setTextColor(ContextCompat.getColor(activity,R.color.red));
                dueDateTextView.setAlpha(1);
                if(dayDifference==-1){
                    return "Due yesterday";
                }
                else{
                    return String.format(Locale.getDefault(), "Due %d days ago", -dayDifference);
                }

            }

        }
        else{
            dueDateTextView.setTextColor(Color.parseColor("#000000"));
            dueDateTextView.setAlpha(0.54f);
            return "Due: ";
        }


    }

    public static void setPriorityImage(int priorityLevel, ImageView priorityImage){
        switch(priorityLevel){
            case PRIORITY_URGENT:
                priorityImage.setImageResource(R.mipmap.ic_launcher);
                break;
            case PRIORITY_HIGH:
                priorityImage.setImageResource(R.mipmap.ic_launcher_foreground);
                break;
            case PRIORITY_DEFAULT:
                priorityImage.setImageResource(R.mipmap.ic_launcher);
                break;
        }

    }

    public static void setPriority(int priorityLevel, Task task, DatabaseReference mTaskDatabaseReference,
                             DatabaseReference mAllTasksDatabaseReference,ImageView priorityImage,Activity activity) {
        mTaskDatabaseReference.child("priority").setValue(priorityLevel);
        mAllTasksDatabaseReference.child("priority").setValue(priorityLevel);
        //If we are in TaskActivity - set priorityChangedFlag to true - to indicate we changed
        //the task's priority (so onChildChanged won't trigger a task count change).
        if(activity instanceof TaskActivity){
            ((TaskActivity)activity).setPriorityChangedFlag(true);
        }
        else if(activity instanceof SpecialTaskListActivity){
            ((SpecialTaskListActivity)activity).setPriorityChangedFlag(true);
        }
        task.setPriority(priorityLevel); //Update currentTask - keeping taskInfoFragment up to date
        setPriorityImage(priorityLevel,priorityImage);//Updating the image to show instant change of priority

    }

    //Update task's checked status - indicating that it's completed.
    public static void updateTaskChecked(TextView title,TextView dueDateTextView,Task task,Calendar calendar,
                                   DatabaseReference mTaskDatabaseReference,DatabaseReference mAllTasksDatabaseReference,Activity activity,boolean showCompleted) {
        Date completionDate =calendar.getTime();
        title.setBackgroundResource(R.drawable.strike_through);
        mTaskDatabaseReference.child("completed").setValue(true);
        mTaskDatabaseReference.child("completionDate").setValue(completionDate);
        mAllTasksDatabaseReference.child("completed").setValue(true);
        mAllTasksDatabaseReference.child("completionDate").setValue(completionDate);
        if(showCompleted){
            dueDateTextView.setText(getDueOrCompletedDate(task,dueDateTextView,calendar,activity,Boolean.TRUE));
            dueDateTextView.setVisibility(View.VISIBLE);
        }
    }

    //Cancel the task's reminder
    public static void cancelTaskReminder(Task task,Activity activity) {
        if(task.getReminderDate()!=null){
            TaskInfoFragment.cancelReminder(activity,AlarmReceiver.class,task.getIntId());
        }
    }

    //Update task's checked status - indicating that it's not completed (un-checking it)
    public static void updateTaskUnchecked(TextView title, TextView dueDateTextView, Task task, DatabaseReference mTaskDatabaseReference,
                                           DatabaseReference mAllTasksDatabaseReference,Calendar calendar,Activity activity) {
        title.setBackgroundResource(R.drawable.task_clicked);
        mTaskDatabaseReference.child("completed").setValue(false);
        mTaskDatabaseReference.child("completionDate").setValue(null);
        mAllTasksDatabaseReference.child("completed").setValue(false);
        mAllTasksDatabaseReference.child("completionDate").setValue(null);
        dueDateTextView.setText(getDueOrCompletedDate(task,dueDateTextView,calendar,activity,Boolean.FALSE));
    }

    //Reset the task's reminder
    public static void resetTaskReminder(Task task,Activity activity,DatabaseReference mAllTasksDatabaseReference) {
        if(task.getReminderDate()!=null && task.getReminderTime()!=null){
            TaskInfoFragment.setReminder(activity,AlarmReceiver.class,task.getReminderDate(),
                    task.getReminderTime(),task,mAllTasksDatabaseReference);
        }
    }
    //Strike-through the task if it's completed, to indicate it's completion
    public static int strikeCompleted(boolean completed){
        if (completed){
            return R.drawable.strike_through;
        }
        else{
            return R.drawable.task_clicked;
        }
    }

    //Get the Task's priorityImage according to the Task's Priority
    public static int getTaskPriorityImage(Task currentTask){
        switch (currentTask.getPriority()){
            case PRIORITY_URGENT:
                return R.mipmap.ic_launcher_round;
            case PRIORITY_HIGH:
                return R.mipmap.ic_launcher_foreground;
            default:
                break;
        }
        return R.mipmap.ic_launcher; //Default priority icon
    }

    //Get task's creation date and update UI accordingly
    public static String getCreationDate(Task currentTask,Calendar calendar){
        int dayDifference=
                ((int)((calendar.getTime().getTime()/(24*60*60*1000))
                        -(int)(currentTask.getCreationDate().getTime()/(24*60*60*1000))));
        switch (dayDifference){
            case 0:
                return "Created today";
            case 1:
                return "Created yesterday";
            default:
                return String.format(Locale.getDefault(),"Created %d days ago",dayDifference);

        }

    }

    //Get a list of the TaskLists so the user can choose where to move his task to
    public static void getTaskLists(final Task taskClicked,ChildEventListener mChildEventListener2,DatabaseReference mTaskListDatabaseReference){
        //Starts a childEventListener to get the list of TaskLists
        mChildEventListener2 = new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot dataSnapshot, String s) {

                TaskList taskList = dataSnapshot.getValue(TaskList.class);
                //Don't show current TaskList in the move-to ListView (you're already there)
                if(isRelevantTaskList(taskList,taskClicked)) {
                    mTaskListAdapter.add(taskList);
                }
            }
            public void onChildChanged(DataSnapshot dataSnapshot, String s) {}
            public void onChildRemoved(DataSnapshot dataSnapshot) {

            }
            public void onChildMoved(DataSnapshot dataSnapshot, String s) {}
            public void onCancelled(DatabaseError databaseError) {}
        };
        mTaskListDatabaseReference.addChildEventListener(mChildEventListener2);
    }



    //Set the TaskMove dialog - showing the TaskLists as a ListView
    public static void setTaskMoveDialog(Activity activity){
        //Initialize TaskList Array, ListView and Adapter for the popup dialog ListView
        final ArrayList<TaskList> taskLists = new ArrayList<TaskList>();
        moveTaskDialog = new Dialog(activity,R.style.CustomDialog);
        moveTaskDialog.setContentView(R.layout.move_task_dialog);
        moveTaskDialog.setTitle("Choose a TaskList");
        moveTaskDialog.getWindow().getAttributes().windowAnimations = R.style.DialogTheme;
        taskListsView= (ListView) moveTaskDialog.findViewById(R.id.List);
        // Create an {@link TaskListAdapter}, whose data source is a list of {@link TaskList}s.
        mTaskListAdapter = new TaskListAdapter(activity, taskLists);
        taskListsView.setAdapter(mTaskListAdapter);
        moveTaskDialog.show();
    }
    //Move relevant task from it's current TaskList to the chosen TaskList
    //Update relevant TaskList's task count.
    public static void moveTaskToSelectedList(final Task task,
                                              final String currentUser, final DatabaseReference mTaskDatabaseReference,
                                              final FirebaseDatabase mFirebaseDatabase, final DatabaseReference mAllTasksDatabaseReference,
                                              final Activity activity, final Object adapter){
        //Initialize an onItemClickListener to allow the user to choose a TaskList
        //And then move the chosen task into that TaskList
        taskListsView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long l) {
                // Find the current task list that was clicked on
                TaskList currentTaskList = mTaskListAdapter.getItem(position);

                Log.d("bro", "onItemClick: "+currentTaskList.getTitle());

                //get the current task list's ID and title
                String currentTaskListId=currentTaskList.getId();
                String currentTaskListTitle=currentTaskList.getTitle();
                //Get references for that specific TaskList and the number of tasks in it
                DatabaseReference mTaskDatabaseReference2=mFirebaseDatabase.getReference().child("users")
                        .child(currentUser).child("TaskLists")
                        .child(currentTaskListId).child("tasks");
                final DatabaseReference  mTaskNumDatabaseReference2=mFirebaseDatabase.getReference().child("users")
                        .child(currentUser).child("TaskLists")
                        .child(currentTaskListId);
                final DatabaseReference  mTaskNumDatabaseReference=mFirebaseDatabase.getReference().child("users")
                        .child(currentUser).child("TaskLists")
                        .child(task.getTaskListId());

                //make changeTaskListFlag true - so SpecialTaskListActivity will respond to onChildChanged
                SpecialTaskListActivity.changeTaskListFlag=true;
                //Move the task inside the DB to another TaskList
                // and Update the task's current TaskList ID and title
                moveTaskFireBase(mTaskDatabaseReference,mTaskDatabaseReference2,task.getId(),currentTaskListId,currentTaskListTitle,currentUser,task,mFirebaseDatabase,mAllTasksDatabaseReference);

                if(adapter instanceof TaskAdapter){
                    ((TaskAdapter)adapter).remove(task);
                }

                //Set flag to true to avoid an infinite loop while updating the taskNum for that TaskList
                flagAdd=true;
                mTaskNumDatabaseReference2.addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        TaskList taskList=dataSnapshot.getValue(TaskList.class);
                        if(taskList!=null){
                            taskCountAdd =taskList.getTaskNum();
                            if(flagAdd) {
                                flagAdd=false;
                                mTaskNumDatabaseReference2.child("taskNum").setValue(taskCountAdd + 1);
                            }
                        }



                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {
                        System.out.println("The read failed: " + databaseError.getCode());
                    }
                });

                //Set flag to true to avoid an infinite loop while updating the taskNum for that TaskList
                flagDelete=true;
                mTaskNumDatabaseReference.addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        taskCountDelete = dataSnapshot.getValue(TaskList.class).getTaskNum();
                        if (flagDelete) {
                            flagDelete = false;
                            mTaskNumDatabaseReference.child("taskNum").setValue(taskCountDelete - 1);
                        }
                    }

                        @Override
                        public void onCancelled(DatabaseError databaseError) {
                            System.out.println("The read failed: " + databaseError.getCode());
                        }
                    });


                moveTaskDialog.dismiss();
                Toast.makeText(activity,"Task moved!", Toast.LENGTH_LONG).show();



            }
        });

    }

    //Show the user a dialog to confirm task deletion - and respond according to user choice.
    public static void confirmDeleteDialog(final Task taskClicked,final Activity activity, final DatabaseReference mTaskDatabaseReference,
                                     final DatabaseReference mAllTasksDatabaseReference,final Object adapter,
                                     final DatabaseReference mTaskNumDatabaseReference){
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        //Set the title
        builder.setTitle("Delete this task?");
        // Add the buttons
        builder.setPositiveButton("Delete", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                //Delete the selected task and cancel the reminder if it had one
                mTaskDatabaseReference.child(taskClicked.getId()).removeValue();
                mAllTasksDatabaseReference.child(taskClicked.getId()).removeValue();
                if(adapter instanceof ExpandableListAdapter){
                    ((ExpandableListAdapter)adapter).notifyDataSetChanged();
                }
                else{
                    ((TaskAdapter)adapter).remove(taskClicked);
                }
                //Check if it's not a completed task (if it is, no need to decrease TaskList's taskCount.
                if(!taskClicked.getCompleted()){
                    Log.d("completed?", "onClick: ");
                    //Set flag to true to avoid an infinite loop while updating the taskNum for that TaskList
                    flagDelete=true;
                    mTaskNumDatabaseReference.addValueEventListener(new ValueEventListener() {
                        @Override
                        public void onDataChange(DataSnapshot dataSnapshot) {
                            taskCountDelete =dataSnapshot.getValue(TaskList.class).getTaskNum();
                            Log.d("wat1234556-1", "onDataChange: ");
                            if(flagDelete) {
                                flagDelete=false;
                                Log.d("wat1234556-2", "onDataChange: ");
                                mTaskNumDatabaseReference.child("taskNum").setValue(taskCountDelete - 1);
                            }




                        }

                        @Override
                        public void onCancelled(DatabaseError databaseError) {
                            System.out.println("The read failed: " + databaseError.getCode());
                        }
                    });
                }

                if(taskClicked.getReminderDate()!=null){
                    TaskInfoFragment.cancelReminder(activity,AlarmReceiver.class,taskClicked.getIntId());
                }
                Toast.makeText(activity, "Task deleted!", Toast.LENGTH_LONG).show();
            }
        });
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                // User cancelled the dialog
                dialog.cancel();
            }
        });

        // Create the AlertDialog
        AlertDialog deleteDialog = builder.create();
        deleteDialog.getWindow().getAttributes().windowAnimations = R.style.DialogTheme;
        deleteDialog.show();
    }

    // "fromPath" and "toPath" are like directories in the DB - we move the task from one to the other.
    private static void moveTaskFireBase(final DatabaseReference fromPath, final DatabaseReference toPath, final String key,
                                         final String currentTaskListId,final String currentTaskListTitle,final String currentUser,
                                         final Task task,final FirebaseDatabase mFirebaseDatabase,final DatabaseReference mAllTasksDatabaseReference) {
        fromPath.child(key).addListenerForSingleValueEvent(new ValueEventListener() {
            // Now "DataSnapshot" holds the key and the value at the "fromPath".
            // So we copy it and transfer it to "toPath"
            //Then we delete the current task in "fromPath"
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                toPath.child(dataSnapshot.getKey())
                        .setValue(dataSnapshot.getValue(), new DatabaseReference.CompletionListener() {
                            @Override
                            public void onComplete(DatabaseError databaseError, DatabaseReference databaseReference) {
                                if (databaseError == null) {
                                    Log.i("TaskActivity", "onComplete: success");
                                    // In order to complete the move, we erase the original copy
                                    // by assigning null as its value.
                                    fromPath.child(key).setValue(null);
                                    //Update the Task's new TaskList details in the DB
                                    taskMoveUpdate(currentTaskListId,currentUser,
                                            currentTaskListTitle,task,mFirebaseDatabase,mAllTasksDatabaseReference);



                                }
                                else {
                                    Log.e("TaskActivity", "onComplete: failure:" + databaseError.getMessage() + ": "
                                            + databaseError.getDetails());
                                }
                            }
                        });
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.e("TaskActivity", "onCancelled: " + databaseError.getMessage() + ": "
                        + databaseError.getDetails());
            }
        });
    }

    //Check if it's a relevant TaskList - we don't want to show our current TaskLisk
    //Or one of the default TaskLists
    public static boolean isRelevantTaskList(TaskList taskList,Task taskClicked) {
        if(taskList.getId().equals(taskClicked.getTaskListId()) || taskList.getId().equals("Due TodayID")
                ||taskList.getId().equals("Due This WeekID")){
            return false;
        }
        return true;
    }

    //Update the task's current TaskList ID and title
    private static void taskMoveUpdate(final String currentTaskListId,final String currentUser,String currentTaskListTitle,
                                       final Task task,final FirebaseDatabase mFirebaseDatabase,final DatabaseReference mAllTasksDatabaseReference){
        DatabaseReference mTaskDatabaseReference2=mFirebaseDatabase.getReference().child("users")
                .child(currentUser).child("TaskLists")
                .child(currentTaskListId).child("tasks");
        mTaskDatabaseReference2.child(task.getId()).child("taskListId").setValue(currentTaskListId);
        mAllTasksDatabaseReference.child(task.getId()).child("taskListId").setValue(currentTaskListId);
        mTaskDatabaseReference2.child(task.getId()).child("taskListTitle").setValue(currentTaskListTitle);
        mAllTasksDatabaseReference.child(task.getId()).child("taskListTitle").setValue(currentTaskListTitle);
    }

    @SuppressLint("ClickableViewAccessibility")
    public static void setImageViewClickAnimation(ImageView createTaskListButton) {
        createTaskListButton.setOnTouchListener(new View.OnTouchListener() {

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN: {
                        //ImageView clicked/focused
                        ImageView view = (ImageView) v;
                        //overlay is black with transparency of 0x77 (119)
                        view.getDrawable().setColorFilter(0x37000000, PorterDuff.Mode.SRC_ATOP);
                        view.invalidate();
                        break;
                    }
                    case MotionEvent.ACTION_UP:
                    case MotionEvent.ACTION_CANCEL: {
                        //Imageview loses focus
                        ImageView view = (ImageView) v;
                        //clear the overlay
                        view.getDrawable().clearColorFilter();
                        view.invalidate();
                        break;
                    }
                }

                return false;
            }
        });
    }
}
