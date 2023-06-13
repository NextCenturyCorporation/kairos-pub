package com.ncc.kairos.moirai.zeus.services;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import com.ncc.kairos.moirai.zeus.dao.DockerRegistryRepository;
import com.ncc.kairos.moirai.zeus.dao.JwtRoleRepository;
import com.ncc.kairos.moirai.zeus.dao.StoredFileRepository;
import com.ncc.kairos.moirai.zeus.model.DockerRegistry;
import com.ncc.kairos.moirai.zeus.model.DockerUpload;
import com.ncc.kairos.moirai.zeus.model.GitLabRequest;
import com.ncc.kairos.moirai.zeus.model.JwtPermission;
import com.ncc.kairos.moirai.zeus.model.JwtRole;
import com.ncc.kairos.moirai.zeus.model.JwtUser;
import com.ncc.kairos.moirai.zeus.model.StoredFile;
import com.ncc.kairos.moirai.zeus.runner.AnsibleRunner;
import com.ncc.kairos.moirai.zeus.runner.ProcessRunner;
import com.ncc.kairos.moirai.zeus.runner.TerraformRunner;
import com.ncc.kairos.moirai.zeus.utililty.AWSS3Connector;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.client.RestClientTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

@TestPropertySource(locations = "classpath:test.properties")
@RestClientTest(AdminServices.class)
public class AdminServicesTest {

    @Autowired
    AdminServices adminServices;
    
    @MockBean
    AWSS3Connector mockAwsS3Connector;

    @Mock
    ProcessRunner processRunner;

    @Mock
    TerraformRunner terraformRunner;

    @Mock
    AnsibleRunner ansibleRunner;

    @Spy
    KairosUserService mockKairosUserService;

    @Spy
    TAServices mockTaServices;

    @Spy
    DockerService mockDockerService;

    @Spy
    UserServicesService mockUserServicesService;

    @MockBean
    DockerRegistryRepository mockDockerRegistryRepository;

    @MockBean
    StoredFileRepository mockStoredFileRepository;

    @MockBean
    JwtRoleRepository mockJwtRoleRepository;
    

    private DockerRegistry dockerRegistry;

    private JwtUser jwtUser;

    private List<JwtRole> testRoles = new ArrayList<>();

    private JwtPermission testPermission;

    void updateReflections() {
        ReflectionTestUtils.setField(adminServices, "awsS3Connector", mockAwsS3Connector);
        ReflectionTestUtils.setField(adminServices, "kairosUserService", mockKairosUserService);
        ReflectionTestUtils.setField(adminServices, "processRunner", processRunner);
        ReflectionTestUtils.setField(adminServices, "ansibleRunner", ansibleRunner);
        ReflectionTestUtils.setField(adminServices, "terraformRunner", terraformRunner);
        ReflectionTestUtils.setField(adminServices, "dockerRegistryRepo", mockDockerRegistryRepository);
        ReflectionTestUtils.setField(adminServices, "storeFileRepo", mockStoredFileRepository);
        ReflectionTestUtils.setField(adminServices, "jwtRoleRepository", mockJwtRoleRepository);
        ReflectionTestUtils.setField(adminServices, "taServices", mockTaServices);
        ReflectionTestUtils.setField(adminServices, "userServicesService", mockUserServicesService);
        ReflectionTestUtils.setField(adminServices, "dockerService", mockDockerService);
    }
    
    

    @BeforeEach
    public void prepareUnitTests() {
        MockitoAnnotations.openMocks(this);
        testPermission = new JwtPermission().description("perm1").id("someId").name("perm1Name");
        testRoles.add(new JwtRole().description("test1").name("test1").id("aewaedaw").permissions(new ArrayList<>()));
        testRoles.add(new JwtRole().description("test2").name("test2").id("aewaedaw2").permissions(new ArrayList<>()));
        jwtUser = new JwtUser().teamName("Moirai").username("John").emailAddress("John@Test.com").active(true).roles(testRoles);
        mockKairosUserService = Mockito.mock(KairosUserService.class);
        mockAwsS3Connector = Mockito.mock(AWSS3Connector.class);
        processRunner = Mockito.mock(ProcessRunner.class);
        updateReflections();

        Mockito.when(mockKairosUserService.findUserByUsername(jwtUser.getUsername())).thenReturn(jwtUser);

        DockerUpload dockerUpload = new DockerUpload().id("awwa").dockerimagelocation("location").registry("Moirai");
        List<DockerUpload> dockerUploadList = new ArrayList<>();
        dockerUploadList.add(dockerUpload);
        dockerUploadList.add(new DockerUpload().id("awawdaddawwa2").dockerimagelocation("location2").registry("Moirai"));
        dockerRegistry = new DockerRegistry().id("Adwawdwa").endpoint("TEST").owner("Moirai").password("encoded").dockerimagelist(dockerUploadList);
    }

