package org.sky.azure;

import com.azure.security.keyvault.secrets.SecretClient;
import com.azure.security.keyvault.secrets.SecretClientBuilder;
import com.azure.security.keyvault.secrets.models.KeyVaultSecret;


public class AzureKeyVaultClient {
  private final SecretClient secretClient;

  public AzureKeyVaultClient(String keyVaultUrl) {
    this.secretClient = createSecretClient(keyVaultUrl);
  }
  private SecretClient createSecretClient(String keyVaultUrl){
    return new SecretClientBuilder()
        .vaultUrl(keyVaultUrl)
        .credential(AzureCredentialsProvider.getCredentials())
        .buildClient();
  }
  public String getSecret(String secretName){
    try{
      KeyVaultSecret secret = secretClient.getSecret(secretName);
      return secret.getValue();
    }catch (Exception e){
      throw new RuntimeException("Failed to retrieve secret: " + secretName, e);
    }
  }
  public String getEncryptionPassword(String secretName){
    return getSecret(secretName);
  }
}
