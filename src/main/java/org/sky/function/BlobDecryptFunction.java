package org.sky.function;

import com.microsoft.azure.functions.ExecutionContext;
import com.microsoft.azure.functions.annotation.BlobTrigger;
import com.microsoft.azure.functions.annotation.FunctionName;
import com.microsoft.azure.functions.annotation.StorageAccount;
import org.sky.FileDecryptor;
import org.sky.azure.AzureBlobStorageDecrypt;
import org.sky.azure.AzureKeyVaultClient;
import org.sky.azure.AzureTableStorageClient;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.logging.Level;

public class BlobDecryptFunction {

  @FunctionName("BlobDecryptTrigger")
  @StorageAccount("AzureWebJobsStorage")
  public void run(
      @BlobTrigger(
          name = "encryptedBlob",
          path = "encrypted-files/{name}",
          dataType = "binary"
      ) byte[] encryptedBlob,
      String name,
      ExecutionContext context
  ) {
    java.util.logging.Logger logger = context.getLogger();
    logger.info("Java Blob trigger function processed a blob. Name: " + name + ", Size: " + encryptedBlob.length + " Bytes");

    Path tempEncrypted = null;
    Path tempDecrypted = null;
    long startTime = System.currentTimeMillis();
    AzureTableStorageClient tableClient = null;

    try {
      String keyVaultUrl = getEnvironmentVariable("KEY_VAULT_URL");
      String secretName = getEnvironmentVariable("ENCRYPTION_SECRET_NAME");
      String destinationStorageUrl = getEnvironmentVariable("DESTINATION_STORAGE_URL");
      String destinationContainer = getEnvironmentVariable("DESTINATION_CONTAINER");
      String logsStorageUrl = getEnvironmentVariable("LOGS_STORAGE_URL");
      String logsTableName = getEnvironmentVariable("LOGS_TABLE_NAME");

      logger.info("Step 0: initializing table storage client for logs");
      tableClient = new AzureTableStorageClient(logsStorageUrl, logsTableName);

      logger.info("Step 1: retrieve password from key vault");
      AzureKeyVaultClient keyVaultClient = new AzureKeyVaultClient(keyVaultUrl);
      String password = keyVaultClient.getEncryptionPassword(secretName);
      logger.info("password retrieved successfully");

      logger.info("Step 2: creating temp files and saving encrypted blob");
      tempEncrypted = Files.createTempFile("encrypted-", ".tmp");
      tempDecrypted = Files.createTempFile("decrypted-", ".tmp");

      Files.write(tempEncrypted, encryptedBlob);
      logger.info("save encrypted blob to temp file successfully");

      logger.info("Step 3: decrypting temp file");
      FileDecryptor.decryptFile(tempEncrypted, tempDecrypted, password, true, logger);
      logger.info("decrypting temp file successfully");

      logger.info("Step 4: uploading decrypted file to destination storage account");
      AzureBlobStorageDecrypt destinationStorage = new AzureBlobStorageDecrypt(
          destinationStorageUrl,
          destinationContainer
      );

      String decryptedBlobName = removeEncExtension(name);
      destinationStorage.uploadBlob(decryptedBlobName, tempDecrypted);
      logger.info("uploading decrypted file to destination storage account successfully");

      long processingTime = System.currentTimeMillis() - startTime;
      tableClient.logSuccess(name, encryptedBlob.length, processingTime);
      logger.info("decryption process logged successfully in table storage");

    } catch (Exception e) {
      logger.log(Level.SEVERE, "Failed to decrypt blob: " + name, e);

      if (tableClient != null) {
        try {
          tableClient.logFailure(name, encryptedBlob.length, e.getMessage());
          logger.info("failure logged in table storage");
        } catch (Exception logEx) {
          logger.warning("Failed to log error to table storage: " + logEx.getMessage());
        }
      }

      throw new RuntimeException("Failed to decrypt blob: " + name, e);

    } finally {
      cleanupTempFiles(logger, tempEncrypted, tempDecrypted);
    }
  }

  private String removeEncExtension(String filename) {
    if (filename.endsWith(".enc")) {
      return filename.substring(0, filename.length() - 4);
    }
    return filename;
  }

  private String getEnvironmentVariable(String name) {
    String value = System.getenv(name);
    if (value == null || value.trim().isEmpty()) {
      throw new IllegalArgumentException("Environment variable " + name + " is not set");
    }
    return value;
  }

  private void cleanupTempFiles(java.util.logging.Logger logger, Path... files) {
    for (Path file : files) {
      try {
        if (file != null && Files.exists(file)) {
          Files.delete(file);
          logger.info("temp file deleted successfully: " + file.getFileName());
        }
      } catch (Exception e) {
        logger.warning("Failed to delete temp file: " + file);
      }
    }
  }
}