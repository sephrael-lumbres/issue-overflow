package com.sephrael.issueoverflow.service;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.*;
import com.sephrael.issueoverflow.entity.AWSFile;
import com.sephrael.issueoverflow.entity.Issue;
import com.sephrael.issueoverflow.entity.Project;
import com.sephrael.issueoverflow.entity.User;
import com.sephrael.issueoverflow.repository.AWSFileRepository;
import com.sephrael.issueoverflow.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.UUID;

@Service
public class AWSFileService {
    @Autowired
    private AmazonS3 s3Client;

    @Autowired
    public AWSFileService(AmazonS3 amazonS3) {
        this.s3Client = amazonS3;
    }

    @Value("${spring.application.name}")
    private String bucketName;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AWSFileRepository awsFileRepository;

    private static final Logger logger = LoggerFactory.getLogger(AWSFileService.class);

    public void checkUserProfilePicture(User user, MultipartFile multipartFile, boolean isProfilePicture) {
        AWSFile awsFile = awsFileRepository.findByUserAndIsProfilePictureTrue(user);

        if(user.hasProfilePicture() && isProfilePicture) {
            s3Client.deleteObject(bucketName, awsFile.getFileKey());
        }
        if(!user.hasProfilePicture() && isProfilePicture) {
            user.setHasProfilePicture(true);
            userRepository.save(user);
        }

        uploadFile(multipartFile, user, null, isProfilePicture);
    }

    public void uploadFile(MultipartFile multipartFile, User user, Issue issue, boolean isProfilePicture) {
        String fileName = multipartFile.getOriginalFilename();
        // generates a random, unique key for a file to be stored in AWS S3
        String fileKey = UUID.randomUUID() + "-" + multipartFile.getOriginalFilename();

        String contentType = multipartFile.getContentType();
        long fileSize = multipartFile.getSize();

        try {
            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentLength(fileSize);
            metadata.setContentType(contentType);
            s3Client.putObject(bucketName, fileKey, multipartFile.getInputStream(), metadata);

            // creates and saves an "AWSFile" that will store metadata of the file to be stored in AWS S3
            String url = String.valueOf(s3Client.getUrl(bucketName, fileKey));
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
        } catch (IOException ioException) {
            logger.error("IOException: " + ioException.getMessage());
        } catch (AmazonServiceException amazonServiceException) {
            logger.info("AmazonServiceException: "+ amazonServiceException.getMessage());
            throw amazonServiceException;
        } catch (AmazonClientException amazonClientException) {
            logger.info("AmazonClientException Message: " + amazonClientException.getMessage());
            throw amazonClientException;
        }
    }

    public void deleteFile(String fileKey) {
        s3Client.deleteObject(bucketName, fileKey);
        awsFileRepository.delete(awsFileRepository.findByFileKey(fileKey));
    }

    public void deleteAllFilesByUser(User user) {
        for(AWSFile awsFile : awsFileRepository.findByUser(user)) {
            s3Client.deleteObject(bucketName, awsFile.getFileKey());
        }
    }

    public void deleteAllFilesByProject(Project project) {
        for(AWSFile awsFile : awsFileRepository.findByProject(project)) {
            s3Client.deleteObject(bucketName, awsFile.getFileKey());
        }
    }

    public void deleteAllFilesByIssue(Issue issue) {
        for(AWSFile awsFile : awsFileRepository.findByIssue(issue)) {
            s3Client.deleteObject(bucketName, awsFile.getFileKey());
        }
    }
}
