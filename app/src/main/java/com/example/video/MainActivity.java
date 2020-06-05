package com.example.video;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends AppCompatActivity {

    public static final String TAG = "MainActivity";
    RecyclerView recyclerView;
    private ProgressDialog mProgressDialog;
    private VideoAdapter adapter;
    private ArrayList<Video> list = new ArrayList<Video>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mProgressDialog= new ProgressDialog(this);
        Toolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setTitle("RecyclerView");
        setSupportActionBar(toolbar);
        recyclerView = findViewById(R.id.recycleview);
        recyclerView.setLayoutManager(new GridLayoutManager(this,2));
        recyclerView.addItemDecoration(new GridItemDecoration(2, dp2px(this, 8), false));

        adapter = new VideoAdapter(this,list);
        recyclerView.setAdapter(adapter);
        getVideos();
        adapter.setOnItemClickListener(new VideoAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(int position) {
                Video item = list.get(position);
                Intent intent = new Intent(MainActivity.this,VideoDetailActivity.class);
                intent.putExtra("url",item.getFeedurl());
                startActivity(intent);
            }
        });
    }
    public static float getScreenDensity(Context context) {
        return context.getResources().getDisplayMetrics().density;
    }
    /**
     * 把密度转换为像素
     */
    public static int dp2px(Context context, float dipValue) {
        final float scale = getScreenDensity(context);
        return (int) (dipValue * scale + 0.5);
    }
    private void getVideos(){
        mProgressDialog.show();
        VideoService videoService =  RetrofitUtils.getService();
        Call<List<Video>> callJoke = videoService.getVideo();
        callJoke.enqueue(new Callback<List<Video>>() {
            @Override
            public void onResponse(Call<List<Video>> call, Response<List<Video>> response) {
                mProgressDialog.dismiss();
                if (response.isSuccessful()){
                    List<Video> datas = response.body();
                    list.clear();
                    list.addAll(datas);
                    adapter.notifyDataSetChanged();
                    Log.d(TAG,"onResponse  Success "+datas.toString());
                }else{
                    Log.d(TAG,"onResponse error "+response.errorBody());
                }
            }

            @Override
            public void onFailure(Call<List<Video>> call, Throwable t) {
                mProgressDialog.dismiss();
                Log.d(TAG,"onFailure "+t.getLocalizedMessage());
            }
        });
    }


}
