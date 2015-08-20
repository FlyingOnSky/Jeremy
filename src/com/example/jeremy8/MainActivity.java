package com.example.jeremy8;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {
	private Button btnEnd;   /*寫使用者名稱頁面按鈕-"完成"*/
	private SharedPreferences preference;
	private String sname,msg;
	private TextView txtName;
	private EditText edtName;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		       //取得介面元件
				btnEnd=(Button)findViewById(R.id.button1);
				txtName=(TextView)findViewById(R.id.textView1);
				edtName=(EditText)findViewById(R.id.editText1);
				//為事件加入監聽
				btnEnd.setOnClickListener(listener);
				
				preference=getSharedPreferences("username",MODE_PRIVATE);
				sname=preference.getString("name", "");
				
				//如果未建立資料就顯示輸入欄位
				if(sname.equals("")){
					msg="Welcome!Please enter your name!";
					//彈出歡迎視窗
					Toast welcome1=
							Toast.makeText(MainActivity.this,
									msg, Toast.LENGTH_LONG);
					welcome1.show();
					
					//把打字區設定為可見
					txtName.setVisibility(TextView.VISIBLE);
					edtName.setVisibility(EditText.VISIBLE);
				}else{
					msg="Hi!"+sname+"!";
					//彈出歡迎視窗
					Toast welcome2=
							Toast.makeText(MainActivity.this,
									msg, Toast.LENGTH_LONG);
					welcome2.show();
					//直接轉去主頁面
					Intent intent=new Intent();
					intent.setClass(MainActivity.this,SelfSpace.class);
					startActivity(intent);
					
				}
				
				
				
	}
	
	//按鈕處理事件
		private Button.OnClickListener listener=
				new Button.OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				Intent intent=new Intent();
				intent.setClass(MainActivity.this,SelfSpace.class);
				startActivity(intent);
			}
		};
		
		//儲存資料(使用者名稱)
		protected void onStop(){
			super.onStop();
			if(sname.equals("")){//如果未輸入資料就將輸入值存檔
				preference.edit()
				.putString("name",edtName.getText().toString())
				.commit();
			}
		}
		
		public boolean onKeyDown(int keyCode, KeyEvent event) {//捕捉返回鍵
	        if ((keyCode == KeyEvent.KEYCODE_BACK)) {   
	            ConfirmExit();//按返回鍵，則執行退出確認
	            return true;   
	        }   
	        return super.onKeyDown(keyCode, event);   
	    }
	    public void ConfirmExit(){//退出確認
	        AlertDialog.Builder ad=new AlertDialog.Builder(MainActivity.this);
	        ad.setTitle("離開");
	        ad.setMessage("確定要離開?");
	        ad.setPositiveButton("是", new DialogInterface.OnClickListener() {//退出按鈕
	            public void onClick(DialogInterface dialog, int i) {
	                // TODO Auto-generated method stub
	            	finish();//關閉activity
	  
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
		getMenuInflater().inflate(R.menu.main, menu);
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
