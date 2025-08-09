package com.undoschool.coursesearch.repository;

import com.undoschool.coursesearch.model.Course;

import java.time.LocalDateTime;
import java.util.List;

public interface CourseCustomRepository {
    List<Course> searchWithFilters(String keyword, String category, String type,
                                   Integer userAge,
                                   Double minPrice, Double maxPrice,
                                   LocalDateTime nextSessionAfter, String sort);
}
