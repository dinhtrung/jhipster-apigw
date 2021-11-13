package com.ft.web.rest;

import com.ft.config.S3ClientConfigurationProperties;
import java.nio.ByteBuffer;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.zalando.problem.Problem;
import org.zalando.problem.Status;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.core.async.AsyncResponseTransformer;
import software.amazon.awssdk.core.async.SdkPublisher;
import software.amazon.awssdk.http.SdkHttpResponse;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;

/**
 * @author Philippe
 *
 */
@RestController
@RequestMapping("/api/public/files")
public class DownloadResource {

    private static final Logger log = LoggerFactory.getLogger(DownloadResource.class);

    private final S3AsyncClient s3client;
    private final S3ClientConfigurationProperties s3config;

    public DownloadResource(S3AsyncClient s3client, S3ClientConfigurationProperties s3config) {
        this.s3client = s3client;
        this.s3config = s3config;
    }

    @GetMapping(path = "/{fileKey}")
    public Mono<ResponseEntity<Flux<ByteBuffer>>> downloadFile(@PathVariable("fileKey") String fileKey) {
        GetObjectRequest request = GetObjectRequest.builder().bucket(s3config.getBucket()).key(fileKey).build();

        return Mono
            .fromFuture(s3client.getObject(request, new FluxResponseProvider()))
            .map(response -> {
                checkResult(response.sdkResponse);
                String filename = response.sdkResponse
                    .metadata()
                    .entrySet()
                    .stream()
                    .filter(e -> e.getKey().equalsIgnoreCase("filename"))
                    .map(Entry::getValue)
                    .findAny()
                    .orElse(fileKey);

                log.info("[I65] filename={}, length={}", filename, response.sdkResponse.contentLength());

                return ResponseEntity
                    .ok()
                    .header(HttpHeaders.CONTENT_TYPE, response.sdkResponse.contentType())
                    .header(HttpHeaders.CONTENT_LENGTH, Long.toString(response.sdkResponse.contentLength()))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                    .body(response.flux);
            });
    }

    // Helper used to check return codes from an API call
    private static void checkResult(GetObjectResponse response) {
        SdkHttpResponse sdkResponse = response.sdkHttpResponse();
        if (sdkResponse != null && sdkResponse.isSuccessful()) {
            return;
        }
        throw Problem
            .builder()
            .withStatus(Status.valueOf(Optional.ofNullable(sdkResponse).map(SdkHttpResponse::statusCode).orElse(502)))
            .withDetail(Optional.ofNullable(sdkResponse).flatMap(SdkHttpResponse::statusText).orElse("unknown"))
            .build();
    }

    static class FluxResponseProvider implements AsyncResponseTransformer<GetObjectResponse, FluxResponse> {

        private FluxResponse response;

        @Override
        public CompletableFuture<FluxResponse> prepare() {
            response = new FluxResponse();
            return response.cf;
        }

        @Override
        public void onResponse(GetObjectResponse sdkResponse) {
            this.response.sdkResponse = sdkResponse;
        }

        @Override
        public void onStream(SdkPublisher<ByteBuffer> publisher) {
            response.flux = Flux.from(publisher);
            response.cf.complete(response);
        }

        @Override
        public void exceptionOccurred(Throwable error) {
            response.cf.completeExceptionally(error);
        }
    }

    /**
     * Holds the API response and stream
     * @author Philippe
     */
    static class FluxResponse {

        final CompletableFuture<FluxResponse> cf = new CompletableFuture<>();
        GetObjectResponse sdkResponse;
        Flux<ByteBuffer> flux;
    }
}
