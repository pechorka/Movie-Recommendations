package ru.surf.course.movierecommendations.models;

/**
 * Created by andrew on 1/29/17.
 */

public class CrewMember extends People {

    private String mCreditId;
    private String mDepartment;
    private String mJob;

    public CrewMember(String name, int id, String profilePath, String mCreditId, String mDepartment, String mJob) {
        super(name, id, profilePath);
        this.mCreditId = mCreditId;
        this.mDepartment = mDepartment;
        this.mJob = mJob;
    }

    public String getCreditId() {
        return mCreditId;
    }

    public void setCreditId(String mCreditId) {
        this.mCreditId = mCreditId;
    }

    public String getDepartment() {
        return mDepartment;
    }

    public void setDepartment(String mDepartment) {
        this.mDepartment = mDepartment;
    }

    public String getJob() {
        return mJob;
    }

    public void setJob(String mJob) {
        this.mJob = mJob;
    }
}
