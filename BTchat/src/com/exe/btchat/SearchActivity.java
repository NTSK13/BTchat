package com.exe.btchat;

import java.util.ArrayList;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;

import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;


public class SearchActivity extends Activity {

	private String tag="wei";
	private BluetoothAdapter myBtAdapter;
	private String string_uuid="00001101-0000-1000-8000-00805F9B34FB";
	
	public ArrayList<BluetoothDevice>  BtDeviceList;
	public ArrayList<BluetoothDevice>  BtBondedDeviceList;
	private ArrayAdapter<String> BT_name_adapter;
	private ArrayAdapter<String> BT_Bonded_name_adapter;
	private Set<BluetoothDevice> pairedDevices;
	
	public int BTcount;
	public BluetoothDevice target_device=null;

	private ChatThread ClientChatThread;
	private TextView tv_get_msg;
	private EditText et_send_msg;
	private Timer myTimer=null;
	private boolean is_from_search_device=false;
    
    public void SendMsgHandler(View v){
  		String msg=et_send_msg.getText().toString();
  		ClientChatThread.write(msg);
  		et_send_msg.setText("");
  	}
  	
  	private Handler ClientHandler=new Handler(){
      	@Override
      	public void handleMessage(Message msg) {		
      		if(msg.what==0x02) {
  				Log.v(tag, "client  get msg");
  				Bundle b=msg.getData();
  				String client_get_msg=b.getString("get_msg");
  				tv_get_msg.setText(client_get_msg);
   			}else if(msg.what==0x01){
   				if( is_from_search_device==true){
   					is_from_search_device=false;
   					myTimer.cancel();
   				}
   				//启动客户端线程
                Thread clientThread=new Thread(new Runnable(){
            		@Override
            		public void run() {
            			Log.v(tag, "clientThread run ");
            			try {
            				BluetoothSocket client_socket =target_device.createRfcommSocketToServiceRecord(UUID.fromString(string_uuid));
            				Log.v(tag, "clientThread wait connect ");
            				client_socket.connect();
            					
            				Log.v(tag, "clientThread  connected ");
            				/*if(current_socket !=null){
            					Log.v(tag, "client socket close");
            					current_socket.close();
            				}*/
            				ClientChatThread =new ChatThread(client_socket, ClientHandler,true);
            				ClientChatThread.start();
              					
            			} catch (Exception e) {
            				e.printStackTrace();
            			}	
            		};
            	});
            	clientThread.start();	
   			}
      	};
      };
      
      
    private void init(){
    	 BtDeviceList=new ArrayList<BluetoothDevice>();
         BtBondedDeviceList=new ArrayList<BluetoothDevice>();
         BT_Bonded_name_adapter=new ArrayAdapter<String>(SearchActivity.this, android.R.layout.select_dialog_singlechoice);
         myBtAdapter=BluetoothAdapter.getDefaultAdapter();//get local BT adapter
    }
    
    private void register_myBTReceiver(){
    	/***********************注册BT广播接收器*************************/
        IntentFilter filter=new IntentFilter(BluetoothDevice.ACTION_FOUND);
        registerReceiver(mReceiver, filter);
    }
    
