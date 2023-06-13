package com.ncc.kairos.moirai.zeus.jobs;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.LocalDate;
import java.util.Calendar;
import java.util.List;
import java.util.stream.Collectors;

import javax.validation.Valid;

import com.ncc.kairos.moirai.zeus.model.JwtUser;
import com.ncc.kairos.moirai.zeus.services.EmailService;
import com.ncc.kairos.moirai.zeus.services.KairosUserService;

@Component
public class PasswordExpirationNotificationJob {

    @Autowired
    private EmailService emailService;

    @Autowired
    private KairosUserService kairosUserService;

    @Value("${jwt.password.email.notification.days}")
    private int passwordEmailNotification;

    @Value("${environmentTier}")
    private String environment;

    private static final Logger LOGGER = LoggerFactory.getLogger(PasswordExpirationNotificationJob.class);

    // "0 */2 * * * *" For Testing (Every 2 minutes)
    // "0 0 7 */1 * *" everyday at 7am
    @Scheduled(cron = "0 0 7 */1 * *")
    public void mainCronJob() throws Exception {
        LOGGER.info("Kicking Off Email Expiration Job");
        Calendar c = Calendar.getInstance();
        c.add(Calendar.DAY_OF_MONTH, passwordEmailNotification);
        // Get all users
        List<JwtUser> userList = this.kairosUserService.findAllUsers();
        List<JwtUser> expiringUsers = userList
                .stream().filter(
                        user -> user.getPasswordExpiration() != null
                                && user.getPasswordExpiration()
                                        .isBefore(LocalDateTime.ofInstant(c.toInstant(), c.getTimeZone().toZoneId())
                                                .toLocalDate())
                                && user.getPasswordExpiration()
                                        .isAfter(LocalDateTime.ofInstant(Calendar.getInstance().toInstant(),
                                                c.getTimeZone().toZoneId()).toLocalDate()))
                .collect(Collectors.toList());
        if (environment.equals("production")) {
            for (JwtUser user : expiringUsers) {
                Long daysTilExpiration = getDaysUntilExpiration(user.getPasswordExpiration());
                emailService.sendSimpleMessage(user.getEmailAddress(), 
                "Moirai Password Expiration Notification (" + daysTilExpiration + ") Days", 
                "", // Plain text field not needed
                "Hello " + user.getUsername() + ",<br/> Your password for MOIRAI is about to expire in " + daysTilExpiration
                + " day(s).<br/> Please login at <a href=\"https://zeus.kairos.nextcentury.com/\">https://zeus.kairos.nextcentury.com/</a> and reset your password under the 'Settings' menu option."
                + "<br/><b>-Moirai User Services</b>");
                LOGGER.info("USERS PASSWORD ABOUT TO EXPIRE   :  " + user.getUsername() + " in " + daysTilExpiration + " days: Sending email to " + user.getEmailAddress());
            }
        }
    }

    private long getDaysUntilExpiration(java.time.@Valid LocalDate passwordExpiration) {
        return Duration.between(LocalDate.now().atStartOfDay(), passwordExpiration.atStartOfDay()).toDays();
    }
}
