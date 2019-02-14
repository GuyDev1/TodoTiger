package com.example.guyerez.todotiger;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;


public class NotesFragment extends Fragment{
    private static boolean isAttached;
    //View references - TextEdit, buttons, etc.
    private EditText mNotes;
    private Button mSaveChangesButton;
    private Button mCancelButton;


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        final TaskInfoFragment taskInfoFragment = ((TaskInfoFragment)NotesFragment.this.getParentFragment());
        View rootView = inflater.inflate(R.layout.notes_fragment, container, false);
        mNotes=rootView.findViewById(R.id.notes_text);

        mCancelButton=rootView.findViewById(R.id.cancel_notes_button);
        mCancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //Cancel changes and go back to TaskInfoFragment
                getActivity().onBackPressed();
            }
        });
        mSaveChangesButton=rootView.findViewById(R.id.save_notes_button);
        mSaveChangesButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //save notes and go back to TaskInfoFragment
                taskInfoFragment.setNotesText(mNotes.getText().toString());
                getActivity().onBackPressed();
            }
        });


        return rootView;

    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        isAttached=true;


    }

    @Override
    public void onDetach() {
        super.onDetach();
        isAttached=false;
    }

    public static boolean isAttached(){
        return isAttached;
    }


}

