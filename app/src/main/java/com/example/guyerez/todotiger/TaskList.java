package com.example.guyerez.todotiger;
/**
 * An {@link TaskList} object contains information about a task list
 */
public class TaskList {

    /** The list's title */
    public String mTitle="";

    /** The number of tasks in the list */
    public int mTaskNum=0;

    /** The task list's ID on FireBase DB */
    public String mId;


    public TaskList() {
        // Default constructor required for calls to DataSnapshot.getValue(TaskList.class)
    }

    /**
     * Constructs a new {@link TaskList} object.
     * @param title is the title of the task list
     * @param id is the list's ID from FireBase
     */

    public TaskList(String title, String id,int taskNum ) {
        mTitle = title;
        mId=id;
        mTaskNum=taskNum;

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
     * Sets the list's ID.
     */
    public void setId(String id) {
        mId=id;
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

    /**
     * Returns the list's ID from FireBase.
     */
    public String getId() {
        return mId;
    }

    }

