package cn.haoqiabin.pet;

import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import cn.haoqiabin.pet.bluetooth.BluetoothChatService;
import cn.haoqiabin.pet.bluetooth.DeviceListActivity;
import cn.haoqiabin.pet.rocker.RockerView;
import cn.haoqiabin.pet.utils.PreferenceUtil;

import static cn.haoqiabin.pet.bluetooth.BluetoothChatService.DEVICE_NAME;
import static cn.haoqiabin.pet.bluetooth.BluetoothChatService.MESSAGE_DEVICE_NAME;
import static cn.haoqiabin.pet.bluetooth.BluetoothChatService.MESSAGE_READ;
import static cn.haoqiabin.pet.bluetooth.BluetoothChatService.MESSAGE_STATE_CHANGE;
import static cn.haoqiabin.pet.bluetooth.BluetoothChatService.MESSAGE_TOAST;
import static cn.haoqiabin.pet.bluetooth.BluetoothChatService.MESSAGE_WRITE;
import static cn.haoqiabin.pet.bluetooth.BluetoothChatService.TOAST;

public class MainActivity extends Activity {

    // Intent 请求码
    private static final int REQUEST_CONNECT_DEVICE = 1;
    private static final int REQUEST_ENABLE_BT = 2;
    // 本地蓝牙适配器
    private BluetoothAdapter mBluetoothAdapter = null;
    private BluetoothChatService mChatService = null;
    private String mConnectedDeviceName = null;
    // 摇杆成员变量
    public RockerView rockerView;
    public RockerView rockerViewPad;
    private TextView mLogTip;
    String message;
    // 按钮成员变量
    Button spinCW,spinACW;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        rockerView = (RockerView) findViewById(R.id.rockerView);
        rockerViewPad = (RockerView) findViewById(R.id.rockerView);
        mLogTip = (TextView) findViewById(R.id.tip_info);
        spinCW = (Button) findViewById(R.id.spinCW);
        spinACW = (Button) findViewById(R.id.spinACW);
        initRocker();
        initButton();
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBluetoothAdapter == null) {
            Toast.makeText(this, "手机不支持蓝牙功能", Toast.LENGTH_SHORT).show();
            finish();
            return;
        } else {
            if (!mBluetoothAdapter.isEnabled()) {
                Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
            } else {
                // Initialize the BluetoothChatService to perform bluetooth connections
                mChatService = new BluetoothChatService(this, mHandler);
            }
        }

        /**
         * 遥杆
         */
        if (rockerViewPad != null) {
            rockerViewPad.setCallBackMode(RockerView.CallBackMode.CALL_BACK_MODE_STATE_CHANGE);
            rockerViewPad.setOnShakeListener(RockerView.DirectionMode.DIRECTION_4_ROTATE_45, new RockerView.OnShakeListener() {
                @Override
                public void onStart() {
                }

                @Override
                public void direction(RockerView.Direction direction) {
                    mLogTip.setText("方向 : " + getDirection(direction));
                }

                @Override
                public void onFinish() {
                }
            });
        }



        //跳转到我的个人站
        Button web = (Button) findViewById(R.id.website);
        web.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("http://www.haoqiabin.cn")));
                }
        });

    }

    public void initRocker() {
        rockerView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_POINTER_DOWN:
                        // 回调 开始
                    case MotionEvent.ACTION_MOVE:// 移动
                        float moveX = event.getX();
                        float moveY = event.getY();
                        break;
                    case MotionEvent.ACTION_UP:// 抬起
                        stop();
                    case MotionEvent.ACTION_CANCEL:// 移出区域
                        // 回调 结束
                        float upX = event.getX();
                        float upY = event.getY();
                        break;
                }
                return false;
            }
        });
    }

    /**
     * 遥杆事件
     *
     * @param direction
     * @return
     */
    private String getDirection(RockerView.Direction direction) {
        message = "";
        switch (direction) {
            case DIRECTION_LEFT:
                message = "左";
                sendMessage(PreferenceUtil.getInstance().getLeftCode());
                break;
            case DIRECTION_RIGHT:
                message = "右";
                sendMessage(PreferenceUtil.getInstance().getRightCode());
                break;
            case DIRECTION_UP:
                message = "前";
                sendMessage(PreferenceUtil.getInstance().getUpCode());
                break;
            case DIRECTION_DOWN:
                message = "后";
                sendMessage(PreferenceUtil.getInstance().getDownCode());
                break;
            default:
                break;
        }
        return message;
    }


    public void stop() {
        sendMessage(PreferenceUtil.getInstance().getStopCode());
    }

    //初始化按钮
    public void initButton(){
        spinCW.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                int action = motionEvent.getAction();
                switch (action) {
                    case MotionEvent.ACTION_DOWN:
                        sendMessage("6");
                        break;
                    case MotionEvent.ACTION_UP:
                        stop();
                        break;
                }
                return true;
            }
        });

        spinACW.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                int action = motionEvent.getAction();
                switch (action) {
                    case MotionEvent.ACTION_DOWN:
                        sendMessage("5");
                        break;
                    case MotionEvent.ACTION_UP:
                        stop();
                        break;
                }
                return true;
            }
        });
    }


    @Override
    protected void onResume() {
        super.onResume();
        if (mChatService != null && mBluetoothAdapter.isEnabled()) {
            // Only if the state is STATE_NONE, do we know that we haven't started already
            if (mChatService.getState() == BluetoothChatService.STATE_NONE) {
                mChatService.start();
            }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mChatService != null) mChatService.stop();
        //  注释这段代码应用退出蓝牙不关闭
        //if (mBluetoothAdapter.isEnabled()) {
        //    mBluetoothAdapter.disable();
        //}
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_CONNECT_DEVICE:
                if (resultCode == Activity.RESULT_OK) {
                    String address = data.getExtras().getString(DeviceListActivity.EXTRA_DEVICE_ADDRESS);
                    BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
                    if (mChatService == null) {
                        mChatService = new BluetoothChatService(this, mHandler);
                        mChatService.start();
                    }
                    mChatService.connect(device);
                }
                break;
            case REQUEST_ENABLE_BT:
                if (resultCode == Activity.RESULT_OK) {
                    Toast.makeText(this, R.string.bt_enabled_success, Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, R.string.bt_not_enabled_leaving, Toast.LENGTH_SHORT).show();
                    finish();
                }
                break;
        }
    }

    //创建菜单
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu, menu);
        return true;
    }

    //菜单选项
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_connect:
                // 启动 DeviceListActivity 查看或搜索蓝牙设备
                Intent serverIntent = new Intent(this, DeviceListActivity.class);
                startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE);
                return true;
            case R.id.setting:
                // 启动 CodeSettingActivity 进行自定义编码
                Intent CodeIntent = new Intent(this, CodeSettingActivity.class);
                startActivity(CodeIntent);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }


    /**
     * Sends a message.
     *
     * @param message A string of text to send.
     */
    private void sendMessage(String message) {
        // Check that we're actually connected before trying anything
        if (mChatService == null || (mChatService.getState() != BluetoothChatService.STATE_CONNECTED)) {
            Toast.makeText(this, R.string.not_connected, Toast.LENGTH_SHORT).show();
            return;
        }
        // Check that there's actually something to send
        if (message.length() > 0) {
            byte[] send = message.getBytes();
            mChatService.write(send);
        }
    }


    //以id为变量传值到ActionBar状态栏
    private final void setStatus(int resId) {
        final ActionBar actionBar = getActionBar();
        actionBar.setSubtitle(resId);
    }

    //以字符串为变量传值到ActionBar状态栏
    private final void setStatus(CharSequence subTitle) {
        final ActionBar actionBar = getActionBar();
        actionBar.setSubtitle(subTitle);
    }

    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MESSAGE_STATE_CHANGE:
                    switch (msg.arg1) {
                        case BluetoothChatService.STATE_CONNECTED:
                            setStatus(getString(R.string.title_connected_to, mConnectedDeviceName));
                            break;
                        case BluetoothChatService.STATE_CONNECTING:
                            setStatus(R.string.title_connecting);
                            //Toast.makeText(getApplicationContext(), "正在连接该蓝牙设备", Toast.LENGTH_SHORT).show();
                            break;
                        case BluetoothChatService.STATE_LISTEN:
                        case BluetoothChatService.STATE_NONE:
                            setStatus(R.string.title_not_connected);
                            break;
                    }
                    break;
                case MESSAGE_WRITE:
                    byte[] writeBuf = (byte[]) msg.obj;
                    // construct a string from the buffer
                    String writeMessage = new String(writeBuf);
                    String result = "";
                    if (writeMessage.equals(PreferenceUtil.getInstance().getStopCode())) {
                        result = "停车";
                    } else if (writeMessage.equals(PreferenceUtil.getInstance().getLeftCode())) {
                        result = "左转";
                    } else if (writeMessage.equals(PreferenceUtil.getInstance().getRightCode())) {
                        result = "右转";
                    } else if (writeMessage.equals(PreferenceUtil.getInstance().getUpCode())) {
                        result = "前进";
                    } else if (writeMessage.equals(PreferenceUtil.getInstance().getDownCode())) {
                        result = "后退";
                    }
                    Log.d("蓝牙小球:", result);
                    break;
                case MESSAGE_READ:
                    byte[] readBuf = (byte[]) msg.obj;
                    // construct a string from the valid bytes in the buffer
                    String readMessage = new String(readBuf, 0, msg.arg1);
                    break;
                case MESSAGE_DEVICE_NAME:
                    // 保存已连接的设备名称
                    mConnectedDeviceName = msg.getData().getString(DEVICE_NAME);
                    Toast.makeText(getApplicationContext(), "连接上 " + mConnectedDeviceName, Toast.LENGTH_SHORT).show();
                    break;
                case MESSAGE_TOAST:
                    Toast.makeText(getApplicationContext(), msg.getData().getString(TOAST), Toast.LENGTH_SHORT).show();
                    break;
            }
        }
    };

    /**
     * 返回键询问是否退出应用
     */
    @Override
    public void onBackPressed() {
        new AlertDialog.Builder(this)
                .setTitle("确定退出这么萌的应用？")
                .setMessage("ˏ₍•ɞ•₎ˎˏ₍•ʚ•₎ˎ")
                .setPositiveButton("确定", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        finish();
                    }
                })
                .setNegativeButton("取消", null)
                .show();
    }
}