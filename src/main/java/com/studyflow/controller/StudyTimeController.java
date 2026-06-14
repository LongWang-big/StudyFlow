package com.studyflow.controller;

import com.studyflow.entity.StudyTimeRecord;
import com.studyflow.service.StudyTimeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/study-time")
public class StudyTimeController {

    @Autowired
    private StudyTimeService studyTimeService;

    /**
     * 开始练习计时
     * POST /api/study-time/start?taskId=1
     */
    @PostMapping("/start")
    public StudyTimeRecord startPractice(@RequestParam Long taskId) {
        return studyTimeService.startPractice(taskId);
    }

    /**
     * 结束练习计时
     * PATCH /api/study-time/{id}/finish
     */
    @PatchMapping("/{id}/finish")
    public StudyTimeRecord finishPractice(@PathVariable Long id) {
        return studyTimeService.finishPractice(id);
    }

    /**
     * 查询某任务的学习记录
     * GET /api/study-time/task/{taskId}
     */
    @GetMapping("/task/{taskId}")
    public List<StudyTimeRecord> getRecordsByTaskId(@PathVariable Long taskId) {
        return studyTimeService.getRecordsByTaskId(taskId);
    }

    /**
     * 查询统计信息
     * GET /api/study-time/stats?taskId=1&days=7
     */
    @GetMapping("/stats")
    public Map<String, Object> getStats(@RequestParam Long taskId,
                                        @RequestParam(defaultValue = "7") int days) {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalDuration", studyTimeService.getTotalDuration(taskId));
        stats.put("dailySummary", studyTimeService.getDailySummary(taskId, days));
        return stats;
    }
}
