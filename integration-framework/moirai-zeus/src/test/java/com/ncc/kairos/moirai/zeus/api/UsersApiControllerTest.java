package com.ncc.kairos.moirai.zeus.api;

import com.ncc.kairos.moirai.zeus.ZeusApplication;
import com.ncc.kairos.moirai.zeus.dao.JwtPermissionRepository;
import com.ncc.kairos.moirai.zeus.dao.JwtRoleRepository;
import com.ncc.kairos.moirai.zeus.dao.ZeusSettingRepository;
import com.ncc.kairos.moirai.zeus.model.*;
import com.ncc.kairos.moirai.zeus.security.utils.JwtUtils;
import com.ncc.kairos.moirai.zeus.services.ContactRequestService;
import com.ncc.kairos.moirai.zeus.services.KairosEmailService;
import com.ncc.kairos.moirai.zeus.services.KairosUserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.Authentication;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.server.ResponseStatusException;

import javax.servlet.http.HttpServletResponse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@ExtendWith(SpringExtension.class)
@TestPropertySource(locations = "classpath:test.properties")
class UsersApiControllerTest { //extends Specification == Hey every method here is a spock test.
    UsersApiController usersApiController;
    JwtUser jwtUser;
    KairosEmailService mockKairosEmailService;
    KairosUserService mockKairosUserService;
    HttpServletResponse mockHttpServletResponse;
    NativeWebRequest mockNativeWebRequest;
    JwtUtils mockJwtUtils;

    @MockBean
    JwtPermissionRepository jwtPermissionRepo;

    @MockBean
    JwtRoleRepository jwtRoleRepo;

    @MockBean
    ZeusSettingRepository zeusSettingRepo;

    @Mock
    AuthenticationManager mockAuthenticationManager;

    @Mock
    ContactRequestService contactRequestService;

    private JwtPermission testPerm;

    private JwtRole testRole;

    void updateReflections() {
        ReflectionTestUtils.setField(usersApiController, "kairosEmailService", mockKairosEmailService);
        ReflectionTestUtils.setField(usersApiController, "kairosUserService", mockKairosUserService);
        ReflectionTestUtils.setField(usersApiController, "request", mockNativeWebRequest);
        ReflectionTestUtils.setField(usersApiController, "jwtUtils", mockJwtUtils);
        ReflectionTestUtils.setField(usersApiController, "jwtPermissionRepo", jwtPermissionRepo);
        ReflectionTestUtils.setField(usersApiController, "jwtRoleRepo", jwtRoleRepo);
        ReflectionTestUtils.setField(usersApiController, "zeusSettingRepo", zeusSettingRepo);
        ReflectionTestUtils.setField(usersApiController, "authenticationManager", mockAuthenticationManager);
        ReflectionTestUtils.setField(usersApiController, "contactRequestService", contactRequestService);
    }

    @BeforeEach
    void setup() {
        testPerm = new JwtPermission().description("soawda").id(UUID.randomUUID().toString()).name("coolNameBro");
        testRole = new JwtRole().description("Cool Role Bro").id(UUID.randomUUID().toString()).name("Cooler Name Bro").addPermissionsItem(testPerm);
        jwtUser = new JwtUser().username("John").emailAddress("John@Test.com").active(true).addRolesItem(testRole);
        mockKairosUserService = Mockito.mock(KairosUserService.class);
        mockNativeWebRequest = Mockito.mock(NativeWebRequest.class);
        mockHttpServletResponse = Mockito.mock(HttpServletResponse.class);
        mockKairosEmailService = Mockito.mock(KairosEmailService.class);
        mockJwtUtils = Mockito.mock(JwtUtils.class);

        Mockito.when(mockNativeWebRequest.getAttribute("token", 0)).thenReturn("token");
        Mockito.when(mockNativeWebRequest.getAttribute("jwtUser", 0)).thenReturn(this.jwtUser);
        Mockito.when(mockKairosUserService.findUserByUsername(jwtUser.getUsername())).thenReturn(jwtUser);
        Mockito.when(mockKairosUserService.generateAccessCodeForAccount(any(JwtUser.class))).thenReturn("granted");
        Mockito.when(mockJwtUtils.generateJwtToken(any(Authentication.class))).thenReturn("");

        usersApiController = new UsersApiController(mockNativeWebRequest);
        updateReflections();
    }

    @Test
    void getRequestTest() {
        Optional<NativeWebRequest> result = usersApiController.getRequest();
        assert result.get() != null;
    }

