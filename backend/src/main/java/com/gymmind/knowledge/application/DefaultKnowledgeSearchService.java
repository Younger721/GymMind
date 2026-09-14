package com.gymmind.knowledge.application;
import com.gymmind.ai.domain.*; import com.gymmind.knowledge.infrastructure.search.*; import com.gymmind.shared.error.*; import com.gymmind.shared.security.CurrentActor; import java.util.*; import org.springframework.stereotype.Service;
@Service public class DefaultKnowledgeSearchService implements KnowledgeSearchService {
 private final KeywordIndexPort keyword; private final VectorIndexPort vector; private final EmbeddingGateway embedding;
 public DefaultKnowledgeSearchService(KeywordIndexPort k,VectorIndexPort v,EmbeddingGateway e){keyword=k;vector=v;embedding=e;}
 public List<ContextSegment> search(CurrentActor a,String q){
  if(a==null||a.tenantId()==null||a.isPlatformAdmin()||!a.hasPermission("knowledge:read"))throw new BusinessException(ErrorCode.FORBIDDEN);
  if(q==null||q.isBlank())throw new BusinessException(ErrorCode.VALIDATION_FAILED);
  List<IndexedChunk> ks=keyword.search(q,a.tenantId(),a.userId()); List<IndexedChunk> found;
  try{found=vector.search(embedding.embed(List.of(q)).get(0),a.tenantId(),a.userId());}catch(RuntimeException ex){found=List.of();}
  final List<IndexedChunk> vs=found; List<RankedChunk> kr=ks.stream().map(c->new RankedChunk(c.id(),ks.indexOf(c)+1)).toList(),vr=vs.stream().map(c->new RankedChunk(c.id(),vs.indexOf(c)+1)).toList(); Map<String,IndexedChunk> all=new HashMap<>(); ks.forEach(c->all.put(c.id(),c)); vs.forEach(c->all.put(c.id(),c));
  return ContextBudgeter.select(RrfFusion.merge(kr,vr,20).stream().map(r->all.get(r.id())).filter(Objects::nonNull).map(c->new ContextSegment(c.id(),c.id(),c.text())).toList(),12000);
 }
}
