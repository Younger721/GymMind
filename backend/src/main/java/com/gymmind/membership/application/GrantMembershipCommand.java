package com.gymmind.membership.application; import java.time.Instant; public record GrantMembershipCommand(Long tenantId,Long memberId,Long packageId,Instant startsAt,Instant expiresAt){}
