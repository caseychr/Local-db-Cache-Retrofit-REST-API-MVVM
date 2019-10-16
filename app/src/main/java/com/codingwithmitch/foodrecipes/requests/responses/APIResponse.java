package com.codingwithmitch.foodrecipes.requests.responses;

import com.codingwithmitch.foodrecipes.models.Recipe;

import java.io.IOException;

import retrofit2.Response;

/**
 * 
 * @param <T>
 */

public class APIResponse<T> {

    public APIResponse<T> create(Throwable error) {
        return new ApiErrorResponse<>(!error.getMessage().equals("") ? error.getMessage() :
                "Unknown error\nCheck network connection");
    }

    public APIResponse<T> create(Response<T> response) {
        if(response.isSuccessful()) {
            T body = response.body();

            if(body instanceof RecipeSearchResponse) {
                if(!CheckRecipeApiKey.isRecipeAPIKeyValid((RecipeSearchResponse) body)) {
                    return new ApiErrorResponse<>("some error message");
                }
            }

            if(body instanceof RecipeResponse) {
                if(!CheckRecipeApiKey.isRecipeAPIKeyValid((RecipeResponse) body)) {
                    return new ApiErrorResponse<>("some error message");
                }
            }

            // Request was successful with no body
            if(body == null || response.code() == 204) {
                return new ApiEmptyResponse<>();
            } else {
                return new ApiSuccessResponse<>(body);
            }
        } else {
            String errorMessage = "";
            try {
                errorMessage = response.errorBody().string();
            } catch (IOException e) {
                e.printStackTrace();
                errorMessage = response.message();
            }
            return new ApiErrorResponse<>(errorMessage);
        }
    }

    public class ApiSuccessResponse<T> extends APIResponse<T> {

        private T body;

        public ApiSuccessResponse(T body) {
            this.body = body;
        }

        public T getBody() {
            return body;
        }
    }

    public class ApiErrorResponse<T> extends APIResponse<T> {

        private String errorMessage;

        public ApiErrorResponse(String errorMessage) {
            this.errorMessage = errorMessage;
        }

        public String getErrorMessage() {
            return errorMessage;
        }
    }

    public class ApiEmptyResponse<T> extends APIResponse<T> {
    }
}