package com.kanzar.networthtracker.api.repositories;

import android.util.Log;
import com.kanzar.networthtracker.api.RetrofitClient;
import com.kanzar.networthtracker.api.endpoints.CommentApi;
import com.kanzar.networthtracker.api.entities.BaseResponse;
import com.kanzar.networthtracker.api.entities.CommentEntity;
import com.kanzar.networthtracker.helpers.Prefs;
import retrofit2.Response;

import java.io.IOException;

public final class CommentRepository {
    private final CommentApi api = RetrofitClient.getRetrofit().create(CommentApi.class);

    public String getComment(int month, int year) {
        try {
            String token = Prefs.getString("token", "");
            Response<BaseResponse<String>> response = api.getComment(token, month, year).execute();
            
            if (response.isSuccessful() && response.body() != null) {
                String comment = response.body().getData();
                return comment != null ? comment : "";
            }
        } catch (IOException e) {
            Log.e("CommentRepository", "Error getting comment: " + e.getMessage(), e);
        }
        return "";
    }

    public Object postComment(int month, int year, String commentText) {
        try {
            CommentEntity entity = new CommentEntity();
            entity.setComment(commentText);
            entity.setMonth(month);
            entity.setYear(year);

            String token = Prefs.getString("token", "");
            Response<Object> response = api.sendComment(token, entity).execute();
            
            if (response.isSuccessful()) {
                return response.body();
            }
        } catch (IOException e) {
            Log.e("CommentRepository", "Error posting comment: " + e.getMessage(), e);
        }
        return null;
    }
}
