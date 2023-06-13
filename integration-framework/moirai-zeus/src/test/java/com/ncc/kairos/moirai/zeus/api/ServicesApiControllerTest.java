package com.ncc.kairos.moirai.zeus.api;

import com.amazonaws.services.elasticache.model.ReplicationGroupAlreadyExistsException;
import com.ncc.kairos.moirai.zeus.model.*;
import com.ncc.kairos.moirai.zeus.model.ClothoServiceDto.DatabaseTypeEnum;
import com.ncc.kairos.moirai.zeus.resources.Constants;
import com.ncc.kairos.moirai.zeus.services.DockerService;
import com.ncc.kairos.moirai.zeus.services.KairosUserService;
import com.ncc.kairos.moirai.zeus.services.TAServices;
import com.ncc.kairos.moirai.zeus.services.UserServicesService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.server.ResponseStatusException;
import org.modelmapper.ModelMapper;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@TestPropertySource(locations = "classpath:test.properties")
public class ServicesApiControllerTest {

    ServicesApiController servicesApiController;

    NativeWebRequest mockNativeWebRequest;

    UserServicesService userServicesService;

    KairosUserService kairosUserService;

    TAServices taServices;

    DockerService dockerService;

    ModelMapper looseModelMapper;

    private JwtUser jwtUser;
    
    private Service service;

    private List<Service> services = new ArrayList<>();
    

    void updateReflections() {
        ReflectionTestUtils.setField(servicesApiController, "userServicesService", userServicesService);
        ReflectionTestUtils.setField(servicesApiController, "kairosUserService", kairosUserService);
        ReflectionTestUtils.setField(servicesApiController, "taServices", taServices);
        ReflectionTestUtils.setField(servicesApiController, "dockerService", dockerService);
        ReflectionTestUtils.setField(servicesApiController, "looseModelMapper", looseModelMapper);
        ReflectionTestUtils.setField(servicesApiController, "request", mockNativeWebRequest);

    }

    @BeforeEach
    void setup() {
        service = new Service()
        .access("Public")
        .details("details")
        .type("CLOTHO")
        .addAwsInstancesItem(new ServiceAwsInstance().id("adwda").instanceId("KAFKA"))
        .status(Constants.SERVICE_STATUS_ACTIVE)
        .name("some name")
        .id("awewaeaw")
        .subtype(DatabaseTypeEnum.NEO4J.toString())
        .addDownloadsItem(new ServiceDownload().id("rgsergseg").name("download 1").uri("google.com"))
        .addEndpointsItem(new ServiceEndpoint().id("asdawdw").name("ep").uri("google2.com"));
        services.add(service);
        jwtUser = new JwtUser().username("John").emailAddress("John@Test.com").active(true).services(services);

        kairosUserService = Mockito.mock(KairosUserService.class);
        mockNativeWebRequest = Mockito.mock(NativeWebRequest.class);
        userServicesService = Mockito.mock(UserServicesService.class);
        taServices = Mockito.mock(TAServices.class);
        dockerService = Mockito.mock(DockerService.class);
        looseModelMapper = Mockito.mock(ModelMapper.class);

        Mockito.when(mockNativeWebRequest.getAttribute("token", 0)).thenReturn("token");
        Mockito.when(mockNativeWebRequest.getAttribute("jwtUser", 0)).thenReturn(this.jwtUser);
        Mockito.when(kairosUserService.findUserByUsername(jwtUser.getUsername())).thenReturn(jwtUser);

        servicesApiController = new ServicesApiController(mockNativeWebRequest);
        updateReflections();
    }

    @Test
    void getRequestTest() {
        Optional<NativeWebRequest> result = servicesApiController.getRequest();
        assert result.get() != null;
    }

    @Test
    void listServicesTest() {
        Mockito.when(userServicesService.getFilteredServiceList(service.getName(), jwtUser)).thenReturn(services);
        ResponseEntity<List<Service>> response = servicesApiController.listServices(service.getName());
        assert response.getStatusCode().equals(HttpStatus.OK);
    }

