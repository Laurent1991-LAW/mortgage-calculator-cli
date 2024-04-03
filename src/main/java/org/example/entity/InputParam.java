package org.example.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

/**
 * @Desc:
 * @Author: Lauren LUO
 * @Date: 2024/04/03
 */
@Data
@AllArgsConstructor
@Builder
public class InputParam {
    private BigDecimal loanAmount;
    private int termInYears;
    private BigDecimal annualInterestRate;
    private BigDecimal downPayment ;
    private ExtraPayment extraPayment;
}
