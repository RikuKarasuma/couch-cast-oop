package net.eureka.androidcast.player.control;

import com.sun.jna.Native;
import com.sun.jna.platform.win32.WinDef.HWND;
import com.sun.jna.win32.W32APIOptions;


public class DesktopControl 
{
	
	
	public interface User32 extends W32APIOptions
	{
		public static final String SHELL_TRAY_WND = "Shell_TrayWnd";
		public static final int WM_COMMAND = 0x111;
		public static final int MIN_ALL = 0x1a3;
		public static final int MIN_ALL_UNDO = 0x1a0;
	    User32 instance = (User32) Native.loadLibrary("user32", User32.class, DEFAULT_OPTIONS);
	    HWND FindWindow(String winClass, String title);
      	long SendMessageA(HWND hWnd, int msg, int num1, int num2);
      	
	}
	
	public static void minimizeAll()
	{
		// get the taskbar's window handle
		HWND shellTrayHwnd = User32.instance.FindWindow(User32.SHELL_TRAY_WND, null);
	    //use it to minimize all windows
	    minimize(shellTrayHwnd);
	}
	
	public static void minimize(HWND window)
	{
		// Minimize a window.
		User32.instance.SendMessageA(window, User32.WM_COMMAND, User32.MIN_ALL, 0);
	}
	
	public static void restoreAll()
	{
		//get the taskbar's window handle
		HWND shellTrayHwnd = User32.instance.FindWindow(User32.SHELL_TRAY_WND, null);
		// then restore previously minimized windows
		restore(shellTrayHwnd);
	}
	
	public static void restore(HWND window)
	{
		User32.instance.SendMessageA(window, User32.WM_COMMAND, User32.MIN_ALL_UNDO, 0);
	}
}
