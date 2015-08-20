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
import android.widget.Toast;

public class ChangName extends Activity {
	private Button btnok;
	private EditText edtname;
	private SharedPreferences preference;
	//上次按下返回键的系统时间  
    private long lastBackTime = 0;  
    //当前按下返回键的系统时间  
    private long currentBackTime = 0;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_chang_name);
		
		//取得介面元件
		btnok=(Button)findViewById(R.id.button6);
		edtname=(EditText)findViewById(R.id.editText2);
		
		//設定觸發事件
		btnok.setOnClickListener(btnResponse);
		
		//尋找儲存檔
		preference=getSharedPreferences("username",MODE_PRIVATE);
	}
	
	private Button.OnClickListener btnResponse=
			new Button.OnClickListener(){
		@Override
		public void onClick(View v){
			
			switch(v.getId()){
			case R.id.button6:
				//確認有沒有輸入
				if(edtname.getText().toString().equals("")){
					
				}else{
					preference.edit()
					.clear()                                       //刪除原本資料
					.putString("name",edtname.getText().toString())//儲存現在資料
					.commit();	
				}
				//轉換頁面
				Intent intent1=new Intent();
				intent1.setClass(ChangName.this,SelfSpace.class);
				startActivity(intent1);
				break;
			default:
				break;			
			}
		}
	};
	
	@Override  
    public boolean onKeyDown(int keyCode, KeyEvent event) {  
        //捕获返回键按下的事件  
        if(keyCode == KeyEvent.KEYCODE_BACK){  
            //获取当前系统时间的毫秒数  
            currentBackTime = System.currentTimeMillis();  
            //比较上次按下返回键和当前按下返回键的时间差，如果大于2秒，则提示再按一次退出  
            if(currentBackTime - lastBackTime > 2 * 1000){  
                Toast.makeText(this, "再按一次返回键退出", Toast.LENGTH_SHORT).show();  
                lastBackTime = currentBackTime;  
                Intent intent=new Intent();
				intent.setClass(ChangName.this,SelfSpace.class);
				startActivity(intent);
            }else{ //如果两次按下的时间差小于2秒，则退出程序  
                finish();  
            }  
            return true;  
        }  
        return super.onKeyDown(keyCode, event);  
    }  
	
	

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.chang_name, menu);
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
