package com.codingwithmitch.foodrecipes.requests.responses;

public class CheckRecipeApiKey {

    protected static boolean isRecipeAPIKeyValid(RecipeSearchResponse response) {
        return response.getError() == null;
    }

    protected static boolean isRecipeAPIKeyValid(RecipeResponse response) {
        return response.getError() == null;
    }
}
