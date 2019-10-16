package com.codingwithmitch.foodrecipes.viewmodels;


import android.app.Application;
import android.arch.lifecycle.AndroidViewModel;
import android.arch.lifecycle.LiveData;
import android.support.annotation.NonNull;

import com.codingwithmitch.foodrecipes.models.Recipe;
import com.codingwithmitch.foodrecipes.repository.RecipeRepository;
import com.codingwithmitch.foodrecipes.util.Resource;


public class RecipeViewModel extends AndroidViewModel {

    private RecipeRepository mRecipeRepository;


    public RecipeViewModel(@NonNull Application application) {
        super(application);
        mRecipeRepository = RecipeRepository.getInstance(application);
    }

    public LiveData<Resource<Recipe>> searchRecipeAPi(String recipeId) {
        return mRecipeRepository.searchRecipeApi(recipeId);
    }
}





















