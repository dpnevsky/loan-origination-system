package calculator.service.impl;

import core.dto.LoanOfferDto;
import core.dto.LoanStatementRequestDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class LoanOfferServiceImplTest {

    @Value("${loan.base-rate}")
    private BigDecimal baseRate;

    @Value("${loan.salary-client-rate-discount}")
    private BigDecimal salaryClientDiscount;

    @Value("${loan.insurance-rate-discount}")
    private BigDecimal insuranceRateDiscount;

    @Autowired
    private LoanOfferServiceImpl loanOfferService;
    private LoanStatementRequestDto request;
    private List<LoanOfferDto> offers;

    @BeforeEach
    void setUp() {
        request = createRequest(BigDecimal.valueOf(20000));
    }

    private LoanStatementRequestDto createRequest(BigDecimal amount) {
        return LoanStatementRequestDto.builder()
                .withAmount(amount)
                .withTerm(12)
                .withFirstName("Ivan")
                .withLastName("Ivanov")
                .withMiddleName("Dmitrievich")
                .withEmail("ivan.ivanov@yandex.ru")
                .withBirthdate(LocalDate.of(1990, 1, 1))
                .withPassportSeries("1234")
                .withPassportNumber("654321")
                .build();
    }

    @Test
    void testConfigValues() {

        assertNotNull(baseRate, "Base rate must not be null");
        assertNotNull(salaryClientDiscount, "Salary client discount must not be null");
        assertNotNull(insuranceRateDiscount, "Insurance rate discount must not be null");
    }

    @Test
    void generateLoanOffersValidRequestSuccess() {

        offers = loanOfferService.generateLoanOffers(request);

        assertNotNull(offers);
        assertEquals(4, offers.size());
    }

    @Test
    void generateLoanOffersCheckRateCalculation() {
        offers = loanOfferService.generateLoanOffers(request);

        assertTrue(offers.stream().allMatch(offer -> offer.getRate().compareTo(BigDecimal.ZERO) > 0));
    }

    @Test
    void generateLoanOffersSortedByRate() {
        offers = loanOfferService.generateLoanOffers(request);

        assertEquals(4, offers.size());
        for (int i = 1; i < offers.size(); i++) {
            assertTrue(offers.get(i - 1).getRate().compareTo(offers.get(i).getRate()) >= 0);
        }
    }

    @Test
    void concurrentRequestsKeepTheirOwnInsurancePrice() throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(8);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<List<LoanOfferDto>>> results = new ArrayList<>();

        try {
            for (int i = 0; i < 64; i++) {
                BigDecimal amount = BigDecimal.valueOf(20000 + 100000L * i);
                results.add(executor.submit(() -> {
                    start.await();
                    return loanOfferService.generateLoanOffers(createRequest(amount));
                }));
            }
            start.countDown();

            for (int i = 0; i < results.size(); i++) {
                BigDecimal amount = BigDecimal.valueOf(20000 + 100000L * i);
                BigDecimal insurance = amount.multiply(new BigDecimal("0.04")).min(new BigDecimal("100000"));
                List<LoanOfferDto> requestOffers = results.get(i).get(10, TimeUnit.SECONDS);
                assertEquals(4, requestOffers.size());

                for (LoanOfferDto offer : requestOffers) {
                    BigDecimal expectedTotal = amount.add(offer.getIsInsuranceEnabled() ? insurance : BigDecimal.ZERO);
                    assertEquals(0, expectedTotal.compareTo(offer.getTotalAmount()),
                            "An offer must use insurance calculated for its own request");
                }
            }
        } finally {
            start.countDown();
            executor.shutdownNow();
        }
    }
}
