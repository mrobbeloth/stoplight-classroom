package com.stoplight.classroom.repository;

import com.stoplight.classroom.model.Course;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface CourseRepository extends JpaRepository<Course, Long> {
    List<Course> findByTeacherIdAndArchivedFalse(Long teacherId);
    List<Course> findByTeacherId(Long teacherId);
}
