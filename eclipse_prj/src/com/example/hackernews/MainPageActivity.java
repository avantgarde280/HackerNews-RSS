package com.example.hackernews;


import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Timer;
import java.util.TimerTask;

import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.view.Menu;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.TextView;
import android.widget.Toast;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class MainPageActivity extends Activity {

	private ArrayAdapter<String> adapter ;
	private ListView listViewRSS;
    private ArrayList<String> listRssTitle ;
    private ArrayList<String> listRssLink ;
    private static final long ONE_SECOND = 1000;
    private static final long ONE_MINUNTE =60 * 1000;
    private Handler m_handlerList;
    private Thread  downloadRssThread;
    private Timer refreshRssTask ;
    public static DownloadRssData RssOnlineData;
	final static BlockingQueue<RSSTypeObj> queueNewNews = new LinkedBlockingQueue<RSSTypeObj>(70);
	static TextView  infoUpdateTxt;
	
	private static final int MSG_UPDATE_INFO = 1;
	
	//handler to display update info in text view
	private static Handler handlerUpdateInfo = new Handler() {
	       public void handleMessage(android.os.Message msg) {
	           if (msg.what == MSG_UPDATE_INFO) {
	               String message = (String)msg.obj;
	               infoUpdateTxt.setText(message);
	           }
	       }
	};
    
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main_page);
		
		infoUpdateTxt = (TextView)findViewById(R.id.txtUpdate);
		listViewRSS    = (ListView) findViewById(R.id.listRSS);
		listRssTitle   = new ArrayList<String>();
		listRssLink    = new ArrayList<String>();
		m_handlerList  = new Handler();
		refreshRssTask = new Timer();
		RssOnlineData  = new DownloadRssData();
		
		adapter = new ArrayAdapter<String>(MainPageActivity.this,
	    			android.R.layout.simple_list_item_1, listRssTitle);

	    listViewRSS.setAdapter(adapter);
	    
	    // Check if the thread is already running
	    downloadRssThread = (Thread) getLastNonConfigurationInstance();
	    if ( (!(downloadRssThread != null)) ||  (!(downloadRssThread.isAlive()))) {
	    	
	    	 downloadRssThread = new getNewRssData_thread();
	    	 
	    	 if(isNetworkConnected()){
	    		 downloadRssThread.start();// get data on start without waiting one minute
	    	 }else{
	    		 displayUpdateInfo("Last update: Connection failure!");
	    	 }
	    }
	    
        /*define user list action*/
		listViewRSS.setOnItemClickListener(new OnItemClickListener() {
        	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        		 try {
	        		/* open RSS link*/
					Uri uri = Uri.parse(listRssLink.get(position));
					Intent intent = new Intent(Intent.ACTION_VIEW, uri);
					startActivity(intent);
        		 } catch (Exception e) {
     		        e.printStackTrace();
     		     }
            }
         });
		
		 startTaskListUpdate(); //start task to update RSS list
		 
		 refreshRssTask.scheduleAtFixedRate(new TimerTask(){ // timer to check new news RSS 
			    public void run() {
			        onTimerTick_getNewRss();
			    }
			}, 0, ONE_MINUNTE);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.activity_main_page, menu);
		return true;
	}
	
    @Override
    protected void onResume() {
    	startTaskListUpdate();//start to update RSS list
    	super.onResume();
    }

    @Override
    protected void onPause() {
    	stopTaskListUpdate();//stop to update RSS list
    	if(downloadRssThread.isAlive()){
    		try {
    			downloadRssThread.interrupt();
				downloadRssThread.join(ONE_SECOND);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
    	}
    	super.onPause();
    }
    
    @Override
    protected void onDestroy() {
    	refreshRssTask.cancel();
    	queueNewNews.clear();
    	super.onDestroy();
    }
    
    
	//******************************
    // return true if device connected
	//******************************
    private boolean isNetworkConnected() {
        ConnectivityManager connectivity  = (ConnectivityManager) 
        				getSystemService(Context.CONNECTIVITY_SERVICE);
        
        NetworkInfo networkInfo = connectivity.getActiveNetworkInfo();
        
        //networkInfo is null for no network available
        if (networkInfo != null && networkInfo.isConnected()) {
            return true;
        }
        return false;
    } 
    
	//******************************
	//update RSS list task
	//******************************
	private Runnable Task_RssListUpdate = new Runnable() {

	    public void run() {
	        try{
		    	
	        	RSSTypeObj oneNewitem = queueNewNews.poll();
	        	if(oneNewitem != null){
	        		
	        		//put data on top
		        	listRssTitle.add(0,oneNewitem.Tittle);
		        	listRssLink.add(0,oneNewitem.LinkArticle);
			    	
		        	//notify that data has change
		        	adapter.notifyDataSetChanged();
	        	}
		    	
	            m_handlerList.postDelayed(Task_RssListUpdate, ONE_SECOND); //trigger again
	        }
	        finally{

	        }
	    }
	};
	
	//******************************
	//start task to update RSS list
	//******************************
	void startTaskListUpdate(){
		Task_RssListUpdate.run(); 
	}

	//******************************
	//stop task to update RSS list
	//******************************
	void stopTaskListUpdate(){
		m_handlerList.removeCallbacks(Task_RssListUpdate);
	}
	
	//******************************
	//task call to get new news RSS
	//******************************
	private void onTimerTick_getNewRss(){
		 if(isNetworkConnected()){//if got connection
			
			 if (!downloadRssThread.isAlive()){ // if thread not alive
				downloadRssThread = new getNewRssData_thread();
		    	downloadRssThread.start();
			}
		 }else{
			 displayUpdateInfo("Last update: Connection failure!");
		 }
	}

	//********************************************************************
	//Display update info
	//********************************************************************
	private static void displayUpdateInfo(String st) {
		   Message msg = new Message();
		   msg.what = MSG_UPDATE_INFO;
		   msg.obj = st;
		   handlerUpdateInfo.sendMessage(msg);
	}
	
	//********************************************************************
	//thread to get new news RSS --> this thread goes online for new RSS
	//********************************************************************
	static public class getNewRssData_thread extends Thread {
		
		/*note: this thread will be executed only once at time*/
	  	
		@Override
	    public void run() {
	      try {
	    	  Calendar c = Calendar.getInstance();
	    	  SimpleDateFormat df = new SimpleDateFormat("HH:mm:ss");
	    	  final String formattedTime = df.format(c.getTime());
	    	  
	    	  displayUpdateInfo("Update in progress ...");

	    	  //go online and refresh RSS data
	    	  RssOnlineData.updateRssData();
	    	  
	    	  //put new data to queue
	    	  for (RSSTypeObj ix : RssOnlineData.newNewsRss) {
	    		  queueNewNews.offer(ix);
	    	  }
	    	  
	    	  displayUpdateInfo("Last update: "+formattedTime);
	    	  
	      } catch (Exception e) {
	        e.printStackTrace();
	      } finally {
	      }
	  }
	}		
}
