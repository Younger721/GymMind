package com.gymmind.knowledge.application;
public interface KnowledgeObjectStore { void put(String key, byte[] content, String contentType); void delete(String key); }
