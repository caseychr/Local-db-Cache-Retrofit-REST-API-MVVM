package com.codingwithmitch.foodrecipes.adapters;

import android.support.annotation.NonNull;
import android.support.v7.widget.AppCompatImageView;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.ListPreloader;
import com.bumptech.glide.RequestManager;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.util.ViewPreloadSizeProvider;
import com.codingwithmitch.foodrecipes.R;
import com.codingwithmitch.foodrecipes.models.Recipe;

public class RecipeViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

    TextView title, publisher, socialScore;
    AppCompatImageView image;
    OnRecipeListener onRecipeListener;
    RequestManager mRequestManager;
    ViewPreloadSizeProvider mViewPreloadSizeProvider;

    public RecipeViewHolder(@NonNull View itemView, OnRecipeListener onRecipeListener,
            RequestManager manager, ViewPreloadSizeProvider sizeProvider) {
        super(itemView);

        this.onRecipeListener = onRecipeListener;
        mRequestManager = manager;
        mViewPreloadSizeProvider = sizeProvider;

        title = itemView.findViewById(R.id.recipe_title);
        publisher = itemView.findViewById(R.id.recipe_publisher);
        socialScore = itemView.findViewById(R.id.recipe_social_score);
        image = itemView.findViewById(R.id.recipe_image);

        itemView.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        onRecipeListener.onRecipeClick(getAdapterPosition());
    }

    public void onBind(Recipe recipe) {
        mRequestManager
                .load(recipe.getImage_url())
                .into(image);


        title.setText(recipe.getTitle());
        publisher.setText(recipe.getPublisher());
        socialScore.setText(String.valueOf(Math.round(recipe.getSocial_rank())));

        mViewPreloadSizeProvider.setView(image);
    }
}





