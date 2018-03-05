package ravn.networkbroadcasttest;

import android.content.Context;
import android.graphics.Color;
import android.net.wifi.WifiManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.Toast;

import java.util.Vector;

import ravn.networkbroadcastshared.BroadcastTestImpl;
import ravn.udpbroadcasttest.R;

public class BroadcastTestActivity extends AppCompatActivity {

    BroadcastTestImpl broadcastTest = new BroadcastTestImpl();
    WifiManager.WifiLock wifiLock = null;
    WifiManager.MulticastLock multicastWifiLock = null;
    //boolean changeCheckboxColor = true;
    Vector<String> logTextList = new Vector<String>();
    Runnable uiLogThread = null;
    public void checkUILogThread(){
        if (uiLogThread==null) {
            BroadcastTestActivity.this.runOnUiThread(uiLogThread = new Runnable() {
                @Override
                public void run() {
                    synchronized (logTextList) {
                        EditText logText = (EditText) findViewById(R.id.logText);
                        for (String txt : logTextList) {
                            logText.setText(logText.getText() + System.lineSeparator() + txt);
                        }
                        uiLogThread = null;
                        logTextList.clear();
                        ScrollView logScroll = (ScrollView) findViewById(R.id.logScroll);
                        logScroll.requestLayout();
                    }
                    BroadcastTestActivity.this.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            ScrollView logScroll = (ScrollView) findViewById(R.id.logScroll);
                            logScroll.fullScroll(View.FOCUS_DOWN);
                        }
                    });
                }
            });
        }
    }
    @Override
    protected void onStop() {
        super.onStop();
        System.out.println("BroadcastTestActivity: onStop()");
        BroadcastTestImpl.messageCall = null;
        broadcastTest.clearListeningThreads();
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        System.out.println("BroadcastTestActivity: broadcastTest.isInitialized=" + broadcastTest.isInitialized);
        if (broadcastTest.isInitialized)
            return;
        broadcastTest.init();
        if (BroadcastTestImpl.messageCall!=null){
            System.out.println("BroadcastTestActivity.onCreate: BroadcastTestImpl.messageCall=" + BroadcastTestImpl.messageCall + " returning");
            return;
        }
        BroadcastTestImpl.messageCall = new BroadcastTestImpl.MessageCallback() {
            @Override
            public void call(String msg) {
                final String fmsg = msg;
                BroadcastTestActivity.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(getApplicationContext(), fmsg, Toast.LENGTH_SHORT).show();
                    }
                });
            }
            @Override
            public void addTextLineToLog(String txt) {
                synchronized (logTextList) {
                    logTextList.add(txt);
                }
                checkUILogThread();
            }
            @Override
            public void udpConnected(boolean con){
                System.out.println("udpConnected: con=" + con);
                CheckBox udp_connected_checkbox = (CheckBox) findViewById(R.id.udpCheckBox);
                udp_connected_checkbox.setChecked(con);
                //if (changeCheckboxColor)
                //    udp_connected_checkbox.set(con ? Color.BLACK : Color.RED);
                Button udp_button = (Button) findViewById(R.id.udp_button);
                udp_button.setEnabled(con);
                CheckBox udp_checkbox = (CheckBox) findViewById(R.id.udp_checkbox);
                if (!con)
                    udp_checkbox.setChecked(false);
                udp_checkbox.setEnabled(con);
            }
            @Override
            public void multicastConnected(boolean con){
                System.out.println("multicastConnected: con=" + con);
                CheckBox multicast_connected_checkbox = (CheckBox) findViewById(R.id.multicastCheckBox);
                multicast_connected_checkbox.setChecked(con);
                //if (changeCheckboxColor)
                //    multicast_connected_checkbox.setTextColor(con ? Color.BLACK : Color.RED);
                Button multicast_button = (Button) findViewById(R.id.multicast_checkbox);
                multicast_button.setEnabled(con);
                CheckBox multicast_checkbox = (CheckBox) findViewById(R.id.multicast_checkbox);
                if (!con)
                    multicast_checkbox.setChecked(false);
                multicast_checkbox.setEnabled(con);
            }
        };
        getWindow().setTitle("Broadcast Window Testing");

        System.setProperty("java.net.preferIPv4Stack", "true");
        WifiManager wifi = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        wifiLock = wifi.createWifiLock(WifiManager.WIFI_MODE_FULL_HIGH_PERF, "test-tag");
        multicastWifiLock = wifi.createMulticastLock("multicast-test-tag");
        wifiLock.acquire();
        multicastWifiLock.acquire();
        setContentView(R.layout.activity_broadcast_test);

        EditText computerLabelText = (EditText) findViewById(R.id.computerLabel);
        computerLabelText.setKeyListener(null);

        EditText computerNumberText = (EditText) findViewById(R.id.computerNumber);
        computerNumberText.setKeyListener(null);
        computerNumberText.setText(String.valueOf(broadcastTest.computerID));

        EditText logText = (EditText) findViewById(R.id.logText);
        logText.setKeyListener(null);
        logText.clearFocus();
        logText.setFocusable(false);

        Button multicastbutton = (Button) findViewById(R.id.multicastbutton);
        multicastbutton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Thread newT = new Thread(){
                    public void run(){
                        broadcastTest.sendMulticastMessage();
                    }
                };
                newT.start();
            }
        });
        CheckBox multicast_checkbox = (CheckBox) findViewById(R.id.multicast_checkbox);
        multicast_checkbox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton var1, boolean val){
                System.out.println("Multicast CheckBox Pressed val=" + val);
                broadcastTest.repeatMulticastSendThreadSet(val);
            }
        });

        Button udp_button = (Button) findViewById(R.id.udp_button);
        udp_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Thread newT = new Thread(){
                    public void run(){
                        broadcastTest.sendUDPMessage(AndroidSocketUtils.getBroadcastAddress(BroadcastTestActivity.this));
                    }
                };
                newT.start();
            }
        });
        CheckBox udp_checkbox = (CheckBox) findViewById(R.id.udp_checkbox);
        udp_checkbox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton var1, boolean val){
                System.out.println("UDP CheckBox Pressed val=" + val);
                broadcastTest.repeatUDPSendThreadSet(AndroidSocketUtils.getBroadcastAddress(BroadcastTestActivity.this), val);
            }
        });

        Button clear = (Button) findViewById(R.id.clear);
        clear.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                EditText logText = (EditText) findViewById(R.id.logText);
                logText.setText("");
                System.out.println("logText cleared");
            }
        });

        Button reset = (Button) findViewById(R.id.reset);
        reset.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                System.out.println("Reset pressed");
                broadcastTest.setupListeningThreads();
                Toast.makeText(getApplicationContext(), "Reset pressed", Toast.LENGTH_SHORT).show();
            }
        });

        Button exit = (Button) findViewById(R.id.exit);
        exit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                System.out.println("Exit pressed");
                System.exit(0);
            }
        });

        CheckBox udp_connected_checkbox = (CheckBox) findViewById(R.id.udpCheckBox);
        udp_connected_checkbox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                boolean con = false;
                if (broadcastTest!=null) {
                    con = broadcastTest.udpIsConnected;
                }
                if (con != b) {
                    CheckBox udp_connected_checkbox = (CheckBox) findViewById(R.id.udpCheckBox);
                    udp_connected_checkbox.setChecked(con);
                }
            }
        });
        CheckBox multicast_connected_checkbox = (CheckBox) findViewById(R.id.multicastCheckBox);
        multicast_connected_checkbox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                boolean con = false;
                if (broadcastTest!=null) {
                    con = broadcastTest.multicastIsConnected;
                }
                if (con != b) {
                    CheckBox multicast_connected_checkbox = (CheckBox) findViewById(R.id.multicastCheckBox);
                    multicast_connected_checkbox.setChecked(con);
                }
            }
        });
        broadcastTest.setupListeningThreads();
    }
}
