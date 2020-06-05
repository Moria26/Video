package com.example.video;


import java.util.List;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

public interface VideoService {
    //发送GET请求，将返回的数据转换成Video的数组
    @GET("api/invoke/video/invoke/video")
    Call<List<Video>> getVideo();
}
