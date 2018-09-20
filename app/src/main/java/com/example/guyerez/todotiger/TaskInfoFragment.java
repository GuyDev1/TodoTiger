package com.example.guyerez.todotiger;

import android.os.Bundle;
import android.support.design.widget.TextInputEditText;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;


public class TaskInfoFragment extends Fragment {

    //Current task reference
    private Task currentTask;

    //View references - TextEdit, buttons, etc.
    private EditText mTaskTitle;
    private Button mSaveChangesButton;

    //FireBase DB references to change task info
    private FirebaseDatabase mFirebaseDatabase;
    private DatabaseReference mTaskDatabaseReference;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        //Inflate the layout for this fragment
        View rootView =inflater.inflate(R.layout.task_info, container, false);

        // Initialize Firebase components
        mFirebaseDatabase = FirebaseDatabase.getInstance();

        //set up Task DB reference
        mTaskDatabaseReference=mFirebaseDatabase.getReference().child("users")
                .child(MainActivity.getCurrentUserId())
                .child(MainActivity.getCurrentTaskListId()).child("tasks").child(currentTask.getId());

        // Initialize references to views
        mTaskTitle=rootView.findViewById(R.id.input_task_title);
        mTaskTitle.setText(currentTask.getTitle());
        mSaveChangesButton=rootView.findViewById(R.id.save_button);

        mSaveChangesButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Save edited task info
                mTaskDatabaseReference.child("title").setValue(mTaskTitle.getText().toString());

                //Recreate activity to update changes and go back to the TaskActivity
                getActivity().recreate();
                getActivity().onBackPressed();


            }
        });



        return rootView;
    }

    public void setCurrentTask(Task task) {
        this.currentTask = task;
    }
}

