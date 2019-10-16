package com.codingwithmitch.foodrecipes.util;

import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MediatorLiveData;
import android.arch.lifecycle.Observer;
import android.support.annotation.MainThread;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.WorkerThread;
import android.util.Log;

import com.codingwithmitch.foodrecipes.AppExecutors;
import com.codingwithmitch.foodrecipes.requests.responses.APIResponse;

// CacheObject: Type for the Resource data. (database cache)
// RequestObject: Type for the API response. (network request)

/**
 * Very Important class. Dectates whether we return data from cache or not
 * @param <CacheObject>
 * @param <RequestObject>
 */
public abstract class NetworkBoundResource<CacheObject, RequestObject> {
    private static final String TAG = "NetworkBoundResource";

    private AppExecutors mAppExecutors;

    // Data the is observed in the UI
    private MediatorLiveData<Resource<CacheObject>> results = new MediatorLiveData<>();

    public NetworkBoundResource(AppExecutors appExecutors) {
        mAppExecutors = appExecutors;
        init();
    }

    private void init() {
        // update LiveData for loading status
        results.setValue((Resource<CacheObject>) Resource.loading(null));

        // observe LiveData source from local DB
        final LiveData<CacheObject> dbSource = loadFromDb();

        results.addSource(dbSource, new Observer<CacheObject>() {
            @Override
            public void onChanged(@Nullable CacheObject cacheObject) {
                results.removeSource(dbSource);
                if(shouldFetch(cacheObject)) {
                    // get data from network
                    fetchFromNetwork(dbSource);
                } else {
                    results.addSource(dbSource, new Observer<CacheObject>() {
                        @Override
                        public void onChanged(@Nullable CacheObject cacheObject) {
                            setValue(Resource.success(cacheObject));
                        }
                    });
                }
            }
        });
    }

    /**
     * 1) Observe local DB
     * 2) If condition then query network
     * 3) Stop observing local DB
     * 4) Insert new Data into local DB
     * 5) Begin observing local DB again to see refreshed state from network
     */
    private void fetchFromNetwork(final LiveData<CacheObject> dbSource) {
        results.addSource(dbSource, new Observer<CacheObject>() {
            @Override
            public void onChanged(@Nullable CacheObject cacheObject) {
                setValue(Resource.loading(cacheObject));
            }
        });
        final LiveData<APIResponse<RequestObject>> apiResponse = createCall();

        results.addSource(apiResponse, new Observer<APIResponse<RequestObject>>() {
            @Override
            public void onChanged(@Nullable final APIResponse<RequestObject> requestObjectAPIResponse) {
                results.removeSource(dbSource);
                results.removeSource(apiResponse);

                if(requestObjectAPIResponse instanceof APIResponse.ApiSuccessResponse) {
                    mAppExecutors.diskIO().execute(new Runnable() {
                        @Override
                        public void run() {

                            // save response to the local DB
                            saveCallResult(
                                    (RequestObject) processResponse(
                                            (APIResponse.ApiSuccessResponse) requestObjectAPIResponse));
                            mAppExecutors.mainThread().execute(new Runnable() {
                                @Override
                                public void run() {
                                    results.addSource(loadFromDb(), new Observer<CacheObject>() {
                                        @Override
                                        public void onChanged(@Nullable CacheObject cacheObject) {
                                            setValue(Resource.success(cacheObject));
                                        }
                                    });
                                }
                            });
                        }
                    });
                } else if(requestObjectAPIResponse instanceof APIResponse.ApiEmptyResponse) {
                    mAppExecutors.mainThread().execute(new Runnable() {
                        @Override
                        public void run() {
                            results.addSource(loadFromDb(), new Observer<CacheObject>() {
                                @Override
                                public void onChanged(@Nullable CacheObject cacheObject) {
                                    setValue(Resource.success(cacheObject));
                                }
                            });
                        }
                    });
                } else if(requestObjectAPIResponse instanceof APIResponse.ApiErrorResponse) {
                    results.addSource(dbSource, new Observer<CacheObject>() {
                        @Override
                        public void onChanged(@Nullable CacheObject cacheObject) {
                            setValue(Resource.error(((APIResponse.ApiErrorResponse)
                                            requestObjectAPIResponse).getErrorMessage(), cacheObject));
                        }
                    });
                }
            }
        });
    }

    private CacheObject processResponse(APIResponse.ApiSuccessResponse response) {
        return (CacheObject) response.getBody();
    }

    private void setValue(Resource<CacheObject> newValue) {
        if(results.getValue() != newValue) {
            results.setValue(newValue);
        }
    }

    // Called to save the result of the API response into the database.
    @WorkerThread
    protected abstract void saveCallResult(@NonNull RequestObject item);

    // Called with the data in the database to decide whether to fetch
    // potentially updated data from the network.
    @MainThread
    protected abstract boolean shouldFetch(@Nullable CacheObject data);

    // Called to get the cached data from the database.
    @NonNull @MainThread
    protected abstract LiveData<CacheObject> loadFromDb();

    // Called to create the API call.
    @NonNull @MainThread
    protected abstract LiveData<APIResponse<RequestObject>> createCall();

    // Returns a LiveData object that represents the resource that's implemented
    // in the base class.
    public final LiveData<Resource<CacheObject>> getAsLiveData(){
        return results;
    };
}
