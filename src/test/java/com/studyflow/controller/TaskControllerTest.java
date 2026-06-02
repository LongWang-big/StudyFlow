package com.studyflow.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.studyflow.entity.Task;
import com.studyflow.service.TaskService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * TaskController 集成测试 —— 使用 MockMvc 验证 HTTP 层行为。
 *
 * 在提取 GlobalExceptionHandler 之前建立基线，确保重构后行为不变。
 */
@WebMvcTest(TaskController.class)
class TaskControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private TaskService taskService;

    @Autowired
    private ObjectMapper objectMapper;

    // ==================== POST /api/tasks ====================

    /**
     * C1-1 正常路径：传入合法 task
     * 预期：返回 200，body 包含 title、priority、id
     */
    @Test
    void createTask_validTask_shouldReturn200WithTask() throws Exception {
        Task task = buildTask("复习高数", "HIGH", futureDate(1));
        Task created = buildTask("复习高数", "HIGH", futureDate(1));
        created.setId(1L);
        when(taskService.createTask(any(Task.class))).thenReturn(created);

        mockMvc.perform(post("/api/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(task)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("复习高数"))
                .andExpect(jsonPath("$.id").value(1));
    }

    /**
     * C1-2 校验失败：title 为空
     * 预期：返回 400，body 包含错误 message
     */
    @Test
    void createTask_blankTitle_shouldReturn400() throws Exception {
        Task task = buildTask("", "LOW", null);
        when(taskService.createTask(any(Task.class)))
                .thenThrow(new IllegalArgumentException("任务标题不能为空"));

        mockMvc.perform(post("/api/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(task)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("任务标题不能为空"));
    }

    /**
     * C1-3 校验失败：高优先级但无截止时间
     * 预期：返回 400，message 为 "高优先级任务必须设置截止时间"
     */
    @Test
    void createTask_highPriorityNoDeadline_shouldReturn400() throws Exception {
        Task task = buildTask("复习高数", "HIGH", null);
        when(taskService.createTask(any(Task.class)))
                .thenThrow(new IllegalArgumentException("高优先级任务必须设置截止时间"));

        mockMvc.perform(post("/api/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(task)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("高优先级任务必须设置截止时间"));
    }

    /**
     * C1-4 校验失败：截止时间已过期
     * 预期：返回 400
     */
    @Test
    void createTask_pastDeadline_shouldReturn400() throws Exception {
        Task task = buildTask("复习高数", "HIGH", pastDate(1));
        when(taskService.createTask(any(Task.class)))
                .thenThrow(new IllegalArgumentException("截止时间不能早于当前时间"));

        mockMvc.perform(post("/api/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(task)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("截止时间不能早于当前时间"));
    }

    // ==================== PUT /api/tasks/{id} ====================

    /**
     * C2-1 正常路径：更新 TODO 任务
     * 预期：返回 200 + 更新后的 task
     */
    @Test
    void updateTask_todoTask_shouldReturn200() throws Exception {
        Task update = buildTask("新标题", "MEDIUM", futureDate(1));
        Task updated = buildTask("新标题", "MEDIUM", futureDate(1));
        updated.setId(1L);
        when(taskService.updateTask(eq(1L), any(Task.class))).thenReturn(updated);

        mockMvc.perform(put("/api/tasks/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(update)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("新标题"));
    }

    /**
     * C2-2 异常路径：更新不存在的任务
     * 预期：返回 400
     */
    @Test
    void updateTask_taskNotFound_shouldReturn400() throws Exception {
        Task update = buildTask("任意", "LOW", null);
        when(taskService.updateTask(eq(999L), any(Task.class)))
                .thenThrow(new IllegalArgumentException("任务不存在"));

        mockMvc.perform(put("/api/tasks/999")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(update)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("任务不存在"));
    }

    /**
     * C2-3 异常路径：更新已完成任务
     * 预期：返回 400
     */
    @Test
    void updateTask_completedTask_shouldReturn400() throws Exception {
        Task update = buildTask("修改", "LOW", futureDate(1));
        when(taskService.updateTask(eq(1L), any(Task.class)))
                .thenThrow(new IllegalArgumentException("已完成任务不允许修改"));

        mockMvc.perform(put("/api/tasks/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(update)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("已完成任务不允许修改"));
    }

    // ==================== DELETE /api/tasks/{id} ====================

    /**
     * C3-1 正常路径：删除存在的任务
     * 预期：返回 200
     */
    @Test
    void deleteTask_existingTask_shouldReturn200() throws Exception {
        doNothing().when(taskService).deleteTask(1L);

        mockMvc.perform(delete("/api/tasks/1"))
                .andExpect(status().isOk());
    }

    /**
     * C3-2 异常路径：删除不存在的任务
     * 预期：返回 400
     */
    @Test
    void deleteTask_taskNotFound_shouldReturn400() throws Exception {
        doThrow(new IllegalArgumentException("任务不存在"))
                .when(taskService).deleteTask(999L);

        mockMvc.perform(delete("/api/tasks/999"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("任务不存在"));
    }

    // ==================== GET /api/tasks/{id} ====================

    /**
     * C4-1 正常路径：查询存在的任务
     * 预期：返回 200 + task 对象
     */
    @Test
    void getTaskById_existingTask_shouldReturn200() throws Exception {
        Task task = buildTask("复习高数", "HIGH", futureDate(1));
        task.setId(1L);
        when(taskService.getTaskById(1L)).thenReturn(task);

        mockMvc.perform(get("/api/tasks/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("复习高数"));
    }

    /**
     * C4-2 边界条件：查询不存在的 id
     * 预期：返回 404
     */
    @Test
    void getTaskById_notFound_shouldReturn404() throws Exception {
        when(taskService.getTaskById(999L)).thenReturn(null);

        mockMvc.perform(get("/api/tasks/999"))
                .andExpect(status().isNotFound());
    }

    // ==================== GET /api/tasks（筛选） ====================

    /**
     * C5-1 无参数：返回全部任务
     */
    @Test
    void getTasks_noParams_shouldReturnAll() throws Exception {
        List<Task> tasks = Arrays.asList(
                buildTask("任务1", "HIGH", futureDate(1)),
                buildTask("任务2", "LOW", futureDate(2))
        );
        when(taskService.getAllTasks()).thenReturn(tasks);

        mockMvc.perform(get("/api/tasks"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));
    }

    /**
     * C5-2 仅 status 参数
     */
    @Test
    void getTasks_statusOnly_shouldReturnFiltered() throws Exception {
        Task t = buildTask("TODO任务", "MEDIUM", futureDate(1));
        t.setStatus("TODO");
        when(taskService.getTasksByStatus("TODO")).thenReturn(Collections.singletonList(t));

        mockMvc.perform(get("/api/tasks").param("status", "TODO"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].title").value("TODO任务"));
    }

    /**
     * C5-3 仅 priority 参数
     */
    @Test
    void getTasks_priorityOnly_shouldReturnFiltered() throws Exception {
        Task t = buildTask("高优先级", "HIGH", futureDate(1));
        when(taskService.getTasksByPriority("HIGH")).thenReturn(Collections.singletonList(t));

        mockMvc.perform(get("/api/tasks").param("priority", "HIGH"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].priority").value("HIGH"));
    }

    /**
     * C5-4 status + priority 同时传入：走 Java 层 stream filter
     * 预期：返回同时满足两个条件的任务
     */
    @Test
    void getTasks_statusAndPriority_shouldReturnIntersection() throws Exception {
        Task match = buildTask("匹配", "HIGH", futureDate(1));
        match.setStatus("TODO");
        Task noMatch = buildTask("不匹配", "LOW", futureDate(1));
        noMatch.setStatus("TODO");

        when(taskService.getTasksByStatus("TODO")).thenReturn(Arrays.asList(match, noMatch));

        mockMvc.perform(get("/api/tasks")
                        .param("status", "TODO")
                        .param("priority", "HIGH"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].title").value("匹配"));
    }

    /**
     * C5-5 筛选结果为空
     */
    @Test
    void getTasks_noMatch_shouldReturnEmptyList() throws Exception {
        when(taskService.getTasksByStatus("COMPLETED")).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/tasks").param("status", "COMPLETED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    // ==================== PATCH /api/tasks/{id}/status ====================

    /**
     * C6-1 正常路径：TODO → IN_PROGRESS
     */
    @Test
    void updateStatus_todoToInProgress_shouldReturn200() throws Exception {
        doNothing().when(taskService).updateTaskStatus(1L, "IN_PROGRESS");

        mockMvc.perform(patch("/api/tasks/1/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"IN_PROGRESS\"}"))
                .andExpect(status().isOk());
    }

    /**
     * C6-2 异常路径：COMPLETED → TODO（非法转移）
     */
    @Test
    void updateStatus_completedToTodo_shouldReturn400() throws Exception {
        doThrow(new IllegalArgumentException("已完成任务不能回退到未开始状态"))
                .when(taskService).updateTaskStatus(1L, "TODO");

        mockMvc.perform(patch("/api/tasks/1/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"TODO\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("已完成任务不能回退到未开始状态"));
    }

    // ==================== GET /api/tasks/stats ====================

    /**
     * C7-1 统计接口返回正确字段结构
     */
    @Test
    void getStats_shouldReturnAllFields() throws Exception {
        when(taskService.getTotalCount()).thenReturn(10);
        when(taskService.getCompletedCount()).thenReturn(6);
        when(taskService.getUnfinishedCount()).thenReturn(4);
        when(taskService.getCompletionRate()).thenReturn(60.0);

        mockMvc.perform(get("/api/tasks/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCount").value(10))
                .andExpect(jsonPath("$.completedCount").value(6))
                .andExpect(jsonPath("$.unfinishedCount").value(4))
                .andExpect(jsonPath("$.completionRate").value(60.0));
    }

    // ==================== 工具方法 ====================

    private Task buildTask(String title, String priority, Date deadline) {
        Task task = new Task();
        task.setTitle(title);
        task.setPriority(priority);
        task.setDeadline(deadline);
        return task;
    }

    private Date futureDate(int daysFromNow) {
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_MONTH, daysFromNow);
        return cal.getTime();
    }

    private Date pastDate(int daysAgo) {
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_MONTH, -daysAgo);
        return cal.getTime();
    }
}
