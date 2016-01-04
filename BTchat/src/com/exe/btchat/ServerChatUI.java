package com.exe.btchat;

import java.util.UUID;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;

public class ServerChatUI extends Activity{
	
	private String tag="wei";
	private BluetoothAdapter myBtAdapter;
	private String string_uuid="00001101-0000-1000-8000-00805F9B34FB";
	private ChatThread ServerChatThread;
	private TextView tv_get_msg;
	private TextView wait_tv;
	private EditText et_send_msg;
	private ProgressBar wait_bar;
	BluetoothSocket current_socket; 
	
	
	@Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_server_chat);
        
        wait_bar=(ProgressBar)findViewById(R.id.waitBar);
        et_send_msg=(EditText)findViewById(R.id.et_send_msg);
        tv_get_msg=(TextView)findViewById(R.id.tv_get_msg);
        wait_tv=(TextView)findViewById(R.id.wait_tv);
        et_send_msg.setEnabled(false);
        myBtAdapter=BluetoothAdapter.getDefaultAdapter();//get local BT adapter
        
        Log.v(tag, "open Bt visible");
    	//可见120s
    	Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
    	discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 100);
    	startActivity(discoverableIntent);
    	    	
        //开启服务端线程
   	    Thread serverThread=new Thread(new Runnable(){
    	   	@Override
    	    public void run() {
    	    	try {
    	    		Log.v(tag, "serverThread run ");
    	    		BluetoothServerSocket server_socket=myBtAdapter.listenUsingRfcommWithServiceRecord("myServerSocket", UUID.fromString(string_uuid));
    	    		Log.v(tag, "wait client request");
    	    		    	    		
    	    		
    	    		current_socket = server_socket.accept();  
    	    		Log.v(tag, "accepted  client request");
    	    		ServerHandler.sendEmptyMessage(0x02);
    	    				
    	    	} catch (Exception e) {
    				e.printStackTrace();
    			}
    	    }
    	});
    	serverThread.start();     
    	
	}
        
	public void SendMsgHandler(View v){
		String msg=et_send_msg.getText().toString();
		ServerChatThread.write(msg);
		et_send_msg.setText("");
	}
	
	private Handler ServerHandler=new Handler(){
    	@Override
    	public void handleMessage(Message msg) {		
    		if(msg.what==0x01) {
				Log.v(tag, "server get msg");
				Bundle b=msg.getData();
				String server_get_msg=b.getString("get_msg");
				tv_get_msg.setText(server_get_msg);
			}else if(msg.what==0x02){
				//可以收发消息
				wait_bar.setVisibility(View.GONE);
				wait_tv.setVisibility(View.GONE);
				et_send_msg.setEnabled(true);

				ServerChatThread =new ChatThread(current_socket, ServerHandler,false);
	    		ServerChatThread.start();
				
			}
    	};
    };
    

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
