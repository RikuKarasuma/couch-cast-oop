package net.eureka.couchcast.foundation.init;

import java.io.File;

import net.eureka.couchcast.foundation.config.Configuration;

/**
 * Contains global application variables such as the operating system, application directory (\<USER>\PCCast\),
 * log encoding and the JVM process ID.
 * 
 * @author Owen McMonagle.
 *
 */
public final class ApplicationGlobals 
{
	
	/**
	 * Used by the {@link MediaPlayer} to decide whether or not to minimize all other desktop windows when the play button is pressed. 
	 */
	private static boolean minimizeWindows = true;
	
	private static boolean musicMode = false;
	
	/**
	 * Name of the program.
	 */
	private static final byte[] name = "Couch Cast".getBytes();
	
	private static final String INSTALL_DIRECTORY =  File.separator + new String(name) + File.separator;
	
	private static final String INSTALL_DIRECTORY_PATH = // (Static.is64BitArch()) ? System.getenv("PROGRAMFILES") + 
			System.getenv("SYSTEMDRIVE") + File.separator + "Program Files" + INSTALL_DIRECTORY;
	
	public static String getName()
	{
		return new String(name);
	}

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
