package core.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

@Builder(setterPrefix = "with")
public record CreditDto (
    @Schema(description = "Total amount payable, including principal and interest", example = "25000.00")
    @NotNull(message = "Credit amount is required")
    @DecimalMin(value = "20000.00", message = "Credit amount must be greater than or equal to 20,000")
    BigDecimal amount,

    @Schema(description = "Term of the credit in months", example = "24")
    @NotNull(message = "Credit term is required")
    @Min(value = 6, message = "Credit term must be at least 6 months")
    Integer term,

    @Schema(description = "Monthly payment", example = "1200.00")
    @NotNull(message = "Monthly payment is required")
    @DecimalMin(value = "0.01", message = "Monthly payment must be greater than zero")
    BigDecimal monthlyPayment,

    @Schema(description = "Interest rate of the credit", example = "5.5")
    @NotNull(message = "Interest rate is required")
    @DecimalMin(value = "0.01", message = "Interest rate must be greater than zero")
    BigDecimal rate,

    @Schema(description = "PSK (effective annual interest rate) for the credit", example = "6.5")
    @NotNull(message = "PSK is required")
    @DecimalMin(value = "0.01", message = "PSK must be greater than zero")
    BigDecimal psk,

    @Schema(description = "Is insurance enabled for the credit", example = "true")
    @NotNull(message = "Insurance enabled flag is required")
    Boolean isInsuranceEnabled,

    @Schema(description = "Is the applicant a salary client", example = "true")
    @NotNull(message = "Salary client flag is required")
    Boolean isSalaryClient,

    @Schema(description = "Payment schedule for the credit")
    @NotNull(message = "Payment schedule is required")
    List<PaymentScheduleElementDto> paymentSchedule
) implements Serializable {}
