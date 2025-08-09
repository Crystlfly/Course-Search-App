package com.undoschool.coursesearch.repository;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import co.elastic.clients.elasticsearch._types.query_dsl.*;
import co.elastic.clients.json.JsonData;
import com.undoschool.coursesearch.model.Course;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import co.elastic.clients.elasticsearch._types.SortOrder;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.time.LocalDateTime;

@Repository
public class CourseCustomRepositoryImpl implements CourseCustomRepository {

    @Autowired
    private ElasticsearchClient elasticsearchClient;

    @Override
public List<Course> searchWithFilters(String keyword, String category, String type,
                                      Integer userAge,
                                      Double minPrice, Double maxPrice,
                                      LocalDateTime nextSessionAfter, String sort) {

    List<Query> filters = new ArrayList<>();

    // ✅ Multi-match: title + description
    if (keyword != null && !keyword.isEmpty()) {
        filters.add(Query.of(q -> q.multiMatch(m -> m
            .fields("title", "description")
            .query(keyword)
            .fuzziness("AUTO")
        )));
    }

    // ✅ Match instead of term for analyzed field
    if (category != null && !category.isEmpty()) {
        filters.add(Query.of(q -> q.match(m -> m.field("category").query(category).fuzziness("2").operator(Operator.And))));
    }

    if (type != null && !type.isEmpty()) {
        filters.add(Query.of(q -> q.match(m -> m.field("type").query(type).fuzziness("2").operator(Operator.And))));
    }

    // ✅ Age logic: overlap of query range and course range
if (userAge != null) {
    filters.add(Query.of(q -> q.range(r -> r
        .field("minAge")
        .lte(JsonData.of(userAge))
    )));
    filters.add(Query.of(q -> q.range(r -> r
        .field("maxAge")
        .gte(JsonData.of(userAge))
    )));
}



    // ✅ Price filter
    if (minPrice != null || maxPrice != null) {
        RangeQuery.Builder priceRange = new RangeQuery.Builder().field("price");

        if (minPrice != null) priceRange.gte(JsonData.of(minPrice));
        if (maxPrice != null) priceRange.lte(JsonData.of(maxPrice));

        filters.add(Query.of(q -> q.range(priceRange.build())));
    }

    // ✅ Next session date filter
   if (nextSessionAfter != null) {
    String isoDateString = nextSessionAfter.toString();  // e.g. "2025-08-12T10:00:00"
    filters.add(Query.of(q -> q
        .range(r -> r
            .field("nextSessionDate")
            .gt(JsonData.of(isoDateString))  // match ES date field format
        )
    ));  
}


    // Combine all filters
    BoolQuery boolQuery = BoolQuery.of(b -> b.must(filters));

    try {
        SearchResponse<Course> response = elasticsearchClient.search(s -> s
        .index("courses")
        .sort(sortBuilder -> {
            if ("priceAsc".equalsIgnoreCase(sort)) {
                return sortBuilder.field(f -> f.field("price").order(SortOrder.Asc));
            } else if ("priceDesc".equalsIgnoreCase(sort)) {
                return sortBuilder.field(f -> f.field("price").order(SortOrder.Desc));
            } else {
                // Default sort by nextSessionDate ascending (put nulls last)
                return sortBuilder.field(f -> f
                    .field("nextSessionDate")
                    .order(SortOrder.Asc)
                    .missing("_last")
                );
            }
        })



        .query(q -> q.bool(boolQuery)),
        Course.class);


        List<Course> results = new ArrayList<>();
        for (Hit<Course> hit : response.hits().hits()) {
            results.add(hit.source());
        }

        return results;

    } catch (IOException e) {
        e.printStackTrace();
        return new ArrayList<>();
    }
}

}
