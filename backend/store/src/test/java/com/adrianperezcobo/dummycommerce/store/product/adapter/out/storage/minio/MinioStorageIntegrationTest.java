package com.adrianperezcobo.dummycommerce.store.product.adapter.out.storage.minio;

import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.*;
import org.testcontainers.utility.DockerImageName;
import java.net.URI;
import java.net.http.*;
import java.util.UUID;
import static org.assertj.core.api.Assertions.*;

@Testcontainers
class MinioStorageIntegrationTest {
    @Container
    static GenericContainer<?> minio = new GenericContainer<>(DockerImageName.parse(
            "coollabsio/minio:RELEASE.2025-10-15T17-29-55Z@sha256:69b55a1c1c5dc285ce04db96689f5b2102317fc77a50680a1874ca6efd1c87f9"))
            .withEnv("MINIO_ROOT_USER", "testadmin").withEnv("MINIO_ROOT_PASSWORD", "MinioTest123!")
            .withCommand("server", "/data").withExposedPorts(9000)
            .waitingFor(Wait.forHttp("/minio/health/ready").forPort(9000));

    private MinioProductImageStorageAdapter storage(String publicEndpoint) {
        String endpoint = "http://" + minio.getHost() + ":" + minio.getMappedPort(9000);
        var properties = new MinioProperties(endpoint, publicEndpoint, "testadmin", "MinioTest123!", "test-images", "us-east-1", 900);
        var config = new MinioConfig();
        var adapter = new MinioProductImageStorageAdapter(config.minioInternalClient(properties), config.minioPublicClient(properties), properties);
        adapter.initializeBucket();
        return adapter;
    }

    @Test
    void initializesBucketIdempotentlyAndUploadsReadsDeletesRealBytes() throws Exception {
        String endpoint = "http://" + minio.getHost() + ":" + minio.getMappedPort(9000);
        var storage = storage(endpoint);
        storage.initializeBucket();
        String key = "products/" + UUID.randomUUID() + "/image";
        byte[] content = new byte[]{(byte) 137, 80, 78, 71, 13, 10, 26, 10};
        assertThat(storage.exists(key)).isFalse();
        var http = HttpClient.newHttpClient();
        var upload = http.send(HttpRequest.newBuilder(URI.create(storage.createUploadUrl(key)))
                .PUT(HttpRequest.BodyPublishers.ofByteArray(content)).build(), HttpResponse.BodyHandlers.discarding());
        assertThat(upload.statusCode()).isEqualTo(200);
        assertThat(storage.exists(key)).isTrue();
        var read = http.send(HttpRequest.newBuilder(URI.create(storage.createReadUrl(key))).GET().build(), HttpResponse.BodyHandlers.ofByteArray());
        assertThat(read.statusCode()).isEqualTo(200);
        assertThat(read.body()).isEqualTo(content);
        storage.delete(key);
        assertThat(storage.exists(key)).isFalse();
    }

    @Test
    void publicUrlSigningDoesNotConnectToBrowserEndpointFromServer() {
        var storage = storage("http://browser-host.invalid:9000");
        assertThat(storage.createUploadUrl("test")).startsWith("http://browser-host.invalid:9000/")
                .contains("us-east-1").contains("X-Amz-Signature");
        assertThat(storage.createReadUrl("test")).startsWith("http://browser-host.invalid:9000/");
    }

    @Test
    void storageOutageIsNotReportedAsMissingUpload() throws Exception {
        int unusedPort;
        try (var socket = new java.net.ServerSocket(0)) { unusedPort = socket.getLocalPort(); }
        var properties = new MinioProperties("http://localhost:" + unusedPort, "http://localhost:" + unusedPort,
                "testadmin", "MinioTest123!", "test-images", "us-east-1", 900);
        var client = new MinioConfig().minioInternalClient(properties);
        var adapter = new MinioProductImageStorageAdapter(client, client, properties);
        assertThatThrownBy(() -> adapter.exists("test")).isInstanceOf(IllegalStateException.class);
    }
}
