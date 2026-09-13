package com.gymmind.membership.application; import java.math.BigDecimal; public record CreateOrderCommand(Long tenantId,Long memberId,BigDecimal total,String currency){}
