package org.sky.function;

public class DecryptionConfig {

  private final String keyVaultUrl;
  private final String destinationStorageUrl;
  private final String destinationContainer;
  private final String logsStorageUrl;
  private final String logsTableName;
  private final String privateKeyPath;
  private final String passphraseSecretName;

  public DecryptionConfig(String keyVaultUrl,
                          String destinationStorageUrl, String destinationContainer,
                          String logsStorageUrl, String logsTableName,
                          String privateKeyPath,
                          String passphraseSecretName) {
    this.keyVaultUrl = keyVaultUrl;
    this.destinationStorageUrl = destinationStorageUrl;
    this.destinationContainer = destinationContainer;
    this.logsStorageUrl = logsStorageUrl;
    this.logsTableName = logsTableName;
    this.privateKeyPath = privateKeyPath;
    this.passphraseSecretName = passphraseSecretName;
  }

  public String getKeyVaultUrl() {
    return keyVaultUrl;
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

  public String getPrivateKeyPath() {
    return privateKeyPath;
  }

  public String getPassphraseSecretName() {
    return passphraseSecretName;
  }
}