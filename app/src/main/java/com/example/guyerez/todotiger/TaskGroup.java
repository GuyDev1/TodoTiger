package com.example.guyerez.todotiger;

import java.util.ArrayList;
import java.util.List;

public class TaskGroup {

    //The taskList title and ID (of this taskGroup)
    public String taskListTitle;
    public String taskListId;
    //An array of tasks that belong to this TaskList
    public final List<Task> tasks = new ArrayList<Task>();

    public TaskGroup(String taskListTitle,String taskListId) {
        this.taskListTitle = taskListTitle;
        this.taskListId=taskListId;
    }
}
