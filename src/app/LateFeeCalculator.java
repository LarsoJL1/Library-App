package app;

import model.Loan;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * THE JAVA ALGORITHM (not SQL): totals late fees per member, then ranks members
 * from highest fee to lowest using a hand-written insertion sort.
 */
public class LateFeeCalculator {
    public static final double DAILY_RATE = 0.25;   // dollars per day late

    /** One row of the result: a member and what they owe. */
    public static class Fee {
        private final int memberId;
        private final String memberName;
        private int totalDaysLate;
        private double amount;

        public Fee(int memberId, String memberName) {
            this.memberId = memberId;
            this.memberName = memberName;
        }

        public int getMemberId() { return memberId; }
        public String getMemberName() { return memberName; }
        public int getTotalDaysLate() { return totalDaysLate; }
        public double getAmount() { return amount; }

        void add(long days) {
            totalDaysLate += (int) days;
            amount += days * DAILY_RATE;
        }
    }

    /** Whole days between the due date and today (never negative). */
    public static long daysLate(Loan loan, LocalDate today) {
        return Math.max(0, ChronoUnit.DAYS.between(loan.getDueDate(), today));
    }

    /** Total fees per member, sorted from highest to lowest. */
    public static List<Fee> calculate(List<Loan> overdueLoans, LocalDate today) {
        // Step 1: add up each member's late days
        Map<Integer, Fee> byMember = new LinkedHashMap<>();
        for (Loan loan : overdueLoans) {
            Fee fee = byMember.get(loan.getMemberId());
            if (fee == null) {
                fee = new Fee(loan.getMemberId(), loan.getMemberName());
                byMember.put(loan.getMemberId(), fee);
            }
            fee.add(daysLate(loan, today));
        }

        // Step 2: sort with our own algorithm instead of Collections.sort
        List<Fee> fees = new ArrayList<>(byMember.values());
        insertionSortDescending(fees);
        return fees;
    }

    /** Insertion sort: highest amount first; ties broken alphabetically by name. */
    private static void insertionSortDescending(List<Fee> fees) {
        for (int i = 1; i < fees.size(); i++) {
            Fee current = fees.get(i);
            int j = i - 1;
            while (j >= 0 && comesAfter(fees.get(j), current)) {
                fees.set(j + 1, fees.get(j));
                j--;
            }
            fees.set(j + 1, current);
        }
    }

    /** True if 'a' should be placed after 'b' in the final order. */
    private static boolean comesAfter(Fee a, Fee b) {
        if (a.getAmount() != b.getAmount()) {
            return a.getAmount() < b.getAmount();
        }
        return a.getMemberName().compareTo(b.getMemberName()) > 0;
    }
}
