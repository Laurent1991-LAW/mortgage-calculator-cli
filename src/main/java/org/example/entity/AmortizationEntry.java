package org.example.entity;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
public class AmortizationEntry {
    private BigDecimal payment;
    private BigDecimal interest;
    private BigDecimal principal;
    private BigDecimal remainingBalance;
}
