/*
@By: Brian Wijeratne
@Project: Integrating Pose and Stereo Video Data for Immersive Mixed Reality
@Supervisor: Matthew Kyan
May 2014 - August 2014
*/

package com.example.videotoframes;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.UnknownHostException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import android.R.color;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Matrix;
import android.hardware.Camera;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.hardware.Camera.PictureCallback;
import android.media.ExifInterface;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnPreparedListener;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.text.format.Time;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceHolder.Callback;
import android.view.SurfaceView;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.MediaController;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;

public class CameraSample extends Activity{

	private Camera camera;
	private VideoView videoView = null;
	private Button retrieveStart = null, retrieveStop = null, retrieveStore = null;
	private ProgressBar spinner;
	
	private static final String TAG = "Video";
	private List<Camera.Size> sizes;
	public static final int MEDIA_TYPE_IMAGE = 1;
	public static final int MEDIA_TYPE_VIDEO = 2;
	private static String IMAGE_DIRECTORY_NAME = "Android Testing " + new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
	public static String timeStamp;
	public static File mediaStorageDir, mediaFileVideo, mediaFilePic, txtData, pictureFile;
	public static Uri fileUri;
	public static boolean startThread = false;
	public static Handler handler;
	public MediaController media_controller;
	
	//SENSORS
    Sensor accelerometer, gyro, magnometer;
	SensorManager sm1, sm2, sm3;
	TextView accX, accY, accZ, gyroRoll, gyroPitch, gyroYaw, magAzimuth, magPitch, magRoll;
	
	//KEEPING COUNT
	public static int count = 0, countIndex = 0;
	public long refTime1 = 0, refTime2 = 0, cycleTime = 0;
	public Timer timer;
	
	//Data Collection
	public static boolean dataCollect = false, dataCollectCamera = false, dataCollectAcc = false, dataCollectGyro = false, dataCollectMag = false;
	public String dataCamera = "\n\nCAMERA DATA:\n", dataAcc = "\n\nACCELEROMETER DATA:\n", dataGyro = "\n\nGYROSCOPE DATA:\n", dataMag = "\n\nMAGNOMETER DATA:\n";
	
	//ImageStorage
	public ArrayList<byte[]> imagesArrayList = new ArrayList<byte[]>();
	public ArrayList<String> imageNameArrayList = new ArrayList<String>();
	public ArrayList<String> AccArrayList = new ArrayList<String>();
	public ArrayList<String> GyroArrayList = new ArrayList<String>();
	public ArrayList<String> MagArrayList = new ArrayList<String>();
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		setContentView(R.layout.camera_sample);
		
		retrieveStart = (Button) findViewById(R.id.bRetrieveStart);
		retrieveStart.setBackgroundColor(Color.GREEN);
		retrieveStop = (Button) findViewById(R.id.bRetrieveStop);
		retrieveStop.setBackgroundColor(Color.YELLOW);
		retrieveStore = (Button) findViewById(R.id.bRetrieveStore);
		retrieveStore.setBackgroundColor(Color.RED);
		
		timer = new Timer();
		
		final ExecutorService cameraExecutor = Executors.newFixedThreadPool(2);
		final ExecutorService sensorExecutor = Executors.newFixedThreadPool(5);
		final ExecutorService saveExecutor = Executors.newFixedThreadPool(1);
		
//		cameraExecutor.execute(new RunnableCamera());
		
		retrieveStart.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				
				Log.i("bro", "START CLICK");
		        
/*		        sensorExecutor.execute(new RunnableCameraService());
		        sensorExecutor.execute(new RunnableGyro());
		        sensorExecutor.execute(new RunnableMag());
		        sensorExecutor.execute(new RunnableAcc());
		        
		        refTime1 = System.currentTimeMillis();
		        dataCollect = true; // tells program its okay to start data collecting & displaying
		        
		        timer.schedule(new UpdateTimeTask(), 10, 500); 
		        
		        Log.i("image", "Threads all Started!"); 
				*/
				
