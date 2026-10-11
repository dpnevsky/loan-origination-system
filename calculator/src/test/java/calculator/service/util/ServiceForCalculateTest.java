package calculator.service.util;

import core.dto.PaymentScheduleElementDto;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.stream.IntStream;
import static org.junit.jupiter.api.Assertions.*;

class ServiceForCalculateTest {

    @Test
    void calculateMonthlyPaymentValidInputsShouldReturnCorrectPayment() {
        BigDecimal totalAmount = BigDecimal.valueOf(200_000);
        BigDecimal rate = BigDecimal.valueOf(12);
        int term = 24;

        BigDecimal monthlyPayment = ServiceForCalculate.calculateMonthlyPayment(totalAmount, rate, term);

        assertNotNull(monthlyPayment);
        assertEquals(BigDecimal.valueOf(9415), monthlyPayment);
    }

    @Test
    void calculateInsurancePriceValidInputShouldReturnFourPercent() {
        BigDecimal amount = BigDecimal.valueOf(500_000);

        BigDecimal insurancePrice = ServiceForCalculate.calculateInsurancePrice(amount);

        assertNotNull(insurancePrice);
        assertEquals(BigDecimal.valueOf(20_000.0), insurancePrice.setScale(1, RoundingMode.HALF_EVEN));
    }

    @Test
    void calculateInsurancePriceExceedsMaxShouldCapAt100000() {
        BigDecimal amount = BigDecimal.valueOf(3_000_000);

        BigDecimal insurancePrice = ServiceForCalculate.calculateInsurancePrice(amount);

        assertNotNull(insurancePrice);
        assertEquals(BigDecimal.valueOf(100_000), insurancePrice);
    }

    @Test
    void calculatePSKValidInputsShouldReturnCorrectPSK() {
        BigDecimal totalAmount = BigDecimal.valueOf(300_000);
        BigDecimal monthlyPayment = BigDecimal.valueOf(14_122);
        int term = 24;
        List<PaymentScheduleElementDto> schedule = IntStream.range(0, term)
                .mapToObj(i -> PaymentScheduleElementDto.builder().withTotalPayment(monthlyPayment).build())
                .toList();

        BigDecimal psk = ServiceForCalculate.calculatePSK(totalAmount, schedule);

        assertNotNull(psk);
        assertTrue(psk.compareTo(BigDecimal.ZERO) > 0);
        assertEquals(BigDecimal.valueOf(12.0), psk.setScale(1, RoundingMode.HALF_EVEN));
    }

    @Test
    void calculatePSKUsesTheActualFinalPayment() {
        List<PaymentScheduleElementDto> schedule = List.of(
                PaymentScheduleElementDto.builder().withTotalPayment(new BigDecimal("500")).build(),
                PaymentScheduleElementDto.builder().withTotalPayment(new BigDecimal("600")).build());

        BigDecimal psk = ServiceForCalculate.calculatePSK(new BigDecimal("1000"), schedule);

        assertEquals(new BigDecimal("76.729"), psk);
    }
}
