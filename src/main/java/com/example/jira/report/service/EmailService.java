package com.example.jira.report.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

@Service
public class EmailService {

    private final JavaMailSender mailSender;
    @Value("${spring.mail.from}")
    private String from;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    /**
     * Sends an email with the given Excel report as an attachment.
     *
     * @param to recipient email
     * @param subject email subject
     * @param body plain-text body
     * @param excelBytes the .xlsx file content
     */
    public void sendReport(String to, String subject, String body, byte[] excelBytes)
            throws MessagingException
    {
        MimeMessage msg = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(msg, true, "UTF-8");
        helper.setFrom(from);
        helper.setTo(to);
        helper.setSubject(subject);
        helper.setText(body);

        // attach the Excel file
        helper.addAttachment("JiraReport.xlsx",
                new ByteArrayResource(excelBytes));

        mailSender.send(msg);
    }
}
