package com.ft.config;

import java.net.URI;
import org.springframework.boot.context.properties.ConfigurationProperties;
import software.amazon.awssdk.regions.Region;

@ConfigurationProperties(prefix = "aws.s3")
public class S3ClientConfigurationProperties {

    private Region region = Region.US_EAST_1;
    private URI endpoint = null;

    private String accessKeyId;
    private String secretAccessKey;

    // Bucket name we'll be using as our backend storage
    private String bucket;

    // AWS S3 requires that file parts must have at least 5MB, except
    // for the last part. This may change for other S3-compatible services, so let't
    // define a configuration property for that
    private int multipartMinPartSize = 5 * 1024 * 1024;

    public Region getRegion() {
        return region;
    }

    public void setRegion(Region region) {
        this.region = region;
    }

    public URI getEndpoint() {
        return endpoint;
    }

    public void setEndpoint(URI endpoint) {
        this.endpoint = endpoint;
    }

    public String getAccessKeyId() {
        return accessKeyId;
    }

    public void setAccessKeyId(String accessKeyId) {
        this.accessKeyId = accessKeyId;
    }

    public String getSecretAccessKey() {
        return secretAccessKey;
    }

    public void setSecretAccessKey(String secretAccessKey) {
        this.secretAccessKey = secretAccessKey;
    }

    public String getBucket() {
        return bucket;
    }

    public void setBucket(String bucket) {
        this.bucket = bucket;
    }

    public int getMultipartMinPartSize() {
        return multipartMinPartSize;
    }

    public void setMultipartMinPartSize(int multipartMinPartSize) {
        this.multipartMinPartSize = multipartMinPartSize;
    }
}
