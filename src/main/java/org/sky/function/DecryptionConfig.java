package org.sky.function;

public class DecryptionConfig {

  private final String keyVaultUrl;
  private final String secretName;
  private final String destinationStorageUrl;
  private final String destinationContainer;
  private final String logsStorageUrl;
  private final String logsTableName;

  public DecryptionConfig(String keyVaultUrl, String secretName,
                          String destinationStorageUrl, String destinationContainer,
                          String logsStorageUrl, String logsTableName) {
    this.keyVaultUrl = keyVaultUrl;
    this.secretName = secretName;
    this.destinationStorageUrl = destinationStorageUrl;
    this.destinationContainer = destinationContainer;
    this.logsStorageUrl = logsStorageUrl;
    this.logsTableName = logsTableName;
  }

  public String getKeyVaultUrl() {
    return keyVaultUrl;
  }

  public String getSecretName() {
    return secretName;
  }

  public String getDestinationStorageUrl() {
    return destinationStorageUrl;
  }

  public String getDestinationContainer() {
    return destinationContainer;
  }

  public String getLogsStorageUrl() {
    return logsStorageUrl;
  }

  public String getLogsTableName() {
    return logsTableName;
  }
}