package net.eureka.couchcast.foundation.config;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;

import net.eureka.couchcast.Logger;
import net.eureka.couchcast.foundation.init.ApplicationGlobals;
import net.eureka.couchcast.foundation.init.NetworkGlobals;
import net.eureka.couchcast.player.Static;


/**
 * Handles the creation, writing and reading of the configuration file located within the application directory. The configuration file holds
 * values such as the server name, download directory and process ID of the current or last session.
 * 
 * @author Owen McMonagle.
 *
 * @see InitialiseFoundation 
 * @see ApplicationGlobals
 * @see NetworkGlobals
 * @see Logger
 */
public final class Configuration
{
	/**
	 * Name of the configuration file.
	 */
	private static final String CONFIG_FILE = "config.txt";
	
	public Configuration() 
	{
		checkConfigurationFile();
	}
	
	/**
	 * Attempts to retrieve and verify the existence of the configuration file. If the configuration file exists, then
	 * the file is read from and data updated. If not the file is created and data is written to it. 
	 */
	private static void checkConfigurationFile()
	{
		// Retrieve configuration file.
		File configuration_file = getConfigurationFile();
		// If configuration file exists...
		if(configuration_file.exists())
			// read from it.
			readFromConfigurationFile();
	}
	
	/**
	 * Reads from the configuration file then updates the data retrieved from it.
	 */
	private static void readFromConfigurationFile()
	{
		// Retrieve configuration file.
		File configuration_file = getConfigurationFile();
		try
		{
			// Open a buffered stream to file, with encoding of UTF-8.
			BufferedReader buffered_reader = new BufferedReader(new InputStreamReader(new FileInputStream(configuration_file), "UTF-8"));
			// Ignore first line.
			buffered_reader.readLine();
			// Ignore server name.
			buffered_reader.readLine();
			// Ignore media directory.
			buffered_reader.readLine();
			// Ignore process id line.
			buffered_reader.readLine();
			// Get is minimised
			boolean is_minimized = Boolean.parseBoolean(buffered_reader.readLine());
			// Ignore deep search.
			buffered_reader.readLine();
			boolean is_music_mode = Boolean.parseBoolean(buffered_reader.readLine());
			// Ignore file/folder search delay.
			buffered_reader.readLine();
			// Ignore update media delay.
			buffered_reader.readLine();
			// Get name of network interface.
			String network_interface_name = buffered_reader.readLine();
			if(NetworkGlobals.getDHCPInterface() == null)
				// Set DHCP interface and name.
				NetworkGlobals.setDhcpNetwork(Static.getInetAddressFromName(network_interface_name));
			// Set minimised.
			ApplicationGlobals.setMinimizeWindows(is_minimized);
			ApplicationGlobals.setMusicMode(is_music_mode);
			// Close file stream.
			buffered_reader.close();
		} 
		catch (IOException e) 
		{
			e.printStackTrace();
			// Append to log if error.
			Logger.append(new StringBuffer(e.getMessage()));
		}
	}
	
	/**
	 * Retrieves the configuration file from the application directory.
	 * @return File - Configuration file of the server.
	 */
	private static File getConfigurationFile()
	{
		// Retrieve the system drive directory.
		String system_drive = System.getenv("SYSTEMDRIVE"),
			   // Retrieve the default home path.
			   home_path = System.getenv("HOME");
		// If home path is null, try the updated renamed home path.
		home_path = ((home_path == null) ? System.getenv("HOMEPATH") : home_path);
		
		// Create default home/download directory path. 
		final StringBuffer directory_path = new StringBuffer(system_drive+home_path+File.separator+ApplicationGlobals.getName()+File.separator);
		return new File(directory_path+CONFIG_FILE.toString());
	}
}
