package com.ncc.kairos.moirai.zeus.api;

import com.ncc.kairos.moirai.zeus.dao.FeatureFlagOverrideRepository;
import com.ncc.kairos.moirai.zeus.dao.FeatureFlagRepository;
import com.ncc.kairos.moirai.zeus.model.*;
import com.ncc.kairos.moirai.zeus.services.KairosUserService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.context.request.NativeWebRequest;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@TestPropertySource(locations = "classpath:test.properties")
public class UiApiControllerTest {

    UiApiController uiApiController;

    FeatureFlagRepository featureFlagRepository;
    FeatureFlagOverrideRepository featureFlagOverrideRepository;
    KairosUserService kairosUserService;
    NativeWebRequest mockNativeWebRequest;

    private JwtUser jwtUser;

    private FeatureFlag featureFlag;

    private FeatureFlagOverride override;

    void updateReflections() {
        ReflectionTestUtils.setField(uiApiController, "featureFlagRepository", featureFlagRepository);
        ReflectionTestUtils.setField(uiApiController, "featureFlagOverrideRepository", featureFlagOverrideRepository);
        ReflectionTestUtils.setField(uiApiController, "kairosUserService", kairosUserService);
        ReflectionTestUtils.setField(uiApiController, "request", mockNativeWebRequest);
    }

    @BeforeEach
    void setup() throws ParseException {
        SimpleDateFormat dateformat2 = new SimpleDateFormat("dd-M-yyyy hh:mm:ssZ");
        Date date = dateformat2.parse("02-04-2013 11:35:42-0400");
        OffsetDateTime startDate =  date.toInstant().atOffset(ZoneOffset.UTC);
        date = dateformat2.parse("02-04-3013 11:35:42-0400");
        OffsetDateTime endDate =  date.toInstant().atOffset(ZoneOffset.UTC);

        jwtUser = new JwtUser().username("John").emailAddress("John@Test.com").active(true);
        override = new FeatureFlagOverride().id(UUID.randomUUID().toString())
        .override(false)
        .startDate(startDate)
        .endDate(endDate);
        featureFlag = new FeatureFlag().enabled(true).id(UUID.randomUUID().toString()).name("name1");

        kairosUserService = Mockito.mock(KairosUserService.class);
        mockNativeWebRequest = Mockito.mock(NativeWebRequest.class);
        featureFlagRepository = Mockito.mock(FeatureFlagRepository.class);
        featureFlagOverrideRepository = Mockito.mock(FeatureFlagOverrideRepository.class);

        Mockito.when(mockNativeWebRequest.getAttribute("token", 0)).thenReturn("token");
        Mockito.when(mockNativeWebRequest.getAttribute("jwtUser", 0)).thenReturn(this.jwtUser);
        Mockito.when(kairosUserService.findUserByUsername(jwtUser.getUsername())).thenReturn(jwtUser);

        uiApiController = new UiApiController(mockNativeWebRequest);
        updateReflections();
    }

    @Test
    void getRequestTest() {
        Optional<NativeWebRequest> result = uiApiController.getRequest();
        assert result.get() != null;
    }

    @Test
    void getFeatureFlagsTes() {
        List<FeatureFlag> list = new ArrayList<>();
        list.add(featureFlag);
        Mockito.when(featureFlagRepository.findAll()).thenReturn(list);
        ResponseEntity<List<FeatureFlag>> response = uiApiController.getFeatureFlags();
        assert !response.getBody().isEmpty();
    }

    @Test
    void getActiveFeatureFlagsTest() {
        List<FeatureFlag> list = new ArrayList<>();
        list.add(featureFlag);
        Mockito.when(featureFlagRepository.findAll()).thenReturn(list);
        // Base feature flag
        ResponseEntity<List<FeatureFlag>> response = uiApiController.getActiveFeatureFlags();
        assert !response.getBody().isEmpty();

        list = new ArrayList<>();
        list.add(featureFlag.addOverridesItem(override));
        Mockito.when(featureFlagRepository.findAll()).thenReturn(list);
        response = uiApiController.getActiveFeatureFlags();
        // Override test
        assert !response.getBody().isEmpty();
        assert !response.getBody().get(0).getEnabled();
    }

    @Test
    void newFeatureFlagTest() {
        Mockito.when(featureFlagRepository.findByName(featureFlag.getName())).thenReturn(featureFlag);
        Mockito.when(featureFlagOverrideRepository.save(override)).thenReturn(override);
        
        // Already exists
        ResponseEntity<StringResponse> response = uiApiController.newFeatureFlag(featureFlag);
        assert response.getStatusCode().equals(HttpStatus.CONFLICT);

        Mockito.when(featureFlagRepository.findByName(featureFlag.getName())).thenReturn(null);
        Mockito.when(featureFlagRepository.save(featureFlag)).thenReturn(featureFlag.addOverridesItem(override));
        // Happy Path
        response = uiApiController.newFeatureFlag(featureFlag);
        assert response.getStatusCode().equals(HttpStatus.OK);
    }

    @Test
    void updateFeatureFlagTest() {
        Mockito.when(featureFlagRepository.save(featureFlag)).thenReturn(featureFlag);
        Mockito.when(featureFlagRepository.findById(featureFlag.getId())).thenReturn(Optional.of(featureFlag.addOverridesItem(override)));

        // Happy Path with override
        ResponseEntity<StringResponse> response = uiApiController.updateFeatureFlag(featureFlag.getId(), featureFlag);
        assert response.getStatusCode().equals(HttpStatus.OK);
    }

    @Test
    void deleteFeatureFlagTest() {
        Mockito.doNothing().when(featureFlagOverrideRepository).delete(override);
        Mockito.doNothing().when(featureFlagRepository).delete(featureFlag);
        Mockito.when(featureFlagRepository.findById(featureFlag.getId())).thenReturn(Optional.of(featureFlag.addOverridesItem(override)));
        Mockito.when(featureFlagRepository.existsById(featureFlag.getId())).thenReturn(true);
        ResponseEntity<StringResponse> response = uiApiController.deleteFeatureFlag(featureFlag);
        assert response.getStatusCode().equals(HttpStatus.OK);

        Mockito.when(featureFlagRepository.existsById(featureFlag.getId())).thenReturn(false);
        response = uiApiController.deleteFeatureFlag(featureFlag);
        assert response.getStatusCode().equals(HttpStatus.NOT_FOUND);
    }

    
}
