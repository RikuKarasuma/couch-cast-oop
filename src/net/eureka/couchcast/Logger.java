package net.eureka.couchcast;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;

import net.eureka.couchcast.foundation.init.ApplicationGlobals;

/**
 * Server logger. Only needs to be instantiated once. Attempts to find the log file via application directory, if not found creates a new one.
 * After the log is set up, the only method needed to be called to write to it is append(StringBuffer... text) below.
 * 
 * @author Owen McMonagle.
 *
 * @see ApplicationGlobals
 * @see InitialiseFoundation
 */
public final class Logger 
{
	/**
	 * Name of the log file.
	 */
	private static final StringBuffer LOGGING_FILE = new StringBuffer(File.separator+"log.txt");
	
	/**
	 * Appends new text log. Can accept any amount of StringBuffer parameters. Printing each object as a new line in the log.
	 * @param StringBuffer... text - Each StringBuffer is a line to print in log.
	 */
	public static void append(StringBuffer... text)
	{
		try 
		{
			// Retrieve the system drive directory.
			String system_drive = System.getenv("SYSTEMDRIVE"),
				   // Retrieve the default home path.
				   home_path = System.getenv("HOME");
			// If home path is null, try the updated renamed home path.
			home_path = ((home_path == null) ? System.getenv("HOMEPATH") : home_path);
			
			// Create default home/download directory path. 
			final StringBuffer directory_path = new StringBuffer(system_drive+home_path+File.separator+ApplicationGlobals.getName());
			directory_path.append(LOGGING_FILE);
			// Open up a buffered stream to the log file, set encoding to UTF-8.
			BufferedWriter buffered_writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(new File(directory_path.toString()), true), "UTF-8"));
			// Iterate through each line to print...
			for(StringBuffer line_of_text : text)
			{
				// Signal new line. \n
				buffered_writer.newLine();
				// Write new log text.
				buffered_writer.write(line_of_text.toString());
			}
			// Flush bytes down stream.
			buffered_writer.flush();
			// Close buffered stream.
			buffered_writer.close();
		}
		catch (Exception e) 
		{
			e.printStackTrace();
		}
	}
}
