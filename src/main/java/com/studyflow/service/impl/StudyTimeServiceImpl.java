package com.studyflow.service.impl;

import com.studyflow.entity.StudyTimeRecord;
import com.studyflow.entity.Task;
import com.studyflow.mapper.StudyTimeMapper;
import com.studyflow.mapper.TaskMapper;
import com.studyflow.service.StudyTimeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.text.SimpleDateFormat;
import java.util.*;

@Service
public class StudyTimeServiceImpl implements StudyTimeService {

    @Autowired
    private StudyTimeMapper studyTimeMapper;

    @Autowired
    private TaskMapper taskMapper;

    @Override
    public StudyTimeRecord startPractice(Long taskId) {
        // 校验任务是否存在
        Task task = taskMapper.selectById(taskId);
        if (task == null) {
            throw new IllegalArgumentException("任务不存在");
        }

        // 校验该任务是否已有未结束的记录
        List<StudyTimeRecord> records = studyTimeMapper.selectByTaskId(taskId);
        for (StudyTimeRecord record : records) {
            if (record.getEndTime() == null) {
                throw new IllegalArgumentException("已有进行中的练习，请先结束");
            }
        }

        // 插入新记录
        StudyTimeRecord record = new StudyTimeRecord();
        record.setTaskId(taskId);
        record.setStartTime(new Date());
        studyTimeMapper.insert(record);
        return record;
    }

    @Override
    public StudyTimeRecord finishPractice(Long id) {
        // 查询记录是否存在
        StudyTimeRecord record = studyTimeMapper.selectById(id);
        if (record == null) {
            throw new IllegalArgumentException("学习记录不存在");
        }

        // 校验是否已结束
        if (record.getEndTime() != null) {
            throw new IllegalArgumentException("该次练习已结束");
        }

        // 计算时长并更新
        Date endTime = new Date();
        double duration = calculateDuration(record.getStartTime(), endTime);
        studyTimeMapper.updateEndTime(id, endTime, duration);

        // 返回更新后的记录
        return studyTimeMapper.selectById(id);
    }

    @Override
    public double calculateDuration(Date startTime, Date endTime) {
        if (startTime == null || endTime == null) {
            throw new IllegalArgumentException("开始时间和结束时间不能为空");
        }
        long startMs = startTime.getTime();
        long endMs = endTime.getTime();
        double minutes = (endMs - startMs) / 60000.0;
        // 四舍五入到小数点后 1 位
        return Math.round(minutes * 10.0) / 10.0;
    }

    @Override
    public List<StudyTimeRecord> getRecordsByTaskId(Long taskId) {
        return studyTimeMapper.selectByTaskId(taskId);
    }

    @Override
    public double getTotalDuration(Long taskId) {
        Double total = studyTimeMapper.sumDurationByTaskId(taskId);
        return total != null ? total : 0.0;
    }

    @Override
    public List<Map<String, Object>> getDailySummary(Long taskId, int days) {
        if (days <= 0) {
            throw new IllegalArgumentException("天数必须大于 0");
        }

        List<StudyTimeRecord> records = studyTimeMapper.selectRecentByTaskId(taskId, days);

        // 按日期分组汇总
        Map<String, Double> dailyMap = new LinkedHashMap<>();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");

        for (StudyTimeRecord record : records) {
            if (record.getDurationMinutes() == null) {
                continue;
            }
            String dateKey = sdf.format(record.getStartTime());
            dailyMap.merge(dateKey, record.getDurationMinutes(), Double::sum);
        }

        // 转换为 List<Map> 输出
        List<Map<String, Object>> result = new ArrayList<>();
        for (Map.Entry<String, Double> entry : dailyMap.entrySet()) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("date", entry.getKey());
            item.put("totalMinutes", Math.round(entry.getValue() * 10.0) / 10.0);
            result.add(item);
        }
        return result;
    }
}
