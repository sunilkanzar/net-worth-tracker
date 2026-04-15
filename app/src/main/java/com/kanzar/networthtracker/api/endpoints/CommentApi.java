package com.kanzar.networthtracker.api.endpoints;

import com.kanzar.networthtracker.api.entities.BaseResponse;
import com.kanzar.networthtracker.api.entities.CommentEntity;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Query;

public interface CommentApi {
    @GET("comment")
    Call<BaseResponse<String>> getComment(@Query("token") String token, @Query("month") int month, @Query("year") int year);

    @POST("comment")
    Call<Object> sendComment(@Query("token") String token, @Body CommentEntity commentEntity);
}
