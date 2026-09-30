package com.cornelius.storage;

import com.cornelius.util.Logger;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.zip.Deflater;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.Inflater;
import java.util.zip.InflaterInputStream;

public class CompressionEngine {

    public record CompressionResult(
            byte[] compressedData,
            long originalBytes,
            long compressedBytes,
            double compressionRatioPercent,
            String sha256Checksum
    ) {}

    public static CompressionResult compress(byte[] data, int level) {
        if (data == null || data.length == 0) {
            return new CompressionResult(new byte[0], 0, 0, 0.0, "");
        }

        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            Deflater deflater = new Deflater(level, false);
            try (DeflaterOutputStream dos = new DeflaterOutputStream(baos, deflater)) {
                dos.write(data);
                dos.finish();
            }

            byte[] compressed = baos.toByteArray();
            long originalSize = data.length;
            long compSize = compressed.length;
            double ratio = (1.0 - ((double) compSize / (double) originalSize)) * 100.0;
            String hash = calculateSha256(data);

            return new CompressionResult(compressed, originalSize, compSize, Math.max(0.0, ratio), hash);
        } catch (IOException e) {
            Logger.error("Compression", "Falha na compressão de dados: " + e.getMessage());
            throw new RuntimeException("Falha ao comprimir", e);
        }
    }

    public static byte[] decompress(byte[] compressedData) {
        if (compressedData == null || compressedData.length == 0) {
            return new byte[0];
        }

        try {
            ByteArrayInputStream bais = new ByteArrayInputStream(compressedData);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            Inflater inflater = new Inflater(false);
            try (InflaterInputStream iis = new InflaterInputStream(bais, inflater)) {
                byte[] buffer = new byte[8192];
                int len;
                while ((len = iis.read(buffer)) > 0) {
                    baos.write(buffer, 0, len);
                }
            }
            return baos.toByteArray();
        } catch (IOException e) {
            Logger.error("Compression", "Falha na descompressão de dados: " + e.getMessage());
            throw new RuntimeException("Falha ao descomprimir", e);
        }
    }

    public static CompressionResult compressString(String text, int level) {
        byte[] bytes = text.getBytes(StandardCharsets.UTF_8);
        return compress(bytes, level);
    }

    public static String decompressString(byte[] compressedData) {
        byte[] raw = decompress(compressedData);
        return new String(raw, StandardCharsets.UTF_8);
    }

    public static CompressionResult compressFileToArchive(Path sourceFile, Path targetArchive, int level) throws IOException {
        byte[] fileBytes = Files.readAllBytes(sourceFile);
        CompressionResult result = compress(fileBytes, level);

        if (targetArchive.getParent() != null && !Files.exists(targetArchive.getParent())) {
            Files.createDirectories(targetArchive.getParent());
        }

        // Custom binary format with Header:
        // [4 bytes MAGIC "CRNZ"] [8 bytes original size] [8 bytes compressed size] [32 bytes SHA-256 hash] [payload]
        try (DataOutputStream dos = new DataOutputStream(new BufferedOutputStream(Files.newOutputStream(targetArchive)))) {
            dos.writeBytes("CRNZ");
            dos.writeLong(result.originalBytes());
            dos.writeLong(result.compressedBytes());
            byte[] hashBytes = result.sha256Checksum().getBytes(StandardCharsets.UTF_8);
            dos.writeInt(hashBytes.length);
            dos.write(hashBytes);
            dos.write(result.compressedData());
        }

        Logger.info("Compression", String.format("Arquivo comprimido: %s -> %s (Original: %s, Comprimido: %s, Economia: %.2f%%)",
                sourceFile.getFileName(), targetArchive.getFileName(),
                formatBytes(result.originalBytes()), formatBytes(result.compressedBytes()),
                result.compressionRatioPercent()));

        return result;
    }

    public static byte[] decompressArchive(Path archiveFile) throws IOException {
        try (DataInputStream dis = new DataInputStream(new BufferedInputStream(Files.newInputStream(archiveFile)))) {
            byte[] magic = new byte[4];
            dis.readFully(magic);
            String magicStr = new String(magic, StandardCharsets.UTF_8);
            if (!"CRNZ".equals(magicStr)) {
                // If not custom header, try direct raw decompress
                byte[] raw = Files.readAllBytes(archiveFile);
                return decompress(raw);
            }
            long origSize = dis.readLong();
            long compSize = dis.readLong();
            int hashLen = dis.readInt();
            byte[] hashBytes = new byte[hashLen];
            dis.readFully(hashBytes);
            String expectedHash = new String(hashBytes, StandardCharsets.UTF_8);

            byte[] compressedData = dis.readAllBytes();
            byte[] decompressed = decompress(compressedData);

            String actualHash = calculateSha256(decompressed);
            if (!expectedHash.equalsIgnoreCase(actualHash)) {
                Logger.warn("Compression", "Aviso de integridade: Checksum SHA-256 diverge!");
            }

            return decompressed;
        }
    }

    public static String calculateSha256(byte[] data) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(data);
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            return "";
        }
    }

    public static String formatBytes(long bytes) {
        if (bytes < 1024) return bytes + " B";
        int exp = (int) (Math.log(bytes) / Math.log(1024));
        String pre = "KMGTPE".charAt(exp - 1) + "B";
        return String.format("%.2f %s", bytes / Math.pow(1024, exp), pre);
    }
}

