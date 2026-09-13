package com.gymmind.analytics.domain;
public interface MemberMetricsPort { long total(Long tenantId); long active(Long tenantId); }
