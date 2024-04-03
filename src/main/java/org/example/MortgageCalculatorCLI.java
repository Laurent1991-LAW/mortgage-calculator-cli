package org.example;

import org.example.entity.AmortizationEntry;
import org.example.entity.AmortizationSchedule;
import org.example.entity.ExtraPayment;
import org.example.entity.InputParam;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URLDecoder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.example.service.MortgageCalculatorService.applyExtraPayment;
import static org.example.service.MortgageCalculatorService.calculateAmortizationSchedule;

/**
 * @Desc:
 * @Author: Lauren LUO
 * @Date: 2024/04/02
 */
public class MortgageCalculatorCLI {

    private static final String DEFAULT_OUTPUT_FILE_NAME = "amortizationSchedule.txt";
    private static final String TABLE_COLUMN = "Month\tPayment\t\tInterest\tPrincipal\tRemaining Balance";

    public static void main(String[] args) {
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

            /** json object parsing */
            InputParam param = parseInputJson(jsonInput);

            /** param valid check */
            checkParamValidity(param);

            /** calculate Amortization Schedule without extra payment */
            AmortizationSchedule amortizationSchedule = calculateAmortizationSchedule(param);

            Path resolvedOutputPath = resolveOutputPath(outputPath);

            printAmortizationSchedule(amortizationSchedule);
            saveMajorResultsToFile(resolvedOutputPath, amortizationSchedule);

            ExtraPayment extraPayment = param.getExtraPayment();
            int extraPaymentMonth = extraPayment.getExtraPaymentMonth();

            /** Ajust Amortization Schedule with extra payment*/
            AmortizationSchedule modifiedAmortizationSchedule = applyExtraPayment(amortizationSchedule,
                    extraPayment);

            printExtraAmortizationSchedule(extraPaymentMonth, modifiedAmortizationSchedule);
            appendExtraResultsToFile(resolvedOutputPath, modifiedAmortizationSchedule, extraPaymentMonth);

        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    private static void printExtraAmortizationSchedule(int extraPaymentMonth,
                                                       AmortizationSchedule modifiedAmortizationSchedule)
    {
        System.out.println("\nNew Amortization Schedule (Post Extra Payment):");
        System.out.println(TABLE_COLUMN);
        for (int i = extraPaymentMonth; i < extraPaymentMonth + 24; i++) {
            AmortizationEntry entry = modifiedAmortizationSchedule.getAmortizationEntry(i);
            System.out.println((i + 1) + "\t\t$" + entry.getPayment().setScale(2, RoundingMode.HALF_UP) +
                    "\t\t$" + entry.getInterest().setScale(2, RoundingMode.HALF_UP) +
                    "\t\t$" + entry.getPrincipal().setScale(2, RoundingMode.HALF_UP) +
                    "\t\t$" + entry.getRemainingBalance().setScale(2, RoundingMode.HALF_UP));
        }
    }

    public static void appendExtraResultsToFile(Path resolvedOutputPath,
                                                AmortizationSchedule modifiedAmortizationSchedule,
                                                int extraPaymentMonth)
    {
        try (FileWriter writer = new FileWriter(resolvedOutputPath.toFile(), true)) {
            writer.write("\nNew Amortization Schedule (Post Extra Payment):\n");
            writer.write(TABLE_COLUMN);
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

    private static void checkParamValidity(InputParam param) {
        // todo
    }

    private static InputParam parseInputJson(JSONObject jsonInput) {
        JSONObject extraPayment = (JSONObject) jsonInput.get("extra_payment");
        return InputParam.builder()
                .loanAmount(new BigDecimal(jsonInput.get("loan_amount").toString()))
                .termInYears(Integer.parseInt(jsonInput.get("term").toString()))
                .annualInterestRate(new BigDecimal(jsonInput.get("rate").toString()))
                .downPayment(new BigDecimal(jsonInput.get("down_payment").toString()))
                .extraPayment(
                        ExtraPayment.builder()
                                .extraPaymentMonth(Integer.parseInt(extraPayment.get("month").toString()))
                                .extraPaymentAmount(new BigDecimal(extraPayment.get("amount").toString()))
                                .build()
                ).build();
    }

    public static void printAmortizationSchedule(AmortizationSchedule amortizationSchedule) {
        System.out.println("\nMonthly payment: $"
                + amortizationSchedule.getMonthlyPayment().setScale(2, RoundingMode.HALF_UP));
        System.out.println("\nAmortization Schedule:");
        System.out.println(TABLE_COLUMN);
        for (int i = 0; i < amortizationSchedule.getMonths(); i++) {
            AmortizationEntry entry = amortizationSchedule.getAmortizationEntry(i);
            System.out.println((i + 1) + "\t\t$" + entry.getPayment().setScale(2, RoundingMode.HALF_UP) +
                    "\t\t$" + entry.getInterest().setScale(2, RoundingMode.HALF_UP) +
                    "\t\t$" + entry.getPrincipal().setScale(2, RoundingMode.HALF_UP) +
                    "\t\t$" + entry.getRemainingBalance().setScale(2, RoundingMode.HALF_UP));
        }
    }

    /**
     * check if output path valid, if not, save file to jar folder
     * @param outputPath
     * @return
     */
    private static Path resolveOutputPath(String outputPath) throws Exception {
        Path outputDir = null;

        boolean save2JarFolder = false;
        if (outputPath == null || outputPath.isEmpty()) {
            System.out.println("\nWarn: Output path isn't specified.");
            save2JarFolder = true;
        } else {
            outputDir = Paths.get(outputPath);
            if (!Files.isDirectory(outputDir)) {
                System.err.println("\nError: specified output path is not a valid directory.");
                save2JarFolder = true;
            }
        }

        if (save2JarFolder) {
            // obtain jar directory
            String path = MortgageCalculatorCLI.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath();
            String jarFolder = new File(URLDecoder.decode(path, "UTF-8")).getParent();
            outputDir = Paths.get(jarFolder);
        }

        Path resolvedOutputPath = outputDir.resolve(DEFAULT_OUTPUT_FILE_NAME);
        System.out.println("\nResults saved to " + resolvedOutputPath.toAbsolutePath());
        return resolvedOutputPath;
    }

    private static void saveMajorResultsToFile(Path outputPath, AmortizationSchedule amortizationSchedule) throws IOException {
        try (FileWriter writer = new FileWriter(outputPath.toFile())) {
            writer.write("Monthly payment: $" + amortizationSchedule.getMonthlyPayment().setScale(2,
                    RoundingMode.HALF_UP) + "\n");
            writer.write("\nAmortization Schedule:\n");
            writer.write(TABLE_COLUMN);
            for (int i = 0; i < amortizationSchedule.getMonths(); i++) {
                AmortizationEntry entry = amortizationSchedule.getAmortizationEntry(i);
                writer.write((i + 1) + "\t\t$" + entry.getPayment().setScale(2, RoundingMode.HALF_UP) +
                        "\t\t$" + entry.getInterest().setScale(2, RoundingMode.HALF_UP) +
                        "\t\t$" + entry.getPrincipal().setScale(2, RoundingMode.HALF_UP) +
                        "\t\t$" + entry.getRemainingBalance().setScale(2, RoundingMode.HALF_UP) + "\n");
            }
        }
    }


}


