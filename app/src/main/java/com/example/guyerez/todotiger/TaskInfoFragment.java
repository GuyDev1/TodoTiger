package com.example.guyerez.todotiger;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.support.design.widget.TextInputEditText;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;


public class TaskInfoFragment extends Fragment {

    //Current task reference
    private Task currentTask;

    //View references - TextEdit, buttons, etc.
    private EditText mTaskTitle;
    private EditText dueDate;
    private EditText mTaskNotes;
    private Button mSaveChangesButton;
    private Button mCancelButton;

    //FireBase DB references to change task info
    private FirebaseDatabase mFirebaseDatabase;
    private DatabaseReference mTaskDatabaseReference;

    //References for DatePicker
    private DatePickerDialog.OnDateSetListener date;
    private Calendar myCalendar;


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
                mTaskDatabaseReference.child("dueDate").setValue(dueDate.getText().toString());
                mTaskDatabaseReference.child("notes").setValue(mTaskNotes.getText().toString());

                //Recreate activity to update changes and go back to the TaskActivity
                getActivity().recreate();
                getActivity().onBackPressed();


            }
        });

        mCancelButton=rootView.findViewById(R.id.cancel_button);
        mCancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //Cancel changes and go back to task list
                getActivity().onBackPressed();
            }
        });



        //Initialize DatePicker related variables
        myCalendar = Calendar.getInstance();
        dueDate= (EditText) rootView.findViewById(R.id.date_picker);
        dueDate.setText(currentTask.getDueDate());
        date = new DatePickerDialog.OnDateSetListener() {

            //Set the DatePicker

            @Override
            public void onDateSet(DatePicker view, int year, int monthOfYear,
                                  int dayOfMonth) {
                myCalendar.set(Calendar.YEAR, year);
                myCalendar.set(Calendar.MONTH, monthOfYear);
                myCalendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                updateLabel();
            }

        };

        dueDate.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                new DatePickerDialog(getActivity(), date, myCalendar
                        .get(Calendar.YEAR), myCalendar.get(Calendar.MONTH),
                        myCalendar.get(Calendar.DAY_OF_MONTH)).show();
            }
        });


        mTaskNotes=rootView.findViewById(R.id.notes);
        mTaskNotes.setText(currentTask.getNotes());

        return rootView;
    }

    //Update the dueDate with the date the user selected
    private void updateLabel() {
        String myFormat = "dd/MM/yyyy";
        SimpleDateFormat sdf = new SimpleDateFormat(myFormat, Locale.getDefault());

        dueDate.setText("Due: " + sdf.format(myCalendar.getTime()));
    }

    public void setCurrentTask(Task task) {
        this.currentTask = task;
    }
}

