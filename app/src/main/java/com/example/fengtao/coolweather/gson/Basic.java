package com.example.fengtao.coolweather.gson;

import com.google.gson.annotations.SerializedName;

public class Basic {
    @SerializedName("city")
    public String cityName;
    @SerializedName("id")
    public String weather_id;
    public Update update;
    public class Update{
        @SerializedName("loc")
        public String updateTime;
    }
    
}
