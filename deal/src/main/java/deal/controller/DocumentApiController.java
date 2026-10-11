package deal.controller;

import core.dto.EmailMessage;
import core.type.KafkaTopicNameType;
import core.type.ThemeType;
import deal.client.DealKafkaProducerClientService;
import deal.service.ClientService;
import deal.service.LoanStatementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.UUID;
import static core.type.ApplicationStatusType.CREDIT_ISSUED;
import static core.type.ApplicationStatusType.DOCUMENT_SIGNED;
import static core.type.ApplicationStatusType.PREPARE_DOCUMENTS;
import static core.type.KafkaTopicNameType.SEND_DOCUMENTS;
import static core.type.KafkaTopicNameType.SEND_SES;

@Tag(name = "Document API Controller", description = "API for working with deal documents")
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/deal/document/{statementId}")
public class DocumentApiController {

    private final LoanStatementService loanStatementService;
    private final ClientService clientService;
    private final DealKafkaProducerClientService dealKafkaProducerClientService;

    @Operation(summary = "Request to send documents")
    @PostMapping("/send")
    public void sendDocument(@PathVariable(name = "statementId") UUID statementId) {
        log.info("Processing request to send documents for statementId: {}", statementId);
        loanStatementService.updateStatementStatus(statementId, PREPARE_DOCUMENTS);
        log.debug("Statement status updated to PREPARE_DOCUMENTS for statementId: {}", statementId);
        String email = clientService.getEmailByStatementId(statementId);
        log.debug("Retrieved email address: {}", email);
        EmailMessage emailMessage = EmailMessage.builder()
                .withAddress(email)
                .withTheme(ThemeType.APPLICATION_STATUS)
                .withStatementId(statementId)
                .withText("Documents. \n Agree with the conditions.")
                .build();
        log.info("Sending document email: {}", emailMessage);
        dealKafkaProducerClientService.sendDocuments(SEND_DOCUMENTS, emailMessage);
    }

    @Operation(summary = "Request to sign documents")
    @PostMapping("/sign")
    public void signDocument(@PathVariable(name = "statementId") UUID statementId) {
        log.info("Processing request to sign documents for statementId: {}", statementId);
        UUID sesCode = UUID.randomUUID();
        loanStatementService.setSesCode(statementId, sesCode);
        log.debug("SES code set for statementId: {}", statementId);
        String email = clientService.getEmailByStatementId(statementId);
        log.debug("Retrieved email address: {}", email);
        EmailMessage emailMessage = EmailMessage.builder()
                .withAddress(email)
                .withTheme(ThemeType.OFFER)
                .withStatementId(statementId)
                .withText("Ses code: " + sesCode + "\n Sign documents.")
                .build();
        log.info("Sending SES code email: {}", emailMessage);
        dealKafkaProducerClientService.sendDocuments(SEND_SES, emailMessage);
    }

    @Operation(summary = "Sign documents")
    @PostMapping("/code")
    public void codeDocument(@PathVariable(name = "statementId") UUID statementId,
                             @RequestBody UUID sesCode) {
        log.info("Processing SES code verification for statementId: {}", statementId);
        UUID expectedSesCode = loanStatementService.getSesCodeByStatementId(statementId);
        if (sesCode.equals(expectedSesCode)) {
            log.info("SES code verified successfully for statementId: {}", statementId);
            loanStatementService.updateStatementStatus(statementId, DOCUMENT_SIGNED);
            log.debug("Statement status updated to DOCUMENT_SIGNED for statementId: {}", statementId);
            loanStatementService.updateStatementStatus(statementId, CREDIT_ISSUED);
            log.debug("Statement status updated to CREDIT_ISSUED for statementId: {}", statementId);
            String email = clientService.getEmailByStatementId(statementId);
            log.debug("Retrieved email address: {}", email);
            EmailMessage emailMessage = EmailMessage.builder()
                    .withAddress(email)
                    .withTheme(ThemeType.APPLICATION_STATUS)
                    .withStatementId(statementId)
                    .withText("Credit issued.")
                    .build();
            log.info("Sending credit issued email: {}", emailMessage);
            dealKafkaProducerClientService.sendDocuments(KafkaTopicNameType.CREDIT_ISSUED, emailMessage);
        } else {
            log.warn("Invalid SES code provided for statementId: {}", statementId);
        }
    }
}
