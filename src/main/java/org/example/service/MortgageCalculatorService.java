package org.example.service;

import org.example.entity.AmortizationEntry;
import org.example.entity.AmortizationSchedule;
import org.example.entity.ExtraPayment;
import org.example.entity.InputParam;

import java.io.FileWriter;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * @Desc: MortgageCalculatorService Class
 * @Author: Lauren LUO
 * @Date: 2024/04/03
 */
public class MortgageCalculatorService {

    private static final int MONTHS_IN_YEAR = 12;

    public static AmortizationSchedule calculateAmortizationSchedule(InputParam param) {
        int termInMonths = param.getTermInYears() * MONTHS_IN_YEAR;
        BigDecimal monthlyInterestRate = param.getAnnualInterestRate().divide(BigDecimal.valueOf(100 * MONTHS_IN_YEAR), 10,
                RoundingMode.HALF_UP);

        BigDecimal principal = param.getLoanAmount().subtract(param.getDownPayment());
        BigDecimal monthlyPayment = calculateMonthlyPayment(principal, monthlyInterestRate, termInMonths);

        List<AmortizationEntry> entries = new ArrayList<>();
        BigDecimal remainingBalance = principal;

        for (int i = 0; i < termInMonths; i++) {
            BigDecimal interest = remainingBalance.multiply(monthlyInterestRate);
            BigDecimal principalPayment = monthlyPayment.subtract(interest);
            remainingBalance = remainingBalance.subtract(principalPayment);
            entries.add(new AmortizationEntry(monthlyPayment, interest, principalPayment, remainingBalance));
        }

        return new AmortizationSchedule(entries);
    }

    public static BigDecimal calculateMonthlyPayment(BigDecimal principal, BigDecimal monthlyInterestRate,
                                                      int termInMonths) {
        BigDecimal monthlyInterestFactor = monthlyInterestRate.add(BigDecimal.ONE).pow(termInMonths);
        BigDecimal numerator = principal.multiply(monthlyInterestRate).multiply(monthlyInterestFactor);
        BigDecimal denominator = monthlyInterestFactor.subtract(BigDecimal.ONE);
        return numerator.divide(denominator, 10, RoundingMode.HALF_UP);
    }

    public static AmortizationSchedule applyExtraPayment(AmortizationSchedule amortizationSchedule,
                                                         ExtraPayment extraPayment) {
        List<AmortizationEntry> entries = new ArrayList<>(amortizationSchedule.getEntries());

        // Apply extra payment to the specified month
        AmortizationEntry extraPaymentEntry = entries.get(extraPayment.getExtraPaymentMonth());
        BigDecimal newRemainingBalance = extraPaymentEntry.getRemainingBalance().subtract(extraPayment.getExtraPaymentAmount());
        if (newRemainingBalance.compareTo(BigDecimal.ZERO) < 0) {
            newRemainingBalance = BigDecimal.ZERO;
        }
        extraPaymentEntry.setRemainingBalance(newRemainingBalance);

        // Track the adjusted remaining balance after applying the extra payment
        BigDecimal adjustedRemainingBalance = newRemainingBalance;

        // Update subsequent entries' payment, interest, principal, and remaining balance
        BigDecimal monthlyInterestRate =
                amortizationSchedule.getMonthlyPayment().divide(amortizationSchedule.getEntries().get(0).getRemainingBalance(), 10, RoundingMode.HALF_UP); // Original monthly interest rate

        for (int i = extraPayment.getExtraPaymentMonth() + 1; i < entries.size(); i++) {
            AmortizationEntry entry = entries.get(i);

            // Calculate new payment, interest, principal, and remaining balance based on the adjusted remaining balance
            entry.setPayment(calculateMonthlyPayment(adjustedRemainingBalance, monthlyInterestRate,
                    entries.size() - i)); // New payment for this month
            entry.setInterest(adjustedRemainingBalance.multiply(monthlyInterestRate));
            entry.setPrincipal(entry.getPayment().subtract(entry.getInterest()));
            adjustedRemainingBalance = adjustedRemainingBalance.subtract(entry.getPrincipal());

            if (adjustedRemainingBalance.compareTo(BigDecimal.ZERO) <= 0) {
                adjustedRemainingBalance = BigDecimal.ZERO;
            }
            entry.setRemainingBalance(adjustedRemainingBalance);
        }
        return new AmortizationSchedule(entries);
    }

}
