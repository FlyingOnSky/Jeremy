package com.example.jeremy8;

import com.example.jeremy8.SelfSpace.MyAdapter;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

public class FileContent extends Activity {
	private ListView lstPrefer;
	private TextView txtData;
	private SharedPreferences preference;
	private String readData;
	private int[] rexIds=new int[]{R.drawable.pictitle01,R.drawable.pictitle02,
			R.drawable.pictitle03};
	private String[] ans=new String[]{"giraffe","hero","rice"};
	//上次按下返回键的系统时间  
    private long lastBackTime = 0;  
    //当前按下返回键的系统时间  
    private long currentBackTime = 0;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_file_content);
		
		//取得資源類別檔中介面元件
		lstPrefer=(ListView)findViewById(R.id.listView4);
		txtData=(TextView)findViewById(R.id.textView9);
		
		//尋找儲存檔
		preference=getSharedPreferences("creatroom",MODE_PRIVATE);
		//讀取資料
		readData=preference.getString("data","unknown");
				//按鈕顯示姓名
		txtData.setText(readData);
		
		//建立自訂adapter
		MyAdapter adapter=new MyAdapter(this);
						
		//設定listview的資料來源
		lstPrefer.setAdapter(adapter);
		
		
	}
	
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
			convertView=myInflater.inflate(R.layout.filelayout,null);
			
			//取得 mylayout.xml 中的文件
			ImageView pictitle=(ImageView)
					convertView.findViewById(R.id.imageView2);
			TextView txtAns=(TextView)
					convertView.findViewById(R.id.textView10);

			//設定元件內容
			pictitle.setImageResource(rexIds[position]);
			txtAns.setText(ans[position]);
			
			return convertView;
		}
		
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
				intent.setClass(FileContent.this,SelfSpace.class);
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
		getMenuInflater().inflate(R.menu.file_content, menu);
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
