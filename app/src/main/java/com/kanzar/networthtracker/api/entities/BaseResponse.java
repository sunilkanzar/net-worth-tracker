package com.kanzar.networthtracker.api.entities;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public final class BaseResponse<T> {

    @SerializedName("data")
    @Expose
    private T data;

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }
}
