package com.ncc.kairos.moirai.zeus.services;

import com.ncc.kairos.moirai.zeus.dao.ZeusSettingRepository;
import com.ncc.kairos.moirai.zeus.resources.Constants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import javax.mail.internet.MimeMessage;
import java.util.Properties;

/**
 * Service handling emailing information to different users.
 *
 * @author ryan scott
 * @version 0.1
 */
@Service
public class EmailService {
    @Autowired
    ZeusSettingRepository zeusSettingRepository;

    /**
     * A generic method to send a email.
     *
     * @param recipient Who to send the email to.
     * @param subject The subject of the email.
     * @param text The body of the email.
     * @throws Exception Exception may result from the JavaMailSender class and is passed through.
     */
    public void sendSimpleMessage(String recipient, String subject, String text, String htmlText) throws Exception {
        JavaMailSender mailSender = getJavaMailSender();
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper;
        helper = new MimeMessageHelper(message, true);
        helper.setTo(recipient);
        helper.setSubject(subject);
        helper.setText(text, htmlText);
        mailSender.send(message);
    }

    protected JavaMailSender getJavaMailSender() {
        JavaMailSenderImpl mailSender = new JavaMailSenderImpl();

        mailSender.setHost("smtp.gmail.com");
        mailSender.setPort(587);
        mailSender.setUsername(zeusSettingRepository.findByName(Constants.ZS_MAIL_USERNAME).getValue());
        mailSender.setPassword(zeusSettingRepository.findByName(Constants.ZS_MAIL_PASSWORD).getValue());

        Properties props = mailSender.getJavaMailProperties();
        props.put("mail.transport.protocol", "smtp");
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.debug", "true");

        return mailSender;
    }
}
