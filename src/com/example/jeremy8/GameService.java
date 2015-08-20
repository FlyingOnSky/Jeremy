package com.example.jeremy8;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.util.Log;

public class GameService extends Service {
	// Debugging
    private static final String TAG = "ChatService";
    private static final boolean D = true;
    
	// Actions that start the service
    public static final String ACTION_START = "com.example.jeremy8.action.START";
	public static final String ACTION_CANCEL = "com.example.jeremy8.action.CANCEL";
	public static final String ACTION_CREATE_ROOM = "com.example.jeremy8.action.CREATE_ROOM";
	public static final String ACTION_JOIN_ROOM = "com.example.jeremy8.action.JOIN_ROOM";
	public static final String ACTION_GUESS = "com.example.jeremy8.action.GUESS";
	public static final String ACTION_PALETTE = "com.example.jeremy8.action.PALETTE";
	
	// Service's state
	enum State{
    	Stopped,
    	Started
    };
    static State mState = State.Stopped;
    
    // Runnable
    private static DoDiscovery doDiscovery;
    
    // local Bluetooth Adapter
    private BluetoothAdapter mBluetoothAdapter;
    // Address list in our bluetooth net
    private ArrayList<String> mAddressList;
    // Address list that record device who I tried to connect but fail
    private ArrayList<String> mFailedAddressList;
    
	// Name for the SDP record when creating server socket
    private static final String NAME = "ChatMulti";
    
    //Handler
    private static Handler mOutSideHandler;
    private static Handler mGameRoomHandler;
    private static Handler mGuessHandler;
    private static Handler mPaletteHandler;
    
    //SharedPreferences
    SharedPreferences preference;
    
	// Member fields
    private AcceptThread mAcceptThread;
    private ConnectThread mConnectThread;
    private ConnectedThread mConnectedThread;
    private PauseThread mPauseThread;
    private ReAcceptThread mReAcceptThread;
    private ReConnectThread mReConnectThread;
    
    private ArrayList<String> mDeviceAddresses; //record the device's address that make a connect to you (maximum:3)
    private ArrayList<ConnectedThread> mConnThreads;
    private ArrayList<AcceptThread> mAcceThreads;
    private ArrayList<BluetoothSocket> mSockets;
    private ArrayList<UUID> mUUIDs;
    private ArrayList<String> mGameAddressList; //record all the address who is in the game room
    private ArrayList<String> mGameNameList; //record all the device's name who is in the game room
    
    //record how many AcceptThread is used , if two AcceptThread is used , then cancel all the AcceptThread
    private int AcceptAmount = 0;
    //record which UUID is used in ConnectThread
    private int connectUUID = -1;
    //record whether I find a device
    private boolean findDevice;
    //record the room's id I get in
    private String MyRoomID;
    //record my own address/name
    private String MyAddress;
    private String MyName;
    //record my position in the gamer list
    private int MyPosition;
    //record who is the target to send the guess and palette
    private String targetAddress;
    
	@Override
	public void onCreate() {
		super.onCreate();
		
		// Register for broadcasts when a device is discovered
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        this.registerReceiver(mReceiver, filter);
        
        //SharedPreference
        preference = getSharedPreferences("username",MODE_PRIVATE);
        
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        MyAddress = mBluetoothAdapter.getAddress();
        MyName = preference.getString("name","unknown");
        MyRoomID = null;
        mAcceThreads = new ArrayList<AcceptThread>();
        mConnThreads = new ArrayList<ConnectedThread>();
        mDeviceAddresses = new ArrayList<String>();
        mSockets = new ArrayList<BluetoothSocket>();
        mUUIDs = new ArrayList<UUID>();
        // 3 randomly-generated UUIDs. These must match on both server and client.
        // two for accept a connection and one for request a connection 
        mUUIDs.add(UUID.fromString("b7746a40-c758-4868-aa19-7ac6b3475dfc"));
        mUUIDs.add(UUID.fromString("2d64189d-5a2c-4511-a074-77f199fd0834"));
        mUUIDs.add(UUID.fromString("e442e09a-51f3-4a7b-91cb-f638491d1412"));
        mFailedAddressList = new ArrayList<String>();
        mAddressList = new ArrayList<String>();
        mAddressList.add(mBluetoothAdapter.getAddress());
        mGameAddressList = new ArrayList<String>();
        mGameNameList = new ArrayList<String>();
        
        for(int i=0;i<3;i++) {
        	 mConnThreads.add(null);
             mDeviceAddresses.add(null);
             mSockets.add(null);
        }
	}
	
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		String action = intent.getAction();
		
