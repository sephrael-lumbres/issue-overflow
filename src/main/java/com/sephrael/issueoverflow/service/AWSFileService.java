package com.sephrael.issueoverflow.service;

import com.sephrael.issueoverflow.configuration.S3Properties;
import com.sephrael.issueoverflow.entity.AWSFile;
import com.sephrael.issueoverflow.entity.Issue;
import com.sephrael.issueoverflow.entity.Project;
import com.sephrael.issueoverflow.entity.User;
import com.sephrael.issueoverflow.repository.AWSFileRepository;
import com.sephrael.issueoverflow.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;
import java.util.UUID;

@Service
public class AWSFileService {

    private final S3Client s3Client;
    private final String bucketName;
    private final UserRepository userRepository;
    private final AWSFileRepository awsFileRepository;
    private static final Logger logger = LoggerFactory.getLogger(AWSFileService.class);

    public AWSFileService(S3Client s3Client, S3Properties s3Properties,
                          UserRepository userRepository, AWSFileRepository awsFileRepository) {
        this.s3Client = s3Client;
        this.bucketName = s3Properties.bucket();
        this.userRepository = userRepository;
        this.awsFileRepository = awsFileRepository;
    }

    public void checkUserProfilePicture(User user, MultipartFile multipartFile, boolean isProfilePicture) {
        AWSFile awsFile = awsFileRepository.findByUserAndIsProfilePictureTrue(user);

        if(user.hasProfilePicture() && isProfilePicture) {
            deleteObject(awsFile.getFileKey());
        }
        if(!user.hasProfilePicture() && isProfilePicture) {
            user.setHasProfilePicture(true);
            userRepository.save(user);
        }

        uploadFile(multipartFile, user, null, isProfilePicture);
    }

    public void uploadFile(MultipartFile multipartFile, User user, Issue issue, boolean isProfilePicture) {
        String fileName = multipartFile.getOriginalFilename();

        // a random key; the original file name is kept in the AWSFile record
        String fileKey = UUID.randomUUID().toString();

        String contentType = multipartFile.getContentType();
        long fileSize = multipartFile.getSize();

        // Uploads are capped at 5 MB, so the whole file is held in memory. A byte
        // array body can be re-sent, so the SDK can retry a failed upload.
        byte[] content;
        try {
            content = multipartFile.getBytes();
        } catch (IOException ioException) {
            logger.error("IOException: {}", ioException.getMessage());
            return;
        }

        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(fileKey)
                .contentType(contentType)
                .contentLength((long) content.length)
                .build();

        try {
            s3Client.putObject(putObjectRequest, RequestBody.fromBytes(content));
        } catch (SdkException sdkException) {
            logger.info("S3 upload failed: {}", sdkException.getMessage());
            throw sdkException;
        }

        // creates and saves an "AWSFile" that will store metadata of the file to be stored in AWS S3
        String url = s3Client.utilities().getUrl(request -> request.bucket(bucketName).key(fileKey)).toExternalForm();
        AWSFile awsFile = new AWSFile(fileName, fileKey, contentType, fileSize, url, isProfilePicture);
        user.addUserToAWSFile(awsFile);

        // if file is a profile picture, store the URL with the user
        if(isProfilePicture) {
            user.setProfilePictureURL(url);
            userRepository.save(user);
        }

        // connect AWSFile to an issue and its project
        if(issue != null) {
            issue.addAWSFileToIssue(awsFile);
            issue.getProject().addAWSFileToProject(awsFile);
        }

        awsFileRepository.save(awsFile);
    }

    public void deleteFile(String fileKey) {
        deleteObject(fileKey);
        awsFileRepository.delete(awsFileRepository.findByFileKey(fileKey));
    }

    public void deleteAllFilesByUser(User user) {
        for(AWSFile awsFile : awsFileRepository.findByUser(user)) {
            deleteObject(awsFile.getFileKey());
        }
    }

    public void deleteAllFilesByProject(Project project) {
        for(AWSFile awsFile : awsFileRepository.findByProject(project)) {
            deleteObject(awsFile.getFileKey());
        }
    }

    public void deleteAllFilesByIssue(Issue issue) {
        for(AWSFile awsFile : awsFileRepository.findByIssue(issue)) {
            deleteObject(awsFile.getFileKey());
        }
    }

    private void deleteObject(String fileKey) {
        s3Client.deleteObject(request -> request.bucket(bucketName).key(fileKey));
    }
}
