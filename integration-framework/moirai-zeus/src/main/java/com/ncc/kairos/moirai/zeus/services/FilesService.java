package com.ncc.kairos.moirai.zeus.services;

import com.amazonaws.services.s3.model.S3ObjectSummary;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ncc.kairos.moirai.zeus.dao.JobRequestRepository;
import com.ncc.kairos.moirai.zeus.dao.StoredFileRepository;
import com.ncc.kairos.moirai.zeus.jobs.JobRequestHandler;
import com.ncc.kairos.moirai.zeus.model.JobRequest;
import com.ncc.kairos.moirai.zeus.model.StoredFile;
import com.ncc.kairos.moirai.zeus.model.ValidationResponse;
import com.ncc.kairos.moirai.zeus.resources.JobRequestTypes;
import com.ncc.kairos.moirai.zeus.utililty.AWSS3Connector;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import javax.transaction.Transactional;
import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.util.*;
import java.util.logging.Logger;

@Service
@Transactional
public class FilesService {

    @Autowired
    StoredFileRepository storedFileRepository;

    @Autowired
    JobRequestRepository jobRequestRepository;

    @Autowired
    PropertiesService propertiesService;

    @Autowired
    AWSS3Connector awsS3Connector;

    private static final String VALIDATION_URI_EXTENSION = ".validation";
    private static final String HUMANREADABLE_URI_EXTENSION = ".humanreadable";

    private static final Logger LOGGER = Logger.getLogger(FilesService.class.getName());

    public List<StoredFile> getAll() {
        return storedFileRepository.findAll();
    }

    public StoredFile getStoredFile(String id) {
        return storedFileRepository.findById(id).get();
    }

    public StoredFile getFileById(String owner, String id) {
        List<StoredFile> files = storedFileRepository.findAllByOwnerAndId(owner, id);
        if (files.size() > 1) {
            throw new ResponseStatusException(HttpStatus.EXPECTATION_FAILED, "Expected one file matching owner " + owner + " and id " + id + " but found " + files.size());
        } else if (files.size() == 0) {
            throw new ResponseStatusException(HttpStatus.EXPECTATION_FAILED, "No file matching owner " + owner + " and id " + id);
        } else {
            return files.get(0);
        }
    }

    public byte[] downloadFile(String id) throws IOException {
        StoredFile file = storedFileRepository.findById(id).get();
        return downloadFile(file);
    }

    public byte[] downloadFile(StoredFile file) throws IOException {
        return awsS3Connector.getFile(file.getUri());
    }

    public byte[] downloadFileValidation(StoredFile file) throws IOException {
        return awsS3Connector.getFile(file.getUri() + VALIDATION_URI_EXTENSION);
    }

    public byte[] downloadFileHumanReadable(StoredFile file) throws IOException {
        return awsS3Connector.getFile(file.getUri() + HUMANREADABLE_URI_EXTENSION);
    }

    public void findOrDelete(List<StoredFile> files) {
        findOrDelete(files, 3);
    }

    private void findOrDelete(List<StoredFile> files, int tries) {
        List<StoredFile> missingFiles = new ArrayList<>();
        List<StoredFile> errors = new ArrayList<>();

        // Collect a list of files that exist in the database but not on s3.
        for (StoredFile file: files) {
            try {
                boolean exists = awsS3Connector.fileExists(file.getUri());
                if (!exists) {
                    missingFiles.add(file);
                }
            } catch (Exception e) {
                errors.add(file);
            }
        }
        // Double check that the missing file is actually missing before deleting.
        for (StoredFile missingFile: missingFiles) {
            try {
                boolean exists = awsS3Connector.fileExists(missingFile.getUri());
                if (!exists) {
                    LOGGER.info("Deleting file: " + missingFile.getUri());
                    storedFileRepository.deleteById(missingFile.getId());
                }
            } catch (Exception e) {
                LOGGER.info(e.getMessage());
            }
        }

        if (tries >= 0) {
            findOrDelete(errors, tries - 1);
        }
    }

