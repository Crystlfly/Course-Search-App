package com.undoschool.coursesearch.config;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import co.elastic.clients.elasticsearch.indices.*;
import co.elastic.clients.elasticsearch._types.mapping.*;

@Component
@RequiredArgsConstructor
public class ElasticIndexInitializer {

    private final ElasticsearchClient elasticsearchClient;

    @PostConstruct
public void createCourseIndexIfNotExists() {
    try {
        boolean exists = elasticsearchClient.indices().exists(e -> e.index("courses")).value();
        if (!exists) {
            elasticsearchClient.indices().create(c -> c
                .index("courses")
                .mappings(m -> m
                    .properties("id", p -> p.keyword(k -> k))
                    .properties("title", p -> p.text(t -> t))
                    .properties("description", p -> p.text(t -> t))
                    .properties("category", p -> p.text(t -> t)) 
                    .properties("type", p -> p.text(t -> t))     
                    .properties("minAge", p -> p.integer(i -> i))
                    .properties("maxAge", p -> p.integer(i -> i))
                    .properties("price", p -> p.float_(f -> f))
                    .properties("nextSessionDate", p -> p.date(d -> d))
                )
            );
            System.out.println("✅ Created 'courses' index with mapping");
        } else {
            System.out.println("ℹ️ Index 'courses' already exists");
        }
    } catch (Exception e) {
        System.err.println("❌ Failed to create 'courses' index: " + e.getMessage());
    }
}

}
