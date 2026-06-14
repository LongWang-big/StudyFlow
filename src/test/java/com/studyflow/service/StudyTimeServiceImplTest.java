package com.studyflow.service;

import com.studyflow.entity.StudyTimeRecord;
import com.studyflow.entity.Task;
import com.studyflow.mapper.StudyTimeMapper;
import com.studyflow.mapper.TaskMapper;
import com.studyflow.service.impl.StudyTimeServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StudyTimeServiceImplTest {

    @Mock
    private StudyTimeMapper studyTimeMapper;

    @Mock
    private TaskMapper taskMapper;

    @InjectMocks
    private StudyTimeServiceImpl studyTimeService;

    // ==================== startPractice ====================

    /**
     * T1-1 正常路径：任务存在且无未结束记录
     * 预期：插入新记录并返回
     */
    @Test
    void startPractice_validTask_shouldInsertAndReturn() {
        Task task = buildTask(1L, "复习高数");
        when(taskMapper.selectById(1L)).thenReturn(task);
        when(studyTimeMapper.selectByTaskId(1L)).thenReturn(Collections.emptyList());
        when(studyTimeMapper.insert(any(StudyTimeRecord.class))).thenReturn(1);

        StudyTimeRecord result = studyTimeService.startPractice(1L);

        assertNotNull(result);
        assertEquals(1L, result.getTaskId());
        assertNotNull(result.getStartTime());
        assertNull(result.getEndTime());
        verify(studyTimeMapper).insert(any(StudyTimeRecord.class));
    }

    /**
     * T1-2 异常路径：任务不存在
     * 预期：抛出 IllegalArgumentException("任务不存在")
     */
    @Test
    void startPractice_taskNotFound_shouldThrowIllegalArgument() {
        when(taskMapper.selectById(999L)).thenReturn(null);

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> studyTimeService.startPractice(999L)
        );
        assertEquals("任务不存在", ex.getMessage());
    }

    /**
     * T1-3 边界条件：任务已有未结束的记录
     * 预期：抛出 IllegalArgumentException("已有进行中的练习，请先结束")
     */
    @Test
    void startPractice_existingUnfinished_shouldThrowIllegalArgument() {
        Task task = buildTask(1L, "复习高数");
        when(taskMapper.selectById(1L)).thenReturn(task);

        StudyTimeRecord unfinished = new StudyTimeRecord();
        unfinished.setId(10L);
        unfinished.setTaskId(1L);
        unfinished.setEndTime(null);
        when(studyTimeMapper.selectByTaskId(1L)).thenReturn(Collections.singletonList(unfinished));

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> studyTimeService.startPractice(1L)
        );
        assertEquals("已有进行中的练习，请先结束", ex.getMessage());
        verify(studyTimeMapper, never()).insert(any(StudyTimeRecord.class));
    }

    /**
     * T1-4 异常路径：taskId 为 null
     * 预期：抛出 IllegalArgumentException("taskId 不能为空")
     */
    @Test
    void startPractice_nullTaskId_shouldThrowIllegalArgument() {
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> studyTimeService.startPractice(null)
        );
        assertEquals("taskId 不能为空", ex.getMessage());
    }

    // ==================== finishPractice ====================

    /**
     * T2-1 正常路径：结束一条未结束的记录
     * 预期：更新 endTime 和 durationMinutes，返回更新后的记录
     */
    @Test
    void finishPractice_unfinishedRecord_shouldUpdateAndReturn() {
        StudyTimeRecord unfinished = new StudyTimeRecord();
        unfinished.setId(1L);
        unfinished.setTaskId(1L);
        unfinished.setStartTime(pastMinutes(30));
        unfinished.setEndTime(null);

        StudyTimeRecord finished = new StudyTimeRecord();
        finished.setId(1L);
        finished.setTaskId(1L);
        finished.setStartTime(unfinished.getStartTime());
        finished.setEndTime(new Date());
        finished.setDurationMinutes(30.0);

        // 第一次 selectById 返回未结束记录，第二次返回已结束记录
        when(studyTimeMapper.selectById(1L)).thenReturn(unfinished, finished);
        when(studyTimeMapper.updateEndTime(eq(1L), any(Date.class), any(Double.class))).thenReturn(1);

        StudyTimeRecord result = studyTimeService.finishPractice(1L);

        assertNotNull(result);
        assertNotNull(result.getEndTime());
        assertNotNull(result.getDurationMinutes());
        verify(studyTimeMapper).updateEndTime(eq(1L), any(Date.class), any(Double.class));
    }

    /**
     * T2-2 异常路径：记录不存在
     * 预期：抛出 IllegalArgumentException("学习记录不存在")
     */
    @Test
    void finishPractice_recordNotFound_shouldThrowIllegalArgument() {
        when(studyTimeMapper.selectById(999L)).thenReturn(null);

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> studyTimeService.finishPractice(999L)
        );
        assertEquals("学习记录不存在", ex.getMessage());
    }

    /**
     * T2-3 边界条件：对已结束的记录再次结束
     * 预期：抛出 IllegalArgumentException("该次练习已结束")
     */
    @Test
    void finishPractice_alreadyFinished_shouldThrowIllegalArgument() {
        StudyTimeRecord record = new StudyTimeRecord();
        record.setId(1L);
        record.setEndTime(new Date());
        when(studyTimeMapper.selectById(1L)).thenReturn(record);

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> studyTimeService.finishPractice(1L)
        );
        assertEquals("该次练习已结束", ex.getMessage());
    }

    /**
     * T2-4 异常路径：id 为 null
     * 预期：抛出 IllegalArgumentException("记录 id 不能为空")
     */
    @Test
    void finishPractice_nullId_shouldThrowIllegalArgument() {
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> studyTimeService.finishPractice(null)
        );
        assertEquals("记录 id 不能为空", ex.getMessage());
    }

    /**
     * T2-5 竞态条件：updateEndTime 返回 0（已被并发完成）
     * 预期：抛出 IllegalArgumentException("该次练习已结束")
     */
    @Test
    void finishPractice_concurrentFinish_shouldThrowIllegalArgument() {
        StudyTimeRecord record = new StudyTimeRecord();
        record.setId(1L);
        record.setStartTime(pastMinutes(30));
        record.setEndTime(null);
        when(studyTimeMapper.selectById(1L)).thenReturn(record);
        when(studyTimeMapper.updateEndTime(eq(1L), any(Date.class), any(Double.class))).thenReturn(0);

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> studyTimeService.finishPractice(1L)
        );
        assertEquals("该次练习已结束", ex.getMessage());
    }

    // ==================== calculateDuration ====================

    /**
     * T3-1 正常路径：30 分钟
     * 预期：返回 30.0
     */
    @Test
    void calculateDuration_30Minutes_shouldReturn30() {
        Date start = pastMinutes(30);
        Date end = new Date();

        double result = studyTimeService.calculateDuration(start, end);

        assertEquals(30.0, result, 0.5);
    }

    /**
     * T3-2 边界条件：相同时间
     * 预期：返回 0.0
     */
    @Test
    void calculateDuration_sameTime_shouldReturnZero() {
        Date time = new Date();

        double result = studyTimeService.calculateDuration(time, time);

        assertEquals(0.0, result, 0.001);
    }

    /**
     * T3-3 边界条件：startTime 为 null
     * 预期：抛出 IllegalArgumentException
     */
    @Test
    void calculateDuration_nullStart_shouldThrowIllegalArgument() {
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> studyTimeService.calculateDuration(null, new Date())
        );
        assertEquals("开始时间和结束时间不能为空", ex.getMessage());
    }

    /**
     * T3-4 边界条件：endTime 为 null
     * 预期：抛出 IllegalArgumentException
     */
    @Test
    void calculateDuration_nullEnd_shouldThrowIllegalArgument() {
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> studyTimeService.calculateDuration(new Date(), null)
        );
        assertEquals("开始时间和结束时间不能为空", ex.getMessage());
    }

    /**
     * T3-5 边界条件：endTime 早于 startTime
     * 预期：抛出 IllegalArgumentException("结束时间不能早于开始时间")
     */
    @Test
    void calculateDuration_endBeforeStart_shouldThrowIllegalArgument() {
        Date start = new Date();
        Date end = pastMinutes(30);

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> studyTimeService.calculateDuration(start, end)
        );
        assertEquals("结束时间不能早于开始时间", ex.getMessage());
    }

    // ==================== getRecordsByTaskId ====================

    /**
     * T4-1 正常路径：查询有记录的任务
     * 预期：返回记录列表
     */
    @Test
    void getRecordsByTaskId_hasRecords_shouldReturnList() {
        StudyTimeRecord r1 = new StudyTimeRecord();
        r1.setId(1L);
        r1.setTaskId(1L);
        StudyTimeRecord r2 = new StudyTimeRecord();
        r2.setId(2L);
        r2.setTaskId(1L);
        when(studyTimeMapper.selectByTaskId(1L)).thenReturn(Arrays.asList(r1, r2));

        List<StudyTimeRecord> result = studyTimeService.getRecordsByTaskId(1L);

        assertEquals(2, result.size());
    }

    /**
     * T4-2 边界条件：查询无记录的任务
     * 预期：返回空列表
     */
    @Test
    void getRecordsByTaskId_noRecords_shouldReturnEmptyList() {
        when(studyTimeMapper.selectByTaskId(999L)).thenReturn(Collections.emptyList());

        List<StudyTimeRecord> result = studyTimeService.getRecordsByTaskId(999L);

        assertNotNull(result);
        assertEquals(0, result.size());
    }

    /**
     * T4-3 异常路径：taskId 为 null
     * 预期：抛出 IllegalArgumentException("taskId 不能为空")
     */
    @Test
    void getRecordsByTaskId_nullTaskId_shouldThrowIllegalArgument() {
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> studyTimeService.getRecordsByTaskId(null)
        );
        assertEquals("taskId 不能为空", ex.getMessage());
    }

    // ==================== getTotalDuration ====================

    /**
     * T5-1 正常路径：有累计时长
     * 预期：返回累计值
     */
    @Test
    void getTotalDuration_hasRecords_shouldReturnSum() {
        when(studyTimeMapper.sumDurationByTaskId(1L)).thenReturn(120.5);

        double result = studyTimeService.getTotalDuration(1L);

        assertEquals(120.5, result, 0.001);
    }

    /**
     * T5-2 边界条件：无任何记录
     * 预期：返回 0.0
     */
    @Test
    void getTotalDuration_noRecords_shouldReturnZero() {
        when(studyTimeMapper.sumDurationByTaskId(999L)).thenReturn(0.0);

        double result = studyTimeService.getTotalDuration(999L);

        assertEquals(0.0, result, 0.001);
    }

    /**
     * T5-3 异常路径：taskId 为 null
     * 预期：抛出 IllegalArgumentException("taskId 不能为空")
     */
    @Test
    void getTotalDuration_nullTaskId_shouldThrowIllegalArgument() {
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> studyTimeService.getTotalDuration(null)
        );
        assertEquals("taskId 不能为空", ex.getMessage());
    }

    // ==================== getDailySummary ====================

    /**
     * T6-1 正常路径：有学习记录
     * 预期：返回按日期分组的汇总
     */
    @Test
    void getDailySummary_hasRecords_shouldReturnDailyMap() {
        StudyTimeRecord r1 = new StudyTimeRecord();
        r1.setStartTime(new Date());
        r1.setDurationMinutes(30.0);
        StudyTimeRecord r2 = new StudyTimeRecord();
        r2.setStartTime(new Date());
        r2.setDurationMinutes(45.0);
        when(studyTimeMapper.selectRecentByTaskId(1L, 7)).thenReturn(Arrays.asList(r1, r2));

        List<Map<String, Object>> result = studyTimeService.getDailySummary(1L, 7);

        assertNotNull(result);
        assertFalse(result.isEmpty());
        // 同一天的两条记录应该合并
        assertEquals(1, result.size());
        assertEquals(75.0, (Double) result.get(0).get("totalMinutes"), 0.5);
    }

    /**
     * T6-2 边界条件：传入 0 天
     * 预期：抛出 IllegalArgumentException("天数必须大于 0")
     */
    @Test
    void getDailySummary_zeroDays_shouldThrowIllegalArgument() {
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> studyTimeService.getDailySummary(1L, 0)
        );
        assertEquals("天数必须大于 0", ex.getMessage());
    }

    /**
     * T6-3 边界条件：传入负数天数
     * 预期：抛出 IllegalArgumentException("天数必须大于 0")
     */
    @Test
    void getDailySummary_negativeDays_shouldThrowIllegalArgument() {
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> studyTimeService.getDailySummary(1L, -1)
        );
        assertEquals("天数必须大于 0", ex.getMessage());
    }

    /**
     * T6-4 异常路径：taskId 为 null
     * 预期：抛出 IllegalArgumentException("taskId 不能为空")
     */
    @Test
    void getDailySummary_nullTaskId_shouldThrowIllegalArgument() {
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> studyTimeService.getDailySummary(null, 7)
        );
        assertEquals("taskId 不能为空", ex.getMessage());
    }

    // ==================== 工具方法 ====================

    private Task buildTask(Long id, String title) {
        Task task = new Task();
        task.setId(id);
        task.setTitle(title);
        task.setPriority("MEDIUM");
        task.setStatus("TODO");
        return task;
    }

    private Date pastMinutes(int minutes) {
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.MINUTE, -minutes);
        return cal.getTime();
    }
}
