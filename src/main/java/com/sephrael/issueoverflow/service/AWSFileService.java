package com.sephrael.issueoverflow.service;

import com.sephrael.issueoverflow.configuration.S3Properties;
import com.sephrael.issueoverflow.entity.AWSFile;
import com.sephrael.issueoverflow.entity.Issue;
import com.sephrael.issueoverflow.entity.Organization;
import com.sephrael.issueoverflow.entity.Project;
import com.sephrael.issueoverflow.entity.User;
import com.sephrael.issueoverflow.repository.AWSFileRepository;
import com.sephrael.issueoverflow.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ContentDisposition;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

@Service
public class AWSFileService {

    private final S3Client s3Client;
    private final S3Presigner s3Presigner;
    private final String bucketName;
    private final Duration presignedUrlTtl;
    private final UserRepository userRepository;
    private final AWSFileRepository awsFileRepository;
    private static final Logger logger = LoggerFactory.getLogger(AWSFileService.class);

    // only these types open in the browser; everything else is served as a download
    private static final Set<String> INLINE_CONTENT_TYPES = Set.of("image/png", "image/jpeg", "image/gif", "image/webp");

    public AWSFileService(S3Client s3Client, S3Presigner s3Presigner, S3Properties s3Properties,
                          UserRepository userRepository, AWSFileRepository awsFileRepository) {
        this.s3Client = s3Client;
        this.s3Presigner = s3Presigner;
        this.bucketName = s3Properties.bucket();
        this.presignedUrlTtl = s3Properties.presignedUrlTtl();
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
        AWSFile awsFile = new AWSFile(fileName, fileKey, contentType, fileSize, isProfilePicture);
        user.addUserToAWSFile(awsFile);

        // if file is a profile picture, store its key with the user
        if(isProfilePicture) {
            user.setProfilePictureKey(fileKey);
            userRepository.save(user);
        }

        // connect AWSFile to an issue and its project
        if(issue != null) {
            issue.addAWSFileToIssue(awsFile);
            issue.getProject().addAWSFileToProject(awsFile);
        }

        awsFileRepository.save(awsFile);
    }

    // Profile pictures are visible to their owner and to members of the owner's organization.
    // Attachments are visible to members of the issue's organization.
    public boolean canView(User viewer, AWSFile awsFile) {
        Organization viewerOrganization = viewer.getOrganization();

        if(awsFile.isProfilePicture()) {
            User owner = awsFile.getUser();
            if(owner == null)
                return false;
            if(Objects.equals(owner.getId(), viewer.getId()))
                return true;
            return viewerOrganization != null && owner.getOrganization() != null
                    && Objects.equals(owner.getOrganization().getId(), viewerOrganization.getId());
        }

        Issue issue = awsFile.getIssue();
        return viewerOrganization != null && issue != null && issue.getOrganization() != null
                && Objects.equals(issue.getOrganization().getId(), viewerOrganization.getId());
    }

    // A short-lived GET URL for the object, signed with the app's credentials.
    public URI presignedUrl(AWSFile awsFile) {
        GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                .bucket(bucketName)
                .key(awsFile.getFileKey())
                .responseContentDisposition(contentDisposition(awsFile))
                .build();

        PresignedGetObjectRequest presigned = s3Presigner.presignGetObject(request -> request
                .signatureDuration(presignedUrlTtl)
                .getObjectRequest(getObjectRequest));

        return URI.create(presigned.url().toExternalForm());
    }

    private static String contentDisposition(AWSFile awsFile) {
        boolean inline = awsFile.getType() != null && INLINE_CONTENT_TYPES.contains(awsFile.getType());
        ContentDisposition.Builder builder = inline ? ContentDisposition.inline() : ContentDisposition.attachment();

        if(awsFile.getFileName() != null && !awsFile.getFileName().isBlank())
            builder.filename(awsFile.getFileName(), StandardCharsets.UTF_8);

        return builder.build().toString();
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
