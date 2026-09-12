package com.udhr.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Service
public class OpenFoodFactsService {

    @Autowired
    private RestTemplate restTemplate;

    private static final String OFF_PRODUCT_URL = "https://world.openfoodfacts.org/api/v2/product/";
    private static final String OFF_SEARCH_URL = "https://world.openfoodfacts.org/cgi/search.pl";

    public String fetchIngredientsByBarcode(String barcode) {
        if (barcode == null || barcode.trim().isEmpty()) {
            throw new IllegalArgumentException("A barcode is required.");
        }

        try {
            String url = OFF_PRODUCT_URL + URLEncoder.encode(barcode.trim(), StandardCharsets.UTF_8) + ".json";
            ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Map<String, Object> body = response.getBody();
                if (body.get("product") != null) {
                    Map<String, Object> product = (Map<String, Object>) body.get("product");

                    String ingredients = "";
                    if (product.get("ingredients_text_en") != null) {
                        ingredients = (String) product.get("ingredients_text_en");
                    } else if (product.get("ingredients_text") != null) {
                        ingredients = (String) product.get("ingredients_text");
                    }

                    if (!ingredients.isEmpty()) {
                        return ingredients;
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Open Food Facts barcode query failed. Error: " + e.getMessage());
            throw new IllegalStateException("Couldn't reach Open Food Facts to look up that barcode. Try again or type the ingredients manually.", e);
        }

        // A real, well-formed response with no usable ingredients text is a
        // "not found," never a made-up product -- never invent ingredients
        // for a check that decides whether something is safe to eat.
        throw new IllegalStateException("No ingredients were found for that barcode. Try again or type the ingredients manually.");
    }

    public String fetchIngredientsBySearch(String query) {
        if (query == null || query.trim().isEmpty()) {
            throw new IllegalArgumentException("A search term is required.");
        }

        try {
            String url = OFF_SEARCH_URL + "?search_terms=" + URLEncoder.encode(query.trim(), StandardCharsets.UTF_8) + "&json=1&limit=3";
            ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Map<String, Object> body = response.getBody();
                List<Map<String, Object>> products = (List<Map<String, Object>>) body.get("products");

                if (products != null && !products.isEmpty()) {
                    // Find first product with ingredients
                    for (Map<String, Object> p : products) {
                        String ingredients = "";
                        if (p.get("ingredients_text_en") != null) {
                            ingredients = (String) p.get("ingredients_text_en");
                        } else if (p.get("ingredients_text") != null) {
                            ingredients = (String) p.get("ingredients_text");
                        }

                        if (!ingredients.isEmpty()) {
                            return ingredients;
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Open Food Facts search query failed. Error: " + e.getMessage());
            throw new IllegalStateException("Couldn't reach Open Food Facts to search for that product. Try again or type the ingredients manually.", e);
        }

        throw new IllegalStateException("No matching products were found. Try a different search term or type the ingredients manually.");
    }
}
