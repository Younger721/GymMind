package com.gymmind.knowledge.infrastructure.search;
import org.junit.jupiter.api.Test; import static org.assertj.core.api.Assertions.*;
class TenantIndexIsolationTest { @Test void keywordAndVectorSearchFilterTenant(){var k=new InMemoryKeywordIndex();var v=new InMemoryVectorIndex();var vector=new float[1024];vector[0]=1;var x=new IndexedChunk("c1",7L,"hello",vector);k.upsert(x);v.upsert(x);assertThat(k.search("hello",7L)).hasSize(1);assertThat(k.search("hello",8L)).isEmpty();assertThat(v.search(vector,8L)).isEmpty();}
 @Test void privateChunksAreVisibleOnlyToTheirOwner(){var k=new InMemoryKeywordIndex();var v=new InMemoryVectorIndex();var vector=new float[1024];vector[0]=1;var x=new IndexedChunk("private",7L,11L,"secret",vector);k.upsert(x);v.upsert(x);assertThat(k.search("secret",7L,11L)).hasSize(1);assertThat(k.search("secret",7L,12L)).isEmpty();assertThat(v.search(vector,7L,12L)).isEmpty();}
}
