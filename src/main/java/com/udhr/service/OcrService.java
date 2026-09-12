package com.udhr.service;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.util.UUID;

@Service
public class OcrService {

    private static final long MAX_FILE_SIZE_BYTES = 5L * 1024 * 1024; // 5MB, matches the frontend limit

    /**
     * Runs Tesseract OCR against the uploaded label photo and returns the
     * extracted text. This never fabricates a result: if the image can't be
     * read (missing/failed Tesseract install, corrupt image, blank output) it
     * throws, so the caller can tell the patient the scan didn't work rather
     * than silently showing guessed ingredients for a safety-relevant check.
     */
    public String extractTextFromImage(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("No image was uploaded.");
        }
        if (file.getSize() > MAX_FILE_SIZE_BYTES) {
            throw new IllegalArgumentException("Image is too large (max 5MB).");
        }
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new IllegalArgumentException("Only image files are supported.");
        }

        // Never trust the client-supplied filename for a filesystem path
        // (it could contain "../" traversal sequences). Derive a safe,
        // server-generated name and a bounded extension instead.
        String extension = switch (contentType) {
            case "image/png" -> ".png";
            case "image/jpeg" -> ".jpg";
            case "image/webp" -> ".webp";
            default -> "";
        };

        String tempDir = System.getProperty("java.io.tmpdir");
        File tempFile = new File(tempDir, "udhr-ocr-" + UUID.randomUUID() + extension);
        File outputBase = new File(tempDir, "udhr-ocr-" + UUID.randomUUID());
        File txtResult = new File(outputBase.getAbsolutePath() + ".txt");

        try {
            file.transferTo(tempFile);

            ProcessBuilder pb = new ProcessBuilder("tesseract", tempFile.getAbsolutePath(), outputBase.getAbsolutePath());
            pb.redirectErrorStream(true);
            Process process;
            try {
                process = pb.start();
            } catch (Exception e) {
                throw new IllegalStateException(
                        "Label scanning isn't available right now (OCR engine not installed). " +
                        "Please type the ingredients in manually.", e);
            }

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    System.out.println("[Tesseract CLI] " + line);
                }
            }

            int exitCode = process.waitFor();
            if (exitCode != 0 || !txtResult.exists()) {
                throw new IllegalStateException(
                        "Couldn't read any text from that photo. Try a clearer, well-lit photo of the " +
                        "ingredients list, or type them in manually.");
            }

            String textContent = Files.readString(txtResult.toPath()).trim();
            if (textContent.isEmpty()) {
                throw new IllegalStateException(
                        "Couldn't read any text from that photo. Try a clearer, well-lit photo of the " +
                        "ingredients list, or type them in manually.");
            }
            return textContent;
        } catch (java.io.IOException e) {
            throw new IllegalStateException("Failed to process the uploaded image.", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Image processing was interrupted.", e);
        } finally {
            try {
                Files.deleteIfExists(tempFile.toPath());
                Files.deleteIfExists(txtResult.toPath());
            } catch (java.io.IOException ignored) {
                // best-effort cleanup
            }
        }
    }
}
