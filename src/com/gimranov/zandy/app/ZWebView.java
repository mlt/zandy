/**
 * 
 */
package com.gimranov.zandy.app;

import android.content.Context;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.webkit.WebView;

/**
 * @author mlt
 *
 */
public class ZWebView extends WebView {
	public ZWebView(Context context) {
		super(context);
		// TODO Auto-generated constructor stub
	}
	public ZWebView(Context context, AttributeSet attrs) {
		super(context, attrs);
		// TODO Auto-generated constructor stub
	}
	public ZWebView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		// TODO Auto-generated constructor stub
	}
	// FIXME: Find out how to send key events to TinyMCE without default WebView bahavior
	// Also see this: http://forum.xda-developers.com/showthread.php?t=1064013
	/*
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        return true;
    }    
    @Override
    public boolean onKeyLongPress(int keyCode, KeyEvent event) {
        return true;
    }    
    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        return true;
    } 
    */   
}
