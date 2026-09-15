package com.gymmind.knowledge.application;
public interface KnowledgeObjectStore { void put(String key, byte[] content, String contentType); default byte[] read(String key) { return null; } void delete(String key); }
