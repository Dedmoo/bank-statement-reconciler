package com.mehmetserin.reconcile.service;

import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
public class ReconciliationService {

    public record ExpectedPayment(String id, LocalDate date, BigDecimal amount, String reference) {}
    public record StatementLine(String id, LocalDate date, BigDecimal amount, String description) {}
    public record Match(String expectedId, String statementId, String reason) {}
    public record ReconcileReport(
            List<Match> matched,
            List<ExpectedPayment> unmatchedExpected,
            List<StatementLine> unmatchedStatement
    ) {}

    public ReconcileReport reconcile(List<ExpectedPayment> expected, List<StatementLine> statement) {
        if (expected == null || statement == null) {
            throw new IllegalArgumentException("Expected payments and statement lines are required.");
        }
        List<Match> matched = new ArrayList<>();
        Set<String> usedExpected = new HashSet<>();
        Set<String> usedStatement = new HashSet<>();

        for (ExpectedPayment exp : expected) {
            for (StatementLine line : statement) {
                if (usedExpected.contains(exp.id()) || usedStatement.contains(line.id())) {
                    continue;
                }
                if (exp.amount().compareTo(line.amount()) != 0) {
                    continue;
                }
                boolean sameDay = exp.date().equals(line.date());
                boolean nearDay = Math.abs(exp.date().toEpochDay() - line.date().toEpochDay()) <= 2;
                boolean refHit = containsRef(line.description(), exp.reference());
                if (sameDay && refHit) {
                    matched.add(new Match(exp.id(), line.id(), "amount+date+reference"));
                    usedExpected.add(exp.id());
                    usedStatement.add(line.id());
                } else if (nearDay && refHit) {
                    matched.add(new Match(exp.id(), line.id(), "amount+near-date+reference"));
                    usedExpected.add(exp.id());
                    usedStatement.add(line.id());
                } else if (sameDay) {
                    matched.add(new Match(exp.id(), line.id(), "amount+date"));
                    usedExpected.add(exp.id());
                    usedStatement.add(line.id());
                }
            }
        }

        List<ExpectedPayment> unmatchedExpected = expected.stream()
                .filter(e -> !usedExpected.contains(e.id())).toList();
        List<StatementLine> unmatchedStatement = statement.stream()
                .filter(s -> !usedStatement.contains(s.id())).toList();
        return new ReconcileReport(matched, unmatchedExpected, unmatchedStatement);
    }

    private static boolean containsRef(String description, String reference) {
        if (description == null || reference == null || reference.isBlank()) {
            return false;
        }
        return description.toLowerCase(Locale.ROOT).contains(reference.toLowerCase(Locale.ROOT).trim());
    }
}
