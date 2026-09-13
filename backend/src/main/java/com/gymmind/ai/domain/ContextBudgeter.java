package com.gymmind.ai.domain;
import java.util.*;
public final class ContextBudgeter {
 private static final int MAX_SEGMENTS=8, MAX_PER_DOCUMENT=2;
 private ContextBudgeter(){}
 public static List<ContextSegment> select(List<ContextSegment> input,int maxChars){
  if(input==null||maxChars<=0)return List.of(); Map<String,Integer> counts=new HashMap<>(); List<ContextSegment> out=new ArrayList<>(); int used=0;
  for(ContextSegment s:input){if(s==null||s.text()==null||s.documentId()==null||counts.getOrDefault(s.documentId(),0)>=MAX_PER_DOCUMENT)continue; int n=s.text().length(); if(used+n>maxChars)break; out.add(s); counts.merge(s.documentId(),1,Integer::sum); used+=n; if(out.size()>=MAX_SEGMENTS)break;}
  return List.copyOf(out);
 }
}
