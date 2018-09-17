package com.example.guyerez.todotiger;

import android.content.Context;
import android.support.v4.content.ContextCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Locale;

/**
 * {@link TaskListAdapter} is an {@link ArrayAdapter} that can provide the layout for each task list item
 * based on a data source, which is a list of {@link TaskList} objects.
 */

public class TaskListAdapter extends ArrayAdapter<TaskList> {

    /**
     * Create a new {@link TaskListAdapter} object.
     *
     * @param context is the current context (i.e. Activity) that the adapter is being created in.
     * @param taskLists is the list of {@link TaskList}s to be displayed.
     */


    public TaskListAdapter(Context context, ArrayList<TaskList> taskLists) {
        super(context, 0, taskLists);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        // Check if an existing view is being reused, otherwise inflate the view
        View listItemView = convertView;
        if (listItemView == null) {
            listItemView = LayoutInflater.from(getContext()).inflate(
                    R.layout.tlist_item, parent, false);
        }
        // Get the {@link TaskList} object located at this position in the list
        TaskList currentTaskList = getItem(position);

        // Locate the TextView in the tlist_item.xml layout with the ID list_title.
        TextView titleTextView = (TextView) listItemView.findViewById(R.id.list_title);
        // Get the task list's title from the currentTaskList object and set it in the text view
        titleTextView.setText(currentTaskList.getTitle());

        // Find the TextView in the tlist_item.xml layout with the ID num_of_tasks.
        TextView numOfTasks = (TextView) listItemView.findViewById(R.id.num_of_tasks);
        // Get the task list's number of tasks from the currentTaskList object and set it in the text view
        numOfTasks.setText(String.format(Locale.getDefault(), "%d", currentTaskList.getTaskNum()));
        // Return the whole list item layout (containing 2 TextViews) so that it can be shown in the ListView.
        return listItemView;
    }
}
