package com.example.jeremy8;

import java.util.ArrayList;

import com.example.jeremy8.SelfSpace.MyAdapter;


import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

public class OutSide extends Activity {
	private Button btncreatroom;
	private ListView lstPrefer;
	private MyAdapter adapter;
	private ArrayList<String> roomName;
	private ArrayList<String> population;
	private ArrayList<String> roomID;
	private ArrayList<String> nowpopulation;

	private SharedPreferences preference;
	
	
	//上次按下返回键的系统时间  
    private long lastBackTime = 0;  
    //当前按下返回键的系统时间  
    private long currentBackTime = 0;
    
    public static final int MESSAGE_NEW_ROOM = 0;
    public static final int MESSAGE_JOIN_ROOM = 1;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_out_side);
		
		//初始化  新加的
		roomName=new ArrayList<String>();
		population=new ArrayList<String>();
		roomID=new ArrayList<String>();
		nowpopulation=new ArrayList<String>();
		//
		
	
		//Deliver this Activity's handler to service
		GameService.getOutSideHandler(mOutSideHandler);
		
		//取得介面元件
		btncreatroom=(Button)findViewById(R.id.button4);
		lstPrefer=(ListView)findViewById(R.id.listView2);
		
		//設定觸發事件
		btncreatroom.setOnClickListener(btnResponse);
		
		//建立自訂adapter
		MyAdapter adapter=new MyAdapter(this);
				
		//設定listview的資料來源
		lstPrefer.setAdapter(adapter);
		
		//設定itemclick事件
		lstPrefer.setOnItemClickListener(lstPreferListener);
		
		preference=getSharedPreferences("creatroom",MODE_PRIVATE);

	}
	
	//觸發事件
	private Button.OnClickListener btnResponse=
			new Button.OnClickListener(){
		@Override
		public void onClick(View v){
			
			switch(v.getId()){
			case R.id.button4:
				Intent intent1=new Intent();
				intent1.setClass(OutSide.this,CreatRoom.class);
				startActivity(intent1);
				break;
			default:
				break;			
			}
		}
	};
	
	public class MyAdapter extends BaseAdapter{

		private LayoutInflater myInflater;
		public MyAdapter(Context c){
			myInflater=LayoutInflater.from(c);
		}
		
		@Override
		public int getCount(){
			return roomName.size();
		}
		
		@Override
		public Object getItem(int position){
			return roomName.get(position);
		}
		
		@Override
		public long getItemId(int position){
			return position;
		}
		
		@Override
		public View getView(int position,View convertView,
				ViewGroup parent){
			
			//取得自訂介面
			convertView=myInflater.inflate(R.layout.outsidelayout,null);
			
			//取得 mylayout.xml 中的文件
			TextView txtRoomname=(TextView)
					convertView.findViewById(R.id.textView4);
			TextView txtPopulation=(TextView)
					convertView.findViewById(R.id.textView16);
			TextView txtnowPopulation=(TextView)
					convertView.findViewById(R.id.textView5);

			
			//設定元件內容
			txtRoomname.setText(roomName.get(position));
			txtPopulation.setText(population.get(position));
			txtnowPopulation.setText("0");
			
			return convertView;
		}
		
	}
	
	//定義onItemClick方法
	private ListView.OnItemClickListener lstPreferListener=
			new ListView.OnItemClickListener(){
		@Override
		public void onItemClick(AdapterView<?> parent,
				View view,int position,long id){
			//顯示listView選項內容
			//儲存資料
			preference.edit()
			.clear()
			.putString("roomname",roomName.get(position))
			.putInt("population",  Integer.parseInt(population.get(position)))		
			.commit();
			
			Intent intent = new Intent(GameService.ACTION_JOIN_ROOM);
			intent.putExtra("roomID",roomID.get(position));
			//**intent.putExtra("roomMaxPopulation", Integer.valueOf(population.get(position)));
			startService(intent);
			
			//轉換頁面
			Intent intent3=new Intent();
			intent3.setClass(OutSide.this,GameRoom.class);
			startActivity(intent3);
		}
	};
	
/*	@Override  
    public boolean onKeyDown(int keyCode, KeyEvent event) {  
        //捕获返回键按下的事件  
        if(keyCode == KeyEvent.KEYCODE_BACK){  
            //获取当前系统时间的毫秒数  
            currentBackTime = System.currentTimeMillis();  
            //比较上次按下返回键和当前按下返回键的时间差，如果大于2秒，则提示再按一次退出  
            if(currentBackTime - lastBackTime > 2 * 1000){  
                Toast.makeText(this, "再按一次返回键退出", Toast.LENGTH_SHORT).show();  
                lastBackTime = currentBackTime;  
                Intent intent1=new Intent();
				intent1.setClass(OutSide.this,SelfSpace.class);
				startActivity(intent1);
            }else{ //如果两次按下的时间差小于2秒，则退出程序  
                finish();  
            }  
            return true;  
        }  
        return super.onKeyDown(keyCode, event);  
    }  
 */ 
  
	

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.out_side, menu);
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
	
	
	private final Handler mOutSideHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			switch(msg.what) {
			case MESSAGE_NEW_ROOM://有人創房間
				/*原本的
				String roomIDtemp = msg.getData().getString("roomID"); //roomID(Founder's address)
				String roomNametemp = msg.getData().getString("roomName"); //roomName
				String populationtemp = msg.getData().getString("population"); //population
				*/
				
			    //新加的
				Toast.makeText(getApplicationContext(),msg.getData().getString("roomName"),Toast.LENGTH_SHORT).show();
				
				roomID.add( msg.getData().getString("roomID"));
				roomName.add(msg.getData().getString("roomName"));
				population.add(msg.getData().getString("population"));
				
				MyAdapter adapter=new MyAdapter(OutSide.this);
				lstPrefer.setAdapter(adapter);
				//
				
				/*原本的
			    //加進arraylist中
				roomID.add(roomIDtemp);
				roomName.add(roomNametemp);
				population.add(populationtemp);
				//重新整理list
				lstPrefer.setAdapter(adapter);
				*/
			

				break;
			case MESSAGE_JOIN_ROOM://有人加入房間
				
				String roomID2temp =(String) msg.obj;
				
				//判斷這是哪個room 人數+1
				for(int i=0 ; i<roomID.size(); i++) {
					if(roomID2temp.equals(roomID.get(i)) ){
						int n=Integer.parseInt(nowpopulation.get(i))+1;
						nowpopulation.set(i,String.valueOf(n));
					}
				}
				/*int i=0;
				do{
					if(roomID2temp.equals(roomID.get(i)) ){
						int n=Integer.parseInt(nowpopulation.get(i))+1;
						nowpopulation.set(i,String.valueOf(n));
					}
					i+=1;
				}while(!roomID2temp.equals(roomID.get(i)) );*/
			}
		}
	};
	
	protected void onStop(){
		super.onStop();
	}
}