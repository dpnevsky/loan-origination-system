package calculator.service.impl;

import core.dto.CreditDto;
import core.dto.EmploymentDto;
import core.dto.PaymentScheduleElementDto;
import core.dto.ScoringDataDto;
import core.type.EmploymentStatusType;
import core.type.GenderType;
import core.type.MaritalStatusType;
import core.type.PositionType;
import org.springframework.beans.factory.annotation.Autowired;
import calculator.exception.LoanCheckException;
import calculator.service.util.loancheck.LoanApplicationProcessor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@SpringBootTest
@ActiveProfiles("test")
class LoanCalculatorServiceImplTest {

    @Value("${loan.base-rate}")
    private BigDecimal baseRate;

    @MockBean
    private LoanApplicationProcessor loanApplicationProcessor;

    @Autowired
    private LoanCalculatorServiceImpl loanCalculatorService;

    private ScoringDataDto scoringDataDto;
    private CreditDto creditDto;

    @BeforeEach
    void setUp() {
        scoringDataDto = createScoringData(BigDecimal.valueOf(20000), 12, false);
        when(loanApplicationProcessor.processLoanApplication(scoringDataDto, baseRate)).thenReturn(BigDecimal.valueOf(13));
    }

    private ScoringDataDto createScoringData(BigDecimal amount, int term, boolean insuranceEnabled) {
        return ScoringDataDto.builder()
                .withEmployment(EmploymentDto.builder()
                        .withEmploymentStatus(EmploymentStatusType.EMPLOYED)
                        .withSalary(BigDecimal.valueOf(30000))
                        .withEmployerINN("12345678912")
                        .withPosition(PositionType.JUNIOR_MANAGER)
                        .withWorkExperienceTotal(19)
                        .withWorkExperienceCurrent(6)
                        .build())
                .withTerm(term)
                .withMaritalStatus(MaritalStatusType.DIVORCED)
                .withBirthdate(LocalDate.of(2000, 1, 1))
                .withPassportIssueBranch("Moscow")
                .withGender(GenderType.MALE)
                .withAmount(amount)
                .withFirstName("Ivan")
                .withLastName("Ivanov")
                .withMiddleName("Dmitrievich")
                .withPassportSeries("1234")
                .withPassportNumber("654321")
                .withIsInsuranceEnabled(insuranceEnabled)
                .withIsSalaryClient(false)
                .build();
    }

    @Test
    void testConfigValues() {
        assertNotNull(baseRate, "Base rate must not be null");
    }

    @Test
    void calculateLoanValidInputSuccessfulCalculation() throws LoanCheckException {
        when(loanApplicationProcessor.processLoanApplication(scoringDataDto, baseRate)).thenReturn(BigDecimal.valueOf(13));

        creditDto = loanCalculatorService.calculateLoan(scoringDataDto);

        assertNotNull(creditDto);
        assertEquals(BigDecimal.valueOf(1786), creditDto.monthlyPayment());
        assertEquals(BigDecimal.valueOf(13.0), creditDto.psk().setScale(1, RoundingMode.HALF_EVEN));
        assertEquals(12, creditDto.term());
    }

    @Test
    void calculateLoanExpectedLastRemainingDebtZero() {
        creditDto = loanCalculatorService.calculateLoan(scoringDataDto);

        assertEquals(0, creditDto.paymentSchedule().get(creditDto.term() - 1).remainingDebt().compareTo(BigDecimal.ZERO));
    }

    @Test
    void generatePaymentScheduleValidInputReturnsSchedule() {
        creditDto = loanCalculatorService.calculateLoan(scoringDataDto);

        Integer term = 12;
        List<PaymentScheduleElementDto> schedule = loanCalculatorService.calculateLoan(scoringDataDto).paymentSchedule();

        assertNotNull(schedule);
        assertEquals(term, schedule.size());
        assertTrue(schedule.stream().allMatch(element -> element.totalPayment().compareTo(BigDecimal.ZERO) > 0));
    }

    @ParameterizedTest
    @CsvSource({
            "200000, 24, 12, false, 200000, 2000.00",
            "200000, 24, 12.5, false, 200000, 2083.33",
            "200000, 24, 12.5, true, 208000, 2166.67",
            "3000000, 120, 23, true, 3100000, 59416.67"
    })
    void calculateLoanPaymentScheduleBalances(BigDecimal requestedAmount, int term, BigDecimal rate,
                                             boolean insuranceEnabled, BigDecimal expectedPrincipal,
                                             BigDecimal expectedFirstInterest) {
        ScoringDataDto scoringData = createScoringData(requestedAmount, term, insuranceEnabled);
        when(loanApplicationProcessor.processLoanApplication(scoringData, baseRate)).thenReturn(rate);

        CreditDto credit = loanCalculatorService.calculateLoan(scoringData);
        List<PaymentScheduleElementDto> schedule = credit.paymentSchedule();
        BigDecimal principalPayments = schedule.stream()
                .map(PaymentScheduleElementDto::debtPayment)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal interestPayments = schedule.stream()
                .map(PaymentScheduleElementDto::interestPayment)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalPayments = schedule.stream()
                .map(PaymentScheduleElementDto::totalPayment)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        assertEquals(term, schedule.size());
        assertEquals(0, expectedPrincipal.compareTo(principalPayments), "The schedule must repay all principal");
        assertEquals(0, credit.amount().compareTo(totalPayments), "Credit amount must include all actual payments");
        assertEquals(0, credit.amount().subtract(expectedPrincipal).compareTo(interestPayments));
        assertEquals(0, expectedFirstInterest.compareTo(schedule.get(0).interestPayment()));

        BigDecimal remainingPayments = credit.amount();
        for (PaymentScheduleElementDto element : schedule) {
            assertEquals(0, element.totalPayment().compareTo(element.debtPayment().add(element.interestPayment())));
            assertTrue(element.debtPayment().compareTo(BigDecimal.ZERO) > 0);
            remainingPayments = remainingPayments.subtract(element.totalPayment());
            assertEquals(0, remainingPayments.compareTo(element.remainingDebt()),
                    "Remaining debt must include all future principal and interest payments");
            assertTrue(element.remainingDebt().compareTo(BigDecimal.ZERO) >= 0);
        }
        assertEquals(0, remainingPayments.compareTo(BigDecimal.ZERO));
    }
}
