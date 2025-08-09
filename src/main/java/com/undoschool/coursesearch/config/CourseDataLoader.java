package com.undoschool.coursesearch.config;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.undoschool.coursesearch.model.Course;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.io.InputStream;
import java.util.List;

@Component
public class CourseDataLoader {

    @Autowired
    private ElasticsearchClient elasticsearchClient;

    @Autowired
    private ObjectMapper objectMapper; // ✅ Inject Spring-managed ObjectMapper

    @PostConstruct
    public void loadData() {
        try {
            InputStream inputStream = new ClassPathResource("data/courses.json").getInputStream();

            List<Course> courses = objectMapper.readValue(
                inputStream,
                new TypeReference<List<Course>>() {}
            );

            for (Course course : courses) {
                elasticsearchClient.index(i -> i
                        .index("courses")
                        .id(course.getId())
                        .document(course)
                );
            }

            System.out.println("✅ Courses loaded into Elasticsearch");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
