package com.zhtian.experimentten;

import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Location;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.ToggleButton;

import com.baidu.mapapi.map.BaiduMap;
import com.baidu.mapapi.map.BitmapDescriptor;
import com.baidu.mapapi.map.BitmapDescriptorFactory;
import com.baidu.mapapi.map.MapStatus;
import com.baidu.mapapi.map.MapStatusUpdate;
import com.baidu.mapapi.map.MapStatusUpdateFactory;
import com.baidu.mapapi.map.MyLocationConfiguration;
import com.baidu.mapapi.map.MyLocationData;
import com.baidu.mapapi.map.TextureMapView;
import com.baidu.mapapi.model.LatLng;
import com.baidu.mapapi.utils.CoordinateConverter;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private TextureMapView mBMapView;
    private SensorUtils mSensorUtils;
    private ToggleButton toggleButton;
    private CoordinateConverter mConverter;
    private String mCity;
    private Location mLocation;
    private List<HotelInfomation> list;
    private final int UPDATE_UI = 1;
    private View sub_view;
    private TextView name1;
    private TextView name2;
    private TextView address1;
    private TextView address2;
    private TextView tel1;
    private TextView tel2;
    private ImageView line;
    private LinearLayout xia;
    private int flag = 0;

    private Handler handler = new Handler() {
        public void handleMessage(Message msg) {
            List<HotelInfomation> temp = (List<HotelInfomation>) msg.obj;
            switch (msg.what) {
                case UPDATE_UI:
                    if (temp == null || temp.size() == 0) {
                        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                        builder.setTitle(mCity + " 附近的酒店");
                        builder.setMessage("1公里以内找不到酒店。");
                        builder.setPositiveButton("确定", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                flag = 0;
                            }
                        });
                        builder.create().show();
                        return;
                    } else if (temp.size() == 1) {
                        LayoutInflater layoutInflater = LayoutInflater.from(MainActivity.this);
                        sub_view = layoutInflater.inflate(R.layout.sub_view, null);
                        name1 = (TextView) sub_view.findViewById(R.id.name1);
                        address1 = (TextView) sub_view.findViewById(R.id.address1);
                        tel1 = (TextView) sub_view.findViewById(R.id.tel1);

                        name1.setText(temp.get(0).getName());
                        address1.setText(temp.get(0).getAddress());
                        tel1.setText(temp.get(0).getTel());
                        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                        builder.setTitle(mCity + " 附近的酒店");
                        builder.setView(sub_view);
                        builder.setPositiveButton("确定", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                flag = 0;
                            }
                        });
                        builder.create().show();

                        list = null;
                    } else {
                        LayoutInflater layoutInflater = LayoutInflater.from(MainActivity.this);
                        sub_view = layoutInflater.inflate(R.layout.sub_view, null);
                        name1 = (TextView) sub_view.findViewById(R.id.name1);
                        address1 = (TextView) sub_view.findViewById(R.id.address1);
                        tel1 = (TextView) sub_view.findViewById(R.id.tel1);
                        name2 = (TextView) sub_view.findViewById(R.id.name2);
                        address2 = (TextView) sub_view.findViewById(R.id.address2);
                        tel2 = (TextView) sub_view.findViewById(R.id.tel2);
                        line = (ImageView) sub_view.findViewById(R.id.line2);
                        xia = (LinearLayout) sub_view.findViewById(R.id.xia);

                        name1.setText(temp.get(0).getName());
                        address1.setText(temp.get(0).getAddress());
                        tel1.setText(temp.get(0).getTel());
                        name2.setText(temp.get(1).getName());
                        address2.setText(temp.get(1).getAddress());
                        tel2.setText(temp.get(1).getTel());
                        line.setVisibility(View.VISIBLE);
                        xia.setVisibility(View.VISIBLE);
                        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                        builder.setTitle(mCity + " 附近的酒店");
                        builder.setView(sub_view);
                        builder.setPositiveButton("确定", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                flag = 0;
                            }
                        });
                        builder.create().show();

                        list = null;
                    }
                    break;
                default:
                    break;
            }
        }
    };

    // 同步处理location更新，判断更新位置还是摇一摇功能
    private SensorUtils.MyListener myListener = new SensorUtils.MyListener() {
        @Override
        public void updateInTime(Location location, float rotation, int type) {
            switch (type) {
                case 1:
                    if (location != null)
                        updateMap(location, rotation);
                    break;
                case 2:
                    mLocation = location;
                    flag++;
                    if (flag == 1)
                        getInfo();
                        int i = 1;
                    break;
                default:
                    break;
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mBMapView = (TextureMapView) findViewById(R.id.bmapview);
        mSensorUtils = SensorUtils.getSensorUtilsInstance();
        toggleButton = (ToggleButton) findViewById(R.id.toggle_button);
        init(mSensorUtils.getCurrentLocation());

        toggleButton.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked && mSensorUtils.getCurrentLocation() != null) {
                    updateMap(mSensorUtils.getCurrentLocation(), mSensorUtils.getCurrentRotation());
                }
            }
        });

        // change togglebutton on touch the screen
        mBMapView.getMap().setOnMapTouchListener(new BaiduMap.OnMapTouchListener() {
            @Override
            public void onTouch(MotionEvent motionEvent) {
                switch (motionEvent.getAction()) {
                    case MotionEvent.ACTION_MOVE:
                        toggleButton.setChecked(false);
                        break;
                    default:
                        break;
                }
            }
        });

    }

    @Override
    protected void onDestroy() {
        //在activity执行onDestroy时执行mMapView.onDestroy()，实现地图生命周期管理
        mBMapView.onDestroy();
        super.onDestroy();

    }

    @Override
    protected void onResume() {
        //在activity执行onResume时执行mMapView. onResume ()，实现地图生命周期管理
        mBMapView.onResume();
        mSensorUtils.registerSensors();
        if (mSensorUtils.getCurrentLocation() != null) {
            updateMap(mSensorUtils.getCurrentLocation(), 0);
        }
        mSensorUtils.setMyListener(myListener);
        super.onResume();
    }

    @Override
    protected void onPause() {
        //在activity执行onPause时执行mMapView. onPause ()，实现地图生命周期管理
        mBMapView.onPause();
        mSensorUtils.unregisterSensors();
        mSensorUtils.removeMyListener(myListener);
        super.onPause();
    }

    private void init(Location location) {
        Bitmap bitmap = Bitmap.createScaledBitmap(BitmapFactory.decodeResource(getResources(), R.mipmap.pointer), 100, 100, true);
        BitmapDescriptor bitmapDescriptor = BitmapDescriptorFactory.fromBitmap(bitmap);
        mBMapView.getMap().setMyLocationEnabled(true);
        MyLocationConfiguration configuration = new MyLocationConfiguration(MyLocationConfiguration.LocationMode.NORMAL, true, bitmapDescriptor);
        mBMapView.getMap().setMyLocationConfigeration(configuration);
        mBMapView.getMap().setMapStatus(MapStatusUpdateFactory.zoomTo(18));
        if (location != null)
            updateMap(location, mSensorUtils.getCurrentRotation());
    }

    //  将GPS设备采集的原始GPS坐标转换成百度坐标
    private LatLng convert2baiduLL(Location location) {
        mConverter = new CoordinateConverter();
        mConverter.from(CoordinateConverter.CoordType.GPS);
        mConverter.coord(new LatLng(location.getLatitude(), location.getLongitude()));
        return mConverter.convert();
    }

    private void updateMap(Location location, float rotation) {
        LatLng temp = convert2baiduLL(location);
        MyLocationData.Builder data = new MyLocationData.Builder();
        data.latitude(temp.latitude);
        data.longitude(temp.longitude);
        data.direction(rotation);
        mBMapView.getMap().setMyLocationData(data.build());
        if (toggleButton.isChecked()) {
            MapStatus mMapStatus = new MapStatus.Builder().target(temp).build();
            MapStatusUpdate mMapStatusUpdate = MapStatusUpdateFactory.newMapStatus(mMapStatus);
            mBMapView.getMap().setMapStatus(mMapStatusUpdate);
        }
    }

    private void getInfo() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                getCity(mLocation);
                getData(mLocation);
                Message message = new Message();
                message.what = UPDATE_UI;
                message.obj = list;
                handler.sendMessage(message);
            }
        }).start();
    }

    private void getCity(Location location) {
        String url = "http://api.map.baidu.com/geocoder/v2/?location=" + location.getLatitude() + ","
                + location.getLongitude() + "&output=json&ak=MDCiRmhXdY9xiopzGReWPejTGunNlv75" +
                "&mcode=E8:A6:CE:2E:B7:E6:E1:0F:09:87:C4:FD:13:92:B2:01:36:40:C8:20;com.zhtian.experimentten&pois=0";
        InputStreamReader isr;
        String result = "";
        try {
            //获取url的输入流
            isr = new InputStreamReader(new URL(url).openStream(), "utf-8");
            BufferedReader br = new BufferedReader(isr);
            String line = "";
            //将输入流中的数据保存到字符串result中
            while ((line = br.readLine()) != null) {
                result += line;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            JSONObject jsonObject = new JSONObject(result);
            mCity = jsonObject.getJSONObject("result")
                    .getJSONObject("addressComponent").getString("city");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void getData(Location location) {
        //拼接url地址
        String url = "http://api.map.baidu.com/telematics/v3/local?location=" + location.getLongitude() + ","
                + location.getLatitude() + "&keyWord=酒店&output=json&ak=MDCiRmhXdY9xiopzGReWPejTGunNlv75" +
                "&mcode=E8:A6:CE:2E:B7:E6:E1:0F:09:87:C4:FD:13:92:B2:01:36:40:C8:20;com.zhtian.experimentten&radius=1000";

        InputStreamReader isr;
        String result = "";
        try {
            //获取url的输入流
            isr = new InputStreamReader(new URL(url).openStream(), "utf-8");
            BufferedReader br = new BufferedReader(isr);
            String line = "";
            //将输入流中的数据保存到字符串result中
            while ((line = br.readLine()) != null) {
                result += line;
            }


        } catch (Exception e) {
            e.printStackTrace();
        }
        list = new ArrayList<>();
        //json字符串处理
        try {
            JSONObject jsonObject = new JSONObject(result);
            JSONArray pointList = jsonObject.getJSONArray("pointList");
            if (pointList == null) {
                return;
            }
            JSONObject info = pointList.getJSONObject(0);
            if (info == null) {
                return;
            }
            JSONObject detial = info.getJSONObject("additionalInformation");
            list.add(new HotelInfomation(detial.getString("name"), detial.getString("address"), detial.getString("telephone")));
            info = pointList.getJSONObject(1);
            if (info == null) {
                return;
            }
            detial = info.getJSONObject("additionalInformation");
            list.add(new HotelInfomation(detial.getString("name"), detial.getString("address"), detial.getString("telephone")));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
