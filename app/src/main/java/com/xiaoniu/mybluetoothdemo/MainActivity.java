package com.xiaoniu.mybluetoothdemo;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.text.SpannableStringBuilder;
import android.text.style.AbsoluteSizeSpan;
import android.text.style.ForegroundColorSpan;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import android.graphics.Color;
import android.text.SpannableString;
import android.os.Handler;
import android.os.Message;

import com.xiaoniu.mybluetoothdemo.adapter.BlueToothDeviceAdapter;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.URISyntaxException;
import java.util.Calendar;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.BarGraphSeries;

import net.wyun.audio.domain.AudioPayload;
import net.wyun.audio.rest.AudioReader;

import static android.text.Spanned.SPAN_INCLUSIVE_INCLUSIVE;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private BluetoothAdapter bTAdatper;
    private BluetoothSocket mSocket;
    private BlueToothDeviceAdapter adapter;

    public static final String DIR_PATH = Environment.getExternalStorageDirectory() + "/心音文件/";
    public String currentFile = "",fileName = "";
    private FileOutputStream mStream;
    private FileInputStream inStream;
    private int chkFlag = 0;
    private static final int MAX_SIZE = 8*1000*60;

    private static final String webIPAddress = "39.108.8.106";
    public String ipAddr = webIPAddress;
    private int sampleCount = 0;

    private GraphView graph1,graph2,graph3;
    BarGraphSeries<DataPoint> seriesLine1,seriesLine2,seriesLine3;
    int iMaxPoints = 400;
    int numTimeCycle = 40000;
    DataPoint[] values1 = new DataPoint[iMaxPoints];//心音波形数据
    DataPoint[] values2 = new DataPoint[iMaxPoints];//血氧波形数据
    DataPoint[] values3 = new DataPoint[iMaxPoints];//心电波形数据
    private int idxV1 = 0,idxV2 = 0,idxV3 = 0;//波形数据位置指针

    int[] dEcg= new int[iMaxPoints];
    int ecgCounter = 0;
    int[] dPcg = new int[iMaxPoints];
    int hrtCounter = 0;

    String heartRate = "--";
    int[] hrArray = new int[15];
    int hrCounter = 0;

    int size = 20;//设置采样表示文字大小
    int tSize = 30;//设置采样文本框显示的文字大小

    private TextView text_state;
    private TextView t1_msg,t2_msg,t3_msg,wd_msg,xy_msg,dp_msg;
    private EditText text_name;
    private Button btn;
    private int idMsg = 1;
    private int idModule = 0;

    private final int BUFFER_SIZE = 16*1024;
    byte[] buffer = new byte[BUFFER_SIZE];
    byte[] dataReceived = new byte[BUFFER_SIZE];

    private static final String NAME = "BT_DEMO";
    private static final UUID BT_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    private ConnectThread connectThread;
    private ListenerThread listenerThread;

    //波形启动/停止命令51
    private byte[] startMsg1 ={(byte)0xf0,(byte)0xc0,(byte)0x03,(byte)0xa3,(byte)0xa0};
    private byte[] stopMsg1 = {(byte)0xf0,(byte)0xc0,(byte)0x03,(byte)0xa4,(byte)0xa1};
    //血压启动/停止命令51
    private byte[] startMsg2 ={(byte)0xf0,(byte)0xc0,(byte)0x03,(byte)0xa5,(byte)0xa2};
    private byte[] stopMsg2 = {(byte)0xf0,(byte)0xc0,(byte)0x03,(byte)0xa6,(byte)0xa3};

    //血氧启动/停止命令
    //private byte[] startMsg1 ={(byte)0xff,(byte)0xc7,(byte)0x03,(byte)0xa3,(byte)0xa0};
    //private byte[] stopMsg1 = {(byte)0xff,(byte)0xc7,(byte)0x03,(byte)0xa4,(byte)0xa1};
    //血压启动/停止命令
    //private byte[] startMsg2 ={(byte)0xff,(byte)0xc0,(byte)0x03,(byte)0xa3,(byte)0xa0};
    //private byte[] stopMsg2 = {(byte)0xff,(byte)0xcd,(byte)0x03,(byte)0xa4,(byte)0xa1};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initView();
        dataValueReset();
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},1);
        }
        /*seriesLine1 = new LineGraphSeries<DataPoint>(values1);
        seriesLine1.resetData(values1);
        seriesLine1.setThickness(5);
        seriesLine2 = new LineGraphSeries<DataPoint>(values2);
        seriesLine2.resetData(values2);
        seriesLine2.setThickness(5);
        seriesLine3 = new LineGraphSeries<DataPoint>(values3);
        seriesLine3.resetData(values3);
        seriesLine3.setThickness(5);*/
        seriesLine1 = new BarGraphSeries<DataPoint>(values1);
        seriesLine1.resetData(values1);
        //seriesLine1.setSpacing(30);
        seriesLine2 = new BarGraphSeries<DataPoint>(values2);
        seriesLine2.resetData(values2);
        //seriesLine2.setSpacing(30);
        seriesLine3 = new BarGraphSeries<DataPoint>(values3);
        seriesLine3.resetData(values3);
        //seriesLine3.setSpacing(30);;

        graph1 = (GraphView) findViewById(R.id.graph1);
        graph1.getViewport().setYAxisBoundsManual(true);
        graph1.getViewport().setXAxisBoundsManual(true);
        graph1.getViewport().setMinY(0);
        graph1.getViewport().setMaxY(65536);
        graph1.getViewport().setMaxX(iMaxPoints);
        graph1.getViewport().setMinX(0);
        graph1.getGridLabelRenderer().setNumHorizontalLabels(0);
        graph1.getGridLabelRenderer().setHorizontalLabelsVisible(false);
        graph1.getGridLabelRenderer().setNumVerticalLabels(0);
        graph1.getGridLabelRenderer().setVerticalLabelsVisible(false);
        graph1.getGridLabelRenderer().setGridColor(Color.GREEN);
        seriesLine1.setColor(Color.GREEN);
        graph1.addSeries(seriesLine1);

        graph2 = (GraphView) findViewById(R.id.graph2);
        graph2.getViewport().setYAxisBoundsManual(true);
        graph2.getViewport().setXAxisBoundsManual(true);
        graph2.getViewport().setMinY(0);
        graph2.getViewport().setMaxY(128);
        graph2.getViewport().setMaxX(iMaxPoints);
        graph2.getViewport().setMinX(0);
        graph2.getGridLabelRenderer().setNumHorizontalLabels(0);
        graph2.getGridLabelRenderer().setHorizontalLabelsVisible(false);
        graph2.getGridLabelRenderer().setNumVerticalLabels(0);
        graph2.getGridLabelRenderer().setVerticalLabelsVisible(false);
        graph2.getGridLabelRenderer().setGridColor(Color.YELLOW);
        seriesLine2.setColor(Color.YELLOW);
        graph2.addSeries(seriesLine2);

        graph3 = (GraphView) findViewById(R.id.graph3);
        graph3.getViewport().setYAxisBoundsManual(true);
        graph3.getViewport().setXAxisBoundsManual(true);
        graph3.getViewport().setMinY(0);
        graph3.getViewport().setMaxY(4096);
        graph3.getViewport().setMaxX(iMaxPoints);
        graph3.getViewport().setMinX(0);
        graph3.getGridLabelRenderer().setNumHorizontalLabels(0);
        graph3.getGridLabelRenderer().setHorizontalLabelsVisible(false);
        graph3.getGridLabelRenderer().setNumVerticalLabels(0);
        graph3.getGridLabelRenderer().setVerticalLabelsVisible(false);
        graph3.getGridLabelRenderer().setGridColor(Color.GREEN);
        seriesLine3.setColor(Color.GREEN);
        graph3.addSeries(seriesLine3);

        bTAdatper = BluetoothAdapter.getDefaultAdapter();
        if (bTAdatper == null) {
            Toast.makeText(this, "当前设备不支持蓝牙功能", Toast.LENGTH_SHORT).show();
        }

        if (!bTAdatper.isEnabled()){
            bTAdatper.enable();
        }

        initReceiver();
        listenerThread = new ListenerThread();
        listenerThread.start();
    }

    private void initView() {
        btn = (Button)findViewById(R.id.btn_search);
        findViewById(R.id.btn_search).setOnClickListener(this);
        findViewById(R.id.check).setOnClickListener(this);
        text_state = (TextView) findViewById(R.id.text_state);

        t1_msg = (TextView) findViewById(R.id.t1);
        t2_msg = (TextView) findViewById(R.id.t2);
        t3_msg = (TextView) findViewById(R.id.t3);
        dp_msg = (TextView) findViewById(R.id.display);
        wd_msg = (TextView)findViewById(R.id.wd);
        xy_msg = (TextView)findViewById(R.id.xy);
        text_name =(EditText)findViewById(R.id.name);

        //btn.setWidth(40);
        text_state.setTextColor(Color.WHITE);
        //text_state.setTextSize(40);

        wd_msg.setTextColor(Color.RED);
        xy_msg.setTextColor(Color.RED);

        t1_msg.setTextColor(Color.WHITE);
        t2_msg.setTextColor(Color.RED);
        t3_msg.setTextColor(Color.WHITE);

        wd_msg.setTextSize(size);
        xy_msg.setTextSize(size);
        t1_msg.setTextSize(size);
        t2_msg.setTextSize(size);
        t3_msg.setTextSize(size);

        //btn.setTextSize(size);

        wd_msg.setText("体温\n");
        xy_msg.setText("血压\n");
        t1_msg.setText("心音");
        t2_msg.setText("血氧");
        t3_msg.setText("心电");
        adapter = new BlueToothDeviceAdapter(getApplicationContext(), R.layout.bluetooth_device_list_item);

        if(!isFolderExists(DIR_PATH)){
            dp_msg.setText("无法创建心音文件目录！");
        }


    }

    private void initReceiver() {
        //注册广播
        IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothDevice.ACTION_FOUND);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        registerReceiver(mReceiver, filter);
    }
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                //避免重复添加已经绑定过的设备
                if (device.getBondState() != BluetoothDevice.BOND_BONDED) {
                    adapter.add(device);
                    adapter.notifyDataSetChanged();
                }
            } else if (BluetoothAdapter.ACTION_DISCOVERY_STARTED.equals(action)) {
                Toast.makeText(MainActivity.this, "开始搜索", Toast.LENGTH_SHORT).show();
            } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                Toast.makeText(MainActivity.this, "搜索完毕", Toast.LENGTH_SHORT).show();
            }
        }
    };

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_search:
                if(mSocket!=null && mSocket.isConnected()){
                    startModule(idMsg%2+1);
                    //btn.setText("启动");
                }
                else {
                    if(mSocket != null){
                        try{
                            mSocket.close();
                            mSocket = null;
                        }catch (IOException e){
                            e.printStackTrace();
                        }
                    }
                    dp_msg.setText("");
                    searchDevices();
                }

                if(chkFlag == 1){//主动停止采样并准备上传分析
                    try {
                        if (mStream != null) {
                            mStream.close();
                            sampleCount = 0;
                            new Thread(networkTask).start();
                            //chkFlag = 0;
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                }
                break;
            case R.id.check:
                String cmdS = text_name.getText().toString();
                if(cmdS.equals("!f")&&(chkFlag != 1) && (chkFlag !=99)) {
                    Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                    File f = new File(Environment.getExternalStorageDirectory() + "/心音文件");
                    //intent.setDataAndType(Uri.fromFile(f),"*/*");
                    intent.setType("*/*");
                    intent.addCategory(Intent.CATEGORY_OPENABLE);
                    startActivityForResult(intent, 1);
                    Toast.makeText(this, "打开文件...", Toast.LENGTH_SHORT).show();
                    break;
                }

                if(!(mSocket!=null && mSocket.isConnected())){
                    dp_msg.setText(dp_msg.getText()+"请先启动测量...!");
                    break;
                }
                if(chkFlag != 99){
                    if(chkFlag == 0){
                        createOutputFile();
                        chkFlag = 1;
                        dp_msg.setText(fileName+"心音信号采样分析开始...");
                    }else {
                        if (chkFlag == 1) {
                            chkFlag = 2;
                        }
                    }
                }
                else{
                    dp_msg.setText(fileName+ "\n心音数据正在分析过程中...");
                }
                break;

        }
    }
    public String FileGetPath(Context context, Uri uri) throws URISyntaxException {
        if ("content".equalsIgnoreCase(uri.getScheme())) {
            String[] projection = { "_data" };
            Cursor cursor = null;
            try {
                cursor = context.getContentResolver().query(uri, projection, null, null, null);
                int column_index = cursor.getColumnIndexOrThrow("_data");
                if (cursor.moveToFirst()) {
                    return cursor.getString(column_index);
                }
            } catch (Exception e) {
                // Eat it  Or Log it.
            }
        } else if ("file".equalsIgnoreCase(uri.getScheme())) {
            return uri.getPath();
        }
        return null;
    }
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == 1) {
                Uri uri = data.getData();
                //currentFile = uri.getPath().toString();
                try {
                    currentFile = FileGetPath(this, uri);
                }catch (URISyntaxException e){

                }
                //Toast.makeText(this, "文件路径："+uri.getPath().toString(), Toast.LENGTH_SHORT).show();
                Log.d("File Uri: " , uri.toString());
                int start = -1;
                if(currentFile != null){
                    start = currentFile.lastIndexOf("/");
                }else{
                    dp_msg.post(new Runnable() {
                        @Override
                        public void run() {
                            dp_msg.setText(currentFile +"-文件出错...");
                        }
                    });

                }
                if(start != -1){
                    fileName = currentFile.substring(start+1);
                    dp_msg.setText(fileName);
                    new Thread(networkTask).start();

                }
            }
        }
    }
    public String startModule(int i){
        String s = "";
        switch (i){
            case 1:
                connectThread.sendMsg(stopMsg2);
                connectThread.sendMsg(startMsg1);
                idMsg = 1;
                s = new String("(波形)");
                xy_msg.setText("血压");
                break;
            case 2:
                 connectThread.sendMsg(stopMsg1);
                 connectThread.sendMsg(startMsg2);
                 idMsg = 2;
                 s = new String("血压测量中...");
                 xy_msg.setText(s);
                 break;
        }
        return s;
    }
    /**
     * 搜索蓝牙设备
     */
    private void searchDevices() {
        if (bTAdatper.isDiscovering()) {
            bTAdatper.cancelDiscovery();
        }
        if (!bTAdatper.isEnabled()){
            bTAdatper.enable();
        }
      getBoundedDevices();
    }

    /**
     * 获取已经配对过的设备
     */
    private void getBoundedDevices() {
        //获取已经配对过的设备
        Set<BluetoothDevice> pairedDevices = bTAdatper.getBondedDevices();
        //将其添加到设备列表中

        int pairedFlag = 0;
        if (pairedDevices.size() > 0) {
            for (BluetoothDevice device : pairedDevices) {
                adapter.add(device);
                String s;
                s = device.getName();

                if(s.length()>= 3){
                    s = s.substring(0,3);
                    if (s.equals("HKL") && (pairedFlag==0)){
                        pairedFlag = 1;
                        dp_msg.setText("测量启动中...");
                        connectDevice(device);
                    }
                }
            }
        } else{
            dp_msg.setText("请通过手机设置菜单进行蓝牙配对：HKL-配对码XXXX：0438！");
        }
        if(pairedFlag != 1){
            dp_msg.setText("请通过手机设置菜单进行蓝牙配对：HKL-配对码XXXX：0438！");
        }
    }

    /**
     * 连接蓝牙设备
     */
    private void connectDevice(BluetoothDevice device) {

        text_state.setText(getResources().getString(R.string.connecting));

        try {
            //创建Socket
            if(mSocket == null){
                mSocket = device.createRfcommSocketToServiceRecord(BT_UUID);
            }else{
                mSocket.close();
                mSocket = null;
                mSocket = device.createRfcommSocketToServiceRecord(BT_UUID);
            }

            //启动连接线程
            connectThread = new ConnectThread(mSocket, true);
            connectThread.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(connectThread != null){
            connectThread.sendMsg(stopMsg1);
            connectThread.sendMsg(stopMsg2);
        }
        if(mStream != null){
            try {
                mStream.close();
            }catch (IOException e){
                e.printStackTrace();
            }
        }
        //取消搜索
        if (bTAdatper != null && bTAdatper.isDiscovering()) {
            bTAdatper.cancelDiscovery();
        }
        //注销BroadcastReceiver，防止资源泄露
        unregisterReceiver(mReceiver);
        if (bTAdatper != null) {
            bTAdatper.disable();
        }
    }


    Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            Bundle data = msg.getData();
            int hv = data.getInt("value");
            int a=0,b=0;
            //String hrS = "";
            // TODO
            // UI界面的更新等相关操作
            String val = data.getString("result");
            //Log.i("mylog", "请求结果为-->" + val);
            // TODO
            // UI界面的更新等相关操作
            if(val != null){
                dp_msg.setText(fileName + "\n"+val);
            }

            if (hv == 1) {
                graph1.removeAllSeries();
                seriesLine1.resetData(values1);
                //seriesLine.setSpacing(50);
                graph1.addSeries(seriesLine1);
                if(hrCounter == iMaxPoints){
                    heartRate = hrPcg();
                    hrCounter = 0;
                }

            }
            ;
            if (hv == 2) {
                graph2.removeAllSeries();
                seriesLine2.resetData(values2);
                //seriesLine.setSpacing(50);
                graph2.addSeries(seriesLine2);
            }
            ;
            if (hv == 3) {
                graph3.removeAllSeries();
                seriesLine3.resetData(values3);
                //seriesLine.setSpacing(50);
                graph3.addSeries(seriesLine3);
                if(ecgCounter == iMaxPoints){
                    heartRate = hrEcg();
                    ecgCounter = 0;
                }
                SpannableString s3 = new SpannableString("心电\n"+String.valueOf(heartRate)+"bpm");
                s3.setSpan(new AbsoluteSizeSpan(size,true), (s3.length()-3),(s3.length()),SPAN_INCLUSIVE_INCLUSIVE);
                s3.setSpan(new AbsoluteSizeSpan(size,true), 0, 3, SPAN_INCLUSIVE_INCLUSIVE);
                t3_msg.setText(s3);

            }
            ;
        }
    };
    public String hrPcg(){
        int hr = 0,i = 0,j = 0,k = 0,average = 0,hr1 = 0;
        int[] dM= new int[8],aM = new int[8];

        //心电采样的800个点分成8段，每段100个点持续时间0.5秒
        //在这100个点中找出最大值及其地址保存到dM和aM中；
        while(i<(iMaxPoints/100)){
            j = 0;
            k = i*100;
            dM[i] = dPcg[k];
            aM[i] = k;
            while(j<99){
                if(dPcg[k+j+1] >= dM[i]){
                    dM[i] = dPcg[k+j+1];
                    aM[i] = k+j+1;
                }
                j++;
            }
            i++;
        }
        //最大值间距小于0.5秒的将小的最大值置零，两次心跳间隔不应小于0.5秒
        //保留下来的最大值是可能最大的心跳峰值点；
        i = 0;
        j = 0;
        while(i<7){
            if(aM[i+1]-aM[i]<100){
                j++;
                if(dM[i+1]<dM[i]){
                    dM[i+1] = 0;
                    i = i + 2;
                }else{
                    dM[i] = 0;
                    i++ ;
                }
            }else{
                i++;
            }

        }
        //计算保留的最大值的均值，防止由于分段过小（0.5秒），导致非峰值心跳误当作为心跳峰值；
        //先计算平均峰值，进行比较各心跳峰值和平均峰值，小于均值的判定为假心跳峰值；
        i = 0;
        while (i < 8) {
            average = average+dM[i];
            i++;
        }
        if(j < 8) {
            average = average / (8 - j);
        }else{
            average = 0;
        }
        //最大值小于最大值均值一半的峰值置零，为零的表示为假心跳峰值；
        i = 0;
        j = 0;
        while (i < 8) {
            if(dM[i] <= average/2){
                dM[i] = 0;
                j++;
            };
            i++;
        }

        //将此4秒时间段的心跳峰值个数保存在对应的心跳数组中
        hrArray[hrCounter%15] = 8-j;
        hrCounter++;

        //保留在心跳数组中的心跳峰值个数求和，如果不满1分钟，则乘以系数15/j;
        i = 0;
        hr = 0;
        j= hrCounter;
        if(j>=15){
            j = 15;
        }
        while(i < j){
            hr = hr + hrArray[i];
            i++;
        }
        if(j != 0) {
            hr = hr * 15 / j;// 1分钟的心跳个数bpm
        }else{
            hr = 0;//心跳个数为零表示还未开始采样
        }

        //计算此4秒时间段保留下来的心跳峰值间的时间间隔总和的均值；
        //利用时间间隔均值
        i = 0;
        j = 0;
        k = 0;
        hr1 = 0;
        while(i < 8){
            if(dM[i] != 0){
                j = i + 1;
                while(j < 8){
                    if(dM[j] != 0){
                        k = k + aM[j]-aM[i];
                        i = j;
                        j++;
                        hr1++;
                    }else{
                        j++;
                    }
                }
                i = 8;
            }else {
                i++;
            }
        }
        if(k != 0) {
            //平均心跳间隔时间计算所得的心律；
            //（60秒*1000/5毫秒）/（k/hr1）：1分钟有60000毫秒，5毫秒采样一个点则总点数为12000个点
            //心跳的周期平均是k/hr1个点，则心率为12000个点除以（k/hr1）bpm
            hr1 = 12 * 1000 * hr1 / k;
        }

        return String.valueOf((hr1));
    }
    public String hrEcg(){
        int hr = 0,i = 0,j = 0,k = 0,average = 0,hr1 = 0;
        int[] dM= new int[8],aM = new int[8];

        //心电采样的800个点分成8段，每段100个点持续时间0.5秒
        //在这100个点中找出最大值及其地址保存到dM和aM中；
        while(i<(iMaxPoints/100)){
            j = 0;
            k = i*100;
            dM[i] = dEcg[k];
            aM[i] = k;
            while(j<99){
                if(dEcg[k+j+1] >= dM[i]){
                    dM[i] = dEcg[k+j+1];
                    aM[i] = k+j+1;
                }
                j++;
            }
            i++;
        }
        //最大值间距小于0.5秒的将小的最大值置零，两次心跳间隔不应小于0.5秒
        //保留下来的最大值是可能最大的心跳峰值点；
        i = 0;
        j = 0;
        while(i<7){
            if(aM[i+1]-aM[i]<100){
                j++;
                if(dM[i+1]<dM[i]){
                    dM[i+1] = 0;
                    i = i + 2;
                }else{
                    dM[i] = 0;
                    i++ ;
                }
            }else{
                i++;
            }

        }
        //计算保留的最大值的均值，防止由于分段过小（0.5秒），导致非峰值心跳误当作为心跳峰值；
        //先计算平均峰值，进行比较各心跳峰值和平均峰值，小于均值的判定为假心跳峰值；
        i = 0;
        while (i < 8) {
            average = average+dM[i];
            i++;
        }
        if(j < 8) {
            average = average / (8 - j);
        }else{
            average = 0;
        }
        //最大值小于最大值均值一半的峰值置零，为零的表示为假心跳峰值；
        i = 0;
        j = 0;
        while (i < 8) {
            if(dM[i] <= average/2){
                dM[i] = 0;
                j++;
            };
            i++;
        }

        //将此4秒时间段的心跳峰值个数保存在对应的心跳数组中
        hrArray[hrCounter%15] = 8-j;
        hrCounter++;

        //保留在心跳数组中的心跳峰值个数求和，如果不满1分钟，则乘以系数15/j;
        i = 0;
        hr = 0;
        j= hrCounter;
        if(j>=15){
            j = 15;
        }
        while(i < j){
            hr = hr + hrArray[i];
            i++;
        }
        if(j != 0) {
            hr = hr * 15 / j;// 1分钟的心跳个数bpm
        }else{
            hr = 0;//心跳个数为零表示还未开始采样
        }

        //计算此4秒时间段保留下来的心跳峰值间的时间间隔总和的均值；
        //利用时间间隔均值
        i = 0;
        j = 0;
        k = 0;
        hr1 = 0;
        while(i < 8){
            if(dM[i] != 0){
                j = i + 1;
                while(j < 8){
                    if(dM[j] != 0){
                        k = k + aM[j]-aM[i];
                        i = j;
                        j++;
                        hr1++;
                    }else{
                        j++;
                    }
                }
                i = 8;
            }else {
                i++;
            }
        }
        if(k != 0) {
            //平均心跳间隔时间计算所得的心律；
            //（60秒*1000/5毫秒）/（k/hr1）：1分钟有60000毫秒，5毫秒采样一个点则总点数为12000个点
            //心跳的周期平均是k/hr1个点，则心率为12000个点除以（k/hr1）bpm
            hr1 = 12 * 1000 * hr1 / k;
        }

        return String.valueOf((hr1));
    }
    /**
     * 连接线程
     */
    private class ConnectThread extends Thread {

        private BluetoothSocket socket;
        private boolean activeConnect;
        InputStream inputStream;
        OutputStream outputStream;
        SpannableString s1 = new SpannableString("心音\n");
        SpannableString s2 = new SpannableString("血氧\n");
        SpannableString s3 = new SpannableString("心电\n");
        SpannableString wd = new SpannableString("体温\n");
        SpannableString xy = new SpannableString("血压\n");


        int dataIdx = 0;
        int dataLen = 0;
        int iSearch = 0;

        int h1 = 0,h2 = 0;
        String sOz1,sOz2;

        int hv = 0;//采样值
        int powerIdx = 100;//电池剩余电量

        int numReceived = 0;
        int bytes = 0;
        String idS = "";//显示采样类型
        int iMatch = 0;
        String sOz ="";
        private ConnectThread(BluetoothSocket socket, boolean connect) {
            this.socket = socket;
            this.activeConnect = connect;
        }

        @Override
        public void run() {
            try {
                //如果是自动连接 则调用连接方法
                if (activeConnect) {
                    socket.connect();
                }
                text_state.post(new Runnable() {
                    @Override
                    public void run() {
                        text_state.setText(getResources().getString(R.string.connect_success));
                    }
                });
                inputStream = socket.getInputStream();
                outputStream = socket.getOutputStream();

                sendMsg(stopMsg2);
                sendMsg(startMsg1);//当蓝牙连接建立成功后，自动启动波形采样；
                idMsg = 1;

                while (true){
                    //读取数据
                    //bytes = 0;

                    if(idMsg == 1){
                        idS = "...波形";
                    }
                    if(idMsg == 2){
                        idS ="...血压";
                    }
                    if(bytes == 0){
                        idS =" ";
                    }

                    bytes = inputStream.read(buffer);
                    text_state.post(new Runnable() {
                        @Override
                        public void run() {//显示收到的数据字节数及轮询的时间周期/及采样通道标识//剩余电量
                            if(powerIdx > 0){
                                text_state.setText("..电量："+String.valueOf(powerIdx)+"%" + idS);// + String.valueOf(bytes));
                            }else{
                                text_state.setText("..电量："+"--%" + idS);// + String.valueOf(bytes));
                            }
                        }
                    });

                    if(bytes >= BUFFER_SIZE){
                        bytes = 0;
                        dp_msg.post(new Runnable() {
                            @Override
                            public void run() {
                                dp_msg.setText(fileName+"-采样："+ String.valueOf(sampleCount*60/MAX_SIZE)+"秒."+"\n数据缓存溢出！");
                            }
                        });
                        if(mStream != null){
                            mStream.close();
                            mStream = null;
                            if(sampleCount*60/MAX_SIZE >= 30){
                                new Thread(networkTask).start();
                            }else{
                                dp_msg.post(new Runnable() {
                                    @Override
                                    public void run() {
                                        dp_msg.setText(fileName+"-信号分析："+ String.valueOf(sampleCount*60/MAX_SIZE)+"秒."+"\n请重新测量分析！");
                                    }
                                });
                            }
                            sampleCount = 0;
                            dataLen = 0;
                            dataIdx = 0;
                            chkFlag = 0;
                        }

                    }
                    System.arraycopy(buffer,0,dataReceived,dataIdx,bytes);
                    dataLen = dataIdx + bytes;//上次未处理的dataIdx个数据和本次收到的数据进行合并,dataLen个数据需要处理

                    if ((idMsg == 1) && (dataLen > 0)) {
                        dataIdx = dataLen;//剩余处理的数据为dataLen个
                        if(dataLen >= 179){//数据达到处理的长度，进行数据提取和分析；
                            iSearch = 0;
                            while(iSearch <= dataLen - 5){
                                if((dataReceived[iSearch] == (byte)0xf0) && (dataReceived[iSearch+1] == (byte)0xc0) && (dataReceived[iSearch+2] == (byte)0xb1) && (dataReceived[iSearch+4] == (byte)0xa0)){//找到数据帧头，5个字节
                                    if((dataLen - iSearch) >= 179){
                                        //至少收到1个完整采样数据包，进行提取分析
                                        //字节：5（帧头）+1（电量）+3（血氧）+2（体温）+8（心电）+160（心音），共179个字节
                                        int dHead = iSearch + 5;//指向实际采样数据的起始数据

                                        ////////////////////////////////////////电量数据提取
                                        powerIdx = dataReceived[dHead];

                                        //////////////////////////////////////血氧提取，并显示波形
                                        hv = (int)dataReceived[dHead+3];
                                        if(hv<0){
                                            hv = hv+256;
                                        }

                                        int oz = (int)dataReceived[dHead+1];
                                        if(oz<0){
                                            oz = oz + 256;
                                        }
                                        int hr = (int)dataReceived[dHead+2];
                                        if(hr<0){
                                            hr = hr + 256;
                                        }
                                        if(oz>100){
                                            oz = 0;
                                            hr = 0;
                                        }
                                        sOz1 = "血氧\n"+ String.valueOf(oz)+"%\n";
                                        sOz2 = "心率\n"+ String.valueOf(hr)+"bpm";
                                        final SpannableStringBuilder builder = new SpannableStringBuilder(sOz1);
                                        builder.setSpan(new AbsoluteSizeSpan(size,true),0,3, SPAN_INCLUSIVE_INCLUSIVE);
                                        builder.setSpan(new ForegroundColorSpan(Color.RED),0,3, SPAN_INCLUSIVE_INCLUSIVE);
                                        builder.append(sOz2);
                                        builder.setSpan(new AbsoluteSizeSpan(size,true),sOz1.length(), sOz1.length()+3,SPAN_INCLUSIVE_INCLUSIVE);
                                        builder.setSpan(new ForegroundColorSpan(Color.RED),sOz1.length(), sOz1.length()+3,SPAN_INCLUSIVE_INCLUSIVE);
                                        builder.setSpan(new AbsoluteSizeSpan(size,true), (sOz1.length()+ sOz2.length()-3),(sOz1.length()+ sOz2.length()),SPAN_INCLUSIVE_INCLUSIVE);
                                        t2_msg.post(new Runnable() {
                                            @Override
                                            public void run() {
                                                t2_msg.setText(builder);
                                            }
                                        });

                                        ds(hv,2);//更新血氧波形数据

                                        if((idxV2+1)%50==0){//大概每1秒更新下血氧波形
                                            Message msg = new Message();
                                            Bundle eData = new Bundle();

                                            eData.putInt("value", 2);
                                            msg.setData(eData);
                                            handler.sendMessage(msg);
                                        }

                                        //////////////////////////////////////////////////////体温提取
                                        h1=dataReceived[dHead+5];
                                        h2=dataReceived[dHead+4];
                                        if(h1<0){
                                            h1 = h1+256;
                                        }
                                        if(h2<0){
                                            h2 = h2+256;
                                        }
                                        hv = h2*256+h1;
                                        double t = hv/10.0;
                                        wd = new SpannableString("体温\n"+String.valueOf(t)+"C°");
                                        wd_msg.post(new Runnable() {
                                            @Override
                                            public void run() {
                                                //wd.setSpan(new AbsoluteSizeSpan(size,true), 0, 3, SPAN_INCLUSIVE_INCLUSIVE);
                                                //wd.setSpan(new ForegroundColorSpan(Color.GREEN),0,3,SPAN_INCLUSIVE_INCLUSIVE);
                                                wd_msg.setText(wd);
                                            }
                                        });

                                        ///////////////////////////////////////////////////////心电提取并显示波形
                                        int xdCounter = dHead + 6;
                                        int xdjk = 0;
                                        while(xdjk < 4){
                                            h1 = dataReceived[xdCounter + 1 + xdjk*2];
                                            h2 = dataReceived[xdCounter + xdjk*2];
                                            if(h1<0){
                                                h1 = h1+256;
                                            }
                                            if(h2<0){
                                                h2 = h2+256;
                                            }
                                            hv = h2*256+h1;
                                            ds(hv,3);

                                            if((idxV3+1)%200==0){//大概每1秒更新下心电波形
                                                Message msg = new Message();
                                                Bundle eData = new Bundle();

                                                eData.putInt("value", 3);
                                                msg.setData(eData);
                                                handler.sendMessage(msg);
                                            }
                                            xdjk++;
                                        }

                                        ////////////////////////////////////心音数据提取并显示波形、进行心音数据分析
                                        int xyCounter = dHead + 14;
                                        int xyjk = 0;

                                        //需要采样并分析心音数据mStream不为空，则写入获得的160个心音采样字节；
                                        if(chkFlag == 1){
                                            try {
                                                if (mStream != null) {
                                                    if(sampleCount % 8000 == 0){//显示采样分析的时间
                                                        dp_msg.post(new Runnable() {
                                                            @Override
                                                            public void run() {
                                                                dp_msg.setText(fileName+"-采样："+ String.valueOf(sampleCount*60/MAX_SIZE)+"秒");//+String.valueOf(dataLen));
                                                            }
                                                        });
                                                    }

                                                    mStream.write(dataReceived,xyCounter,160);
                                                    sampleCount = sampleCount + 160;

                                                    if(sampleCount >= MAX_SIZE){//当采样到最大字节数时，停止采样并上传分析，目前设置为1分钟
                                                        mStream.close();
                                                        sampleCount = 0;
                                                        new Thread(networkTask).start();
                                                        //chkFlag = 0;
                                                    }
                                                }
                                                else{
                                                    dp_msg.post(new Runnable() {
                                                        @Override
                                                        public void run() {
                                                            dp_msg.setText("文件操作失败！...请在手机设置中授权存储权限!");//+String.valueOf(dataLen));
                                                        }
                                                    });
                                                }
                                            } catch (IOException e) {
                                                e.printStackTrace();
                                            }
                                        }
                                        if(chkFlag == 2){//主动停止采样并准备上传分析
                                            try {
                                                if (mStream != null) {
                                                    mStream.write(dataReceived,xyCounter,160);
                                                    sampleCount = sampleCount + 160;
                                                    mStream.close();
                                                    sampleCount = 0;
                                                    new Thread(networkTask).start();
                                                    //chkFlag = 0;
                                                }
                                            } catch (IOException e) {
                                                e.printStackTrace();
                                            }

                                        }

                                        //进行降频采样数据并显示波形
                                        while(xyjk < 4) {//16*10/2共80个心音数据采集点
                                            h1 = dataReceived[xyCounter + 1 + xyjk*40];//心音4K采样率进行降频为200Hz进行波形显示，每10个点取一个值进行显示，即一次只有8个点显示；
                                            h2 = dataReceived[xyCounter + xyjk*40];
                                            if(h1<0){
                                                h1 = h1+256;
                                            }
                                            if(h2<0){
                                                h2 = h2+256;
                                            }
                                            hv = h2*256+h1;
                                            ds(hv,1);
                                            s1 = new SpannableString("心音\n"+String.valueOf(hv)+"\n"+heartRate+"bpm");
                                            t1_msg.post(new Runnable() {
                                                @Override
                                                public void run() {
                                                    s1.setSpan(new AbsoluteSizeSpan(size,true), 0, 3, SPAN_INCLUSIVE_INCLUSIVE);
                                                    t1_msg.setText(s1);
                                                }
                                            });
                                            if((idxV1+1)%200==0){//大概每1000毫秒更新下心音波形
                                                Message msg = new Message();
                                                Bundle eData = new Bundle();

                                                eData.putInt("value", 1);
                                                msg.setData(eData);
                                                handler.sendMessage(msg);
                                            }
                                            xyjk++;
                                        }

                                        //本次数据包处理完毕后，i指针移动179
                                        iSearch= iSearch + 179;
                                        dataIdx = dataLen - iSearch;//还剩余需要处理的字节数
                                    }else{//收到的数据采样长度不够,跳出循环，进一步接收数据后再处理
                                        dataIdx = dataLen - iSearch;
                                        iSearch = dataLen;//将i设定一个大值，跳出循环
                                    }
                                }
                                else{//没有找到固定帧头字节，向后挪1个字节
                                    iSearch++;
                                    dataIdx = dataLen - iSearch;
                                }
                            }
                        }
                        if(dataIdx != 0 && dataIdx != dataLen){//当前的dataId个数据未处理完，保留到下帧数据合并处理
                            System.arraycopy(dataReceived,dataLen-dataIdx,buffer,0,dataIdx);
                            System.arraycopy(buffer,0,dataReceived,0,dataIdx);
                        }
                    }

                    if((idMsg == 2) &&(dataLen > 0)){
                        dataIdx = dataLen;
                        powerIdx = 0;//血压测量时没有电源剩余数值，置0，不显示
                        if(dataLen >=  6){
                            iSearch = 0;
                            while(iSearch <= dataLen - 5) {
                                if ((dataReceived[iSearch] == (byte) 0xf0) && (dataReceived[iSearch + 1] == (byte) 0xc0) && (dataReceived[iSearch + 2] == (byte) 0x04) && (dataReceived[iSearch + 4] == (byte) 0xac)) {
                                //if ((dataReceived[iSearch] == (byte) 0xff) && (dataReceived[iSearch + 1] == (byte) 0xc0) && (dataReceived[iSearch + 2] == (byte) 0x04) && (dataReceived[iSearch + 4] == (byte) 0xad)) {
                                    h1 = dataReceived[iSearch + 5];
                                    String errInfo = "测量有误";
                                    switch (h1){
                                        case 1:
                                            errInfo = "!袖袋太松";
                                            break;
                                        case 3:
                                            errInfo = "!袖袋太紧";
                                            break;
                                        case 0:
                                            errInfo = "!无脉搏";
                                    }
                                    xy = new SpannableString("血压\n" + String.valueOf(h1)+":"+errInfo);
                                    xy_msg.post(new Runnable() {
                                        @Override
                                        public void run() {
                                            //xy.setSpan(new AbsoluteSizeSpan(size,true), 0, 3, SPAN_INCLUSIVE_INCLUSIVE);
                                            xy_msg.setText(xy);
                                        }
                                    });
                                }
                                iSearch++;
                            }
                        }
                        if(dataLen >= 10){
                            iSearch = 0;
                            while(iSearch <= dataLen - 5){
                                if((dataReceived[iSearch] == (byte)0xf0) && (dataReceived[iSearch+1] == (byte)0xc0) && (dataReceived[iSearch+2] == (byte)0x08) && (dataReceived[iSearch+4] == (byte)0xab)){
                                //if ((dataReceived[iSearch] == (byte) 0xff) && (dataReceived[iSearch + 1] == (byte) 0xc0) && (dataReceived[iSearch + 2] == (byte) 0x08) && (dataReceived[iSearch + 4] == (byte) 0xac)) {

                                    if(dataLen - iSearch >= 10){
                                        h1 = dataReceived[iSearch+6];
                                        h2 = dataReceived[iSearch+5];
                                        if (h1 < 0) {
                                            h1 = h1 + 256;
                                        }
                                        if (h2 < 0) {
                                            h2 = h2 + 256;
                                        }
                                        h1 = h1 + h2*256;
                                        xy = new SpannableString("血压：" + String.valueOf(h1));
                                        h1 = dataReceived[iSearch+8];
                                        h2 = dataReceived[iSearch+7];
                                        if (h1 < 0) {
                                            h1 = h1 + 256;
                                        }
                                        if (h2 < 0) {
                                            h2 = h2 + 256;
                                        }
                                        h1 = h1 + h2;
                                        xy = new SpannableString(xy.toString()+"/" + String.valueOf(h1));

                                        h1 = dataReceived[iSearch+9];
                                        xy = new SpannableString(xy.toString() + "\n心率：" + String.valueOf(h1));
                                        xy_msg.post(new Runnable() {
                                            @Override
                                            public void run() {
                                                xy_msg.setTextSize(size);
                                                //xy.setSpan(new AbsoluteSizeSpan(size,true), 0, 3, SPAN_INCLUSIVE_INCLUSIVE);
                                                xy_msg.setText(xy);
                                            }
                                        });
                                        //本次数据包处理完毕后，i指针移动10
                                        iSearch= iSearch + 10;
                                        dataIdx = dataLen - iSearch;//还剩余需要处理的字节数
                                    } else{
                                        //收到的数据采样长度不够,跳出循环，进一步接收数据后再处理
                                        dataIdx = dataLen - iSearch;
                                        iSearch = dataLen;//将i设定一个大值，跳出循环
                                    }
                                }else{
                                    //没有找到固定帧头字节，向后挪1个字节
                                    iSearch++;
                                    dataIdx = dataLen - iSearch;
                                }
                            }
                        }
                        if(dataIdx != 0 && dataIdx != dataLen){//当前的dataId个数据未处理完，保留到下帧数据合并处理
                            System.arraycopy(dataReceived,dataLen-dataIdx,buffer,0,dataIdx);
                            System.arraycopy(buffer,0,dataReceived,0,dataIdx);
                        }
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
                text_state.post(new Runnable() {
                    @Override
                    public void run() {
                        text_state.setText(getResources().getString(R.string.connect_error));
                    }
                });
                if(mSocket != null){
                    try{
                        mSocket.close();
                        mSocket = null;
                    }catch (IOException eSocket){
                        eSocket.printStackTrace();
                    }
                }
                dp_msg.post(new Runnable() {
                    @Override
                    public void run() {
                        dp_msg.setText("请打开采样器蓝牙并重新启动测量！");
                    }
                });
            }
        }

        /**
         * 发送数据
         *
         * @param msg
         */
        public void sendMsg(final byte[] msg) {

            //byte[] bytes = msg.getBytes();
            if (outputStream != null) {
                try {
                    //发送数据
                    outputStream.write(msg);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
    public void dataValueReset(){
        double a = 0, z = 0;
        for (int j = 0; j < iMaxPoints; j++) {
            a = a + 1;
            z = 0;
            values1[j] = new DataPoint(a, z);
            values2[j] = new DataPoint(a, z);
            values3[j] = new DataPoint(a, z);
        }
        if(hrCounter != iMaxPoints){
            hrCounter = 0;
        }

        if(ecgCounter != iMaxPoints){
            ecgCounter = 0;
        }
    }
    public  void ds(int hv,int c) {
        double a = 0;
        double z = 0;
        switch (c){
            case 1:
                idxV1 = idxV1%iMaxPoints;
                if(idxV1 == 0){
                    for (int j = 0; j < iMaxPoints; j++) {
                        a = a + 1;
                        z = 0;
                        values1[j] = new DataPoint(a, z);
                    }
                }
                a = idxV1+1;
                z = hv;
                values1[idxV1] = new DataPoint(a,z);
                idxV1++;
                break;
            case 2:
                idxV2 = idxV2%iMaxPoints;
                if(idxV2 == 0){
                    for (int j = 0; j < iMaxPoints; j++) {
                        a = a + 1;
                        z = 0;
                        values2[j] = new DataPoint(a, z);
                    }
                }
                a = idxV2+1;
                z = hv;
                values2[idxV2] = new DataPoint(a,z);
                idxV2++;
                break;
            case 3:
                idxV3 = idxV3%iMaxPoints;
                if(idxV3 == 0){
                    for (int j = 0; j < iMaxPoints; j++) {
                        a = a + 1;
                        z = 0;
                        values3[j] = new DataPoint(a, z);
                    }
                }
                a = idxV3+1;
                z = hv;
                values3[idxV3] = new DataPoint(a,z);
                idxV3++;
                break;

        }
        if(c == 3) {
            if (ecgCounter <= iMaxPoints - 1) {
                dEcg[ecgCounter] = hv;
                ecgCounter = ecgCounter + 1;
            }
        }
        if(c == 1) {
            if (hrCounter <= iMaxPoints - 1) {
                dPcg[hrCounter] = hv;
                hrCounter = hrCounter + 1;
            }
        }
    }
    /**
     * 监听线程
     */
    private class ListenerThread extends Thread {

        private BluetoothServerSocket serverSocket;
        private BluetoothSocket socket;

        @Override
        public void run() {
            try {
                serverSocket = bTAdatper.listenUsingRfcommWithServiceRecord(
                        NAME, BT_UUID);
                while (true) {
                    //线程阻塞，等待别的设备连接
                    socket = serverSocket.accept();
                    text_state.post(new Runnable() {
                        @Override
                        public void run() {
                            text_state.setText(getResources().getString(R.string.connecting));
                        }
                    });
                    connectThread = new ConnectThread(socket, false);
                    connectThread.start();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
    private boolean isFolderExists(String strFolder) {
        File file = new File(strFolder);
        if (!file.exists()) {
            if (file.mkdirs()) {
                return true;
            } else {
                return false;

            }
        }
        return true;

    }
    private boolean createOutputFile() {
        currentFile = "";
        try {
            Calendar now = Calendar.getInstance();
            String filename = "" + now.get(Calendar.YEAR);
            if((now.get(Calendar.MONTH)+1)<10) {
                filename = filename + "0"+(now.get(Calendar.MONTH) + 1);
            }else{
                filename = filename +(now.get(Calendar.MONTH) + 1);
            }
            if((now.get(Calendar.DAY_OF_MONTH)<10)) {
                filename = filename + "0"+now.get(Calendar.DAY_OF_MONTH);
            }else{
                filename = filename +now.get(Calendar.DAY_OF_MONTH);
            }
            if((now.get(Calendar.HOUR_OF_DAY)<10)) {
                filename = filename + "0"+now.get(Calendar.HOUR_OF_DAY);
            }else{
                filename = filename +now.get(Calendar.HOUR_OF_DAY);
            }
            if((now.get(Calendar.MINUTE)<10)) {
                filename = filename + "0"+now.get(Calendar.MINUTE);
            }else{
                filename = filename +now.get(Calendar.MINUTE);
            }
            if((now.get(Calendar.SECOND)<10)) {
                filename = filename + "0"+now.get(Calendar.SECOND);
            }else{
                filename = filename +now.get(Calendar.SECOND);
            }
            filename = filename + "."+text_name.getText().toString()+".hrt";//text_name.getText().toString()
            dp_msg.setText("采样文件："+filename);

            fileName = new String(filename);

            filename = DIR_PATH + filename;
            currentFile = currentFile + filename;

            mStream = new FileOutputStream(new File(filename));
            return true;
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        return false;
    }
    Runnable networkTask = new Runnable() {

        @Override
        public void run() {
            // TODO
            // 在这里进行 http request.网络请求相关操作
            dp_msg.post(new Runnable() {
                @Override
                public void run() {
                    dp_msg.setText(dp_msg.getText() +"-上传...");
                }
            });
            chkFlag = 99;
            String s = "分析->";
            byte[] audio = s.getBytes();
            Message msg = new Message();
            Bundle data = new Bundle();
            FileInputStream inStream = null;
            int size = 1024;
            try {
                inStream = new FileInputStream(currentFile);
            }catch (FileNotFoundException e) {
                e.printStackTrace();
            };
            if(inStream == null){
                chkFlag = 0;
                dp_msg.post(new Runnable() {
                    @Override
                    public void run() {
                        dp_msg.setText(currentFile +"-文件不存在...");
                    }
                });

                return;
            }
            try {
                size = inStream.available();
            }catch(IOException e){
                e.printStackTrace();
            }

            byte[] audio1 = new byte[size];
            try {
                inStream.read(audio1);
            } catch (IOException e) {
                e.printStackTrace();
            }
            System.out.print(currentFile);
            System.out.print(String.valueOf(size));

            AudioPayload payload = new AudioPayload(currentFile, Base64.encodeToString(audio1, Base64.DEFAULT));
            //AudioPayload payload = new AudioPayload("audio", b64encode(audio1));
            String ipA = "http://"+ipAddr+":80/";
            AudioReader ar = new AudioReader(ipA);

            Map<String, String> map = null;
            try {
                map = ar.readAudio(payload);
            } catch (Exception e) {
                e.printStackTrace();
            }
            System.out.print(map);
            System.out.println("通过Map.keySet遍历key和value：");
            if(map != null) {
                for (String key : map.keySet()) {
                    System.out.println("key= "+ key + " and value= " + map.get(key));
                    s =  s + "\n" + key + map.get(key);
                }
            }else{
                s = s + "请重新上传数据...";
            }
            data.putString("result", s);
            msg.setData(data);
            handler.sendMessage(msg);
            chkFlag = 0;
        }
    };
}

