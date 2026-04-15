package com.kanzar.networthtracker.api.endpoints;

import com.kanzar.networthtracker.api.entities.AssetEntity;
import com.kanzar.networthtracker.api.entities.BaseResponse;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Query;

import java.util.List;

public interface AssetApi {
    @GET("assets")
    Call<BaseResponse<List<AssetEntity>>> getAll(@Query("token") String token, @Query("updatedAt") long updatedAt);

    @POST("assets")
    Call<Object> sendAll(@Query("token") String token, @Body RequestBody assets);
}
