package com.revhire.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    @Autowired
    private JavaMailSender mailSender;

    public void sendPasswordResetEmail(String to, String token) {
        String resetUrl = "http://localhost:8080/auth/reset-password?token=" + token;
        
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        message.setSubject("Password Reset Request - RevHire");
        message.setText("Dear User,\n\n" +
            "You have requested to reset your password. Click the link below to reset it:\n\n" +
            resetUrl + "\n\n" +
            "This link will expire in 24 hours.\n\n" +
            "If you did not request this, please ignore this email.\n\n" +
            "Thanks,\n" +
            "RevHire Team");
        
        mailSender.send(message);
    }
}