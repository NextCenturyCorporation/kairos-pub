package com.ncc.kairos.moirai.zeus.api;

import com.ncc.kairos.moirai.zeus.model.*;
import com.ncc.kairos.moirai.zeus.services.FilesService;
import com.ncc.kairos.moirai.zeus.services.KairosUserService;
import com.ncc.kairos.moirai.zeus.services.PropertiesService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.AdditionalAnswers;
import org.mockito.Mockito;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import org.springframework.web.context.request.NativeWebRequest;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@TestPropertySource(locations = "classpath:test.properties")
class FilesApiControllerTest {

    FilesApiController filesApiController;

    NativeWebRequest mockNativeWebRequest;

    KairosUserService mockKairosUserService;

    FilesService mockFilesService;

    private JwtUser jwtUser;

    private JwtPermission testPerm;

    private JwtRole testRole;

    private StoredFile storedFile;

    private List<StoredFile> storedFiles = new ArrayList<>();

    private PropertiesService mockPropertiesService;

    private MockMvc mockMvc;

    void updateReflections() {
        ReflectionTestUtils.setField(filesApiController, "request", mockNativeWebRequest);
        ReflectionTestUtils.setField(filesApiController, "kairosUserService", mockKairosUserService);
        ReflectionTestUtils.setField(filesApiController, "filesService", mockFilesService);
        ReflectionTestUtils.setField(filesApiController, "propertiesService", mockPropertiesService);
    }

    @BeforeEach
    void setup() {
        storedFile = new StoredFile().canSubmit(true).category("category").experiment("someEp").filename("fileName")
                .humanReadableDone(true).id("afgegjiuljhiurgf").owner("owner").uri("path/file.json");
        storedFiles.add(storedFile);

        testPerm = new JwtPermission().description("soawda").id(UUID.randomUUID().toString()).name("coolNameBro");
        testRole = new JwtRole().description("Cool Role Bro").id(UUID.randomUUID().toString()).name("Cooler Name Bro")
                .addPermissionsItem(testPerm);
        jwtUser = new JwtUser().username("John").emailAddress("John@Test.com").active(true).addRolesItem(testRole);

        mockNativeWebRequest = Mockito.mock(NativeWebRequest.class);
        mockKairosUserService = Mockito.mock(KairosUserService.class);
        mockFilesService = Mockito.mock(FilesService.class);

        Mockito.when(mockNativeWebRequest.getAttribute("token", 0)).thenReturn("token");
        Mockito.when(mockNativeWebRequest.getAttribute("jwtUser", 0)).thenReturn(this.jwtUser);
        Mockito.when(mockKairosUserService.findUserByUsername(jwtUser.getUsername())).thenReturn(jwtUser);
        Mockito.doNothing().when(mockKairosUserService).assertUserExists(any());

        filesApiController = new FilesApiController(mockNativeWebRequest);
        mockMvc = MockMvcBuilders.standaloneSetup(filesApiController).build();
        updateReflections();
    }

    @Test
    void deleteFileTest() throws Exception {
        Mockito.doNothing().when(mockFilesService).deleteFileByOwnerAndId(anyString(), anyString());
        mockMvc.perform(delete("/files/{id}", "someId")).andExpect(status().isOk());
    }

    @Test
    void getFilesTest() throws Exception {
        Mockito.when(mockFilesService.getBaseFilesByOwnerAndExperiment(anyString(), anyString()))
                .thenReturn(storedFiles);
        mockMvc.perform(get("/files").param("experiment", "awadwadwa")).andExpect(status().isOk());
    }

    @Test
    void getFilesByExperimentTest() throws Exception {
        Mockito.when(mockFilesService.getBaseFilesWithPublicAccess(anyString())).thenReturn(storedFiles);
        mockMvc.perform(get("/files/experiment").param("experiment", "awadwadwa")).andExpect(status().isOk());
    }

    // @Test
    // void downloadFileTest() throws Exception {
    //     byte[] temp = "someString".getBytes();
    //     Mockito.when(mockFilesService.getStoredFile(storedFile.getId())).thenReturn(storedFile);
    //     Mockito.when(mockFilesService.downloadFile(anyString())).thenReturn(temp);
    //     mockMvc.perform(get("/files/{id}", storedFile.getId())).andExpect(status().isInternalServerError());
    //     verify(mockFilesService, times(1)).downloadFile(anyString());
    // }
    
    @Test
    void displayFilesTest() throws Exception {
        byte[] temp = "someString".getBytes(StandardCharsets.UTF_8);

        storedFile.humanReadableDone(true);
        storedFile.validationDone(true);
        Mockito.when(mockFilesService.getStoredFile(anyString())).thenReturn(storedFile);
        Mockito.when(mockFilesService.downloadFile(anyString())).thenReturn(temp);
        Mockito.when(mockFilesService.downloadFileValidation(any())).thenReturn(temp);
        Mockito.when(mockFilesService.downloadFileHumanReadable(any())).thenReturn(temp);

        mockMvc.perform(get("/files/{id}/display", storedFile.getId())).andExpect(status().isInternalServerError());

    }

    @Test
    void experimentSnapshotTest() throws Exception {
        Mockito.when(mockFilesService.getBaseFilesWithPublicAccess(anyString())).thenReturn(storedFiles);
        Mockito.doNothing().when(mockFilesService).copyFile(any(), any());
        Mockito.when(mockFilesService.save(any())).thenReturn(storedFile);
        mockMvc.perform(post("/files/experiment/snapshot").param("experiment", "awadwadwa")).andExpect(status().isOk());
    }

    @Test
    void syncFilesTest() throws Exception {
        Mockito.when(mockFilesService.getAll()).thenReturn(storedFiles);
        Mockito.when(mockFilesService.save(any())).then(AdditionalAnswers.returnsFirstArg());
        Mockito.doNothing().when(mockFilesService).findOrDelete(any());
        Mockito.doNothing().when(mockFilesService).findOrAdd();
        Mockito.doNothing().when(mockFilesService).correctPaths();

        mockMvc.perform(post("/files/sync")).andExpect(status().isOk());        
    }

}