package calculator.service.util;

import lombok.extern.slf4j.Slf4j;
import core.dto.PaymentScheduleElementDto;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.stream.IntStream;
import static calculator.service.util.Constant.INSURANCE_PRICE_IN_PERCENT;
import static calculator.service.util.Constant.MAX_INSURANCE_PRICE;
import static calculator.service.util.Constant.ONE_HUNDRED_PERCENT;
import static calculator.service.util.Constant.TOTAL_AMOUNT_AS_FIRST_PAYMENT;
import static calculator.service.util.Constant.TWELVE_MONTH;
import static org.apache.poi.ss.formula.functions.Irr.irr;

@Slf4j
public class ServiceForCalculate {

    /**
     * Рассчитывает аннуитетный платеж на основе суммы кредита, процентной ставки и срока.
     * Формула расчета аннуитетного платежа Х = С * К
     * где X — аннуитетный платеж, С — сумма кредита, К — коэффициент аннуитета.
     * Коэффициент аннуитета считается:
     * К = (М * (1 + М) ^ S) / ((1 + М) ^ S — 1)
     * где М — месячная процентная ставка по кредиту, S — срок кредита в месяцах.
     *
     * @param totalAmount общая сумма кредита
     * @param rate        годовая процентная ставка
     * @param term        срок кредита в месяцах
     * @return аннуитетный платеж
     */
    public static BigDecimal calculateMonthlyPayment(BigDecimal totalAmount, BigDecimal rate, Integer term) {

        log.debug("Calculating monthly payment with TotalAmount: {}, Rate: {}, Term: {}", totalAmount, rate, term);

        BigDecimal monthlyRate = rate.divide(ONE_HUNDRED_PERCENT.multiply(TWELVE_MONTH), 10, RoundingMode.HALF_EVEN);
        BigDecimal pow = BigDecimal.ONE.add(monthlyRate).pow(term);
        BigDecimal annuityCoefficient = monthlyRate.multiply(pow).divide(pow.subtract(BigDecimal.ONE), 10, RoundingMode.HALF_EVEN);
        BigDecimal monthlyPayment = totalAmount.multiply(annuityCoefficient).setScale(0, RoundingMode.HALF_EVEN);

        log.debug("Calculated monthly payment: {}", monthlyPayment);
        return monthlyPayment;
    }

    /**
     * Рассчитывает стоимость страховки.
     * Стоимость страховки равняется 4%, но не больше 100_000.
     *
     * @param amount запрашиваемая сумма кредита
     * @return стоимость страховки
     */
    public static BigDecimal calculateInsurancePrice(BigDecimal amount){
        log.debug("Calculating insurance price with amount: {}", amount);
        BigDecimal insurancePrice = amount.multiply(INSURANCE_PRICE_IN_PERCENT);
        if(insurancePrice.compareTo(MAX_INSURANCE_PRICE) > 0) {
            insurancePrice = MAX_INSURANCE_PRICE;
        }
        log.debug("Calculated insurance price: {}", insurancePrice);
        return insurancePrice;
    }

    /**
     * В соответствии со статьёй 6 Федерального закона «О потребительском кредите (займе)» 353-ФЗ
     * Полная стоимость потребительского кредита (займа) определяется в процентах годовых по формуле:
     * ПСК = i x ЧБП x 100, где ПСК — полная стоимость кредита в процентах годовых с точностью до
     * третьего знака после запятой;
     * ЧБП — число базовых периодов в календарном году. Продолжительность календарного года признаётся
     * равной трёмстам шестидесяти пяти дням;
     * i — процентная ставка базового периода, выраженная в десятичной форме.
     */
    public static BigDecimal calculatePSK(BigDecimal totalAmount, List<PaymentScheduleElementDto> paymentSchedule) {
        int term = paymentSchedule.size();
        log.debug("Calculating PSK with TotalAmount: {}, Payment count: {}", totalAmount, term);

        double[] cashFlows = new double[term + TOTAL_AMOUNT_AS_FIRST_PAYMENT];
        cashFlows[0] = totalAmount.multiply(BigDecimal.valueOf(-1)).doubleValue();
        IntStream.range(0, term)
                .forEach(i -> cashFlows[i + TOTAL_AMOUNT_AS_FIRST_PAYMENT] = paymentSchedule.get(i).totalPayment().doubleValue());

        BigDecimal basePeriodInterestRate = BigDecimal.valueOf(irr(cashFlows));
        BigDecimal psk = basePeriodInterestRate.multiply(ONE_HUNDRED_PERCENT.multiply(TWELVE_MONTH)).setScale(3,RoundingMode.HALF_UP);
        log.debug("Calculated PSK: {}", psk);
        return psk;
    }
}
