package dossier.service.impl;

import core.dto.EmailMessage;
import dossier.service.DossierKafkaConsumerService;
import dossier.service.util.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class DossierKafkaConsumerServiceImpl implements DossierKafkaConsumerService {

    private final EmailService emailService;

    @Override
    @KafkaListener(
            topics = {"finish-registration", "create-documents", "send-documents",
                    "send-ses", "credit-issued", "statement-denied"},
            groupId = "dossier-service-group"
    )
    public void consumeMessage(EmailMessage message) {
        log.info("Received message from Kafka: {}", message);
        try {
            log.debug("Preparing to send email to: {}, with theme: {}", message.address(), message.theme().name());
            emailService.sendEmail(message.address(), message.theme().name(), message.text());
        } catch (Exception e) {
            log.error("Error while processing message: {}, Error type: {}", message, e.getClass().getSimpleName());
        }
    }
}