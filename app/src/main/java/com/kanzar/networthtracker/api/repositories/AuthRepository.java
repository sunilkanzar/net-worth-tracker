package com.kanzar.networthtracker.api.repositories;

import android.os.Build;
import android.util.Log;
import com.kanzar.networthtracker.api.RetrofitClient;
import com.kanzar.networthtracker.api.endpoints.AuthApi;
import com.kanzar.networthtracker.api.entities.AuthEntity;
import com.kanzar.networthtracker.api.entities.BaseResponse;
import retrofit2.Response;

import java.io.IOException;

public final class AuthRepository {
    private final AuthApi api = RetrofitClient.getRetrofit().create(AuthApi.class);

    public String auth(String googleToken) {
        try {
            String device = Build.MANUFACTURER + " " + Build.MODEL;
            String platform = "android";
            
            Response<BaseResponse<AuthEntity>> response = api.sendServerToken(googleToken, device, platform).execute();
            if (response.isSuccessful() && response.body() != null) {
                AuthEntity data = response.body().getData();
                if (data != null) {
                    return data.getToken();
                }
            }
        } catch (IOException e) {
            Log.e("AuthRepository", "Error during auth: " + e.getMessage(), e);
        }
        return null;
    }
}
