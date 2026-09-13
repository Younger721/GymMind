package com.gymmind.membership.api;

import com.gymmind.membership.application.DefaultPaymentRecordService;
import com.gymmind.membership.application.PaymentRecordView;
import com.gymmind.membership.application.RecordOfflinePaymentCommand;
import com.gymmind.shared.api.ApiResponse;
import com.gymmind.shared.security.CurrentActorProvider;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/v1/payment-records")
@SecurityRequirement(name = "bearerAuth")
public class PaymentRecordController {
    private final DefaultPaymentRecordService service;
    private final CurrentActorProvider actors;

    public PaymentRecordController(DefaultPaymentRecordService service, CurrentActorProvider actors) {
        this.service = service;
        this.actors = actors;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<PaymentRecordView> record(@Valid @RequestBody RecordRequest request) {
        return ApiResponse.success(service.recordOfflinePayment(actors.requireCurrent(),
                new RecordOfflinePaymentCommand(null, request.orderId(), request.amount(), request.currency(),
                        request.reference(), request.paymentMethod())));
    }

    public record RecordRequest(Long orderId, BigDecimal amount, @NotBlank String currency,
                                @NotBlank String reference, @NotBlank String paymentMethod) {
    }
}
