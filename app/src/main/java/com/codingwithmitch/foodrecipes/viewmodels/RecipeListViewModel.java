package com.codingwithmitch.foodrecipes.viewmodels;


import android.app.Application;
import android.arch.lifecycle.AndroidViewModel;
import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MediatorLiveData;
import android.arch.lifecycle.MutableLiveData;
import android.arch.lifecycle.Observer;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import com.codingwithmitch.foodrecipes.models.Recipe;
import com.codingwithmitch.foodrecipes.repository.RecipeRepository;
import com.codingwithmitch.foodrecipes.util.Resource;

import java.util.List;

public class RecipeListViewModel extends AndroidViewModel {

    private static final String TAG = "RecipeListViewModel";

    public static final String NO_MORE_RESULTS = "NO_MORE_RESULTS";

    /**
     * Same as using public state final int CATEGORY = 1, etc.
     */
    public enum ViewState {CATEGORIES, RECIPES};

    /**
     * For observing and changing the ViewState.
     *
     * MutableLiveData vs LiveData -> in LiveData class setValue() and postValue() are not public methods. Also
     * LiveData is an immutable class. MutableLiveData inherits from LiveData but the setValue() and postValue()
     * ARE public and reachable so we can call those methods and update the value which will update all observers.
     * setValue() is for updating LiveData on the main thread and postValue() is for doing it on a background thread.
     */
    public MutableLiveData<ViewState> viewState;

    /**
     * We want to alter the LiveData returned from the request before displaying it in the UI
     * which is why we're using MediatorLiveData
     */
    public MediatorLiveData<Resource<List<Recipe>>> mRecipes = new MediatorLiveData<>();

    private RecipeRepository mRecipeRepository;

    //Query Extras
    private boolean mIsQueryExhausted;
    private boolean mIsPerformingQuery;
    private int mPageNumber;
    private String mQuery;
    private boolean mCancelRequest;
    private long mRequestStartTime;

    public RecipeListViewModel(@NonNull Application application) {
        super(application);
        mRecipeRepository = RecipeRepository.getInstance(application);
        init();
    }

    public void init() {
        if(viewState == null) {
            viewState = new MutableLiveData<>();
            viewState.setValue(ViewState.CATEGORIES);
        }
    }

    public LiveData<ViewState> getViewState() {
        return viewState;
    }

    public LiveData<Resource<List<Recipe>>> getRecipes() {
        return mRecipes;
    }

    public int getPageNumber() {
        return mPageNumber;
    }

    public void setViewCategories() {
        viewState.setValue(ViewState.CATEGORIES);
    }

    public void searchRecipesApi(String query, int pageNumber) {
        if(!mIsPerformingQuery) {
            if(pageNumber == 0) {
                pageNumber = 1;
            }
            this.mPageNumber = pageNumber;
            this.mQuery = query;
            mIsQueryExhausted = false;
            executeSearch();
        }
    }

    public void searchNextPage() {
        if(!mIsQueryExhausted && !mIsPerformingQuery) {
            mPageNumber++;
            executeSearch();
        }
    }

    private void executeSearch() {
        mRequestStartTime = System.currentTimeMillis();
        mCancelRequest = false;
        mIsPerformingQuery = true;
        viewState.setValue(ViewState.RECIPES);
        final LiveData<Resource<List<Recipe>>> repositorySource = mRecipeRepository.searchRecipesApi(mQuery, mPageNumber);
        mRecipes.addSource(repositorySource, new Observer<Resource<List<Recipe>>>() {
            @Override
            public void onChanged(@Nullable Resource<List<Recipe>> listResource) {
                // We can do some stuff to the data before returning it to the UI
                if(!mCancelRequest) {
                    if(listResource != null) {
                        mRecipes.setValue(listResource);
                        if(listResource.status == Resource.Status.SUCCESS) {
                            Log.i(TAG, "onChanged REQUEST TIME: "+ (System.currentTimeMillis() - mRequestStartTime)/1000+" secs");
                            mIsPerformingQuery = false;
                            if(listResource.data != null) {
                                if(listResource.data.size() == 0) {
                                    Log.i(TAG, "onChanged: query is exhausted");
                                    mRecipes.setValue(new Resource<List<Recipe>>(Resource.Status.ERROR
                                            , listResource.data, NO_MORE_RESULTS));
                                }
                            }
                            mRecipes.removeSource(repositorySource);
                        } else if(listResource.status == Resource.Status.ERROR) {
                            mIsPerformingQuery = false;
                            mRecipes.removeSource(repositorySource);
                        }
                    } else {
                        // We have to remove source otherwise we keep pushing to observers!
                        mRecipes.removeSource(repositorySource);
                    }
                } else {
                    mRecipes.removeSource(repositorySource);
                }
            }
        });
    }

    public void cancelSearchRequest() {
        if(mIsPerformingQuery) {
            Log.i(TAG, "Canceling Search Query");
            mCancelRequest = true;
            mIsPerformingQuery = false;
            mPageNumber = 1;
        }
    }
}


