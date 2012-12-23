package com.example.hackernews;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Queue;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

public class DownloadRssData {
	
	final int MAX_ITEMS_TO_TRACK = 99;  //Maximum news titles to remember(track) 
	public Queue <String> localTitlesList_track;   //contain previous RSS titles
	public ArrayList<RSSTypeObj> newNewsRss; 	   //contain new RSS entry
	
	DownloadRssData(){
		localTitlesList_track = new  LinkedList<String>();
		newNewsRss         	  = new ArrayList<RSSTypeObj>();
	}
	
	public void updateRssData(){
		
		String url_add ="http://news.ycombinator.com/rss";
				
		try {
			  URL url = new URL(url_add);
			  HttpURLConnection con = (HttpURLConnection) url.openConnection();
			  con.setUseCaches(false);
			  con.setConnectTimeout (10000);
			  
			  parseRSSData(con.getInputStream());
			  
			  } catch (Exception e) {
			  e.printStackTrace();
			}
	}
	
	private void parseRSSData(InputStream inStream){
		
		try {
			XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
			factory.setNamespaceAware(false);
			XmlPullParser xpp = factory.newPullParser();
			
			//set input
			xpp.setInput(inStream, "UTF_8");
			
			// flag set to true if inside <item> tag , just to be sure that we get information for a valid RSS item
			boolean insideItem = false;
			//set to true if new news found
			boolean newNewsFound_flag =false;
			
			String rssTitle = "empty_try";
			String rssLinkArticle = "empty_try";
			
			//Type of current event: START_TAG, END_TAG ...
			int eventType = xpp.getEventType();
			
			newNewsRss.clear();
			
			while (eventType != XmlPullParser.END_DOCUMENT) {
				
				if (eventType == XmlPullParser.START_TAG) {
					if (xpp.getName().equalsIgnoreCase("item")) {
						insideItem = true; //new item entry
					} else if (xpp.getName().equalsIgnoreCase("title")) {
						if (insideItem){
							
							//get item title
							rssTitle = xpp.nextText();
							//check if new news
							if (! localTitlesList_track.contains(rssTitle)){// if found new news
								newNewsFound_flag=true;
								localTitlesList_track.offer(rssTitle);
							}
						}
					} else if (xpp.getName().equalsIgnoreCase("link")) {
						if (insideItem){ 
							
							//get item link
							if(newNewsFound_flag){
								rssLinkArticle=xpp.nextText();
							}
						}
					}
				}else if(eventType==XmlPullParser.END_TAG && xpp.getName().equalsIgnoreCase("item")){
					
					if(newNewsFound_flag){ //add to list if new News
						RSSTypeObj rssdataEntry = new RSSTypeObj(rssTitle,rssLinkArticle); 
						newNewsRss.add(rssdataEntry);
					}
					
					insideItem=false; //leaving the item tag
					newNewsFound_flag=false; //rest new flag
				}
				
				eventType = xpp.next(); //go to next element
			}
			
		// exception catch
		} catch (MalformedURLException e) {
			e.printStackTrace();
		} catch (XmlPullParserException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		//remove news items which exceed maximum number to track
		 int ItemToClear=localTitlesList_track.size() - MAX_ITEMS_TO_TRACK;
		 if(ItemToClear>0){
			 for(int ix=0;ix<ItemToClear;ix++){
				 localTitlesList_track.poll();
			 }
		 }
	}
}