package com.example.guyerez.todotiger;
/**
 * An {@link TaskList} object contains information about a task list
 */
public class TaskList {

    /** The list's title */
    private String mTitle="";

    /** The number of tasks in the list */
    private int mTaskNum=0;


    public TaskList() {
        // Default constructor required for calls to DataSnapshot.getValue(TaskList.class)
    }

    /**
     * Constructs a new {@link TaskList} object.
     * @param title is the title of the task list
     */

    public TaskList(String title) {
        mTitle = title;
    }

    /**
     * Sets the list's title.
     */
    public void setTitle(String title) {
        mTitle=title;
    }

    /**
     * Sets the number of tasks in the list.
     */
    public void setTaskNum(int taskNum) {
        mTaskNum=taskNum;
    }

    /**
     * Returns the list's title.
     */
    public String getTitle() {
            return mTitle;
        }

    /**
     * Returns the number of tasks in the list.
     */
    public int getTaskNum() {
        return mTaskNum;
    }

    }

