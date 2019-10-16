package com.codingwithmitch.foodrecipes.persistence;


import static android.arch.persistence.room.OnConflictStrategy.IGNORE;
import static android.arch.persistence.room.OnConflictStrategy.REPLACE;

import android.arch.lifecycle.LiveData;
import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Insert;
import android.arch.persistence.room.Query;

import com.codingwithmitch.foodrecipes.models.Recipe;

import java.util.List;

@Dao
public interface RecipeDao {

    /**
     * This inserts a list of recipes and returns an array of the indexes of what recipes were inserted.
     * If not inserted it will returned for -1 when
     * @param recipes
     * @return
     */
    @Insert(onConflict = IGNORE)
    long[] insertRecipes(Recipe... recipes);

    @Insert(onConflict = REPLACE)
    void insertRecipe(Recipe recipe);

    /**
     * We can write custom queries here
     */
    @Query("UPDATE recipes SET title = :title, publisher = :publisher, "
            + "image_url = :image_url, social_rank = :social_rank WHERE recipe_id = :recipe_id")
    void updateRecipe(String recipe_id, String title, String publisher,
            String image_url, float social_rank);

    /**
     * WE CAN RETURN AND GRAB LiveData in Room!!!!
     * @param query
     * @param pageNumber
     * @return
     */
    @Query("SELECT * FROM recipes WHERE title LIKE '%'|| :query ||'%' OR ingredients LIKE '%'|| :query ||'%'"
            + "ORDER BY social_rank DESC LIMIT (:pageNumber * 30)")
    LiveData<List<Recipe>> searchRecipes(String query, int pageNumber);

    /**
     * Using this in RecipeActivity
     * @param recipe_id
     * @return
     */
    @Query("SELECT * FROM recipes WHERE recipe_id = :recipe_id")
    LiveData<Recipe> getRecipe(String recipe_id);
}
