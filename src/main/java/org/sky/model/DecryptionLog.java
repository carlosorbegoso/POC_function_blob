package org.sky.model;

import com.azure.data.tables.models.TableEntity;

import java.time.OffsetDateTime;
import java.util.UUID;

public class DecryptionLog {
  private String blobName;
  private String status;
  private Long fileSizeBytes;
  private Long processingTimeMs;
  private String errorMessage;
  private OffsetDateTime timestamp;
  private String rowKey;

  public DecryptionLog(String blobName, String status) {
    this.blobName = blobName;
    this.status = status;
    this.timestamp = OffsetDateTime.now();
    this.rowKey = UUID.randomUUID().toString();
  }

  public void setFileSizeBytes(Long fileSizeBytes) {
    this.fileSizeBytes = fileSizeBytes;
  }

  public void setProcessingTimeMs(Long processingTimeMs) {
    this.processingTimeMs = processingTimeMs;
  }

  public void setErrorMessage(String errorMessage) {
    this.errorMessage = errorMessage;
  }

  public TableEntity toTableEntity() {
    TableEntity entity = new TableEntity("DecryptionLog", rowKey);

    entity.addProperty("BlobName", blobName);
    entity.addProperty("Status", status);
    entity.addProperty("Timestamp", timestamp.toString());

    if (fileSizeBytes != null) {
      entity.addProperty("FileSizeBytes", fileSizeBytes);
    }

    if (processingTimeMs != null) {
      entity.addProperty("ProcessingTimeMs", processingTimeMs);
    }

    if (errorMessage != null) {
      entity.addProperty("ErrorMessage", errorMessage);
    }

    return entity;
  }
}