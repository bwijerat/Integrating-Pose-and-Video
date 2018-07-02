/*
@By: Brian Wijeratne
@Project: Integrating Pose and Stereo Video Data for Immersive Mixed Reality
@Supervisor: Matthew Kyan
May 2014 - August 2014
*/

package com.example.videotoframes;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.drawable.Drawable;
import android.hardware.Camera;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.hardware.Camera.AutoFocusCallback;
import android.media.CamcorderProfile;
import android.media.MediaMetadataRetriever;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.view.ViewGroup.LayoutParams;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

public class CamTestActivity extends Activity {
	Preview preview;
	Button bStart, bStop, bStore, bView, bExpand, bContract;
	Camera camera;
	String fileName;
	Activity act;
	static Context ctx;
	ProgressBar spinner;
	
	public static final int MEDIA_TYPE_IMAGE = 1;
	public static final int MEDIA_TYPE_VIDEO = 2;
	private static String IMAGE_DIRECTORY_NAME = "Android Testing " + new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
	public static String timeStamp;
	public static File mediaStorageDir, mediaFileVideo, mediaFilePic, txtData, pictureFile;
	public static Uri fileUri;
	
	//KEEPING COUNT
	public static int count = 0, countIndex = 0;
	public long refTime1 = 0, refTime2 = 0, cycleTime = 0;
	long time1, time2, videoLength;			/* used to calculate video length */
	int TIMEDELAY = 33;					/* time delay used for retrieving frames and sensor data */
	
	//Video
	public MediaRecorder mMediaRecorder;
	boolean isRecording = false, prepVideoCamera = false;
	
	//SENSORS
    Sensor accelerometer, gyro, magnometer;
	SensorManager sm1, sm2, sm3;
	TextView accX, accY, accZ, gyroRoll, gyroPitch, gyroYaw, magAzimuth, magPitch, magRoll;
	
	//Data Collection
	public static boolean dataCollect = false, dataCollectCamera = false, dataCollectAcc = false, dataCollectGyro = false, dataCollectMag = false;
	public String dataCamera = "\n\nCAMERA DATA:\n", dataAcc = "\n\nACCELEROMETER DATA:\n", dataGyro = "\n\nGYROSCOPE DATA:\n", dataMag = "\n\nMAGNOMETER DATA:\n";
	
	//ImageStorage 
	// chosen Integer, Long, Long, Long type arraylists to not slow down its conversion between data types
	public static ArrayList<String> imageNameArrayList = new ArrayList<String>();
	public static ArrayList<Integer> imageArrayList = new ArrayList<Integer>();
	public static ArrayList<Long> AccArrayList = new ArrayList<Long>();
	public static ArrayList<Long> GyroArrayList = new ArrayList<Long>();
	public static ArrayList<Long> MagArrayList = new ArrayList<Long>();

	public static ArrayList<Bitmap> bitmapArrayList = new ArrayList<Bitmap>();
	public static int numFrames = 0, countFramesRetrieved = 0;
	
	//Data Gallery
	TextView noData;
	LinearLayout layout, dataGallery;
	boolean isViewingData = false;
	Bitmap setSize;
	int IMAGE_HEIGHT = 700, IMAGE_WIDTH = 400;
	
