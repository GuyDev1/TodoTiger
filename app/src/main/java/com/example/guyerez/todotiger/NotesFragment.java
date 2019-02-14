package com.example.guyerez.todotiger;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

public class NotesFragment extends Fragment{
    private static boolean isAttached;


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        return inflater.inflate(R.layout.notes_fragment, container, false);

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