    private Process testProcess() throws Exception {
        ProcessBuilder temp = new ProcessBuilder();
        temp.command("echo");
        Process itsGoing = temp.start();
        itsGoing.waitFor(1, TimeUnit.SECONDS);
        itsGoing.destroy();
        return itsGoing;
    }
    
    @Test
    public void deleteDockerRegistryTest() throws Exception {
        Mockito.when(mockDockerRegistryRepository.findByOwner(any())).thenReturn(dockerRegistry);
        Mockito.when(processRunner.runCommands(any(), any())).thenReturn(testProcess());
        doNothing().when(mockAwsS3Connector).deleteDockerUpload(any());
        adminServices.deleteDockerRegistry("doesnt matter");
        verify(processRunner, times(1)).runCommands(any(), any());

        // null registry
        Mockito.when(mockDockerRegistryRepository.findByOwner(any())).thenReturn(null);
        assertThrows(ResponseStatusException.class, () -> {
            adminServices.deleteDockerRegistry(any());
        });
    }

    @Test 
    public void provisionEnclaveInfrastructureTest() throws ResponseStatusException, Exception {
        Mockito.when(processRunner.runCommands(any(), any())).thenReturn(testProcess());
        adminServices.provisionEnclaveInfrastructure();
        verify(processRunner, times(1)).runCommands(any(), any());
    }

    @Test 
    public void destroyEnclaveInfrastructureTest() throws ResponseStatusException, Exception {
        Mockito.when(processRunner.runCommands(any(), any())).thenReturn(testProcess());
        adminServices.destroyEnclaveInfrastructure();
        verify(processRunner, times(1)).runCommands(any(), any());
    }

    @Test
    public void toggleAccountActivationTest() {
        Mockito.when(mockKairosUserService.findUserByUsername(jwtUser.getUsername())).thenReturn(null);
        assertThrows(ResponseStatusException.class, () -> {
            adminServices.toggleAccountActivation(any());
        });

        Mockito.when(mockKairosUserService.findUserByUsername(jwtUser.getUsername())).thenReturn(jwtUser);
        doNothing().when(mockKairosUserService).saveUser(any());
        adminServices.toggleAccountActivation(jwtUser.getUsername());
        verify(mockKairosUserService, times(1)).saveUser(any());
    }

    // @Test
    // public void assignUserRolesTest() {
    //     UserDataDto dto = new UserDataDto();
    //     dto.roles(testRoles);
    //     dto.id(jwtUser.getId());
    //     doNothing().when(mockKairosUserService).saveUser(any());
    //     assertThrows(ResponseStatusException.class, () -> {
    //         adminServices.updateUserAccount(jwtUser.getId(), dto);
    //     });

    //     Mockito.when(mockJwtRoleRepository.findById(any())).thenReturn(Optional.of(dto.getRoles().get(0)));
    //     adminServices.updateUserAccount(jwtUser.getId(), dto);
    //     verify(mockKairosUserService, times(1)).saveUser(any());

    //     Mockito.when(mockKairosUserService.findUserByUsername(jwtUser.getId())).thenReturn(null);
    //     assertThrows(ResponseStatusException.class, () -> {
    //         adminServices.updateUserAccount(any(),new UserDataDto());
    //     });
    // }

    @Test
    public void deleteRolesTest() {
        List<JwtUser> userList = new ArrayList<>();
        userList.add(jwtUser);
        Mockito.when(mockKairosUserService.findAllUsers()).thenReturn(userList);
        Mockito.when(mockJwtRoleRepository.findById(any())).thenReturn(Optional.of(testRoles.get(0)));
        doNothing().when(mockJwtRoleRepository).delete(any());
        assertThrows(ResponseStatusException.class, () -> {
            adminServices.deleteRoles(testRoles.get(0));
            
        });
        jwtUser.setRoles(new ArrayList<>());
        adminServices.deleteRoles(testRoles.get(0));
        verify(mockJwtRoleRepository, times(1)).delete(any());
    }

