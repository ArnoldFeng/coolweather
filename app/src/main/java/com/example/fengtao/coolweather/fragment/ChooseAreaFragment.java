/*
城市数据接口地址为
http://guolin.tech/api/china
 */
package com.example.fengtao.coolweather.fragment;

import android.app.ProgressDialog;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.example.fengtao.coolweather.R;
import com.example.fengtao.coolweather.db.City;
import com.example.fengtao.coolweather.db.County;
import com.example.fengtao.coolweather.db.Province;
import com.example.fengtao.coolweather.util.HttpUtil;
import com.example.fengtao.coolweather.util.Utility;

import org.litepal.LitePal;
import org.litepal.crud.LitePalSupport;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

public class ChooseAreaFragment extends Fragment {
    private static final int LEVEL_PROVINCE = 0;
    private static final int LEVEL_CITY = 1;
    private static final int LEVEL_COUNTY=2;
    private TextView titleText;
    private Button backButton;
    private ListView listView;
    private ArrayAdapter<String> adapter;
    private List<String> dataList = new ArrayList<>();
    private int currentLevel;
    private List<Province> provinceList;
    private List<City> cityList;
    private List<County> countyList;
    private Province selectedProvince;
    private City selectedCity;
    private County selectedCounty;
    private ProgressBar progressBar;
    private ProgressDialog progressDialog;
    
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup viewGroup,Bundle savedInstanceStated){
        View view = inflater.inflate(R.layout.choose_area,viewGroup,false);
        titleText = (TextView)view.findViewById(R.id.title_text);
        backButton = (Button)view.findViewById(R.id.back_button);
        listView = (ListView)view.findViewById(R.id.list_view);
        adapter = new ArrayAdapter<>(getContext(),android.R.layout.simple_list_item_1,dataList);
        listView.setAdapter(adapter);
        return view;
    }
    
    @Override
    public void onActivityCreated(Bundle savedInstanceStated){
        super.onActivityCreated(savedInstanceStated);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener(){
            @Override
            public void onItemClick(AdapterView<?> parent,View view,int position,long id){
                if(currentLevel == LEVEL_PROVINCE){
                    selectedProvince = provinceList.get(position);
                    Log.d("selectedProvince.id",""+selectedProvince.getId());
                    queryCities();
                }else if(currentLevel == LEVEL_CITY){
                    selectedCity = cityList.get(position);
                    Log.d("selectedCity.id",""+selectedCity.getId());
                    queryCounties();
                }
            }
        });
        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (currentLevel == LEVEL_COUNTY){
                    queryCities();
                }else if(currentLevel == LEVEL_CITY){
                    queryProvinces();
                }
            }
        });
        queryProvinces();
    }
    
    //查询省的信息，优先从数据库中查，如果数据库中没有数据则向服务器请求数据，请求的结果存入数据库中后再通过查询数据库得数据结果
    private void queryProvinces() {
        titleText.setText("中国");
        //设后退按钮为不可见
        backButton.setVisibility(View.GONE);
        //Litepal的查询数据库的方法，结果放入provinceList中，它是一个泛型实例，List<Province>
        provinceList = LitePal.findAll(Province.class);
        if(provinceList.size() > 0 ){
            dataList.clear();
            for (Province province : provinceList){
                dataList.add(province.getProvinceName());
            }
            adapter.notifyDataSetChanged();
            //默认选中第一个数据
            listView.setSelection(0);
            //设置当前页的级别为province，其实就是标识当前页，为后续操作做准备
            currentLevel = LEVEL_PROVINCE;
        }else{
            String address="http://guolin.tech/api/china";
            queryFromServer(address,"province");
        }
    }
    
    //查询市的数据，优先向数据库中去查，没有则向服务器请求
    private void queryCities() {
        titleText.setText(selectedProvince.getProvinceName());
        backButton.setVisibility(View.VISIBLE);
        cityList = LitePal.where("provinceId = ?",String.valueOf(selectedProvince.getId())).find(City.class);
        if(cityList.size() > 0){
            dataList.clear();
            for(City city:cityList){
                dataList.add(city.getCityName());
            }
            adapter.notifyDataSetChanged();
            listView.setSelection(0);
            currentLevel = LEVEL_CITY;
        }else {
            int provinceCode = selectedProvince.getProvinceCode();
            String address = "http://guolin.tech/api/china/" + provinceCode;
            queryFromServer(address,"city");
        }
    }
    
    //查询县的数据，优先查数据库，没有再向服务器请求数据
    private void queryCounties() {
        titleText.setText(selectedCity.getCityName());
        backButton.setVisibility(View.VISIBLE);
        countyList = LitePal.where("cityid like ?",String.valueOf(selectedCity.getId())).find(County.class);
        if(countyList.size()>0){
            dataList.clear();
            for(County county:countyList){
                dataList.add(county.getCountyName());
            }
            adapter.notifyDataSetChanged();
            listView.setSelection(0);
            currentLevel = LEVEL_COUNTY;  
        }else{
            int provinceCode = selectedProvince.getProvinceCode();
            int cityCode = selectedCity.getCityCode();
            String address = "http://guolin.tech/api/china/" + provinceCode +"/"+cityCode;
            queryFromServer(address,"county");
        }

        
    }
    
    
    private void queryFromServer(String address, final String type) {
        //显示一个进度条
        showProgressDialog();
        //HttpUtil类中封装了请求接口数据的方法，在sendOkHttpRequest中传入一个回调函数，在回调函数中处理服务器返回的结果
        HttpUtil.sendOkHttpRequest(address, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        closeProgressDialog();
                        Toast.makeText(getContext(),"数据加载失败",Toast.LENGTH_SHORT).show();
                    }
                });
            }
            
            @Override
            public void onResponse(Call call, Response response) throws IOException {
                //获取返回内容，这里返回的结果是json字符串
                String responseText = response.body().string();
                boolean result = false;
                if("province".equals(type)){
                    //Utility的handleProvinceResponse负责处理返回的省级数据
                    result = Utility.handleProvinceResponse(responseText);
                }else if("city".equals(type)){
                    result = Utility.handleCityResponse(responseText,selectedProvince.getId());
                }else if("county".equals(type)){
                    result = Utility.handleCountyResponse(responseText,selectedCity.getId());
                }
                //根据获得结果更新界面的数据，此处由于涉及更新界面所以在runOnUiThread中进行
                if(result){
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            closeProgressDialog();
                            if("province".equals(type)){
                                queryProvinces();
                            }else if("city".equals(type)){
                                queryCities();
                            }else if("county".equals(type)){
                                queryCounties();
                            }
                        }
                    });
                }
            }
        });

    }

    private void closeProgressDialog() {
        if(progressDialog != null){
            progressDialog.dismiss();
        }
    }
    

    private void showProgressDialog() {
        if(progressDialog == null){
            progressDialog = new ProgressDialog(getActivity());
            progressDialog.setMessage("正在加载...");
            progressDialog.setCanceledOnTouchOutside(false);
        }
        progressDialog.show();
    }

}
