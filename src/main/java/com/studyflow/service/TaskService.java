package com.studyflow.service;

import com.studyflow.entity.Task;

import java.util.List;

public interface TaskService {

    Task createTask(Task task);

    Task updateTask(Long id, Task task);

    void deleteTask(Long id);

    Task getTaskById(Long id);

    List<Task> getAllTasks();

    List<Task> getTasksByStatus(String status);

    List<Task> getTasksByPriority(String priority);

    void updateTaskStatus(Long id, String status);

    int getTotalCount();

    int getCompletedCount();

    int getUnfinishedCount();

    double getCompletionRate();
}
