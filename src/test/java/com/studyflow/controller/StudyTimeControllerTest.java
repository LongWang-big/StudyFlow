package com.studyflow.controller;

import com.studyflow.entity.StudyTimeRecord;
import com.studyflow.service.StudyTimeService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.*;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * StudyTimeController 集成测试 —— 使用 MockMvc 验证 HTTP 层行为。
 */
@WebMvcTest(StudyTimeController.class)
class StudyTimeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private StudyTimeService studyTimeService;

    // ==================== POST /api/study-time/start ====================

    /**
     * C1-1 正常路径：开始练习
     * 预期：返回 200，body 包含 taskId、startTime
     */
    @Test
    void startPractice_validTaskId_shouldReturn200() throws Exception {
        StudyTimeRecord record = buildRecord(1L, 1L, pastMinutes(30));
        when(studyTimeService.startPractice(1L)).thenReturn(record);

        mockMvc.perform(post("/api/study-time/start").param("taskId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.taskId").value(1))
                .andExpect(jsonPath("$.startTime").isNotEmpty());
    }

    /**
     * C1-2 异常路径：任务不存在
     * 预期：返回 400，body 包含错误 message
     */
    @Test
    void startPractice_taskNotFound_shouldReturn400() throws Exception {
        when(studyTimeService.startPractice(999L))
                .thenThrow(new IllegalArgumentException("任务不存在"));

        mockMvc.perform(post("/api/study-time/start").param("taskId", "999"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("任务不存在"));
    }

    /**
     * C1-3 异常路径：重复开始练习
     * 预期：返回 400
     */
    @Test
    void startPractice_duplicate_shouldReturn400() throws Exception {
        when(studyTimeService.startPractice(1L))
                .thenThrow(new IllegalArgumentException("已有进行中的练习，请先结束"));

        mockMvc.perform(post("/api/study-time/start").param("taskId", "1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("已有进行中的练习，请先结束"));
    }

    // ==================== PATCH /api/study-time/{id}/finish ====================

    /**
     * C2-1 正常路径：结束练习
     * 预期：返回 200，body 包含 durationMinutes
     */
    @Test
    void finishPractice_validRecord_shouldReturn200() throws Exception {
        StudyTimeRecord record = buildRecord(1L, 1L, pastMinutes(30));
        record.setEndTime(new Date());
        record.setDurationMinutes(30.0);
        when(studyTimeService.finishPractice(1L)).thenReturn(record);

        mockMvc.perform(patch("/api/study-time/1/finish"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.durationMinutes").value(30.0));
    }

    /**
     * C2-2 异常路径：记录不存在
     * 预期：返回 400
     */
    @Test
    void finishPractice_recordNotFound_shouldReturn400() throws Exception {
        when(studyTimeService.finishPractice(999L))
                .thenThrow(new IllegalArgumentException("学习记录不存在"));

        mockMvc.perform(patch("/api/study-time/999/finish"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("学习记录不存在"));
    }

    /**
     * C2-3 边界条件：已结束的记录再次结束
     * 预期：返回 400
     */
    @Test
    void finishPractice_alreadyFinished_shouldReturn400() throws Exception {
        when(studyTimeService.finishPractice(1L))
                .thenThrow(new IllegalArgumentException("该次练习已结束"));

        mockMvc.perform(patch("/api/study-time/1/finish"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("该次练习已结束"));
    }

    // ==================== GET /api/study-time/task/{taskId} ====================

    /**
     * C3-1 正常路径：查询有记录的任务
     * 预期：返回 200 + 列表
     */
    @Test
    void getRecordsByTaskId_hasRecords_shouldReturn200WithList() throws Exception {
        StudyTimeRecord r1 = buildRecord(1L, 1L, pastMinutes(30));
        StudyTimeRecord r2 = buildRecord(2L, 1L, pastMinutes(60));
        when(studyTimeService.getRecordsByTaskId(1L)).thenReturn(Arrays.asList(r1, r2));

        mockMvc.perform(get("/api/study-time/task/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));
    }

    /**
     * C3-2 边界条件：无记录
     * 预期：返回 200 + 空列表
     */
    @Test
    void getRecordsByTaskId_noRecords_shouldReturn200WithEmptyList() throws Exception {
        when(studyTimeService.getRecordsByTaskId(999L)).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/study-time/task/999"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    // ==================== GET /api/study-time/stats ====================

    /**
     * C4-1 正常路径：查询统计信息
     * 预期：返回 200 + totalDuration + dailySummary
     */
    @Test
    void getStats_validTaskId_shouldReturn200WithStats() throws Exception {
        when(studyTimeService.getTotalDuration(1L)).thenReturn(120.5);

        Map<String, Object> daily1 = new LinkedHashMap<>();
        daily1.put("date", "2026-06-14");
        daily1.put("totalMinutes", 120.5);
        when(studyTimeService.getDailySummary(eq(1L), eq(7)))
                .thenReturn(Collections.singletonList(daily1));

        mockMvc.perform(get("/api/study-time/stats")
                        .param("taskId", "1")
                        .param("days", "7"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalDuration").value(120.5))
                .andExpect(jsonPath("$.dailySummary", hasSize(1)))
                .andExpect(jsonPath("$.dailySummary[0].date").value("2026-06-14"));
    }

    /**
     * C4-2 边界条件：days 参数使用默认值
     * 预期：默认 days=7
     */
    @Test
    void getStats_defaultDays_shouldUseSeven() throws Exception {
        when(studyTimeService.getTotalDuration(1L)).thenReturn(0.0);
        when(studyTimeService.getDailySummary(eq(1L), eq(7)))
                .thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/study-time/stats").param("taskId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalDuration").value(0.0));

        verify(studyTimeService).getDailySummary(1L, 7);
    }

    // ==================== 工具方法 ====================

    private StudyTimeRecord buildRecord(Long id, Long taskId, Date startTime) {
        StudyTimeRecord record = new StudyTimeRecord();
        record.setId(id);
        record.setTaskId(taskId);
        record.setStartTime(startTime);
        return record;
    }

    private Date pastMinutes(int minutes) {
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.MINUTE, -minutes);
        return cal.getTime();
    }
}
