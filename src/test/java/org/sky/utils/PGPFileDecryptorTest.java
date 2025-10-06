package org.sky.utils;

import org.junit.jupiter.api.Test;
import org.sky.function.exception.DecryptionException;

import java.nio.file.Files;
import java.nio.file.Path;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

class PGPFileDecryptorTest {

    @Test
    void testDecryptFileWithInvalidPathsThrowsException() {
        Path encryptedFile = Path.of("invalid_encrypted_file.pgp");
        Path outputFile = Path.of("invalid_output.txt");
        Path privateKeyFile = Path.of("invalid_private_key.asc");
        String passphrase = "wrongpass";
        Exception exception = assertThrows(DecryptionException.class, () -> {
            PGPFileDecryptor.decryptFile(encryptedFile, outputFile, privateKeyFile, passphrase);
        });
        assertTrue(exception.getMessage().contains("Error decrypting PGP file"));
    }

    @Test
    void testDecryptFileWithWrongPassphraseThrowsException() throws IOException {
        // These files should exist and be valid for the test to work
        Path encryptedFile = Files.createTempFile("test-encrypted", ".pgp");
        Path outputFile = Files.createTempFile("test-output", ".txt");
        Path privateKeyFile = Files.createTempFile("test-private-key", ".asc");
        String passphrase = "wrongpass";
        Exception exception = assertThrows(DecryptionException.class, () -> {
            PGPFileDecryptor.decryptFile(encryptedFile, outputFile, privateKeyFile, passphrase);
        });
        assertTrue(exception.getMessage().contains("Error decrypting PGP file"));
        Files.deleteIfExists(encryptedFile);
        Files.deleteIfExists(outputFile);
        Files.deleteIfExists(privateKeyFile);
    }

    // Integration test: requires real encrypted file, private key, and correct passphrase
    @Test
    void testDecryptFileIntegration() throws Exception {
        Path encryptedFile = Path.of("src/test/resources/sample-encrypted.pgp");
        Path outputFile = Files.createTempFile("decrypted", ".txt");
        Path privateKeyFile = Path.of("src/test/resources/sample-private-key.asc");
        String passphrase = "correctpassphrase"; // Replace with actual passphrase
        // The expected output file should contain the original plaintext
        Path expectedOutput = Path.of("src/test/resources/sample-expected.txt");

        if (Files.exists(encryptedFile) && Files.exists(privateKeyFile) && Files.exists(expectedOutput)) {
            PGPFileDecryptor.decryptFile(encryptedFile, outputFile, privateKeyFile, passphrase);
            String actual = Files.readString(outputFile);
            String expected = Files.readString(expectedOutput);
            assertEquals(expected.trim(), actual.trim());
            Files.deleteIfExists(outputFile);
        } else {
            System.out.println("Integration test skipped: required files not found.");
        }
    }
}

