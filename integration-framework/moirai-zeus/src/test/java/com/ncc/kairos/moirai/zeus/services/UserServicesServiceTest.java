package com.ncc.kairos.moirai.zeus.services;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.ncc.kairos.moirai.zeus.dao.JwtUserRepository;
import com.ncc.kairos.moirai.zeus.dao.UserServiceRepository;
import com.ncc.kairos.moirai.zeus.model.JwtUser;
import com.ncc.kairos.moirai.zeus.model.Service;
import com.ncc.kairos.moirai.zeus.model.ServiceAwsInstance;
import com.ncc.kairos.moirai.zeus.model.ServiceDownload;
import com.ncc.kairos.moirai.zeus.model.ServiceEndpoint;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

@TestPropertySource(locations = "classpath:test.properties")
public class UserServicesServiceTest {

    @Autowired
    UserServicesService usersServices;

    @Spy
    KairosUserService mockKairosUserService;

    @MockBean
    UserServiceRepository mockUserServiceRepository;
    
    @MockBean
    JwtUserRepository mockJwtUserRepository;

    private JwtUser jwtUser;

    private Service testService;

    void updateReflections() {
        ReflectionTestUtils.setField(usersServices, "jwtUserRepository", mockJwtUserRepository);
        ReflectionTestUtils.setField(usersServices, "kairosUserService", mockKairosUserService);
        ReflectionTestUtils.setField(usersServices, "userServiceRepository", mockUserServiceRepository);
    }
    

    @BeforeEach
    public void prepareUnitTests() {
        mockKairosUserService = Mockito.mock(KairosUserService.class);

        testService = new Service().access("Public").details("details")
        .addAwsInstancesItem(new ServiceAwsInstance().id("adwda").instanceId("KAFKA"))
        .status("Active").name("some name")
        .id("awewaeaw")
        .addDownloadsItem(new ServiceDownload().id("rgsergseg").name("download 1").uri("google.com"))
        .addEndpointsItem(new ServiceEndpoint().id("asdawdw").name("ep").uri("google2.com"));

        jwtUser = new JwtUser().teamName("Moirai").username("John").emailAddress("John@Test.com").active(true).addServicesItem(testService);
        Mockito.when(mockKairosUserService.findUserByUsername(jwtUser.getUsername())).thenReturn(jwtUser);
        updateReflections();
    }

    @Test
    public void updateServiceStatusTest() {
        Mockito.when(mockUserServiceRepository.findById(testService.getId())).thenReturn(Optional.of(testService));
        Mockito.when(mockUserServiceRepository.save(any())).thenReturn(testService);
        usersServices.updateServiceStatus(testService);
        verify(mockUserServiceRepository, times(1)).save(any());

        Mockito.when(mockUserServiceRepository.findById(testService.getId())).thenReturn(Optional.of(new Service().id("adwadaaaaw").name("name12").access("public")));
        usersServices.updateServiceStatus(testService);
        verify(mockUserServiceRepository, times(2)).save(any());
    }

    @Test
    public void saveNewServiceTest() {
        Mockito.when(mockJwtUserRepository.save(any())).thenReturn(jwtUser);
        usersServices.saveNewService(testService.id(null), jwtUser);
        verify(mockJwtUserRepository, times(1)).save(any());
    }

    @Test
    public void getServiceByNameTest() {
        List<Service> testList = new ArrayList<>();
        testList.add(testService);
        Mockito.when(mockUserServiceRepository.findByName(any())).thenReturn(testList);
        List<Service> results = usersServices.getServiceByName(any());
        assert (!results.isEmpty());
        Mockito.when(mockUserServiceRepository.findByName(any())).thenThrow(ResponseStatusException.class);
        results = usersServices.getServiceByName(any());
        assert (results.isEmpty());
    }

    @Test
    public void getFilteredServiceListTest() {
        List<Service> testList = new ArrayList<>();
        testList.add(testService);
        testList.add(new Service().name("asdad").access("public").id("awdwadada"));
        Mockito.when(mockUserServiceRepository.findByNameAndAccess(any(), any())).thenReturn(testList);
        List<Service> results = usersServices.getFilteredServiceList("test", jwtUser);
        assert (!results.isEmpty());
        
        Mockito.when(mockUserServiceRepository.findByAccess(any())).thenReturn(testList);
        results = usersServices.getFilteredServiceList("", jwtUser);
        assert (!results.isEmpty());
    }
    
}
