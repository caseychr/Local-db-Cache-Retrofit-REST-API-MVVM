package com.codingwithmitch.foodrecipes.repository;

import android.arch.lifecycle.LiveData;
import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import com.codingwithmitch.foodrecipes.AppExecutors;
import com.codingwithmitch.foodrecipes.models.Recipe;
import com.codingwithmitch.foodrecipes.persistence.RecipeDao;
import com.codingwithmitch.foodrecipes.persistence.RecipeDatabase;
import com.codingwithmitch.foodrecipes.requests.ServiceGenerator;
import com.codingwithmitch.foodrecipes.requests.responses.APIResponse;
import com.codingwithmitch.foodrecipes.requests.responses.RecipeResponse;
import com.codingwithmitch.foodrecipes.requests.responses.RecipeSearchResponse;
import com.codingwithmitch.foodrecipes.util.Constants;
import com.codingwithmitch.foodrecipes.util.NetworkBoundResource;
import com.codingwithmitch.foodrecipes.util.Resource;

import java.util.List;

public class RecipeRepository {
    private static final String TAG = "RecipeRepository";

    public static RecipeRepository instance;

    private RecipeDao mRecipeDao;

    public static RecipeRepository getInstance(Context context) {
        if(instance == null) {
            instance = new RecipeRepository(context);
        }
        return instance;
    }

    private RecipeRepository(Context context) {
        mRecipeDao = RecipeDatabase.getInstance(context).getRecipeDao();
    }

    /**
     * This is our search method for searching the API. We're returning all of the abstract methods from
     * NetworkBoundResource. Here we're deciding either to pull from cache or the network.
     *
     * We've utilized alot of the generic classes and wrappers to boil the decision for network/cache calls
     * down to only this. This is a good idea.
     * @param query
     * @param pageNumber
     * @return
     */
    public LiveData<Resource<List<Recipe>>> searchRecipesApi(final String query, final int pageNumber) {
        Log.i(TAG, "REPO SEARCH");
        return new NetworkBoundResource<List<Recipe>, RecipeSearchResponse>(AppExecutors.getInstance()){

            /**
             * Here we save the data from Retrofit into the cache
             * @param item
             */
            @Override
            protected void saveCallResult(@NonNull RecipeSearchResponse item) {
                Log.i(TAG, "saveCallResult");
                if(item.getRecipes() != null) { //recipe list can be null if API_KEY expires etc.
                    Recipe[] recipes = new Recipe[item.getRecipes().size()];
                    int index = 0;
                    for(long rowid: mRecipeDao.insertRecipes((Recipe[]) (item.getRecipes().toArray(recipes)))) {
                        if(rowid == -1) {
                            /**
                             * if the recipe already exists... I don't want to set the ingredients or timestamp
                             * b/c they will be erased. This is why we built to different methods and are looping
                             * through each recipe.
                             *
                             * In this case if the recipe already exists just update the entry with update();
                             */
                            Log.i(TAG, "saveCallResult: CONFLICT - Recipe is already in the cache");
                            mRecipeDao.updateRecipe(recipes[index].getRecipe_id(),
                                    recipes[index].getTitle(),
                                    recipes[index].getPublisher(),
                                    recipes[index].getImage_url(),
                                    recipes[index].getSocial_rank());
                        }
                        index++;
                    }
                }
            }

            /**
             * Deciding via timestamp if to refresh the cache.
             * Since we're returning true we always refresh the cache. Easier to do that.
             * @param data
             * @return
             */
            @Override
            protected boolean shouldFetch(@Nullable List<Recipe> data) {
                return true;
            }

            /**
             * Responsible for retrieving data from the local cache
             * @return
             */
            @NonNull
            @Override
            protected LiveData<List<Recipe>> loadFromDb() {
                Log.i(TAG, "loadFromDb");
                return mRecipeDao.searchRecipes(query, pageNumber);
            }

            /**
             * Creates a LiveData Retrofit call object. It uses Retrofit Converters to convert that to LiveData.
             * Fortunately this happens already on background threads so no need for Executors or Runnables. Yay!
             * @return
             */
            @NonNull
            @Override
            protected LiveData<APIResponse<RecipeSearchResponse>> createCall() {
                Log.i(TAG, "createCall");
                return ServiceGenerator.getRecipeApi().searchRecipe(Constants.API_KEY,
                        query, String.valueOf(pageNumber));
            }
        }.getAsLiveData();
    }

    public LiveData<Resource<Recipe>> searchRecipeApi(final String recipeId) {
        return new NetworkBoundResource<Recipe, RecipeResponse>(AppExecutors.getInstance()) {

            @Override
            protected void saveCallResult(@NonNull RecipeResponse item) {
                if(item.getRecipe() != null) {
                    item.getRecipe().setTimestamp((int) (System.currentTimeMillis()/1000));
                    mRecipeDao.insertRecipe(item.getRecipe());
                }
            }

            @Override
            protected boolean shouldFetch(@Nullable Recipe data) {
                Log.i(TAG, "shouldFetch: recipe: "+data.toString());
                int currentTime = (int)(System.currentTimeMillis()/1000);
                Log.i(TAG, "shouldFetch: current time: "+currentTime);
                int lastRefresh = data.getTimestamp();
                Log.i(TAG, "shouldFetch: last time: "+lastRefresh);
                Log.i(TAG, "shouldFetch: it's been: "+((currentTime - lastRefresh)/60/60/24));
                if(((currentTime) - data.getTimestamp()) >= Constants.RECIPE_REFRESH_TIME) {
                    Log.i(TAG, "should refresh recipe:"+true);
                    return true;
                }
                Log.i(TAG, "should refresh recipe:"+false);
                return false;
            }

            @NonNull
            @Override
            protected LiveData<Recipe> loadFromDb() {
                return mRecipeDao.getRecipe(recipeId);
            }

            @NonNull
            @Override
            protected LiveData<APIResponse<RecipeResponse>> createCall() {
                return ServiceGenerator.getRecipeApi().getRecipe(Constants.API_KEY, recipeId);
            }
        }.getAsLiveData();
    }

}