    @Test
    void registerTest() {
        RegistrationDto dto = new RegistrationDto().emailAddress("new.email@address.com").password("someSecurePWD").performerGroup("TA1").teamName("greatest");
        ZeusSetting registrationAllowed = new ZeusSetting().value("TRUE");
        Mockito.when(jwtRoleRepo.findByName(any())).thenReturn(testRole);
        Mockito.when(zeusSettingRepo.findByName(any())).thenReturn(registrationAllowed);
        Mockito.doNothing().when(mockKairosUserService).saveUserEncryptPassword(any());

        verify(mockKairosUserService, times(0)).saveUserEncryptPassword(any());
        ResponseEntity<StringResponse> response1 = usersApiController.register(dto);
        verify(mockKairosUserService, times(1)).saveUserEncryptPassword(any());
        assert response1.getStatusCode() == HttpStatus.CREATED;


        Mockito.when(jwtRoleRepo.save(any())).thenReturn(testRole);
        Mockito.when(jwtRoleRepo.findByName(any())).thenReturn(null);
        Mockito.when(jwtPermissionRepo.findByName(any())).thenReturn(testPerm);
        response1 = usersApiController.register(dto);
        verify(jwtRoleRepo, times(1)).save(any());
    }

    @Test
    void updateUserSettingsTest() {
        //setup
        SettingsDto dto = new SettingsDto().currentPassword("awdasawd").darkMode(true).emailAddress("woopwoop").id(UUID.randomUUID().toString()).username("importante");
        
        Authentication authentication = Mockito.mock(Authentication.class);
        Mockito.when(mockAuthenticationManager.authenticate(any())).thenReturn(authentication);

        Mockito.when(mockKairosUserService.updateUser(any(JwtUser.class), any(JwtUser.class))).thenReturn(jwtUser);

        //test
        ResponseEntity<StringResponse> response1 = usersApiController.updateUserSettings(dto);
        verify(mockKairosUserService, times(1)).updateUser(any(), any());
        assert response1.getStatusCode() == HttpStatus.OK;
    }

    @Test
    void getUserAccountTest() {
        ResponseEntity<JwtUser> response1 = usersApiController.getUserAccount();
        assert response1.getBody().getPassword().isBlank();
        assert response1.getStatusCode() == HttpStatus.OK;
    }

    @Test
    void authenticateTest() {
        Authentication authentication = Mockito.mock(Authentication.class);
        Mockito.when(mockAuthenticationManager.authenticate(any())).thenReturn(authentication);
        ZeusLoginRequest request = new ZeusLoginRequest().password("test").username("surname");

        ResponseEntity<StringResponse> response1 = usersApiController.authenticate(request);
        assert response1.getStatusCode() == HttpStatus.OK;
    }

    @Test
    void forgotUsernameTest() throws Exception {
        StringRequest test = new StringRequest().value(jwtUser.getEmailAddress());
        Mockito.doNothing().when(mockKairosEmailService).sendForgotUsernameMessage(anyString(), anyString());

        Mockito.when(mockKairosUserService.findUserByEmail(any())).thenReturn(null);
        ResponseEntity<StringResponse> response1 = usersApiController.forgotUsername(test);
        verify(mockKairosEmailService, times(0)).sendForgotUsernameMessage(anyString(), anyString());
        assert response1.getStatusCode() == HttpStatus.NOT_FOUND;

        Mockito.when(mockKairosUserService.findUserByEmail(any())).thenReturn(jwtUser);
        response1 = usersApiController.forgotUsername(test);
        verify(mockKairosEmailService, times(1)).sendForgotUsernameMessage(anyString(), anyString());
        assert response1.getStatusCode() == HttpStatus.OK;

        Mockito.doThrow(ResponseStatusException.class).when(mockKairosEmailService).sendForgotUsernameMessage(anyString(), anyString());
        response1 = usersApiController.forgotUsername(test);
        assert response1.getStatusCode() == HttpStatus.INTERNAL_SERVER_ERROR;
    }

    @Test
    void testForgotPasswordChecksExistingUser() {
        StringRequest validUsernameRequest = new StringRequest().value(jwtUser.getUsername());
        StringRequest invalidUsernameRequest = new StringRequest().value("invalid");

        Mockito.when(mockKairosUserService.generateAccessCodeForAccount(any(JwtUser.class))).thenReturn("granted");

        ResponseEntity<StringResponse> response1 = usersApiController.resetPassword(invalidUsernameRequest);
        assert response1.getStatusCode() == HttpStatus.NOT_FOUND;

        ResponseEntity<StringResponse> response2 = usersApiController.forgotPassword(validUsernameRequest);
        assert response2.getStatusCode() == HttpStatus.OK;

        Mockito.when(mockKairosUserService.findUserByUsername(jwtUser.getUsername())).thenReturn(null);
        response2 = usersApiController.forgotPassword(validUsernameRequest);
        assert response2.getStatusCode() == HttpStatus.NOT_FOUND;
    }

