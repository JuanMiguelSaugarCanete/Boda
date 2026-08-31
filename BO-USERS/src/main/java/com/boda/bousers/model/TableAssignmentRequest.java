package com.boda.bousers.model;

public class TableAssignmentRequest {

    private String table;

    public TableAssignmentRequest() {
    }

    public TableAssignmentRequest(String table) {
        this.table = table;
    }

    public String getTable() {
        return table;
    }

    public void setTable(String table) {
        this.table = table;
    }
}