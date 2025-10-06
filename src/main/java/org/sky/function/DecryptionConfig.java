package org.sky.function;

public class DecryptionConfig {
  private final String keyVaultUrl;
  private final String privateKeySecretName;
  private final String passphraseSecretName;
  private final String destinationStorageUrl;
  private final String destinationContainer;
  private final String logsStorageUrl;
  private final String logsTableName;

  public DecryptionConfig(String keyVaultUrl,
                          String destinationStorageUrl,
                          String destinationContainer,
                          String logsStorageUrl,
                          String logsTableName,
                          String privateKeySecretName,
                          String passphraseSecretName) {
    this.keyVaultUrl = keyVaultUrl;
    this.destinationStorageUrl = destinationStorageUrl;
    this.destinationContainer = destinationContainer;
    this.logsStorageUrl = logsStorageUrl;
    this.logsTableName = logsTableName;
    this.privateKeySecretName = privateKeySecretName;
    this.passphraseSecretName = passphraseSecretName;
  }

  public String getKeyVaultUrl() { return keyVaultUrl; }
  public String getPrivateKeySecretName() { return privateKeySecretName; }
  public String getPassphraseSecretName() { return passphraseSecretName; }
  public String getDestinationStorageUrl() { return destinationStorageUrl; }
  public String getDestinationContainer() { return destinationContainer; }
  public String getLogsStorageUrl() { return logsStorageUrl; }
  public String getLogsTableName() { return logsTableName; }
}