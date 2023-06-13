package com.ncc.kairos.moirai.zeus.services;

import com.ncc.kairos.moirai.zeus.dao.JwtUserRepository;
import com.ncc.kairos.moirai.zeus.model.JwtUser;
import com.ncc.kairos.moirai.zeus.utililty.PasswordUtil;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.transaction.Transactional;

/**
 * Service for managing user accounts.
 *
 * @author ryan scott
 * @version 0.1
 */
@Service
public class KairosUserService implements UserDetailsService {

    @Autowired
    private JwtUserRepository repository;

    @Value("${jwt.password.expire.days}")
    private int passwordExpiration;

    private final String taken = " is taken.";

    private static Map<String, String> accessCodes = new HashMap<>();

    /**
     * Searches stored users and finds the one matching the given username.
     *
     * @param username The username to match.
     * @return The matching jwt user.
     */
    @Transactional
    public JwtUser findUserByUsername(String username) {
        return this.repository.findByUsername(username);
    }

    @Transactional
    public JwtUser findUserById(String id) {
        Optional<JwtUser> user = this.repository.findById(id);
        return user.isPresent() ? user.get() : null;
    }

    @Override
    @Transactional
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        JwtUser jwtUser = this.repository.findByUsername(username);
        List<GrantedAuthority> authorities = jwtUser.getRoles().stream()
                .flatMap(role -> role.getPermissions().stream())
                .map(perm -> new SimpleGrantedAuthority(perm.getName()))
                .collect(Collectors.toList());

        LocalDate passwordExpiration = jwtUser.getPasswordExpiration();
        boolean passwordExpired = (passwordExpiration == null) ? true : jwtUser.getPasswordExpiration().isBefore(LocalDate.now());

        return User.withUsername(jwtUser.getUsername())
                .password(jwtUser.getPassword())
                .authorities(authorities)
                .accountLocked(!jwtUser.getActive())
                .credentialsExpired(passwordExpired)
                .build();
    }

    public List<JwtUser> findUserByTeamName(String teamName) {
        return this.repository.findByTeamName(teamName);
    }

    /**
     * Searches stored users and finds the one matching the given email address.
     *
     * @param email The email address to match.
     * @return The matching jwt user.
     */
    public JwtUser findUserByEmail(String email) {
        return this.repository.findByEmailAddress(email);
    }

    /**
     * Takes a jwtUser and adds it to the known users list.
     *
     * @param jwtUser The new jwtUser.
     */
    public void saveUserEncryptPassword(JwtUser jwtUser) {
        jwtUser.setPassword(PasswordUtil.getSecurePassword(jwtUser.getPassword()));
        this.repository.save(jwtUser);
    }

    /**
     * Returns all stored users.
     *
     * @return
     */
    public List<JwtUser> findAllUsers() {
        List<JwtUser> userList = new ArrayList<>();
        Iterable<JwtUser> it = repository.findAll();

        for (JwtUser user : it) {
            userList.add(user);
        }
        return userList;
    }

    /**
     * Updates an existing jwtUser.
     *
     * @param jwtUser The new jwtUser.
     */
    public void saveUser(JwtUser jwtUser) {
        this.repository.save(jwtUser);
    }

    /**
     * Updates the existingJwtUser with values stored in the newJwtSettings object.
     *
     * @param existingJwtUser The jwtUser that needs to be updates
     * @param newJwtSettings  Object containing the new values for the jwtUser
     * @return the updated user.
     */
    public JwtUser updateUser(JwtUser existingJwtUser, JwtUser newJwtSettings) {
        // TODO validate that the new username, password, email address, and team name
        // for length/chars/etc
        if (StringUtils.isNotEmpty(newJwtSettings.getEmailAddress())
                && !newJwtSettings.getEmailAddress().equals(existingJwtUser.getEmailAddress())) {
            assertUniqueEmailAddress(newJwtSettings.getEmailAddress());
            existingJwtUser.setEmailAddress(newJwtSettings.getEmailAddress());
        }

        if (StringUtils.isNotEmpty(newJwtSettings.getPassword())) {
            updatePassword(existingJwtUser.getUsername(), newJwtSettings.getPassword());
        }

        if (StringUtils.isNotEmpty(newJwtSettings.getPerformerGroup())) {
            existingJwtUser.setPerformerGroup(newJwtSettings.getPerformerGroup());
        }

        if (StringUtils.isNotEmpty(newJwtSettings.getTimezone())) {
            existingJwtUser.setTimezone(newJwtSettings.getTimezone());
        }

        existingJwtUser.profilePictureURL(newJwtSettings.getProfilePictureURL()).darkMode(newJwtSettings.getDarkMode());

        saveUser(existingJwtUser);
        return existingJwtUser;
    }

    /**
     * Sets a new password for an existing account.
     *
     * @param username    The account to set the password for.
     * @param newPassword The new password
     */
    public void updatePassword(String username, String newPassword) {
        JwtUser storedUser = findUserByUsername(username);
        if (storedUser.getPassword().equals(PasswordUtil.getSecurePassword(newPassword))) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Your new password cannot be the same as your last one.");
        }
        Calendar c = Calendar.getInstance();
        c.add(Calendar.DAY_OF_MONTH, passwordExpiration);
        storedUser.setPasswordExpiration(
                LocalDateTime.ofInstant(c.toInstant(), c.getTimeZone().toZoneId()).toLocalDate());
        storedUser.setPassword(PasswordUtil.getSecurePassword(newPassword));
        this.repository.save(storedUser);
        // Remove any accessCode associated with the account.
        for (String key : accessCodes.keySet()) {
            if (username.equals(accessCodes.get(key))) {
                accessCodes.remove(key);
            }
        }
    }

    /**
     * Throws an exception if a jwtUser exists with the given username.
     *
     * @param username the username to look for
     */
    public void assertUniqueUserName(String username) {
        JwtUser existingUser = findUserByUsername(username);
        if (existingUser != null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Error: Account name " + username + taken);
        }
    }

    /**
     * Throws an exception if jwtUser exists given teamname.
     * 
     * @param teamName teamName to look for
     */
    public void assertUniqueTeamName(String teamName) {
        List<JwtUser> existingUsers = findUserByTeamName(teamName);
        if (!existingUsers.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Error: Account team name " + teamName + taken);
        }
    }

    /**
     * Throws an exception if a jwtUser exists with the given email address.
     *
     * @param emailAddress the email address to look for
     */
    public void assertUniqueEmailAddress(String emailAddress) {
        JwtUser existingUser = findUserByEmail(emailAddress);
        if (existingUser != null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Error: Email address " + emailAddress + taken);
        }
    }

    /**
     * Generates a new temporary access code for the existing account to be used for
     * password reset.
     *
     * @param existingUser The user to generate the code for.
     * @return The generated code.
     */
    public String generateAccessCodeForAccount(JwtUser existingUser) {
        String generatedString = PasswordUtil.generateAccessCode();
        accessCodes.put(generatedString, existingUser.getUsername());
        return generatedString;
    }

    /**
     * Find the account associated with a previously generated access code.
     *
     * @param accessCode The code used to lookup the account.
     * @return The matching account.
     */
    public JwtUser getAccountForAccessCode(String accessCode) {
        String accountName = accessCodes.get(accessCode);
        return this.repository.findByUsername(accountName);
    }

    /**
     * Throws an exception if a jwtUser doesn't exist with the given username.
     *
     * @param username the username to look for
     */
    public void assertUserExists(String username) {
        JwtUser existingUser = findUserByUsername(username);
        if (existingUser == null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Error: Not a valid account");
        }
    }
}
