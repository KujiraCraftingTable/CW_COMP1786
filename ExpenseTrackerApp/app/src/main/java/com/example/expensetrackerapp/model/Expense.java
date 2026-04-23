package com.example.expensetrackerapp.model;

import java.io.Serializable;

public class Expense implements Serializable {

    private int id;
    private int projectId;
    private String expenseCode;
    private String expenseDate;
    private double amount;
    private String currency;
    private String expenseType;
    private String paymentMethod;
    private String claimant;
    private String paymentStatus;
    private String description;
    private String location;
    private String receiptNumber;
    private String createdAt;

    public Expense() {}

    public Expense(int projectId, String expenseCode, String expenseDate,
                   double amount, String currency, String expenseType,
                   String paymentMethod, String claimant, String paymentStatus,
                   String description, String location, String receiptNumber,
                   String createdAt) {
        this.projectId = projectId;
        this.expenseCode = expenseCode;
        this.expenseDate = expenseDate;
        this.amount = amount;
        this.currency = currency;
        this.expenseType = expenseType;
        this.paymentMethod = paymentMethod;
        this.claimant = claimant;
        this.paymentStatus = paymentStatus;
        this.description = description;
        this.location = location;
        this.receiptNumber = receiptNumber;
        this.createdAt = createdAt;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getProjectId() { return projectId; }
    public void setProjectId(int projectId) { this.projectId = projectId; }

    public String getExpenseCode() { return expenseCode; }
    public void setExpenseCode(String expenseCode) { this.expenseCode = expenseCode; }

    public String getExpenseDate() { return expenseDate; }
    public void setExpenseDate(String expenseDate) { this.expenseDate = expenseDate; }

    public double getAmount() { return amount; }
    public void setAmount(double amount) { this.amount = amount; }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }

    public String getExpenseType() { return expenseType; }
    public void setExpenseType(String expenseType) { this.expenseType = expenseType; }

    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }

    public String getClaimant() { return claimant; }
    public void setClaimant(String claimant) { this.claimant = claimant; }

    public String getPaymentStatus() { return paymentStatus; }
    public void setPaymentStatus(String paymentStatus) { this.paymentStatus = paymentStatus; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public String getReceiptNumber() { return receiptNumber; }
    public void setReceiptNumber(String receiptNumber) { this.receiptNumber = receiptNumber; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
}
