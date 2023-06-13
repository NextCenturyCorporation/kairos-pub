package com.ncc.kairos.moirai.zeus.services;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import com.amazonaws.services.ec2.model.Instance;
import com.ncc.kairos.moirai.zeus.model.JwtUser;
import com.ncc.kairos.moirai.zeus.model.Service;
import com.ncc.kairos.moirai.zeus.model.ServiceAwsInstance;
import com.ncc.kairos.moirai.zeus.model.ServiceDownload;
import com.ncc.kairos.moirai.zeus.model.ServiceEndpoint;
import com.ncc.kairos.moirai.zeus.resources.Constants;
import com.ncc.kairos.moirai.zeus.resources.EnvironmentTier;
import com.ncc.kairos.moirai.zeus.runner.AnsibleRunner;
import com.ncc.kairos.moirai.zeus.runner.ProcessRunner;
import com.ncc.kairos.moirai.zeus.utililty.AWSEC2Connector;
import com.ncc.kairos.moirai.zeus.utililty.ModelToAWSMapper;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

@TestPropertySource(locations = "classpath:test.properties")
public class TaServicesTest {

    @Autowired
    TAServices taServices;

    @Mock
    AWSEC2Connector mockAwsEc2Connector;

    @Mock
    AnsibleRunner ansibleRunner;

    @Mock
    ProcessRunner processRunner;

    @Spy
    PropertiesService mockPropertiesService;

    @Spy
    UserServicesService mockUserServicesService;

    @Spy
    KairosUserService mockKairosUserService;

    private JwtUser jwtUser;

    private Service testService;

    private List<Service> testServices = new ArrayList<>();

    void updateReflections() {
        ReflectionTestUtils.setField(taServices, "awsEc2Connector", mockAwsEc2Connector);
        ReflectionTestUtils.setField(taServices, "kairosUserService", mockKairosUserService);
        ReflectionTestUtils.setField(taServices, "processRunner", processRunner);
        ReflectionTestUtils.setField(taServices, "ansibleRunner", ansibleRunner);
        ReflectionTestUtils.setField(taServices, "propertiesService", mockPropertiesService);
        ReflectionTestUtils.setField(taServices, "userServicesService", mockUserServicesService);
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
        mockKairosUserService = Mockito.mock(KairosUserService.class);
        mockPropertiesService = Mockito.mock(PropertiesService.class);
        mockUserServicesService = Mockito.mock(UserServicesService.class);
        processRunner = Mockito.mock(ProcessRunner.class);
        updateReflections();

        testService = new Service()
        .access("Public")
        .details("details")
        .addAwsInstancesItem(new ServiceAwsInstance().id("adwda")
        .instanceId("KAFKA"))
        .status(Constants.SERVICE_STATUS_ACTIVE)
        .name("some name")
        .id("awewaeaw")
        .subtype("notnull")
        .addDownloadsItem(new ServiceDownload().id("rgsergseg").name("download 1").uri("google.com"))
        .addEndpointsItem(new ServiceEndpoint().id("asdawdw").name("ep").uri("google2.com"));

        jwtUser = new JwtUser().teamName("Moirai").username("John").emailAddress("John@Test.com").active(true);
        testServices.add(testService);

        Mockito.when(mockKairosUserService.findUserByUsername(jwtUser.getUsername())).thenReturn(jwtUser);
    }

    @Test
    public void provisionClothoTest() throws Exception {
        Mockito.when(ansibleRunner.runAnsible(any(), any(), anyInt())).thenReturn(testProcess());
        Mockito.when(processRunner.runCommands(any(), any())).thenReturn(testProcess());
        Mockito.when(mockUserServicesService.saveNewService(any(), any())).thenReturn(testService);
        Mockito.when(mockUserServicesService.getServiceByName(any())).thenReturn(testServices);
        Mockito.when(mockPropertiesService.whichEnvironment()).thenReturn(EnvironmentTier.TESTING);
        // Verification check failed
        assertThrows(ResponseStatusException.class, () -> {
            taServices.provisionClotho(testService, jwtUser);
        });

        // Running ansible command
        Mockito.when(mockUserServicesService.getServiceByName(any())).thenReturn(new ArrayList<>());
        Mockito.when(mockAwsEc2Connector.getCreatedInstanceByTag(any(), any())).thenReturn(new Instance());
        doNothing().when(mockUserServicesService).updateServiceStatus(any());
        try (MockedStatic<ModelToAWSMapper> mocked = Mockito.mockStatic(ModelToAWSMapper.class)) {
            mocked.when(() -> { 
                ModelToAWSMapper.getServiceAwsInstance(any()); 
            }).thenReturn(new ServiceAwsInstance().id("WhoCAres"));
            mocked.when(() -> { 
                ModelToAWSMapper.getServiceEndpointsInTags(any()); 
            }).thenReturn(new ArrayList<>());
            taServices.provisionClotho(testService, jwtUser);
            verify(mockUserServicesService, times(1)).updateServiceStatus(any());
        }
    }

    @Test
    public void terminateClothoInstanceTest() throws Exception {
        Mockito.when(ansibleRunner.runAnsible(any(), any(), anyInt())).thenReturn(testProcess());
        doNothing().when(mockUserServicesService).updateServiceStatus(any());
        taServices.terminateClothoInstance(testService, jwtUser);
        verify(mockUserServicesService, times(1)).updateServiceStatus(any());
    }

    // @Test
    // public void createEnclaveTest() throws Exception {
    //     Mockito.when(mockUserServicesService.saveNewService(any(), any())).thenReturn(testService);
    //     Mockito.when(processRunner.runCommands(any(), any())).thenReturn(testProcess());
    //     CreateEnclaveRequest enclaveReq = new CreateEnclaveRequest().experimentId("12345");
    //     taServices.createEnclave(enclaveReq.getExperimentId());
    //     verify(mockUserServicesService, times(1)).updateServiceStatus(any());
    //     verify(processRunner, times(2)).runCommands(any(), any());
    // }
    
    // @Test
    // public void terminateEnclaveTest() throws Exception {
    //     Mockito.when(mockUserServicesService.saveNewService(any(), any())).thenReturn(testService);
    //     Mockito.when(processRunner.runCommands(any(), any())).thenReturn(testProcess());
    //     taServices.terminateEnclave(testService, jwtUser);
    //     verify(mockUserServicesService, times(1)).updateServiceStatus(any());
    //     verify(processRunner, times(1)).runCommands(any(), any());
    // }
}