	//UPD DATA
	static TextView serverMessage;
	TextView serverMessage2;
	android.os.Handler customHandler;
	static int number = 0;
	LinearLayout llUDP_Data;
	
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		ctx = this;
		act = this;
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);

		setContentView(R.layout.cam_test);

		preview = new Preview(this, (SurfaceView)findViewById(R.id.surfaceView));
		preview.setLayoutParams(new LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT));
		((FrameLayout) findViewById(R.id.preview)).addView(preview);
		preview.setKeepScreenOn(true);

		bStart = (Button) findViewById(R.id.buttonStart);
		bStop = (Button) findViewById(R.id.buttonStop);
		bStore = (Button) findViewById(R.id.buttonStore);
		bView = (Button) findViewById(R.id.bViewPictures);
		bView.setEnabled(false);
		
		dataGallery = (LinearLayout) findViewById(R.id.llDataGallery);
		
		//UDP Output Display 
		serverMessage = (TextView) findViewById(R.id.tvUPD_Data);
		serverMessage2 = (TextView) findViewById(R.id.tvTitle);
		llUDP_Data = (LinearLayout) findViewById(R.id.llUDP_Data);
		bExpand = (Button) findViewById(R.id.bExpand);
		bContract = (Button) findViewById(R.id.bContract);
		
		Bitmap bitmapToResize = BitmapFactory.decodeResource(getResources(), R.drawable.red_x);
		setSize = Bitmap.createScaledBitmap(bitmapToResize, IMAGE_HEIGHT, IMAGE_WIDTH, true);
		
		final ExecutorService sensorExecutor = Executors.newFixedThreadPool(5);
		
		bStart.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
			
				if (isRecording) {
	                // stop recording and release camera
	                mMediaRecorder.stop();  // stop the recording
	                time2 = System.currentTimeMillis();
	                releaseMediaRecorder(); // release the MediaRecorder object
	                camera.lock();         // take camera access back from MediaRecorder
	                
	                videoLength = time2 - refTime1;
	                
	                // inform the user that recording has stopped
	                bStop.setText("" + videoLength);
	                isRecording = false; 
	                
	                dataCollect = false; //stops data collection, SENSORS AND UDP
	                sensorExecutor.shutdownNow();
	            } else {
	                // initialize video camera
	                if (prepareVideoRecorder()) {
	                    // Camera is available and unlocked, MediaRecorder is prepared,
	                    // now you can start recording
	                	
	                    sensorExecutor.execute(new RunnableGyro());
	    		        sensorExecutor.execute(new RunnableMag());
	    		        sensorExecutor.execute(new RunnableAcc());
	    		        
	                    mMediaRecorder.start();
	                    
	                    refTime1 = System.currentTimeMillis();
	                    // inform the user that recording has started
	                    
	                    isRecording = true;
	                    dataCollect = true; // tells program its okay to start data collecting & displaying
	                    
	                    /* *//** START UPD Client *//* */
	            		new UPD_Receive_BackgroundOperation().execute("");
	                    
	                    bStop.setText("Recording");

	                } else {
	                    // prepare didn't work, release the camera
	                	releaseMediaRecorder();
	                    // inform user
	                }
	            }
				
			}
		});
		
		bStop.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				
		
		        
			}
		});
		
		bStore.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				
				new LongOperation().execute(""); 
			}
		});
		
		bView.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				
				if(isViewingData){
					
					dataGallery.removeAllViews();
					isViewingData = false;
				} else{
					
					int[] tempIntArray = {AccArrayList.size(), MagArrayList.size(), GyroArrayList.size(), bitmapArrayList.size()};
					Arrays.sort(tempIntArray);
					
					Log.d("video", "ARRAY SIZE: " + tempIntArray[0]);
					
					for(int i = 0; i < tempIntArray[0]; i++){ 
						//using GyroArrayList size because bitmapArrayList gives runtime error
						//Gyro slowest sensor, sometimes gets one less measurement than other sensors
						//so using GyroArrayList size ensures preview of frames with all sensor measurements

						dataGallery.addView(instantiateView(i));
						
					}
					isViewingData = true;
				}
			}
			
		});
		
		bStart.setOnLongClickListener(new OnLongClickListener(){
			@Override
			public boolean onLongClick(View arg0) {
				
				camera.autoFocus(new AutoFocusCallback(){
					@Override
					public void onAutoFocus(boolean arg0, Camera arg1) {
						//camera.takePicture(shutterCallback, rawCallback, jpegCallback);
					}
				});
				
				return true;
			}
		});
		
		bContract.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				
				serverMessage2.setText("");
				
				llUDP_Data.getLayoutParams().height = LayoutParams.WRAP_CONTENT;
				llUDP_Data.getLayoutParams().width = LayoutParams.WRAP_CONTENT;
				
				bContract.setVisibility(View.GONE); 
			}
		});
		
		bExpand.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				
				llUDP_Data.getLayoutParams().height = LayoutParams.FILL_PARENT;
				llUDP_Data.getLayoutParams().width = LayoutParams.FILL_PARENT;
				
				bContract.setVisibility(View.VISIBLE);
				
				serverMessage2.setText(serverMessage.getText()); 
			}
		}); 
	}
	
	public LinearLayout instantiateView(int viewCount){
		
		layout = new LinearLayout(CamTestActivity.this);
		layout.setOrientation(LinearLayout.VERTICAL);
		layout.setGravity(Gravity.CENTER);
		layout.setPadding(10, 0, 0, 10);
		
		final ImageView img = new ImageView(CamTestActivity.this);
		img.setMaxHeight(100);
		img.setMaxWidth(100);
		
    	img.setImageBitmap(bitmapArrayList.get(viewCount));
    	
        TextView view1 = new TextView(CamTestActivity.this); 
        view1.setText(viewCount + ") Image Capture Time: " + imageArrayList.get(viewCount));
        view1.setTextColor(Color.BLUE);
        
        TextView view2 = new TextView(this);
        view2.setTextColor(Color.CYAN);
        view2.setText(viewCount + ") Acc Capture Time: " + AccArrayList.get(viewCount));
        TextView view3 = new TextView(this);
        view3.setTextColor(Color.CYAN);
        view3.setText(viewCount + ") Gyro Capture Time: " + GyroArrayList.get(viewCount));
        TextView view4 = new TextView(this);
        view4.setTextColor(Color.CYAN);
        view4.setText(viewCount + ") Mag Capture Time: " + MagArrayList.get(viewCount));
        
        LinearLayout layout1 = new LinearLayout(this);
		layout1.setOrientation(LinearLayout.VERTICAL);
		layout1.setGravity(Gravity.CENTER);
		
		layout1.addView(view1);
        layout1.addView(view2);
        layout1.addView(view3);
        layout1.addView(view4);
        
        LinearLayout layout2 = new LinearLayout(this);
		layout2.setOrientation(LinearLayout.HORIZONTAL);
		layout2.setGravity(Gravity.CENTER);
		
		Button b1 = new Button(CamTestActivity.this);
		b1.setText("X");
		b1.setTextAppearance(ctx, android.R.style.TextAppearance_Large);
		b1.setTextColor(Color.RED);
		b1.setWidth(50);
		b1.setHeight(50);
        
		final Bitmap tempBitmap123 = setSize;
		
        b1.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				
				img.setImageBitmap(tempBitmap123);
				//img.setImageResource(R.drawable.red_x);
			}
        	
		});
        
        layout2.addView(layout1);
		layout2.addView(b1);
        
   //     layout.addView(img, new LinearLayout.LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT));
        layout.addView(img);
        layout.addView(layout2);
		
        return layout;
	}
	
	public boolean prepareVideoRecorder(){
	    mMediaRecorder = new MediaRecorder();

	    // Step 1: Unlock and set camera to MediaRecorder
	    camera.unlock();
	    mMediaRecorder.setCamera(camera);

	    // Step 2: Set sources
	    mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.CAMCORDER);
	    mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);

	    // Step 3: Set a CamcorderProfile (requires API Level 8 or higher)
	    mMediaRecorder.setProfile(CamcorderProfile.get(CamcorderProfile.QUALITY_HIGH));

	    // Step 4: Set output file
	    mMediaRecorder.setOutputFile(getOutputMediaFile(MEDIA_TYPE_VIDEO).toString());

	    // Step 5: Set the preview output
	    mMediaRecorder.setPreviewDisplay(preview.mSurfaceView.getHolder().getSurface());

	    // Step 6: Prepare configured MediaRecorder
	    try {
	        mMediaRecorder.prepare();
	    } catch (IllegalStateException e) {
	        Log.d("video", "IllegalStateException preparing MediaRecorder: " + e.getMessage());
	        releaseMediaRecorder();
	        return false;
	    } catch (IOException e) {
	        Log.d("video", "IOException preparing MediaRecorder: " + e.getMessage());
	        releaseMediaRecorder();
	        return false;
	    }
	    return true;
	}
	
	private void releaseMediaRecorder(){
        if (mMediaRecorder != null) {
            mMediaRecorder.reset();   // clear recorder configuration
            mMediaRecorder.release(); // release the recorder object
            mMediaRecorder = null;
            camera.lock();           // lock camera for later use
        }
    }

    private void releaseCamera(){
        if (camera != null){
            camera.release();        // release the camera for other applications
            camera = null;
        }
    }
	
	@Override
	protected void onResume() {
		super.onResume();
		camera = Camera.open(0);	/** 0 to open default camera*/
		camera.startPreview();
		preview.setCamera(camera);
	}

	@Override
	protected void onPause() {
		if(camera != null) {
			camera.stopPreview();
			preview.setCamera(null);
			camera.release();
			camera = null;
		}
		super.onPause();
        releaseMediaRecorder();       // if you are using MediaRecorder, release it first
        releaseCamera();
	}
	
	public File getOutputMediaFile(int type) {
		
		// External sdcard location
		mediaStorageDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), IMAGE_DIRECTORY_NAME);
		
		// Create the storage directory if it does not exist
		if (!mediaStorageDir.exists()) {
			if (!mediaStorageDir.mkdirs()) {
				Log.d("video", "Oops! Failed create " + IMAGE_DIRECTORY_NAME + " directory");
				return null;
			}
		}
		
		timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
		
		// Create a media file name
		if (type == MEDIA_TYPE_IMAGE) {
			
			mediaFilePic = new File(mediaStorageDir.getPath() + File.separator + "IMG_" + System.currentTimeMillis() + ".png");
			countIndex++;
			Log.d("video", "stored Image");
			return mediaFilePic;
			
		} else if (type == MEDIA_TYPE_VIDEO) {

			mediaFileVideo = new File(mediaStorageDir.getPath() + File.separator + "VID_" + timeStamp + ".mp4");
			Log.d("video", "stored VIDEO");
			return mediaFileVideo;
		}
		
		return null;
	}
	
	/* */ /** ACCLEROMETER THREAD *******************************/ /* */  /************************************************* */ /* */
	class RunnableAcc implements Runnable, SensorEventListener  {

        final ScheduledExecutorService serviceAcc = Executors.newSingleThreadScheduledExecutor();
        float[] accValues;
        
        RunnableAcc() { //Initialize and start Accelerometer
        	
        	accX = (TextView) findViewById(R.id.tvAccX);
    		accY = (TextView) findViewById(R.id.tvAccY);
    		accZ = (TextView) findViewById(R.id.tvAccZ);

    		sm1 = (SensorManager) getSystemService(Context.SENSOR_SERVICE);

    		if (sm1.getSensorList(Sensor.TYPE_ACCELEROMETER).size() != 0){
    			accelerometer = sm1.getSensorList(Sensor.TYPE_ACCELEROMETER).get(0);
    			sm1.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);
    		}
        } 
        
        
        
        @Override 
        public void run() {
        	
        	serviceAcc.scheduleWithFixedDelay(new Runnable()
    	    {
    			
    	        @Override
    	        public void run()
    	        {
    	        	if(dataCollect) {
    	        		
    	        		refTime2 = System.currentTimeMillis();
    	        		long currentTime = (refTime2 - refTime1);
    	        		AccArrayList.add(currentTime);
    	        		
    	        		count++; //base count for data capture
    	        		dataAcc += "\n" + count + ")\tTime: " + currentTime + "\t"  + accValues[0] + "\t\t" + accValues[1] + "\t\t" + accValues[2] + "\n";
    	        		
    	        		accX.setText("X: " + accValues[0]);
    					accY.setText("Y: " + accValues[1]);
    					accZ.setText("Z: " + accValues[2]);
    	        	}
    	        	
    	        }
    	      }, 0, TIMEDELAY, TimeUnit.MILLISECONDS);
        }
        
		@Override
		public void onSensorChanged(SensorEvent event) {
			// TODO Auto-generated method stub
			
			accValues = event.values;
		}

		@Override
		public void onAccuracyChanged(Sensor sensor, int accuracy) {
			// TODO Auto-generated method stub
			
		}
	}
	
	/* */ /** GYROSCOPE THREAD *******************************/ /* */  /**************************************************** */ /* */ 
	class RunnableGyro implements Runnable, SensorEventListener  {

        final ScheduledExecutorService serviceGyro = Executors.newSingleThreadScheduledExecutor();
        
        RunnableGyro() { //Initialize and start Gyro
        	
        	gyroRoll = (TextView) findViewById(R.id.tvGyroRoll);
			gyroPitch = (TextView) findViewById(R.id.tvGyroPitch);
			gyroYaw = (TextView) findViewById(R.id.tvGyroYaw);

			sm2 = (SensorManager) getSystemService(Context.SENSOR_SERVICE);

			if (sm2.getSensorList(Sensor.TYPE_GYROSCOPE).size() != 0){
				gyro = sm2.getSensorList(Sensor.TYPE_GYROSCOPE).get(0);
				sm2.registerListener(this, gyro, SensorManager.SENSOR_DELAY_NORMAL);

			}
        } 
        
        @Override 
        public void run() {
        	
    		serviceGyro.scheduleWithFixedDelay(new Runnable()
    	      {
    	    	
    	        @Override
    	        public void run()
    	        {
    	        	if(dataCollect) {

    	        		//Log.d("bro", "Date: " + new Date());
    	        		refTime2 = System.currentTimeMillis();
    	        		long currentTime = refTime2 - refTime1;
    	        		
    	        		GyroArrayList.add(currentTime);
    	        		
    	        		dataGyro += "\n" + count + ")\tTime: " + currentTime + "\t" + gyroRoll.getText() + "\t\t" + gyroPitch.getText() + "\t\t" + gyroYaw.getText()  + "\n";
    	        		
    	        	}
    	        }
    	      }, 0, TIMEDELAY, TimeUnit.MILLISECONDS);
        }

		@Override
		public void onSensorChanged(SensorEvent event) {
			// TODO Auto-generated method stub
			
			if(isRecording){
				gyroRoll.setText("X(Roll): " + event.values[2]);
				gyroPitch.setText("Y(Pitch): " + event.values[1]);
				gyroYaw.setText("Z(Yaw): " + event.values[0]);
			}
		}

		@Override
		public void onAccuracyChanged(Sensor sensor, int accuracy) {
			// TODO Auto-generated method stub
			
		}
	}
	
	/* */ /** MAGNOMETER THREAD *******************************/ /* */  /**************************************************** */ /* */ 
	class RunnableMag implements Runnable, SensorEventListener  {

        final ScheduledExecutorService serviceMag = Executors.newSingleThreadScheduledExecutor();
        String[] magData = new String[4];
        
        RunnableMag() { //Initialize and start Accelerometer
        	
        	magAzimuth = (TextView) findViewById(R.id.tvMagAzimuth);
        	magPitch = (TextView) findViewById(R.id.tvMagPitch);
    		magRoll = (TextView) findViewById(R.id.tvMagRoll);
    		
    		sm3 = (SensorManager) getSystemService(Context.SENSOR_SERVICE);

    		if (sm3.getSensorList(Sensor.TYPE_ACCELEROMETER).size() != 0){
    			magnometer = sm3.getSensorList(Sensor.TYPE_MAGNETIC_FIELD).get(0);
    			sm3.registerListener(this, magnometer, SensorManager.SENSOR_DELAY_NORMAL);
    	
    		}
        } 
        
        @Override 
        public void run() {
        	
    		serviceMag.scheduleWithFixedDelay(new Runnable()
    	      {
    	    	
    	        @Override
    	        public void run()
    	        {
    	        	
    	        	if(dataCollect) {
    	        		
    	        		refTime2 = System.currentTimeMillis();
    	        		long currentTime = refTime2 - refTime1;
    	        		
    	        		MagArrayList.add(currentTime);
    	        		
    	        		dataMag += "\n" + count + ")\tTime: " + currentTime + "\t" + magAzimuth.getText() + "\t\t" + magPitch.getText() + "\t\t" + magRoll.getText()  + "\n";
        	        	
    	        	}
    	        }
    	      }, 0, TIMEDELAY, TimeUnit.MILLISECONDS);
        }

		@Override
		public void onSensorChanged(SensorEvent event) {
			// TODO Auto-generated method stub
			if(isRecording){	
				magAzimuth.setText("Azimuth �: " + event.values[0]);
				magPitch.setText("Pitch �: " + event.values[1]);
				magRoll.setText("Roll �: " + event.values[2]);
			}	
		}

		@Override
		public void onAccuracyChanged(Sensor sensor, int accuracy) {
			// TODO Auto-generated method stub
			
		}
	}

	/* */ /** RETRIEVE IMAGES *********************************/ /* */  /**************************************************** */ /* */
	class LongOperation extends AsyncTask<String, Void, String> {
		
		Bitmap bitmap, resizedBitmap;
		TextView imageProgress;
		
		@Override
		protected String doInBackground(String... params) {
			
			retrieveFrames();
			
			return null;
		}
		
		@Override
        protected void onPostExecute(String result) {
			spinner.setVisibility(View.GONE);
			
			imageProgress.setText("Retrieval complete: " + numFrames + " frames");
			bView.setBackgroundColor(Color.parseColor("#80cccccc"));
			bView.setEnabled(true);
        }

        @Override
        protected void onPreExecute() {
        	imageProgress = (TextView) findViewById(R.id.tvImageProgress);
        	spinner = (ProgressBar)findViewById(R.id.progressBar1);
    		spinner.setVisibility(View.VISIBLE);
    		
    		numFrames = (int) (videoLength/TIMEDELAY);
			
    		imageProgress.setText("Image Retrieval: " + countFramesRetrieved + "/" + numFrames);
        }
		
		private void retrieveFrames(){
			
			MediaMetadataRetriever retriever = new MediaMetadataRetriever(); 
			
			if(mediaFileVideo != null) {
				retriever.setDataSource(mediaFileVideo.getPath());
				
				for(int inn = TIMEDELAY; inn < videoLength; inn += TIMEDELAY) {
					
					imageArrayList.add(inn);
					
					long tempNum = inn*1000;
				
					bitmap = retriever.getFrameAtTime(tempNum, MediaMetadataRetriever.OPTION_CLOSEST);  //works in microseconds !
					
					resizedBitmap = Bitmap.createScaledBitmap(bitmap, IMAGE_HEIGHT, IMAGE_WIDTH, true);
					
					bitmapArrayList.add(resizedBitmap);
					
					countFramesRetrieved++;
					
					runOnUiThread(new Runnable() 
				    {
				        public void run() 
				        {
				        	imageProgress.setText("Image Retrieval: " + countFramesRetrieved + "/" + numFrames);
				        }
				    });
					
					Log.d("video", "Image retrieved " + inn/TIMEDELAY);
				}
			}
			
			retriever.release();
		}
		
		private void storeFrames(){
			
			int countcount = 0;
			
			for(int iss = TIMEDELAY; iss < videoLength; iss += TIMEDELAY) {
			
				mediaFilePic = getOutputMediaFile(MEDIA_TYPE_IMAGE);
				
				try {
					FileOutputStream out = new FileOutputStream(mediaFilePic);
					
					bitmapArrayList.get(countcount).compress(Bitmap.CompressFormat.PNG, 90, out);
					out.flush();
					out.close();
				} catch (Exception e) {
					e.printStackTrace();
					Toast t = Toast.makeText(CamTestActivity.this, "Error: Bitmap to stored .PNG file not created", Toast.LENGTH_LONG);
		     		t.show();
				} 
				
				countcount++;
			} 
			
			saveText();
		}
		
		public File getOutputMediaFile(int type) {
			
			// External sdcard location
			mediaStorageDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), IMAGE_DIRECTORY_NAME);
			
			// Create the storage directory if it does not exist
			if (!mediaStorageDir.exists()) {
				if (!mediaStorageDir.mkdirs()) {
					Log.d("video", "Oops! Failed create " + IMAGE_DIRECTORY_NAME + " directory");
					return null;
				}
			}
			
			timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
			
			// Create a media file name
			if (type == MEDIA_TYPE_IMAGE) {
				
				mediaFilePic = new File(mediaStorageDir.getPath() + File.separator + "IMG_" + System.currentTimeMillis() + ".png");
				countIndex++;
				Log.d("video", "stored Image");
				return mediaFilePic;
				
			} else if (type == MEDIA_TYPE_VIDEO) {

				mediaFileVideo = new File(mediaStorageDir.getPath() + File.separator + "VID_" + timeStamp + ".mp4");
				Log.d("video", "stored VIDEO");
				return mediaFileVideo;
			}
			
			return null;
		}
		
		public void saveText(){    

			try {
				timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());

				//Choose text file location and name, then create new file
				mediaStorageDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), IMAGE_DIRECTORY_NAME);
				txtData = new File(mediaStorageDir.getPath() + File.separator + "Data_" + timeStamp + ".txt");
				txtData.createNewFile();

				//Choose text file to output too, startup text writer
				FileOutputStream fOut = new FileOutputStream(txtData);
				OutputStreamWriter myOutWriter = new OutputStreamWriter(fOut);

				//Write to text file, show progress, close writer and file 
				myOutWriter.append(dataCamera + dataGyro + dataMag + dataAcc);
				myOutWriter.close();
				fOut.close();
				Log.d("images","Txt file successfully created!");
				} catch (Exception e) {
					Log.d("images","Txt writing Exception!");
					e.printStackTrace();
				}         
		}
		
	}

	/* */ /** UDP SERVER *********************************/ /* */  /**************************************************** */ /* */
	class UPD_Receive_BackgroundOperation extends AsyncTask<String, Void, String> {
		
		String displayed_UDP_Data = "";
		
		public static final String SERVERIP = "192.168.1.132"; // ‘Within’ the emulator!
		public static final int SERVERPORT = 5000;
		
		public Handler Handler = new Handler() {
			@Override
			public void handleMessage(Message msg) {
				displayed_UDP_Data += msg.obj.toString() + "\n\n";
			}
		};
		
		@Override
		protected String doInBackground(String... params) {

			Log.d("udp", "in Server.start()");
			try {
					
					InetAddress serverAddr = InetAddress.getByName(SERVERIP);
					updatetrack("Server: Start connecting");
					
					do{
						
						DatagramSocket socket = new DatagramSocket(SERVERPORT, serverAddr);
					
						Log.e("udp", "in Server while loop");
						byte[] buf = new byte[500];
						Log.v("udp", "created byte[]");
						final DatagramPacket packet = new DatagramPacket(buf, buf.length);
						Log.v("udp", "created packet");
						updatetrack("Server: Receiving");
						socket.receive(packet);
						
						Log.v("udp", "Packet:" + new String(packet.getData()));
						updatetrack( new String(packet.getData()) );
						
				        socket.close();
					}while(dataCollect);

			} catch (Exception e) {
				updatetrack("Server: Data receive Error!\n");
				Log.e("udp", "in Server receiver ERROR!");
			}
			
			return null;
			
		}
		
		public void updatetrack(String s) {
			Message msg = new Message();
			String textTochange = s;
			msg.obj = textTochange;
			Handler.sendMessage(msg);
		}

		@Override
        protected void onPostExecute(String result) {
			serverMessage.setText(displayed_UDP_Data);
			Log.w("udp", displayed_UDP_Data);
        }

        @Override
        protected void onPreExecute() {
        	
        } 
	} 
}
