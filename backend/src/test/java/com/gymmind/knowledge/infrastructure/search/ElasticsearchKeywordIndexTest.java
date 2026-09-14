package com.gymmind.knowledge.infrastructure.search;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
class ElasticsearchKeywordIndexTest {
 @Test void indexedDocumentCarriesPrivateOwnerScope(){var body=ElasticsearchKeywordIndex.documentBody(new IndexedChunk("c1",7L,11L,"secret",null));assertThat(body).containsEntry("tenantId",7L).containsEntry("ownerUserId",11L).containsEntry("text","secret");}
}
