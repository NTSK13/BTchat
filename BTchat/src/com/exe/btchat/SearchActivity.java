package com.exe.btchat;

import java.util.ArrayList;
import java.util.UUID;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;


public class SearchActivity extends Activity {

	private String tag="wei";
	private BluetoothAdapter myBtAdapter;
	private String string_uuid="00001101-0000-1000-8000-00805F9B34FB";
	
	public ArrayList  BtDeviceList;
	public ArrayList  BtDeviceNameList;
	public int BTcount;
	public BluetoothDevice target_device=null;
	
	private ListView lv;
	

	private ChatThread ClientChatThread;
	private TextView tv_get_msg;
	private EditText et_send_msg;
	
	
	
	private Handler SearchHandler=new Handler(){
    	@Override
    	public void handleMessage(Message msg) {		
    		switch (msg.what) {
			case 0x04:  
				Log.v(tag, "SearchHandler msg");
				lv.invalidateViews();
				break;
						
			default:
				break;
			}
    		
    	};
    };
	
    
    public void SendMsgHandler(View v){
  		String msg=et_send_msg.getText().toString();
  		ClientChatThread.write(msg);
  		et_send_msg.setText("");
  	}
  	
  	private Handler ClientHandler=new Handler(){
      	@Override
      	public void handleMessage(Message msg) {		
      		switch (msg.what) {
  			case 0x02:  
  				Log.v(tag, "client  get msg");
  				Bundle b=msg.getData();
  				String client_get_msg=b.getString("get_msg");
  				tv_get_msg.setText(client_get_msg);
  				break;
  						
  			default:
  				break;
  			}
      		
      	};
      };
	
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lv);
        
        lv=(ListView)findViewById(R.id.lv_bt);
        
        BtDeviceList=new ArrayList();
        BtDeviceNameList=new ArrayList();
    	
    	
        ArrayAdapter<String> mAdapter=new ArrayAdapter<String>(this, android.R.layout.simple_list_item_single_choice,BtDeviceNameList);
        lv.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
        
        lv.setOnItemClickListener(new OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position,long id) {
                  
            	Log.v(tag, "get target_device position is: "+position);
            	target_device=(BluetoothDevice)BtDeviceList.get(position);
            	
            	myBtAdapter.cancelDiscovery();
            	lv.setVisibility(View.GONE);
            	setContentView(R.layout.activity_client_chat);
            	et_send_msg=(EditText)findViewById(R.id.et_send_msg);
                tv_get_msg=(TextView)findViewById(R.id.tv_get_msg);
                
                Log.v(tag, "pairing ...");
    			target_device.createBond();
    			
                //开启客户端线程
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
        });
        
        lv.setAdapter(mAdapter);

        myBtAdapter=BluetoothAdapter.getDefaultAdapter();//get local BT adapter
        
        /***********************注册蓝牙扫描相关的广播*************************/
        IntentFilter filter=new IntentFilter(BluetoothDevice.ACTION_FOUND);
        registerReceiver(mReceiver, filter);
        /**********************************************************************************/
        Log.v(tag, "start search...");
        myBtAdapter.startDiscovery();//search
        Toast.makeText(this, "搜索中 ...", Toast.LENGTH_LONG).show();
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
        		        BtDeviceNameList.add(device.getName());

        				SearchHandler.sendEmptyMessage(0x04);
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


