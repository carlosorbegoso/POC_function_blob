package org.sky;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Base64;
import java.util.logging.Logger;

public class FileDecryptor {

  private static final String OPENSSL_SALT_PREFIX = "Salted__";
  private static final int SALT_PREFIX_LENGTH = 8;
  private static final int SALT_LENGTH = 8;
  private static final int IV_LENGTH = 16;
  private static final int KEY_LENGTH = 32;
  private static final int BUFFER_SIZE = 8192;
  private static final int PROGRESS_UPDATE_INTERVAL = BUFFER_SIZE * 1000;
  private static final double BYTES_TO_MB = 1024.0 * 1024.0;

  private static final String AES_ALGORITHM = "AES";
  private static final String CIPHER_TRANSFORMATION = "AES/CBC/PKCS5Padding";
  private static final String HASH_ALGORITHM_MD5 = "MD5";
  private static final String HASH_ALGORITHM_SHA256 = "SHA-256";

  public static void decryptFile(Path inputPath, Path outputPath, String password,
                                 boolean isBase64Encoded, Logger logger) throws Exception {

    printProcessingInfo(inputPath, logger);

    try (InputStream inputStream = createInputStream(inputPath, isBase64Encoded)) {
      decryptStream(inputStream, outputPath, password, logger);
    }

    printSuccessInfo(outputPath, logger);
  }

  private static InputStream createInputStream(Path inputPath, boolean isBase64Encoded) throws IOException {
    InputStream fileStream = Files.newInputStream(inputPath);
    return isBase64Encoded ? Base64.getDecoder().wrap(fileStream) : fileStream;
  }

  private static void printProcessingInfo(Path inputPath, Logger logger) throws IOException {
    long fileSize = Files.size(inputPath);
    String message = String.format("Processing file: %s (%.2f MB)",
        inputPath.getFileName(), fileSize / BYTES_TO_MB);

    if (logger != null) {
      logger.info(message);
    } else {
      System.out.println(message);
    }
  }

  private static void printSuccessInfo(Path outputPath, Logger logger) throws IOException {
    long outputSize = Files.size(outputPath);
    String message = String.format("Decrypted successfully: %s (%.2f MB)",
        outputPath.getFileName(), outputSize / BYTES_TO_MB);

    if (logger != null) {
      logger.info(message);
    } else {
      System.out.println(message);
    }
  }

  private static void decryptStream(InputStream inputStream, Path outputPath,
                                    String password, Logger logger) throws Exception {

    byte[] header = readExactBytes(inputStream, SALT_PREFIX_LENGTH);

    if (isOpenSSLFormat(header)) {
      decryptOpenSSLStream(inputStream, outputPath, password, header, logger);
    } else {
      decryptSimpleStream(inputStream, outputPath, password, header, logger);
    }
  }

  private static boolean isOpenSSLFormat(byte[] header) {
    String prefix = new String(header, StandardCharsets.UTF_8);
    return OPENSSL_SALT_PREFIX.equals(prefix);
  }

  private static void decryptOpenSSLStream(InputStream inputStream, Path outputPath,
                                           String password, byte[] header, Logger logger) throws Exception {

    byte[] salt = readExactBytes(inputStream, SALT_LENGTH);
    KeyAndIV keyAndIV = deriveKeyAndIVFromPassword(password, salt);

    performStreamDecryption(inputStream, outputPath, keyAndIV.getKey(), keyAndIV.getIv(), logger);
  }

  private static void decryptSimpleStream(InputStream inputStream, Path outputPath,
                                          String password, byte[] header, Logger logger) throws Exception {

    byte[] remainingIV = readExactBytes(inputStream, IV_LENGTH - SALT_PREFIX_LENGTH);
    byte[] iv = concatenateBytes(header, remainingIV);
    byte[] key = generateKeyFromPassword(password);

    performStreamDecryption(inputStream, outputPath, key, iv, logger);
  }

  private static void performStreamDecryption(InputStream inputStream, Path outputPath,
                                              byte[] key, byte[] iv, Logger logger) throws Exception {

    Cipher cipher = createDecryptionCipher(key, iv);

    try (CipherInputStream cis = new CipherInputStream(inputStream, cipher);
         OutputStream outputStream = Files.newOutputStream(outputPath)) {

      processStreamWithProgress(cis, outputStream, logger);
    }
  }

