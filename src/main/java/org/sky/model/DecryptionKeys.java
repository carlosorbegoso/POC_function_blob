package org.sky.model;

import org.bouncycastle.openpgp.PGPPrivateKey;
import org.bouncycastle.openpgp.PGPPublicKeyEncryptedData;

public class DecryptionKeys {
  public final PGPPrivateKey privateKey;
  public final PGPPublicKeyEncryptedData encryptedData;

  public DecryptionKeys(PGPPrivateKey privateKey, PGPPublicKeyEncryptedData encryptedData) {
    this.privateKey = privateKey;
    this.encryptedData = encryptedData;
  }
}