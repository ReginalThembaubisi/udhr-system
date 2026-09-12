package com.udhr.controller;

import com.udhr.dto.IngredientCheckResult;
import com.udhr.service.FoodIngredientService;
import com.udhr.service.OcrService;
import com.udhr.service.OpenFoodFactsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.util.*;

@RestController
@RequestMapping("/api/food-checker")
public class FoodIngredientController {

    @Autowired
    private OcrService ocrService;

    @Autowired
    private OpenFoodFactsService openFoodFactsService;

    @Autowired
    private FoodIngredientService foodIngredientService;

    private String getLoggedInPatientId() {
        return (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }

    @PostMapping("/ocr")
    public ResponseEntity<?> parseImageLabel(@RequestParam("file") MultipartFile file) {
        String extractedText = ocrService.extractTextFromImage(file);
        Map<String, String> response = new HashMap<>();
        response.put("extractedText", extractedText);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/search")
    public ResponseEntity<?> searchIngredients(@RequestParam("type") String type, @RequestParam("query") String query) {
        String ingredients = "barcode".equalsIgnoreCase(type)
                ? openFoodFactsService.fetchIngredientsByBarcode(query)
                : openFoodFactsService.fetchIngredientsBySearch(query);

        Map<String, String> response = new HashMap<>();
        response.put("ingredients", ingredients);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/check")
    public ResponseEntity<?> checkIngredientsSafety(@RequestBody Map<String, String> request) {
        String idNumber = getLoggedInPatientId();
        String ingredientsText = request.get("ingredients");
        if (ingredientsText == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", "Ingredients text is required"));
        }

        List<IngredientCheckResult> results = foodIngredientService.checkIngredients(idNumber, ingredientsText);
        return ResponseEntity.ok(results);
    }
}
