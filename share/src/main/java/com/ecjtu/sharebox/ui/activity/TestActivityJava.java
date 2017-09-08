package com.ecjtu.sharebox.ui.activity;

import android.content.Intent;
import android.os.Parcel;
import android.os.SystemClock;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import com.dd.CircularProgressButton;
import com.ecjtu.sharebox.R;
import com.ecjtu.sharebox.ui.fragment.WebViewFragment;
import com.ecjtu.sharebox.util.cache.FileCacheHelper;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TestActivityJava extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_test_java);


//        Map<String,List<String>> map=new LinkedHashMap<>();
//        List<String> lst=new ArrayList<>();
//        lst.add("/sdcard/test.mp4");
//        lst.add("/sdcard/test2.mp4");
//        map.put("Movie",lst);
//
//        List<String> lst2=new ArrayList<>();
//        lst2.add("/sdcard/test3.mp4");
//        lst2.add("/sdcard/test4.mp4");
//        map.put("Music",lst2);
//        DeviceInfo info=new DeviceInfo("123","192.168.43.1",8080,"/icon",map);
//        JSONObject root=Info.deviceInfo2Json(info);
//        info=Info.json2DeviceInfo(root);

        final CircularProgressButton circularButton1 = (CircularProgressButton) findViewById(R.id.circularButton1);
        circularButton1.setIndeterminateProgressMode(true);
        circularButton1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (circularButton1.getProgress() == 0) {
                    circularButton1.setProgress(50);
                } else if (circularButton1.getProgress() == 100) {
                    circularButton1.setProgress(0);
                } else {
                    circularButton1.setProgress(100);
                }
            }
        });

//        Intent intent=ImmersiveFragmentActivity.newInstance(this, WebViewFragment.class);
//        startActivity(intent);

        final Map<String, List<String>> map = new HashMap<>();

        for (int i = 0; i < 10000; i++) {
            List<String> childList = new ArrayList<>();
            for (int j = 0; j < 100; j++) {
                childList.add(new String("Child Say Hello" + j));
            }
            map.put("HelloWorld" + i, childList);
        }


        new Thread(new Runnable() {
            @Override
            public void run() {
                long startTime = System.currentTimeMillis();
                FileCacheHelper helper = new FileCacheHelper("/sdcard");
                helper.persistObject("key", map);

//                    Log.e("tttttttttt", "persistObject time " + (System.currentTimeMillis() - startTime));
                startTime = System.currentTimeMillis();
                Map<String,List<String>> childMap= helper.readObject("key");
                Log.e("tttttttttt", "persistObject time " + (System.currentTimeMillis() - startTime));


//                try {
//                    saveParcel(map,new FileOutputStream("/sdcard/cache.par"));
////                    Log.e("tttttttttt", "persistObject time " + (System.currentTimeMillis() - startTime));
//                    startTime = System.currentTimeMillis();
////                    Map<String,List<String>> childMap= (Map<String, List<String>>) helper.readObject("key");
//                    readParcel(new FileInputStream("/sdcard/cache.par"));
//                    Log.e("tttttttttt", "persistObject time " + (System.currentTimeMillis() - startTime));
//                } catch (IOException e) {
//                    e.printStackTrace();
//                }
            }
        }).start();
    }

    public void saveParcel(Map<String,List<String>> map, FileOutputStream fos) throws IOException {
        Parcel parcel=Parcel.obtain();
        parcel.writeMap(map);
        fos.write(parcel.marshall());
        parcel.recycle();
        map.clear();
    }

    public void readParcel(FileInputStream fis) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        int len;
        byte[] buf = new byte[1024 * 1024];
        while ((len = fis.read(buf)) > 0) {
            bos.write(buf, 0, len);
        }
        Parcel parcel = Parcel.obtain();
        buf = bos.toByteArray();
        parcel.unmarshall(buf, 0, buf.length);
        parcel.setDataPosition(0);

        //read parcel
        Map<String,List<String>> map=new HashMap<>();
        parcel.readMap(map,HashMap.class.getClassLoader());


        parcel.recycle();
    }
}