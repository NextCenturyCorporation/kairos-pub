package com.ncc.kairos.moirai.zeus.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import com.ncc.kairos.moirai.zeus.dao.DockerRegistryRepository;
import com.ncc.kairos.moirai.zeus.dao.DockerUploadRepository;
import com.ncc.kairos.moirai.zeus.model.DockerRegistry;
import com.ncc.kairos.moirai.zeus.model.DockerUpload;
import com.ncc.kairos.moirai.zeus.model.JwtUser;
import com.ncc.kairos.moirai.zeus.runner.ProcessRunner;
import com.ncc.kairos.moirai.zeus.utililty.AWSS3Connector;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

@TestPropertySource(locations = "classpath:test.properties")
public class DockerServiceTest {

    @Autowired
    DockerService dockerService;
    
    @MockBean
    AWSS3Connector mockAwsS3Connector;

    @Mock
    ProcessRunner processRunner;

    @Spy
    KairosUserService mockKairosUserService;

    @MockBean
    DockerRegistryRepository mockDockerRegistryRepository;

    @MockBean
    DockerUploadRepository mockDockerUploadRepository;

    private JwtUser jwtUser;

    private DockerRegistry dockerRegistry;

    private DockerUpload dockerUpload;

    void updateReflections() {
        ReflectionTestUtils.setField(dockerService, "awsS3Connector", mockAwsS3Connector);
        ReflectionTestUtils.setField(dockerService, "kairosUserService", mockKairosUserService);
        ReflectionTestUtils.setField(dockerService, "processRunner", processRunner);
        ReflectionTestUtils.setField(dockerService, "dockerRegistryRepository", mockDockerRegistryRepository);
        ReflectionTestUtils.setField(dockerService, "dockerUploadRepository", mockDockerUploadRepository);
    }

    private Process testProcess() throws Exception {
        ProcessBuilder temp = new ProcessBuilder();
        temp.command("echo");
        Process itsGoing = temp.start();
        itsGoing.waitFor(1, TimeUnit.SECONDS);
        itsGoing.destroy();
        return itsGoing;
    }

    @BeforeEach
    public void prepareUnitTests() {
        MockitoAnnotations.openMocks(this);
        jwtUser = new JwtUser().teamName("Moirai").username("John").emailAddress("John@Test.com").active(true);
        mockKairosUserService = Mockito.mock(KairosUserService.class);
        mockAwsS3Connector = Mockito.mock(AWSS3Connector.class);
        processRunner = Mockito.mock(ProcessRunner.class);
        mockDockerRegistryRepository = Mockito.mock(DockerRegistryRepository.class);
        mockDockerUploadRepository = Mockito.mock(DockerUploadRepository.class);
        updateReflections();

        Mockito.when(mockKairosUserService.findUserByUsername(jwtUser.getUsername())).thenReturn(jwtUser);
        
        dockerUpload = new DockerUpload().id("awwa").dockerimagelocation("location").registry("Moirai");
        List<DockerUpload> dockerUploadList = new ArrayList<>();
        dockerUploadList.add(dockerUpload);
        dockerUploadList.add(new DockerUpload().id("awawdaddawwa2").dockerimagelocation("location2").registry("Moirai"));
        dockerRegistry = new DockerRegistry().id("Adwawdwa").endpoint("TEST").owner("Moirai").password("encoded").dockerimagelist(dockerUploadList);

    }

    @Test
    public void saveNewRegistryTest() throws Exception {
        Mockito.when(processRunner.runCommands(any(), any())).thenReturn(testProcess());
        dockerService.createDockerRegistry("any", jwtUser.getTeamName());
        verify(processRunner, times(1)).runCommands(any(), any());
        verify(mockDockerRegistryRepository, times(1)).save(any());
    }

    @Test
    public void ResetRegistryTest() throws Exception {
        Mockito.when(processRunner.runCommands(any(), any())).thenReturn(testProcess());
        Mockito.when(mockDockerRegistryRepository.findByOwner(jwtUser.getTeamName())).thenReturn(dockerRegistry);

        dockerService.resetDockerPassword("anewPAssword", jwtUser.getTeamName());
        verify(processRunner, times(1)).runCommands(any(), any());
        verify(mockDockerRegistryRepository, times(1)).save(any());
    }

    @Test
    public void getDockerRegistriesTest() {
        Mockito.when(mockDockerRegistryRepository.findByOwner(jwtUser.getTeamName())).thenReturn(dockerRegistry);

        List<DockerRegistry> returned = dockerService.getDockerRegistries("Moirai");
        assertThat(returned.size()).isEqualTo(1); 

        assertThrows(ResponseStatusException.class, () -> {
            dockerService.getDockerRegistries("*");
        });
    }

    @Test
    public void deleteDockerUploadTest() { 
        // No Mocks should fail right away
        assertThrows(ResponseStatusException.class, () -> {
            dockerService.deleteDockerUpload("awwa", jwtUser.getTeamName());
        });
        // Do nothing for AWS stuff
        doNothing().when(mockAwsS3Connector).deleteDockerUpload(any());
        // Errors when no registry
        Mockito.when(mockDockerUploadRepository.findById(any())).thenReturn(Optional.of(dockerUpload));
        assertThrows(ResponseStatusException.class, () -> {
            dockerService.deleteDockerUpload("awwa", jwtUser.getTeamName());
        });

        Mockito.when(mockDockerRegistryRepository.findByOwner(jwtUser.getTeamName())).thenReturn(dockerRegistry);
        dockerService.deleteDockerUpload("awwa", jwtUser.getTeamName());
    }

    @Test
    public void refreshRegistryTest() { 
        DockerUpload newUpload = new DockerUpload().id("141412131").dockerimagelocation("location").registry("Moirai");
        List<DockerUpload> newUploadList = dockerRegistry.getDockerimagelist();
        newUploadList.add(newUpload);
        Mockito.when(mockAwsS3Connector.getDockerRegistryS3Data(jwtUser.getTeamName())).thenReturn(newUploadList);
        dockerService.refreshRegistry(dockerRegistry);
        // verify(mockDockerUploadRepository, times(1)).save(any());
    }

    @Test
    public void singlerefreshRegistryTest() {
        Mockito.when(mockDockerRegistryRepository.findByOwner(jwtUser.getTeamName())).thenReturn(dockerRegistry);
        dockerService.refreshRegistry(jwtUser.getTeamName());
    }

    @Test
    public void refreshAllRegistriesTest() {
        DockerUpload newUpload = new DockerUpload().id("141412131").dockerimagelocation("location").registry("Moirai");
        dockerRegistry.addDockerimagelistItem(newUpload);
        Mockito.when(mockDockerRegistryRepository.findAll()).thenReturn(Arrays.asList(dockerRegistry));
        dockerService.refreshAllRegistries();
        // verify(mockDockerUploadRepository, times(1)).save(any());
    }

    @Test
    public void restoreAllRegistriesTest() throws ResponseStatusException, Exception {
        Mockito.when(processRunner.runCommands(any(), any())).thenReturn(testProcess());
        Mockito.when(mockDockerRegistryRepository.findAll()).thenReturn(Arrays.asList(dockerRegistry));
        dockerService.restoreAllRegistries();
        verify(processRunner, times(1)).runCommands(any(), any());
    }

}
