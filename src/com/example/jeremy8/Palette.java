package com.example.jeremy8;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Message;
import android.util.JsonWriter;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

public class Palette extends Activity {
	private ImageView iv;
	private Bitmap baseBitmap;
	private Canvas canvas;
	private Paint paint;
	private TextView txtAns;
	private SharedPreferences preference, MAXpre;
	private String readAns;
	private TextView mTextView;//倒數計時
	private int i, MAX, self;
	private String MyGuess;
	private String senderGuess;
	private int MyPosition;
	private int senderPosition;
	private int[] draw;
	
	public static final int MESSAGE_GUESS = 0;
	public static final int MESSAGE_ENDGAME = 1;
	
	//任軒-json檔名-從LISTVIEW拿
	
	public ArrayList<Integer> coordinate=new ArrayList<Integer>(); 

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_palette);
		
		//Deliver this Activity's handler to service , so service can update this activity
		GameService.getPaletteHandler(mPaletteHandler);
		
		Intent intent=this.getIntent();
		Bundle bundle1=intent.getExtras();
		i=bundle1.getInt("round");
		self=bundle1.getInt("self");
		
		//取得介面元素
		txtAns=(TextView)findViewById(R.id.textView12);
		
		//尋找儲存檔
		preference=getSharedPreferences("guess",MODE_PRIVATE);
		MAXpre=getSharedPreferences("creatroom",MODE_PRIVATE);
		//拿資料
	    readAns=preference.getString("data","unknown");
		MAX = MAXpre.getInt("population", 7);
		String file = MAXpre.getString("roomname", "unknown");
		String FILENAME = file+".json";
		//顯示
		txtAns.setText(MyGuess);
		
		
		
		this.iv = (ImageView) this.findViewById(R.id.imageView3);
		// 创建一张空白图片
		baseBitmap = Bitmap.createBitmap(480, 640, Bitmap.Config.ARGB_8888);
		// 创建一张画布
		canvas = new Canvas(baseBitmap);
		// 画布背景为白色
		canvas.drawColor(Color.WHITE);
		// 创建画笔
		paint = new Paint();
		// 画笔颜色为黑色
		paint.setColor(Color.BLACK);
		// 宽度5个像素
		paint.setStrokeWidth(5);
		// 先将灰色背景画上
		canvas.drawBitmap(baseBitmap, new Matrix(), paint);
		iv.setImageBitmap(baseBitmap);
		
		int ini=self-i+1;  //引入self
		if(ini < 1)
			ini += MAX;
			
/*		try{
			
			 FileOutputStream out = openFileOutput(FILENAME, MODE_WORLD_READABLE);
		 
			 final JsonWriter writer = new JsonWriter(new OutputStreamWriter(out, "UTF-8"));
			 writer.setIndent("  ");
			 
			 writer.beginObject();
		     writer.name(Integer.toString(i));
		     writer.beginObject();
		     writer.name(Integer.toString(ini));
		     writer.beginArray();
*/		
		iv.setOnTouchListener(new OnTouchListener() { 
			  int startX; 
			  int startY;
			  
			  
			  @Override
			  public boolean onTouch(View v, MotionEvent event) { 
				  switch (event.getAction()) {  
				  case MotionEvent.ACTION_DOWN: 
					  // 获取手按下时的坐标 
					  startX = (int) event.getX();  
					  startY = (int) event.getY();
					  break; 
				  case MotionEvent.ACTION_MOVE:  
					// 获取手移动后的坐标
					  int stopX = (int) event.getX(); 
					  int stopY = (int) event.getY();  
					// 在开始和结束坐标间画一条线  
					  
					 canvas.drawLine(startX, startY, stopX, stopY, paint); 
					  //紀錄與傳輸座標  傳輸還在努力中
					  coordinate.add(startX);
					  coordinate.add(startY); 
					  coordinate.add(stopX); 
					  coordinate.add(stopX); 
				  
			/*		try {
						writer.value(startX);
						writer.value(startY);
						writer.value(stopX);
						writer.value(stopY);
					} catch (IOException e) {
						Log.e("log_tag", "Error saving string "+e.toString());
						e.printStackTrace();
					}
			*/			
					 
					// 实时更新开始坐标
					  startX = (int) event.getX(); 
					  startY = (int) event.getY();  
					  iv.setImageBitmap(baseBitmap);  
					  break;  
					  
				  }
				  return true;
			  }
		  });
		
		//倒數計時
				time(/*writer*/);