				try {
					
					Class<?> ourClass;
					ourClass = Class.forName("com.example.videotoframes.CamTestActivity");
					Intent ourIntent = new Intent(CameraSample.this, ourClass);
					startActivity(ourIntent);
					
				} catch (ClassNotFoundException e) {
					
					Log.i("image", "FAILED STARTING CamTestActivity!");
				} 
			}

		});  
		
		retrieveStop.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				Log.i("bro", "STOP CLICK");
				
				dataCollect = false; //stop data collecting and displaying
				sensorExecutor.shutdownNow();
				
				Log.d("image", "ArrayList Size: " + imagesArrayList.size());
			}
		});
		
		retrieveStore.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				
				Log.i("image", "STORE CLICK");
				spinner = (ProgressBar)findViewById(R.id.pbDataSaveProgress);
				spinner.setVisibility(View.VISIBLE);
				
				saveExecutor.execute(new RunnableSave());
				
			}
		
		});
		
	}
	
	class RunnableCameraService implements Runnable {
		
		ScheduledExecutorService serviceCamCam = new ScheduledThreadPoolExecutor(10);
		
		@Override
		public void run() {
			
			serviceCamCam.scheduleWithFixedDelay(new Runnable()
		    {
		    	
		        @Override
		        public void run()
		        {
		        	if(dataCollect) {
		        		
		        		camera.takePicture(null, null, mPicture);
		        	}
		        }
		      }, 0, 250, TimeUnit.MILLISECONDS);
			
		}
		
		private PictureCallback mPicture = new PictureCallback() {
			
			@Override
			public void onPictureTaken(byte[] data, Camera camera) {
				
			//	camera.startPreview(); 
				
				imagesArrayList.add(data);
				
				refTime2 = System.currentTimeMillis();
        		long currentTime = refTime2 - refTime1;
        		long tempCycle = currentTime - cycleTime;
        		Log.d("timedelay", "CamService Time Delay: " + currentTime);
        		Log.d("timedelay", "CamService Cycle Delay: " + tempCycle);
        		cycleTime = currentTime;
				
				imageNameArrayList.add("IMG_" + currentTime + ".png");
				
				count++;
				dataCamera += "\n" + count + ")\tIMG_" + currentTime + ".png";
				
				Log.d("image", "byte[] added to ArrayList");
			
			}
		}; 
	}
	
	/* */ /** CAMERA THREAD *******************************/ /* */  /**************************************************** */ /* */
	
	class RunnableCamera implements Runnable {
		
//		ScheduledExecutorService serviceCam = new ScheduledThreadPoolExecutor(1);
		
		@Override
		public void run() {
			startCamera CAM = new startCamera();
			
/*			serviceCam.scheduleAtFixedRate(new Runnable()
    	    {
    	    	
    	        @Override
    	        public void run()
    	        {
    	        	if(dataCollect) {
    	        		
    	        		refTime2 = System.currentTimeMillis();
    	        		long currentTime = refTime2 - refTime1;
    	        		long tempCycle = currentTime - cycleTime;
    	        		Log.d("timedelay", "Camera Time Delay: " + currentTime);
    	        		Log.d("timedelay", "Camera Cycle Delay: " + tempCycle);
    	        		cycleTime = currentTime;
    	        		
    	        	}
    	        }
    	      }, 0, 250, TimeUnit.MILLISECONDS);  */
		}
    	
    	public class startCamera implements Callback {
    		
    		//private Camera camera;
    		private SurfaceHolder holder = null;
    		
			public startCamera() {
				videoView = (VideoView) findViewById(R.id.vwCamera);
		    	
		    	holder = videoView.getHolder();
		    	holder.addCallback(this);
				
		    	Log.d("bro", "startCamera() constructor");
			}
			
			@Override
			public void surfaceCreated(SurfaceHolder holder) {
				try {
					Log.i("bro", "surfaceCreated");
					camera = Camera.open(0);	//CHANGE BETWEEN 0 AND 1 to find proper camera for device

					camera.setDisplayOrientation(90);

					if (camera != null) {
						camera.setPreviewDisplay(holder);

					} else {
						Log.i("bro", "camera = null");
					}
				} catch (Exception e) {
					Log.v(TAG, "Could not start the preview-Display123");
					e.printStackTrace();
				}
				
			}

			@Override
			public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
				Log.i("bro", "surfaceChanged");
				
				//Sets correct preview resolution 
				if (holder.getSurface() == null){
					Log.i("bro", "holder.getSurface() == null");
					return;
				}
				
				Camera.Parameters parameters = camera.getParameters();
				Log.i("bro", "camera.getParameters();");
				
				sizes = parameters.getSupportedPreviewSizes();
				Log.i("bro", "parameters.getSupportedPreviewSizes();");
				Camera.Size optimalSize = getBestPreviewSize(width, height);
				try {
					parameters.setPreviewSize(optimalSize.width, optimalSize.height);
					camera.setParameters(parameters);

				} catch (NullPointerException a) {

				}
				
				Log.i("bro", "startPreview()");
				camera.startPreview();
				
			}

			@Override
			public void surfaceDestroyed(SurfaceHolder holder) {
				Log.i("bro", "surfaceDestroyed");
				if (camera != null) {
					camera.stopPreview();
					camera.release();
					Log.i("bro", "CAMERA STOPPED");
				}
				
			}
			
			private Camera.Size getBestPreviewSize(int width, int height) {
				Camera.Size result = null;
				Camera.Parameters p = camera.getParameters();
				for (Camera.Size size : p.getSupportedPreviewSizes()) {
					if (size.width <= width && size.height <= height) {
						if (result == null) {
							result = size;
						} else {
							int resultArea = result.width * result.height;
							int newArea = size.width * size.height;

							if (newArea > resultArea) {
								result = size;
							}
						}
					}
				}
				return result;

			} 
    		
    	}
		
    } 
	
	/* */ /** ACCLEROMETER THREAD *******************************/ /* */  /**************************************************** */ /* */ 
	
	class RunnableAcc implements Runnable, SensorEventListener  {

        final ScheduledExecutorService serviceAcc = Executors.newSingleThreadScheduledExecutor();
        
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
        	
        	Log.d("bro", "Im IN A THREAD!!!");
        	
    		serviceAcc.scheduleWithFixedDelay(new Runnable()
    	    {
    	    	
    	        @Override
    	        public void run()
    	        {
    	        	if(dataCollect) {
    	        		
    	        		refTime2 = System.currentTimeMillis();
    	        		long currentTime = refTime2 - refTime1;
    	        		
    	        		dataAcc += "\n" + count + ")\tTime: " + currentTime + "\t"  + accX.getText() + "\t\t" + accY.getText() + "\t\t" + accZ.getText() + "\n";
    	        		
    	        		long tempCycle = currentTime - cycleTime;
    	        		Log.i("timedelay", "Acc Time Delay: " + currentTime);
    	        		Log.i("timedelay", "Acc Cycle Delay: " + tempCycle);
    	        		cycleTime = currentTime;
    	        		
    	        	}
    	    
    	        }
    	      }, 0, 250, TimeUnit.MILLISECONDS);
        }
        
		@Override
		public void onSensorChanged(SensorEvent event) {
			// TODO Auto-generated method stub
			if(dataCollect) {
				accX.setText("X: " + event.values[0]);
				accY.setText("Y: " + event.values[1]);
				accZ.setText("Z: " + event.values[2]);
				
			}
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
        	
        	Log.d("bro", "Im IN A THREAD!!!");
        	
    		serviceGyro.scheduleWithFixedDelay(new Runnable()
    	      {
    	    	
    	        @Override
    	        public void run()
    	        {
    	        	
    	        	if(dataCollect) {
    	        	
    	        		//Log.d("bro", "Date: " + new Date());
    	        		refTime2 = System.currentTimeMillis();
    	        		long currentTime = refTime2 - refTime1;
    	        		
    	        		dataGyro += "\n" + count + ")\tTime: " + currentTime + "\t" + gyroRoll.getText() + "\t\t" + gyroPitch.getText() + "\t\t" + gyroYaw.getText()  + "\n";
    	        		
    	        		long tempCycle = currentTime - cycleTime;
    	        		Log.w("timedelay", "Gyro Time Delay: " + currentTime);
    	        		Log.w("timedelay", "Gyro Cycle Delay: " + tempCycle);
    	        		cycleTime = currentTime;
    	        	
    	        	}
    	        }
    	      }, 0, 250, TimeUnit.MILLISECONDS);
        }

		@Override
		public void onSensorChanged(SensorEvent event) {
			// TODO Auto-generated method stub
			if(dataCollect) {
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
        	
        	Log.d("bro", "Im IN A THREAD!!!");
        	
    		serviceMag.scheduleWithFixedDelay(new Runnable()
    	      {
    	    	
    	        @Override
    	        public void run()
    	        {
    	        	
    	        	if(dataCollect) {
    	        		
    	        		refTime2 = System.currentTimeMillis();
    	        		long currentTime = refTime2 - refTime1;
    	        		
    	        		dataMag += "\n" + count + ")\tTime: " + currentTime + "\t" + magAzimuth.getText() + "\t\t" + magPitch.getText() + "\t\t" + magRoll.getText()  + "\n";
        	        	
    	        		long tempCycle = currentTime - cycleTime;
    	        		Log.v("timedelay", "Mag Time Delay: " + currentTime);
    	        		Log.v("timedelay", "Mag Cycle Delay: " + tempCycle);
    	        		cycleTime = currentTime;
    	        		
    	        	}
    	        }
    	      }, 0, 250, TimeUnit.MILLISECONDS);
        }

		@Override
		public void onSensorChanged(SensorEvent event) {
			// TODO Auto-generated method stub
			if(dataCollect) {
				magAzimuth.setText("Azimuth ¿: " + event.values[0]);
				magPitch.setText("Pitch ¿: " + event.values[1]);
				magRoll.setText("Roll ¿: " + event.values[2]);
			}
		}

		@Override
		public void onAccuracyChanged(Sensor sensor, int accuracy) {
			// TODO Auto-generated method stub
			
		}
	}
	
	/* */ /** SAVE THREAD *******************************/ /* */  /**************************************************** */ /* */ 
	
	class RunnableSave implements Runnable {

		@Override
		public void run() {
			
			saveImages();
	        saveText();
		}
		
		private void saveImages(){
			
			Log.i("image", "Image ARRAY size: " + imagesArrayList.size());
			
			for(int i=0; i < imagesArrayList.size(); i++) {
				pictureFile = getOutputMediaFile(MEDIA_TYPE_IMAGE);
			
				if (pictureFile == null) {
				Log.d("bro",
						"Error creating media file, check storage permissions");
					return;
				}
				else {
					imageRotate(imagesArrayList.get(i));
				}
			}
		}
		
		private File getOutputMediaFile(int type) {
			
			// External sdcard location
			mediaStorageDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), IMAGE_DIRECTORY_NAME);

			// Create the storage directory if it does not exist
			if (!mediaStorageDir.exists()) {
				if (!mediaStorageDir.mkdirs()) {
					Log.d(IMAGE_DIRECTORY_NAME, "Oops! Failed create " + IMAGE_DIRECTORY_NAME + " directory");
					return null;
				}
			}

			timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());

			// Create a media file name
			if (type == MEDIA_TYPE_IMAGE) {
				
				mediaFilePic = new File(mediaStorageDir.getPath() + File.separator + imageNameArrayList.get(countIndex));
				countIndex++;
				return mediaFilePic;
			} else if (type == MEDIA_TYPE_VIDEO) {

				mediaFileVideo = new File(mediaStorageDir.getPath() + File.separator + "VID_" + timeStamp + ".mp4");
				return mediaFileVideo;
			} else {
				return null;
			}
		}

		// ROTATE IMAGE FILE
		private void imageRotate(byte[] bitmapdata) {
			
			int angle = 270;
			
			Matrix mat = new Matrix();
			mat.postRotate(angle); 		//rotates the image 270 degrees clockwise
			mat.preScale(1.0f, -1.0f);	//flips the image on its center vertical axis
			Bitmap bmp = BitmapFactory.decodeByteArray(bitmapdata , 0, bitmapdata.length);
			Bitmap captureBmp = Bitmap.createBitmap(bmp, 0, 0, bmp.getWidth(),
					bmp.getHeight(), mat, true);
			FileOutputStream fOut;
			try {
				fOut = new FileOutputStream(pictureFile);
				captureBmp.compress(Bitmap.CompressFormat.PNG, 100, fOut);
				fOut.flush();
				fOut.close();
				Log.i("image", "image rotated !!! ");
			} catch (Exception e) {
				Log.i("image", "Could not save rotated image file!");
			}
			
		}
			
		
		//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
		/* WRITING TO TXT FILE */ /******************************************************************************************/
		//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
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
	
}
