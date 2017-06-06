package com.example.baidu;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.baidu.lbsapi.auth.e;
import com.baidu.location.BDLocation;
import com.baidu.location.BDLocationListener;
import com.baidu.location.LocationClient;
import com.baidu.location.LocationClientOption;
import com.baidu.mapapi.map.BaiduMap;
import com.baidu.mapapi.map.BitmapDescriptor;
import com.baidu.mapapi.map.BitmapDescriptorFactory;
import com.baidu.mapapi.map.MapStatus;
import com.baidu.mapapi.map.MapStatusUpdateFactory;
import com.baidu.mapapi.map.MapView;
import com.baidu.mapapi.map.MarkerOptions;
import com.baidu.mapapi.map.MyLocationConfiguration;
import com.baidu.mapapi.map.MyLocationConfiguration.LocationMode;
import com.baidu.mapapi.map.MyLocationData;
import com.baidu.mapapi.map.OverlayOptions;
import com.baidu.mapapi.model.LatLng;
import com.example.entity.Bean;
import com.example.entity.Position;
import com.example.lib.HttpApi;
import com.example.other.MapListAdapter;
import com.example.thread.initData;
import com.example.thread.sendMyPosition;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.Settings.Secure;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Button;
import android.widget.GridView;
import android.widget.LinearLayout;
import android.widget.TextView;

public class MainActivity extends Activity {  
//	private TextView tv_option;
	private MapView mMapView;
	private BaiduMap mBaiduMap;
	private List<Bean> mapBeans;
    Button requestLocButton;
    private GridView gridView;

	//定位相关
    BitmapDescriptor mCurrentMarker;
	private LocationClient mLocClient;
    private LocationMode mCurrentMode;
	private boolean isFirstLoc = true;//是否首次定位
	private MyLocationListenner myListener = new MyLocationListenner();

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		

//		tv_option = (TextView) findViewById(R.id.tv_option);
		gridView = (GridView) findViewById(R.id.grid_map_user_list);
		mMapView = (MapView) findViewById(R.id.bmapView);
		mBaiduMap = mMapView.getMap();
		//开启定位图层
		mBaiduMap.setMyLocationEnabled(true);
		//定位初始化
		mLocClient = new LocationClient(getApplicationContext());
		mLocClient.registerLocationListener(myListener);
		LocationClientOption option = new LocationClientOption();
		option.setOpenGps(true);// 打开GPS
		option.setCoorType("bd09ll");//设置坐标类型
		option.setScanSpan(9000);//设置扫描间隔，单位是毫秒 当<1000(1s)时，定时定位无效	
		option.setIsNeedAddress(true);//设置地址信息，默认无地址信息
		mLocClient.setLocOption(option);
		mLocClient.start();
		mBaiduMap.setMapStatus(MapStatusUpdateFactory.newMapStatus(new MapStatus.Builder().zoom(13).build()));
		
