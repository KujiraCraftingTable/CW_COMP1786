package com.example.expensetrackerapp.model;

import java.io.Serializable;

public class Project implements Serializable {

    private int id;
    private String projectCode;
    private String projectName;
    private String description;
    private String startDate;
    private String endDate;
    private String manager;
    private String status;
    private double budget;
    private String specialRequirements;
    private String clientDepartment;
    private String priority;
    private String notes;
    private String createdAt;
    /** 0 = not yet uploaded / modified since last upload, 1 = in sync with server */
    private int isSynced;

    public Project() {}

    public Project(String projectCode, String projectName, String description,
                   String startDate, String endDate, String manager,
                   String status, double budget, String specialRequirements,
                   String clientDepartment, String priority, String notes,
                   String createdAt) {
        this.projectCode = projectCode;
        this.projectName = projectName;
        this.description = description;
        this.startDate = startDate;
        this.endDate = endDate;
        this.manager = manager;
        this.status = status;
        this.budget = budget;
        this.specialRequirements = specialRequirements;
        this.clientDepartment = clientDepartment;
        this.priority = priority;
        this.notes = notes;
        this.createdAt = createdAt;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getProjectCode() { return projectCode; }
    public void setProjectCode(String projectCode) { this.projectCode = projectCode; }

    public String getProjectName() { return projectName; }
    public void setProjectName(String projectName) { this.projectName = projectName; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getStartDate() { return startDate; }
    public void setStartDate(String startDate) { this.startDate = startDate; }

    public String getEndDate() { return endDate; }
    public void setEndDate(String endDate) { this.endDate = endDate; }

    public String getManager() { return manager; }
    public void setManager(String manager) { this.manager = manager; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public double getBudget() { return budget; }
    public void setBudget(double budget) { this.budget = budget; }

    public String getSpecialRequirements() { return specialRequirements; }
    public void setSpecialRequirements(String specialRequirements) {
        this.specialRequirements = specialRequirements;
    }

    public String getClientDepartment() { return clientDepartment; }
    public void setClientDepartment(String clientDepartment) {
        this.clientDepartment = clientDepartment;
    }

    public String getPriority() { return priority; }
    public void setPriority(String priority) { this.priority = priority; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }

    public int getIsSynced() { return isSynced; }
    public void setIsSynced(int isSynced) { this.isSynced = isSynced; }

    public boolean isSynced() { return isSynced == 1; }
}
