package com.example.jeremy8;

import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

public class SelfSpace extends Activity {
	private ListView lstPrefer;
	private int[] rexIds=new int[]{R.drawable.pictitle01,R.drawable.pictitle02,
			R.drawable.pictitle03,R.drawable.pictitle01,R.drawable.pictitle02,
			R.drawable.pictitle03,R.drawable.pictitle01,R.drawable.pictitle02,
			R.drawable.pictitle03};
	private String[] roomName=new String[]{"Animal","Human","Food","Animal",
			"Human","Food","Animal","Human","Food"};
	private String[] ans=new String[]{"giraffe","hero","rice","giraffe",
			"hero","rice","giraffe","hero","rice"};
	private Button btnoutside,btnname;
	private SharedPreferences preference;
	private String readname;
	//藍芽---------------------------------------------------------------------------
	// Local Bluetooth adapter
    private BluetoothAdapter mBluetoothAdapter = null;
    // Intent request codes
    private static final int REQUEST_ENABLE_BT = 1;
    
    public static final int MESSAGE_DEVICE_NAME = 0; //**

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_self_space);
		//---------------------------------------------------------------------------
		// Get local Bluetooth adapter
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        
        GameService.getSelfSpaceHandler(mSelfSpaceHandler); //**

        // If the adapter is null, then Bluetooth is not supported
        if (mBluetoothAdapter == null) {
            Toast.makeText(this, "Bluetooth is not available", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

	    // If BT is not on, request that it be enabled.
	    // setupChat() will then be called during onActivityResult
	    if (!mBluetoothAdapter.isEnabled()) {
	        Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
	        startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
	    // Otherwise, setup the chat session
	    } else {
	    	if (GameService.mState.equals(GameService.State.Stopped)) {setupService();}
	    }
        
        //Ensure to be visible
	    ensureDiscoverable();
		//---------------------------------------------------------------------------
		
		//取得資源類別檔中介面元件
		lstPrefer=(ListView)findViewById(R.id.listView1);
				
		//取得按鈕介面元件
		btnname=(Button)findViewById(R.id.button2);
		btnoutside=(Button)findViewById(R.id.button3);		
		
		//觸發事件
		btnoutside.setOnClickListener(btnResponse);
		btnname.setOnClickListener(btnResponse);
						
		//建立自訂adapter
		MyAdapter adapter=new MyAdapter(this);
				
		//設定listview的資料來源
		lstPrefer.setAdapter(adapter);
		
		//尋找儲存檔
		preference=getSharedPreferences("username",MODE_PRIVATE);
		//讀取資料
		readname=preference.getString("name","unknown");
		//按鈕顯示姓名
		btnname.setText(readname);
		
		//設定itemclick事件
		lstPrefer.setOnItemClickListener(lstPreferListener);
		
		}
	
	// 按鈕觸發事件
	private Button.OnClickListener btnResponse=
			new Button.OnClickListener(){
		@Override
		public void onClick(View v){
			
			switch(v.getId()){
			case R.id.button2:
				Intent intent1=new Intent();
				intent1.setClass(SelfSpace.this,ChangName.class);
				startActivity(intent1);
				SelfSpace.this.finish();
				break;
			case R.id.button3:
				Intent intent2=new Intent();
				intent2.setClass(SelfSpace.this,OutSide.class);
				startActivity(intent2);
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
				return ans.length;
			}
			
			@Override
			public Object getItem(int position){
				return ans[position];
			}
			
			@Override
			public long getItemId(int position){
				return position;
			}
			
			@Override
			public View getView(int position,View convertView,
					ViewGroup parent){
				
				//取得自訂介面
				convertView=myInflater.inflate(R.layout.mylayout,null);
				
				//取得 mylayout.xml 中的文件
				ImageView pictitle=(ImageView)
						convertView.findViewById(R.id.imageview1);
				TextView txtName=(TextView)
						convertView.findViewById(R.id.textView2);
				TextView txtengName=(TextView)
						convertView.findViewById(R.id.textView3);
				
				
				//設定元件內容
				pictitle.setImageResource(rexIds[position]);
				txtName.setText(roomName[position]);
				txtengName.setText(ans[position]);
				
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
				Intent intent3=new Intent();
				intent3.setClass(SelfSpace.this,FileContent.class);
				startActivity(intent3);
			}
		};
		
		public boolean onKeyDown(int keyCode, KeyEvent event) {//捕捉返回鍵
	        if ((keyCode == KeyEvent.KEYCODE_BACK)) {   
	            ConfirmExit();//按返回鍵，則執行退出確認
	            return true;   
	        }   
	        return super.onKeyDown(keyCode, event);   
	    }
	    public void ConfirmExit(){//退出確認
	        AlertDialog.Builder ad=new AlertDialog.Builder(SelfSpace.this);
	        ad.setTitle("離開");
	        ad.setMessage("確定要離開?");
	        ad.setPositiveButton("是", new DialogInterface.OnClickListener() {//退出按鈕
	            public void onClick(DialogInterface dialog, int i) {
	                // TODO Auto-generated method stub
	               SelfSpace.this.finish();//關閉activity
	  
	            }
	        });
	        ad.setNegativeButton("否",new DialogInterface.OnClickListener() {
	            public void onClick(DialogInterface dialog, int i) {
	                //不退出不用執行任何操作
	            }
	        });
	        ad.show();//示對話框
	    }

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.self_space, menu);
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

//-----------------------------------------------------------------------------------
     //Ensure discoverable
	 private void ensureDiscoverable() {
	     if (mBluetoothAdapter.getScanMode() !=
	             BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE) {
	             Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
	             discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
	             startActivity(discoverableIntent);
	     }
	 }
	 
	 private void setupService() {
	     // Initialize the ChatService to perform bluetooth connections
	     Intent intent = new Intent(GameService.ACTION_START);
	     startService(intent);
	 }
	 
	 public void onActivityResult(int requestCode, int resultCode, Intent data) {
	        switch (requestCode) {
	        case REQUEST_ENABLE_BT:
	            // When the request to enable Bluetooth returns
	            if (resultCode == Activity.RESULT_OK) {
	                // Bluetooth is now enabled, so set up a chat session
	                setupService();
	            } else {
	                // User did not enable Bluetooth or an error occured
	                Toast.makeText(this, "Bluetooth is not enabled, leaving", Toast.LENGTH_LONG).show();
	                finish();
	            }
	            break;
	        }
	 }
	 
	 private final Handler mSelfSpaceHandler = new Handler() { //**
			@Override
			public void handleMessage(Message msg) {
				switch(msg.what) {
				case MESSAGE_DEVICE_NAME:
					String deviceName = msg.getData().getString("device_name");
	                Toast.makeText(getApplicationContext(), "Connected to "
	                               + deviceName, Toast.LENGTH_SHORT).show();
					break;
				}
			}
		};
	 
}