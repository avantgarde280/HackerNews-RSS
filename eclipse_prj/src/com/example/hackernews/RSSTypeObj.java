package com.example.hackernews;

public class RSSTypeObj {

	public String Tittle;
	public String LinkArticle;
	
	public RSSTypeObj(){
		Tittle="empty";
		LinkArticle="empty";
	}
	public RSSTypeObj(String title_l,String LinkArticle_l){
		Tittle=title_l;
		LinkArticle=LinkArticle_l;
	}

}
