package com.mehmetserin.reconcile.web;

import com.mehmetserin.reconcile.service.ReconciliationService;
import com.mehmetserin.reconcile.service.ReconciliationService.ExpectedPayment;
import com.mehmetserin.reconcile.service.ReconciliationService.ReconcileReport;
import com.mehmetserin.reconcile.service.ReconciliationService.StatementLine;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/reconcile")
public class ReconcileController {

    public record ReconcileRequest(
            @NotNull List<ExpectedPayment> expected,
            @NotNull List<StatementLine> statement
    ) {}

    private final ReconciliationService service;

    public ReconcileController(ReconciliationService service) {
        this.service = service;
    }

    @PostMapping
    public ReconcileReport reconcile(@Valid @RequestBody ReconcileRequest request) {
        return service.reconcile(request.expected(), request.statement());
    }

    @GetMapping("/health")
    public Map<String, String> health() {
        return Map.of("status", "healthy", "service", "bank-statement-reconciler");
    }
}
