package com.taskflow.demo.dto;

import java.util.List;

public class TaskListResponse {

    private List<TaskResponse> tasks;
    private int count;

    public TaskListResponse() {
    }

    public TaskListResponse(List<TaskResponse> tasks, int count) {
        this.tasks = tasks;
        this.count = count;
    }

    public List<TaskResponse> getTasks() {
        return tasks;
    }

    public void setTasks(List<TaskResponse> tasks) {
        this.tasks = tasks;
    }

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }
}
