package com.ncc.kairos.moirai.zeus.services;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import com.ncc.kairos.moirai.zeus.dao.JwtUserRepository;
import com.ncc.kairos.moirai.zeus.model.JwtRole;
import com.ncc.kairos.moirai.zeus.model.JwtUser;
import com.ncc.kairos.moirai.zeus.utililty.PasswordUtil;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

@TestPropertySource(locations = "classpath:test.properties")
public class KairosUserServiceTest {

    @Autowired
    KairosUserService kairosUserService;

    @MockBean
    JwtUserRepository mockJwtUserRepository;

    private JwtUser jwtUser;

    private List<JwtRole> testRoles = new ArrayList<>();

    private String rawPassword = "password1234";

    void updateReflections() {
        ReflectionTestUtils.setField(kairosUserService, "repository", mockJwtUserRepository);
    }

    @BeforeEach
    public void prepareUnitTests() {
        testRoles.add(new JwtRole().description("test1").name("test1").id("aewaedaw").permissions(new ArrayList<>()));
        testRoles.add(new JwtRole().description("test2").name("test2").id("aewaedaw2").permissions(new ArrayList<>()));
        jwtUser = new JwtUser()
                .teamName("Moirai")
                .password(PasswordUtil.getSecurePassword(rawPassword))
                .passwordExpiration(LocalDateTime.ofInstant(validPasswordExpiration().toInstant(), validPasswordExpiration().getTimeZone().toZoneId()).toLocalDate())
                .username("John")
                .emailAddress("John@Test.com")
                .active(false)
                .roles(testRoles);
        updateReflections();
    }

    @Test
    public void findUserByUsernameTest() {
        Mockito.when(mockJwtUserRepository.findByUsername(jwtUser.getUsername())).thenReturn(jwtUser);
        kairosUserService.findUserByUsername(jwtUser.getUsername());
        verify(mockJwtUserRepository, times(1)).findByUsername(jwtUser.getUsername());
    }

    @Test
    public void loadUserByUsernameTest() {
        Mockito.when(mockJwtUserRepository.findByUsername(jwtUser.getUsername())).thenReturn(jwtUser);
        UserDetails returnedDetails = kairosUserService.loadUserByUsername(jwtUser.getUsername());
        assert (returnedDetails.isCredentialsNonExpired());
    }

    @Test
    public void findUserByTeamNameTest() {
        List<JwtUser> returnThis = new ArrayList<>();
        returnThis.add(jwtUser);
        Mockito.when(mockJwtUserRepository.findByTeamName(jwtUser.getTeamName())).thenReturn(returnThis);
        kairosUserService.findUserByTeamName(jwtUser.getTeamName());
        verify(mockJwtUserRepository, times(1)).findByTeamName(jwtUser.getTeamName());
    }

    @Test
    public void findUserByEmailTest() {
        Mockito.when(mockJwtUserRepository.findByEmailAddress(jwtUser.getEmailAddress())).thenReturn(jwtUser);
        kairosUserService.findUserByEmail(jwtUser.getEmailAddress());
        verify(mockJwtUserRepository, times(1)).findByEmailAddress(jwtUser.getEmailAddress());
    }

    @Test
    public void saveUserEncryptPasswordTest() {
        Mockito.when(mockJwtUserRepository.save(jwtUser)).thenReturn(jwtUser);
        kairosUserService.saveUserEncryptPassword(jwtUser);
        verify(mockJwtUserRepository, times(1)).save(jwtUser);
    }

    @Test
    public void saveUserTest() {
        Mockito.when(mockJwtUserRepository.save(jwtUser)).thenReturn(jwtUser);
        kairosUserService.saveUser(jwtUser);
        verify(mockJwtUserRepository, times(1)).save(jwtUser);
    }

