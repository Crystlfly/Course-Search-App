package com.undoschool.coursesearch.service;

import com.undoschool.coursesearch.model.Course;
import java.util.List;

public interface CourseSearchService {
    List<Course> searchCourses(String keyword);
}
