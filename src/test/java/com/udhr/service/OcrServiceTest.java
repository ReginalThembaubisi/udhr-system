package com.udhr.service;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Reading the ingredients off a food label photo, and never making them up. */
class OcrServiceTest {

    private final OcrService ocr = new OcrService();

    @Test
    void picksTheIngredientsListOutOfTheLabelText() {
        OcrService.Result result = OcrService.pickIngredients(
                "CRUNCHY PEANUT BITES\nIngredients: Wheat flour, sugar, pea-\nnuts, salt. Contains: peanuts, gluten.\n"
                + "Nutritional information per 100g Energy 2100kJ\nBest before: see pack");
        assertThat(result.foundIngredientsList()).isTrue();
        assertThat(result.text()).isEqualTo("Wheat flour, sugar, peanuts, salt. Contains: peanuts, gluten.");

        // The heading wins over the word earlier in a sentence.
        assertThat(OcrService.pickIngredients("Type an ingredients list or scan a label. Ingredients: Maize, salt.").text())
                .isEqualTo("Maize, salt.");

        OcrService.Result noHeading = OcrService.pickIngredients("Maize meal, salt\nvitamin A");
        assertThat(noHeading.foundIngredientsList()).isFalse();
        assertThat(noHeading.text()).isEqualTo("Maize meal, salt vitamin A");
    }

    @Test
    void refusesFilesThatAreNotPhotos() {
        assertThatThrownBy(() -> ocr.extractIngredients(new MockMultipartFile("file", "juice.pdf", "application/pdf", new byte[] {1, 2, 3})))
                .hasMessageContaining("JPG or PNG");
        assertThatThrownBy(() -> ocr.extractIngredients(new MockMultipartFile("file", "label.jpg", "image/jpeg", new byte[0])))
                .hasMessageContaining("Choose a photo");
    }

    @Test
    void readsARealLabelImage() throws Exception {
        Assumptions.assumeTrue(tesseractInstalled(), "Tesseract isn't installed here (it is in the Docker image)");

        MockMultipartFile photo = new MockMultipartFile("file", "IMG_1234.jpg", "image/png",
                label("INGREDIENTS: Maize meal, Sugar, Peanuts, Salt.", "Nutritional information per 100 g"));
        OcrService.Result result = ocr.extractIngredients(photo);
        assertThat(result.foundIngredientsList()).isTrue();
        assertThat(result.text().toLowerCase()).contains("maize").contains("peanuts").doesNotContain("nutritional");
        // Nothing invented from the file name or anywhere else.
        assertThat(result.text().toLowerCase()).doesNotContain("wheat").doesNotContain("milk powder");

        MockMultipartFile blank = new MockMultipartFile("file", "blank.png", "image/png", label());
        assertThatThrownBy(() -> ocr.extractIngredients(blank)).hasMessageContaining("Couldn't read any text");
    }

    private static byte[] label(String... lines) throws Exception {
        BufferedImage image = new BufferedImage(1400, 120 + 70 * lines.length, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, image.getWidth(), image.getHeight());
        g.setColor(Color.BLACK);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 40));
        for (int i = 0; i < lines.length; i++) {
            g.drawString(lines[i], 40, 90 + 70 * i);
        }
        g.dispose();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(image, "png", out);
        return out.toByteArray();
    }

    private static boolean tesseractInstalled() {
        try {
            return new ProcessBuilder("tesseract", "--version").redirectErrorStream(true).start().waitFor() == 0;
        } catch (Exception e) {
            return false;
        }
    }
}
