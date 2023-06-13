package com.ncc.kairos.moirai.zeus.utililty;

import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.*;
import com.amazonaws.util.IOUtils;
import com.ncc.kairos.moirai.zeus.model.DockerUpload;
import com.ncc.kairos.moirai.zeus.resources.Constants;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.stream.Collectors;

@Service
public class AWSS3Connector {

    private static final Logger LOGGER = Logger.getLogger(AWSS3Connector.class.getName());

    AmazonS3 s3 = getS3SynchronousClient();

    public String bucketFromURI(String URI) {
        //substring to remove s3://
        String temp = URI.substring(5);
        //substring to remove anything after bucket name
        String bucket = temp.substring(0, temp.indexOf("/"));     
        
        return bucket;
    }

    public String keyFromURI(String URI) {
        //substring to remove s3://
        String temp = URI.substring(5);        
        String key = temp.substring(temp.indexOf("/") + 1);

        return key;
    }

    public void saveFile(String data, String bucket, String key) {

        s3.putObject(bucket, key, data);
    }

    // new saveFile that takes single URI and data
    public void saveFile(String data, String URI) {
        // call old saveFile
        saveFile(data, bucketFromURI(URI), keyFromURI(URI));
    }

    public void saveFile(File file, String bucket, String key) {
        LOGGER.log(Level.INFO, () -> "Saving file to s3://" + bucket + "/" + key);

        s3.putObject(bucket, key, file);
    }

    // new saveFile that takes single URI and file
    public void saveFile(File file, String URI) {
        // call old saveFile
        saveFile(file, bucketFromURI(URI), keyFromURI(URI));
    }

    // new delete file that takes single URI
    public void deleteFile(String URI) {
        deleteFile(bucketFromURI(URI), keyFromURI(URI));
    }
     
    public void deleteFile(String bucket, String key) {
        LOGGER.log(Level.INFO, () -> "Deleting file s3://" + bucket + "/" + key);

        s3.deleteObject(bucket, key);
    }

    public boolean fileExists(String bucket, String key) throws IOException {
        AmazonS3 s3 = AmazonS3ClientBuilder.standard().build();
        return s3.doesObjectExist(bucket, key);
    }

    // new fileExists that takes a single URI
    public boolean fileExists(String URI) throws IOException {       
        return fileExists(bucketFromURI(URI), keyFromURI(URI));
    }

    public byte[] getFile(String bucket, String key) throws IOException {
        LOGGER.log(Level.INFO, () -> "Getting file s3://" + bucket + "/" + key);

        S3Object s3Object = s3.getObject(bucket, key);
        S3ObjectInputStream stream = s3Object.getObjectContent();

        byte[] content = IOUtils.toByteArray(stream);
        s3Object.close();

        return content;
    }

    // new getFile that takes a single URI
    public byte[] getFile(String URI) throws IOException {
        return getFile(bucketFromURI(URI), keyFromURI(URI));
    }

    public String getFileContents(S3ObjectSummary summary) {
        try {
            byte[] bytes = getFile(summary.getBucketName(), summary.getKey());
            return new String(bytes);
        } catch (IOException e) {
            return "unknown";
        }
    }

    public void copyFile(String fromBucket, String fromKey, String toBucket, String toKey) throws IOException {
        LOGGER.log(Level.INFO, () -> "Copying file s3://" + fromBucket + "/" + fromKey + " to " + toBucket + "/" + toKey);

        CopyObjectRequest req = new CopyObjectRequest(fromBucket, fromKey, toBucket, toKey);
        s3.copyObject(req);
    }
    // new copyFile that takes fromURI and toURI
    public void copyFile(String fromURI, String toURI) throws IOException {
        copyFile(bucketFromURI(fromURI), keyFromURI(fromURI), bucketFromURI(toURI), keyFromURI(toURI));
    }

    public void moveFile(String fromBucket, String fromKey, String toBucket, String toKey) throws IOException {
        LOGGER.log(Level.INFO, () -> "Moving file s3://" + fromBucket + "/" + fromKey + " to " + toBucket + "/" + toKey);
        copyFile(fromBucket, fromKey, toBucket, toKey);
        deleteFile(fromBucket, fromKey);
    }

    // new moveFile that takes URIs
    public void moveFile(String fromURI, String toURI) throws IOException {
        moveFile(bucketFromURI(fromURI), keyFromURI(fromURI), bucketFromURI(toURI), keyFromURI(toURI));
    }

    public List<S3ObjectSummary> listFileSummaries(String bucketName, String prefix) {
        ObjectListing listing = s3.listObjects(bucketName, prefix);
        List<S3ObjectSummary> summaries = listing.getObjectSummaries();

        while (listing.isTruncated()) {
            listing = s3.listNextBatchOfObjects(listing);
            summaries.addAll(listing.getObjectSummaries());
        }
        return summaries;
    }

