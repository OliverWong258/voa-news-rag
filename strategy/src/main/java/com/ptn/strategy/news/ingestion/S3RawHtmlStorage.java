package com.ptn.strategy.news.ingestion;

import com.ptn.strategy.config.AwsProperties;
import java.nio.charset.StandardCharsets;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

@Component
public class S3RawHtmlStorage implements RawHtmlStorage {

    private final S3Client s3Client;
    private final AwsProperties awsProperties;

    public S3RawHtmlStorage(S3Client s3Client, AwsProperties awsProperties) {
        this.s3Client = s3Client;
        this.awsProperties = awsProperties;
    }

    @Override
    public String store(long taskId, String html) {
        String key = "raw/voa/tasks/" + taskId + ".html";
        PutObjectRequest request = PutObjectRequest.builder()
                .bucket(awsProperties.s3().rawContentBucket())
                .key(key)
                .contentType("text/html; charset=UTF-8")
                .build();
        s3Client.putObject(request, RequestBody.fromBytes(html.getBytes(StandardCharsets.UTF_8)));
        return key;
    }
}