    public void findOrAdd() {
        StringBuilder pathBuilder = new StringBuilder();
        pathBuilder.append(propertiesService.whichEnvironment().toString()).append("/");
        pathBuilder.append(propertiesService.getPerformerBaseKey()).append("/");
        List<S3ObjectSummary> s3ObjectSummaries = awsS3Connector.listFileSummaries(propertiesService.getPerformerDataBucket(), pathBuilder.toString().toLowerCase());
        for (S3ObjectSummary summary: s3ObjectSummaries) {
            if (!summary.getKey().endsWith(VALIDATION_URI_EXTENSION) && !summary.getKey().endsWith("/")) {
                String fileUri = uriFromKey(summary.getBucketName(), summary.getKey());
                List<StoredFile> matchingFiles = storedFileRepository.findAllByUri(fileUri);
                if (matchingFiles.size() == 0) {
                    LOGGER.info("Creating record for " + fileUri);
                    String[] splitKey = summary.getKey().split("/");
                    StoredFile newFile = new StoredFile()
                            .uri(fileUri)
                            .experiment(splitKey[2])
                            .category(splitKey[3])
                            .owner(splitKey[4])
                            .filename(String.join("/", Arrays.copyOfRange(splitKey, 5, splitKey.length)))
                            .dateReceived(LocalDate.of(1999, 12, 31))
                            .publicAccess(true)
                            .canSubmit(false)
                            .humanReadableDone(false)
                            .validationDone(false)
                            .extraData("{}");
                    storedFileRepository.save(newFile);
                }
            } else if (summary.getKey().endsWith(VALIDATION_URI_EXTENSION)) {
                String baseFile = summary.getKey().substring(0, summary.getKey().length() - VALIDATION_URI_EXTENSION.length());
                try {
                    if (!awsS3Connector.fileExists(propertiesService.getPerformerDataBucket(), baseFile)) {
                        awsS3Connector.deleteFile(propertiesService.getPerformerDataBucket(), summary.getKey());
                    }
                } catch (Exception e) {
                    LOGGER.severe(e.getMessage());
                }
            }

        }
    }

    public void copyFile(StoredFile fromFile, StoredFile toFile) throws IOException {
        awsS3Connector.copyFile(fromFile.getUri(), toFile.getUri());
    }

    public void addValidation(String id, ValidationResponse validation) throws JsonProcessingException {
        StoredFile file = getStoredFile(id);
        ObjectMapper mapper = new ObjectMapper();
        String jsonString = mapper.writeValueAsString(validation);
        awsS3Connector.saveFile(jsonString, file.getUri() + VALIDATION_URI_EXTENSION);
        file.setValidationDone(true);
        int errorCount = validation.getErrorsList().size() + validation.getFatalList().size();
        file.setCanSubmit(errorCount == 0);


        storedFileRepository.save(file);
    }

    public void setValidationDone(String id) {
        StoredFile file = getStoredFile(id);
        file.setValidationDone(true);
        storedFileRepository.save(file);
    }

    public List<StoredFile> getBaseFilesByOwner(String owner) {
        return storedFileRepository.findAllByOwner(owner);
    }

    public List<StoredFile> getBaseFilesByExperiment(String experiment) {
        return storedFileRepository.findAllByExperiment(experiment);
    }

    public List<StoredFile> getBaseFilesByOwnerAndExperiment(String owner, String experiment) {
        return storedFileRepository.findAllByOwnerAndExperiment(owner, experiment);
    }

    public List<StoredFile> getBaseFilesWithPublicAccess(String experiment) {
        return storedFileRepository.findAllByPublicAccessAndExperiment(true, experiment);
    }

    public String generateUriForFile(StoredFile file) {
        StringBuilder pathBuilder = new StringBuilder();
        pathBuilder.append("s3://");
        pathBuilder.append(propertiesService.getPerformerDataBucket()).append("/");
        pathBuilder.append(propertiesService.whichEnvironment().toString()).append("/");
        pathBuilder.append(propertiesService.getPerformerBaseKey()).append("/");
        pathBuilder.append(file.getExperiment()).append("/");
        pathBuilder.append(file.getCategory()).append("/");
        pathBuilder.append(file.getOwner()).append("/");
        
        StringBuilder uriBuilder = new StringBuilder(pathBuilder.toString().toLowerCase());
        uriBuilder.append(file.getFilename());
        return uriBuilder.toString().replace(" ", "_");
    }

    public StoredFile makeFileRecord(String filename, String owner, String experiment, String category, Boolean isBase, String extraData, Boolean display) {
        StoredFile newFile = new StoredFile();
        newFile.setFilename(filename);
        newFile.setOwner(owner);
        newFile.setExperiment(experiment);
        newFile.setCategory(category);

        newFile.setDisplay(display);
        newFile.setValidationDone(false);
        newFile.setHumanReadableDone(false);
        newFile.setExtraData(extraData);
        newFile.setPublicAccess(false);
        newFile.canSubmit(false);
        if (newFile.getDateReceived() == null) {
            newFile.dateReceived(LocalDate.now());
        }
        newFile.setUri(generateUriForFile(newFile));
        return newFile;
    }

