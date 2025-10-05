package org.sky.model;

import com.azure.data.tables.models.TableEntity;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class DecryptionLog {

  private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

  private String partitionKey;
  private String rowKey;
  private String blobName;
  private String status;
  private String errorMessage;
  private long fileSizeBytes;
  private String timestamp;
  private long processingTimeMs;

  public DecryptionLog() {
  }

  public DecryptionLog(String blobName, String status) {
    this.blobName = blobName;
    this.status = status;
    this.timestamp = LocalDateTime.now().format(FORMATTER);
    this.partitionKey = LocalDateTime.now().toLocalDate().toString();
    this.rowKey = System.currentTimeMillis() + "_" + blobName;
  }

  public TableEntity toTableEntity() {
    TableEntity entity = new TableEntity(partitionKey, rowKey);
    entity.addProperty("BlobName", blobName);
    entity.addProperty("Status", status);
    entity.addProperty("Timestamp", timestamp);
    entity.addProperty("FileSizeBytes", fileSizeBytes);
    entity.addProperty("ProcessingTimeMs", processingTimeMs);

    if (errorMessage != null) {
      entity.addProperty("ErrorMessage", errorMessage);
    }

    return entity;
  }

  public String getPartitionKey() {
    return partitionKey;
  }

  public void setPartitionKey(String partitionKey) {
    this.partitionKey = partitionKey;
  }

  public String getRowKey() {
    return rowKey;
  }

  public void setRowKey(String rowKey) {
    this.rowKey = rowKey;
  }

  public String getBlobName() {
    return blobName;
  }

  public void setBlobName(String blobName) {
    this.blobName = blobName;
  }

  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }

  public String getErrorMessage() {
    return errorMessage;
  }

  public void setErrorMessage(String errorMessage) {
    this.errorMessage = errorMessage;
  }

  public long getFileSizeBytes() {
    return fileSizeBytes;
  }

  public void setFileSizeBytes(long fileSizeBytes) {
    this.fileSizeBytes = fileSizeBytes;
  }

  public String getTimestamp() {
    return timestamp;
  }

  public void setTimestamp(String timestamp) {
    this.timestamp = timestamp;
  }

  public long getProcessingTimeMs() {
    return processingTimeMs;
  }

  public void setProcessingTimeMs(long processingTimeMs) {
    this.processingTimeMs = processingTimeMs;
  }
}