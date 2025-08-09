package com.undoschool.coursesearch.service;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.query_dsl.*;
import co.elastic.clients.elasticsearch.core.*;
import co.elastic.clients.elasticsearch.core.search.Hit;
import com.undoschool.coursesearch.model.Course;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import co.elastic.clients.json.JsonData;
import co.elastic.clients.elasticsearch._types.FieldValue;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import co.elastic.clients.elasticsearch._types.SortOrder;



import java.io.IOException;
import java.util.*;

@Service
public class CourseService {

    private final ElasticsearchClient elasticsearchClient;
    private static final String INDEX_NAME = "courses"; // your index name

    @Autowired
    public CourseService(ElasticsearchClient elasticsearchClient) {
        this.elasticsearchClient = elasticsearchClient;
    }

    // Save a single course
    public Course saveCourse(Course course) {
        try {
            elasticsearchClient.index(i -> i
                .index(INDEX_NAME)
                .id(course.getId())
                .document(course)
            );
        } catch (IOException e) {
            throw new RuntimeException("Error saving course to Elasticsearch", e);
        }
        return course;
    }

    // Save multiple courses
    public List<Course> saveAllCourses(List<Course> courses) {
        try {
            for (Course course : courses) {
                elasticsearchClient.index(i -> i
                    .index(INDEX_NAME)
                    .id(course.getId())
                    .document(course)
                );
            }
        } catch (IOException e) {
            throw new RuntimeException("Error saving courses to Elasticsearch", e);
        }
        return courses;
    }

    // Get course by ID
    public Optional<Course> getCourseById(String id) {
        try {
            GetResponse<Course> response = elasticsearchClient.get(g -> g
                .index(INDEX_NAME)
                .id(id),
                Course.class
            );
            return response.found() ? Optional.of(response.source()) : Optional.empty();
        } catch (IOException e) {
            throw new RuntimeException("Error retrieving course", e);
        }
    }

    // Delete course by ID
    public void deleteCourseById(String id) {
        try {
            elasticsearchClient.delete(d -> d
                .index(INDEX_NAME)
                .id(id)
            );
        } catch (IOException e) {
            throw new RuntimeException("Error deleting course", e);
        }
    }

    // Get all courses
    public List<Course> getAllCourses() {
        try {
            SearchResponse<Course> response = elasticsearchClient.search(s -> s
                .index(INDEX_NAME)
                .query(q -> q.matchAll(m -> m)),
                Course.class
            );
            List<Course> courses = new ArrayList<>();
            for (Hit<Course> hit : response.hits().hits()) {
                courses.add(hit.source());
            }
            return courses;
        } catch (IOException e) {
            throw new RuntimeException("Error fetching all courses", e);
        }
    }

    // Search by title keyword
    public List<Course> searchByKeyword(String keyword) {
        try {
            SearchResponse<Course> response = elasticsearchClient.search(s -> s
                .index(INDEX_NAME)
                .query(q -> q.match(m -> m
                    .field("title")
                    .query(keyword)
                )),
                Course.class
            );

            List<Course> courses = new ArrayList<>();
            for (Hit<Course> hit : response.hits().hits()) {
                courses.add(hit.source());
            }
            return courses;
        } catch (IOException e) {
            throw new RuntimeException("Error searching courses", e);
        }
    }

    // Advanced search with filters
public List<Course> searchWithFilters(
        String keyword,
        String category,
        String type,
        Integer userAge,
        Double minPrice,
        Double maxPrice,
        LocalDateTime nextSessionAfter,
        Integer page,
        Integer size,
        String sort
) {
    try {
        List<Query> filters = new ArrayList<>();

        // 🔍 Full-text search on title and description (fuzzy match)
        if (keyword != null && !keyword.isEmpty()) {
            filters.add(Query.of(q -> q.bool(b -> b.should(s -> s
                    .match(m -> m
                        .field("title")
                        .query(keyword)
                        .fuzziness("AUTO")
                    )
                ).should(s -> s
                    .match(m -> m
                        .field("description")
                        .query(keyword)
                        .fuzziness("AUTO")
                    )
                )
            )));
        }

        // Category match
        if (category != null) {
            filters.add(Query.of(q -> q.match(m -> m
                .field("category")
                .query(FieldValue.of(category))
            )));
        }

        // Type match
        if (type != null) {
            filters.add(Query.of(q -> q.match(m -> m
                .field("type")
                .query(FieldValue.of(type))
            )));
        }

        // Age-based eligibility
        if (userAge != null) {
            filters.add(Query.of(q -> q.bool(b -> b
                .must(m1 -> m1.range(r -> r
                    .field("minAge")
                    .lte(JsonData.of(userAge))
                ))
                .must(m2 -> m2.range(r -> r
                    .field("maxAge")
                    .gte(JsonData.of(userAge))
                ))
            )));
        }

        // Price range filter
        if (minPrice != null || maxPrice != null) {
            filters.add(Query.of(q -> q.range(r -> r
                .field("price")
                .gte(minPrice != null ? JsonData.of(minPrice) : null)
                .lte(maxPrice != null ? JsonData.of(maxPrice) : null)
            )));
        }

        // Filter by next session date
        if (nextSessionAfter != null) {
            filters.add(Query.of(q -> q.range(r -> r
                .field("nextSessionDate")
                .gt(JsonData.of(nextSessionAfter.atOffset(ZoneOffset.UTC).toString()))
            )));
        }

        // 🔀 Combine all filters
        Query finalQuery = Query.of(q -> q.bool(b -> b.filter(filters)));

        // 📄 Pagination
        int from = (page != null && page > 0 ? page - 1 : 0) * (size != null ? size : 10);
        int pageSize = size != null ? size : 10;

        // ↕️ Sorting
        String sortField;
        SortOrder sortOrder;

        if ("priceAsc".equalsIgnoreCase(sort)) {
            sortField = "price";
            sortOrder = SortOrder.Asc;
        } else if ("priceDesc".equalsIgnoreCase(sort)) {
            sortField = "price";
            sortOrder = SortOrder.Desc;
        } else {
            sortField = "nextSessionDate";
            sortOrder = SortOrder.Asc;
        }

        // 🧠 Perform search
        SearchResponse<Course> response = elasticsearchClient.search(s -> s
            .index(INDEX_NAME)
            .from(from)
            .size(pageSize)
            .query(finalQuery)
            .sort(sortBuilder -> sortBuilder
                .field(f -> f
                    .field(sortField)
                    .order(sortOrder)
                )
            ),
            Course.class
        );

        List<Course> result = new ArrayList<>();
        for (Hit<Course> hit : response.hits().hits()) {
            result.add(hit.source());
        }

        return result;

    } catch (IOException e) {
        throw new RuntimeException("Error performing filtered search", e);
    }
}

}