    private void search_device_and_chat(){
    	Log.v(tag, "start search...");
        myBtAdapter.startDiscovery();//search
        Toast.makeText(SearchActivity.this, "搜索蓝牙设备中 ...", Toast.LENGTH_LONG).show();
    	
        BT_name_adapter=new ArrayAdapter<String>(SearchActivity.this, android.R.layout.select_dialog_singlechoice);
        AlertDialog.Builder myDialog=new AlertDialog.Builder(SearchActivity.this);
        myDialog.setTitle("BT搜索结果");
        myDialog.setCancelable(false);
        
        myDialog.setNegativeButton("取消", new OnClickListener(){
			@Override
			public void onClick(DialogInterface arg0, int arg1) {
				Toast.makeText(SearchActivity.this, "取消", Toast.LENGTH_SHORT).show();				
			}
        });
        
        myDialog.setPositiveButton("确定", new OnClickListener() {
			@Override
			public void onClick(DialogInterface arg0, int arg1) {
				Toast.makeText(SearchActivity.this, "确定", Toast.LENGTH_SHORT).show();
                Log.v(tag, "pairing ...");
    			target_device.createBond();
    			myTimer=new Timer();
    			myTimer.schedule(new TimerTask() {
					@Override
					public void run() {
						if(target_device.getBondState()==BluetoothDevice.BOND_BONDED){
							is_from_search_device=true;
							ClientHandler.sendEmptyMessage(0x01);
						}
					}
				}, 0,100);
			}
		});
        
        myDialog.setSingleChoiceItems(BT_name_adapter, 0, new OnClickListener() {
			@Override
			public void onClick(DialogInterface arg0, int position) {
				//取消蓝牙扫描
            	myBtAdapter.cancelDiscovery();
				Toast.makeText(SearchActivity.this, "选择: "+position, Toast.LENGTH_SHORT).show();
				Log.v(tag, "get target_device position is: "+position);
            	target_device=(BluetoothDevice)BtDeviceList.get(position);
            	
			}
		});
        myDialog.show();       
    }
    
    private void  get_Bonded_device_and_chat(){
    	 pairedDevices = myBtAdapter.getBondedDevices(); 
         if (pairedDevices.size() > 0) { 
         	for (BluetoothDevice device : pairedDevices) { 
         		BT_Bonded_name_adapter.add(device.getName());
         		BtBondedDeviceList.add(device);
         	 } 
         }
         
         AlertDialog.Builder myBondedDialog=new AlertDialog.Builder(this);
         myBondedDialog.setTitle("已绑定设备列表");
         myBondedDialog.setCancelable(false);
         
         myBondedDialog.setNegativeButton("扫描其他设备", new OnClickListener(){
 			@Override
 			public void onClick(DialogInterface arg0, int arg1) {
 				search_device_and_chat();
 			}
         });
         
         myBondedDialog.setPositiveButton("开始聊天", new OnClickListener() {
 			@Override
 			public void onClick(DialogInterface arg0, int arg1) {
 				Toast.makeText(SearchActivity.this, "确定", Toast.LENGTH_SHORT).show();
                ClientHandler.sendEmptyMessage(0x01);
 			}
 		});
         
         myBondedDialog.setSingleChoiceItems(BT_Bonded_name_adapter, 0, new OnClickListener() {
 			@Override
 			public void onClick(DialogInterface arg0, int position) {
 				Toast.makeText(SearchActivity.this, "选择: "+position, Toast.LENGTH_SHORT).show();
 				Log.v(tag, "get target_device position is: "+position);
             	target_device=(BluetoothDevice) BtBondedDeviceList.get(position);
             	Log.v(tag, "target_device name is: "+target_device.getName());
 			}
 		});
         myBondedDialog.show(); 
    }
	
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_client_chat);

    	et_send_msg=(EditText)findViewById(R.id.et_send_msg);
        tv_get_msg=(TextView)findViewById(R.id.tv_get_msg);
        init();
        register_myBTReceiver();
        //start chat
        get_Bonded_device_and_chat();
    }
 
    
    public  BroadcastReceiver mReceiver=new BroadcastReceiver(){
    	@Override
    	public void onReceive(android.content.Context mContext, Intent it) {
    		try {
    			String action=it.getAction();
        		if(BluetoothDevice.ACTION_FOUND.equals(action)  ){
        			BluetoothDevice device=it.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
        			//if(device.getBondState() != BluetoothDevice.BOND_BONDED){
        			Log.v(tag, "Find device: "+device.getName()+",Device address is :"+device.getAddress() );
        			if(! device.getName().equals("")){
        				BtDeviceList.add(device);
        				BT_name_adapter.add(device.getName());  
        			}
        		}	
			} catch (Exception e) {
				e.printStackTrace();
			}
    	};
    };
    
    
	@Override
	protected void onDestroy() {
		// TODO Auto-generated method stub
		super.onDestroy();
		unregisterReceiver(mReceiver);
	}

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.init, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}


