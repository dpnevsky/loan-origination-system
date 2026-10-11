package calculator.service.impl;

import calculator.exception.LoanCheckException;
import calculator.service.LoanCalculatorService;
import calculator.service.util.loancheck.LoanApplicationProcessor;
import core.dto.CreditDto;
import core.dto.PaymentScheduleElementDto;
import core.dto.ScoringDataDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.util.ArrayList;
import java.util.List;
import java.math.RoundingMode;
import java.math.BigDecimal;
import java.time.LocalDate;
import static calculator.service.util.Constant.INITIAL_INSURANCE_PRICE;
import static calculator.service.util.Constant.ONE_HUNDRED_PERCENT;
import static calculator.service.util.Constant.TWELVE_MONTH;
import static calculator.service.util.ServiceForCalculate.calculateInsurancePrice;
import static calculator.service.util.ServiceForCalculate.calculateMonthlyPayment;
import static calculator.service.util.ServiceForCalculate.calculatePSK;

@Slf4j
@Service
public class LoanCalculatorServiceImpl implements LoanCalculatorService {

    @Value("${loan.base-rate}")
    private BigDecimal baseRate;

    private final LoanApplicationProcessor loanApplicationProcessor;

    public LoanCalculatorServiceImpl(LoanApplicationProcessor loanApplicationProcessor) {
        this.loanApplicationProcessor = loanApplicationProcessor;
    }

    @Override
    public CreditDto calculateLoan(ScoringDataDto scoringData) throws LoanCheckException {
        log.info("Starting loan calculation with scoring data: {}", scoringData);
        BigDecimal rate = loanApplicationProcessor.processLoanApplication(scoringData, baseRate);
        log.debug("Calculated rate: {}", rate);

        Boolean insuranceEnabled = scoringData.isInsuranceEnabled();
        Boolean isSalaryClient = scoringData.isSalaryClient();
        BigDecimal requestedAmount = scoringData.amount();
        BigDecimal insurancePrice = INITIAL_INSURANCE_PRICE;
        if(insuranceEnabled) {
            insurancePrice = calculateInsurancePrice(scoringData.amount());
            requestedAmount = scoringData.amount().add(insurancePrice);
        }
        log.debug("Insured price: {}, Requested amount with insurance price: {}", insurancePrice, requestedAmount);

        Integer term = scoringData.term();
        BigDecimal monthlyPayment = calculateMonthlyPayment(requestedAmount, rate, term);
        log.debug("Calculated monthly payment: {}", monthlyPayment);

        List<PaymentScheduleElementDto> paymentSchedule = generatePaymentSchedule(requestedAmount, rate, monthlyPayment, term);
        log.debug("Generated payment schedule with {} elements", paymentSchedule.size());

        BigDecimal psk = calculatePSK(requestedAmount, paymentSchedule);
        log.debug("Calculated PSK (Full Loan Cost): {}", psk);

        BigDecimal totalPayment = paymentSchedule.stream()
                .map(PaymentScheduleElementDto::totalPayment)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        CreditDto creditDto = CreditDto.builder()
                .withAmount(totalPayment)
                .withTerm(term)
                .withMonthlyPayment(monthlyPayment)
                .withRate(rate)
                .withPsk(psk)
                .withIsInsuranceEnabled(insuranceEnabled)
                .withIsSalaryClient(isSalaryClient)
                .withPaymentSchedule(paymentSchedule)
                .build();

        log.info("Loan calculation completed. Result: {}", creditDto);
        return creditDto;
    }

    private List<PaymentScheduleElementDto> generatePaymentSchedule(BigDecimal principal, BigDecimal rate,
                                                                    BigDecimal monthlyPayment, Integer term) {
        log.debug("Generating payment schedule with Principal: {}, Rate: {}, MonthlyPayment: {}, Term: {}",
                principal, rate, monthlyPayment, term);

        BigDecimal remainingPrincipal = principal;
        LocalDate calculationDate = LocalDate.now();
        List<PaymentScheduleElementDto> paymentScheduleElementDtoList = new ArrayList<>(term);

        for (int i = 1; i <= term; i++) {
            BigDecimal interestPayment = remainingPrincipal.multiply(rate)
                    .divide(ONE_HUNDRED_PERCENT.multiply(TWELVE_MONTH), 2, RoundingMode.HALF_UP);
            // The final payment settles the principal left after rounding previous payments.
            BigDecimal debtPayment = i == term ? remainingPrincipal : monthlyPayment.subtract(interestPayment);
            BigDecimal totalPayment = debtPayment.add(interestPayment);
            remainingPrincipal = remainingPrincipal.subtract(debtPayment);

            paymentScheduleElementDtoList.add(PaymentScheduleElementDto.builder()
                    .withNumber(i)
                    .withDate(calculationDate.plusMonths(i))
                    .withTotalPayment(totalPayment)
                    .withInterestPayment(interestPayment)
                    .withDebtPayment(debtPayment)
                    .withRemainingDebt(BigDecimal.ZERO)
                    .build());
        }

        // remainingDebt is the total of future payments, including principal and interest.
        BigDecimal remainingPayments = BigDecimal.ZERO;
        for (int i = paymentScheduleElementDtoList.size() - 1; i >= 0; i--) {
            PaymentScheduleElementDto element = paymentScheduleElementDtoList.get(i);
            PaymentScheduleElementDto updatedElement = new PaymentScheduleElementDto(
                    element.number(), element.date(), element.totalPayment(), element.interestPayment(),
                    element.debtPayment(), remainingPayments);
            paymentScheduleElementDtoList.set(i, updatedElement);
            remainingPayments = remainingPayments.add(element.totalPayment());
            log.debug("Generated payment schedule element: {}", updatedElement);
        }

        log.debug("Completed payment schedule generation");
        return paymentScheduleElementDtoList;
    }
}
