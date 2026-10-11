package core.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.Builder;
import core.type.ThemeType;
import java.io.Serializable;
import java.util.UUID;

@Builder(setterPrefix = "with")
public record EmailMessage(

        @Schema(description = "Recipient's email address", example = "example@domain.com")
        @NotNull(message = "Email address is required")
        @Email(message = "Invalid email address format")
        String address,

        @Schema(description = "Email theme", example = "Application Status", allowableValues = "Application Status, Offer, Rejection")
        @NotNull(message = "Email theme is required")
        ThemeType theme,

        @Schema(description = "Statement ID associated with this email", example = "123456")
        @NotNull(message = "Statement ID is required")
        UUID statementId,

        @Schema(description = "Text content of the email", example = "Your loan application has been approved.")
        @NotNull(message = "Email text is required")
        String text
) implements Serializable {
        @Override
        public String toString() {
                return "EmailMessage{theme=" + theme + ", statementId=" + statementId + '}';
        }
}
