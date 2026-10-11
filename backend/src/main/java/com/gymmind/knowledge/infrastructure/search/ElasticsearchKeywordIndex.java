package com.gymmind.knowledge.infrastructure.search;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import java.util.*;

@Component
@ConditionalOnProperty(name="gymmind.search.keyword-backend", havingValue="elasticsearch")
public class ElasticsearchKeywordIndex implements KeywordIndexPort {
    private final RestClient client; private final String index; private final ObjectMapper mapper=new ObjectMapper(); private volatile boolean indexReady;
    public ElasticsearchKeywordIndex(RestClient.Builder b){client=b.baseUrl(Optional.ofNullable(System.getenv("GYMMIND_ES_URL")).orElse("http://localhost:9202")).build();index=Optional.ofNullable(System.getenv("GYMMIND_ES_INDEX")).orElse("gymmind-knowledge");}
    public static Map<String,Object> indexBody(){return Map.of("settings",Map.of("analysis",Map.of("analyzer",Map.of("gymmind_smartcn",Map.of("type","custom","tokenizer","smartcn_tokenizer")))),"mappings",Map.of("properties",Map.of("tenantId",Map.of("type","long"),"documentId",Map.of("type","long"),"ownerUserId",Map.of("type","long"),"knowledgeKey",Map.of("type","keyword"),"knowledgeVersion",Map.of("type","integer"),"indexGeneration",Map.of("type","keyword"),"visibility",Map.of("type","keyword"),"audienceRoles",Map.of("type","keyword"),"text",Map.of("type","text","analyzer","gymmind_smartcn"))));}
    public static Map<String,Object> documentBody(IndexedChunk c){var m=new HashMap<String,Object>();m.put("tenantId",c.tenantId());m.put("text",c.text());m.put("knowledgeVersion",c.knowledgeVersion());if(c.documentId()!=null)m.put("documentId",c.documentId());if(c.ownerUserId()!=null)m.put("ownerUserId",c.ownerUserId());if(c.knowledgeKey()!=null)m.put("knowledgeKey",c.knowledgeKey());if(c.indexGeneration()!=null)m.put("indexGeneration",c.indexGeneration());if(c.visibility()!=null)m.put("visibility",c.visibility());if(c.audienceRoles()!=null&&!c.audienceRoles().isEmpty())m.put("audienceRoles",c.audienceRoles());return m;}
    private void ensureIndex(){if(indexReady)return;synchronized(this){if(indexReady)return;boolean exists=client.head().uri("/"+index).exchange((request,response)->response.getStatusCode().is2xxSuccessful());if(!exists)client.put().uri("/"+index).body(indexBody()).retrieve().toBodilessEntity();indexReady=true;}}
    public void upsert(IndexedChunk c){ensureIndex();client.put().uri("/"+index+"/_doc/"+c.id()).body(documentBody(c)).retrieve().toBodilessEntity();}
    public List<IndexedChunk> search(String q,Long t){return search(q,t,(Long)null);} public List<IndexedChunk> search(String q,Long t,Long u){return search(q,t,new KnowledgeRecallFilter(u,java.util.Set.of(),false));} @Override public List<IndexedChunk> search(String q,Long t,KnowledgeRecallFilter filter){ensureIndex();var filters=new ArrayList<Object>();filters.add(Map.of("term",Map.of("tenantId",t)));Long u=filter==null?null:filter.userId();if(u!=null)filters.add(Map.of("bool",Map.of("should",List.of(Map.of("bool",Map.of("must_not",Map.of("exists",Map.of("field","ownerUserId")))),Map.of("term",Map.of("ownerUserId",u))),"minimum_should_match",1)));if(filter!=null&&!filter.skipAudienceFilter()){var roleNames=KnowledgeAudienceFilters.actorRoleNames(filter.actorRoles());if(!roleNames.isEmpty())filters.add(Map.of("bool",Map.of("should",List.of(Map.of("terms",Map.of("audienceRoles",roleNames)),Map.of("bool",Map.of("must_not",Map.of("exists",Map.of("field","audienceRoles"))))),"minimum_should_match",1)));}Map<String,Object> body=Map.of("query",Map.of("bool",Map.of("must",List.of(Map.of("match",Map.of("text",q))),"filter",filters)),"size",30);return parseSearch(client.post().uri("/"+index+"/_search").body(body).retrieve().body(String.class));} private List<IndexedChunk> parseSearch(String raw){if(raw==null)return List.of();try{JsonNode hits=mapper.readTree(raw).path("hits").path("hits");List<IndexedChunk> out=new ArrayList<>();for(JsonNode h:hits){JsonNode s=h.path("_source");JsonNode owner=s.get("ownerUserId");JsonNode document=s.get("documentId");String generation=s.path("indexGeneration").asText(null);List<String> roles=readAudienceRoles(s.path("audienceRoles"));out.add(new IndexedChunk(h.path("_id").asText(),s.path("tenantId").asLong(),document==null||document.isNull()?null:document.asLong(),owner==null||owner.isNull()?null:owner.asLong(),s.path("text").asText(),null,s.path("knowledgeKey").asText(null),s.path("knowledgeVersion").asInt(1),generation,s.path("visibility").asText(null),roles));}return out;}catch(Exception e){return List.of();}} private static List<String> readAudienceRoles(JsonNode node){if(node==null||node.isMissingNode()||node.isNull())return List.of();if(node.isArray()){List<String> roles=new ArrayList<>();node.forEach(item->roles.add(item.asText()));return roles;}return List.of(node.asText());}
    public void delete(String id,Long t){ensureIndex();client.delete().uri("/"+index+"/_doc/"+id).retrieve().toBodilessEntity();}
    public void deleteDocument(Long tenantId, Long documentId) {
        ensureIndex();
        var filters = List.of(Map.of("term", Map.of("tenantId", tenantId)),
                Map.of("term", Map.of("documentId", documentId)));
        var query = Map.of("query", Map.of("bool", Map.of("filter", filters)));
        client.post().uri("/" + index + "/_delete_by_query?refresh=true")
                .body(query).retrieve().toBodilessEntity();
    }
    @Override
    public void deleteStaleIndexGenerations(Long tenantId, Long documentId, String keepGeneration) {
        ensureIndex();
        var filters = List.of(
                Map.of("term", Map.of("tenantId", tenantId)),
                Map.of("term", Map.of("documentId", documentId)));
        var mustNot = List.of(Map.of("term", Map.of("indexGeneration", keepGeneration)));
        var query = Map.of("query", Map.of("bool", Map.of("filter", filters, "must_not", mustNot)));
        client.post().uri("/" + index + "/_delete_by_query?refresh=true")
                .body(query).retrieve().toBodilessEntity();
    }
}
