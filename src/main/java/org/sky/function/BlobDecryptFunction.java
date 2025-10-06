package org.sky.function;

import com.microsoft.azure.functions.ExecutionContext;
import com.microsoft.azure.functions.annotation.BlobTrigger;
import com.microsoft.azure.functions.annotation.FunctionName;
import com.microsoft.azure.functions.annotation.StorageAccount;
import org.sky.azure.AzureKeyVaultClient;
import org.sky.utils.PGPFileDecryptor;
import org.sky.azure.AzureBlobStorageDecrypt;
import org.sky.azure.AzureTableStorageClient;
import org.sky.function.exception.DecryptionException;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.logging.Level;
import java.util.logging.Logger;

public class BlobDecryptFunction {
  private Logger logger;

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
    this.logger = context.getLogger();
    logger.info(() -> String.format("Java Blob trigger function processed a blob. Name: %s, Size: %d Bytes",
        name, encryptedBlob.length));

    Path tempEncrypted = null;
    Path tempDecrypted = null;
    Path tempPrivateKey = null;
    long startTime = System.currentTimeMillis();
    AzureTableStorageClient tableClient = null;

    try {
      DecryptionConfig config = loadConfiguration();
      tableClient = initializeTableClient(config);

      tempEncrypted = Files.createTempFile("encrypted-", ".pgp");
      tempDecrypted = Files.createTempFile("decrypted-", ".tmp");
      tempPrivateKey = Files.createTempFile("pgp-key-", ".asc");

      Files.write(tempEncrypted, encryptedBlob);

      processDecryption(config, tempEncrypted, tempDecrypted, tempPrivateKey, name);

      long processingTime = System.currentTimeMillis() - startTime;
      tableClient.logSuccess(name, encryptedBlob.length, processingTime);
      logger.info("decryption process logged successfully in table storage");

    } catch (Exception e) {
      handleDecryptionError(e, name, encryptedBlob.length, tableClient);
    } finally {
      cleanupTempFiles(tempEncrypted, tempDecrypted, tempPrivateKey);
    }
  }

  private DecryptionConfig loadConfiguration() {
    return new DecryptionConfig(
        getEnvironmentVariable("KEY_VAULT_URL"),
        getEnvironmentVariable("DESTINATION_STORAGE_URL"),
        getEnvironmentVariable("DESTINATION_CONTAINER"),
        getEnvironmentVariable("LOGS_STORAGE_URL"),
        getEnvironmentVariable("LOGS_TABLE_NAME"),
        getEnvironmentVariable("PGP_PRIVATE_KEY_SECRET_NAME"),
        getEnvironmentVariable("PGP_PASSPHRASE_SECRET_NAME")
    );
  }

  private AzureTableStorageClient initializeTableClient(DecryptionConfig config) {
    logger.info("Step 0: initializing table storage client for logs");
    return new AzureTableStorageClient(config.getLogsStorageUrl(), config.getLogsTableName());
  }

  private void processDecryption(DecryptionConfig config, Path tempEncrypted,
                                 Path tempDecrypted, Path tempPrivateKey, String name) throws Exception {
    logger.info("Step 1: retrieve PGP credentials from Azure Key Vault");
    AzureKeyVaultClient keyVaultClient = new AzureKeyVaultClient(config.getKeyVaultUrl());

    String privateKeyBase64 = keyVaultClient.getSecret(config.getPrivateKeySecretName());
    String passphrase = keyVaultClient.getSecret(config.getPassphraseSecretName());

    logger.info("PGP credentials retrieved successfully from Key Vault");

    logger.info("Step 2: preparing private key file");
    byte[] privateKeyBytes = Base64.getDecoder().decode(privateKeyBase64);
    Files.write(tempPrivateKey, privateKeyBytes);
    logger.info("private key file created");

    logger.info("Step 3: decrypting PGP file");
    PGPFileDecryptor.decryptFile(tempEncrypted, tempDecrypted, tempPrivateKey, passphrase);
    logger.info("file decrypted successfully");

    logger.info("Step 4: uploading decrypted file");
    AzureBlobStorageDecrypt destinationStorage = new AzureBlobStorageDecrypt(
        config.getDestinationStorageUrl(),
        config.getDestinationContainer()
    );

    String decryptedBlobName = removeEncExtension(name);
    destinationStorage.uploadBlob(decryptedBlobName, tempDecrypted);
    logger.info("decrypted file uploaded successfully");
  }

  private void handleDecryptionError(Exception e, String name, long fileSize,
                                     AzureTableStorageClient tableClient) {
    logger.log(Level.SEVERE, "Decryption error for blob: " + name, e);

    if (tableClient != null) {
      try {
        tableClient.logFailure(name, fileSize, e.getMessage());
        logger.info("failure logged in table storage");
      } catch (Exception logEx) {
        logger.warning("Failed to log error to table storage: " + logEx.getMessage());
      }
    }

    throw new DecryptionException("Failed to decrypt blob: " + name, e);
  }

  private String removeEncExtension(String filename) {
    if (filename.endsWith(".pgp")) {
      return filename.substring(0, filename.length() - 4);
    }
    if (filename.endsWith(".gpg")) {
      return filename.substring(0, filename.length() - 4);
    }
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

  private void cleanupTempFiles(Path... files) {
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