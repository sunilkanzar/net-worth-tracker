package com.kanzar.networthtracker.api.entities;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import com.kanzar.networthtracker.models.AssetFields;

public final class CommentEntity {

    @SerializedName("comment")
    @Expose
    private String comment;

    @SerializedName(AssetFields.MONTH)
    @Expose
    private Integer month;

    @SerializedName(AssetFields.YEAR)
    @Expose
    private Integer year;

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public Integer getMonth() {
        return month;
    }

    public void setMonth(Integer month) {
        this.month = month;
    }

    public Integer getYear() {
        return year;
    }

    public void setYear(Integer year) {
        this.year = year;
    }
}
