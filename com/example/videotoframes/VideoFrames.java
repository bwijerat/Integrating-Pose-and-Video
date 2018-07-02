/*
@By: Brian Wijeratne
@Project: Integrating Pose and Stereo Video Data for Immersive Mixed Reality
@Supervisor: Matthew Kyan
May 2014 - August 2014
*/

package com.example.videotoframes;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.MediaMetadataRetriever;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnPreparedListener;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;

public class VideoFrames extends Activity implements SensorEventListener{

	// Activity request codes
	private static final int CAMERA_CAPTURE_IMAGE_REQUEST_CODE = 100;
	private static final int CAMERA_CAPTURE_VIDEO_REQUEST_CODE = 200;
	public static final int MEDIA_TYPE_IMAGE = 1;
	public static final int MEDIA_TYPE_VIDEO = 2;
	public static String timeStamp;
	public static File mediaStorageDir, mediaFileVideo, mediaFilePic, txtData;
	public static Uri fileUri; // file url to store image/video
	public Bitmap bitmap;
	public static Long refTime = (long) 0.00, refTimeSensor = (long) 0.00;
	static ArrayList<String> imageNames = new ArrayList<String>();
	public long videoTime;
	public static long inn;
	public static String dataName1 = ""; 
	public static String dataAcc1 = "";
	public static String dataGyro1 = "";
	public boolean dataAcc = false;
	public static boolean dataAcc0 = false;
	public boolean dataGyro = false;
	
	// directory name to store captured images and videos
	private static final String IMAGE_DIRECTORY_NAME = "Android Testing";

	private ImageView imgPreview;
	private VideoView videoPreview;
	private Button btnCapturePicture, btnRecordVideo, btnFrames;
	
