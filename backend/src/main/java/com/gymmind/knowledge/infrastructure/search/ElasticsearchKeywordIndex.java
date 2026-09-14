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
    public ElasticsearchKeywordIndex(RestClient.Builder b){client=b.baseUrl(Optional.ofNullable(System.getenv("GYMMIND_ES_URL")).orElse("http://localhost:9200")).build();index=Optional.ofNullable(System.getenv("GYMMIND_ES_INDEX")).orElse("gymmind-knowledge");}
    public static Map<String,Object> indexBody(){return Map.of("settings",Map.of("analysis",Map.of("analyzer",Map.of("gymmind_smartcn",Map.of("type","custom","tokenizer","smartcn_tokenizer")))),"mappings",Map.of("properties",Map.of("tenantId",Map.of("type","long"),"ownerUserId",Map.of("type","long"),"text",Map.of("type","text","analyzer","gymmind_smartcn"))));}
    public static Map<String,Object> documentBody(IndexedChunk c){var m=new HashMap<String,Object>();m.put("tenantId",c.tenantId());m.put("text",c.text());if(c.ownerUserId()!=null)m.put("ownerUserId",c.ownerUserId());return m;}
    private void ensureIndex(){if(indexReady)return;synchronized(this){if(indexReady)return;boolean exists=client.head().uri("/"+index).exchange((request,response)->response.getStatusCode().is2xxSuccessful());if(!exists)client.put().uri("/"+index).body(indexBody()).retrieve().toBodilessEntity();indexReady=true;}}
    public void upsert(IndexedChunk c){ensureIndex();client.put().uri("/"+index+"/_doc/"+c.id()).body(documentBody(c)).retrieve().toBodilessEntity();}
    public List<IndexedChunk> search(String q,Long t){return search(q,t,null);} public List<IndexedChunk> search(String q,Long t,Long u){ensureIndex();var filters=new ArrayList<Object>();filters.add(Map.of("term",Map.of("tenantId",t)));if(u!=null)filters.add(Map.of("bool",Map.of("should",List.of(Map.of("bool",Map.of("must_not",Map.of("exists",Map.of("field","ownerUserId")))),Map.of("term",Map.of("ownerUserId",u))),"minimum_should_match",1)));Map<String,Object> body=Map.of("query",Map.of("bool",Map.of("must",List.of(Map.of("match",Map.of("text",q))),"filter",filters)),"size",30);String raw=client.post().uri("/"+index+"/_search").body(body).retrieve().body(String.class);if(raw==null)return List.of();try{JsonNode hits=mapper.readTree(raw).path("hits").path("hits");List<IndexedChunk> out=new ArrayList<>();for(JsonNode h:hits){JsonNode s=h.path("_source");JsonNode owner=s.get("ownerUserId");out.add(new IndexedChunk(h.path("_id").asText(),s.path("tenantId").asLong(),owner==null||owner.isNull()?null:owner.asLong(),s.path("text").asText(),null));}return out;}catch(Exception e){return List.of();}}
    public void delete(String id,Long t){ensureIndex();client.delete().uri("/"+index+"/_doc/"+id).retrieve().toBodilessEntity();}
}