		init_click();//初始化各种点击事件
	}
	
	public void init_click(){
        requestLocButton = (Button) findViewById(R.id.button1);
        requestLocButton.setText("普通");
        mCurrentMode = LocationMode.NORMAL; //普通
        mCurrentMarker = null; // 自定义图标
        
        OnClickListener btnClickListener = new OnClickListener() {
            public void onClick(View v) {
                switch (mCurrentMode) {
                    case NORMAL:
                        requestLocButton.setText("跟随");
                        mCurrentMode = LocationMode.FOLLOWING;
                        mBaiduMap.setMyLocationConfiguration(new MyLocationConfiguration(mCurrentMode, true, mCurrentMarker));
                        MapStatus.Builder builder = new MapStatus.Builder();
                        builder.overlook(0);
                        mBaiduMap.animateMapStatus(MapStatusUpdateFactory.newMapStatus(builder.build()));
                        break;
                    case FOLLOWING:
                        requestLocButton.setText("普通");
                        mCurrentMode = LocationMode.NORMAL;
                        mBaiduMap
                                .setMyLocationConfiguration(new MyLocationConfiguration(
                                        mCurrentMode, true, mCurrentMarker));
                        break;
                    default:
                        break;
                }
            }
        };
        requestLocButton.setOnClickListener(btnClickListener);
	
        
        OnItemClickListener gridItemClickListener = new AdapterView.OnItemClickListener() {
        	@Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                Position posi=(Position)gridView.getItemAtPosition(position);
                
                //设置中中心点
                LatLng ll = new LatLng(posi.getPosition_l(),posi.getPosition_r());
                MapStatus.Builder builder = new MapStatus.Builder();
                builder.target(ll).zoom(15);
                mBaiduMap.animateMapStatus(MapStatusUpdateFactory.newMapStatus(builder.build())); 
                
                HttpApi.MyLog(posi.getName()+" 点击了定位");
                
            }
        };
		gridView.setOnItemClickListener(gridItemClickListener);
        
	}
	
	/*
	 * 获取到数据源更新地图make
	 * */
	Handler handler = new Handler() {  
	    @Override  
	    public void handleMessage(Message msg) {  
	        super.handleMessage(msg);  
	        
			List<Position> json_list = (List<Position>) msg.obj;
	    	initData(json_list); //获取到数据 
	    }  
	};  
	
	/*
	 * 添加marker
	 * */
	BitmapDescriptor bdGround = BitmapDescriptorFactory.fromResource(R.drawable.ground_overlay);
	private void initData(List<Position>  json_list) {
		mBaiduMap.clear(); 
		//初始化gridView 及lsit
		List<Map<String,String>> list = new ArrayList<Map<String,String>>();
		
		//添加地图marker
		for (Position position : json_list) {
			
			//设置地图上显示的名字view
			LatLng latLng = new LatLng(position.getPosition_l(), position.getPosition_r());			
			View view = View.inflate(getApplicationContext(), R.layout.item_bean, null);
			TextView tView = (TextView)view.findViewById(R.id.item_bean);
			tView.setText(position.getName() + "");

			//将View转化为Bitmap
			BitmapDescriptor descriptor = BitmapDescriptorFactory.fromView(view);
			OverlayOptions options = new MarkerOptions().position(latLng).icon(descriptor).draggable(true);
			mBaiduMap.addOverlay(options);
			
			//设置头部展示哪些人参与了
			Map<String,String> listItem = new HashMap<String,String>();
			listItem.put("name", position.getName());
			list.add(listItem);
			
			HttpApi.MyLog(position.getName()+","+position.getPosition_l()+","+position.getPosition_r());
		}
		
		//设置哪些人加入定位
		Integer columns = json_list.size();
		LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(200*columns, LinearLayout.LayoutParams.FILL_PARENT);
		gridView.setLayoutParams(params); // 设置GirdView布局参数,横向布局的关键
        gridView.setColumnWidth(200); // 设置列表项宽
        gridView.setHorizontalSpacing(5); // 设置列表项水平间距
        gridView.setStretchMode(GridView.NO_STRETCH);
        gridView.setNumColumns(columns); // 设置列数量=列表集合数
		gridView.setAdapter(new MapListAdapter(this, json_list, gridView));
	    
	}
	
    /*
     * 定位SDK监听函数
     */
	private class MyLocationListenner implements BDLocationListener{
		@Override
		public void onReceiveLocation(BDLocation location) {
			// TODO Auto-generated method stub
			// map view 销毁后不在处理新接收的位置
            if (location == null || mMapView == null) {
                return;
            }
            MyLocationData locData = new MyLocationData.Builder()
            .accuracy(location.getRadius())
                    // 此处设置开发者获取到的方向信息，顺时针0-360
            .direction(100).latitude(location.getLatitude())
            .longitude(location.getLongitude()).build();
            mBaiduMap.setMyLocationData(locData);
            if (isFirstLoc) {
                isFirstLoc = false;
                LatLng ll = new LatLng(location.getLatitude(),location.getLongitude());
//                tv_option.setText(ll +"详情地点：" + location.getAddrStr() + "   城市：" +location.getCity());
                MapStatus.Builder builder = new MapStatus.Builder();
                builder.target(ll).zoom(13.0f);
                mBaiduMap.animateMapStatus(MapStatusUpdateFactory.newMapStatus(builder.build())); 
                HttpApi.MyLog(location.getLatitude()+","+ location.getLongitude() +"详情地点：" + location.getAddrStr() + "   城市：" +location.getCity());    
            }
            
            //发送自己的定位
            String m_szAndroidID = Secure.getString(getContentResolver(), Secure.ANDROID_ID);
            Position position = new Position();
            position.setM_szAndroidID(m_szAndroidID);
            position.setPosition_r(location.getLongitude());
            position.setPosition_l(location.getLatitude());
            position.setAddress(location.getAddrStr());
            sendMyPosition sr1 = new sendMyPosition(position);  
            new Thread(sr1).start();
            
            //加载其他人定位
            initData thr1 = new initData(handler);
    	    new Thread(thr1).start();
    	    
    	    
    	    
    	    
    	    
		}
	}
	
	@Override
	protected void onResume() {
		// TODO Auto-generated method stub
		super.onResume();
		mMapView.onResume();
	}
	@Override
	protected void onPause() {
		// TODO Auto-generated method stub
		super.onPause();
		mMapView.onPause();
	}
	@Override
	protected void onDestroy() {
		// TODO Auto-generated method stub
		super.onDestroy();
		mMapView.onDestroy();
		mBaiduMap = null;
		bdGround.recycle();
	}
  
}

