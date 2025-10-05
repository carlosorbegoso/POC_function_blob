package org.sky.azure;

import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.BlobServiceClientBuilder;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.logging.Logger;

public class AzureBlobStorageDecrypt {

  private static final DateTimeFormatter TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

  private final BlobServiceClient blobServiceClient;
  private final String containerName;

  public AzureBlobStorageDecrypt(String storageAccountUrl, String containerName) {
    this.blobServiceClient = createBlobServiceClient(storageAccountUrl);
    this.containerName = containerName;
  }

  private BlobServiceClient createBlobServiceClient(String storageAccountUrl) {
    return new BlobServiceClientBuilder()
        .endpoint(storageAccountUrl)
        .credential(AzureCredentialsProvider.getCredentials())
        .buildClient();
  }

  public void uploadBlob(String blobName, Path sourcePath) {
    uploadBlob(blobName, sourcePath, null);
  }

  public void uploadBlob(String blobName, Path sourcePath, Logger logger) {
    try {
      String blobNameWithTimestamp = addTimestampToBlobName(blobName);
      BlobClient blobClient = getBlobClient(blobNameWithTimestamp);

      long fileSize = Files.size(sourcePath);
      String uploadMessage = String.format("Uploading decrypted blob: %s (%.2f MB)",
          blobNameWithTimestamp, fileSize / (1024.0 * 1024.0));

      if (logger != null) {
        logger.info(uploadMessage);
      } else {
        System.out.println(uploadMessage);
      }

      blobClient.uploadFromFile(sourcePath.toString(), true);

      String successMessage = String.format("Uploaded successfully: %s", blobNameWithTimestamp);
      if (logger != null) {
        logger.info(successMessage);
      } else {
        System.out.println(successMessage);
      }

    } catch (Exception e) {
      throw new RuntimeException("Failed to upload decrypted blob: " + blobName, e);
    }
  }

  private BlobClient getBlobClient(String blobName) {
    BlobContainerClient containerClient = getContainerClient();
    return containerClient.getBlobClient(blobName);
  }

  private BlobContainerClient getContainerClient() {
    return blobServiceClient.getBlobContainerClient(containerName);
  }

  private String addTimestampToBlobName(String blobName) {
    String timestamp = LocalDateTime.now().format(TIMESTAMP_FORMAT);
    return timestamp + "-" + blobName;
  }
}