    public StoredFile save(StoredFile file) {
        storedFileRepository.save(file);
        return file;
    }

    public StoredFile saveNewFile(File file, String filename, String owner, String experiment, String category, String extraData, boolean display) {
        try {
            StoredFile newFile = makeFileRecord(filename, owner, experiment, category, true, extraData, display);
            assertUniqueFile(newFile.getUri());
            awsS3Connector.saveFile(file, newFile.getUri());
            storedFileRepository.save(newFile);
            return newFile;
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Unable to save file");
        }
    }

    public void deleteFileByOwnerAndId(String owner, String id) {
        StoredFile file = getFileById(owner, id);
        awsS3Connector.deleteFile(file.getUri());
        awsS3Connector.deleteFile(file.getUri() + VALIDATION_URI_EXTENSION);
        awsS3Connector.deleteFile(file.getUri() + HUMANREADABLE_URI_EXTENSION);
        storedFileRepository.deleteById(file.getId());
    }

    public static File convertMultiPartFile(MultipartFile mFile) throws IOException {
        File file = File.createTempFile("temp", ".temp");
        file.deleteOnExit();
        mFile.transferTo(file);
        return file;
    }

    public void createValidationJob(StoredFile file) {
        Map<String, String> requestParams = Collections.singletonMap("fileId", file.getId());
        JobRequest request = JobRequestHandler.makeJobRequest(JobRequestTypes.VALIDATION, 3, requestParams);
        jobRequestRepository.save(request);
    }

    public void createHumanReadableJob(StoredFile file) {
        Map<String, String> requestParams = Collections.singletonMap("fileId", file.getId());
        JobRequest request = JobRequestHandler.makeJobRequest(JobRequestTypes.HUMAN_READABLE, 3, requestParams);
        jobRequestRepository.save(request);
    }

    /**
     * Throws an exception if a file exists with the given uri.
     *
     * @param uri the uri to look for
     */
    public void assertUniqueFile(String uri) {
        List<StoredFile> matchingFiles = storedFileRepository.findAllByUri(uri);
        if (matchingFiles.size() > 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Error: file already exists.");
        }
    }

    public void copySnapshotToExperiment(String snapshot, String experiment) throws IOException {
        List<StoredFile> ta1Files = storedFileRepository.findAllByExperiment(snapshot);
        for (StoredFile file : ta1Files) {
            String owner = file.getOwner().toLowerCase().replace("lestat", "isi"); //TODO make this unnecessary
            String pathName = experiment + "/experiment-input/" + file.getCategory() + "/" + owner + "/" + file.getFilename();
            pathName = pathName.replace(" ", "_").toLowerCase();
            awsS3Connector.copyFile(file.getUri(), propertiesService.getExperimentDataBucket() + pathName);
        }
    }

    public String uriFromKey(String key) {
        return uriFromKey(propertiesService.getPerformerDataBucket(), key);
    }

    public String uriFromKey(String bucket, String key) {
        if (key.startsWith("s3://")) {
            return key;
        }
        StringBuilder builder = new StringBuilder();
        builder.append("s3://");
        builder.append(bucket);
        builder.append("/");
        builder.append(key);
        return builder.toString();
    }
    /**
     * Compares the path the file should be on to the path the file is one and moves the file and associated files.
     */
    public void correctPaths() {
        List<StoredFile> files = storedFileRepository.findAll();
        for (StoredFile file: files) {
            String expectedUri = generateUriForFile(file);
            if (!expectedUri.equals(file.getUri())) {
                LOGGER.info("Moving " + file.getUri() + " -> " + expectedUri);
                try {
                    assertUniqueFile(expectedUri);
                    awsS3Connector.moveFile(file.getUri(), expectedUri);
                    if (awsS3Connector.fileExists(file.getUri() + VALIDATION_URI_EXTENSION)) {
                        awsS3Connector.moveFile(file.getUri() + VALIDATION_URI_EXTENSION, expectedUri + VALIDATION_URI_EXTENSION);
                    }
                    if (awsS3Connector.fileExists(file.getUri() + HUMANREADABLE_URI_EXTENSION)) {
                        awsS3Connector.moveFile(file.getUri() + HUMANREADABLE_URI_EXTENSION, expectedUri + HUMANREADABLE_URI_EXTENSION);
                    }
                    file.setUri(expectedUri);
                    save(file);
                } catch (Exception e) {
                    LOGGER.severe("Failed Moving " + file.getUri() + " -> " + expectedUri);
                }
            }
        }
    }
}
