package net.eureka.androidcast.player.control;

import java.nio.charset.Charset;

import net.eureka.androidcast.player.Static;

import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.platform.win32.WinDef.HWND;
import com.sun.jna.platform.win32.WinUser;
import com.sun.jna.platform.win32.WinUser.WNDENUMPROC;
import com.sun.jna.win32.StdCallLibrary;


public final class WindowSearcher 
{
	private static HWND foundWindow = null;
	
	private interface User32 extends StdCallLibrary
	{
	      User32 INSTANCE = (User32) Native.loadLibrary("user32", User32.class);
	      boolean EnumWindows(WinUser.WNDENUMPROC lpEnumFunc, Pointer arg);
	      int GetWindowTextW(HWND hWnd, byte[] lpString, int nMaxCount);
	}
	
	public static HWND getWindow(final String window_title_text)
	{
		foundWindow = null;
		final User32 user32 = User32.INSTANCE;
		final String system_charset_name = (Static.isEndianessBig()) ? "UTF-16BE" : "UTF-16LE" ;
       	final Charset system_charset = Charset.forName(system_charset_name);
		user32.EnumWindows(new WNDENUMPROC() 
    	{
	        @Override
	        public boolean callback(HWND hWnd, Pointer arg1)
	        {
	        	if(foundWindow != null)
	        		return true;
	        	final int buffer_length = 2048;
	        	byte[] windowText = new byte[buffer_length];
	           	user32.GetWindowTextW(hWnd, windowText, buffer_length);
           		String wText = new String(windowText, system_charset).trim();
	            if (wText.isEmpty()) 
	               return true;
	            if(window_title_text.equals(wText))
	            	foundWindow = hWnd;
	            return true;
	        }
        }, null);
		return foundWindow;
	}
}
