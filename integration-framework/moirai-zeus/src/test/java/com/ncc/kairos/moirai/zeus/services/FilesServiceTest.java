package com.ncc.kairos.moirai.zeus.services;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.amazonaws.services.s3.model.S3ObjectSummary;
import com.ncc.kairos.moirai.zeus.dao.JobRequestRepository;
import com.ncc.kairos.moirai.zeus.dao.StoredFileRepository;
import com.ncc.kairos.moirai.zeus.jobs.JobRequestHandler;
import com.ncc.kairos.moirai.zeus.model.JobRequest;
import com.ncc.kairos.moirai.zeus.model.StoredFile;
import com.ncc.kairos.moirai.zeus.model.ValidationResponse;
import com.ncc.kairos.moirai.zeus.resources.EnvironmentTier;
import com.ncc.kairos.moirai.zeus.resources.JobRequestTypes;
import com.ncc.kairos.moirai.zeus.utililty.AWSS3Connector;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

@TestPropertySource(locations = "classpath:test.properties")
public class FilesServiceTest {

    @Autowired
    FilesService filesService;

    @MockBean
    StoredFileRepository mockStoredFileRepository;

    @MockBean
    JobRequestRepository mockJobRequestRepository;

    @MockBean
    AWSS3Connector mockAWSS3Connector;

    @Spy
    PropertiesService mockPropertiesService;

    private StoredFile storedFile;

    private List<StoredFile> storedFiles = new ArrayList<>();

    void updateReflections() {
        ReflectionTestUtils.setField(filesService, "storedFileRepository", mockStoredFileRepository);
        ReflectionTestUtils.setField(filesService, "jobRequestRepository", mockJobRequestRepository);
        ReflectionTestUtils.setField(filesService, "propertiesService", mockPropertiesService);
        ReflectionTestUtils.setField(filesService, "awsS3Connector", mockAWSS3Connector);
    }

    @BeforeEach
    public void prepareUnitTests() {
        storedFile = new StoredFile().canSubmit(true).category("category").experiment("someEp").filename("fileName").uri("somethingsomething")
                .humanReadableDone(true).id("afgegjiuljhiurgf").owner("owner");
        storedFiles.add(storedFile);

        mockPropertiesService = Mockito.mock(PropertiesService.class);
        mockAWSS3Connector = Mockito.mock(AWSS3Connector.class);
        updateReflections();
    }

    @Test
    public void getAllTest() {
        Mockito.when(mockStoredFileRepository.findAll()).thenReturn(storedFiles);
        List<StoredFile> results = filesService.getAll();
        assert (!results.isEmpty());
    }

    @Test
    public void getStoredFileTest() {
        Mockito.when(mockStoredFileRepository.findById(any())).thenReturn(Optional.of(storedFile));
        StoredFile result = filesService.getStoredFile(any());
        assert (result != null);
    }

    @Test
    public void getFileByIdTest() {
        List<StoredFile> returnMe = new ArrayList<>();
        Mockito.when(mockStoredFileRepository.findAllByOwnerAndId(any(), any())).thenReturn(returnMe);
        // Throws error on empty list
        assertThrows(ResponseStatusException.class, () -> {
            filesService.getFileById(any(), any());
        });

        returnMe.add(storedFile);
        returnMe.add(storedFile);
        Mockito.when(mockStoredFileRepository.findAllByOwnerAndId(any(), any())).thenReturn(returnMe);
        // Throws error if more than 1 result returned
        assertThrows(ResponseStatusException.class, () -> {
            filesService.getFileById(any(), any());
        });

        // No error and get a return
        Mockito.when(mockStoredFileRepository.findAllByOwnerAndId(any(), any())).thenReturn(storedFiles);
        StoredFile result = filesService.getFileById(any(), any());
        assert (result != null);
    }

    @Test
    public void findOrDeleteTest() throws IOException {
        doNothing().when(mockStoredFileRepository).deleteById(any());
        Mockito.when(mockAWSS3Connector.fileExists(any(), any())).thenReturn(false);
        filesService.findOrDelete(storedFiles);
        verify(mockStoredFileRepository, times(1)).deleteById(any());
    }

    @Test
    public void generateUriForFileTest() {
        Mockito.when(mockPropertiesService.whichEnvironment()).thenReturn(EnvironmentTier.TESTING);
        Mockito.when(mockPropertiesService.getPerformerBaseKey()).thenReturn("baseKey");
        String result = "";
        result = filesService.generateUriForFile(storedFile);
        assert (!result.isEmpty());
    }

    @Test
    public void makeFileRecordTest() {
        Mockito.when(mockPropertiesService.whichEnvironment()).thenReturn(EnvironmentTier.TESTING);
        Mockito.when(mockPropertiesService.getPerformerBaseKey()).thenReturn("baseKey");
        StoredFile result = filesService.makeFileRecord("fileName", "newOwner", "experiment", "category", true, null, true);
        assert (result != null);
        assert (result.getOwner().equals("newOwner"));
    }

    @Test
    public void saveTest() {
        Mockito.when(mockStoredFileRepository.save(any())).thenReturn(storedFile);
        filesService.save(storedFile);
        verify(mockStoredFileRepository, times(1)).save(any());
    }

