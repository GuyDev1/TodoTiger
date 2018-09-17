package com.example.guyerez.todotiger;
/**
 * An {@link Task} object contains information about a specific task
 */
public class Task {

    /** The task's title */
    private String mTitle="";

    /** Task completed or not */
    private boolean mCompleted=false;


    public Task() {
        // Default constructor required for calls to DataSnapshot.getValue(Task.class)
    }

    /**
     * Constructs a new {@link Task} object.
     * @param title is the title of the task list
     * @param completed indicates whether the task is completed or not
     */

    public Task(String title, boolean completed) {
        mTitle = title;
        mCompleted=completed;
    }

    /**
     * Sets the task's title.
     */
    public void setTitle(String title) {
        mTitle=title;
    }

    /**
     * Sets the task's completion.
     */
    public void setCompleted(boolean completed) {
        mCompleted=completed;
    }


    /**
     * Returns the task's title.
     */
    public String getTitle() {
        return mTitle;
    }

    /**
     * Returns whether task is completed.
     */
    public boolean getCompleted() {
        return mCompleted;
    }

}


