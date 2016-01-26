package com.exe.chat;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
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
	BluetoothServerSocket server_bt_socket;
	
	private ListView lv;
	private Button bsend_msg;
	private Button bRequest_connect;
	private Button bWait_connect;
	private EditText et_send_msg;
	private ProgressDialog transfer_progressDiag;
	private long transfer_progressStatus=0;
	private ProgressDialog receive_progressDiag;
	private long receive_progressStatus=0;

    private final int request_visibility_item_id=0x100;
    private final int FB_REQUEST_CODE=0x01;
    private final int FB_RESULT_CODE=0x02;
    private final int client_ready2=0x01;
    private final int client_msg_get=0x02;
    private final int client_ready=0x03;
    private final int server_msg_get=0x12;
    private final int server_ready=0x13;
    private final int rev_file_finished=0x22;
    private final int update_transfer_progress=0x23;
    private final int start_receive_file=0x24;
    private final int update_receive_progress=0x25;
    
    private final int start_transfer_file=0x26;
    private final int ask_if_accept=0x27;
    private final int response_if_accept=0x28;
    
    private String send_msg;
    private boolean is_client=false;
	private boolean is_from_search_device=false;
	
	private String send_file_path;
	private String send_file_name;
	private long send_file_size;
	
    private Thread serverThread;
    private Thread clientThread;
	private Timer myTimer=null;
    private ChatThread ServerChatThread;
    private ChatThread ClientChatThread;
    private BluetoothSocket target_socket;
    
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
	
	private int bonded_item_position=0;
	private boolean bonded_item_selected=false;
	private int founded_item_position=0;
	private boolean founded_item_selected=false;
	
    private Handler chat_handler=new Handler(){

		@Override
		public void handleMessage(Message msg) {
			// TODO Auto-generated method stub
			//super.handleMessage(msg);

			switch (msg.what) {
				case rev_file_finished:
					Bundle b_rev_name=msg.getData();
	  				String rev_name=b_rev_name.getString("rev_name");
					Toast.makeText(ChatActivity.this, "接收完成,文件位于: "+rev_name, Toast.LENGTH_LONG).show();
					break;
				case client_msg_get:
					Log.e(tag, "client  get msg");
	  				Bundle b_client_msg_get=msg.getData();
	  				String client_get_msg=b_client_msg_get.getString("get_msg");
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
	            			Log.e(tag, "clientThread run ");
	            			try {
	            				client_socket =target_device.createRfcommSocketToServiceRecord(UUID.fromString(string_uuid));
	            				Log.e(tag, "clientThread wait connect ");
	            				client_socket.connect();
	            					
	            				Log.e(tag, "clientThread  connected ");
	            				/*if(current_socket !=null){
	            					Log.e(tag, "client socket close");
	            					current_socket.close();
	            				}*/
	            				target_socket=client_socket;
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
				case update_transfer_progress :
					
					Bundle b_transfer_progress=msg.getData();
					transfer_progressStatus=b_transfer_progress.getLong("hasTransfered");
					Log.e(tag,"fileSize : "+send_file_size+" ,transfer_progressStatus : "+transfer_progressStatus);
					transfer_progressDiag.setMax((int)send_file_size);
					
					Log.e(tag,"ChatActivity update transfer progress,transfer_progressStatus= "+transfer_progressStatus);
					if( transfer_progressStatus >= (send_file_size-1000)){
						transfer_progressDiag.dismiss();
						break;
					}
				
					transfer_progressDiag.setProgress((int)transfer_progressStatus  );
					break;
				case start_receive_file:
					receive_progressDiag=new ProgressDialog(ChatActivity.this);
					
					receive_progressDiag.setTitle("接收进度");
					receive_progressDiag.setCancelable(false);
					receive_progressDiag.setIndeterminate(false);
					receive_progressDiag.setMessage("接收完成百分比:");
					receive_progressDiag.setProgress(0);
					receive_progressDiag.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
					receive_progressDiag.show();
					break;
				case update_receive_progress:
					Bundle b_receive_progress=msg.getData();
					receive_progressStatus=b_receive_progress.getLong("hasReceived");
					long receive_file_size=b_receive_progress.getLong("receive_file_size");
					receive_progressDiag.setMax(  (int)receive_file_size);
					Log.e(tag,"ChatActivity fileSize : "+receive_file_size+" ,receive_progressStatus : "+receive_progressStatus);
					//receive_progressStatus=receive_progressStatus *100 / receive_file_size ;
					Log.e(tag,"update receive progress,receive_progressStatus= "+receive_progressStatus);
					if( receive_progressStatus >= ((int)receive_file_size-1000)){
						receive_progressDiag.dismiss();
						break;
					}
				
					receive_progressDiag.setProgress((int)receive_progressStatus  );
					break;
					
				case start_transfer_file:
					transfer_progressDiag.show();
					new Thread(new Runnable() {
						@Override
						public void run() {
							//send file by BT
							if(is_client){
								ClientChatThread.transfer(send_file_path, send_file_name,send_file_size);
							}else{
								ServerChatThread.transfer(send_file_path, send_file_name,send_file_size);
							}
						}
					}).start(); 
					break;
				case ask_if_accept:
					Bundle b_ask_name=msg.getData();
					String ask_name=b_ask_name.getString("ask_name");
					
					AlertDialog.Builder ask_accept_Dialog=new AlertDialog.Builder(ChatActivity.this);
			        ask_accept_Dialog.setTitle("传输请求");
			        ask_accept_Dialog.setMessage("对方想发送"+ask_name+" 给你,是否接收 ?");
			        
			        ask_accept_Dialog.setCancelable(false);
			        
			        ask_accept_Dialog.setNegativeButton("取消", new OnClickListener(){
						@Override
						public void onClick(DialogInterface arg0, int arg1) {
							Toast.makeText(ChatActivity.this, "取消", Toast.LENGTH_SHORT).show();	
							if(is_client){
								ClientChatThread.write("ask_accept@#cancel");
							}else{
								ServerChatThread.write("ask_accept@#cancel");
							}
						}
			        });
			        
			        ask_accept_Dialog.setPositiveButton("接收", new OnClickListener() {
						@Override
						public void onClick(DialogInterface arg0, int arg1) {
							Toast.makeText(ChatActivity.this, "确定接收", Toast.LENGTH_SHORT).show();
							if(is_client){
								ClientChatThread.write("ask_accept@#accept");
							}else{
								ServerChatThread.write("ask_accept@#accept");
							}
						}
					});
			        ask_accept_Dialog.show();     
					
					break;
				case response_if_accept:
					Bundle b_response_if_accept=msg.getData();
					String response_result=b_response_if_accept.getString("response_result");
					if(response_result.equals("accept")){
						chat_handler.sendEmptyMessage(start_transfer_file); 
					}
					break;
				default:
				break;
			}
		}
    	
    };
	
    public void et_send_click_handler(View v){
    	bsend_msg.setBackgroundResource(R.drawable.send);
    }
    
    
    @Override
	protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
		// TODO Auto-generated method stub
		super.onActivityResult(requestCode, resultCode, intent);
		Log.e(tag, "onActivityResult");
				
		transfer_progressDiag=new ProgressDialog(this);
		
		transfer_progressDiag.setTitle("发送进度");
		transfer_progressDiag.setCancelable(false);
		transfer_progressDiag.setIndeterminate(false);
		transfer_progressDiag.setMessage("发送完成百分比:");
		transfer_progressDiag.setProgress(0);
		transfer_progressDiag.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
		
		if(requestCode==FB_REQUEST_CODE && resultCode ==RESULT_OK){
			Log.e(tag, "prepare transfer");
			send_file_name=intent.getStringExtra("fileName");
			send_file_path=intent.getStringExtra("filePath");
			send_file_size=intent.getLongExtra("fileSize", 0);
			
			Log.e(tag, "file_path:"+send_file_path+" , file_name : "+send_file_name);
			// ask opposite side accept file
			String encode_ask_send_file_name=send_file_name+"@#filename@#ask";
			if(is_client){
				ClientChatThread.write(encode_ask_send_file_name);
			}else{
				ServerChatThread.write(encode_ask_send_file_name);
			}

		}else if( resultCode ==RESULT_CANCELED){
			Log.e(tag,"you cancel transfer files");
		}
		
	}


	public void send_msg_handler(View v){
    	send_msg=et_send_msg.getText().toString().trim();
		if(send_msg.equals("")){
			Intent it=new Intent();
			it.setAction("android.intent.action.FileBrowser");
			
			startActivityForResult(it, FB_REQUEST_CODE);	
		}
		
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
		bsend_msg.setBackgroundResource(R.drawable.add);
    }
    
    public void request_connect_handler(View v){
    	is_client=true;
    	init();
    	 //start chat
        get_Bonded_device_and_chat();
        
    	bRequest_connect.setVisibility(View.GONE);
    	bWait_connect.setVisibility(View.GONE);
    }
         
    public void wait_connect_handler(View v){
    	enable_my_device_visible();
    	is_client=false;
    	//开启服务端线程
   	    serverThread=new Thread(new Runnable(){
    	   	@Override
    	    public void run() {
    	    	try {
    	    		Log.e(tag, "serverThread run ");
    	    		server_bt_socket=myBtAdapter.listenUsingRfcommWithServiceRecord("myServerSocket", UUID.fromString(string_uuid));
    	    		Log.e(tag, "wait client request");
    	    		    	    		
    	    		server_socket = server_bt_socket.accept();  
    	    		Log.e(tag, "accepted  client request");
    	    		target_socket=server_socket;
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
 			Log.e(tag, "Get BT adapter");
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
    	Log.e(tag, "enable Bt visible");
    	//可见120s
    	Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
    	discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 120);
    	startActivity(discoverableIntent);
    }
    
    private void register_myBTReceiver(){
    	/***********************注册BT广播接收器************************/
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
        			Log.e(tag, "Find device: "+device.getName()+",Device address is :"+device.getAddress() );
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
		Log.e(tag, "onDestroy start");
		if(mReceiver !=null){
			unregisterReceiver(mReceiver);
		}
		
		if(is_client){
			try {
				client_socket.close();
			
				ClientChatThread.destroy();
			    clientThread.destroy();
			} catch (Exception e) {
				e.printStackTrace();
			}
		      
		}else{
			try {
				server_socket.close(); 
				server_bt_socket.close();
				ServerChatThread.destroy();
			    serverThread.destroy();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		
	}


    private void init(){

   	 find_device_list=new ArrayList<BluetoothDevice>();
     bonded_device_list=new ArrayList<BluetoothDevice>();
     bonded_device_name_adapter=new ArrayAdapter<String>(this, android.R.layout.select_dialog_singlechoice);
     
   }
    
    private void search_device_and_chat(){
    	Log.e(tag, "start search...");
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
				Log.e(tag, "founded_item_position="+founded_item_position);
				if(founded_item_selected==false){
					founded_item_position=0;
					//取消蓝牙扫描
	            	myBtAdapter.cancelDiscovery();
				}
				founded_item_selected=false;
				target_device=(BluetoothDevice)find_device_list.get(founded_item_position);
            	your_device_name=target_device.getName();
            	
                Log.e(tag, "pairing ...");
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
				Log.e(tag, "get target_device position is: "+position);
				founded_item_position=position;
				founded_item_selected=true;
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
 				Toast.makeText(ChatActivity.this, "开始聊天-确定", Toast.LENGTH_SHORT).show();
 				Log.e(tag, "bonded_item_position="+bonded_item_position);
 				if(bonded_item_selected==false){
 					bonded_item_position=0;
 				}
 				bonded_item_selected=false;
 				target_device=(BluetoothDevice) bonded_device_list.get(bonded_item_position);
             	Log.e(tag, "target_device name is: "+target_device.getName());
               	your_device_name=target_device.getName();
               	
 				chat_handler.sendEmptyMessage(client_ready);
 			}
 		});
         
         myBondedDialog.setSingleChoiceItems(bonded_device_name_adapter, 0, new OnClickListener() {
 			@Override
 			public void onClick(DialogInterface arg0, int position) {
 				Log.v(tag, "get target_device position is: "+position);
             	bonded_item_position=position;
             	bonded_item_selected=true;
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
