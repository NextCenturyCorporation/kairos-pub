package com.ncc.kairos.moirai.zeus.api;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import com.ncc.kairos.moirai.zeus.dao.JwtPermissionRepository;
import com.ncc.kairos.moirai.zeus.dao.JwtRoleRepository;
import com.ncc.kairos.moirai.zeus.model.GitLabRequest;
import com.ncc.kairos.moirai.zeus.model.JwtPermission;
import com.ncc.kairos.moirai.zeus.model.JwtRole;
import com.ncc.kairos.moirai.zeus.model.JwtUser;
import com.ncc.kairos.moirai.zeus.model.StringRequest;
import com.ncc.kairos.moirai.zeus.model.StringResponse;
import com.ncc.kairos.moirai.zeus.model.UserDataDto;
import com.ncc.kairos.moirai.zeus.security.utils.JwtUtils;
import com.ncc.kairos.moirai.zeus.services.AdminServices;
import com.ncc.kairos.moirai.zeus.services.KairosUserService;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(SpringExtension.class)
@TestPropertySource(locations = "classpath:test.properties")
public class AdminApiControllerTest {

    AdminApiController adminApiController;

    AdminServices adminServices;

    KairosUserService mockKairosUserService;

    JwtPermissionRepository mockJwtPermissionService;

    JwtRoleRepository mockJwtRoleRepository;

    NativeWebRequest mockNativeWebRequest;

    JwtUtils mockJwtUtils;

    private JwtUser jwtUser;

    private JwtPermission permission;

    private List<JwtPermission> permissions = new ArrayList<>();

    private JwtRole role;

    private List<JwtRole> roles = new ArrayList<>();

    void updateReflections() {
        ReflectionTestUtils.setField(adminApiController, "adminServices", adminServices);
        ReflectionTestUtils.setField(adminApiController, "kairosUserService", mockKairosUserService);
        ReflectionTestUtils.setField(adminApiController, "request", mockNativeWebRequest);
        ReflectionTestUtils.setField(adminApiController, "jwtUtils", mockJwtUtils);
        ReflectionTestUtils.setField(adminApiController, "jwtPermissionService", mockJwtPermissionService);
        ReflectionTestUtils.setField(adminApiController, "jwtRoleRepository", mockJwtRoleRepository);
    }

    @BeforeEach
    void setup() {
        permission = new JwtPermission().description("ADMIN").name("ADMIN");
        permissions.add(permission);
        role = new JwtRole().permissions(permissions);
        roles.add(role);
        jwtUser = new JwtUser()
        .username("John")
        .emailAddress("John@Test.com")
        .password("somePass")
        .addRolesItem(role)
        .active(true)
        .passwordExpiration(LocalDateTime.ofInstant(validPasswordExpiration().toInstant(), validPasswordExpiration().getTimeZone().toZoneId()).toLocalDate());

        mockKairosUserService = Mockito.mock(KairosUserService.class);
        mockNativeWebRequest = Mockito.mock(NativeWebRequest.class);
        mockJwtUtils = Mockito.mock(JwtUtils.class);
        adminServices = Mockito.mock(AdminServices.class);
        mockJwtPermissionService = Mockito.mock(JwtPermissionRepository.class);
        mockJwtRoleRepository = Mockito.mock(JwtRoleRepository.class);

        Mockito.when(mockNativeWebRequest.getAttribute("token", 0)).thenReturn("token");
        Mockito.when(mockNativeWebRequest.getAttribute("jwtUser", 0)).thenReturn(this.jwtUser);
        Mockito.when(mockKairosUserService.findUserByUsername(jwtUser.getUsername())).thenReturn(jwtUser);
        Mockito.when(mockKairosUserService.generateAccessCodeForAccount(any(JwtUser.class))).thenReturn("granted");
        Mockito.when(mockJwtUtils.generateJwtToken(any(Authentication.class))).thenReturn("");

        adminApiController = new AdminApiController(mockNativeWebRequest);
        updateReflections();
    }

    @Test
    void getRequestTest() {
        Optional<NativeWebRequest> result = adminApiController.getRequest();
        assert result.get() != null;
    }

    @Test
    public void gitlabProvisionTest() {
        GitLabRequest request = new GitLabRequest().prop1("someprop").prop2("anotherprop?");
        Mockito.doNothing().when(adminServices).provisionGitlab(request);
        ResponseEntity<StringResponse> response = adminApiController.provisionGitlab(request);
        verify(adminServices, times(1)).provisionGitlab(any());
        assert response.getStatusCode().equals(HttpStatus.OK);

        Mockito.doThrow(ResponseStatusException.class).when(adminServices).provisionGitlab(request);
        response = adminApiController.provisionGitlab(request);
        assert response.getStatusCode().equals(HttpStatus.BAD_REQUEST);
    }

