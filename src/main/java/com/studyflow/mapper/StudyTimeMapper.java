package com.studyflow.mapper;

import com.studyflow.entity.StudyTimeRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Date;
import java.util.List;

@Mapper
public interface StudyTimeMapper {

    int insert(StudyTimeRecord record);

    int updateEndTime(@Param("id") Long id,
                      @Param("endTime") Date endTime,
                      @Param("durationMinutes") Double durationMinutes);

    StudyTimeRecord selectById(@Param("id") Long id);

    List<StudyTimeRecord> selectByTaskId(@Param("taskId") Long taskId);

    List<StudyTimeRecord> selectRecentByTaskId(@Param("taskId") Long taskId,
                                               @Param("days") int days);

    Double sumDurationByTaskId(@Param("taskId") Long taskId);
}
