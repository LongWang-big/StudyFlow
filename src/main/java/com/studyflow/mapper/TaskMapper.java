package com.studyflow.mapper;

import com.studyflow.entity.Task;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface TaskMapper {

    int insert(Task task);

    int updateById(Task task);

    int deleteById(@Param("id") Long id);

    Task selectById(@Param("id") Long id);

    List<Task> selectAll();

    List<Task> selectByStatus(@Param("status") String status);

    List<Task> selectByPriority(@Param("priority") String priority);

    List<Task> selectByStatusAndPriority(@Param("status") String status, @Param("priority") String priority);

    int countAll();

    int countByStatus(@Param("status") String status);
}
