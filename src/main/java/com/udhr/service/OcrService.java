package com.udhr.service;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Reads the ingredients list off a photo of a food label with Tesseract, the
 * open-source text reader installed in the server image (see Dockerfile).
 *
 * It never makes text up: when a photo can't be read, the patient is told so
 * and can type the list instead. (An earlier version returned a fixed list
 * chosen from the file name whenever Tesseract was missing, so every phone
 * photo "contained" the same wheat, sugar and peanut butter.)
 */
@Service
public class OcrService {

    /** What came off the label, and whether an "Ingredients" heading was found on it. */
    public record Result(String text, boolean foundIngredientsList) {}

    private static final Set<String> IMAGE_TYPES = Set.of("image/jpeg", "image/jpg", "image/png", "image/webp");
    private static final long TIMEOUT_SECONDS = 40;

    private static final Pattern INGREDIENTS_HEADING = Pattern.compile("(?i)\\bingred[il1]ents?\\b\\s*[:;.\\-]?");
    // Where an ingredients list usually ends on a label. "Contains" and "may
    // contain" are kept: allergen statements are what an allergy check needs.
    private static final Pattern LIST_END = Pattern.compile(
            "(?i)\\b(?:nutrition(?:al)? information|nutrition facts|typical values|energy\\s*\\(?kj|store in|storage|"
            + "best before|use by|keep refrigerated|manufactured by|produced by|packed by|distributed by|net (?:weight|wt|mass))\\b");

    public Result extractIngredients(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Choose a photo of the label first.");
        }
        String contentType = file.getContentType() == null ? "" : file.getContentType().toLowerCase();
        if (!IMAGE_TYPES.contains(contentType)) {
            throw new IllegalArgumentException("Please upload a JPG or PNG photo.");
        }

        String raw = runTesseract(file);
        if (raw.replaceAll("[^A-Za-z]", "").length() < 6) {
            throw new IllegalArgumentException("Couldn't read any text in that photo. Take a clear, well-lit photo of just the "
                    + "ingredients list, or type the ingredients instead.");
        }
        return pickIngredients(raw);
    }

    /** The ingredients part of the label text, joined onto one line. */
    static Result pickIngredients(String raw) {
        // Words split across lines ("pre-\nservatives") are joined back up.
        String text = raw.replaceAll("-\\s*\\n\\s*", "").replaceAll("\\s+", " ").trim();

        Matcher heading = INGREDIENTS_HEADING.matcher(text);
        if (!heading.find()) {
            return new Result(text, false);
        }
        String list = text.substring(heading.end()).trim();
        Matcher end = LIST_END.matcher(list);
        if (end.find() && end.start() > 0) {
            list = list.substring(0, end.start()).trim();
        }
        return new Result(list.isEmpty() ? text : list, true);
    }

    private String runTesseract(MultipartFile file) {
        Path image = null;
        Process process = null;
        try {
            // A name of our own: the uploaded file's name never reaches the disk.
            image = Files.createTempFile("label-", ".img");
            try (InputStream in = file.getInputStream()) {
                Files.copy(in, image, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            }
            // "stdout" makes Tesseract print the text instead of writing a file;
            // page mode 6 reads the photo as one block of text, which suits a label.
            process = new ProcessBuilder("tesseract", image.toString(), "stdout", "-l", "eng", "--psm", "6")
                    .redirectError(ProcessBuilder.Redirect.DISCARD)
                    .start();
            byte[] out = process.getInputStream().readAllBytes();
            if (!process.waitFor(TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                throw new IllegalStateException("Reading the label took too long. Try a smaller, clearer photo.");
            }
            if (process.exitValue() != 0) {
                throw new IllegalStateException("Couldn't read that photo. Try a JPG or PNG of just the ingredients list.");
            }
            return new String(out, StandardCharsets.UTF_8);
        } catch (IOException e) {
            // Usually Tesseract isn't installed (a local run outside the Docker image).
            throw new IllegalStateException("Reading photos isn't available right now. Please type the ingredients instead.", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Reading the label was interrupted. Please try again.", e);
        } finally {
            if (process != null && process.isAlive()) {
                process.destroyForcibly();
            }
            if (image != null) {
                try {
                    Files.deleteIfExists(image);
                } catch (IOException ignored) {
                    // A temp file left behind is cleared with the container.
                }
            }
        }
    }
}
