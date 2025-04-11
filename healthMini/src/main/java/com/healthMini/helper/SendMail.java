package com.healthMini.helper;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Random;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import com.healthMini.entityDto.HealthCareUser;
import com.healthMini.repository.HealthCareUserRepository;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

@Service
public class SendMail {

    @Autowired
    private JavaMailSender mailSender;

    public int generateOTP() {
        return new Random().nextInt(900000) + 100000;
    }

    public void sendEmail(HealthCareUser user, String subject, String templateName) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true);

            helper.setFrom("planotechevents24@gmail.com", "HealthCare App");
            helper.setTo(user.getEmail());
            helper.setSubject(subject);

            // Load and customize the email template
            String htmlBody = readHtmlTemplate(templateName);
            htmlBody = htmlBody.replace("{{USERNAME}}", user.getFullName());

            if (htmlBody.contains("{{OTP}}")) {
                htmlBody = htmlBody.replace("{{OTP}}", String.valueOf(user.getOtp()));
            }

            helper.setText(htmlBody, true);

            // Send Email
            mailSender.send(message);
        } catch (MessagingException | IOException e) {
            throw new RuntimeException("Error sending email: " + e.getMessage());
        }
    }

    private String readHtmlTemplate(String templateName) throws IOException {
        ClassPathResource resource = new ClassPathResource("templates/" + templateName);
        try (InputStream inputStream = resource.getInputStream()) {
            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}




