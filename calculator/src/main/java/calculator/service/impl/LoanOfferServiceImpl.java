package calculator.service.impl;


import calculator.service.LoanOfferService;
import calculator.service.util.ServiceForCalculate;
import core.dto.LoanOfferDto;
import core.dto.LoanStatementRequestDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
@Service
public class LoanOfferServiceImpl implements LoanOfferService {

    @Value("${loan.base-rate}")
    private BigDecimal baseRate;

    @Value("${loan.salary-client-rate-discount}")
    private BigDecimal salaryClientDiscount;

    @Value("${loan.insurance-rate-discount}")
    private BigDecimal insuranceRateDiscount;

    @Override
    public List<LoanOfferDto> generateLoanOffers(LoanStatementRequestDto request) {
        log.info("Starting loan offers generation with request: {}", request);

        BigDecimal requestedAmount = request.amount();
        Integer term = request.term();

        log.debug("Input values - Requested Amount: {}, Term: {}", requestedAmount, term);
        BigDecimal insurancePrice = ServiceForCalculate.calculateInsurancePrice(requestedAmount);
        log.debug("Insurance price will be: {}", insurancePrice);

        List<LoanOfferDto> offers =
                Stream.of(false, true) // Перебираем значения для isInsuranceEnabled
                        .flatMap(isInsuranceEnabled ->
                                Stream.of(false, true) // Перебираем значения для isSalaryClient
                                        .map(isSalaryClient -> {
                                            BigDecimal rate = calculateRateForLoanOffers(isInsuranceEnabled, isSalaryClient);
                                            BigDecimal totalAmount = requestedAmount
                                                    .add(isInsuranceEnabled ? insurancePrice : BigDecimal.ZERO);
                                            BigDecimal monthlyPayment = ServiceForCalculate
                                                    .calculateMonthlyPayment(totalAmount, rate, term);

                                            log.debug("Intermediate values - IsInsuranceEnabled: {}, IsSalaryClient: {}, " +
                                                            "Rate: {}, TotalAmount: {}, MonthlyPayment: {}",
                                                    isInsuranceEnabled, isSalaryClient, rate, totalAmount, monthlyPayment);

                                            return LoanOfferDto.builder()
                                                    .withStatementId(UUID.randomUUID())
                                                    .withRequestedAmount(requestedAmount)
                                                    .withTotalAmount(totalAmount)
                                                    .withTerm(term)
                                                    .withMonthlyPayment(monthlyPayment)
                                                    .withRate(rate)
                                                    .withIsInsuranceEnabled(isInsuranceEnabled)
                                                    .withIsSalaryClient(isSalaryClient)
                                                    .build();
                                        })
                        ).sorted(Comparator.comparing(LoanOfferDto::getRate).reversed()).collect(Collectors.toList());

        log.info("Generated loan offers: {}", offers);
        return offers;
    }

    private BigDecimal calculateRateForLoanOffers(boolean isInsuranceEnabled, boolean isSalaryClient) {
        log.debug("Calculating rate with IsInsuranceEnabled: {}, IsSalaryClient: {}", isInsuranceEnabled, isSalaryClient);
        BigDecimal rate = baseRate;
        if (isInsuranceEnabled) {
            rate = rate.subtract(insuranceRateDiscount);
        }
        if (isSalaryClient) {
            rate = rate.subtract(salaryClientDiscount);
        }

        log.debug("Calculated rate: {}", rate);
        return rate;
    }
}
