package com.exe.btchat;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;


public class InitActivity extends Activity {

	private String tag="wei";
	
	private BluetoothAdapter myBtAdapter;
	
	
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_init);
        
        myBtAdapter=BluetoothAdapter.getDefaultAdapter();//get local BT adapter
        try {
			Log.v(tag, "Get BT adapter");
			if(! myBtAdapter.isEnabled()){// ���û�д�����,��ô������
				//�����Ի���,��ʾ�û�������
				//Intent it =new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
				//startActivity(it);
				myBtAdapter.enable();//��ֱ֪ͨ�Ӵ�����
			}	
			
		} catch (Exception e) {
			Log.e(tag, "My device has no BT adapter.");
			e.printStackTrace();
		}
    }
 
    public void StartServerChatHandler(View v){   	
    	//��ʼ����
    	Intent it=new Intent();
    	it.setAction("android.intent.action.ServerChatUI");
    	startActivity(it);
    	finish();
    	
    }
  
    public void StartClientChatHandler(View v){
    	//��ʼ��������device����
    	Intent it=new Intent();
    	it.setAction("android.intent.action.SearchActivity");
    	startActivity(it);
    	finish();
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