    @Test
    void provisionClothoTest() {
        ClothoServiceDto dto = new ClothoServiceDto().databaseType(DatabaseTypeEnum.NEO4J).details(service.getDetails()).name(service.getName());
        Mockito.when(looseModelMapper.map(dto, Service.class)).thenReturn(service);
        Mockito.doNothing().when(taServices).provisionClotho(service , jwtUser);

        ResponseEntity<StringResponse> response = servicesApiController.provisionClotho(dto);
        assert response.getStatusCode().equals(HttpStatus.OK);     

        Mockito.when(userServicesService.saveNewService(service, jwtUser)).thenReturn(service); 
        Mockito.doNothing().when(userServicesService).updateServiceStatus(service);
        Mockito.doThrow(ReplicationGroupAlreadyExistsException.class).when(taServices).provisionClotho(any() , any());
        response = servicesApiController.provisionClotho(dto);
        assert response.getStatusCode().equals(HttpStatus.BAD_REQUEST);   
    }

    @Test
    void terminateClothoInstanceTest() {
        Mockito.doNothing().when(taServices).terminateClothoInstance(service , jwtUser);

        ResponseEntity<StringResponse> response = servicesApiController.terminateClothoInstance(service);
        assert response.getStatusCode().equals(HttpStatus.OK); 

        Mockito.doNothing().when(userServicesService).updateServiceStatus(service);
        Mockito.doThrow(ReplicationGroupAlreadyExistsException.class).when(taServices).terminateClothoInstance(any() , any());
        response = servicesApiController.terminateClothoInstance(service);
        assert response.getStatusCode().equals(HttpStatus.BAD_REQUEST);  
    } 

    // @Test
    // void createServicesEnclavetest() {
        // CreateEnclaveRequest request = new CreateEnclaveRequest().service(service);
        // Mockito.doNothing().when(taServices).createEnclave(request, jwtUser);

        // ResponseEntity<StringResponse> response = servicesApiController.createServicesEnclave(request);
        // assert response.getStatusCode().equals(HttpStatus.OK); 


        // Mockito.when(userServicesService.saveNewService(service, jwtUser)).thenReturn(service); 
        // Mockito.doNothing().when(userServicesService).updateServiceStatus(service);
        // Mockito.doThrow(ReplicationGroupAlreadyExistsException.class).when(taServices).createEnclave(any(), any());
        // response = servicesApiController.createServicesEnclave(request);
        // assert response.getStatusCode().equals(HttpStatus.BAD_REQUEST);
    // }

    // @Test
    // void terminateEnclaveTest() {
    //     Mockito.doNothing().when(taServices).terminateEnclave(service, jwtUser);

    //     ResponseEntity<StringResponse> response = servicesApiController.terminateEnclave(service);
    //     assert response.getStatusCode().equals(HttpStatus.OK);

    //     Mockito.doThrow(ReplicationGroupAlreadyExistsException.class).when(taServices).terminateEnclave(any(), any());
    //     response = servicesApiController.terminateEnclave(service);
    //     assert response.getStatusCode().equals(HttpStatus.BAD_REQUEST);
    // }

    @Test
    void createDockerRegistryTest() {
        StringRequest request = new StringRequest().value("somethingMoreinterestingthanthis");
        Mockito.doNothing().when(dockerService).createDockerRegistry(request.getValue(), jwtUser.getTeamName());

        ResponseEntity<StringResponse> response = servicesApiController.createDockerRegistry(request);
        assert response.getStatusCode().equals(HttpStatus.OK);   
        
        Mockito.doThrow(ReplicationGroupAlreadyExistsException.class).when(dockerService).createDockerRegistry(any(), any());
        response = servicesApiController.createDockerRegistry(request);
        assert response.getStatusCode().equals(HttpStatus.BAD_REQUEST);
    }

    @Test
    void resetDockerRegistryPasswordTest() {
        StringRequest request = new StringRequest().value("somethingMoreinterestingthanthis");
        Mockito.doNothing().when(dockerService).resetDockerPassword(request.getValue(), jwtUser.getTeamName());

        ResponseEntity<StringResponse> response = servicesApiController.resetDockerRegistryPassword(request);
        assert response.getStatusCode().equals(HttpStatus.OK);   
        
        Mockito.doThrow(ReplicationGroupAlreadyExistsException.class).when(dockerService).resetDockerPassword(any(), any());
        response = servicesApiController.resetDockerRegistryPassword(request);
        assert response.getStatusCode().equals(HttpStatus.BAD_REQUEST);
    }