  private static Cipher createDecryptionCipher(byte[] key, byte[] iv) throws Exception {
    SecretKeySpec keySpec = new SecretKeySpec(key, AES_ALGORITHM);
    IvParameterSpec ivSpec = new IvParameterSpec(iv);

    Cipher cipher = Cipher.getInstance(CIPHER_TRANSFORMATION);
    cipher.init(Cipher.DECRYPT_MODE, keySpec, ivSpec);

    return cipher;
  }

  private static void processStreamWithProgress(InputStream inputStream,
                                                OutputStream outputStream,
                                                Logger logger) throws IOException {

    byte[] buffer = new byte[BUFFER_SIZE];
    int bytesRead;
    long totalBytes = 0;

    while ((bytesRead = inputStream.read(buffer)) != -1) {
      outputStream.write(buffer, 0, bytesRead);
      totalBytes += bytesRead;

      if (shouldPrintProgress(totalBytes)) {
        printProgress(totalBytes, logger);
      }
    }
  }

  private static boolean shouldPrintProgress(long totalBytes) {
    return totalBytes % PROGRESS_UPDATE_INTERVAL == 0;
  }

  private static void printProgress(long totalBytes, Logger logger) {
    String message = String.format("Processed: %.2f MB", totalBytes / BYTES_TO_MB);

    if (logger != null) {
      logger.info(message);
    } else {
      System.out.println(message);
    }
  }

  private static byte[] readExactBytes(InputStream inputStream, int numberOfBytes) throws IOException {
    byte[] result = new byte[numberOfBytes];
    int totalRead = 0;

    while (totalRead < numberOfBytes) {
      int bytesRead = inputStream.read(result, totalRead, numberOfBytes - totalRead);

      if (bytesRead == -1) {
        throw new IOException("Unexpected end of stream");
      }

      totalRead += bytesRead;
    }

    return result;
  }

  private static byte[] concatenateBytes(byte[] first, byte[] second) {
    byte[] result = new byte[first.length + second.length];
    System.arraycopy(first, 0, result, 0, first.length);
    System.arraycopy(second, 0, result, first.length, second.length);
    return result;
  }

  private static KeyAndIV deriveKeyAndIVFromPassword(String password, byte[] salt)
      throws NoSuchAlgorithmException {

    byte[] passwordBytes = password.getBytes(StandardCharsets.UTF_8);
    MessageDigest md5 = MessageDigest.getInstance(HASH_ALGORITHM_MD5);

    byte[] generatedData = generateKeyMaterial(md5, passwordBytes, salt);

    byte[] key = Arrays.copyOfRange(generatedData, 0, KEY_LENGTH);
    byte[] iv = Arrays.copyOfRange(generatedData, KEY_LENGTH, KEY_LENGTH + IV_LENGTH);

    return new KeyAndIV(key, iv);
  }

  private static byte[] generateKeyMaterial(MessageDigest md5, byte[] passwordBytes, byte[] salt) {
    int requiredLength = KEY_LENGTH + IV_LENGTH;
    byte[] generatedData = new byte[requiredLength];
    int generatedLength = 0;
    byte[] lastHash = null;

    while (generatedLength < requiredLength) {
      lastHash = hashPasswordWithSalt(md5, passwordBytes, salt, lastHash);
      generatedLength = appendHashToGeneratedData(generatedData, lastHash, generatedLength, requiredLength);
    }

    return generatedData;
  }

  private static byte[] hashPasswordWithSalt(MessageDigest md5, byte[] passwordBytes,
                                             byte[] salt, byte[] previousHash) {

    md5.reset();

    if (previousHash != null) {
      md5.update(previousHash);
    }

    md5.update(passwordBytes);
    md5.update(salt);

    return md5.digest();
  }

  private static int appendHashToGeneratedData(byte[] generatedData, byte[] hash,
                                               int currentLength, int requiredLength) {

    int copyLength = Math.min(hash.length, requiredLength - currentLength);
    System.arraycopy(hash, 0, generatedData, currentLength, copyLength);

    return currentLength + copyLength;
  }

  private static byte[] generateKeyFromPassword(String password) throws NoSuchAlgorithmException {
    MessageDigest sha256 = MessageDigest.getInstance(HASH_ALGORITHM_SHA256);
    return sha256.digest(password.getBytes(StandardCharsets.UTF_8));
  }
}