/*		}catch(Exception e) {
	  Log.e("log_tag", "Error saving string "+e.toString());
	  }
*/	}
	
	public void time(/*final JsonWriter writer*/){	
		// 倒數計時     
		mTextView = (TextView)findViewById(R.id.timeView2);
		new CountDownTimer(61000,1000){
		            
			@Override
			public void onFinish() {
			// TODO Auto-generated method stub
				mTextView.setText("Time is up");
				//儲存資料-未完成(任軒)
/*				try{
				writer.endArray();
				writer.endObject();
				writer.endObject();
				writer.flush();
				writer.close();
				}catch (IOException e) {
					Log.e("log_tag", "Somthing went wrong");
				}*/	
				
				draw=new int[coordinate.size()];
				for(int j=0;j<coordinate.size();j++){
					draw[j]=coordinate.get(j);
				}
				
				Intent intent = new Intent(GameService.ACTION_PALETTE);
				intent.putExtra("coordinate", draw);//<-------int[]
				startService(intent);
				
				//跳轉頁面
				Intent intent2=new Intent();
				intent2.setClass(Palette.this,Guess.class);
				Bundle bundle2=new Bundle();
				bundle2.putInt("round",i+1);
				bundle2.putInt("self", self);
				intent2.putExtras(bundle2);
				startActivity(intent2);
			}

			@Override
			public void onTick(long millisUntilFinished) {
			// TODO Auto-generated method stub
			mTextView.setText("seconds remaining:"+millisUntilFinished/1000);
			}

			}.start();
		}
	
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.palette, menu);
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
	
	
	
	private final Handler mPaletteHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {

			switch(msg.what) {
			case MESSAGE_GUESS:
				MyGuess = (String) msg.obj;
				MyPosition = msg.getData().getInt("MyPosition");
				txtAns.setText(MyGuess);
				int ini3=MyPosition-i+1;
				if(ini3 < 1)
					ini3 += MAX;
				
				/*try{
					FileOutputStream tempf = openFileOutput("temp.json", MODE_WORLD_READABLE);
					  JsonWriter temp = new JsonWriter(new OutputStreamWriter(tempf, "UTF-8"));
					temp.setIndent("  ");
					temp.beginObject();
					temp.name(Integer.toString(i-1));
					temp.beginObject();
					temp.name(Integer.toString(ini3)).value(MyGuess);
					temp.endObject();
					temp.endObject();
					temp.flush();
					temp.close();
					
				}catch(Exception e) {
					  Log.e("log_tag", "Error saving string "+e.toString());
				  }*/
				break;
			case MESSAGE_ENDGAME:
				senderGuess = (String) msg.obj;
				senderPosition = msg.getData().getInt("senderPosition");
				
				int ini2=senderPosition-i+2;
				if(ini2 < 1)
					ini2 += MAX;
						
	/*			try{
					FileOutputStream tempf = openFileOutput("temp.json", MODE_WORLD_READABLE);
					  JsonWriter temp = new JsonWriter(new OutputStreamWriter(tempf, "UTF-8"));
					temp.setIndent("  ");
					temp.beginObject();
					temp.name(Integer.toString(i-1));
					temp.beginObject();
					temp.name(Integer.toString(ini2)).value(senderGuess);
					temp.endObject();
					temp.endObject();
					temp.flush();
					temp.close();
					
				}catch(Exception e) {
					  Log.e("log_tag", "Error saving string "+e.toString());
				}*/
				break;
			}
		}
	};
	
	public boolean onKeyDown(int keyCode, KeyEvent event) {//捕捉返回鍵
        if ((keyCode == KeyEvent.KEYCODE_BACK)) {   
        	Toast.makeText(this, "You can't leave now.", Toast.LENGTH_SHORT).show();
            return true;   
        }   
        return super.onKeyDown(keyCode, event);   
    }
}

