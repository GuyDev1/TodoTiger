package com.example.guyerez.todotiger;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.ListView;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Set the content of the activity to use the activity_main.xml layout file - the task lists
        setContentView(R.layout.activity_main);

        //create a temporary list of task lists.
        ArrayList<TaskList> taskLists = new ArrayList<TaskList>();
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

    }
}
