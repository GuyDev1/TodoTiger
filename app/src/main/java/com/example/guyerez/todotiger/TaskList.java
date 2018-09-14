package com.example.guyerez.todotiger;
/**
 * An {@link TaskList} object contains information about a task list
 */
public class TaskList {

    /** The list's title */
    private String mTitle;

    /** The number of tasks in the list */
    private int mTaskNum=0;

    /**
     * Constructs a new {@link TaskList} object.
     * @param title is the title of the task list
     */

    public TaskList(String title) {
        mTitle = title;
    }

    /**
     * Returns the list's title.
     */
    public int setTaskNum() {
        return mTaskNum;
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

