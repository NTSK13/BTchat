package com.exe.chat;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;
import android.widget.SimpleAdapter;


public class FileBrowserActivity extends Activity {
	
	private String tag="wei";
	private ListView lv;
	private File currentParentPath;
	private File[] currentFiles;

	private int count=0;
	private ArrayList<File> ParentPathArray;
	private ArrayList<File[]> ParentFilesArray;
	
	private String root_dir;
    private final int back_item_id=0x111;
	

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		// TODO Auto-generated method stub
		if(keyCode ==KeyEvent.KEYCODE_BACK ){
			setResult(RESULT_CANCELED);
			finish();
		}
		
		return super.onKeyDown(keyCode, event);
	}


	@Override
	protected void onCreate(Bundle savedInstanceState)  {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_filebrowser);
		
		lv=(ListView)findViewById(R.id.listView);
		//root_dir=Environment.getExternalStorageDirectory().getAbsolutePath();
		root_dir=new String("/storage/emulated/0/");
		File root=new File(root_dir);
		if(root.exists()){
			Log.v(tag, root_dir +"  is exist");
			currentParentPath=root;
			currentFiles=root.listFiles();
			count=0;
			ParentPathArray=new ArrayList<File>();
			ParentFilesArray=new ArrayList<File[]>();
			
			ParentPathArray.add(count, currentParentPath);
			ParentFilesArray.add(count, currentFiles);
			count++;
			
			
			Arrays.sort(currentFiles);
			inflateListView(currentFiles);
		}
		
		lv.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View v,int position,long id)
			{
				if( currentFiles[position].isFile() )//it is a file
				{
					//return file name and file path
					Intent it= new Intent();
					it.putExtra("filePath", currentFiles[position].getAbsolutePath());
					it.putExtra("fileName", currentFiles[position].getName());
					it.putExtra("fileSize", currentFiles[position].length() );
	
					setResult(RESULT_OK, it);
					finish();
				}
				
				File[] tmp=currentFiles[position].listFiles();
				ParentPathArray.add(count, currentParentPath);
				ParentFilesArray.add(count, currentFiles);
				count++;
				
				if( tmp !=null && tmp.length!=0){
					
					currentParentPath=currentFiles[position];
					currentFiles=tmp;
					Arrays.sort(currentFiles);
					inflateListView(currentFiles);
				}
			}
		});
	}
	
	
	private void inflateListView(File[] files)
	{
		List<Map<String, Object>> listItems=new ArrayList<Map<String, Object>>();
		//Log.v(tag, " enter  inflateListView ");
		for(int i=0;i< files.length;i++){
			Map<String, Object> listItem=new HashMap<String,Object>();
			
			if(files[i].isDirectory()){
				listItem.put("icon",R.drawable.folder);
			}else{
				listItem.put("icon",R.drawable.file);
			}
			listItem.put("fileName",files[i].getName());
			listItems.add(listItem);
		}
		
		SimpleAdapter sadp=new SimpleAdapter(this,listItems,R.layout.line,new String[]{"icon","fileName"}, new int[] {R.id.icon,R.id.file_name});
		lv.setAdapter(sadp);
				
	}
	
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.chat, menu);
        menu.add(0, back_item_id, 0, "返回到上一目录");
   
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
       
        if(id==back_item_id){
        	Log.v(tag, "start save record");
        	back_to_upper();
        }

        return super.onOptionsItemSelected(item);
    }
    
    private void back_to_upper(){
    	count--;
    	currentParentPath=ParentPathArray.get(count);
    	currentFiles=ParentFilesArray.get(count);
		count++;
		
		inflateListView(currentFiles);
	}
	
}
