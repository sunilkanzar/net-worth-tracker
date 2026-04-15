package com.kanzar.networthtracker.api.repositories;

import android.util.Log;
import com.google.gson.Gson;
import com.kanzar.networthtracker.api.RetrofitClient;
import com.kanzar.networthtracker.api.endpoints.AssetApi;
import com.kanzar.networthtracker.api.entities.AssetEntity;
import com.kanzar.networthtracker.api.entities.BaseResponse;
import com.kanzar.networthtracker.helpers.Prefs;
import com.kanzar.networthtracker.models.Asset;
import okhttp3.MediaType;
import okhttp3.RequestBody;
import retrofit2.Response;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class AssetRepository {
    private final AssetApi api = RetrofitClient.getRetrofit().create(AssetApi.class);

    public List<Asset> getAll(long updatedAt) {
        try {
            String token = Prefs.getString("token", "");
            Response<BaseResponse<List<AssetEntity>>> response = api.getAll(token, updatedAt).execute();
            
            if (response.isSuccessful() && response.body() != null) {
                List<AssetEntity> data = response.body().getData();
                if (data != null) {
                    List<Asset> assets = new ArrayList<>(data.size());
                    for (AssetEntity entity : data) {
                        assets.add(entity.toAsset());
                    }
                    return assets;
                }
            }
        } catch (IOException e) {
            Log.e("AssetRepository", "Error getting assets: " + e.getMessage(), e);
        }
        return Collections.emptyList();
    }

    public Object sendAll(List<Asset> assets) {
        if (assets == null || assets.isEmpty()) {
            return null;
        }
        try {
            String json = new Gson().toJson(assets);
            RequestBody requestBody = RequestBody.create(MediaType.parse("application/json"), json);
            String token = Prefs.getString("token", "");
            
            Response<Object> response = api.sendAll(token, requestBody).execute();
            if (response.isSuccessful()) {
                return response.body();
            }
        } catch (IOException e) {
            Log.e("AssetRepository", "Error sending assets: " + e.getMessage(), e);
        }
        return null;
    }
}
