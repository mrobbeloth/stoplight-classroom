package com.stoplight.classroom.service;

import com.stoplight.classroom.dto.CourseResponse;
import com.stoplight.classroom.dto.CreateCourseRequest;
import com.stoplight.classroom.exception.ResourceNotFoundException;
import com.stoplight.classroom.model.Course;
import com.stoplight.classroom.model.User;
import com.stoplight.classroom.repository.CourseRepository;
import com.stoplight.classroom.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CourseService {

    private final CourseRepository courseRepository;
    private final UserRepository userRepository;

    public CourseService(CourseRepository courseRepository, UserRepository userRepository) {
        this.courseRepository = courseRepository;
        this.userRepository = userRepository;
    }

    public CourseResponse create(String teacherUsername, CreateCourseRequest request) {
        User teacher = findTeacher(teacherUsername);
        Course course = new Course(request.name(), request.term(), teacher);
        return CourseResponse.from(courseRepository.save(course));
    }

    public List<CourseResponse> listForTeacher(String teacherUsername) {
        User teacher = findTeacher(teacherUsername);
        return courseRepository.findByTeacherId(teacher.getId()).stream()
                .map(CourseResponse::from).toList();
    }

    public CourseResponse get(String teacherUsername, Long courseId) {
        return CourseResponse.from(findOwned(teacherUsername, courseId));
    }

    public CourseResponse update(String teacherUsername, Long courseId, CreateCourseRequest request) {
        Course course = findOwned(teacherUsername, courseId);
        course.setName(request.name());
        course.setTerm(request.term());
        return CourseResponse.from(courseRepository.save(course));
    }

    public void archive(String teacherUsername, Long courseId) {
        Course course = findOwned(teacherUsername, courseId);
        course.setArchived(true);
        courseRepository.save(course);
    }

    Course findOwned(String teacherUsername, Long courseId) {
        User teacher = findTeacher(teacherUsername);
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ResourceNotFoundException("Course not found"));
        if (!course.getTeacher().getId().equals(teacher.getId())) {
            throw new ResourceNotFoundException("Course not found");
        }
        return course;
    }

    private User findTeacher(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }
}
