package com.mehmetserin.reconcile;

import com.mehmetserin.reconcile.service.ReconciliationService;
import com.mehmetserin.reconcile.service.ReconciliationService.ExpectedPayment;
import com.mehmetserin.reconcile.service.ReconciliationService.StatementLine;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReconciliationServiceTest {

    private final ReconciliationService service = new ReconciliationService();

    @Test
    void matchesOnAmountDateAndReference() {
        var report = service.reconcile(
                List.of(new ExpectedPayment("E1", LocalDate.of(2026, 6, 1), new BigDecimal("250.00"), "INV-42")),
                List.of(new StatementLine("S1", LocalDate.of(2026, 6, 1), new BigDecimal("250.00"), "Payment INV-42 Alice"))
        );
        assertEquals(1, report.matched().size());
        assertEquals("amount+date+reference", report.matched().get(0).reason());
        assertTrue(report.unmatchedExpected().isEmpty());
        assertTrue(report.unmatchedStatement().isEmpty());
    }

    @Test
    void leavesUnmatchedWhenNoPair() {
        var report = service.reconcile(
                List.of(new ExpectedPayment("E1", LocalDate.of(2026, 6, 1), new BigDecimal("100.00"), "A")),
                List.of(new StatementLine("S1", LocalDate.of(2026, 6, 1), new BigDecimal("200.00"), "B"))
        );
        assertTrue(report.matched().isEmpty());
        assertEquals(1, report.unmatchedExpected().size());
        assertEquals(1, report.unmatchedStatement().size());
    }

    @Test
    void prefersReferenceMatchOverWeakSameDay() {
        var report = service.reconcile(
                List.of(new ExpectedPayment("E1", LocalDate.of(2026, 6, 1), new BigDecimal("100.00"), "INV-9")),
                List.of(
                        new StatementLine("S1", LocalDate.of(2026, 6, 1), new BigDecimal("100.00"), "misc transfer"),
                        new StatementLine("S2", LocalDate.of(2026, 6, 1), new BigDecimal("100.00"), "Payment INV-9")
                )
        );
        assertEquals(1, report.matched().size());
        assertEquals("S2", report.matched().get(0).statementId());
        assertEquals("amount+date+reference", report.matched().get(0).reason());
    }
}
