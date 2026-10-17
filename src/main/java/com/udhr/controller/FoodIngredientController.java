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
        try {
            OcrService.Result result = ocrService.extractIngredients(file);
            Map<String, Object> response = new HashMap<>();
            response.put("extractedText", result.text());
            response.put("foundIngredientsList", result.foundIngredientsList());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", e.getMessage()));
        }
    }

    @GetMapping("/search")
    public ResponseEntity<?> searchIngredients(@RequestParam("type") String type, @RequestParam("query") String query) {
        try {
            String ingredients = "";
            if ("barcode".equalsIgnoreCase(type)) {
                ingredients = openFoodFactsService.fetchIngredientsByBarcode(query);
            } else {
                ingredients = openFoodFactsService.fetchIngredientsBySearch(query);
            }

            Map<String, String> response = new HashMap<>();
            response.put("ingredients", ingredients);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    @PostMapping("/check")
    public ResponseEntity<?> checkIngredientsSafety(@RequestBody Map<String, String> request) {
        try {
            String idNumber = getLoggedInPatientId();
            String ingredientsText = request.get("ingredients");
            if (ingredientsText == null) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Ingredients text is required");
            }

            List<IngredientCheckResult> results = foodIngredientService.checkIngredients(idNumber, ingredientsText);
            return ResponseEntity.ok(results);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }
}
