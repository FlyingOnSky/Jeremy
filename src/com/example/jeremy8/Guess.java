package com.example.jeremy8;

import java.io.FileOutputStream;
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
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

public class Guess extends Activity {
	private SharedPreferences preference, MAXpre;
	private EditText edtguess;
	private TextView mTextView;//倒數計時
	private int i, MAX, self;
	public String file;
	public String FILENAME;
	public FileOutputStream out;
    public JsonWriter writer;
	//
	private ImageView iv;
	private Bitmap baseBitmap;
	private Canvas canvas;
	private Paint paint;
	private float[] MyPalette;  // <--------------- 接收資料存放處
	private int MyPosition;
	private float[] senderPalette;
	private int senderPosition;
	
	//ArrayList<Integer> gameArrayP;
	//ArrayList<Integer> gameArrayN;
	
	public static final int MESSAGE_PALETTE = 0;
	public static final int MESSAGE_ENDGAME = 1;
	public static final int MESSAGE_TEST = 2; //**
	//
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_guess);
		
		//Deliver this Activity's handler to service , so service can update this activity
		GameService.getGuessHandler(mGuessHandler);
		
		Intent intent1=this.getIntent();
		Bundle bundle1=intent1.getExtras();
		i=bundle1.getInt("round");
		self=bundle1.getInt("self");

		
		//取得介面元件
		edtguess=(EditText)findViewById(R.id.editText6);
		
		preference=getSharedPreferences("guess",MODE_PRIVATE);
		MAXpre=getSharedPreferences("creatroom",MODE_PRIVATE);
		MAX = MAXpre.getInt("population", 7);
		String file = MAXpre.getString("roomname", "unknown");  // <------ 房間名稱+self
		String FILENAME = file+".json";
		
		this.iv = (ImageView) this.findViewById(R.id.imageView4);
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
		writer.name(Integer.toHexString(ini));
*/	    
		//倒數計時
		time(writer);
/*		}catch(Exception e) {
			  Log.e("log_tag", "Error saving string "+e.toString());
			  }*/
	}
	
	public void time(final JsonWriter writer){	
		// 倒數計時     
		mTextView = (TextView)findViewById(R.id.timeView3);
		new CountDownTimer(31000,1000){
				
			@Override
			public void onFinish() {
			// TODO Auto-generated method stub
				mTextView.setText("Time is up");
		/*		preference.edit().remove("guess");
		   	    preference.edit()
				.putString("data",edtguess.getText().toString())
				.commit();*/
		/*		try{
				//----------- title ----------
					writer.value(edtguess.getText().toString());  // <----- 改成題目變數
					writer.endObject();
					writer.beginObject();
					writer.flush();
					writer.close();   
				}catch(Exception e) {
					  Log.e("log_tag", "Error saving string "+e.toString());
				  }
		*/
				//Deliver guess to GameService , then GameService will send it out
				//**Intent intent = new Intent(GameService.ACTION_GUESS);
				//**intent.putExtra("guess", preference.getString("data","unkown"));
				//**startService(intent);
				
		
		
				//跳轉業面
				if(i+1>7){
					Intent intent2=new Intent();
					intent2.setClass(Guess.this,EndGame.class);
					startActivity(intent2);
				}else{
				   Intent intent3=new Intent();
				   intent3.setClass(Guess.this,Palette.class);
				   Bundle bundle2=new Bundle();
				   bundle2.putInt("round",i+1);
				   bundle2.putInt("self", self);
				   intent3.putExtras(bundle2);
				   startActivity(intent3);
				}
			}

			@Override
			public void onTick(long millisUntilFinished) {
			// TODO Auto-generated method stub
			mTextView.setText("seconds remaining:"+millisUntilFinished/1000);
			}
			            
			}.start();
		}
	
	//儲存資料(使用者名稱)
	protected void onStop(){
		super.onStop();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.guess, menu);
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
	
	private final Handler mGuessHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			
			switch(msg.what) {
			case MESSAGE_PALETTE:
				//print this palette out and store it at EndGame Activity
				MyPalette =(float[]) msg.obj;
				MyPosition = msg.getData().getInt("MyPosition");
				//**Toast.makeText(getApplicationContext(), "Length:"+MyPalette.length, Toast.LENGTH_SHORT).show();
				//**Toast.makeText(getApplicationContext(), "1:"+MyPalette[0]+",2:"+MyPalette[1], Toast.LENGTH_SHORT).show();
				
				
				
				
				canvas.drawLines(MyPalette,0,MyPalette.length, paint);
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
					temp.name(Integer.toString(ini3));
					temp.beginArray();
					for(int j = 0; j < MyPalette.length; j++,temp.endObject())
					{
						temp.value(MyPalette[j]);
					}
					temp.endArray();
					temp.endObject();
					temp.endObject();
					temp.flush();
					temp.close();
					
				}catch(Exception e) {
					Log.e("log_tag", "Error saving string "+e.toString());
				}
				*/
				break;
			case MESSAGE_ENDGAME:
				//store this palette at EndGame Activity
				senderPalette = (float[]) msg.obj;
				senderPosition = msg.getData().getInt("senderPosition");
				int ini2 = senderPosition-i+2;
				if(ini2 < 1)
					ini2 += MAX;
				/*try{
					FileOutputStream tempf = openFileOutput("temp.json", MODE_WORLD_READABLE);
					  JsonWriter temp = new JsonWriter(new OutputStreamWriter(tempf, "UTF-8"));
					temp.setIndent("  ");
					temp.beginObject();
					temp.name(Integer.toString(i-1));
					temp.beginObject();
					temp.name(Integer.toString(ini2));
					temp.beginArray();
					for(int j = 0; j < senderPalette.length; j++,temp.endObject())
					{
						temp.value(senderPalette[j]);
					}
					temp.endArray();
					temp.endObject();
					temp.endObject();
					temp.flush();
					temp.close();
					
				}catch(Exception e) {
					Log.e("log_tag", "Error saving string "+e.toString());
				}
				*/
				break;
			case MESSAGE_TEST: //**
				byte[] a =(byte[]) msg.obj;
				//Extract the coordinate it contained
       		 	byte[] palette = new byte[a.length-35];
       		 	System.arraycopy(a,35,palette,0,a.length-35);
   			 
       		 	//Convert byte[] to int[]
       		 	int[] intPalette = new int[palette.length / 4];
       		 	for(int i=0 ; i<palette.length / 4; i++) {
       		 		byte[] XorY = new byte[4];
       		 		System.arraycopy(palette, i*4, XorY, 0, 4);
       		 		intPalette[i] = byteArrayToInt(XorY);
       		 	}
   			 
       		 	//Convert int[] to float[] (to print it out)
       		 	float[] floatPalette = new float[intPalette.length];
       		 	for(int i=0; i<intPalette.length ; i++) {
       		 		floatPalette[i] = intPalette[i];
       		 	}
       		 	
       		 	canvas.drawLines(floatPalette,0,floatPalette.length, paint);
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
	
	public static int byteArrayToInt(byte[] b) //**
	{
	    return (b[3] & 0xFF) + ((b[2] & 0xFF) << 8) + ((b[1] & 0xFF) << 16) + ((b[0] & 0xFF) << 24);
	}
}