    @Test
    void refreshDockerRegistryTest() {
        Mockito.doNothing().when(dockerService).refreshRegistry(jwtUser.getTeamName());

        ResponseEntity<StringResponse> response = servicesApiController.refreshDockerRegistry();
        assert response.getStatusCode().equals(HttpStatus.OK);   
        
        Mockito.doThrow(ReplicationGroupAlreadyExistsException.class).when(dockerService).refreshRegistry(jwtUser.getTeamName());
        response = servicesApiController.refreshDockerRegistry();
        assert response.getStatusCode().equals(HttpStatus.BAD_REQUEST);
    }

    @Test
    void restoreDockerRegistryTest() {
        Mockito.doNothing().when(dockerService).restoreAllRegistries();
        Mockito.doNothing().when(dockerService).refreshAllRegistries();

        ResponseEntity<StringResponse> response = servicesApiController.restoreDockerRegistry();
        assert response.getStatusCode().equals(HttpStatus.OK);  

        Mockito.doThrow(ReplicationGroupAlreadyExistsException.class).when(dockerService).restoreAllRegistries();

        response = servicesApiController.restoreDockerRegistry();
        assert response.getStatusCode().equals(HttpStatus.BAD_REQUEST);   
    }

    @Test
    void getDockerRegistryTest() {
        List<DockerRegistry> result = new ArrayList<>();

        Mockito.when(dockerService.getDockerRegistries(jwtUser.getTeamName())).thenReturn(result);
        assertThrows(ResponseStatusException.class, () -> {
            servicesApiController.getDockerRegistry();
        });

        // Mockito.when(dockerService.getDockerRegistries(jwtUser.getTeamName())).thenThrow(ReplicationGroupAlreadyExistsException.class);
        // assertThrows(ResponseStatusException.class, () -> {
        //     servicesApiController.getDockerRegistry();
        // });

        result.add(new DockerRegistry().id(UUID.randomUUID().toString()).endpoint("ep"));
        Mockito.when(dockerService.getDockerRegistries(jwtUser.getTeamName())).thenReturn(result);
        ResponseEntity<DockerRegistry> response = servicesApiController.getDockerRegistry();
        assert response.getStatusCode().equals(HttpStatus.OK);
    }

    @Test
    void getDockerRegistryForUserTest() {
        List<DockerRegistry> result = new ArrayList<>();
        Mockito.when(dockerService.getDockerRegistries(jwtUser.getTeamName())).thenReturn(result);
        ResponseEntity<List<DockerRegistry>> response = servicesApiController.getDockerRegistryForUser(jwtUser.getTeamName());
        assert response.getStatusCode().equals(HttpStatus.OK);


        Mockito.when(dockerService.getDockerRegistries(jwtUser.getTeamName())).thenThrow(ResponseStatusException.class);
        assertThrows(ResponseStatusException.class, () -> {
            servicesApiController.getDockerRegistryForUser(jwtUser.getTeamName());
        });
    }

    // Ryan re enable after docker bug is fixed
    // @Test
    // void destroyDockerUploadsTest() {
    //     Mockito.doNothing().when(dockerService).deleteDockerUpload(anyString(), any());

    //     ResponseEntity<StringResponse> response = servicesApiController.destroyDockerUploads(jwtUser.getTeamName());
    //     assert response.getStatusCode().equals(HttpStatus.OK);

    //     // Mockito.doThrow(ResponseStatusException.class).when(dockerService).deleteDockerUpload(anyString(), any());
    //     // assertThrows(ResponseStatusException.class, () -> {
    //     //     servicesApiController.destroyDockerUploads(jwtUser.getTeamName());
    //     // });

    // }

    // @Test
    // void terminateServiceInstanceTest() {
    //     Mockito.doNothing().when(taServices).terminateClothoInstance(service , jwtUser);
    //     Mockito.doNothing().when(taServices).terminateEnclave(service, jwtUser);
    //     ResponseEntity<StringResponse> response = servicesApiController.terminateServiceInstance(service);
    //     assert response.getStatusCode().equals(HttpStatus.OK);
    //     response = servicesApiController.terminateServiceInstance(service.type("enclave"));
    //     assert response.getStatusCode().equals(HttpStatus.OK);

    //     assertThrows(ResponseStatusException.class, () -> {
    //         servicesApiController.terminateServiceInstance(service.type("12311e"));
    //     });
    // }

}
