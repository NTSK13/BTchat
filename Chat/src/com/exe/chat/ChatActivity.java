package com.exe.chat;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;

import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
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
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;


public class ChatActivity extends Activity {

	private String tag="wei";
	private BluetoothAdapter myBtAdapter;
	private String string_uuid="00001101-0000-1000-8000-00805F9B34FB";
	private BluetoothSocket server_socket; 
	private BluetoothSocket client_socket; 
	
	private ListView lv;
	private Button bsend_msg;
	private Button bRequest_connect;
	private Button bWait_connect;
	private EditText et_send_msg;
	

    private final int request_visibility_item_id=0x100;

    private final int client_ready2=0x01;
    private final int client_msg_get=0x02;
    private final int client_ready=0x03;
    private final int server_msg_get=0x12;
    private final int server_ready=0x13;
    
    private String send_msg;
    private boolean is_client=false;
	private boolean is_from_search_device=false;
	
    private Thread serverThread;
    private Thread clientThread;
	private Timer myTimer=null;
    private ChatThread ServerChatThread;
    private ChatThread ClientChatThread;
    
    private ChatMsgEntity Myentity = new ChatMsgEntity();
    private ChatMsgEntity Yourentity = new ChatMsgEntity();
    List<ChatMsgEntity> mDataArrays = new ArrayList<ChatMsgEntity>();
    private String my_device_name;
    private String your_device_name;  
    
    ChatMsgViewAdapter mListViewAdapter;

    
	public ArrayList<BluetoothDevice>  find_device_list;
	public ArrayList<BluetoothDevice>  bonded_device_list;
	private ArrayAdapter<String> find_device_name_adapter;
	private ArrayAdapter<String> bonded_device_name_adapter;
	private Set<BluetoothDevice> pairedDevices;
	public BluetoothDevice target_device=null;
    
	
    private Handler chat_handler=new Handler(){

		@Override
		public void handleMessage(Message msg) {
			// TODO Auto-generated method stub
			//super.handleMessage(msg);
			
			switch (msg.what) {
				case client_msg_get:
					Log.v(tag, "client  get msg");
	  				Bundle b=msg.getData();
	  				String client_get_msg=b.getString("get_msg");
	  				 Yourentity.setDate(Yourentity.getDate());
				     Yourentity.setText(client_get_msg);
				     Yourentity.setName(your_device_name);
				     Yourentity.setMsgType(true);
				     mDataArrays.add(Yourentity);
				     
				     mListViewAdapter.notifyDataSetChanged();
				     lv.setSelection(mListViewAdapter.getCount() - 1);
	  				
					break;
					
				case client_ready:
					if( is_from_search_device==true){
	   					is_from_search_device=false;
	   					myTimer.cancel();
	   				}

	   				//启动客户端线程
	                clientThread=new Thread(new Runnable(){
	            		@Override
	            		public void run() {
	            			Log.v(tag, "clientThread run ");
	            			try {
	            				client_socket =target_device.createRfcommSocketToServiceRecord(UUID.fromString(string_uuid));
	            				Log.v(tag, "clientThread wait connect ");
	            				client_socket.connect();
	            					
	            				Log.v(tag, "clientThread  connected ");
	            				/*if(current_socket !=null){
	            					Log.v(tag, "client socket close");
	            					current_socket.close();
	            				}*/
	            				
	            				chat_handler.sendEmptyMessage(client_ready2);
	              					
	            			} catch (Exception e) {
	            				e.printStackTrace();
	            			}	
	            		};
	            	});
	            	clientThread.start();	
	            	break;
				case client_ready2:
					
	            	ClientChatThread =new ChatThread(client_socket, chat_handler,true);
    				ClientChatThread.start();
    				
    				lv.setVisibility(View.VISIBLE);
		            bsend_msg.setVisibility(View.VISIBLE);
		            et_send_msg.setVisibility(View.VISIBLE);
		            ClientChatThread.write(my_device_name+"#fclientname#");
    			    
					break;
				case server_msg_get:
					Bundle bd=msg.getData();
					String server_get_msg=bd.getString("get_msg");
					
					if(server_get_msg.endsWith("#fclientname#")){
						your_device_name=server_get_msg.split("#")[0];
						break;
					}
					
					 Yourentity.setDate(Yourentity.getDate());
				     Yourentity.setText(server_get_msg);
				     Yourentity.setName(your_device_name);
				     Yourentity.setMsgType(true);
				     mDataArrays.add(Yourentity);
				     
				     mListViewAdapter.notifyDataSetChanged();
				     lv.setSelection(mListViewAdapter.getCount() - 1);
	
					break;
					
				case server_ready:
					ServerChatThread =new ChatThread(server_socket, chat_handler,false);
		    		ServerChatThread.start();
		    		
		    		lv.setVisibility(View.VISIBLE);
		            bsend_msg.setVisibility(View.VISIBLE);
		            et_send_msg.setVisibility(View.VISIBLE);
		            
					break;
				default:
				break;
			}
		}
    	
    };
	
