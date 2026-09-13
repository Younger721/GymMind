package com.gymmind.ai.domain; import java.util.List; public interface EmbeddingGateway { List<float[]> embed(List<String> texts); }
