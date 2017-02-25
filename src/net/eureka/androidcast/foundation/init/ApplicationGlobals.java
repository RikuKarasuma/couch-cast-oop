package net.eureka.androidcast.foundation.init;

import java.io.File;

import net.eureka.androidcast.foundation.config.Configuration;
import net.eureka.androidcast.player.Static;

/**
 * Abstract class so it can't be Instantiated. Contains global application variables such as the operating system, application directory (\<USER>\PCCast\),
 * log encoding and the JVM process ID.
 * 
 * @author Owen McMonagle.
 *
 */
public abstract class ApplicationGlobals 
{
	
	/**
	 * Used by the {@link MediaPlayer} to decide whether or not to minimize all other desktop windows when the play button is pressed. 
	 */
	private static boolean minimizeWindows = true;
	
	private static boolean musicMode = false;
	
	private static final String INSTALL_DIRECTORY =  File.separator + "Android Cast Media Server" + File.separator;
	
	private static final String INSTALL_DIRECTORY_PATH = 
			System.getenv("SYSTEMDRIVE") + File.separator + ( (Static.is64BitArch()) ? "Program Files (x86)" : "Program Files" ) + INSTALL_DIRECTORY;
	

	public static boolean isMinimizeWindows() 
	{
		new Configuration();
		return minimizeWindows;
	}

	public static void setMinimizeWindows(boolean minimizeWindows) 
	{
		ApplicationGlobals.minimizeWindows = minimizeWindows;
	}

	public static boolean isMusicMode() 
	{
		new Configuration();
		return musicMode;
	}

	public static void setMusicMode(boolean musicMode)
	{
		ApplicationGlobals.musicMode = musicMode;
	}
	
	public static String getInstallPath()
	{
		return INSTALL_DIRECTORY_PATH;
	}
	
	
}
