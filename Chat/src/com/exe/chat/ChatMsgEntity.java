package com.exe.chat;

import java.util.Calendar;


public class ChatMsgEntity {
    private static final String TAG = ChatMsgEntity.class.getSimpleName();
 
    private String name;
 
    private String date;
 
    private String text;
 
    private boolean msgType = true;
 
    public boolean getMsgType() {
        return msgType;
    }
 
    public void setMsgType(boolean msgType) {
        this.msgType = msgType;
    }
 
    public String getName() {
        return name;
    }
 
    public void setName(String name) {
        this.name = name;
    }
 

 
    public void setDate(String date) {
        this.date = date;
    }
 
    public String getText() {
        return text;
    }
 
    public void setText(String text) {
        this.text = text;
    }
 
	public String getDate() {
	        Calendar c = Calendar.getInstance();
	        String year = String.valueOf(c.get(Calendar.YEAR));
	        String month = String.valueOf(c.get(Calendar.MONTH)+1);
	        String day = String.valueOf(c.get(Calendar.DAY_OF_MONTH) );
	        String hour = String.valueOf(c.get(Calendar.HOUR_OF_DAY));
	        String mins = String.valueOf(c.get(Calendar.MINUTE));
	        StringBuffer sbBuffer = new StringBuffer();
	        sbBuffer.append(year + "-" + month + "-" + day + " " + hour + ":" + mins);
	        date=sbBuffer.toString();
	        return date;
    }
 
    public ChatMsgEntity() {
    }
 
    public ChatMsgEntity(String name, String date, String text, boolean msgType) {
        this.name = name;
        this.date = date;
        this.text = text;
        this.msgType = msgType;
    }
}