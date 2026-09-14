package com.gymmind.knowledge.infrastructure.search;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;

class SearchBackendConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withBean(RestClient.Builder.class, RestClient::builder)
            .withUserConfiguration(ElasticsearchKeywordIndex.class, MilvusVectorIndex.class);

    @Test
    void loadsElasticsearchAndMilvusTogether() {
        contextRunner
                .withPropertyValues(
                        "gymmind.search.keyword-backend=elasticsearch",
                        "gymmind.search.vector-backend=milvus")
                .run(context -> {
                    assertThat(context).hasSingleBean(KeywordIndexPort.class);
                    assertThat(context).hasSingleBean(VectorIndexPort.class);
                });
    }
}