		if(action == GameService.ACTION_START) {
			// Cancel any thread attempting to make a connection
	        if (mConnectThread != null) {mConnectThread.cancel(); mConnectThread = null;}

	        // Cancel any thread currently running a connection
	        if (mConnectedThread != null) {mConnectedThread.cancel(); mConnectedThread = null;}

	        // Start 3 thread to listen on a BluetoothServerSocket
	        if (mAcceptThread == null) {
	        	for(int i=0;i<3;i++) {
	        		mAcceptThread = new AcceptThread(i);
	                mAcceptThread.start();
	                mAcceThreads.add(mAcceptThread);
	        	}
	        }
	        
			mState = State.Started;
			doDiscovery = new DoDiscovery();
			new Thread(doDiscovery).start();
			
		} else if(action == GameService.ACTION_CREATE_ROOM) {
			String roomName = intent.getStringExtra("roomname");
			int population = intent.getIntExtra("population",0);
			
			//Record the roomID , which is my own address
			MyRoomID = mBluetoothAdapter.getAddress();
			
			//Convert int to String
			String stringPopulation = Integer.toString(population);
			//str = "roomName,population"
			String str = roomName + "," + stringPopulation;
			
			//Convert str to byte
			byte[] byt = str.getBytes();
			//Convert my address to byte
			byte[] Myaddr = mBluetoothAdapter.getAddress().getBytes();
			
			//out = {1 Myaddress "roomName,population"}
			byte[] out = new byte[byt.length + Myaddr.length + 1];
			out[0] = 1; //1 represent "create room" message
			System.arraycopy(Myaddr,0,out,1,17);//address has 17 byte[]
			System.arraycopy(byt,0,out,18,byt.length);
			write(out);
			
		} else if(action == GameService.ACTION_JOIN_ROOM) {
			String roomID = intent.getStringExtra("roomID");
			String addressName = mBluetoothAdapter.getAddress() + "," //addressName = "address,name"
					+ getSharedPreferences("username",MODE_PRIVATE).getString("name","unknown");
			
			//Record the room's id I get in
			MyRoomID = roomID;
			
			//Convert data to byte
			byte[] byteRoomID = roomID.getBytes();
			byte[] byteAddressName = addressName.getBytes();
			
			//out = {1 roomID "MyAddress,MyName"}
			byte[] out = new byte[byteRoomID.length + byteAddressName.length + 1];
			out[0] = 2; //2 represent "join room" message
			System.arraycopy(roomID,0,out,1,17);//roomID has 17 byte[]
			System.arraycopy(byteAddressName,0,out,18,byteAddressName.length);
			write(out);
			
		} else if(action == GameService.ACTION_GUESS) {
			String guess = intent.getStringExtra("guess");
			byte[] byteGuess = guess.getBytes();
			
			//we want to deliver it to the target whose position in gamer list is one bigger than me
			for(int i=0; i<mGameAddressList.size(); i++) {
				//find my position in the gamer list
				if(MyAddress.equals(mGameAddressList.get(i))) {
					MyPosition = i;//record my position in the gamer list
					if(i+1 != mGameAddressList.size()) { //if I am not the last one in the gamer list
						targetAddress = mGameAddressList.get(i+1);
					} else {
						targetAddress = mGameAddressList.get(0);
					}
					break;
				}
			}
			
			//out = {5 roomID targetAddress "guess"}
			byte[] out = new byte[byteGuess.length + 35];
			out[0] = 5;//5 represent "guess" message
			System.arraycopy(MyRoomID,0,out,1,17);
			System.arraycopy(targetAddress,0,out,18,17);
			System.arraycopy(byteGuess,0,out,35,byteGuess.length);
			write(out);
			
		} else if(action == GameService.ACTION_PALETTE) {
			int[] coordinate = intent.getIntArrayExtra("coordinate");
			byte[] palette = new byte[coordinate.length*4];
			
			//Convert int[] to byte[]
			for(int i=0; i<palette.length; i++) {
				byte[] XorY = intToByteArray(coordinate[i]);
				System.arraycopy(XorY,0,palette,i*4,4);
			}
			
			//separate it and send each part out
			if(palette.length > 989) {
				//out = {7 6 (0.1) roomID targetAddress "coordinate"}
				if(palette.length % 987 == 0) {
					byte[][] out = new byte[palette.length / 987][1024];
					
					for(int i=0 ; i<palette.length / 987 ; i++) {
						out[i][0] = 7;//7 means this message is separated
						out[i][1] = 6;//6 means this message is a palette coordinate 
						if(i+1 != palette.length / 987) 
							out[i][2] = 0;//0 means this message isn't the last part
						else
							out[i][2] = 1;//1 means this message is the last part
						
						//roomID , targetAddress
						System.arraycopy(MyRoomID,0,out[i],3,17);
						System.arraycopy(targetAddress,0,out[i],20,17);
						
						//palette coordinate
						System.arraycopy(palette,i*987,out[i],37,987);
						write(out[i]);
					}
				} else {
					//out = {7 6 (0.1) roomID targetAddress "coordinate"}
					byte[][] out = new byte[palette.length / 987 + 1][1024];
					int part = palette.length / 987 + 1 ;// to record how many part it will separate
					
					for(int i=0 ; i<palette.length / 987 ; i++) {
						out[i][0] = 7;//7 means this message is separated
						out[i][1] = 6;//6 means this message is a palette coordinate
						out[i][2] = 0;//0 means this message isn't the last part
						//roomID . targetAddress
						System.arraycopy(MyRoomID,0,out[i],3,17);
						System.arraycopy(targetAddress,0,out[i],20,17);
						
						//palette coordinate
						System.arraycopy(palette,i*987,out[i],37,987);
						write(out[i]);
					}
					
					byte[] lastPart = new byte[palette.length % 987 + 37];
					lastPart[0] = 7;//7 means this message is separated
					lastPart[1] = 6;//6 means this message is a palette coordinate
					lastPart[2] = 1;//0 means this message isn't the last part
					//roomID . targetAddress
					System.arraycopy(MyRoomID,0,lastPart,3,17);
					System.arraycopy(targetAddress,0,lastPart,20,17);
					
					//palette coordinate
					System.arraycopy(palette,(part-1) * 987,lastPart,37,palette.length % 987);
					write(lastPart);
				}
				
			} else {
				//out = {6 roomID targetAddress "coordinate"}
				byte[] out = new byte [palette.length + 35];
				out[0] = 6;//6 represent "palette" message
				System.arraycopy(MyRoomID,0,out,1,17);
				System.arraycopy(targetAddress,0,out,18,17);
				System.arraycopy(palette,0,out,35,palette.length);
				write(out);
			}
			
		} else if(action == GameService.ACTION_CANCEL) {
			mState = State.Stopped;
			cancel();
		}
		
