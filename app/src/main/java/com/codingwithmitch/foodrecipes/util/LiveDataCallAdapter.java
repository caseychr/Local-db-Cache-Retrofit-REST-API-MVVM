package com.codingwithmitch.foodrecipes.util;

import android.arch.lifecycle.LiveData;

import com.codingwithmitch.foodrecipes.requests.responses.APIResponse;

import java.lang.reflect.Type;

import retrofit2.Call;
import retrofit2.CallAdapter;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Takes Retrofit Calls and converts them into LiveData
 *
 * Generic type R is the Retrofit Response Type
 *
 * This still requires a factory class to produce these Adapters before we can implement in ServiceGenerator.
 * @param <R>
 */
public class LiveDataCallAdapter<R> implements CallAdapter<R, LiveData<APIResponse<R>>> {

    private Type responseType;

    public LiveDataCallAdapter(Type responseType) {
        this.responseType = responseType;
    }

    @Override
    public Type responseType() {
        return responseType;
    }

    @Override
    public LiveData<APIResponse<R>> adapt(final Call<R> call) {
        return new LiveData<APIResponse<R>>() {
            @Override
            protected void onActive() {
                super.onActive();
                final APIResponse apiResponse = new APIResponse();
                call.enqueue(new Callback<R>() {
                    @Override
                    public void onResponse(Call<R> call, Response<R> response) {
                        postValue(apiResponse.create(response));
                    }

                    @Override
                    public void onFailure(Call<R> call, Throwable t) {
                        postValue(apiResponse.create(t));
                    }
                });
            }
        };
    }
}
