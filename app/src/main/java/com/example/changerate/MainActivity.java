package com.example.changerate;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;


import org.jetbrains.annotations.NotNull;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.lang.Runnable;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Hashtable;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity {
    private  static final String URL ="https://%s.fxexchangerate.com/rss.xml";
    private String uint;
    ArrayList<String> currencyUnits = new ArrayList<>();
    public OkHttpClient client;
    Hashtable<String,Double>changeRate = new Hashtable<>() ;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        // Khoi tao OkHttpClient de lay du lieu
        client= new OkHttpClient();

        //Tao request len server
        uint="aud";
        Request request = RequestUrl(String.format(URL,uint));

        //Thuc thi request de lay mang danh sach tien te
        client.newCall(request).enqueue(CallToGetArray());

        //Su kien khi chon item cua spiner
        Spinner spinnerFirst = (Spinner)findViewById(R.id.spinnerFirst);
        spinnerFirst.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                //Toast.makeText(MainActivity.this,currencyUnits.get(position),Toast.LENGTH_SHORT).show();
                uint=currencyUnits.get(position);
                Request newrequest = RequestUrl(String.format(URL,uint.toLowerCase()));
                client.newCall(newrequest).enqueue(CallbackToGetRate());

            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        //
        Button btn = (Button)findViewById(R.id.btnShow);
        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String t="";
                for(int i=0;i<currencyUnits.size();i++){
                    t+=String.format("1 %s = %f %s\n",uint.toUpperCase(),changeRate.get(currencyUnits.get(i)),currencyUnits.get(i));
                }
                Toast.makeText(MainActivity.this,t,Toast.LENGTH_LONG).show();
            }
        });

        //
        ImageButton btnConvert= (ImageButton)findViewById(R.id.btnConvert);
        btnConvert.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                TextView txtResult= (TextView)findViewById(R.id.txtResult);
                EditText value = (EditText)findViewById(R.id.value);
                Spinner second = (Spinner)findViewById(R.id.spinnerSecond);
                String uint= second.getSelectedItem().toString();
                String result=Double.toString(Double.parseDouble(value.getText().toString())*changeRate.get(uint));
                //Toast.makeText(MainActivity.this,result,Toast.LENGTH_LONG).show();
                //String temp = Double.toString(Double.parseDouble(value.getText().toString()) * changeRate.get((Spinner)findViewById(R.id.spinnerSecond)));
                txtResult.setText(result);
            }
        });


    }
    public Callback CallToGetArray(){
        Callback callback = new Callback() {
            public String xml;
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e("Error", "Network Error");
            }
            @Override
            public void onResponse(Call call, final Response response) throws IOException {
                //Lay thong tin xml tra ve
                xml = response.body().string();

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        getArrayCurrencies(xml);
                        Spinner spinnerfirst = (Spinner)findViewById(R.id.spinnerFirst);
                        Spinner spinnersecond = (Spinner)findViewById(R.id.spinnerSecond);
                        ArrayAdapter adapter = new ArrayAdapter(MainActivity.this,R.layout.item_spinner,currencyUnits);
                        spinnerfirst.setAdapter(adapter);
                        spinnersecond.setAdapter(adapter);
                        TextView txtResult= (TextView)findViewById(R.id.txtResult);
                        Spinner second = (Spinner)findViewById(R.id.spinnerSecond);
                        String uint= second.getSelectedItem().toString();
                        txtResult.setText(Double.toString(changeRate.get(uint)));
                        //init();
                    }
                });
            }
        };
        return callback;
    }

    public Callback CallbackToGetRate(){
        Callback callback = new Callback() {
            @Override
            public void onFailure(@NotNull Call call, @NotNull IOException e) {
                Log.e("Error", "Network Error");
            }

            @Override
            public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
                final String xml = response.body().string();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        getRateSelect(xml);
                    }
                });
            }
        };
        return  callback;
    }

    public Request RequestUrl(String url){
        Request request = new Request.Builder()
                .url(url)
                .build();
        return request;
    }
    public String getRateFromString(String s){

        String [] arrOfdesc=s.split("=");
        String [] rate = arrOfdesc[1].split(" ");
        return rate[1];
    }
    public void getRateSelect(String xml){
        XMLDOMParser parser = new XMLDOMParser();
        Document document = parser.getDocument(xml);
        NodeList nodes = document.getElementsByTagName("item");
        String desc;
        Hashtable<String,Double> tempHashTable = new Hashtable<>();
        for (int i=0 ;i< nodes.getLength();i++)
        {
            Element element = (Element) nodes.item(i);
            desc = parser.getValue(element,"description");
            tempHashTable.put(currencyUnits.get(i),Double.parseDouble(getRateFromString(desc)));
        }
        changeRate =  new Hashtable<>(tempHashTable);

    }
    public void getArrayCurrencies(String xml){
        XMLDOMParser parser = new XMLDOMParser();
        Document document = parser.getDocument(xml);
        NodeList nodes = document.getElementsByTagName("item");
        String desc,title;
        //ArrayList<String> description = new ArrayList<>();
        //Hashtable<String,Double> tempHashTable = new Hashtable<>();
        for (int i=0 ;i< nodes.getLength();i++)
        {
            Element element = (Element) nodes.item(i);
            title = parser.getValue(element,"title");
            String temp = title.substring(title.length()-4,title.length()-1);
            currencyUnits.add(temp);
            //desc = parser.getValue(element,"description");
            //tempHashTable.put(temp,Double.parseDouble(getRateFromString(desc)));
            //changeRate.put(temp,Double.parseDouble(getRateFromString(desc)));
            //changeRate.put()
            /*String h="1 AUD" + " = " + rate[1] + " " +temp +"\n";
            h+="1 AUD" + " = " + changeRate.get(temp) + " " +temp;
            Toast.makeText(getApplicationContext(),h,Toast.LENGTH_SHORT).show();*/
        }
        getRateSelect(xml);
        //changeRate = tempHashTable;
    }

}
