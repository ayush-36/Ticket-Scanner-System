package com.example.ticket_scanner;

import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.util.Base64;

@Service
public class EmailService {

    @Autowired
    private JavaMailSender mailSender;

    /**
     * Sends an email with the QR code embedded in the email content.
     * @param toEmail recipient's email address
     * @param refernceId unique ticket ID
     * @param body message body to display above the QR code
     * @param qrBase64 base64 encoded QR image
     */
    public void sendEmailWithQRCode(String toEmail, String refernceId, String body, String qrBase64) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true);

            helper.setTo(toEmail);
            helper.setSubject("Your Event Ticket: " + refernceId);

            String htmlMsg = "<h3>" + body + "</h3><img src='cid:qrImage'>";
            helper.setText(htmlMsg, true);

            byte[] imageBytes = Base64.getDecoder().decode(qrBase64);
            ByteArrayResource byteArrayResource = new ByteArrayResource(imageBytes);

            helper.addInline("qrImage", byteArrayResource, "image/png");

            mailSender.send(message);

            System.out.println("Email sent successfully to " + toEmail);

        } catch (Exception e) {
            System.err.println("Failed to send email to " + toEmail);
            e.printStackTrace();
        }
    }
}