    @Test
    void testForgotPasswordSendsEmailOnValidUser() throws Exception {
        StringRequest validUsernameRequest = new StringRequest().value(jwtUser.getUsername());
        StringRequest invalidUsernameRequest = new StringRequest().value("invalid");

        Mockito.when(mockKairosUserService.generateAccessCodeForAccount(any(JwtUser.class))).thenReturn("granted");

        usersApiController.resetPassword(invalidUsernameRequest);
        verify(mockKairosEmailService, times(0)).sendForgotPasswordMessage(anyString(), anyString());

        usersApiController.forgotPassword(validUsernameRequest);
        verify(mockKairosEmailService).sendForgotPasswordMessage(anyString(), anyString());
    }

    @Test
    void testResetPasswordCheckForAssociatedAccount() {
        String validCode = "valid";
        String invalidCode = "invalid";
        StringRequest validPasswordRequest = new StringRequest().value(validCode);
        StringRequest invalidPasswordRequest = new StringRequest().value(invalidCode);
        Mockito.when(mockKairosUserService.getAccountForAccessCode(validCode)).thenReturn(jwtUser);
        Mockito.when(mockKairosUserService.getAccountForAccessCode(invalidCode)).thenReturn(null);

        ResponseEntity<StringResponse> response1 = usersApiController.resetPassword(validPasswordRequest);
        assert response1.getStatusCode() == HttpStatus.OK;

        ResponseEntity<StringResponse> response2 = usersApiController.resetPassword(invalidPasswordRequest);
        assert response2.getStatusCode() == HttpStatus.NOT_FOUND;
    }

    @Test
    void testSetPasswordChecksJwtPermissions() {
        StringRequest newPasswordRequest = new StringRequest().value("newPassword");
        StringRequest invalidShortPasswordRequest = new StringRequest().value("1234");
        StringRequest invalidLongPasswordRequest = new StringRequest().value("123456789abcdefghijklmnopqrstuvwxyz");

        // Test setPassword rejects user without valid permission
        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> usersApiController.setPassword(invalidShortPasswordRequest));
        assert exception.getStatus() == HttpStatus.BAD_REQUEST;

        ResponseStatusException exception2 = assertThrows(ResponseStatusException.class, () -> usersApiController.setPassword(invalidLongPasswordRequest));
        assert exception2.getStatus() == HttpStatus.BAD_REQUEST;

        updateReflections();
        ResponseEntity<StringResponse> response2 = usersApiController.setPassword(newPasswordRequest);
        assert response2.getStatusCode() == HttpStatus.OK;
    }

    @Test
    void registerUserToTeamTest() {
        Mockito.when(jwtPermissionRepo.findByName(anyString())).thenReturn(testPerm);
        Mockito.doNothing().when(mockKairosUserService).assertUniqueUserName(anyString());
        Mockito.doNothing().when(mockKairosUserService).assertUniqueEmailAddress(anyString());
        Mockito.doNothing().when(mockKairosUserService).saveUserEncryptPassword(any());

        RegistrationDto dto = new RegistrationDto().emailAddress("new.email@address.com").password("someSecurePWD").performerGroup("TA1").teamName("greatest");

        ResponseEntity<StringResponse> response = usersApiController.registerUserToTeam(dto);
        assert response.getStatusCode() == HttpStatus.CREATED;
    }

    @Test
    void addContactRequestTest() {
        ContactRequestDto dto = new ContactRequestDto().message("testMessage").requestor("somedude").topic("WORLDS GREATEST TOPIC");
        Mockito.doNothing().when(contactRequestService).saveNewRequest(any());

        ResponseEntity<StringResponse> response = usersApiController.addContactRequest(dto);
        assert response.getStatusCode() == HttpStatus.OK;
    }

    @Test
    void listContactRequestsTest() {
        List<ContactRequest> expectedReturn = new ArrayList<>();
        Mockito.when(contactRequestService.findAllContactRequests()).thenReturn(expectedReturn);
        ResponseEntity<List<ContactRequest>> response = usersApiController.listContactRequestsByStatus("");
        assert response.getStatusCode() == HttpStatus.OK;

        expectedReturn.add(new ContactRequest());
        Mockito.when(contactRequestService.findAllByStatus(any())).thenReturn(expectedReturn);
        response = usersApiController.listContactRequestsByStatus("UNANSWERED");
        assert response.getStatusCode() == HttpStatus.OK;
    }
}
