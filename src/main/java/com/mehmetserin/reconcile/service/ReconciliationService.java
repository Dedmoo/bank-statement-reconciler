package com.mehmetserin.reconcile.service;

import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
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

    private record Candidate(String expectedId, String statementId, String reason, int score) {}

    public ReconcileReport reconcile(List<ExpectedPayment> expected, List<StatementLine> statement) {
        if (expected == null || statement == null) {
            throw new IllegalArgumentException("Expected payments and statement lines are required.");
        }

        List<Candidate> candidates = new ArrayList<>();
        for (ExpectedPayment exp : expected) {
            if (exp.id() == null || exp.date() == null || exp.amount() == null) {
                throw new IllegalArgumentException("Expected payment id, date and amount are required.");
            }
            for (StatementLine line : statement) {
                if (line.id() == null || line.date() == null || line.amount() == null) {
                    throw new IllegalArgumentException("Statement line id, date and amount are required.");
                }
                if (exp.amount().subtract(line.amount()).abs().compareTo(new BigDecimal("0.01")) > 0) {
                    continue;
                }
                boolean sameDay = exp.date().equals(line.date());
                boolean nearDay = Math.abs(exp.date().toEpochDay() - line.date().toEpochDay()) <= 2;
                boolean refHit = containsRef(line.description(), exp.reference());
                if (sameDay && refHit) {
                    candidates.add(new Candidate(exp.id(), line.id(), "amount+date+reference", 300));
                } else if (nearDay && refHit) {
                    candidates.add(new Candidate(exp.id(), line.id(), "amount+near-date+reference", 200));
                } else if (sameDay) {
                    candidates.add(new Candidate(exp.id(), line.id(), "amount+date", 100));
                }
            }
        }

        candidates.sort(Comparator.comparingInt(Candidate::score).reversed()
                .thenComparing(Candidate::expectedId)
                .thenComparing(Candidate::statementId));

        List<Match> matched = new ArrayList<>();
        Set<String> usedExpected = new HashSet<>();
        Set<String> usedStatement = new HashSet<>();
        for (Candidate c : candidates) {
            if (usedExpected.contains(c.expectedId()) || usedStatement.contains(c.statementId())) {
                continue;
            }
            matched.add(new Match(c.expectedId(), c.statementId(), c.reason()));
            usedExpected.add(c.expectedId());
            usedStatement.add(c.statementId());
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
