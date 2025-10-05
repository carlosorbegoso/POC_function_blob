package org.sky.azure;

import com.azure.data.tables.TableClient;
import com.azure.data.tables.TableClientBuilder;
import com.azure.data.tables.models.TableEntity;
import org.sky.model.DecryptionLog;

public class AzureTableStorageClient {

  private final TableClient tableClient;

  public AzureTableStorageClient(String storageAccountUrl, String tableName) {
    this.tableClient = createTableClient(storageAccountUrl, tableName);
    ensureTableExists();
  }

  private TableClient createTableClient(String storageAccountUrl, String tableName) {
    String tableEndpoint = storageAccountUrl + "/" + tableName;

    return new TableClientBuilder()
        .endpoint(tableEndpoint)
        .credential(AzureCredentialsProvider.getCredentials())
        .tableName(tableName)
        .buildClient();
  }

  private void ensureTableExists() {
    try {
      tableClient.createTable();
    } catch (Exception e) {
      // Table already exists, ignore
    }
  }

  public void logDecryption(DecryptionLog log) {
    try {
      TableEntity entity = log.toTableEntity();
      tableClient.createEntity(entity);
    } catch (Exception e) {
      throw new RuntimeException("Failed to log decryption to table storage", e);
    }
  }

  public void logSuccess(String blobName, long fileSizeBytes, long processingTimeMs) {
    DecryptionLog log = new DecryptionLog(blobName, "SUCCESS");
    log.setFileSizeBytes(fileSizeBytes);
    log.setProcessingTimeMs(processingTimeMs);
    logDecryption(log);
  }

  public void logFailure(String blobName, long fileSizeBytes, String errorMessage) {
    DecryptionLog log = new DecryptionLog(blobName, "FAILED");
    log.setFileSizeBytes(fileSizeBytes);
    log.setErrorMessage(errorMessage);
    logDecryption(log);
  }
}