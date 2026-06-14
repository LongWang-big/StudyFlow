package com.studyflow.service;

import com.studyflow.entity.StudyTimeRecord;

import java.util.Date;
import java.util.List;
import java.util.Map;

public interface StudyTimeService {

    /** 开始练习，记录开始时间。同一 taskId 不允许同时存在未结束的记录。 */
    StudyTimeRecord startPractice(Long taskId);

    /** 结束练习，计算时长并更新数据库。 */
    StudyTimeRecord finishPractice(Long id);

    /** 计算开始时间到结束时间的时长（分钟），四舍五入到小数点后 1 位。 */
    double calculateDuration(Date startTime, Date endTime);

    /** 查询某任务的全部学习记录，按开始时间倒序。 */
    List<StudyTimeRecord> getRecordsByTaskId(Long taskId);

    /** 统计某任务的累计学习时长（分钟），未结束的记录不计入。 */
    double getTotalDuration(Long taskId);

    /** 最近 N 天按日汇总：返回 List<Map>，每项含 "date"(String) 和 "totalMinutes"(Double)。 */
    List<Map<String, Object>> getDailySummary(Long taskId, int days);
}
