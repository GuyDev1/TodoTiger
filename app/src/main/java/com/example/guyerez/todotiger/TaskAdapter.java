package com.example.guyerez.todotiger;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.TextView;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.ArrayList;
import java.util.Locale;

/**
 * {@link TaskAdapter} is an {@link ArrayAdapter} that can provide the layout for each task item
 * based on a data source, which is a list of {@link Task} objects.
 */

public class TaskAdapter extends ArrayAdapter<Task> {

    //Define FireBase instance variables
    private DatabaseReference mTaskDatabaseReference;
    private FirebaseDatabase mFirebaseDatabase;
    /**
     * Create a new {@link TaskAdapter} object.
     *
     * @param context is the current context (i.e. Activity) that the adapter is being created in.
     * @param tasks is the list of {@link Task}s to be displayed.
     */


    public TaskAdapter(Context context, ArrayList<Task> tasks) {
        super(context, 0, tasks);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        // Check if an existing view is being reused, otherwise inflate the view
        View listItemView = convertView;
        if (listItemView == null) {
            listItemView = LayoutInflater.from(getContext()).inflate(
                    R.layout.task_item, parent, false);
        }
        // Get the {@link Task} object located at this position in the list
        final Task currentTask = getItem(position);

        // Locate the TextView in the task_item.xml layout with the ID task_title.
        final TextView titleTextView = (TextView) listItemView.findViewById(R.id.task_title);
        // Get the task's title from the currentTask object and set it in the text view
        titleTextView.setText(currentTask.getTitle());
        //If the task is completed - title Strikethrough
        titleTextView.setBackgroundResource(strikeCompleted(currentTask.getCompleted()));

        //Initialize the check box and check it if the task was completed.
        CheckBox checkBox = (CheckBox) listItemView.findViewById(R.id.check_box);
        checkBox.setChecked(currentTask.getCompleted());

        //Initialize the creation date TextView in the task_item.xml layout with the ID creation_date
        TextView creationDateTextView = (TextView) listItemView.findViewById(R.id.creation_date);
        //Get the task's creation date from the currentTask object and set it in the text view
        creationDateTextView.setText(currentTask.getCreationDate());

        // Initialize Firebase DB
        mFirebaseDatabase = FirebaseDatabase.getInstance();
        //Get the task DB reference to edit task completion status
        mTaskDatabaseReference=mFirebaseDatabase.getReference()
                .child("users").child(MainActivity.getCurrentUserId())
                .child(MainActivity.getCurrentTaskListId()).child("tasks").child(currentTask.getId());
        // Find the CheckBox in the task_item.xml layout with the ID check_box.

        checkBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView,boolean isChecked) {
                if(isChecked){
                    titleTextView.setBackgroundResource(R.drawable.strike_through);
                    mTaskDatabaseReference.child("completed").setValue(true);
                    }
                    else{
                    titleTextView.setBackgroundResource(0);
                    mTaskDatabaseReference.child("completed").setValue(false);
                    }
            }
            }
        );

        // Return the whole list item layout (containing 1 text view and 1 checkbox) so that it can be shown in the ListView.
        return listItemView;
    }
    private int strikeCompleted(boolean completed){
        if (completed){
            return R.drawable.strike_through;
        }
        else{
            return 0;
        }
    }
}
