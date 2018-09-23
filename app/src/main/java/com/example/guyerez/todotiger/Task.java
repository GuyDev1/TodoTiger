package com.example.guyerez.todotiger;
/**
 * An {@link Task} object contains information about a specific task
 */
public class Task {

    /** The task's title */
    private String mTitle="";

    /** Task completed or not */
    private boolean mCompleted=false;

    /** The task's ID on FireBase DB */
    private String mId;

    /** The task's creation date */
    private String mCreationDate;

    /** The task's due date */
    private String mDueDate;

    /** The task's completion date */
    private String mCompletionDate;

    /** The task's notes */
    private String mNotes;



    public Task() {
        // Default constructor required for calls to DataSnapshot.getValue(Task.class)
    }

    /**
     * Constructs a new {@link Task} object.
     * @param title is the task's title
     * @param completed indicates whether the task is completed or not
     * @param id is the task's ID from FireBase
     * @param creationDate is the task's creation date
     * @param dueDate is the task's due date
     * @param notes is the task related notes
     */

    public Task(String title, boolean completed, String id, String creationDate,String dueDate,String notes) {
        mTitle = title;
        mCompleted=completed;
        mId=id;
        mCreationDate=creationDate;
        mDueDate=dueDate;
        mNotes=notes;
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
     * Sets the task's ID.
     */
    public void setId(String id) {
        mId=id;
    }

    /**
     * Sets the task's creation date.
     */
    public void setCreationDate(String creationDate) {
        mCreationDate=creationDate;
    }


    /**
     * Sets the task's due date.
     */
    public void setDueDate(String dueDate) {
        mDueDate=dueDate;
    }

    /**
     * Sets the task's completion date.
     */
    public void setCompletionDate(String completionDate) {
        mCompletionDate=completionDate;
    }

    /**
     * Sets the task's notes.
     */
    public void setNotes(String notes) {
        mNotes=notes;
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

    /**
     * Returns the task's ID.
     */
    public String getId() {
        return mId;
    }

    /**
     * Returns the task's creation date.
     */
    public String getCreationDate() {
        return mCreationDate;
    }

    /**
     * Returns the task's due date.
     */
    public String getCompletionDate() {
        return mCompletionDate;
    }

    /**
     * Returns the task's due date.
     */
    public String getDueDate() {
        return mDueDate;
    }

    /**
     * Returns the task's notes.
     */
    public String getNotes() {
        return mNotes;
    }

}


