# mortgage-calculator-cli

## Overview:
This is a command-line mortgage calculator. It can calculate monthly mortgage payments, generate an amortization schedule for a specified term, and show the impact of additional lump sum payments on the amortization schedule.



## Functionalities:
- Programming Language: Java 11
- Functionalities:
  1.	Calculate monthly mortgage payments using loan amount, term, down payment, and interest rate.
  2.	Generate an amortization schedule for the first m years.
  3.	Modify the amortization schedule based on a lump sum payment made after a specific month.
        
## Input Specification:

The tool accept a JSON file as input. The JSON file will specify the loan amount, term in years, annual interest rate, down payment, and any extra payment details.
        Example JSON Input:
```
{
        "loan_amount": "500000",
        "term": "30",
        "rate": "4.9",
        "down_payment": "20000",
        "extra_payment": {
        "month": "36",
        "amount": "80000"
        }
}
```

      
## Note:
The `loan_amount` is the total loan before down payment, term is the duration of the loan in years, rate is the annual interest rate (percentage), `down_payment` is the initial payment subtracted from the loan amount, and `extra_payment` specifies an additional payment to principal after a certain month.
        
## Output:
        
The program will output the monthly payment and an amortization schedule. After applying an extra payment, it will also provide a new amortization schedule for the following 24 months.

````
Sample Output Format:
Monthly payment: $xxxx.xx

Amortization Schedule:
Month	Payment	Interest    Principal	Remaining Balance
1    	$xxxx.xx   $aaaa.xx    $bbbb.xx 	$ccccccc.xx
2    	$xxxx.xx   $aaaa.xx    $bbbb.xx 	$ccccccc.xx
...
And after an extra payment:
New Amortization Schedule (Post Extra Payment):
Month	Payment	Interest    Principal	Remaining Balance
...
````

## Instructions:
- Java 11 (or above) JRE installed
- download `mortgage-calculator-cli.jar` to local and run in commandline
`java -DinputPath=/json/input/file/location/input.json -DoutputPath=/results/file/folder/ -jar /jar/folder/mortgage-calculator-cli.jar`
- `inputPath` option is mandatory
- `outputPath` option is optional, by default or in case of invalid path, results file will be saved to jar folder