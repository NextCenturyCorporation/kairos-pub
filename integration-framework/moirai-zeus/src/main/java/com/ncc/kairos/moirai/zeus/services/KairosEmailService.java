package com.ncc.kairos.moirai.zeus.services;

import org.springframework.stereotype.Service;

/**
 * Service handling emailing information to different users.
 *
 * @author ryan scott
 * @version 0.1
 */
@Service
public class KairosEmailService extends EmailService {

    //Headers
    private static String forgotUsernameHeader = "KAIROS Support - Forgot Username";
    private static String forgotPasswordHeader = "KAIROS Support - Forgot Password";

    //BodyTemplates
    private static String forgotUsernameBodyTemplate = "Your KAIROS username is %s.";
    private static String forgotPasswordBodyTemplate = "Your requested a password reset for your KAIROS account.\nUse the following access code to reset your password.\n\n\t  Access code: %s";

    /**
     * Sends an email to the given recipient containing the username.
     *
     * @param recipient The email to send the email to.
     * @param username The username of the account associated with the email.
     * @throws Exception Any error during transmission of the email.
     */
    public void sendForgotUsernameMessage(String recipient, String username) throws Exception {
        String body = String.format(forgotUsernameBodyTemplate, username);
        this.sendSimpleMessage(recipient, forgotUsernameHeader, "", body);
    }

    /**
     * Sends an email to the given recipient containing a generated accesscode.
     *
     * @param recipient The email to send the email to.
     * @param accessCode The access code granted to the account.
     * @throws Exception Any error during transmission of the email.
     */
    public void sendForgotPasswordMessage(String recipient, String accessCode) throws Exception {
        String body = String.format(forgotPasswordBodyTemplate, accessCode);
        this.sendSimpleMessage(recipient, forgotPasswordHeader, "", body);
    }
}
