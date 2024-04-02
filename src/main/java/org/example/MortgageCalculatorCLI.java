package org.example;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

public class MortgageCalculatorCLI {
    private static final int MONTHS_IN_YEAR = 12;
    private static final String DEFAULT_OUTPUT_FILE_NAME = "output.txt";

    public static void main(String[] args) throws ParseException {
        try {
            String inputPath = System.getProperty("inputPath");
            String outputPath = System.getProperty("outputPath");

            if (inputPath == null) {
                System.err.println("Error: Please provide the input path using the -DinputPath=<path> option.");
                return;
            }

            /** input file parsing */
            File inputFile = new File(inputPath);
            JSONParser parser = new JSONParser();
            JSONObject jsonInput =
                    (JSONObject) parser.parse(new FileReader(inputFile));

            BigDecimal loanAmount = new BigDecimal(jsonInput.get("loan_amount").toString());
            int termInYears = Integer.parseInt(jsonInput.get("term").toString());
            BigDecimal annualInterestRate = new BigDecimal(jsonInput.get("rate").toString());
            BigDecimal downPayment = new BigDecimal(jsonInput.get("down_payment").toString());

            /** calculate Amortization Schedule */
            AmortizationSchedule amortizationSchedule = calculateAmortizationSchedule(loanAmount, termInYears,
                    annualInterestRate, downPayment);

            Path resolvedOutputPath = resolveOutputPath(outputPath);
            if (resolvedOutputPath != null) {
                saveResultsToFile(resolvedOutputPath, amortizationSchedule);
                System.out.println("\nResults saved to " + resolvedOutputPath.toAbsolutePath());
            } else {
                System.out.println("\nNo valid output path provided, printing results to console only.");
            }
            printAmortizationSchedule(amortizationSchedule);

            JSONObject extraPayment = (JSONObject) jsonInput.get("extra_payment");
            int extraPaymentMonth = Integer.parseInt(extraPayment.get("month").toString());
            BigDecimal extraPaymentAmount = new BigDecimal(extraPayment.get("amount").toString());

            AmortizationSchedule modifiedAmortizationSchedule = applyExtraPayment(amortizationSchedule,
                    extraPaymentMonth, extraPaymentAmount);

            System.out.println("\nNew Amortization Schedule (Post Extra Payment):");
            System.out.println("Month\tPayment\t\tInterest\tPrincipal\tRemaining Balance");
            for (int i = extraPaymentMonth; i < extraPaymentMonth + 24; i++) {
                AmortizationEntry entry = modifiedAmortizationSchedule.getAmortizationEntry(i);
                System.out.println((i + 1) + "\t\t$" + entry.getPayment().setScale(2, RoundingMode.HALF_UP) +
                        "\t\t$" + entry.getInterest().setScale(2, RoundingMode.HALF_UP) +
                        "\t\t$" + entry.getPrincipal().setScale(2, RoundingMode.HALF_UP) +
                        "\t\t$" + entry.getRemainingBalance().setScale(2, RoundingMode.HALF_UP));
            }

            if (resolvedOutputPath != null) {
                appendExtraResultsToFile(resolvedOutputPath, modifiedAmortizationSchedule, extraPaymentMonth);
                System.out.println("\nResults saved to " + resolvedOutputPath.toAbsolutePath());
            }
        } catch (IOException | org.json.simple.parser.ParseException e) {
            e.printStackTrace();
        }
    }

