package core.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;

@Builder(setterPrefix = "with")
public record PaymentScheduleElementDto (
    @Schema(description = "Payment schedule element number", example = "1")
    @NotNull(message = "Payment number is required")
    @Min(value = 1, message = "Payment number must be greater than or equal to 1")
    Integer number,

    @Schema(description = "Date of the payment", example = "2023-06-15")
    @NotNull(message = "Payment date is required")
    LocalDate date,

    @Schema(description = "Total payment amount", example = "1500.00")
    @DecimalMin(value = "0.01", message = "Total payment must be greater than zero")
    BigDecimal totalPayment,

    @Schema(description = "Interest part of the payment", example = "500.00")
    @DecimalMin(value = "0.01", message = "Interest payment must be greater than zero")
    BigDecimal interestPayment,

    @Schema(description = "Principal part of the payment", example = "1000.00")
    @DecimalMin(value = "0.01", message = "Debt payment must be greater than zero")
    BigDecimal debtPayment,

    @Schema(description = "Remaining total of future payments, including principal and interest", example = "3000.00")
    @DecimalMin(value = "0.00", message = "Remaining debt cannot be negative")
    BigDecimal remainingDebt
) implements Serializable {}
