package com.studyflow.service;

import com.studyflow.entity.Task;
import com.studyflow.mapper.TaskMapper;
import com.studyflow.service.impl.TaskServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TaskServiceImplTest {

    @Mock
    private TaskMapper taskMapper;

    @InjectMocks
    private TaskServiceImpl taskService;

    // ==================== createTask ====================

    /**
     * T1-1 正常路径：传入合法 task（title / priority / deadline 均有效）
     * 预期：返回带 id 的对象，createTime 不为 null
     */
    @Test
    void createTask_validTask_shouldCallInsertAndReturnTask() {
        Task task = buildTask("复习高数", "HIGH", futureDate(1));
        when(taskMapper.insert(any(Task.class))).thenReturn(1);

        Task result = taskService.createTask(task);

        assertSame(task, result);
        verify(taskMapper).insert(task);
    }

    /**
     * T1-2 正常路径：priority = "MEDIUM" 且 deadline = null
     * 预期：正常创建，不报错
     */
    @Test
    void createTask_mediumPriorityNoDeadline_shouldSucceed() {
        Task task = buildTask("复习高数", "MEDIUM", null);
        when(taskMapper.insert(any(Task.class))).thenReturn(1);

        Task result = taskService.createTask(task);

        assertNotNull(result);
        verify(taskMapper).insert(task);
    }

    /**
     * T1-3 正常路径：显式设置 status = "IN_PROGRESS"
     * 预期：返回对象 status 为 "IN_PROGRESS"，不被覆盖
     */
    @Test
    void createTask_explicitStatus_shouldNotBeOverridden() {
        Task task = buildTask("复习高数", "LOW", null);
        task.setStatus("IN_PROGRESS");
        when(taskMapper.insert(any(Task.class))).thenReturn(1);

        Task result = taskService.createTask(task);

        assertEquals("IN_PROGRESS", result.getStatus());
    }

    /**
     * T1-4 业务不变量：不设置 status（传 null）
     * 预期：status 默认为 "TODO"
     */
    @Test
    void createTask_nullStatus_shouldDefaultToTODO() {
        Task task = buildTask("复习高数", "LOW", null);
        task.setStatus(null);
        when(taskMapper.insert(any(Task.class))).thenReturn(1);

        Task result = taskService.createTask(task);

        assertEquals("TODO", result.getStatus());
    }

    /**
     * T1-5 边界条件：title 为纯空格 "   "
     * 预期：抛出 IllegalArgumentException("任务标题不能为空")
     */
    @Test
    void createTask_blankTitle_shouldThrowIllegalArgument() {
        Task task = buildTask("   ", "LOW", null);

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> taskService.createTask(task)
        );
        assertEquals("任务标题不能为空", ex.getMessage());
    }

    /**
     * T1-6 异常输入：priority = "HIGH" 且 deadline = null
     * 预期：抛出 IllegalArgumentException("高优先级任务必须设置截止时间")
     */
    @Test
    void createTask_highPriorityWithoutDeadline_shouldThrowIllegalArgument() {
        Task task = buildTask("复习高数", "HIGH", null);

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> taskService.createTask(task)
        );
        assertEquals("高优先级任务必须设置截止时间", ex.getMessage());
    }

    /**
     * T1-7 异常输入：deadline 为过去时间
     * 预期：抛出 IllegalArgumentException("截止时间不能早于当前时间")
     */
    @Test
    void createTask_pastDeadline_shouldThrowIllegalArgument() {
        Task task = buildTask("复习高数", "HIGH", pastDate(1));

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> taskService.createTask(task)
        );
        assertEquals("截止时间不能早于当前时间", ex.getMessage());
    }

    // ==================== updateTask ====================

    /**
     * T2-1 正常路径：更新 TODO 任务的 title
     * 预期：返回更新后对象，字段已变更
     */
    @Test
    void updateTask_todoTask_shouldReturnUpdatedTask() {
        Task existing = buildTask("旧标题", "MEDIUM", futureDate(1));
        existing.setId(1L);
        existing.setStatus("TODO");
        when(taskMapper.selectById(1L)).thenReturn(existing);
        when(taskMapper.updateById(any(Task.class))).thenReturn(1);

        Task update = buildTask("新标题", "MEDIUM", futureDate(1));
        Task result = taskService.updateTask(1L, update);

        assertEquals(1L, result.getId());
        verify(taskMapper).updateById(update);
    }

    /**
     * T2-2 边界条件：更新 COMPLETED 状态的任务
     * 预期：抛出 IllegalArgumentException("已完成任务不允许修改")
     */
    @Test
    void updateTask_completedTask_shouldThrowIllegalArgument() {
        Task existing = buildTask("已完成任务", "LOW", futureDate(1));
        existing.setId(1L);
        existing.setStatus("COMPLETED");
        when(taskMapper.selectById(1L)).thenReturn(existing);

        Task update = buildTask("修改标题", "LOW", futureDate(1));

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> taskService.updateTask(1L, update)
        );
        assertEquals("已完成任务不允许修改", ex.getMessage());
    }

    /**
     * T2-3 异常输入：id 对应的任务不存在
     * 预期：抛出 IllegalArgumentException("任务不存在")
     */
    @Test
    void updateTask_taskNotFound_shouldThrowIllegalArgument() {
        when(taskMapper.selectById(999L)).thenReturn(null);

        Task update = buildTask("任意标题", "LOW", null);

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> taskService.updateTask(999L, update)
        );
        assertEquals("任务不存在", ex.getMessage());
    }

    /**
     * T2-4 异常输入：id 为 null
     * 预期：mapper.selectById(null) 返回 null，触发 "任务不存在"
     */
    @Test
    void updateTask_nullId_shouldThrowIllegalArgument() {
        Task update = buildTask("标题", "LOW", null);

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> taskService.updateTask(null, update)
        );
        assertEquals("任务不存在", ex.getMessage());
    }

    // ==================== deleteTask ====================

    /**
     * T3-1 正常路径：删除一个存在的任务
     * 预期：方法正常返回
     */
    @Test
    void deleteTask_existingTask_shouldSucceed() {
        Task existing = buildTask("任务", "LOW", null);
        existing.setId(1L);
        when(taskMapper.selectById(1L)).thenReturn(existing);
        when(taskMapper.deleteById(1L)).thenReturn(1);

        assertDoesNotThrow(() -> taskService.deleteTask(1L));
        verify(taskMapper).deleteById(1L);
    }

    /**
     * T3-2 边界条件：删除后调用 getTaskById(id)
     * 预期：返回 null
     */
    @Test
    void deleteTask_afterDelete_getByIdShouldReturnNull() {
        Task existing = buildTask("任务", "LOW", null);
        existing.setId(1L);
        when(taskMapper.selectById(1L))
                .thenReturn(existing)
                .thenReturn(null);
        when(taskMapper.deleteById(1L)).thenReturn(1);

        taskService.deleteTask(1L);
        Task result = taskService.getTaskById(1L);

        assertNull(result);
    }

    /**
     * T3-3 边界条件：重复删除同一个 id
     * 预期：第二次抛出 IllegalArgumentException("任务不存在")
     */
    @Test
    void deleteTask_duplicateDelete_shouldThrowIllegalArgument() {
        Task existing = buildTask("任务", "LOW", null);
        existing.setId(1L);
        when(taskMapper.selectById(1L))
                .thenReturn(existing)
                .thenReturn(null);
        when(taskMapper.deleteById(1L)).thenReturn(1);

        taskService.deleteTask(1L);

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> taskService.deleteTask(1L)
        );
        assertEquals("任务不存在", ex.getMessage());
    }

    /**
     * T3-4 业务不变量：删除任务 A 后，查询任务 B
     * 预期：任务 B 不受影响，数据完整
     */
    @Test
    void deleteTask_shouldNotAffectOtherTasks() {
        Task taskA = buildTask("任务A", "LOW", null);
        taskA.setId(1L);
        Task taskB = buildTask("任务B", "HIGH", futureDate(1));
        taskB.setId(2L);

        when(taskMapper.selectById(1L)).thenReturn(taskA);
        when(taskMapper.deleteById(1L)).thenReturn(1);
        when(taskMapper.selectById(2L)).thenReturn(taskB);

        taskService.deleteTask(1L);
        Task result = taskService.getTaskById(2L);

        assertNotNull(result);
        assertEquals("任务B", result.getTitle());
        assertEquals("HIGH", result.getPriority());
    }

    // ==================== getTaskById ====================

    /**
     * T4-1 正常路径：查询存在的 id
     * 预期：返回完整 Task 对象
     */
    @Test
    void getTaskById_existingTask_shouldReturnTask() {
        Task task = buildTask("复习高数", "HIGH", futureDate(1));
        task.setId(1L);
        when(taskMapper.selectById(1L)).thenReturn(task);

        Task result = taskService.getTaskById(1L);

        assertNotNull(result);
        assertEquals("复习高数", result.getTitle());
    }

    /**
     * T4-2 边界条件：查询不存在的 id
     * 预期：返回 null，不抛异常
     */
    @Test
    void getTaskById_notExistingId_shouldReturnNull() {
        when(taskMapper.selectById(999L)).thenReturn(null);

        Task result = taskService.getTaskById(999L);

        assertNull(result);
    }

    /**
     * T4-3 正常路径：查询刚创建的任务
     * 预期：返回对象的 id、title、status、createTime 均正确
     */
    @Test
    void getTaskById_newlyCreatedTask_shouldReturnCompleteInfo() {
        Task task = buildTask("新建任务", "MEDIUM", futureDate(3));
        task.setId(1L);
        task.setStatus("TODO");
        task.setCreateTime(new Date());
        when(taskMapper.selectById(1L)).thenReturn(task);

        Task result = taskService.getTaskById(1L);

        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("新建任务", result.getTitle());
        assertEquals("TODO", result.getStatus());
        assertNotNull(result.getCreateTime());
    }

    // ==================== getAllTasks ====================

    /**
     * T5-1 边界条件：数据库为空
     * 预期：返回空列表（size() == 0），不返回 null
     */
    @Test
    void getAllTasks_emptyDatabase_shouldReturnEmptyList() {
        when(taskMapper.selectAll()).thenReturn(Collections.emptyList());

        List<Task> result = taskService.getAllTasks();

        assertNotNull(result);
        assertEquals(0, result.size());
    }

    /**
     * T5-2 正常路径：数据库有多条任务
     * 预期：返回包含所有记录的列表
     */
    @Test
    void getAllTasks_multipleTasks_shouldReturnAll() {
        Task t1 = buildTask("任务1", "HIGH", futureDate(1));
        Task t2 = buildTask("任务2", "LOW", futureDate(2));
        when(taskMapper.selectAll()).thenReturn(Arrays.asList(t1, t2));

        List<Task> result = taskService.getAllTasks();

        assertEquals(2, result.size());
    }

    // ==================== getTasksByStatus ====================

    /**
     * T6-1 正常路径：传入 "TODO"，存在匹配任务
     * 预期：返回所有 TODO 状态的任务
     */
    @Test
    void getTasksByStatus_todo_shouldReturnTodoTasks() {
        Task t1 = buildTask("任务1", "HIGH", futureDate(1));
        t1.setStatus("TODO");
        when(taskMapper.selectByStatus("TODO")).thenReturn(Collections.singletonList(t1));

        List<Task> result = taskService.getTasksByStatus("TODO");

        assertEquals(1, result.size());
        assertEquals("TODO", result.get(0).getStatus());
    }

    /**
     * T6-2 边界条件：传入 null
     * 预期：返回空列表，不抛异常
     */
    @Test
    void getTasksByStatus_null_shouldReturnEmptyList() {
        when(taskMapper.selectByStatus(null)).thenReturn(Collections.emptyList());

        List<Task> result = taskService.getTasksByStatus(null);

        assertNotNull(result);
        assertEquals(0, result.size());
    }

    /**
     * T6-3 边界条件：传入无效值 "DONE"
     * 预期：返回空列表
     */
    @Test
    void getTasksByStatus_invalidValue_shouldReturnEmptyList() {
        when(taskMapper.selectByStatus("DONE")).thenReturn(Collections.emptyList());

        List<Task> result = taskService.getTasksByStatus("DONE");

        assertEquals(0, result.size());
    }

    // ==================== getTasksByPriority ====================

    /**
     * T7-1 正常路径：传入 "HIGH"，存在匹配任务
     * 预期：返回所有高优先级任务
     */
    @Test
    void getTasksByPriority_high_shouldReturnHighTasks() {
        Task t1 = buildTask("任务1", "HIGH", futureDate(1));
        when(taskMapper.selectByPriority("HIGH")).thenReturn(Collections.singletonList(t1));

        List<Task> result = taskService.getTasksByPriority("HIGH");

        assertEquals(1, result.size());
        assertEquals("HIGH", result.get(0).getPriority());
    }

    /**
     * T7-2 边界条件：传入 null
     * 预期：返回空列表，不抛异常
     */
    @Test
    void getTasksByPriority_null_shouldReturnEmptyList() {
        when(taskMapper.selectByPriority(null)).thenReturn(Collections.emptyList());

        List<Task> result = taskService.getTasksByPriority(null);

        assertNotNull(result);
        assertEquals(0, result.size());
    }

    /**
     * T7-3 边界条件：传入无效值 "URGENT"
     * 预期：返回空列表
     */
    @Test
    void getTasksByPriority_invalidValue_shouldReturnEmptyList() {
        when(taskMapper.selectByPriority("URGENT")).thenReturn(Collections.emptyList());

        List<Task> result = taskService.getTasksByPriority("URGENT");

        assertEquals(0, result.size());
    }

    // ==================== updateTaskStatus ====================

    /**
     * T8-1 正常路径："TODO" → "IN_PROGRESS"
     * 预期：状态更新成功
     */
    @Test
    void updateTaskStatus_todoToInProgress_shouldSucceed() {
        Task existing = buildTask("任务", "MEDIUM", futureDate(1));
        existing.setId(1L);
        existing.setStatus("TODO");
        when(taskMapper.selectById(1L)).thenReturn(existing);
        when(taskMapper.updateById(any(Task.class))).thenReturn(1);

        taskService.updateTaskStatus(1L, "IN_PROGRESS");

        verify(taskMapper).updateById(argThat(task -> "IN_PROGRESS".equals(task.getStatus())));
    }

    /**
     * T8-2 正常路径："IN_PROGRESS" → "COMPLETED"
     * 预期：状态更新成功
     */
    @Test
    void updateTaskStatus_inProgressToCompleted_shouldSucceed() {
        Task existing = buildTask("任务", "MEDIUM", futureDate(1));
        existing.setId(1L);
        existing.setStatus("IN_PROGRESS");
        when(taskMapper.selectById(1L)).thenReturn(existing);
        when(taskMapper.updateById(any(Task.class))).thenReturn(1);

        taskService.updateTaskStatus(1L, "COMPLETED");

        verify(taskMapper).updateById(argThat(task -> "COMPLETED".equals(task.getStatus())));
    }

    /**
     * T8-3 边界条件："COMPLETED" → "TODO"
     * 预期：抛出 IllegalArgumentException("已完成任务不能回退到未开始状态")
     */
    @Test
    void updateTaskStatus_completedToTodo_shouldThrowIllegalArgument() {
        Task existing = buildTask("任务", "LOW", futureDate(1));
        existing.setId(1L);
        existing.setStatus("COMPLETED");
        when(taskMapper.selectById(1L)).thenReturn(existing);

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> taskService.updateTaskStatus(1L, "TODO")
        );
        assertEquals("已完成任务不能回退到未开始状态", ex.getMessage());
    }

    /**
     * T8-4 边界条件："COMPLETED" → "IN_PROGRESS"
     * 预期：当前实现允许，状态更新成功
     */
    @Test
    void updateTaskStatus_completedToInProgress_shouldSucceed() {
        Task existing = buildTask("任务", "LOW", futureDate(1));
        existing.setId(1L);
        existing.setStatus("COMPLETED");
        when(taskMapper.selectById(1L)).thenReturn(existing);
        when(taskMapper.updateById(any(Task.class))).thenReturn(1);

        assertDoesNotThrow(() -> taskService.updateTaskStatus(1L, "IN_PROGRESS"));
        verify(taskMapper).updateById(argThat(task -> "IN_PROGRESS".equals(task.getStatus())));
    }

    /**
     * T8-5 业务不变量：连续流转 TODO → IN_PROGRESS → COMPLETED
     * 预期：全链路正常，最终状态为 "COMPLETED"
     */
    @Test
    void updateTaskStatus_fullChain_shouldComplete() {
        Task existing = buildTask("任务", "MEDIUM", futureDate(1));
        existing.setId(1L);
        existing.setStatus("TODO");
        when(taskMapper.selectById(1L)).thenReturn(existing);
        when(taskMapper.updateById(any(Task.class))).thenReturn(1);

        taskService.updateTaskStatus(1L, "IN_PROGRESS");
        existing.setStatus("IN_PROGRESS");
        taskService.updateTaskStatus(1L, "COMPLETED");

        verify(taskMapper, times(2)).updateById(any(Task.class));
    }

    // ==================== getTotalCount ====================

    /**
     * T9-1 边界条件：数据库为空
     * 预期：返回 0
     */
    @Test
    void getTotalCount_emptyDatabase_shouldReturnZero() {
        when(taskMapper.countAll()).thenReturn(0);

        assertEquals(0, taskService.getTotalCount());
    }

    /**
     * T9-2 正常路径：数据库有 3 条任务
     * 预期：返回 3
     */
    @Test
    void getTotalCount_threeTasks_shouldReturnThree() {
        when(taskMapper.countAll()).thenReturn(3);

        assertEquals(3, taskService.getTotalCount());
    }

    // ==================== getCompletedCount ====================

    /**
     * T10-1 边界条件：无已完成任务
     * 预期：返回 0
     */
    @Test
    void getCompletedCount_noCompleted_shouldReturnZero() {
        when(taskMapper.countByStatus("COMPLETED")).thenReturn(0);

        assertEquals(0, taskService.getCompletedCount());
    }

    /**
     * T10-2 正常路径：有 2 个 COMPLETED 任务
     * 预期：返回 2
     */
    @Test
    void getCompletedCount_twoCompleted_shouldReturnTwo() {
        when(taskMapper.countByStatus("COMPLETED")).thenReturn(2);

        assertEquals(2, taskService.getCompletedCount());
    }

    // ==================== getUnfinishedCount ====================

    /**
     * T11-1 边界条件：全部完成
     * 预期：返回 0
     */
    @Test
    void getUnfinishedCount_allCompleted_shouldReturnZero() {
        when(taskMapper.countAll()).thenReturn(5);
        when(taskMapper.countByStatus("COMPLETED")).thenReturn(5);

        assertEquals(0, taskService.getUnfinishedCount());
    }

    /**
     * T11-2 边界条件：数据库为空
     * 预期：返回 0
     */
    @Test
    void getUnfinishedCount_emptyDatabase_shouldReturnZero() {
        when(taskMapper.countAll()).thenReturn(0);
        when(taskMapper.countByStatus("COMPLETED")).thenReturn(0);

        assertEquals(0, taskService.getUnfinishedCount());
    }

    /**
     * T11-3 正常路径：总数 5，已完成 3
     * 预期：返回 2
     */
    @Test
    void getUnfinishedCount_partialCompleted_shouldReturnDifference() {
        when(taskMapper.countAll()).thenReturn(5);
        when(taskMapper.countByStatus("COMPLETED")).thenReturn(3);

        assertEquals(2, taskService.getUnfinishedCount());
    }

    // ==================== getCompletionRate ====================

    /**
     * T12-1 边界条件：任务总数为 0
     * 预期：返回 0.0，不抛除零异常
     */
    @Test
    void getCompletionRate_noTasks_shouldReturnZero() {
        when(taskMapper.countAll()).thenReturn(0);

        assertEquals(0.0, taskService.getCompletionRate(), 0.001);
    }

    /**
     * T12-2 正常路径：全部完成
     * 预期：返回 100.0
     */
    @Test
    void getCompletionRate_allCompleted_shouldReturn100() {
        when(taskMapper.countAll()).thenReturn(4);
        when(taskMapper.countByStatus("COMPLETED")).thenReturn(4);

        assertEquals(100.0, taskService.getCompletionRate(), 0.001);
    }

    /**
     * T12-3 正常路径：3 个任务完成 1 个
     * 预期：返回约 33.33，精度误差 < 0.01
     */
    @Test
    void getCompletionRate_oneOfThree_shouldReturnApprox33() {
        when(taskMapper.countAll()).thenReturn(3);
        when(taskMapper.countByStatus("COMPLETED")).thenReturn(1);

        assertEquals(33.33, taskService.getCompletionRate(), 0.01);
    }

    /**
     * T12-4 正常路径：全部未完成
     * 预期：返回 0.0
     */
    @Test
    void getCompletionRate_noneCompleted_shouldReturnZero() {
        when(taskMapper.countAll()).thenReturn(5);
        when(taskMapper.countByStatus("COMPLETED")).thenReturn(0);

        assertEquals(0.0, taskService.getCompletionRate(), 0.001);
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