    public void send_msg_handler(View v){
    	send_msg=et_send_msg.getText().toString().trim();
		
        Myentity.setDate(Myentity.getDate());
        Myentity.setText(send_msg);
        Myentity.setName("我-"+my_device_name);
        Myentity.setMsgType(false);
        mDataArrays.add(Myentity);
        
		if(is_client){
			ClientChatThread.write(send_msg);
		}else{
			ServerChatThread.write(send_msg);

		}
		et_send_msg.setText("");
    }
    
    public void request_connect_handler(View v){
    	
    	Toast.makeText(this, "request connect others device", Toast.LENGTH_LONG).show();
    	is_client=true;
    	init();
    	
    	 //start chat
        get_Bonded_device_and_chat();
        
    	bRequest_connect.setVisibility(View.GONE);
    	bWait_connect.setVisibility(View.GONE);
    }
         
    public void wait_connect_handler(View v){
    	Toast.makeText(this, "wait others device request", Toast.LENGTH_LONG).show();
    	enable_my_device_visible();
    	is_client=false;
    	//开启服务端线程
   	    serverThread=new Thread(new Runnable(){
    	   	@Override
    	    public void run() {
    	    	try {
    	    		Log.v(tag, "serverThread run ");
    	    		BluetoothServerSocket server_bt_socket=myBtAdapter.listenUsingRfcommWithServiceRecord("myServerSocket", UUID.fromString(string_uuid));
    	    		Log.v(tag, "wait client request");
    	    		    	    		
    	    		server_socket = server_bt_socket.accept();  
    	    		Log.v(tag, "accepted  client request");
    	    		chat_handler.sendEmptyMessage(server_ready);
    	    				
    	    	} catch (Exception e) {
    				e.printStackTrace();
    			}
    	    }
    	});
    	serverThread.start();  
    	
    	bRequest_connect.setVisibility(View.GONE);
    	bWait_connect.setVisibility(View.GONE);
    }
            
            
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);
        
        lv=(ListView)findViewById(R.id.chat_list_view);
        bsend_msg=(Button)findViewById(R.id.btn_send);
        et_send_msg=(EditText)findViewById(R.id.et_sendmsg);
        bRequest_connect=(Button)findViewById(R.id.request_connect);
    	bWait_connect=(Button)findViewById(R.id.wait_connect);
        
        lv.setVisibility(View.INVISIBLE);
        bsend_msg.setVisibility(View.INVISIBLE);
        et_send_msg.setVisibility(View.INVISIBLE);
         
          
        //enable BT 
        myBtAdapter=BluetoothAdapter.getDefaultAdapter();//get local BT adapter
        my_device_name=myBtAdapter.getName();
        enable_my_device();
        
        mListViewAdapter = new ChatMsgViewAdapter(this, mDataArrays );
        lv.setAdapter(mListViewAdapter);
        register_myBTReceiver();
    }


    private void  enable_my_device(){

         try {
 			Log.v(tag, "Get BT adapter");
 			if(! myBtAdapter.isEnabled()){// 如果没有打开蓝牙,那么打开蓝牙
 				//弹出对话框,提示用户打开蓝牙
 				//Intent it =new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
 				//startActivity(it);
 				myBtAdapter.enable();//无通知直接打开蓝牙
 			}	
 			
 		} catch (Exception e) {
 			Log.e(tag, "My device has no BT adapter.");
 			e.printStackTrace();
 		}
    }
    
    private void enable_my_device_visible(){
    	Log.v(tag, "open Bt visible");
    	//可见120s
    	Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
    	discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 120);
    	startActivity(discoverableIntent);
    }
    
    private void register_myBTReceiver(){
    	/***********************注册BT广播接收器*************************/
        IntentFilter filter=new IntentFilter(BluetoothDevice.ACTION_FOUND);
        registerReceiver(mReceiver, filter);
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
        			if(! device.getName().equals(null)){
        				find_device_list.add(device);
        				find_device_name_adapter.add(device.getName());  
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
		Log.v(tag, "onDestroy start");
		if(mReceiver !=null){
			unregisterReceiver(mReceiver);
		}
	}

    private void init(){

   	 find_device_list=new ArrayList<BluetoothDevice>();
     bonded_device_list=new ArrayList<BluetoothDevice>();
     bonded_device_name_adapter=new ArrayAdapter<String>(this, android.R.layout.select_dialog_singlechoice);
     
   }
    
    private void search_device_and_chat(){
    	Log.v(tag, "start search...");
        myBtAdapter.startDiscovery();//search
        Toast.makeText(this, "搜索蓝牙设备中 ...", Toast.LENGTH_LONG).show();
    	
        find_device_name_adapter=new ArrayAdapter<String>(this, android.R.layout.select_dialog_singlechoice);
        AlertDialog.Builder myDialog=new AlertDialog.Builder(this);
        myDialog.setTitle("BT搜索结果");
        myDialog.setCancelable(false);
        
        myDialog.setNegativeButton("取消", new OnClickListener(){
			@Override
			public void onClick(DialogInterface arg0, int arg1) {
				Toast.makeText(ChatActivity.this, "取消", Toast.LENGTH_SHORT).show();				
			}
        });
        
        myDialog.setPositiveButton("确定", new OnClickListener() {
			@Override
			public void onClick(DialogInterface arg0, int arg1) {
				Toast.makeText(ChatActivity.this, "确定", Toast.LENGTH_SHORT).show();
                Log.v(tag, "pairing ...");
    			target_device.createBond();
    			myTimer=new Timer();
    			myTimer.schedule(new TimerTask() {
					@Override
					public void run() {
						if(target_device.getBondState()==BluetoothDevice.BOND_BONDED){
							is_from_search_device=true;
							chat_handler.sendEmptyMessage(client_ready);
						}
					}
				}, 0,100);
			}
		});
        
        myDialog.setSingleChoiceItems(find_device_name_adapter, 0, new OnClickListener() {
			@Override
			public void onClick(DialogInterface arg0, int position) {
				//取消蓝牙扫描
            	myBtAdapter.cancelDiscovery();
				Toast.makeText(ChatActivity.this, "选择: "+position, Toast.LENGTH_SHORT).show();
				Log.v(tag, "get target_device position is: "+position);
            	target_device=(BluetoothDevice)find_device_list.get(position);
            	your_device_name=target_device.getName();
            	
			}
		});
        myDialog.show();       
    }
    
    private void  get_Bonded_device_and_chat(){
    	 pairedDevices = myBtAdapter.getBondedDevices(); 
         if (pairedDevices.size() > 0) { 
         	for (BluetoothDevice device : pairedDevices) { 
         		bonded_device_name_adapter.add(device.getName());
         		bonded_device_list.add(device);
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
 				Toast.makeText(ChatActivity.this, "确定", Toast.LENGTH_SHORT).show();
 				chat_handler.sendEmptyMessage(client_ready);
 			}
 		});
         
         myBondedDialog.setSingleChoiceItems(bonded_device_name_adapter, 0, new OnClickListener() {
 			@Override
 			public void onClick(DialogInterface arg0, int position) {
 				Toast.makeText(ChatActivity.this, "选择: "+position, Toast.LENGTH_SHORT).show();
 				Log.v(tag, "get target_device position is: "+position);
             	target_device=(BluetoothDevice) bonded_device_list.get(position);
             	Log.v(tag, "target_device name is: "+target_device.getName());
               	your_device_name=target_device.getName();
 			}
 		});
         myBondedDialog.show(); 
    }
    
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.chat, menu);
        menu.add(0, request_visibility_item_id, 0, "打开设备可见性");

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
       
        if(id==request_visibility_item_id){
        	enable_my_device_visible();
        }

        return super.onOptionsItemSelected(item);
    }
}
