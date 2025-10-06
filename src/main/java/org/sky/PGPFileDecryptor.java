package org.sky;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openpgp.*;
import org.bouncycastle.openpgp.jcajce.JcaPGPObjectFactory;
import org.bouncycastle.openpgp.operator.jcajce.JcaKeyFingerprintCalculator;
import org.bouncycastle.openpgp.operator.jcajce.JcePBESecretKeyDecryptorBuilder;
import org.bouncycastle.openpgp.operator.jcajce.JcePublicKeyDataDecryptorFactoryBuilder;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.Security;
import java.util.Iterator;

public class PGPFileDecryptor {

  private static final int BUFFER_SIZE = 8192;

  static {
    if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
      Security.addProvider(new BouncyCastleProvider());
    }
  }

  public static void decryptFile(Path encryptedFile, Path outputFile,
                                 Path privateKeyFile, String passphrase) throws Exception {

    try (InputStream keyIn = Files.newInputStream(privateKeyFile);
         InputStream encIn = Files.newInputStream(encryptedFile);
         OutputStream out = Files.newOutputStream(outputFile)) {

      decryptFile(encIn, out, keyIn, passphrase.toCharArray());
    }
  }

  private static void decryptFile(InputStream encryptedStream, OutputStream outputStream,
                                  InputStream privateKeyStream, char[] passphrase) throws Exception {

    encryptedStream = PGPUtil.getDecoderStream(encryptedStream);
    PGPEncryptedDataList encDataList = getEncryptedDataList(encryptedStream);
    PGPSecretKeyRingCollection secretKeyRings = loadSecretKeyRing(privateKeyStream);

    DecryptionKeys keys = findDecryptionKeys(encDataList, secretKeyRings, passphrase);
    InputStream decryptedStream = getDecryptedStream(keys);
    Object message = getUncompressedMessage(decryptedStream);

    processMessage(message, outputStream);
    verifyIntegrity(keys.encryptedData);
  }

  private static PGPEncryptedDataList getEncryptedDataList(InputStream encryptedStream) throws Exception {
    PGPObjectFactory pgpFactory = new JcaPGPObjectFactory(encryptedStream);
    Object obj = pgpFactory.nextObject();

    if (obj instanceof PGPEncryptedDataList) {
      return (PGPEncryptedDataList) obj;
    }
    return (PGPEncryptedDataList) pgpFactory.nextObject();
  }

  private static DecryptionKeys findDecryptionKeys(PGPEncryptedDataList encDataList,
                                                   PGPSecretKeyRingCollection secretKeyRings,
                                                   char[] passphrase) throws PGPException {

    Iterator<PGPEncryptedData> it = encDataList.getEncryptedDataObjects();

    while (it.hasNext()) {
      PGPEncryptedData data = it.next();

      if (data instanceof PGPPublicKeyEncryptedData) {
        PGPPublicKeyEncryptedData pkEncData = (PGPPublicKeyEncryptedData) data;
        PGPSecretKey secretKey = secretKeyRings.getSecretKey(pkEncData.getKeyID());

        if (secretKey != null) {
          PGPPrivateKey privateKey = extractPrivateKey(secretKey, passphrase);
          return new DecryptionKeys(privateKey, pkEncData);
        }
      }
    }

    throw new PGPException("No matching secret key found or wrong passphrase");
  }

  private static InputStream getDecryptedStream(DecryptionKeys keys) throws PGPException {
    return keys.encryptedData.getDataStream(
        new JcePublicKeyDataDecryptorFactoryBuilder()
            .setProvider(BouncyCastleProvider.PROVIDER_NAME)
            .build(keys.privateKey)
    );
  }

  private static Object getUncompressedMessage(InputStream decryptedStream) throws Exception {
    PGPObjectFactory plainFactory = new JcaPGPObjectFactory(decryptedStream);
    Object message = plainFactory.nextObject();

    if (message instanceof PGPCompressedData) {
      PGPCompressedData compressedData = (PGPCompressedData) message;
      plainFactory = new JcaPGPObjectFactory(compressedData.getDataStream());
      message = plainFactory.nextObject();
    }

    return message;
  }

  private static void processMessage(Object message, OutputStream outputStream)
      throws IOException, PGPException {

    if (message instanceof PGPLiteralData) {
      PGPLiteralData literalData = (PGPLiteralData) message;
      try (InputStream dataStream = literalData.getInputStream()) {
        copyStream(dataStream, outputStream);
      }
    } else if (message instanceof PGPOnePassSignatureList) {
      throw new PGPException("Encrypted message contains a signed message - not literal data");
    } else {
      throw new PGPException("Message is not a simple encrypted file - type unknown");
    }
  }

  private static void verifyIntegrity(PGPPublicKeyEncryptedData encryptedData) throws PGPException {
    if (encryptedData.isIntegrityProtected() && !encryptedData.verify()) {
      throw new PGPException("Message failed integrity check");
    }
  }

  private static PGPSecretKeyRingCollection loadSecretKeyRing(InputStream keyIn) throws Exception {
    keyIn = PGPUtil.getDecoderStream(keyIn);
    return new PGPSecretKeyRingCollection(keyIn, new JcaKeyFingerprintCalculator());
  }

  private static PGPPrivateKey extractPrivateKey(PGPSecretKey secretKey, char[] passphrase)
      throws PGPException {

    return secretKey.extractPrivateKey(
        new JcePBESecretKeyDecryptorBuilder()
            .setProvider(BouncyCastleProvider.PROVIDER_NAME)
            .build(passphrase)
    );
  }

  private static void copyStream(InputStream in, OutputStream out) throws IOException {
    byte[] buffer = new byte[BUFFER_SIZE];
    int bytesRead;

    while ((bytesRead = in.read(buffer)) != -1) {
      out.write(buffer, 0, bytesRead);
    }
  }

  private static class DecryptionKeys {
    final PGPPrivateKey privateKey;
    final PGPPublicKeyEncryptedData encryptedData;

    DecryptionKeys(PGPPrivateKey privateKey, PGPPublicKeyEncryptedData encryptedData) {
      this.privateKey = privateKey;
      this.encryptedData = encryptedData;
    }
  }
}