package com.example.guyerez.todotiger;

import java.util.Date;

/**
 * An {@link TaskList} object contains information about a task list
 */
public class TaskList {

    /** The TaskList's title */
    private String mTitle="";

    /** The number of tasks in the list */
    private int mTaskNum=0;

    /** The task list's ID on FireBase DB */
    private String mId;

    /** The TaskList's creation date */
    private Date mCreationDate;


    public TaskList() {
        // Default constructor required for calls to DataSnapshot.getValue(TaskList.class)
    }

    /**
     * Constructs a new {@link TaskList} object.
     * @param title is the title of the TaskList
     * @param id is the TaskList's ID from FireBase
     * @param creationDate is the TaskList's ID from FireBase
     */

    public TaskList(String title, String id,Date creationDate) {
        mTitle = title;
        mId=id;
        mCreationDate=creationDate;
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
     * Sets the TaskList's creation date.
     */
    public void setCreationDate(Date creationDate) {
        mCreationDate=creationDate;
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

    /**
     * Returns the TaskList's creation date.
     */
    public Date getCreationDate() {
        return mCreationDate;
    }

    }