    public List<DockerUpload> getDockerRegistryS3Data(String registryName) {
        List<S3ObjectSummary> summaries = listFileSummaries(Constants.DOCKER_REGISTRY_BUCKET_NAME, Constants.REPO_FOLDER + registryName);
        List<S3ObjectSummary> taggedSummaries = summaries.stream().filter(summary -> Constants.DOCKER_TAG_PATTERN.matcher(summary.getKey()).find()).collect(Collectors.toList());
        List<DockerUpload> uploads = taggedSummaries.stream()
        .map(summary ->
                new DockerUpload()
                        .registry(registryName)
                        .repo(getRepoFromKey(summary.getKey()))
                        .tag(getTagFromKey(summary.getKey()))
                        .digest(getFileContents(summary))
                        .lastModified(TimeUtil.getDatabaseTime(summary.getLastModified()))
                        .dockerimagelocation("https://" + getRegistryFromKey(summary.getKey()) + "." + Constants.DOCKER_REGISTRY_CNAME + "/" + getRepoFromKey(summary.getKey()) + ":" + getTagFromKey(summary.getKey()))
        ).collect(Collectors.toList());

        // Now compare all digests to eachother and consolidate on duplicate digest Ids.
        List<DockerUpload> duplicateUploads = uploads.stream().collect(Collectors.groupingBy(du -> du.getDigest() + "-" + du.getRepo(), Collectors.toList()))
        .values().stream().filter(i -> i.size() > 1).flatMap(j -> j.stream()).collect(Collectors.toList());
        // Remove duplicates from og list
        uploads.removeAll(duplicateUploads);
        // Create new records on duplicates
        List<DockerUpload> combinedRecords = new ArrayList<>();
        for (DockerUpload upload : duplicateUploads) {
            // If the combined list already contains the digest then combine tags
            Optional<DockerUpload> optionalRecord = combinedRecords.stream().filter(up -> up.getDigest().equals(upload.getDigest())).findFirst();
            if (optionalRecord.isPresent()) {
                DockerUpload record = optionalRecord.get();
                combinedRecords.remove(record); // remove original
                record.setTag(record.getTag() + "," + upload.getTag()); // update tag
                combinedRecords.add(record); // add new record
            } else {
                combinedRecords.add(upload);
            }
        }
        uploads.addAll(combinedRecords);

        return uploads;
    }

    public String getRegistryFromKey(String key) {
        Matcher m = Constants.DOCKER_REGISTRY_PATTERN.matcher(key);
        if (m.find()) {
            return m.group(1);
        } else {
            return "";
        }
    }

    public String getRepoFromKey(String key) {
        Matcher m = Constants.DOCKER_REPO_PATTERN.matcher(key);
        if (m.find()) {
            return m.group(1);
        } else {
            return "";
        }
    }

    public String getTagFromKey(String key) {
        Matcher m = Constants.DOCKER_TAG_PATTERN.matcher(key);
        if (m.find()) {
            return m.group(1);
        } else {
            return "";
        }
    }

    public void moveDockerUpload(String newRegistry, String oldRegistry) {
        ObjectListing objectListing = s3.listObjects(Constants.DOCKER_REGISTRY_BUCKET_NAME);
        while (objectListing.isTruncated()) {
            // Loop through all registries in S3 bucket
            List<S3ObjectSummary> s3Summary = objectListing.getObjectSummaries();
            for (S3ObjectSummary os : s3Summary) {
                if (os.getKey().startsWith(Constants.REPO_FOLDER + oldRegistry + Constants.DOCKER_REGISTRY_V2)) {
                    s3.copyObject(Constants.DOCKER_REGISTRY_BUCKET_NAME, os.getKey(), 
                    Constants.DOCKER_REGISTRY_BUCKET_NAME, os.getKey().replaceAll(oldRegistry, newRegistry));
                    // After copy delete old?
                    LOGGER.info("\nMoving record record: \n*** From: " + os.getKey() + "\n*** To: " + os.getKey().replaceAll(oldRegistry, newRegistry));
                }
            }
            objectListing = s3.listNextBatchOfObjects(objectListing);
        }
    }

    public void deleteDockerUpload(DockerUpload image) {
        String prefix = Constants.REPO_FOLDER + image.getRegistry() + Constants.DOCKER_REGISTRY_V2_REPOSITORIES + image.getRepo() + Constants.MANIFEST_TAGS + image.getTag();
        List<S3ObjectSummary> summaries = listFileSummaries(Constants.DOCKER_REGISTRY_BUCKET_NAME, prefix);
        summaries.forEach(summary -> deleteFile(Constants.DOCKER_REGISTRY_BUCKET_NAME, summary.getKey()));
    }

    private static AmazonS3 getS3SynchronousClient() {
        return AmazonS3ClientBuilder
                .standard()
                .withCredentials(new DefaultAWSCredentialsProviderChain())
                .withRegion(Regions.US_EAST_1)
                .build();
    }

}
