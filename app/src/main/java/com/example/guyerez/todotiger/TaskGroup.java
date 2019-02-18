package com.example.guyerez.todotiger;

import java.util.ArrayList;
import java.util.List;

public class TaskGroup {

    public String taskListTitle;
    public String taskListId;
    public final List<Task> tasks = new ArrayList<Task>();

    public TaskGroup(String taskListTitle,String taskListId) {
        this.taskListTitle = taskListTitle;
        this.taskListId=taskListId;
    }
}