    @Test
    public void createOrUpdateRolesTest() {
        assertThrows(ResponseStatusException.class, () -> {
            adminServices.createOrUpdateRoles(testRoles);
        });

        Mockito.when(mockJwtRoleRepository.findById(any())).thenReturn(Optional.of(testRoles.get(0)));
        // Adding permission
        for (JwtRole role : testRoles) {
            role.addPermissionsItem(testPermission);
        }
        adminServices.createOrUpdateRoles(testRoles);
        verify(mockJwtRoleRepository, times(2)).save(any());

        JwtRole newRole = new JwtRole().description("test1").name("test1").id("").permissions(new ArrayList<>());
        newRole.addPermissionsItem(testPermission);
        List<JwtRole> newRoles = new ArrayList<>();
        newRoles.add(newRole);
        adminServices.createOrUpdateRoles(newRoles);
        verify(mockJwtRoleRepository, times(3)).save(any());
    }

    @Test
    public void updateTeamNameTest() throws ResponseStatusException, Exception {
        List<JwtUser> userList = new ArrayList<>();
        userList.add(jwtUser);
        Mockito.when(processRunner.runCommands(any(), any())).thenReturn(testProcess());
        Mockito.when(mockKairosUserService.findAllUsers()).thenReturn(userList);
        Mockito.when(mockJwtRoleRepository.findById(any())).thenReturn(Optional.of(testRoles.get(0)));
        Mockito.when(mockUserServicesService.getServicesByTeamName(anyString())).thenReturn(new ArrayList<>());
        doNothing().when(mockAwsS3Connector).deleteDockerUpload(any());
        doNothing().when(mockDockerService).createDockerRegistry(anyString(), any());

        Mockito.when(mockDockerRegistryRepository.findByOwner(any())).thenReturn(dockerRegistry);
        List<StoredFile> returnThis = new ArrayList<>();
        returnThis.add(new StoredFile().category("something").filename("name").canSubmit(true));
        Mockito.when(mockStoredFileRepository.findAllByOwner(jwtUser.getTeamName())).thenReturn(returnThis);
        doNothing().when(mockKairosUserService).saveUser(any());
        
        adminServices.updateTeamName(jwtUser, jwtUser.getTeamName());

        verify(mockDockerRegistryRepository, times(0)).save(any());
        verify(mockKairosUserService, times(1)).saveUser(any());

    }

    @Test
    public void migrateTeamNameTest() throws ResponseStatusException, Exception {
        List<JwtUser> userList = new ArrayList<>();
        userList.add(jwtUser);
        Mockito.when(processRunner.runCommands(any(), any())).thenReturn(testProcess());
        Mockito.when(mockKairosUserService.findAllUsers()).thenReturn(userList);
        Mockito.when(mockJwtRoleRepository.findById(any())).thenReturn(Optional.of(testRoles.get(0)));

        doNothing().when(mockDockerService).createDockerRegistry(anyString(), any());
        doNothing().when(mockAwsS3Connector).deleteDockerUpload(any());


        Mockito.when(mockDockerRegistryRepository.findByOwner(any())).thenReturn(dockerRegistry);
        Mockito.when(mockDockerRegistryRepository.findByOwner("NotMoirai")).thenReturn(null);
        doNothing().when(mockDockerRegistryRepository).delete(any());


        List<StoredFile> returnThis = new ArrayList<>();
        returnThis.add(new StoredFile().category("something").filename("name").canSubmit(true));
        Mockito.when(mockStoredFileRepository.findAllByOwner(jwtUser.getTeamName())).thenReturn(returnThis);
        adminServices.migrateTeamName(jwtUser, "NotMoirai");
        verify(mockKairosUserService, times(1)).saveUser(any());

    }

    @Test
    public void provisionGitlabTest() throws Exception {
        Mockito.when(terraformRunner.runTerraform(any(), any(), any())).thenReturn(testProcess());
        Mockito.when(ansibleRunner.runAnsible(any(), any(), anyInt())).thenReturn(testProcess());
        adminServices.provisionGitlab(new GitLabRequest());
        verify(terraformRunner, times(2)).runTerraform(any(), any(), any());
        verify(ansibleRunner, times(1)).runAnsible(any(), any(), anyInt());
    }

    @Test
    public void destroyGitlabTest() throws Exception {
        Mockito.when(terraformRunner.runTerraform(any(), any(), any())).thenReturn(testProcess());
        Mockito.when(ansibleRunner.runAnsible(any(), any(), anyInt())).thenReturn(testProcess());
        adminServices.destroyGitlab();
        verify(terraformRunner, times(2)).runTerraform(any(), any(), any());
        verify(ansibleRunner, times(1)).runAnsible(any(), any(), anyInt());
    }
    
}