     @Test
     public void saveNewFileTest() {
         Mockito.when(mockPropertiesService.whichEnvironment()).thenReturn(EnvironmentTier.TESTING);
         Mockito.when(mockPropertiesService.getPerformerBaseKey()).thenReturn("baseKey");
         Mockito.when(mockPropertiesService.getPerformerDataBucket()).thenReturn("bucket");
         doNothing().when(mockAWSS3Connector).saveFile((File) any(), any(), any());
         Mockito.when(mockStoredFileRepository.save(any())).thenReturn(storedFile);

         List<StoredFile> matchingFiles = new ArrayList<>();
         Mockito.when(mockStoredFileRepository.findAllByUri(any())).thenReturn(matchingFiles);

         filesService.saveNewFile(null, "something", "another", "somethingelse", "dawdwa", "adawda", true);
         verify(mockStoredFileRepository, times(1)).save(any());

         matchingFiles.add(storedFile);
         assertThrows(ResponseStatusException.class, () ->
                 filesService.saveNewFile(null, "something", "another", "somethingelse", "dawdwa", "adawda", true)
         );
     }

     @Test
     public void addValidationTest() {
        Mockito.when(mockStoredFileRepository.findById(any())).thenReturn(Optional.of(storedFile));
        doNothing().when(mockAWSS3Connector).saveFile((File) any(), any(), any());
        ValidationResponse response = new ValidationResponse();
        
        response.setErrorsList(new ArrayList<String>());
        response.setFatalList(new ArrayList<String>());
        try {
            filesService.addValidation("0", response);
        } catch (Exception e) {
            fail("addValidation should not throw any exception");
        }
        assert (storedFile.getCanSubmit());
        
        response.addErrorsListItem("an error");
        try {
        filesService.addValidation("0", response);
        } catch (Exception e) {
            fail("addValidation should not throw any exception");
        } 
        assert (!storedFile.getCanSubmit());
     }

    @Test
    public void assertUniqueFileTest() {
        Mockito.when(mockStoredFileRepository.findAllByUri(any())).thenReturn(new ArrayList<>());
        filesService.assertUniqueFile(any());

        Mockito.when(mockStoredFileRepository.findAllByUri(any())).thenReturn(storedFiles);
        assertThrows(ResponseStatusException.class, () -> {
            filesService.assertUniqueFile(any());
        });
    }

    @Test
    public void deleteFileByOwnerAndIdTest() {
        Mockito.when(mockStoredFileRepository.findAllByOwnerAndId(any(), any())).thenReturn(storedFiles);
        Mockito.when(mockPropertiesService.getPerformerDataBucket()).thenReturn("someString");
        
        Mockito.doNothing().when(mockAWSS3Connector).deleteFile(anyString(), anyString());
        Mockito.doNothing().when(mockStoredFileRepository).deleteById(anyString());

        filesService.deleteFileByOwnerAndId("someString", "String2");
        verify(mockAWSS3Connector, times(3)).deleteFile(anyString());
    }

    @Test
    public void createValidationJobTesT() {
        JobRequest request = new JobRequest().attempt(3).attemptLimit(3).id("adwadwad").requestType(JobRequestTypes.VALIDATION.name());
        Map<String, String> tempMap = new HashMap<>();
        try (MockedStatic<JobRequestHandler> mocked = Mockito.mockStatic(JobRequestHandler.class)) {
            mocked.when(() -> { 
                JobRequestHandler.makeJobRequest(JobRequestTypes.VALIDATION, 3, tempMap); 
            }).thenReturn(request);
            filesService.createValidationJob(storedFile);
            verify(mockJobRequestRepository, times(1)).save(any());
        }
    }

    @Test
    public void createHumanReadableJobTest() {
        JobRequest request = new JobRequest().attempt(3).attemptLimit(3).id("adwadwad").requestType(JobRequestTypes.HUMAN_READABLE.name());
        Map<String, String> tempMap = new HashMap<>();
        try (MockedStatic<JobRequestHandler> mocked = Mockito.mockStatic(JobRequestHandler.class)) {
            mocked.when(() -> { 
                JobRequestHandler.makeJobRequest(JobRequestTypes.HUMAN_READABLE, 3, tempMap); 
            }).thenReturn(request);
            filesService.createHumanReadableJob(storedFile);
            verify(mockJobRequestRepository, times(1)).save(any());
        }
    }

    @Test
    void findOrAddTest() {
        Mockito.when(mockStoredFileRepository.save(any())).thenReturn(storedFile);
        Mockito.when(mockPropertiesService.getPerformerDataBucket()).thenReturn("someString");
        Mockito.when(mockPropertiesService.getPerformerBaseKey()).thenReturn("perfKey");
        Mockito.when(mockPropertiesService.whichEnvironment()).thenReturn(EnvironmentTier.TESTING);
        // Testing new creation of file
        Mockito.when(mockStoredFileRepository.findAllByUri(any())).thenReturn(new ArrayList<>());

        List<S3ObjectSummary> mockedReturn = new ArrayList<>();
        S3ObjectSummary e = new S3ObjectSummary();
        e.setBucketName("testBucket");
        e.setKey("SomeAmbiguousKey/attr2/attr3/attr4/attr5/attr6");
        mockedReturn.add(e);

        Mockito.when(mockAWSS3Connector.listFileSummaries(anyString(), anyString())).thenReturn(mockedReturn);

        filesService.findOrAdd();
        // Verify new file saved
        verify(mockStoredFileRepository, times(1)).save(any());
    }
}