	Sensor accelerometer, gyro;
	SensorManager sm1, sm2;
	TextView accX, accY, accZ, gyroRoll, gyroPitch, gyroYaw;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.video_frames);
		
		System.out.println("PROGRAM START");
		
		imgPreview = (ImageView) findViewById(R.id.imgPreview);
		videoPreview = (VideoView) findViewById(R.id.videoPreview);
		btnCapturePicture = (Button) findViewById(R.id.btnCapturePicture);
		btnRecordVideo = (Button) findViewById(R.id.btnRecordVideo);
		btnFrames = (Button) findViewById(R.id.btnFrames); 
		
		//STARTUP ACCELEROMETER /////////////////////////////////
		
		accX = (TextView) findViewById(R.id.tvAccX);
		accY = (TextView) findViewById(R.id.tvAccY);
		accZ = (TextView) findViewById(R.id.tvAccZ);

		sm1 = (SensorManager) getSystemService(Context.SENSOR_SERVICE);

		if (sm1.getSensorList(Sensor.TYPE_ACCELEROMETER).size() != 0){
			accelerometer = sm1.getSensorList(Sensor.TYPE_ACCELEROMETER).get(0);
			sm1.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);
	
		}
		
		/** //////////////////////////////////////////////////////// */
		
		//STARTUP GYRO /////////////////////////////////
		
				gyroRoll = (TextView) findViewById(R.id.tvGyroRoll);
				gyroPitch = (TextView) findViewById(R.id.tvGyroPitch);
				gyroYaw = (TextView) findViewById(R.id.tvGyroYaw);

				sm2 = (SensorManager) getSystemService(Context.SENSOR_SERVICE);

				if (sm2.getSensorList(Sensor.TYPE_GYROSCOPE).size() != 0){
					gyro = sm2.getSensorList(Sensor.TYPE_GYROSCOPE).get(0);
					sm2.registerListener(this, gyro, SensorManager.SENSOR_DELAY_NORMAL);
	
				}
				
		/** /////////////////////////////////////////////////////// */
		
		
		/**
		 * Capture image button click event
		 * */
		btnCapturePicture.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				// capture picture
				captureImage();
			}
		});

		/**
		 * Record video button click event
		 */
		
		btnRecordVideo.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				// record video
				recordVideo();
				System.out.println("Size of Video File: " + mediaFileVideo.length() + "!!!");
			}
		});
		
		/**
		 * Create Frames button click event
		 */
		
		btnFrames.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				// extract frames from video
				 
				MediaMetadataRetriever retriever = new MediaMetadataRetriever(); 
				
				retriever.setDataSource(mediaFileVideo.getPath());
				
				for(inn = 33; inn < videoTime; inn += 33) {
					
					long tempNum = inn*1000;
					
					bitmap = retriever.getFrameAtTime(tempNum, MediaMetadataRetriever.OPTION_CLOSEST);  //works in microseconds !
					
					fileUri = getOutputMediaFileUri(MEDIA_TYPE_IMAGE);
				
					try {
						FileOutputStream out = new FileOutputStream(mediaFilePic);
						bitmap.compress(Bitmap.CompressFormat.PNG, 90, out);
						out.flush();
						out.close();
					} catch (Exception e) {
						e.printStackTrace();
						Toast t = Toast.makeText(VideoFrames.this, "Error: Bitmap to stored .PNG file not created", Toast.LENGTH_LONG);
				     	t.show();
					}
					
				}
				
				retriever.release();
				// After splitting into frames, store data in text file
				SaveText();
			}
		});
		
	} /** END OF ONCREATE */
	
	private boolean isDeviceSupportCamera() {
        if (getApplicationContext().getPackageManager().hasSystemFeature(
                PackageManager.FEATURE_CAMERA)) {
            // this device has a camera
            return true;
        } else {
            // no camera on this device
            return false;
        }
    }
	
	/** RECORED AND PREVIEW PICTURES (.JPG) */
	
	private void captureImage() {
		
	    Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
	    
	    fileUri = getOutputMediaFileUri(MEDIA_TYPE_IMAGE);
	    
	    intent.putExtra(MediaStore.EXTRA_OUTPUT, fileUri);
	    
	    // start the image capture Intent
	    startActivityForResult(intent, CAMERA_CAPTURE_IMAGE_REQUEST_CODE);
	}
	
	private void previewCapturedImage() {
        try {
            // hide video preview
            videoPreview.setVisibility(View.GONE);
 
            imgPreview.setVisibility(View.VISIBLE);
 
            // bimatp factory
            BitmapFactory.Options options = new BitmapFactory.Options();
 
            // downsizing image as it throws OutOfMemory Exception for larger
            // images
            options.inSampleSize = 8;
 
            final Bitmap bitmap = BitmapFactory.decodeFile(fileUri.getPath(), options);
 
            imgPreview.setImageBitmap(bitmap);
        } catch (NullPointerException e) {
            e.printStackTrace();
        }
    }
	
	/** RECORED AND PREVIEW VIDEO (.MP4) */
	
	private void recordVideo() {
	    Intent intent = new Intent(MediaStore.ACTION_VIDEO_CAPTURE);
	 
	    fileUri = getOutputMediaFileUri(MEDIA_TYPE_VIDEO);
	 
	    // set video quality
	    // 1- for high quality video
	    intent.putExtra(MediaStore.EXTRA_VIDEO_QUALITY, 1);
	    
	    intent.putExtra(MediaStore.EXTRA_OUTPUT, fileUri);
	 
	    refTime = System.currentTimeMillis(); //Start count from video camera call
	    
	    System.out.println("Size of Video File: " + mediaFileVideo.length() + "!!!");
	    dataAcc = true; // START THE ACCELEROMETER DATA INPUT
	    
	    // start the video capture Intent
	    startActivityForResult(intent, CAMERA_CAPTURE_VIDEO_REQUEST_CODE);
	}
	
	private void previewVideo() {
        try {
            // hide image preview
            imgPreview.setVisibility(View.GONE);
 
            videoPreview.setVisibility(View.VISIBLE);
            videoPreview.setVideoPath(fileUri.getPath());
            // start playing
            videoPreview.start();
                          
            videoPreview.setOnPreparedListener(new OnPreparedListener() {
            	
            	//Gets the video time
                public void onPrepared(MediaPlayer mp) {
                	videoTime = videoPreview.getDuration();
                	System.out.println("Video Time1: " + videoTime);
                }
            });
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
	
	//ACTIVITY ON RESULT
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
	    // if the result is capturing Image
		
		refTimeSensor = System.currentTimeMillis();
        
    	Toast.makeText(this,"Video End: RefTimeSensor", Toast.LENGTH_SHORT).show();
    	
    	dataAcc = false;
    	dataAcc0 = false;
    	
    	refTime = System.currentTimeMillis();
		Log.d("tagged", "Time End: " + refTime);
		Log.d("tagged", "Video Time2: " + (refTime - refTimeSensor));
    	
	    if (requestCode == CAMERA_CAPTURE_IMAGE_REQUEST_CODE) {
	        if (resultCode == RESULT_OK) {
	        	
	            // successfully captured the image
	            // display it in image view
	            previewCapturedImage();
	        } else if (resultCode == RESULT_CANCELED) {
	            // user cancelled Image capture
	            Toast.makeText(getApplicationContext(),
	                    "User cancelled image capture", Toast.LENGTH_SHORT).show();
	        } else {
	            // failed to capture image
	            Toast.makeText(getApplicationContext(),
	                    "Sorry! Failed to capture image", Toast.LENGTH_SHORT).show();
	        }
	    } else if (requestCode == CAMERA_CAPTURE_VIDEO_REQUEST_CODE) {
            if (resultCode == RESULT_OK) {
            	
                // video successfully recorded
                // preview the recorded video
                previewVideo();
            } else if (resultCode == RESULT_CANCELED) {
                // user cancelled recording
                Toast.makeText(getApplicationContext(),
                        "User cancelled video recording", Toast.LENGTH_SHORT)
                        .show();
            } else {
                // failed to record video
                Toast.makeText(getApplicationContext(),
                        "Sorry! Failed to record video", Toast.LENGTH_SHORT)
                        .show();
            }
        }
	}
	
	//HELPER METHODS BECAUSE using fileUri will give us a NullPointerException
	@Override
	protected void onSaveInstanceState(Bundle outState) {
	    super.onSaveInstanceState(outState);
	 
	    // save file url in bundle as it will be null on scren orientation
	    // changes
	    outState.putParcelable("file_uri", fileUri);
	}
	
	@Override
	protected void onRestoreInstanceState(Bundle savedInstanceState) {
	    super.onRestoreInstanceState(savedInstanceState);
	 
	    // get the file url
	    fileUri = savedInstanceState.getParcelable("file_uri");
	}
	
	//*********************************************
	/**
     * Creating file uri to store image/video
     */
    public Uri getOutputMediaFileUri(int type) {
        return Uri.fromFile(getOutputMediaFile(type));
    }
 
    /** returning image / video */
    private static File getOutputMediaFile(int type) {
 
        // External sdcard location
        mediaStorageDir = new File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), IMAGE_DIRECTORY_NAME);
 
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
        	
        	if (refTime > 0) {
        		timeStamp = timeStamp + "_" + inn;
        		System.out.println(timeStamp);
        		imageNames.add("IMG_" + timeStamp + ".png");
        		dataName1 += "IMG_" + timeStamp + ".png\n";
        	}
            mediaFilePic = new File(mediaStorageDir.getPath() + File.separator + "IMG_" + timeStamp + ".png");
            return mediaFilePic;
        } else if (type == MEDIA_TYPE_VIDEO) {
            mediaFileVideo = new File(mediaStorageDir.getPath() + File.separator + "VID_" + timeStamp + ".mp4");
            dataAcc0 = true;
            return mediaFileVideo;
        } else {
            return null;
        }
    }
    
    //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // WRITING TO TXT FILE
    //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    public void SaveText(){    
        
        try {
        	timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        	
        	//Choose text file location and name, then create new file
        	mediaStorageDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), IMAGE_DIRECTORY_NAME);
        	txtData = new File(mediaStorageDir.getPath() + File.separator + "Data_" + timeStamp + ".txt");
        	txtData.createNewFile();
        	
        	//Choose text file to output too, startup text writer
        	FileOutputStream fOut = new FileOutputStream(txtData);
            OutputStreamWriter myOutWriter = new OutputStreamWriter(fOut);
            
            //Write to text file, close writer and file
            myOutWriter.append("Names: " + dataName1 + "\n\nAccelerometer: " + dataAcc1 + "\n\nGyroscope: " + dataGyro1);
            myOutWriter.close();
            fOut.close();
        	
        	} catch (Exception e) {
        		Toast.makeText(this,"Txt writing Exception!",Toast.LENGTH_LONG).show();
        	  e.printStackTrace();
        	}
               
    }
    
    ////////////////////////////////////////////////
    //ACCELEROMETER/GYRO
    ////////////////////////////////////////////////
	@Override
	public void onSensorChanged(SensorEvent event) {
		// TODO Auto-generated method stub
		
		if(dataAcc){
			
			long times = refTimeSensor - refTime;
			System.out.println("Video Button start: " + times);
			
			dataAcc = false;
		}

		if(dataAcc0){
			Log.w("tagg", "Size of Video File: " + mediaFileVideo.length() + "!!!");
			refTimeSensor = System.currentTimeMillis();
			Log.d("tagged", "Time Start: " + refTimeSensor);
		}
		
		//checks if the sensor is the accelerometer and if data collection is enabled
		if ((event.sensor).equals(accelerometer) && dataAcc){
			accX.setText("X: " + event.values[0]);
			accY.setText("Y: " + event.values[1]);
			accZ.setText("Z: " + event.values[2]);
			
			dataAcc1 = "\nX: " + event.values[0] + "\tY: " + event.values[1] + "\tZ: " + event.values[2];
		}
		
		//checks if the sensor is the gyroscope, if it accurate and if data collection is enabled
		if ( ((event.sensor).equals(gyro)) && (event.accuracy != SensorManager.SENSOR_STATUS_UNRELIABLE) && dataGyro){
			gyroRoll.setText("X: " + event.values[2]);
			gyroPitch.setText("Y: " + event.values[1]);
			gyroYaw.setText("Z: " + event.values[0]);
			
			dataGyro1 = "\nX: " + event.values[2] + "\tY: " + event.values[1] + "\tZ: " + event.values[0];
		} 
		
	}

	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) {
		// TODO Auto-generated method stub
		
	}
}