    private static void appendExtraResultsToFile(Path resolvedOutputPath,
                                                 AmortizationSchedule modifiedAmortizationSchedule,
                                                 int extraPaymentMonth) {
        try (FileWriter writer = new FileWriter(resolvedOutputPath.toFile(), true)) {
            writer.write("\nNew Amortization Schedule (Post Extra Payment):\n");
            writer.write("Month\tPayment\t\tInterest\tPrincipal\tRemaining Balance\n");
            for (int i = extraPaymentMonth; i < extraPaymentMonth + 24; i++) {
                AmortizationEntry entry = modifiedAmortizationSchedule.getAmortizationEntry(i);
                writer.write((i + 1) + "\t\t$" + entry.getPayment().setScale(2, RoundingMode.HALF_UP) +
                        "\t\t$" + entry.getInterest().setScale(2, RoundingMode.HALF_UP) +
                        "\t\t$" + entry.getPrincipal().setScale(2, RoundingMode.HALF_UP) +
                        "\t\t$" + entry.getRemainingBalance().setScale(2, RoundingMode.HALF_UP) + "\n");
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static void printAmortizationSchedule(AmortizationSchedule amortizationSchedule) {
        System.out.println("\nMonthly payment: $" + amortizationSchedule.getMonthlyPayment().setScale(2,
                RoundingMode.HALF_UP));
        System.out.println("\nAmortization Schedule:");
        System.out.println("Month\tPayment\t\tInterest\tPrincipal\tRemaining Balance");
        for (int i = 0; i < amortizationSchedule.getMonths(); i++) {
            AmortizationEntry entry = amortizationSchedule.getAmortizationEntry(i);
            System.out.println((i + 1) + "\t\t$" + entry.getPayment().setScale(2, RoundingMode.HALF_UP) +
                    "\t\t$" + entry.getInterest().setScale(2, RoundingMode.HALF_UP) +
                    "\t\t$" + entry.getPrincipal().setScale(2, RoundingMode.HALF_UP) +
                    "\t\t$" + entry.getRemainingBalance().setScale(2, RoundingMode.HALF_UP));
        }
    }

    private static Path resolveOutputPath(String outputPath) {
        if (outputPath == null || outputPath.isEmpty()) {
            return null;
        }

        Path outputDir = Paths.get(outputPath);
        if (!Files.isDirectory(outputDir)) {
            System.err.println("Error: Specified output path is not a valid directory.");
            return null;
        }

        Path resolvedOutputPath = outputDir.resolve(DEFAULT_OUTPUT_FILE_NAME);
        return resolvedOutputPath;
    }

    private static void saveResultsToFile(Path outputPath, AmortizationSchedule amortizationSchedule) throws IOException {
        try (FileWriter writer = new FileWriter(outputPath.toFile())) {
            writer.write("Monthly payment: $" + amortizationSchedule.getMonthlyPayment().setScale(2,
                    RoundingMode.HALF_UP) + "\n");
            writer.write("\nAmortization Schedule:\n");
            writer.write("Month\tPayment\t\tInterest\tPrincipal\tRemaining Balance\n");
            for (int i = 0; i < amortizationSchedule.getMonths(); i++) {
                AmortizationEntry entry = amortizationSchedule.getAmortizationEntry(i);
                writer.write((i + 1) + "\t\t$" + entry.getPayment().setScale(2, RoundingMode.HALF_UP) +
                        "\t\t$" + entry.getInterest().setScale(2, RoundingMode.HALF_UP) +
                        "\t\t$" + entry.getPrincipal().setScale(2, RoundingMode.HALF_UP) +
                        "\t\t$" + entry.getRemainingBalance().setScale(2, RoundingMode.HALF_UP) + "\n");
            }
        }
    }

    private static AmortizationSchedule calculateAmortizationSchedule(BigDecimal loanAmount, int termInYears,
                                                                      BigDecimal annualInterestRate,
                                                                      BigDecimal downPayment) {
        int termInMonths = termInYears * MONTHS_IN_YEAR;
        BigDecimal monthlyInterestRate = annualInterestRate.divide(BigDecimal.valueOf(100 * MONTHS_IN_YEAR), 10,
                RoundingMode.HALF_UP);

        BigDecimal principal = loanAmount.subtract(downPayment);
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

    private static BigDecimal calculateMonthlyPayment(BigDecimal principal, BigDecimal monthlyInterestRate,
                                                      int termInMonths) {
        BigDecimal monthlyInterestFactor = monthlyInterestRate.add(BigDecimal.ONE).pow(termInMonths);
        BigDecimal numerator = principal.multiply(monthlyInterestRate).multiply(monthlyInterestFactor);
        BigDecimal denominator = monthlyInterestFactor.subtract(BigDecimal.ONE);
        return numerator.divide(denominator, 10, RoundingMode.HALF_UP);
    }

    private static AmortizationSchedule applyExtraPayment(AmortizationSchedule amortizationSchedule,
                                                          int extraPaymentMonth, BigDecimal extraPaymentAmount) {
        List<AmortizationEntry> entries = new ArrayList<>(amortizationSchedule.getEntries());

        // Apply extra payment to the specified month
        AmortizationEntry extraPaymentEntry = entries.get(extraPaymentMonth);
        BigDecimal newRemainingBalance = extraPaymentEntry.getRemainingBalance().subtract(extraPaymentAmount);
        if (newRemainingBalance.compareTo(BigDecimal.ZERO) < 0) {
            newRemainingBalance = BigDecimal.ZERO;
        }
        extraPaymentEntry.setRemainingBalance(newRemainingBalance);

        // Track the adjusted remaining balance after applying the extra payment
        BigDecimal adjustedRemainingBalance = newRemainingBalance;

        // Update subsequent entries' payment, interest, principal, and remaining balance
        BigDecimal monthlyInterestRate =
                amortizationSchedule.getMonthlyPayment().divide(amortizationSchedule.getEntries().get(0).getRemainingBalance(), 10, RoundingMode.HALF_UP); // Original monthly interest rate

        for (int i = extraPaymentMonth + 1; i < entries.size(); i++) {
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

class AmortizationSchedule {
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

@Data
@AllArgsConstructor
class AmortizationEntry {
    private BigDecimal payment;
    private BigDecimal interest;
    private BigDecimal principal;
    private BigDecimal remainingBalance;
}