		return START_NOT_STICKY;
	}
	
	public class DoDiscovery implements Runnable {
		@Override
		 public void run() {
			findDevice = false;
			while(!findDevice) {
				// If we're already discovering, stop it
				if (mBluetoothAdapter.isDiscovering()) {
					mBluetoothAdapter.cancelDiscovery();
				}

				// Request discover from BluetoothAdapter
				mBluetoothAdapter.startDiscovery();
				
				try {
					Thread.sleep(60000);
				} catch(InterruptedException e) {}
			}
		}
	}
	
	private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            // When discovery finds a device
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                // Get the BluetoothDevice object from the Intent
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                
                
                boolean connectable = true;
                //Check if this device is in our bluetooth net already
                for(int i=0;i<mAddressList.size();i++) {
                	if(mAddressList.get(i).equals(device.getAddress()))
                		connectable = false;
                }
                //Check if this device I had tried to connect but fail
                for(int i=0;i<mFailedAddressList.size();i++) {
                	if(mFailedAddressList.get(i).equals(device.getAddress()))
                		connectable = false;
                }
                if(connectable) {
	                //Cancel discovery and discover loop,then connect the device
	                mBluetoothAdapter.cancelDiscovery();
	                findDevice = true;
	                connect(device);
                }
            }
        }
	};
	
	public synchronized void cancel() {
	     if (D) Log.d(TAG, "stop");
	     findDevice = true;
	     if (mConnectThread != null) {mConnectThread.cancel(); mConnectThread = null;}
	     for(int i=0;i<3;i++) {
	    	 if(mAcceThreads.get(i) != null) {mAcceThreads.get(i).cancel(); mAcceThreads.set(i,null);}
	    	 if(mConnThreads.get(i) != null) {mConnThreads.get(i).cancel(); mConnThreads.set(i,null);}
	     }
	     if(mReAcceptThread != null) {mReAcceptThread.cancel(); mReAcceptThread = null;}
	     if(mReConnectThread != null) {mReConnectThread.cancel(); mReConnectThread = null;} 
	 }
	 
	 
	 public synchronized void connect(BluetoothDevice device) {
	     if (D) Log.d(TAG, "connect to: " + device);

	     // Cancel any thread attempting to make a connection
	     if (mConnectThread != null) {mConnectThread.cancel(); mConnectThread = null;}

	     // Create a new thread and attempt to connect to each UUID one-by-one.    
	     try {
            mConnectThread = new ConnectThread(device);
            
            //PauseThread : if not being interrupted , it will stop 0.5 second and then call ConnectThread.start()
            mPauseThread = new PauseThread();
            mPauseThread.start();
    	} catch (Exception e) {}
	 }
	 
	 public synchronized void connected(BluetoothSocket socket, BluetoothDevice device,int i) {
	     if (D) Log.d(TAG, "connected");
	     
	     //Record device's address who directly connect to you 
	     mSockets.set(i,socket);
		 mDeviceAddresses.set(i,socket.getRemoteDevice().getAddress());

	     // Start the thread to manage the connection and perform transmissions
	     mConnectedThread = new ConnectedThread(socket,i);
	     mConnectedThread.start();
	     // Add each connected thread to an array
	     mConnThreads.set(i,mConnectedThread);
	     
	     //Write my address list to that device,and if that device has connected to the other device
	     //then the address list will pass on those device too.
	     write_address(device,i);
	    }
	
	private void connectionLost(int i) {   //the lost socket was created in ConnectThread
	    if(i == connectUUID) { 
	    	//Try to re-connect it
	    	mReConnectThread  = new ReConnectThread(mConnectThread.getDevice(),mConnectThread.getNumber());
	    	mReConnectThread.start();
	    } else { //the lost socket was created in AcceptThread
	    	AcceptAmount--;
	    	//Try to re-accept it
	    	mReAcceptThread = new ReAcceptThread(i);
	    	mReAcceptThread.start();
	    }
	}
	
	public void write(byte[] out) {
    	// When writing, try to write out to all connected threads 
    	for (int i = 0; i < 3; i++) {
    		try {
                // Create temporary object
                ConnectedThread r;
                // Synchronize a copy of the ConnectedThread
                synchronized (this) {
                    r = mConnThreads.get(i);
                }
                // Perform the write unsynchronized
                r.write(out);
    		} catch (Exception e) {    			
    		}
    	}
    }
    
    public void write_address(BluetoothDevice device,int i) {
    	ConnectedThread r;
    	synchronized (this) {
            r = mConnThreads.get(i);
		}
    	
    	//Combine all the address(String) to a String writen as : address1,address2 ....
    	String addressString=mAddressList.get(0); //my address
    	
    	for(int j=1;j<mAddressList.size();j++) {
    		if(mAddressList.get(j)!=device.getAddress()) { //we don't want to deliver his address to himself
    			addressString+=",";
    			addressString+=mAddressList.get(j);
    		}
    	}
    	
    	//Convert the String address to byte address
    	byte[] addressByte = addressString.getBytes();
    	
    	//separate it and send each part out
    	if(addressByte.length > 1023) {
    		//out = {7 0 (0,1) "address1,address2 ..."}
    		if(addressByte.length % 1021 == 0) { 
				byte[][] out = new byte[addressByte.length / 1021][1024];
				
				for(int j=0 ; j<addressByte.length / 1021 ; j++) {
					out[j][0] = 7;//7 means this message is separated
					out[j][1] = 0;//0 means this message is a address list
					if(j+1 != addressByte.length / 1021) 
						out[j][2] = 0;//0 means this message isn't the last part
					else
						out[j][2] = 1;//1 means this message is the last part
					
					//address list
					System.arraycopy(addressByte,j*1021,out[j],3,1021);
					r.write(out[j]);
				}
			} else {
				//out = {7 0 (0,1) "address1,address2 ..."}
				byte[][] out = new byte[addressByte.length / 1021 + 1][1024];
				int part = addressByte.length / 1021 + 1 ;// to record how many part it will separate
				
				for(int j=0 ; j<addressByte.length / 1021 ; j++) {
					out[j][0] = 7;//7 means this message is separated
					out[j][1] = 0;//0 means this message is address list
					out[j][2] = 0;//0 means this message isn't the last part
					
					//address list
					System.arraycopy(addressByte,j*1021,out[j],3,1021);
					r.write(out[j]);
				}
				
				byte[] lastPart = new byte[addressByte.length % 1021 + 3];
				lastPart[0] = 7;//7 means this message is separated
				lastPart[1] = 0;//0 means this message is address list
				lastPart[2] = 1;//0 means this message isn't the last part
				
				//address list
				System.arraycopy(addressByte,(part-1) * 1021,lastPart,3,addressByte.length % 1021);
				r.write(lastPart);
			}
    	}
    }
    
    private class PauseThread extends Thread {
    	public void run() {
    		try {
    			sleep(500);
    			mConnectThread.start();
    		}catch(InterruptedException e) {}
    	}
    }
		
	private class AcceptThread extends Thread {
    	BluetoothServerSocket serverSocket = null;
        BluetoothSocket socket = null;
    	int I=0;
        
        public AcceptThread(int i) {
        	I=i;
        }

        public void run() {
            if (D) Log.d(TAG, "BEGIN mAcceptThread" + this);
            setName("AcceptThread");
            try {
            	serverSocket = mBluetoothAdapter.listenUsingInsecureRfcommWithServiceRecord(NAME, mUUIDs.get(I));
                socket = serverSocket.accept();
               
                if (socket != null) {
                	//Check whether I had found a device
                	if(mConnectThread != null) {
                		//Check whether I am running ConnectThread now
                		if(mConnectThread.isAlive()) {
                			//Check whether this device is the same device that connecting me
                			if(socket.getRemoteDevice().getAddress().equals(mConnectThread.getAddress())) {
                				//Compare the first char of my address and his address
                				//the bigger one running ConnectThread , the smaller one running AcceptThread
                				char mine=mBluetoothAdapter.getAddress().charAt(0),
                						yours= socket.getRemoteDevice().getAddress().charAt(0); 
                				if(mine > yours) {
                					Interrupt();
                					sleep(1000); //sleep so I can be interrupted
                				} else {
                					mConnectThread.interrupt();
                				}
                			}else {//I am connecting other device while I being connected
                				//Close the ConnectThread first, because it may be one of our bluetooth net member
                				mConnectThread.interrupt();
                			}
                		} else { //I got a device but I am not running ConnectedThread yet(stop for 0.5 second)
                			//stop run connect(device)
                			mPauseThread.interrupt();
                		}
                	} else { //I haven't found a device
                		//stop discovery
                		mBluetoothAdapter.cancelDiscovery();
                	}
                 	
                	connected(socket, socket.getRemoteDevice(),I);
                	AcceptAmount++;
                }
            
                cancel(); //To prevent another device connect us by this UUID
                if (D) Log.i(TAG, "END mAcceptThread");
            
                //If I have accepted 2 connection , then close all AcceptThread's severSocket
                if(AcceptAmount >=2 ) {
                	AcceptThread a;
                	synchronized (this) {
                		for(int i=0;i<3;i++) {
                			a = mAcceThreads.get(i);
                			if(a != null) {
                				a.cancel();
                				a = null;
                			}
                		}
                	}
                }
            } catch(InterruptedException e) {
            	cancel();
        		try{
        			socket.close();
        		} catch(IOException e2) {}
        	} catch (IOException e) {
        		cancel();
        		Log.e(TAG, "accept() failed", e);
        	}
        }

        public void cancel() { 
            if (D) Log.d(TAG, "cancel " + this);
            try {
                serverSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "close() of server failed", e);
            }
        }
        
        //sleep 0.1 second then interrupt this AcceptThread
        public void Interrupt() {
        	new Thread() {
        		public void run(){
        			try {
        				sleep(100);
        			}catch (InterruptedException e) {}
        		}
        	}.start();
        	interrupt();
        }
        
    }
	
	private class ConnectThread extends Thread {
        private  BluetoothSocket mmSocket;
        private final BluetoothDevice mmDevice;
        boolean connect_success;
        int I=0;

        public ConnectThread(BluetoothDevice device) {
            mmDevice = device;
        }
        
        public void run() {
            // Always cancel discovery because it will slow down a connection
            mBluetoothAdapter.cancelDiscovery();
            
            try {
            	for(int i=0;i<3;i++) {
            		I=i;
            		connect_success = true;
                	
            		try {
            			mmSocket = mmDevice.createInsecureRfcommSocketToServiceRecord(mUUIDs.get(i));

            			//stop 0.5 second before and after connect , so I might be able to be interrupted
            			sleep(500); //In case AccepetThread has started at this moment
            			mmSocket.connect();
                    	sleep(500); //In case we connect to each other 
            		} catch (IOException e) {
            			connect_success = false;
            			try {
            				mmSocket.close();
            			} catch (IOException e2) {
            				Log.e(TAG, "unable to close() socket during connection failure", e2);
            			}
            		}
                	
            		//If socket connect success , jump out the loop
            		if(connect_success) {
            			//record which UUID is used to connect other device
            			connectUUID = I;
            			//close the UUID[I]'s AcceptThread , because I have used UUID[I] to connect other device
            			synchronized (mAcceThreads.get(I)) {
            				AcceptThread a = mAcceThreads.get(I);
            				a.cancel();
            				a = null;
            				mAcceThreads.set(I,null);
            			}
            			break;
            		}
            	}
                
            	//If all connect fail , jump out the ConnectThread 
            	if(!connect_success) {
            		// start discovery again
            		mFailedAddressList.add(mmDevice.getAddress());
            		
            		// Reset the ConnectThread because this is failed(invalid) one
            		synchronized (GameService.this) {
            			mConnectThread = null;
            		}
            		return;
            	}

            	//If connect success , start the ConnectedThread
            	connected(mmSocket, mmDevice,I);
            } catch(InterruptedException e) {
            	try{
            		mmSocket.close();
            	} catch(IOException e2) {}
            }
        }
        
        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "close() of connect socket failed", e);
            }
        }
        
        public BluetoothDevice getDevice() {
        	return mmDevice;
        }
        
        public int getNumber() {
        	return I;
        }
        
        public String getAddress() {
        	return mmDevice.getAddress();
        }
        
    }
	
	private class ConnectedThread extends Thread{
		 private final BluetoothSocket mmSocket;
	     private final InputStream mmInStream;
	     private final OutputStream mmOutStream;
	     int I=0;
	     
	     //Use a bufferList to store all the separated part of message
	     ArrayList<byte[]> bufferList = new ArrayList<byte[]>();
	        
		 public ConnectedThread(BluetoothSocket socket,int i){
			 Log.d(TAG, "create ConnectedThread");
			 
			 mmSocket = socket;
			 InputStream tmpIn = null;
	         OutputStream tmpOut = null;
	         I=i;
	         
	         try{
	        	 tmpIn = mmSocket.getInputStream();
	        	 tmpOut = mmSocket.getOutputStream();
	         } catch(IOException e){
	        	 Log.e(TAG, "temp sockets not created", e);
	         }
	         
	         mmInStream = tmpIn;
	         mmOutStream = tmpOut;
		 }
	       
		 public void run() {
			 Log.i(TAG, "BEGIN mConnectedThread");
			 byte[] buffer = new byte[1024];
			 int bytes;
	         
        	 
        	 
			 while(true){
				 try{
					 // Read from the InputStream
					 bytes = mmInStream.read(buffer);
	                 
					 //We tag the data before send it out, now we need to distinguish them
		             switch(buffer[0]) {
		             case 0: //0 represent a address list , we need to add this to mAddressList
		            	 //Ignore 0 and convert it to string
		            	 String addressList = new String(buffer,1,buffer.length-1);
		            	 //addressList : "address1,address2 ..."
		            	 String[] addressArray = addressList.split(",");
		            	 
		            	 //add the String[] to arraylist<String>
		            	 for(int j=0;j<addressArray.length;j++)  {
		            		 mAddressList.add(addressArray[j]);
		            	 }
		            	 break;
		             case 1://1 represent a message of create new room
		            	 //buffer = {1 roomID(founder's address) "roomName,population"}
		            	 
		            	 //Add my address/name to the game list
		            	 mGameAddressList.add(MyAddress);
		            	 mGameNameList.add(MyName);
		            	 
		            	 //Get the room's id(Founder's address)
		            	 byte[] roomID = new byte[17];
		            	 System.arraycopy(buffer,1,roomID,0,17);
		            	 String stringRoomID = new String(roomID);
		            	 
		            	 //Get the room's name and maximum population
		            	 byte[] roomNamePopulation = new byte[buffer.length-18];
		            	 System.arraycopy(buffer,18,roomNamePopulation,0,buffer.length-18);
		            	 String stringRoomNamePopulation = new String(roomNamePopulation);
		            	 
		            	 //strArray[0] = room name , strArray[1] = population
		            	 String[] strArray = stringRoomNamePopulation.split(",");
		            	 
		            	 //Send the data back to OutSide Activity
		            	 Message msg = mOutSideHandler.obtainMessage(OutSide.MESSAGE_NEW_ROOM);
	            		 Bundle bundle = new Bundle();
	            	     bundle.putString("roomID",stringRoomID);
	            	     bundle.putString("roomName",strArray[0]);
	            	     bundle.putString("population",strArray[1]);
	            	     msg.setData(bundle);
	            	     mOutSideHandler.sendMessage(msg);
		            	 break;
		             case 2://2 represent a message of "join room"
		            	 //buffer = {2 roomID "NewAddress,NewName"}
		            	 
		            	 //Get the room's id(Founder's address)
		            	 byte[] roomID2 = new byte[17];
		            	 System.arraycopy(buffer,1,roomID2,0,17);
		            	 stringRoomID = new String(roomID2);
		            	 
		            	 //Check whether I am the game room's founder
		            	 if(stringRoomID.equals(MyAddress) ) {
		            		 byte[] addressName = new byte[buffer.length-18];
		            		 System.arraycopy(buffer,18,addressName,0,buffer.length-18);
		            		 String stringAddressName = new String(addressName);
		            		 
		            		 //strArray[0] = address , strArray[1] = name
		            		 strArray = stringAddressName.split(",");
		            		 
		            		 //Add his address/name to the game list
		            		 mGameAddressList.add(strArray[0]);
		            		 mGameNameList.add(strArray[1]);
		            		 
		            		 for (int i = 0; i < 3; i++) {
		            	    	 try {
		            	    		 ConnectedThread r;
		            	             synchronized (this) {
		            	               	 r = mConnThreads.get(i);
		            	             }
		            	            
		            	             if(i == I) {
		            	            	 // 回傳遊戲室人員名單給新進人員
		            	            	 String GameAddressNameString;
		            	            	 
		            	            	 //GameAddressNameString = "address1,name1,address2,name2 ..."
		            	            	 GameAddressNameString = mGameAddressList.get(0) + ","
		            	            			 +mGameNameList.get(0);
		            	            	 for(int j=1; j<mGameAddressList.size(); j++) {
		            	            		 GameAddressNameString += ",";
		            	            		 GameAddressNameString += mGameAddressList.get(j);
		            	            		 GameAddressNameString += ",";
		            	            		 GameAddressNameString += mGameNameList.get(j);
		            	            	 }
		            	            	 
		            	            	 //Convert it to byte
		            	            	 byte[] GameAddressNameByte = GameAddressNameString.getBytes();
		            	            	 
		            	            	 if(GameAddressNameByte.length > 1006) {
		            	            		 //Separate it , because we can only deliver 1024 byte a time
		            	            		 //out = {7 3 (01) targetAddress "address1,name1..."}
		            	            		 if(GameAddressNameByte.length % 1004 != 0) {
		            	            			 byte[][] separate =
		            	            					 new byte[GameAddressNameByte.length/1004 + 1][1024];
		            	            			 for(int j=0 ; j<GameAddressNameByte.length/1004 ; j++) {
		            	            				 separate[j][0] = 7;// 7 represent this is a separated message
		            	            				 separate[j][1] = 3;// 3 represent a message of "gamer list"
		            	            				 separate[j][2] = 0;// 0 means this is not the last part
		            	            				 System.arraycopy(strArray[0],0,separate[j],3,17);
		            	            				 System.arraycopy(GameAddressNameByte
		            	            						 ,j*1004,separate[j],20,1004);
		            	            				 
		            	            				 r.write(separate[j]);
		            	            			 }
		            	            			 byte[] lastPart = new byte[GameAddressNameByte.length % 1004 + 20];
		            	            			 lastPart[0] = 7;// 7 represent this is a separated message
		            	            			 lastPart[1] = 3;// 3 represent a message of "gamer list"
		            	            			 lastPart[2] = 0;// 1 means this is the last part
		            	            			 System.arraycopy(strArray[0],0,lastPart,3,17);
		            	            			 System.arraycopy(GameAddressNameByte,
		            	            					 (GameAddressNameByte.length/1004)*1004,
		            	            					 lastPart,20,GameAddressNameByte.length % 1004);
		            	            			 
		            	            			 r.write(lastPart);
		            	            		 }
		            	            		 
		            	            	 } else {
		            	            		 //out1 = {3 targetAddress "address1,name1,address2,name2 ... "}
		            	            		 byte[] out1 =new byte[GameAddressNameByte.length + 18];
		            	            		 out1[0] = 3; // 3 represent a message of "gamer list"
		            	            		 System.arraycopy(strArray[0],0,out1,1,17);
		            	            		 System.arraycopy(GameAddressNameByte,0,out1,18
		            	            				 ,GameAddressNameByte.length);
		            	            		 
		            	            		 r.write(out1);
		            	            	 }
		            	            	 //-----------------------------------------------------------------
		            	            	 //將新進人員資料傳給廣播給現有遊戲室人員
		            	            	 byte[] out2 = new byte[addressName.length + 18];
		            	            	 out2[0] = 4;// 4 represent a message of "new gamer's data"
			            	    		 System.arraycopy(MyAddress,0,out2,1,17);
			            	    		 System.arraycopy(addressName,0,out2,18,addressName.length);
			            	    		
			            	    		 r.write(out2);
		            	            	 
			            	    	 } else { //將新進人員名單傳給廣播給現有人員
			            	    		 //out = {4 roomID "NewAddress,NewName" }
			            	    		 byte[] out = new byte[addressName.length + 18];
			            	    		 out[0] = 4;// 4 represent a message of "new gamer's data"
			            	    		 System.arraycopy(MyAddress,0,out,1,17);
			            	    		 System.arraycopy(addressName,0,out,18,addressName.length);
			            	    		
			            	    		 r.write(out);
			            	    	 }
		            	    	} catch(Exception e) {}
		            		 }
		            		 
		            	 } else {//I am not the game room's founder
		            		 //find which room this is and +1 to its exist population
		            		 mOutSideHandler.obtainMessage(OutSide.MESSAGE_JOIN_ROOM,-1,-1,stringRoomID)
		            		 	.sendToTarget();
		            	 }
		            	 
		            	 break;
		             case 3://3 represent a message of gamer list
		            	 //buffer = {3 targetAddress "address1,name1,address2,name2 ..."}
		            	 
		            	 byte[] targetAddress = new byte[17];
		            	 System.arraycopy(buffer,1,targetAddress,0,17);
		            	 
		            	 String stringTargetAddress = new String(targetAddress);
		            	 
		            	 if(stringTargetAddress.equals(MyAddress) ) {
		            		 byte[] addressName = new byte[buffer.length-18];
		            		 System.arraycopy(buffer, 18, addressName, 0, buffer.length-18);
		            		 
		            		 String stringAddressName = new String(addressName);
		            		 
		            		 mGameRoomHandler.obtainMessage(GameRoom.MESSAGE_GAMER_LIST,-1,-1,stringAddressName)
		            		 	.sendToTarget();
		            	 }
		            	 break;
		             case 4://4 represent a message of new gamer's data
		            	 //buffer = {4 roomID "NewAddress,NewName"}
		            	 
		            	 byte[] roomID3 =new byte[17];
		            	 System.arraycopy(buffer,1,roomID3,0,17);
		            	 
		            	 String stringRoomID3 = new String(roomID3);
		            	 
		            	 if(stringRoomID3.equals(MyRoomID)) {//if I am in this game room
		            		 byte[] addressName = new byte[buffer.length-18];
		            		 
		            		 String stringAddressName = new String(addressName);
		            		 String[] strArray2 = stringAddressName.split(",");//strArray2[0] = address , [1] = name
		            		 
		            		 if(!strArray2[0].equals(MyAddress)) {//if this address/name is not mine
		            			 Message msg2 = mGameRoomHandler.obtainMessage(GameRoom.MESSAGE_NEW_GAMER);
		            			 Bundle bundle2 = new Bundle();
		            			 bundle2.putString("address",strArray2[0]);
		            			 bundle2.putString("name", strArray2[1]);
		            			 msg2.setData(bundle2);
		            			 mGameRoomHandler.sendMessage(msg2); 
		            		 }
		            	 }
		            	 break;
		             case 5://5 represent a guess
		            	 //buffer = {5 roomID targetAddress "guess"}
		            	 byte[] roomID4 =new byte[17];
		            	 System.arraycopy(buffer,1,roomID4,0,17);
		            	 String stringRoomID4 = new String(roomID4);
		            	 
		            	 if(stringRoomID4.equals(MyRoomID)) {// if this message is in my game room
		            		 byte[] targetAddress2 = new byte[17];
		            		 System.arraycopy(buffer,18,targetAddress2,0,17);
		            		 String stringTargetAddress2 = new String(targetAddress2);
		            		 
		            		 byte[] guess = new byte[buffer.length-35];
	            			 System.arraycopy(buffer,35,guess,0,buffer.length-35);
	            			 String stringGuess = new String(guess);
		            		 
		            		 if(stringTargetAddress2.equals(MyAddress)) { //if this message is for me
		            			 //send this guess and my position in the gamer list to Palette Activity
		            			 Message msg2 
		            			 	= mPaletteHandler.obtainMessage(Palette.MESSAGE_GUESS,-1,-1,stringGuess);
		            			 
		            			 Bundle bundle2 = new Bundle();
		            			 bundle2.putInt("MyPosition", MyPosition);
		            			 msg2.setData(bundle2);
		            			 mPaletteHandler.sendMessage(msg2);
		            		 } else {
		            			 //send this guess and the sender's position in the gamer list to Palette Activity
		            			 Message msg2 
		            			 	= mPaletteHandler.obtainMessage(Palette.MESSAGE_ENDGAME,-1,-1,stringGuess);
		            			 
		            			 //Find the sender's position in game list
		            			 int senderPosition = -1;
		            			 for(int i=0 ; i<mGameAddressList.size() ; i++) {
		            				 if(stringTargetAddress2.equals(mGameAddressList.get(i)) ) {
		            					 if(i == 0){
		            						 senderPosition = mGameAddressList.size() - 1;
		            					 } else {
		            						 senderPosition = i - 1;
		            					 }
		            				 }
		            			 }
		            			 Bundle bundle2 = new Bundle();
		            			 bundle2.putInt("senderPosition",senderPosition);
		            			 msg2.setData(bundle2);
		            			 mPaletteHandler.sendMessage(msg2);
		            		 }
		            	 }
		            	 break;
		             case 6://6 represent a palette
		            	 //buffer = {6 roomID targetAddress "coordinate(int[])"}
		            	 byte[] roomID5 =new byte[17];
		            	 System.arraycopy(buffer,1,roomID5,0,17);
		            	 String stringRoomID5 = new String(roomID5);
		            	 
		            	 if(stringRoomID5.equals(MyRoomID)) {// if this message is in my game room
		            		 byte[] targetAddress3 = new byte[17];
		            		 System.arraycopy(buffer,18,targetAddress3,0,17);
		            		 String stringTargetAddress3 = new String(targetAddress3);
		            		 

	            			 //Extract the coordinate it contained
		            		 byte[] palette = new byte[buffer.length-35];
	            			 System.arraycopy(buffer,35,palette,0,buffer.length-35);
	            			 
	            			 //Convert byte[] to int[]
	            			 int[] intPalette = new int[palette.length / 4];
	            			 for(int i=0 ; i<palette.length / 4; i++) {
	            				 byte[] XorY = new byte[4];
	            				 System.arraycopy(palette, i*4, XorY, 0, 4);
	            				 intPalette[i] = byteArrayToInt(XorY);
	            			 }
		            		 
		            		 if(stringTargetAddress3.equals(MyAddress)) {//if this message is for me
		            			 //send this palette and my position in gamer list to Guess Activity
		            			 Message msg2 = mGuessHandler.obtainMessage(Guess.MESSAGE_PALETTE,-1,-1,intPalette);
		            			 
		            			 Bundle bundle2 = new Bundle();
		            			 bundle2.putInt("MyPosition",MyPosition);
		            			 msg2.setData(bundle2);
		            			 mGuessHandler.sendMessage(msg2);
		            		 } else {
		            			 //send this palette and sender's position in gamer list to Guess Activity
		            			 Message msg2 = mGuessHandler.obtainMessage(Guess.MESSAGE_ENDGAME,-1,-1,intPalette);
		            			 
		            			 //Find the sender's position in game list
		            			 int senderPosition = -1;
		            			 for(int i=0 ; i<mGameAddressList.size() ; i++) {
		            				 if(stringTargetAddress3.equals(mGameAddressList.get(i)) ) {
		            					 if(i == 0){
		            						 senderPosition = mGameAddressList.size() - 1;
		            					 } else {
		            						 senderPosition = i - 1;
		            					 }
		            				 }
		            			 }
		            			 Bundle bundle2 = new Bundle();
		            		     bundle2.putInt("senderPosition",senderPosition);
		            		     msg2.setData(bundle2);
		            		     mGuessHandler.sendMessage(msg2);
		            		 }
		            	 }
		            	 break;
		             case 7: //7 means this is a separated message
		            	 switch(buffer[1]) {
		            	 case 3: //3 represent a message of gamer list
			            	 //buffer = {7 3 (0,1) targetAddress "address1,name1,address2,name2 ..."}
		            		 
		            		 byte[] target = new byte[17];
		            		 System.arraycopy(buffer,4,target,0,17);
		            		 String stringTarget = new String(target);
		            		 
		            		//Check if this gamer list is for me
		            		 if(stringTarget.equals(MyAddress)) {
		            			 //Use a ArrayList to store all the separated part of this message
			            		 bufferList.add(buffer);
			            		 
			            		 if(buffer[2] == 1) {
			            			 //Combine all the separated gamer list to one
			            			 byte[] allByte = new byte[bufferList.size()*1004];
			            			 for(int i=0 ; i<bufferList.size(); i++) {
			            				 System.arraycopy(bufferList.get(i),20,allByte,allByte.length
			            						 ,bufferList.get(i).length - 20);
			            			 }
			            			 
			            			 //Convert it to string
			            			 String GamerList = new String(allByte);
			            			 
			            			 mGameRoomHandler.obtainMessage(GameRoom.MESSAGE_GAMER_LIST,-1,-1,GamerList)
				            		 	.sendToTarget();
			            			 
			            			 //Clear the bufferList
			            			 while(bufferList.size() != 0) {
			            				 bufferList.remove(0);
			            			 }
			            		 }
		            		 }
		            		 break;
		            	 case 6://6 represent a palette
			            	 //buffer = { 7 6 (0,1) roomID targetAddress "coordinate(int[])"}
		            		 
		            		 byte[] roomID6 = new byte[17];
		            		 System.arraycopy(buffer,4,roomID6,0,17);
		            		 String stringRoomID6 = new String(roomID6);
		            		 
		            		 //Check if this message is in my game room
		            		 if(roomID6.equals(MyRoomID)) {
		            			 //Use a ArrayList to store all the separated part of this message
			            		 bufferList.add(buffer);
			            		 
			            		 if(buffer[2] == 1) {// 1 means this is the last part of separated message
			            			 //Check who is the target( = Check who send it)
			            			 byte[] target2 = new byte[17];
			            			 System.arraycopy(buffer, 20, target2, 0, 17);
			            			 String stringTarget2 = new String(target2);
			            			 
			            			 List<Integer> position = new ArrayList<Integer>();
			            			 int byteAmount = -1;
			            			 
			            			 //Check which buffer is send by the same guy send this buffer(have same target)
			            			 for(int i=0 ; i<bufferList.size(); i++) {
			            				 byte[] check = new byte[17];
			            				 System.arraycopy(bufferList.get(i),20,check,0,17);
			            				 String stringCheck = new String(check);
			            				 
			            				 //Combine all this buffer together
			            				 if(stringCheck.equals(stringTarget2)) {
			            					 position.add(i);
			            					 byteAmount += (bufferList.get(i).length-37);
			            				 }
			            			 }
			            			 
			            			 //Combine all this buffer's contained coordinate(byte)
			            			 byte[] allByte = new byte[byteAmount];
			            			 for(int i=0 ; i<position.size(); i++) {
			            				 System.arraycopy(bufferList.get(position.get(i)),
			            						37, allByte, allByte.length, bufferList.get(position.get(i)).length - 37);
			            			 }
			            			 
			            			 //Convert byte[] to int[]
			            			 int[] intPalette2 = new int[allByte.length / 4];
			            			 for(int i=0 ; i<allByte.length / 4; i++) {
			            				 byte[] XorY = new byte[4];
			            				 System.arraycopy(allByte, i*4, XorY, 0, 4);
			            				 intPalette2[i] = byteArrayToInt(XorY);
			            			 }
			            			 
			            			 if(stringTarget2.equals(MyAddress)) {//if this message is for me
			            				 //print this palette out at the Guess Activity
				            			 mGuessHandler.obtainMessage(Guess.MESSAGE_PALETTE,-1,-1,intPalette2)
				            			 	.sendToTarget();
				            		 } else {
				            			 //send this palette and its target to Guess Activity
				            			 //then Guess will store this palette at EndGame Activity
				            			 Message msg2 
				            			   = mGuessHandler.obtainMessage(Guess.MESSAGE_ENDGAME,-1,-1,intPalette2);
				            			 
				            			 Bundle bundle2 = new Bundle();
				            		     bundle2.putString("targetAddress",stringTarget2);
				            		     msg2.setData(bundle2);
				            		     mGuessHandler.sendMessage(msg2);
				            		 }
			            			 
			            			 //Clear those used buffer from bufferList
			            			 for(int i=0 ; i<position.size(); i++) {
			            				 bufferList.remove(position.get(i)-i);
			            			 }
			            		 }
		            		 }
		            		 break;
		            		 
		            	 }
		            	 break;
		             }
				 } catch(IOException e){
	         	   Log.e(TAG, "disconnected", e);
	         	   //Delete the ConnectedThread's reference , because this ConnectThread's connection is lost now
	         	   synchronized (GameService.this) {
	                   mConnThreads.set(I,null);
	               }
	         	   
	         	   cancel();
	         	   connectionLost(I);
	         	   break;
				 }
			 }
			 
		 }
		 
		 public int getNumber() {
	        	return I;
	     }
		 
		 public void write(byte[] buffer){
	        try {
	        	mmOutStream.write(buffer);
	        } catch (IOException e) {
	            Log.e(TAG, "Exception during write", e);
	        }
	    }
	 
		 public void cancel(){
			 try {
	             mmSocket.close();
	         } catch (IOException e) {
	             Log.e(TAG, "close() of connect socket failed", e);
	         }
		 }
		 
	 }
	 
	 private class ReAcceptThread extends Thread {
		 private BluetoothServerSocket serverSocket;
		 private BluetoothSocket socket;
		 private int I;
		 
		 ReAcceptThread(int i) {
			 I=i;
		 }
		 
		 public void run() {
			 boolean reAcceptSuccess = false;
			 
			 while(!reAcceptSuccess) {
				 try {
					 serverSocket = 
							 mBluetoothAdapter.listenUsingInsecureRfcommWithServiceRecord(NAME, mUUIDs.get(I));
					 socket = serverSocket.accept();
		             if (socket != null) {
		            	 //Check whether this device is the one who had connect me
		            	 if(socket.getRemoteDevice().getAddress().equals(mDeviceAddresses.get(I))) {
		                    connected(socket, socket.getRemoteDevice(),I);
		                    AcceptAmount++;
		                    reAcceptSuccess = true;
		                    cancel();
		                } else {
		                	socket.close();
		                }
		             }
		        } catch(IOException e) {}
			 }
		 }
		 
		 public void cancel() {
			 try {
				 serverSocket.close();
	         } catch (IOException e) {}
		 }
		 
	 }
	 
	 private class ReConnectThread extends Thread {
		 private BluetoothSocket mmSocket;
		 private BluetoothDevice mmDevice;
		 private int I;
		 
		 ReConnectThread(BluetoothDevice device,int i) {
			 mmDevice = device;
			 I = i;
		 }
		 
		 public void run() {
			 try {
				 mmSocket = mmDevice.createInsecureRfcommSocketToServiceRecord(mUUIDs.get(I));
	        	 mmSocket.connect();
	             	
	             connected(mmSocket,mmDevice,I);
	         } catch (IOException e) {
	        	 cancel();
	         }
		 }
		 
		 public void cancel() {
			 try {
				 mmSocket.close();
	         } catch (IOException e) {}
		 }
	 }
	
	public static void getOutSideHandler(Handler handler) {
		mOutSideHandler = handler;
	}
	
	public static void getGameRoomHandler(Handler handler) {
		mGameRoomHandler = handler;
	}
	
	public static void getGuessHandler(Handler handler) {
		mGuessHandler = handler;
	}
	
	public static void getPaletteHandler(Handler handler) {
		mPaletteHandler = handler;
	}
	
	@Override
	public IBinder onBind(Intent arg0) {
		// TODO Auto-generated method stub
		return null;
	}
	
	@Override
	public void onDestroy() {
		super.onDestroy();
	}
	
	public static byte[] intToByteArray(int a)
	{
	    byte[] ret = new byte[4];
	    ret[3] = (byte) (a & 0xFF);   
	    ret[2] = (byte) ((a >> 8) & 0xFF);   
	    ret[1] = (byte) ((a >> 16) & 0xFF);   
	    ret[0] = (byte) ((a >> 24) & 0xFF);
	    return ret;
	}
	
	public static int byteArrayToInt(byte[] b)
	{
	    return (b[3] & 0xFF) + ((b[2] & 0xFF) << 8) + ((b[1] & 0xFF) << 16) + ((b[0] & 0xFF) << 24);
	}
}