package com.studyflow.service.impl;

import com.studyflow.entity.Task;
import com.studyflow.mapper.TaskMapper;
import com.studyflow.service.TaskService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TaskServiceImpl implements TaskService {

    @Autowired
    private TaskMapper taskMapper;

    @Override
    public Task createTask(Task task) {
        throw new UnsupportedOperationException("TODO");
    }

    @Override
    public Task updateTask(Long id, Task task) {
        Task existing = taskMapper.selectById(id);
        if (existing == null) {
            throw new IllegalArgumentException("任务不存在");
        }
        if ("COMPLETED".equals(existing.getStatus())) {
            throw new IllegalArgumentException("已完成任务不允许修改");
        }
        task.setId(id);
        taskMapper.updateById(task);
        return taskMapper.selectById(id);
    }

    @Override
    public void deleteTask(Long id) {
        Task existing = taskMapper.selectById(id);
        if (existing == null) {
            throw new IllegalArgumentException("任务不存在");
        }
        taskMapper.deleteById(id);
    }

    @Override
    public Task getTaskById(Long id) {
        return taskMapper.selectById(id);
    }

    @Override
    public List<Task> getAllTasks() {
        return taskMapper.selectAll();
    }

    @Override
    public List<Task> getTasksByStatus(String status) {
        return taskMapper.selectByStatus(status);
    }

    @Override
    public List<Task> getTasksByPriority(String priority) {
        return taskMapper.selectByPriority(priority);
    }

    @Override
    public void updateTaskStatus(Long id, String status) {
        throw new UnsupportedOperationException("TODO");
    }

    @Override
    public int getTotalCount() {
        return taskMapper.countAll();
    }

    @Override
    public int getCompletedCount() {
        return taskMapper.countByStatus("COMPLETED");
    }

    @Override
    public int getUnfinishedCount() {
        return getTotalCount() - getCompletedCount();
    }

    @Override
    public double getCompletionRate() {
        int total = getTotalCount();
        if (total == 0) {
            return 0.0;
        }
        return (double) getCompletedCount() / total * 100;
    }
}
