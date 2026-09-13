package com.gymmind.knowledge.infrastructure.search;
import org.junit.jupiter.api.Test; import static org.assertj.core.api.Assertions.*;
class TenantIndexIsolationTest { @Test void keywordAndVectorSearchFilterTenant(){var k=new InMemoryKeywordIndex();var v=new InMemoryVectorIndex();var vector=new float[1024];vector[0]=1;var x=new IndexedChunk("c1",7L,"hello",vector);k.upsert(x);v.upsert(x);assertThat(k.search("hello",7L)).hasSize(1);assertThat(k.search("hello",8L)).isEmpty();assertThat(v.search(vector,8L)).isEmpty();} }
