package com.sensorsdata.manager.demo;

import android.content.ContentValues;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

import com.sensorsdata.manager.db.DBUtil;

import org.json.JSONArray;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        findViewById(R.id.btn_insert).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ContentValues contentValues = new ContentValues();
                contentValues.put("data", "测试埋点数据");
                contentValues.put("created_at", System.currentTimeMillis());
                getContentResolver().insert(Uri.parse("content://com.sensorsdata.manager.SensorsDataContentProvider/events"), contentValues);
            }
        });

        findViewById(R.id.btn_delete).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                JSONArray ids = new JSONArray();
                ids.put(1);
                ids.put(2);
                ids.put(3);
                ids.put(4);
                DBUtil.getInstance(MainActivity.this.getBaseContext()).cleanupEvents(Uri.parse("content://com.sensorsdata.manager.SensorsDataContentProvider/events"), ids);
            }
        });
    }
}