    @Test
    public void findAllUsersTest() {
        List<JwtUser> returnThis = new ArrayList<>();
        returnThis.add(jwtUser);
        Iterable<JwtUser> it = returnThis;
        Mockito.when(mockJwtUserRepository.findAll()).thenReturn(it);
        List<JwtUser> result = kairosUserService.findAllUsers();
        assert (!result.isEmpty());
    }

    @Test
    public void updateUserTest() {
        Mockito.when(mockJwtUserRepository.findByUsername(jwtUser.getUsername())).thenReturn(jwtUser);
        kairosUserService.updateUser(jwtUser, jwtUser);
        verify(mockJwtUserRepository, times(2)).save(jwtUser);
    }

    @Test
    public void updatePasswordTest() {
        Mockito.when(mockJwtUserRepository.findByUsername(jwtUser.getUsername())).thenReturn(jwtUser);
        Mockito.when(mockJwtUserRepository.save(jwtUser)).thenReturn(jwtUser);
        kairosUserService.updatePassword(jwtUser.getUsername(), "newPassword");
        verify(mockJwtUserRepository, times(1)).save(jwtUser);
        
        // Throw error for same password
        // assertThrows(ResponseStatusException.class, () -> {
        //     kairosUserService.updatePassword(jwtUser.getUsername(), rawPassword);
        // });

    }

    @Test
    public void assertUniqueUserNameTest() {
        Mockito.when(mockJwtUserRepository.findByUsername(jwtUser.getUsername())).thenReturn(jwtUser);
        // Should throw error since user exists
        assertThrows(ResponseStatusException.class, () -> {
            kairosUserService.assertUniqueUserName(jwtUser.getUsername());
        });
        // No Error
        kairosUserService.assertUniqueUserName("uniqueUsername");
    }

    @Test
    public void assertUniqueTeamNameTest() {
        List<JwtUser> returnThis = new ArrayList<>();
        returnThis.add(jwtUser);
        Mockito.when(mockJwtUserRepository.findByTeamName(jwtUser.getTeamName())).thenReturn(returnThis);
        // Should throw error since user exists
        assertThrows(ResponseStatusException.class, () -> {
            kairosUserService.assertUniqueTeamName(jwtUser.getTeamName());
        });
        // No error
        kairosUserService.assertUniqueTeamName("uniqueTeamName");
    }

    @Test
    public void assertUniqueEmailAddressTest() {
        Mockito.when(mockJwtUserRepository.findByEmailAddress(jwtUser.getEmailAddress())).thenReturn(jwtUser);
        // No error
        kairosUserService.assertUniqueEmailAddress("uniqueEmail");
        // Should throw error since user exists
        assertThrows(ResponseStatusException.class, () -> {
            kairosUserService.assertUniqueEmailAddress(jwtUser.getEmailAddress());
        });
    }

    @Test
    public void generateAccessCodeForAccountTest() {
        String returned = kairosUserService.generateAccessCodeForAccount(jwtUser);
        assert (!returned.isEmpty());
    }

    @Test
    public void getAccountForAccessCodeTest() {
        Mockito.when(mockJwtUserRepository.findByUsername(jwtUser.getUsername())).thenReturn(jwtUser);
        String accessCode = kairosUserService.generateAccessCodeForAccount(jwtUser);
        assert (!accessCode.isEmpty());
        JwtUser getUserFromAccessCode = kairosUserService.getAccountForAccessCode(accessCode);
        assert (getUserFromAccessCode.equals(jwtUser));

    }

    @Test
    public void assertUserExistsTest() {
        Mockito.when(mockJwtUserRepository.findByUsername(jwtUser.getUsername())).thenReturn(jwtUser);
        // No error
        kairosUserService.assertUserExists(jwtUser.getUsername());

        // error
        assertThrows(ResponseStatusException.class, () -> {
            kairosUserService.assertUserExists("wrong name");
        });
    }


    // Create a always valid password expiration date
    private Calendar validPasswordExpiration() {
        Calendar c = Calendar.getInstance();
        c.add(Calendar.DAY_OF_MONTH, 30);
        return c;
    }
    
    
}
