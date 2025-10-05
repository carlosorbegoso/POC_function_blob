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
import java.util.logging.Logger;

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
      DecryptionConfig config = loadConfiguration();
      tableClient = initializeTableClient(config, logger);

      tempEncrypted = Files.createTempFile("encrypted-", ".tmp");
      tempDecrypted = Files.createTempFile("decrypted-", ".tmp");
      Files.write(tempEncrypted, encryptedBlob);

      processDecryption(config, tempEncrypted, tempDecrypted, name, logger);

      long processingTime = System.currentTimeMillis() - startTime;
      tableClient.logSuccess(name, encryptedBlob.length, processingTime);
      logger.info("decryption process logged successfully in table storage");

    } catch (Exception e) {
      handleDecryptionError(e, name, encryptedBlob.length, tableClient, logger);
    } finally {
      cleanupTempFiles(logger, tempEncrypted, tempDecrypted);
    }
  }

  private DecryptionConfig loadConfiguration() {
    return new DecryptionConfig(
        getEnvironmentVariable("KEY_VAULT_URL"),
        getEnvironmentVariable("ENCRYPTION_SECRET_NAME"),
        getEnvironmentVariable("DESTINATION_STORAGE_URL"),
        getEnvironmentVariable("DESTINATION_CONTAINER"),
        getEnvironmentVariable("LOGS_STORAGE_URL"),
        getEnvironmentVariable("LOGS_TABLE_NAME")
    );
  }

  private AzureTableStorageClient initializeTableClient(DecryptionConfig config,
                                                        java.util.logging.Logger logger) {
    logger.info("Step 0: initializing table storage client for logs");
    return new AzureTableStorageClient(config.getLogsStorageUrl(), config.getLogsTableName());
  }

  private void processDecryption(DecryptionConfig config, Path tempEncrypted,
                                 Path tempDecrypted, String name,
                                 Logger logger) throws Exception {
    logger.info("Step 1: retrieve password from key vault");
    AzureKeyVaultClient keyVaultClient = new AzureKeyVaultClient(config.getKeyVaultUrl());
    String password = keyVaultClient.getEncryptionPassword(config.getSecretName());
    logger.info("password retrieved successfully");

    logger.info("Step 2: decrypting temp file");
    FileDecryptor.decryptFile(tempEncrypted, tempDecrypted, password, true, logger);
    logger.info("decrypting temp file successfully");

    logger.info("Step 3: uploading decrypted file");
    AzureBlobStorageDecrypt destinationStorage = new AzureBlobStorageDecrypt(
        config.getDestinationStorageUrl(),
        config.getDestinationContainer()
    );

    String decryptedBlobName = removeEncExtension(name);
    destinationStorage.uploadBlob(decryptedBlobName, tempDecrypted, logger);
    logger.info("uploading decrypted file successfully");
  }

  private void handleDecryptionError(Exception e, String name, long fileSize,
                                     AzureTableStorageClient tableClient,
                                     Logger logger) {
    logger.log(Level.SEVERE, "Failed to decrypt blob: " + name, e);

    if (tableClient != null) {
      try {
        tableClient.logFailure(name, fileSize, e.getMessage());
        logger.info("failure logged in table storage");
      } catch (Exception logEx) {
        logger.warning("Failed to log error to table storage: " + logEx.getMessage());
      }
    }

    throw new RuntimeException("Failed to decrypt blob: " + name, e);
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

  private void cleanupTempFiles(Logger logger, Path... files) {
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