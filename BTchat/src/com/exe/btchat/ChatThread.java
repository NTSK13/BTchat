package com.exe.btchat;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import android.bluetooth.BluetoothSocket;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

public class ChatThread extends Thread{
	
	private InputStream mInStream ;
	private OutputStream mOutStream ;
	private final BluetoothSocket mSocket;
	private Handler mHandler;
	private boolean is_client;
	private String tag="wei";
	
	public ChatThread(BluetoothSocket socket, Handler handler, boolean isc)
	{
		mSocket=socket;
		mHandler=handler;
		mInStream = null;
		mOutStream = null;
		is_client=isc;
		try {
			mInStream = mSocket.getInputStream();
			mOutStream = mSocket.getOutputStream();
	    	 Log.v(tag, "mInStream==null ? "+(mInStream==null));
	    	 Log.v(tag, "mOutStream==null ? "+(mOutStream==null));
	    	
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
     
	 public void run() {
	         String get_msg;
	         byte[] buffer = new byte[1024];  // buffer store for the stream
	         int bytes; // bytes returned from read()
	        // Keep listening to the InputStream until an exception occurs
	        while (true) {
	        	Log.v(tag, "ChatThread run ");
	            try {
	               Log.v(tag, "ChatThread  Read from the InputStream ");
	                // Read from the InputStream
	               bytes = mInStream.read(buffer);
	               
	               get_msg = new String(buffer, 0, bytes);
	               Log.v(tag, "run get  msg is :"+get_msg);
	               
	               Message msg=new Message();
            	   Bundle b=new Bundle();
            	   b.putString("get_msg", get_msg);
            	   msg.setData(b);
            	   
	               if(is_client){
	            	   msg.what=0x02;// send to client
	               }else{
	            	   msg.what=0x01;// send to server
	            	   
	               }
	               mHandler.sendMessage(msg);
	               
	            } catch (IOException e) {
	            	e.printStackTrace();
	                break;
	            }
	        }
	    }
	 public void write(String msg) {
		 try {
			 Log.v(tag, "111111111 write msg is :"+msg);
			 mOutStream.write(msg.getBytes());
			 mOutStream.flush();
			 Log.v(tag, "222222222 write msg is :"+msg);
	     } catch (IOException e) { 
	    	 e.printStackTrace();
	     }
	 }

}
