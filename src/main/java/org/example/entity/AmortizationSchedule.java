package org.example.entity;

import java.math.BigDecimal;
import java.util.List;

public class AmortizationSchedule {
    private List<AmortizationEntry> entries;

    public AmortizationSchedule(List<AmortizationEntry> entries) {
        this.entries = entries;
    }

    public List<AmortizationEntry> getEntries() {
        return entries;
    }

    public int getMonths() {
        return entries.size();
    }

    public AmortizationEntry getAmortizationEntry(int month) {
        return entries.get(month);
    }

    public BigDecimal getMonthlyPayment() {
        return entries.get(0).getPayment();
    }
}