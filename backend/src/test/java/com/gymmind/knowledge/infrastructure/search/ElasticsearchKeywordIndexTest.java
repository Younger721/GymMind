package com.gymmind.knowledge.infrastructure.search;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
class ElasticsearchKeywordIndexTest {
 @Test void indexedDocumentCarriesPrivateOwnerScope(){var body=ElasticsearchKeywordIndex.documentBody(new IndexedChunk("c1",7L,11L,"secret",null));assertThat(body).containsEntry("tenantId",7L).containsEntry("ownerUserId",11L).containsEntry("text","secret");}

 @Test void indexMappingUsesSmartcnAndTenantIsolationFields(){var body=ElasticsearchKeywordIndex.indexBody();assertThat(body.get("mappings").toString()).contains("tenantId","ownerUserId","text","smartcn");}

 @Test void createsMissingIndexBeforeFirstDocument(){var builder=RestClient.builder();var server=MockRestServiceServer.bindTo(builder).build();var index=new ElasticsearchKeywordIndex(builder);server.expect(requestTo("http://localhost:9202/gymmind-knowledge")).andRespond(withStatus(HttpStatus.NOT_FOUND));server.expect(requestTo("http://localhost:9202/gymmind-knowledge")).andRespond(withSuccess());server.expect(requestTo("http://localhost:9202/gymmind-knowledge/_doc/c1")).andRespond(withSuccess());index.upsert(new IndexedChunk("c1",7L,"text",new float[1024]));server.verify();}
}
