package com.example.jeremy8;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

public class NearBy extends Activity {
	//上次按下返回键的系统时间  
    private long lastBackTime = 0;  
    //当前按下返回键的系统时间  
    private long currentBackTime = 0;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_near_by);
	/*	private ListView lstPrefer;
		static ArrayList<String> thisAddressList;

		ListView lstPrefer = (ListView) this.findViewById(R.id.listView3); 
		thisAddressList=ChatService.getAllAddress();
		ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_expandable_list_item_1,thisAddressList); 
		lstPrefer.setAdapter(adapter);
		待藍芽加入後測試
	*/
	}
	
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
				intent.setClass(NearBy.this,OutSide.class);
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
		getMenuInflater().inflate(R.menu.near_by, menu);
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
