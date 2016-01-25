package com.exe.chat;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
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
	
	
	private String my_dir_path=new String("/sdcard/achat/");
	private String rev_name;
	private FileOutputStream outs;
	private int hasWrited=0;
	private boolean is_transfer=false;
	private int transfer_rev_count=0;
	private boolean is_transfer_finished=false;
	private final int client_msg_get=0x02;
    private final int server_msg_get=0x12;
    private final int rev_file_finished=0x22;

	
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
	        	
	            try {
	               //Log.v(tag, "Read msg from the InputStream ");
	                // Read from the InputStream
	               bytes = mInStream.read(buffer);
	               
	               get_msg = new String(buffer, 0, bytes);
	               Log.v(tag, "msg is :"+get_msg);
	               
	               if( get_msg.endsWith("@#filename#@")){ //get file name
	            	   Log.v(tag, "file name is :"+get_msg);
	            	   
	            	   File my_dir=new File(my_dir_path);
	    	           my_dir.mkdir();
	    	           
	            	   String name=get_msg.split("@#")[0];
	            	   rev_name=my_dir_path+name;
	            	   Log.v(tag, "rev_name is :"+rev_name);
	            	   File rev_file=new File(rev_name);
	            	   outs=new FileOutputStream(rev_file);
	            	   is_transfer=true;
	            	   hasWrited=0;
	               }else  if(get_msg.endsWith("@#fileFinish#@")){// transfer finish
	            	   Log.v(tag, "file transfer finish");
	            	   is_transfer=true;
	            	   is_transfer_finished=true;
	            	   Message msg=new Message();
	            	   Bundle b=new Bundle();
	            	   b.putString("rev_name", rev_name);
	            	   msg.setData(b);
	            	   msg.what=rev_file_finished;
	            	   //Toast.makeText(this, "传输结束,文件位于: "+rev_name, Toast.LENGTH_LONG).show();
	            	   mHandler.sendMessage(msg);
	               }
	               
	               Log.v(tag, "is_transfer : "+is_transfer+" , transfer_rev_count: "+transfer_rev_count);
	               if(is_transfer ==false ){// receive msg
	            	   Log.v(tag, "receive a msg ");
	            	   Message msg=new Message();
	            	   Bundle b=new Bundle();
	            	   b.putString("get_msg", get_msg);
	            	   msg.setData(b);
            	   
	            	   if(is_client){
	            		   msg.what=client_msg_get;// send to client
	            	   }else{
	            		   msg.what=server_msg_get;// send to server
	            	   }
	            	   mHandler.sendMessage(msg);
	               }else if(is_transfer && (transfer_rev_count>0)  && (is_transfer_finished !=true) ){//transfer
	            	   Log.v(tag, "file transfer .......");
	            	   outs.write(buffer, hasWrited, bytes);
		               hasWrited +=bytes;
		               transfer_rev_count++;
	               }else   if(is_transfer_finished==true){// transfer finish
	            	   Log.v(tag, "file transfer finish ");
	            	   is_transfer_finished=false;
	            	   is_transfer=false;
	            	   transfer_rev_count=0;
	               }else if(is_transfer){// start  transfer process
	            	   transfer_rev_count++;
	               }
	               
	            } catch (IOException e) {
	            	e.printStackTrace();
	                break;
	            }
	        }
	    }
	 public void write(String msg) {
		 try {
			 Log.v(tag, "write msg is :"+msg);
			 mOutStream.write(msg.getBytes());
			 mOutStream.flush();
	     } catch (IOException e) { 
	    	 e.printStackTrace();
	     }
	 }

	 public void transfer(String filePath, String fileName) {
		 String fname=fileName+"@#filename#@";
		 String fileEnd=fileName+"@#fileFinish#@";
		 try {
			Log.v(tag, "transfer file is :"+filePath);
			Log.v(tag, "transfer file name is :"+fileName);
			Log.v(tag, "encode file name is :"+fname);
			mOutStream.write(fname.getBytes());
			mOutStream.flush();
			try {
				Thread.sleep(500);
			} catch (Exception e) {
				e.printStackTrace();
			}

		    File target_file=new File(filePath);
			InputStream ins=new FileInputStream(target_file);
			byte[] buffer=new byte[1024];
			int len=0;
			int k=0;
			while ( (len=ins.read(buffer) )!= -1) {
				Log.v(tag, "transfer "+k+" th");
				mOutStream.write(buffer,0,len);
				try {
					Thread.sleep(10);
				} catch (Exception e) {
					e.printStackTrace();
				}
					
				mOutStream.flush();
				k++;
			}
			
			try {
				Thread.sleep(500);
			} catch (Exception e) {
				e.printStackTrace();
			}
			
			mOutStream.write(fileEnd.getBytes());
			mOutStream.flush();
			 
	     } catch (IOException e) { 
	    	 e.printStackTrace();
	     }
	 }
}
