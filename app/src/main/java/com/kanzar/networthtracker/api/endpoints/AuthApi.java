package com.kanzar.networthtracker.api.endpoints;

import com.kanzar.networthtracker.api.entities.AuthEntity;
import com.kanzar.networthtracker.api.entities.BaseResponse;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

public interface AuthApi {
    @GET("auth")
    Call<BaseResponse<AuthEntity>> sendServerToken(@Query("google") String token, @Query("device") String device, @Query("platform") String platform);
}
