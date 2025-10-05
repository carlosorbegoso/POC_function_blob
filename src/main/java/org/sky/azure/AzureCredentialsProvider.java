package org.sky.azure;

import com.azure.identity.DefaultAzureCredential;
import com.azure.identity.DefaultAzureCredentialBuilder;

public class AzureCredentialsProvider {
  private static DefaultAzureCredential credential;

  private AzureCredentialsProvider(){}

  public static DefaultAzureCredential getCredentials(){
    if(credential == null){
      credential = createCredential();
    }
    return credential;
  }

  private static DefaultAzureCredential createCredential() {
    return new DefaultAzureCredentialBuilder().build();
  }
}
