package com.example.fengtao.coolweather.service;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.IBinder;
import android.os.SystemClock;
import android.preference.PreferenceManager;

import com.example.fengtao.coolweather.gson.Weather;
import com.example.fengtao.coolweather.util.HttpUtil;
import com.example.fengtao.coolweather.util.Utility;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

public class AutoUpdateService extends Service {
    public AutoUpdateService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }
    
    @Override
    public int onStartCommand(Intent intent,int flags,int startId){
        updateWeather();
        updateBingPic();
        /*
            AlarmManager是Android中的一种系统级别的提示服务，可在设定的时间下执行一个intent，通常是PendingIntent，这个intent可以是启动服务，发送广播，跳转Activity等
            AlarmManager具有在手机休眠的情况下唤醒cpu的能力，
        * */
        AlarmManager alarmManager = (AlarmManager)getSystemService(ALARM_SERVICE);
        int anHour = 8*60*60*1000;//1小时的毫秒数
        long triggerAtTime = SystemClock.elapsedRealtime()+anHour;//elapsedRealtimes：返回系统启动到现在的实现，包含设备深度休眠的时间
        Intent intent1 = new Intent(this,AutoUpdateService.class); 
        PendingIntent pendingIntent = PendingIntent.getService(this,0,intent1,0);
        alarmManager.cancel(pendingIntent);
        /* AlarmManager.ELAPSED_REALTIME_WAKEUP：闹钟在系统睡眠状态下会唤醒系统并执行提示功能，该状态下闹钟使用相对时间（相对于系统启动时间）
          第二个参数表示闹钟执行的时间
          第三个参数表示闹钟响应的动作
          */
        alarmManager.set(AlarmManager.ELAPSED_REALTIME_WAKEUP,triggerAtTime,pendingIntent);
        return super.onStartCommand(intent,flags,startId);
    }
    /*
    * 更新天气信息
    * */
    private void updateWeather(){
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        String weatherStr = prefs.getString("weather",null);
        if(weatherStr != null){
            Weather weather = Utility.handleWeatherResponse(weatherStr);
            String weatherId = weather.basic.weather_id;
            String weatherUrl = "http://guolin.tech/api/weather?cityid="+weatherId+"&key=bc0418b57b2d4918819d3974ac1285d9";
            HttpUtil.sendOkHttpRequest(weatherUrl, new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    e.printStackTrace();
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    String responseText = response.body().string();
                    Weather weather = Utility.handleWeatherResponse(responseText);
                    if(weather != null && "ok".equals(weather.status)){
                        SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(AutoUpdateService.this).edit();
                        editor.putString("weather",responseText);
                        editor.apply();
                    }
                }
            });
        }
    }
    /*
    * 更新图片
    * */
    private void updateBingPic(){
        String requestBingPic = "http://guolin.tech/api/bing_pic";
        HttpUtil.sendOkHttpRequest(requestBingPic, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                e.printStackTrace();
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String bingPic = response.body().string();
                SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(AutoUpdateService.this).edit();
                editor.putString("bing_pic",bingPic);
                editor.apply();
            }
        });
    }
}
