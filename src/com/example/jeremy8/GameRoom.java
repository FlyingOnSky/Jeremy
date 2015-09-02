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
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

public class GameRoom extends Activity {
	private TextView txtroomname,txtpopulation,txtnowpopulation;
	private SharedPreferences preference;
	private String readroomname;
	private int readpopulation;
	
	ArrayList<String> addressList = new ArrayList<String>();
	ArrayList<String> nameList = new ArrayList<String>();
	
	public static final int MESSAGE_GAMER_LIST = 0;
	public static final int MESSAGE_NEW_GAMER = 1;
	public static final int MESSAGE_ASK_FOR_START_GAME = 2;
	public static final int MESSAGE_START_GAME = 3;
	
	//�M��+�H���C��
	private ListView listPrefer;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_game_room);
		
		
		//Deliver this Activity's handler to service
		GameService.getGameRoomHandler(mGameRoomHandler);
		
		//���o��������
		txtroomname=(TextView)findViewById(R.id.textView8);
		txtpopulation=(TextView)findViewById(R.id.textView14);
		listPrefer=(ListView)findViewById(R.id.listView3);
		txtnowpopulation=(TextView)findViewById(R.id.textView15);
		
		
		//�M���x�s��
		preference=getSharedPreferences("creatroom",MODE_PRIVATE);
		
		//�����
		readroomname=preference.getString("roomname","5words");
		readpopulation=preference.getInt("population", 7);
		
		//���
		txtroomname.setText(readroomname);	
		txtpopulation.setText("/"+String.valueOf(readpopulation));
		
		//�إ�arrayadapt
		ArrayAdapter<String> adapterName=
				new ArrayAdapter<String>(this,android.R.layout.simple_list_item_1,
						nameList);
		
		//�]�wLISTVIEW��ƨӷ�
		listPrefer.setAdapter(adapterName);
			
	}
	
	public boolean onKeyDown(int keyCode, KeyEvent event) {//������^��
        if ((keyCode == KeyEvent.KEYCODE_BACK)) {   
            ConfirmExit();//����^��A�h����h�X�T�{
            return true;   
        }   
        return super.onKeyDown(keyCode, event);   
    }
    public void ConfirmExit(){//�h�X�T�{
        AlertDialog.Builder ad=new AlertDialog.Builder(GameRoom.this);
        ad.setTitle("Leave Room");
        ad.setMessage("Sure to Leave GameRoom?");
        ad.setPositiveButton("YES", new DialogInterface.OnClickListener() {//�h�X���s
            public void onClick(DialogInterface dialog, int i) {
                // TODO Auto-generated method stub
            	Intent intent=new Intent();
				intent.setClass(GameRoom.this,OutSide.class);
				startActivity(intent);
                GameRoom.this.finish();//����activity
  
            }
        });
        ad.setNegativeButton("NO",new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int i) {
                //���h�X���ΰ������ާ@
            }
        });
        ad.show();//�ܹ�ܮ�
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
				
				//�H�ƺ��F�N�}�l
				int size=nameList.size();
				txtnowpopulation.setText(String.valueOf(size));
				break;
			case MESSAGE_ASK_FOR_START_GAME:
				//ask for start the game (for founder)
				new AlertDialog.Builder(GameRoom.this)
				.setTitle("~~Start~~")
				.setIcon(R.drawable.ic_launcher)
				.setMessage("Sure to Start?")
				.setPositiveButton("Sure",new DialogInterface.OnClickListener()
				{
					public void onClick(DialogInterface dialoginterface,int i){
						Intent intent = new Intent(GameService.ACTION_START_GAME);
						startService(intent);
						
						Intent intent2=new Intent();
						intent2.setClass(GameRoom.this,Title.class);
						startActivity(intent2);
					}
				})
				.show();
				break;
			case MESSAGE_START_GAME:
				//start the game (for other gamer beside founder)
				Intent intent2=new Intent();
				intent2.setClass(GameRoom.this,Title.class);
				startActivity(intent2);
				break;
							
			}
		}
	};
}
