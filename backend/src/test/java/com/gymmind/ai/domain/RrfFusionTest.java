package com.gymmind.ai.domain;
import org.junit.jupiter.api.Test; import java.util.*; import static org.assertj.core.api.Assertions.*;
class RrfFusionTest { @Test void mergesAndDeduplicatesWithStableOrdering(){var a=List.of(new RankedChunk("a",1),new RankedChunk("b",2));var b=List.of(new RankedChunk("b",1),new RankedChunk("c",2));var out=RrfFusion.merge(a,b,20);assertThat(out).extracting(RankedChunk::id).containsExactly("b","a","c");} @Test void oneRouteCanBeEmpty(){assertThat(RrfFusion.merge(List.of(new RankedChunk("a",1)),List.of(),20)).hasSize(1);} }
