package com.example.video;


import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;


public class RetrofitUtils {
    public static VideoService getService(){
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("http://beiyou.bytedance.com/")
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        return retrofit.create(VideoService.class);
    }
}
