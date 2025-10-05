package org.sky.azure;

import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.BlobServiceClientBuilder;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class AzureBlobStorageDecrypt {
  private static final DateTimeFormatter TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

  private final BlobServiceClient blobServiceClient;
  private final String containerName;

  public AzureBlobStorageDecrypt(String storageAccountUrl, String containerName) {
    this.blobServiceClient = createBlobServiceClient(storageAccountUrl);
    this.containerName = containerName;
  }
  private BlobServiceClient createBlobServiceClient(String storageAccountUrl){
    return new BlobServiceClientBuilder()
        .endpoint(storageAccountUrl)
        .credential(AzureCredentialsProvider.getCredentials())
        .buildClient();
  }

  public void uploadBlob(String blobName, Path sourcePath){
    try{
      String blobNameWithTimestamp = addTimestampToBlobName(blobName);
      BlobClient blobClient = getBlobClient(blobNameWithTimestamp);

      long fileSize = Files.size(sourcePath);
      System.out.printf("Uploading decrypted blob: %s (%.2f MB)%n",
          blobNameWithTimestamp, fileSize / (1024.0 * 1024.0));

      blobClient.uploadFromFile(sourcePath.toString(), true);
      System.out.printf("Uploaded successfully: %s%n", blobNameWithTimestamp);
    }catch (Exception e){
      throw new RuntimeException("Failed to upload decrypted blob: " + blobName, e);
    }
  }

  private BlobClient getBlobClient(String blobName) {
    BlobContainerClient containerClient = getContainerClient(containerName);
    return  containerClient.getBlobClient(blobName);
  }

  private BlobContainerClient getContainerClient(String containerName){
    return blobServiceClient.getBlobContainerClient(containerName);
  }

  private String addTimestampToBlobName(String blobName) {
    String timestamp = LocalDateTime.now().format(TIMESTAMP_FORMAT);
    int lastDotIndex = blobName.lastIndexOf('.');
    if (lastDotIndex > 0) {
      String nameWithoutExtension = blobName.substring(0, lastDotIndex);
      String extension = blobName.substring(lastDotIndex);
      return nameWithoutExtension + "-" + timestamp + extension;
    }
    return blobName + "-" + timestamp;
  }
}
