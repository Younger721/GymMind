package com.gymmind.search.application;

public record ArticleView(long id, long tenantId, String url, String title, String summary) { }
