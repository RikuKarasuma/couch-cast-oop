package net.eureka.couchcast.player.control;

import java.nio.charset.Charset;


import com.sun.jna.Native;
import com.sun.jna.platform.win32.WinDef.HWND;
import com.sun.jna.win32.W32APIOptions;

import net.eureka.couchcast.player.Static;
 
public final class ActivateWindow
{
    private interface User32 extends W32APIOptions
    {
          User32 instance = (User32) Native.loadLibrary("user32", User32.class, DEFAULT_OPTIONS);
          boolean ShowWindow(HWND hWnd, int nCmdShow);
          boolean SetForegroundWindow(HWND hWnd);
          HWND FindWindow(String winClass, String title);
          int SW_SHOW = 1;
    }
    
    public static void setFocusedWindow(String window_title)
    {
    	final String utf_16_charset_name = (Static.isEndianessBig()) ? "UTF-16BE" : "UTF-16LE";
    	Charset system_utf_16_default_charset = Charset.forName(utf_16_charset_name);
    	String cast_utf_16_window_title = new String(window_title.getBytes(system_utf_16_default_charset), system_utf_16_default_charset);
    	HWND window_handle = WindowSearcher.getWindow(cast_utf_16_window_title);
    	setFocusedWindow(window_handle);
    }
    
    public static void setFocusedWindow(HWND hWnd)
    {
    	User32 user32 = User32.instance;
        user32.ShowWindow(hWnd, User32.SW_SHOW);
        user32.SetForegroundWindow(hWnd);
    }
}