package com.sq.wolf;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.IBinder;
import android.support.constraint.solver.Cache;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.suke.widget.SwitchButton;

import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity {
    private final static String TAG = "MainActivity";
    private NavigationCustom nc_hw;
    private NavigationCustom nc_lock;
    private Button btn_lock;
    private Button btn_cun;
    private EditText nc_mima;
    private EditText nc_mima2;
    private Button btn_sure;
    private ACache cache;
    private MqttService.MqttBinder mqttBinder;
    private CommReceiver commReceiver;
    private String imei;
    private String ip;
    private String port;
    private char[] dev_mima ={9,9,9,9,9,9};
    private boolean hw_status = false;
    private boolean lock_status = false;
    private boolean qu_flag = false;
    private boolean cun_flag = false;
    private Handler handler = new Handler();
    private Timer timer;
    private String randNum_str;

    class CommReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            switch (intent.getIntExtra("msg", 0)) {
                case MqttService.CONNECTED:
                    Toast.makeText(MainActivity.this, "连上了服务器", Toast.LENGTH_LONG).show();
                    break;
                case MqttService.FAIL:
                    Toast.makeText(MainActivity.this, "连接失败，5S后重连", Toast.LENGTH_SHORT).show();
                    handler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            mqttBinder.connect("tcp://" + ip + ":" + port,"admin", "password",randNum_str, imei);
                        }
                    }, 5000);
                    break;
                case MqttService.DISCONNECTED:
                    Toast.makeText(MainActivity.this, "连接断开，5S后重连", Toast.LENGTH_SHORT).show();
                    handler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            mqttBinder.connect("tcp://" + ip + ":" + port, "admin", "password",randNum_str, imei);
                        }
                    }, 5000);
                    break;
                case MqttService.RECV:
                    String content = intent.getStringExtra("content");
                    Log.e(TAG, content);
                    updateUI(content);
                    break;
                default:
                    break;

            }
        }
    }

    private ServiceConnection servConn = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {

            mqttBinder = (MqttService.MqttBinder)service;
            mqttBinder.connect("tcp://" + ip + ":" + port, "admin", "password",randNum_str,imei);
        }
        @Override
        public void onServiceDisconnected(ComponentName name) {
        }
    };

    private void updateUI(String str)
    {
                char [] strArr1 = str.toCharArray();
                if(strArr1[0]=='T')
                {
                    if (timer != null) {
                        timer.cancel();
                        timer = null;

                        timer = new Timer();
                        timer.schedule(new TimerTask() {
                            @Override
                            public void run() {
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        if (mqttBinder.getConnStatus()) {
                                            Toast.makeText(MainActivity.this, "设备下线了", Toast.LENGTH_LONG).show();
                                        }
                                    }
                                });
                            }
                        }, 60000);

                    }
                int hw = (strArr1[1]-0x30);
                int lock = (strArr1[2]-0x30);
                for (int i=0;i<6;i++) {
                    dev_mima[i] = strArr1[3+i];
                }
                if(hw==0)
                {
                    hw_status = false;             //货物空
                    nc_hw.setRightText("空");
                    btn_cun.setEnabled(true);
                    btn_lock.setEnabled(false);
                }
                else
                {
                    hw_status = true;             //货物满
                    nc_hw.setRightText("满");
                    btn_lock.setEnabled(true);
                    btn_cun.setEnabled(false);
                }
                if(lock==0)
                {
                    lock_status = false;        //锁开
                    nc_lock.setRightText("开");
                }
                else
                {
                    lock_status = true;        //锁关
                    nc_lock.setRightText("关");
                }
            }
    }

    private void initPara() {
        cache = ACache.get(this);
        imei = cache.getAsString("imei");
        ip = cache.getAsString("ip");
        port = cache.getAsString("port");

        int N = 999999999;

        Random rand = new Random();

        int randNum = rand.nextInt(N);

        randNum_str = String.valueOf(randNum);

        commReceiver = new CommReceiver();
        Intent intent = new Intent(MainActivity.this, MqttService.class);
        bindService(intent, servConn, BIND_AUTO_CREATE);
        IntentFilter filter = new IntentFilter("com.sq.wolf.mqtt");
        registerReceiver(commReceiver, filter);
    }

    private void initUI() {

        nc_hw = (NavigationCustom)findViewById(R.id.nc_temp);
        nc_lock = (NavigationCustom)findViewById(R.id.nc_humi);
        btn_lock = (Button)findViewById(R.id.button);
        btn_cun = (Button)findViewById(R.id.button1);
        nc_mima = (EditText)findViewById(R.id.et_ip);
        nc_mima2 = (EditText)findViewById(R.id.et_port);
        btn_sure = (Button)findViewById(R.id.btn_sure);

        btn_cun.setEnabled(false);
        btn_lock.setEnabled(false);
        nc_mima.setEnabled(false);
        nc_mima2.setEnabled(false);
        btn_sure.setEnabled(false);

        btn_lock.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                nc_mima.setEnabled(true);
                nc_mima2.setEnabled(true);
                btn_sure.setEnabled(true);
                qu_flag = true;
                cun_flag = false;
            }
        });

        btn_cun.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                nc_mima.setEnabled(true);
                nc_mima2.setEnabled(true);
                btn_sure.setEnabled(true);
                qu_flag = false;
                cun_flag = true;
            }
        });

        btn_sure.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String M1 = nc_mima.getText().toString();
                String M2 = nc_mima2.getText().toString();
                if (M1.length()!=6)
                {
                    Toast.makeText(MainActivity.this, "密码长度需为6位数字", Toast.LENGTH_SHORT).show();
                    return;
                }
                if(!M1.equals(M2))
                {
                    Toast.makeText(MainActivity.this, "两次输入密码不一致", Toast.LENGTH_SHORT).show();
                    return;
                }
                else {
                    if (qu_flag) {
                        String MIMA = String.valueOf(dev_mima);
                        if(!M1.equals(MIMA)) {
                            Toast.makeText(MainActivity.this, "与设备密码不一致", Toast.LENGTH_SHORT).show();
                            return;
                        } else {
                            if (!mqttBinder.getConnStatus()) {
                                Toast.makeText(MainActivity.this, "未连上服务器", Toast.LENGTH_SHORT).show();
                                return;
                            }
                            btn_cun.setEnabled(false);
                            btn_lock.setEnabled(false);
                            nc_mima.setText("");
                            nc_mima2.setText("");
                            nc_mima.setEnabled(false);
                            nc_mima2.setEnabled(false);
                            btn_sure.setEnabled(false);

                            MqttMessage msg = new MqttMessage();
                            msg.setQos(0);
                            msg.setRetained(false);

                            msg.setPayload(("11").getBytes());              //开锁

                            mqttBinder.publish("nydev/" + imei, msg);




                        }
                    }
                    else if (cun_flag)
                    {
                        if (!mqttBinder.getConnStatus()) {
                            Toast.makeText(MainActivity.this, "未连上服务器", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        btn_cun.setEnabled(false);
                        btn_lock.setEnabled(false);
                        nc_mima.setText("");
                        nc_mima2.setText("");
                        nc_mima.setEnabled(false);
                        nc_mima2.setEnabled(false);
                        btn_sure.setEnabled(false);

                        MqttMessage msg = new MqttMessage();
                        msg.setQos(0);
                        msg.setRetained(false);


                        msg.setPayload(("00"+M1).getBytes());             //闭锁+新密码

                        mqttBinder.publish("nydev/" + imei, msg);


                    }
                }
            }
        });
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initPara();
        initUI();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mqttBinder.disconnect();
        unbindService(servConn);
        unregisterReceiver(commReceiver);
    }
}
