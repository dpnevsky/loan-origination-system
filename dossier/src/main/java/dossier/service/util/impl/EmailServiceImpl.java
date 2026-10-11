package dossier.service.util.impl;

import dossier.service.util.EmailService;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailServiceImpl implements EmailService {

    @Value("${emailFrom}")
    private String emailFrom;

    private final JavaMailSender mailSender;

    @Override
    public void sendEmail(String to, String subject, String body) throws MessagingException {
        log.info("Preparing to send email...");
        log.debug("Email details - To: {}, Subject: {}", to, subject);

        if (to == null || to.isBlank()) {
            log.error("Recipient email address is null or empty.");
            throw new IllegalArgumentException("Recipient email address must not be null or empty.");
        }
        if (subject == null || subject.isBlank()) {
            log.error("Email subject is null or empty.");
            throw new IllegalArgumentException("Email subject must not be null or empty.");
        }
        if (body == null || body.isBlank()) {
            log.error("Email body is null or empty.");
            throw new IllegalArgumentException("Email body must not be null or empty.");
        }

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, false);

            helper.setTo(to);
            helper.setFrom(emailFrom);
            helper.setSubject(subject);
            helper.setText(body, false);

            log.debug("Sending email from: {}", emailFrom);
            mailSender.send(message);
            log.info("Email successfully sent to: {}", to);
        } catch (MessagingException e) {
            log.error("Failed to send email to: {}, Error type: {}", to, e.getClass().getSimpleName());
            throw e;
        }
    }
}
