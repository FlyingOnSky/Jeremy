package com.example.jeremy8;

import java.util.ArrayList;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;

public class GameRoom extends Activity {
	private TextView txtroomname,txtpopulation;
	private SharedPreferences preference;
	private String readroomname;
	private int readpopulation;
	
	ArrayList<String> addressList = new ArrayList<String>();
	ArrayList<String> nameList = new ArrayList<String>();
	
	public static final int MESSAGE_GAMER_LIST = 0;
	public static final int MESSAGE_NEW_GAMER = 1;
	
	//清單+人物列表

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_game_room);
		
		//Deliver this Activity's handler to service
		GameService.getGameRoomHandler(mGameRoomHandler);
		
		//取得介面元件
		txtroomname=(TextView)findViewById(R.id.textView8);
		txtpopulation=(TextView)findViewById(R.id.textView14);
		
		//尋找儲存檔
		preference=getSharedPreferences("creatroom",MODE_PRIVATE);
		//拿資料
		readroomname=preference.getString("roomname","unknown");
		readpopulation=preference.getInt("population", 0);
		
		//顯示
		txtroomname.setText(readroomname);	
		txtpopulation.setText(String.valueOf(readpopulation));
		
		//先直接開始~~
		new AlertDialog.Builder(GameRoom.this)
		.setTitle("~~Start~~")
		.setIcon(R.drawable.ic_launcher)
		.setMessage("Sure to Start?")
		.setPositiveButton("Sure",new DialogInterface.OnClickListener()
		{
			public void onClick(DialogInterface dialoginterface,int i){
				Intent intent=new Intent();
				intent.setClass(GameRoom.this,Title.class);
				startActivity(intent);
			}
		})
		.show();
	}
	
	public boolean onKeyDown(int keyCode, KeyEvent event) {//捕捉返回鍵
        if ((keyCode == KeyEvent.KEYCODE_BACK)) {   
            ConfirmExit();//按返回鍵，則執行退出確認
            return true;   
        }   
        return super.onKeyDown(keyCode, event);   
    }
    public void ConfirmExit(){//退出確認
        AlertDialog.Builder ad=new AlertDialog.Builder(GameRoom.this);
        ad.setTitle("Leave Room");
        ad.setMessage("Sure to Leave GameRoom?");
        ad.setPositiveButton("YES", new DialogInterface.OnClickListener() {//退出按鈕
            public void onClick(DialogInterface dialog, int i) {
                // TODO Auto-generated method stub
            	Intent intent=new Intent();
				intent.setClass(GameRoom.this,OutSide.class);
				startActivity(intent);
                GameRoom.this.finish();//關閉activity
  
            }
        });
        ad.setNegativeButton("NO",new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int i) {
                //不退出不用執行任何操作
            }
        });
        ad.show();//示對話框
    }

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.game_room, menu);
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
	
	private final Handler mGameRoomHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			switch(msg.what) {
			case MESSAGE_GAMER_LIST:
				String addressNameList =(String) msg.obj; //addressNameList = "address1,name1,address2,name2 ..."
				String[] str = addressNameList.split(",");
				
				//Add each address/name to ArrayList<String>
				for(int i=0;i<str.length;i+=2) {
					addressList.add(str[i]);
				}
				for(int i=1;i<str.length;i+=2) {
					nameList.add(str[i]);
				}
				break;
			case MESSAGE_NEW_GAMER:
				String newAddress = msg.getData().getString("address");
				String newName = msg.getData().getString("name");
				
				//Add the address/name to ArrayList<String>
				addressList.add(newAddress);
				nameList.add(newName);
			}
		}
	};
	
	/*//把資料傳到service
	Intent intent = new Intent(GameService.ACTION_CREATE_ROOM);
	intent.putExtra("roomname",readroomname);
	intent.putExtra("population",readpopulation);
	startService(intent);*/
}
