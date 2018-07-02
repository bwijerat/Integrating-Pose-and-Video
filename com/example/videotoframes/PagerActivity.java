/*
@By: Brian Wijeratne
@Project: Integrating Pose and Stereo Video Data for Immersive Mixed Reality
@Supervisor: Matthew Kyan
May 2014 - August 2014
*/

package com.example.videotoframes;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
 
/**
 * PagerActivity: A Sample Activity for PagerContainer
 */
public class PagerActivity extends Activity{
	
	//Data Gallery
	PagerContainer mContainer;
	ScrollView sv;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
        setContentView(R.layout.scrollview_test);
        
        LinearLayout llbuttons = (LinearLayout) findViewById(R.id.lladdButtons);
		
		for(int i = 0; i < 23; i++){
			
			/*
			Button b1 = new Button(this);
		    b1.setText("NEW");
			llbuttons.addView(b1); */
			
			LinearLayout ll1 = instantiateView();
			llbuttons.addView(ll1);
		}
        
/*		b1.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				
				
			}
			
		}); */
		
        /*
        mContainer = (PagerContainer) findViewById(R.id.pager_container1);
 
        ViewPager pager = mContainer.getViewPager();
        PagerAdapter adapter = new MyPagerAdapter();
        pager.setAdapter(adapter);
        //Necessary or the pager will only have one extra page to show
        // make this at least however many pages you can see
        pager.setOffscreenPageLimit(adapter.getCount());
        //A little space between pages
        pager.setPageMargin(15);
 
        //If hardware acceleration is enabled, you should also remove
        // clipping on the pager for its children.
        pager.setClipChildren(false);  
        
        LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
	    View v1 = inflater.inflate(R.layout.view_pager, null);

	    // Find the ScrollView 
	    sv = (ScrollView) v1.findViewById(R.id.svHorizontalScroll);

	    // Create a LinearLayout element
	    LinearLayout ll = new LinearLayout(PagerActivity.this);
	    ll.setOrientation(LinearLayout.VERTICAL);

	    // Add text
	    TextView tv = new TextView(PagerActivity.this);
	    tv.setText("my text");
	    ll.addView(tv); 

	    // Add the LinearLayout element to the ScrollView
	    sv.addView(ll);

	    // Display the view
	    setContentView(v1); */
	}
	
	public LinearLayout instantiateView(){
		
		LinearLayout layout;
		
		layout = new LinearLayout(this);
		layout.setOrientation(LinearLayout.VERTICAL);
		layout.setGravity(Gravity.CENTER);
		
		final ImageView img = new ImageView(this);
		img.setMaxHeight(100);
		img.setMaxWidth(100);
    	img.setImageResource(R.drawable.image01);
    	
        final TextView view1 = new TextView(this);
   //     view.setText("Item "+position);
   //     view.setGravity(Gravity.CENTER);
   //     view.setBackgroundColor(Color.argb(255, position * 50, position * 10, position * 50)); 
        view1.setText("TEST 123 123");
        
        final TextView view2 = new TextView(this);
        view2.setText("TEST 4567893 123");
    	
        LinearLayout layout1 = new LinearLayout(this);
		layout1.setOrientation(LinearLayout.VERTICAL);
		layout1.setGravity(Gravity.CENTER);
		
		layout1.addView(view1);
        layout1.addView(view2);
        
        LinearLayout layout2 = new LinearLayout(this);
		layout2.setOrientation(LinearLayout.HORIZONTAL);
		layout2.setGravity(Gravity.CENTER);
		
		Button b1 = new Button(this);
		b1.setWidth(50);
		b1.setHeight(50);
        
        b1.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				
				img.setImageResource(R.drawable.red_x);
				view2.setText("BRO");
			}
        	
		});
        
        layout2.addView(layout1);
		layout2.addView(b1);
        
   //     layout.addView(img, new LinearLayout.LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT));
        layout.addView(img);
        layout.addView(layout2);
		
        return layout;
	}
	
    //Nothing special about this adapter, just throwing up colored views for demo
    public class MyPagerAdapter extends PagerAdapter {
    	
    	@Override
        public Object instantiateItem(ViewGroup container, int position) {
        	
    		LinearLayout layout = new LinearLayout(PagerActivity.this);
    		layout.setOrientation(LinearLayout.VERTICAL);
    		layout.setGravity(Gravity.CENTER);
    		layout.setClickable(true);
    		
    	/*	final ImageView img = new ImageView(PagerActivity.this);
        	img.setImageResource(R.drawable.image01);
        	img.setMinimumHeight(200);
        	img.setMinimumWidth(100); */
        	
            TextView view1 = new TextView(PagerActivity.this);
       //     view.setText("Item "+position);
       //     view.setGravity(Gravity.CENTER);
       //     view.setBackgroundColor(Color.argb(255, position * 50, position * 10, position * 50)); 
            view1.setText("TEST 123 123");
            
            TextView view2 = new TextView(PagerActivity.this);
            view2.setText("TEST 4567893 123");
        	
       //     layout.addView(img, new LinearLayout.LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT));
       //     layout.addView(img);
            layout.addView(view1);
            layout.addView(view2);
            
            layout.setOnClickListener(new OnClickListener() {
    			public void onClick(View v) {
    				 
    			//	img.setImageResource(R.drawable.image02);
    			}
    		});
            
            container.addView(layout);
            return layout;
        }
    	
        @Override
        public void destroyItem(ViewGroup container, int position, Object object) {
            container.removeView((View)object);
        }
 
        @Override
        public int getCount() {
            return 4;
        }
 
        @Override
        public boolean isViewFromObject(View view, Object object) {
            return (view == object);
        }
    }
}