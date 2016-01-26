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
	private long hasReceived=0;
	private long hasTransfered=0;
	private boolean is_transfer=false;
	private int transfer_rev_count=0;
	private boolean is_transfer_finished=false;
	private final int client_msg_get=0x02;
    private final int server_msg_get=0x12;
    private final int rev_file_finished=0x22;
    private final int update_transfer_progress=0x23;
    private final int start_receive_file=0x24;
    private final int update_receive_progress=0x25;
    private final int start_transfer_file=0x26;
    private final int ask_if_accept=0x27;
    private final int response_if_accept=0x28;
    private long receive_file_size;
	
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
	    	 Log.e(tag, "mInStream==null ? "+(mInStream==null));
	    	 Log.e(tag, "mOutStream==null ? "+(mOutStream==null));
	    	
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
	                // Read from the InputStream
	               bytes = mInStream.read(buffer);     
	               get_msg = new String(buffer, 0, bytes);
	               Log.e(tag, "run_msg is :"+get_msg);
	               
	               if(get_msg.startsWith("ask_accept@#")){ // response
	            	   Log.e(tag, "response_if_accept ");
            	       String sresult=get_msg.split("@#")[1];
            	       
            	       Message msg=new Message();
            		   Bundle b_ask=new Bundle();
            		   b_ask.putString("response_result", sresult);
	            	   msg.setData(b_ask);
	            	   msg.what=response_if_accept;
	       
	            	   mHandler.sendMessage(msg);
	            	   continue;
               }
	               
	               if(get_msg.endsWith("@#filename@#ask")){//ask
	            	   Log.e(tag, "ask_if_accept ");
	            	       String name=get_msg.split("@#")[0];
	            	       Message msg=new Message();
	            		   Bundle b_ask=new Bundle();
	            		   b_ask.putString("ask_name", name);
		            	   msg.setData(b_ask);
		            	   msg.what=ask_if_accept;
		
		            	   mHandler.sendMessage(msg);
		            	   continue;
	               }
	               
	               if( get_msg.endsWith("@#filename@#send")){ //get file name
	            	   Log.e(tag, "file name is :"+get_msg); 
	            	   File my_dir=new File(my_dir_path);
	    	           my_dir.mkdir();
	    	           
	            	   String name=get_msg.split("@#")[0];
	            	   String sfileSize=get_msg.split("@#")[1];
	            	   
	            	   receive_file_size=Long.parseLong(sfileSize);
	            	   transfer_rev_count=0;
	            	   rev_name=my_dir_path+name;
	            	   Log.e(tag, "rev_name is :"+rev_name+" ,receive_file_size : "+receive_file_size);
	            	   File rev_file=new File(rev_name);
	            	   outs=new FileOutputStream(rev_file);
	            	   is_transfer=true;
	            	   hasReceived=0;
	            	   mHandler.sendEmptyMessage(start_receive_file);
	            	   continue;
	               }
	               if(get_msg.endsWith("@#fileFinish#@")){// transfer finish
	            	   Log.e(tag, "file transfer finish");
	            	   is_transfer=false;

	            	   Message msg=new Message();
	            	   Bundle b_finished=new Bundle();
	            	   b_finished.putString("rev_name", rev_name);
	            	   msg.setData(b_finished);
	            	   msg.what=rev_file_finished;
	   
	            	   mHandler.sendMessage(msg);
	            	   continue;
	               }
	               
	               Log.e(tag, "is_transfer : "+is_transfer+" , transfer_rev_count: "+transfer_rev_count);
	               if(is_transfer ==false ){// receive a  msg
	            	   Log.e(tag, "receive a msg ");
	            	   Message msg=new Message();
	            	   Bundle b_rcv_msg=new Bundle();
	            	   b_rcv_msg.putString("get_msg", get_msg);
	            	   msg.setData(b_rcv_msg);
            	   
	            	   if(is_client){
	            		   msg.what=client_msg_get;// send to client
	            	   }else{
	            		   msg.what=server_msg_get;// send to server
	            	   }
	            	   mHandler.sendMessage(msg);
	            	   continue;
	               }
	               if(is_transfer ){//transfer
	            	   outs.write(buffer, 0, bytes);
	            	   hasReceived +=bytes;
		               transfer_rev_count++;
		               //if(transfer_rev_count %10==0){
		            	   Message msg=new Message();
						   Bundle b=new Bundle();
		            	   b.putLong("hasReceived", hasReceived);
		            	   b.putLong("receive_file_size", receive_file_size);
		            	   msg.setData(b);
			         	   msg.what=update_receive_progress;
		            	   mHandler.sendMessage(msg);
		              // }
	               }    
	               
	            } catch (IOException e) {
	            	e.printStackTrace();
	                break;
	            }
	        }
	    }
	 public void write(String msg) {
		 try {
			 Log.e(tag, "write msg is :"+msg);
			 mOutStream.write(msg.getBytes());
			 mOutStream.flush();
	     } catch (IOException e) { 
	    	 e.printStackTrace();
	     }
	 }

	 public void transfer(String filePath, String fileName,long fileSize) {
		 String stmp=Long.toString(fileSize);
		 String fname=fileName+"@#"+stmp+"@#filename@#send";
		 String fileEnd=fileName+"@#fileFinish#@";
		 try {
			Log.e(tag, "transfer file is :"+filePath);
			Log.e(tag, "transfer file name is :"+fileName);
			Log.e(tag, "encode file name is :"+fname);
			mOutStream.write(fname.getBytes());
			mOutStream.flush();
			try {
				Thread.sleep(100);
			} catch (Exception e) {
				e.printStackTrace();
			}

		    File target_file=new File(filePath);
			InputStream ins=new FileInputStream(target_file);
			byte[] buffer=new byte[1024];
			int len=0;
			hasTransfered=0;
			while ( (len=ins.read(buffer) )!= -1) {
				//Log.e(tag, "transfer "+k+" th");
				mOutStream.write(buffer,0,len);			
				mOutStream.flush();
				hasTransfered+=len;
				//if(k%10==0){
					   Message msg=new Message();
					   Bundle b=new Bundle();
	            	   b.putLong("hasTransfered", hasTransfered);
	            	   msg.setData(b);
		         	   msg.what=update_transfer_progress;
		         	   Log.e(tag, "ChatThread update transfer progress");
	            	   mHandler.sendMessage(msg);
				//}
				
			}
			
			try {
				Thread.sleep(100);
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
