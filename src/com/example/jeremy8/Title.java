package com.example.jeremy8;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Message;
import android.util.JsonWriter;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

public class Title extends Activity {
	private EditText edttitle;
	private SharedPreferences preference, MAXpre;
	private TextView mTextView;//�˼ƭp��
	private int i=1;
	//���a-json�ɦW-�qLISTVIEW��
	
	
	public static final int MESSAGE_GETSELF = 0;
	
	private Bundle selfLocated=this.getIntent().getExtras();
	private int self=selfLocated.getInt("self");

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_title);
		
		//���o��������
		edttitle=(EditText)findViewById(R.id.editText5);
		
		//�إ��x�s��
		preference=getSharedPreferences("ans",MODE_PRIVATE);
		MAXpre = getSharedPreferences("creatroom",MODE_PRIVATE);
		int MAX = MAXpre.getInt("population", 0);
		String file = MAXpre.getString("roomname", "unknown");
		
		//��JSON��
		try{
		  String FILENAME = file+".json";
		  //gives file name
		  FileOutputStream out = openFileOutput(FILENAME, MODE_WORLD_READABLE);
		  JsonWriter writer = new JsonWriter(new OutputStreamWriter(out, "UTF-8"));
		  writer.setIndent("  ");
		  writer.beginObject();
		  
			//�˼ƭp��
				time(writer);
				
	      }catch(Exception e) {
		  Log.e("log_tag", "Error saving string "+e.toString());
		  }

	}
	
	//�x�s���(�ϥΪ̦W��)
	protected void onStop(){
		super.onStop();	
		Title.this.finish();
	}
	
	public void time(JsonWriter writer){	
		// �˼ƭp��     
		mTextView = (TextView)findViewById(R.id.timeView1);
		new CountDownTimer(5000,1000){
		            
			
			public void onFinish(JsonWriter writer) {
				  
			// TODO Auto-generated method stub
				mTextView.setText("Time is up");
				//�x�s���
				preference.edit()
				.putString("title",edttitle.getText().toString())
				.commit();
				
				  try {
					writer.name(Integer.toString(i));
					writer.beginObject();
					writer.name(Integer.toString(self));   // �ޤJself
				    writer.value(edttitle.getText().toString());
				    writer.endObject();
				    writer.endObject();
				    writer.flush();
				    writer.close();
				} catch (IOException e) {
					Log.e("log_tag", "Somthing went wrong");
				}
				  
				
				
				//Deliver the title to Service
				Intent intent = new Intent(GameService.ACTION_GUESS);
				intent.putExtra("guess", preference.getString("data","unkown"));
				startService(intent);
				
				//����~��
				Intent intent2=new Intent();
				intent2.setClass(Title.this,Palette.class);
				Bundle bundle=new Bundle();
				bundle.putInt("round",i+1);
				bundle.putInt("self", self);
				intent2.putExtras(bundle);
				startActivity(intent2);
				
			}

			@Override
			public void onTick(long millisUntilFinished) {
			// TODO Auto-generated method stub
			mTextView.setText("seconds remaining:"+millisUntilFinished/1000);
			}

			@Override
			public void onFinish() {
				// TODO Auto-generated method stub
				
			}
			            
			}.start();
		}

			
	
	

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.title, menu);
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
	
	public boolean onKeyDown(int keyCode, KeyEvent event) {//������^��
        if ((keyCode == KeyEvent.KEYCODE_BACK)) {   
        	Toast.makeText(this, "You can't leave now.", Toast.LENGTH_SHORT).show();
            return true;   
        }   
        return super.onKeyDown(keyCode, event);   
    }
}