    @Test
    public void destroyGitlabTest() {
        Mockito.doNothing().when(adminServices).destroyGitlab();
        ResponseEntity<StringResponse> response = adminApiController.destroyGitlab();
        verify(adminServices, times(1)).destroyGitlab();
        assert response.getStatusCode().equals(HttpStatus.OK);
    }

    @Test
    public void provisionEnclaveInfrastructureTest() {
        StringRequest stringR = new StringRequest().value("somethingneatooo");
        Mockito.doNothing().when(adminServices).provisionEnclaveInfrastructure();
        ResponseEntity<StringResponse> response = adminApiController.provisionEnclaveInfrastructure(stringR);
        verify(adminServices, times(1)).provisionEnclaveInfrastructure();
        assert response.getStatusCode().equals(HttpStatus.OK);
    }

    @Test
    public void listUsersTest() {
        List<JwtUser> userList = new ArrayList<>(); 
        userList.add(jwtUser);
        Mockito.when(mockKairosUserService.findAllUsers()).thenReturn(userList);

        ResponseEntity<List<JwtUser>> response = adminApiController.listUsers();
        assert response.getStatusCode().equals(HttpStatus.OK);
    }

    @Test
    void listPermissionsTest() {
        Mockito.when(mockJwtPermissionService.findAll()).thenReturn(permissions);

        ResponseEntity<List<JwtPermission>> response = adminApiController.listPermissions();
        assert response.getStatusCode().equals(HttpStatus.OK);
    }
    
    @Test
    void listRolesTest() {
        Mockito.when(mockJwtRoleRepository.findAll()).thenReturn(roles);

        ResponseEntity<List<JwtRole>> response = adminApiController.listRoles();
        assert response.getStatusCode().equals(HttpStatus.OK);
    }
    
    @Test
    void assignUserRolesTest() {
        UserDataDto dto = new UserDataDto();
        Mockito.doNothing().when(adminServices).updateUserAccount(dto);

        ResponseEntity<StringResponse> response = adminApiController.updateUserAccount(dto);
        assert response.getStatusCode().equals(HttpStatus.OK);
    }
        
    @Test
    void createOrUpdateRoleTest() {
        Mockito.doNothing().when(adminServices).createOrUpdateRoles(roles);

        ResponseEntity<StringResponse> response = adminApiController.createOrUpdateRole(roles);
        assert response.getStatusCode().equals(HttpStatus.OK);
    }

    @Test
    void deleteRolesTest() {
        Mockito.doNothing().when(adminServices).deleteRoles(role);

        ResponseEntity<StringResponse> response = adminApiController.deleteRoles(role);
        assert response.getStatusCode().equals(HttpStatus.OK);
    }
    
    @Test
    void updateTeamNameTest() {
        Mockito.doNothing().when(adminServices).migrateTeamName(jwtUser, jwtUser.getTeamName());

        ResponseEntity<StringResponse> response = adminApiController.updateTeamName(jwtUser.getTeamName(), jwtUser);
        assert response.getStatusCode().equals(HttpStatus.OK);
    }

    @Test
    void deleteTeamRegistryTest() {
        Mockito.doNothing().when(adminServices).deleteDockerRegistry(jwtUser.getTeamName());

        ResponseEntity<StringResponse> response = adminApiController.deleteTeamRegistry(jwtUser.getTeamName());
        assert response.getStatusCode().equals(HttpStatus.OK);
    }

    @Test
    void toggleAccountActivationTest() {
        Mockito.doNothing().when(adminServices).toggleAccountActivation(jwtUser.getTeamName());

        ResponseEntity<StringResponse> response = adminApiController.toggleAccountActivation(jwtUser.getTeamName());
        assert response.getStatusCode().equals(HttpStatus.OK);
    }

    @Test
    void assumeUserTest() {
        Collection<GrantedAuthority> authorities = new ArrayList<>();
        authorities.add(new SimpleGrantedAuthority("test"));

        UserDetails details =  User.withUsername(jwtUser.getUsername())
        .password(jwtUser.getPassword())
        .accountLocked(!jwtUser.getActive())
        .authorities(authorities)
        .credentialsExpired(jwtUser.getPasswordExpiration().isBefore(LocalDate.now()))
        .build();
        Mockito.when(mockKairosUserService.loadUserByUsername(jwtUser.getUsername())).thenReturn(details);
        StringRequest req = new StringRequest().value(jwtUser.getUsername());
        ResponseEntity<StringResponse> response = adminApiController.assumeUser(req);
        assert response.getStatusCode().equals(HttpStatus.OK);
    }

    private Calendar validPasswordExpiration() {
        Calendar c = Calendar.getInstance();
        c.add(Calendar.DAY_OF_MONTH, 30);
        return c;
    }
}
