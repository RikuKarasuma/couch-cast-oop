package net.eureka.couchcast.mediaserver;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;

import net.eureka.couchcast.Logger;
import net.eureka.couchcast.foundation.config.Configuration;
import net.eureka.couchcast.foundation.init.NetworkGlobals;
import net.eureka.couchcast.player.MediaPlayer;
import net.eureka.couchcast.player.Static;

public final class Bridge
{
	
	private static final int PORT = 63054, TIMEOUT = 4000, CONNECTION_BACKLOG = 100;
	
	private static ServerSocket server = null;
	
	private static Socket socket = null;
	
	public static void main(String[] args)
	{
		new Configuration();
		new Bridge();
	}
	
	Bridge()
	{
		startServer();
		startListening();
	}
	
	private static void startServer()
	{
		try
		{
			server = new ServerSocket(PORT, CONNECTION_BACKLOG, NetworkGlobals.getDHCPInterface());
			server.setSoTimeout(TIMEOUT);
		}
		catch (IOException e) 
		{
			e.printStackTrace();
		}
	}
	
	private static void startListening()
	{
		try
		{
			System.out.println("Waiting....");
			socket = server.accept();
			new BridgeIO();
		}
		catch (IOException e) 
		{
			e.printStackTrace();
		}
	}
	
	
	private static final class BridgeIO
	{
		/**
		 *	 Returned to client when a command has been successful.
		 */
		//private static final byte[] PLAYER_SUCCESS_SEQUENCE = new byte[]{ 120, 45};

		/**
		 * 	Returned to client when a command has failed.
		 */
		//private static final byte[] PLAYER_FAILURE_SEQUENCE = new byte[]{ 120, 80};
		
		/**
		 *	Sets the OOP video frame to invisible.
		 */
		private static final byte[] HIDE_FRAME_SEQUENCE = new byte[]{ 80, 10};

		/**
		 * 	Sets the OOP video frame to visible.
		 */
		private static final byte[] SHOW_FRAME_SEQUENCE = new byte[]{ 80, 20};
		
		/**
		 * 	Play media file.
		 */
		private static final byte[] PLAY_FILE_SEQUENCE = new byte[] { 110, 40};
		
		/**
		 * 	Play media file.
		 */
		private static final byte[] PLAY_TUBE_SEQUENCE = new byte[] { 120, 40};
		
		/**
		 * 	Plays or Pauses media
		 */
		private static final byte[] PLAY_PAUSE_SEQUENCE = new byte[]{ 40, 21};
		
		/**
		 * 	Stops current media.
		 */
		private static final byte[] STOP_FILE_SEQUENCE = new byte[]{ 25, 55};
		
		/**
		 * 	Enable/Disable Fast forward.
		 */
		private static final byte[] FAST_FORWARD_SEQUENCE = new byte[]{ 58, 35};
		
		/**
		 * 	Rewind back 5 seconds.
		 */
		private static final byte[] FAST_REWIND_SEQUENCE = new byte[]{ 57, 34};
		
		
		
		private static final int BUFFER_SIZE = 2;
		private static final String SOCKET_NOT_INITIALIZED_ERROR = "Socket is not initialized. Exiting...";
		private static byte[] read = new byte[BUFFER_SIZE];
		private static boolean runCommand = false;
		private static int index = -1;
		private static ObjectOutputStream output = null;
		private static ObjectInputStream input = null;
		private static MediaPlayer player = null;
		private static String path = null;
		
		BridgeIO()
		{
			if(socket != null)
			{
				player = new MediaPlayer();
				initialiseStreams();
				inputProcessingThread();
				outputProcessingThread();
			}
			else
			{
				System.err.println(SOCKET_NOT_INITIALIZED_ERROR);
				System.exit(1);
			}
		}
		
		
		/////////////////////// Synchronisation ERROR in Input bridge thread.
		
		private static void inputProcessingThread()
		{
			new Thread(new Runnable()
			{
				
				
				@Override
				public void run() 
				{
					Thread.currentThread().setName("Asynchronous Input Bridge");
					boolean connected = true;
					while(connected)
					{
						try {
							processInput();
						} catch (IOException e)
						{
							connected = false;
							// TODO Auto-generated catch block
							Logger.append(new StringBuffer(e.getMessage()));
							e.printStackTrace();
						}
					}
					System.exit(0);
				}
			}).start();
		}
		
		private static void processInput() throws IOException
		{
			// For checking number of bytes read.
			int bytes_read = 0;
			// Check if client connection has been received, if so ..
			try 
			{
				System.out.println("Inputing...");
				// Read the first two bytes into array.
				bytes_read = input.read(read, 0, BUFFER_SIZE);
				//Logger.append(new StringBuffer("Bytes read: "+read[0] +", "+read[1]));
				System.out.println("Bytes read: "+read[0] +", "+read[1]);
				// Check for file associated with command. NOTE: Would only happen if command was PLAY_FILE_SEQUENCE.
				checkForFilePath();
				//System.out.println("");
				// Check for string associated with command. NOTE: Would only happen if command was PLAY_TUBE_SEQUENCE.
				//checkForMRL();
				//System.out.println("Path:"+path);
			}
			catch (ClassNotFoundException e)
			{
				e.printStackTrace();
				Logger.append(new StringBuffer(e.getMessage()));
			}
			finally
			{
				// Check if number of bytes read equals 2, if so...
				if(bytes_read == 2)
					// Proceed to validate address and command.
					validateCommand();
			}
		}
		
		private static void outputProcessingThread()
		{
			new Thread(new Runnable()
			{
				
				@Override
				public void run()
				{
					Thread.currentThread().setName("Asynchronous Output Bridge");
					processOutput();
					System.exit(0);
				}
			}).start();
				
		}
		
		private static void processOutput()
		{
			boolean connected = true, first_write = true;
			while(connected)
				try 
				{
					System.out.println("Outputing...");
					if(!first_write)
						waitForOppositeRead();
					output.reset();
					MediaInfo info = MediaPlayer.getMediaInfo();
					output.writeObject(info);
					output.flush();
					first_write = false;
				}
				catch (IOException e) 
				{
					connected = false;
					// TODO Auto-generated catch block
					e.printStackTrace();
					Logger.append(new StringBuffer(e.getMessage()));	
				}
		}
		
		private static void waitForOppositeRead()
		{
			synchronized (Thread.currentThread()) 
			{
				try
				{
					Thread.currentThread().wait(910);
				}
				catch(InterruptedException e)
				{
					e.printStackTrace();
				}
			}
		}
		
		/**
		 * Goes through each command sequence trying to match what was read so it will know which command
		 * to run through the media player.
		 */
		private static void validateCommand()
		{
			// Check if bytes read equal PLAY_FILE_SEQUENCE...
			if(Static.compareBytes(read, PLAY_FILE_SEQUENCE))
			{
				runCommand = true;
				// Play file read in.
				play();
			}
			// Check if bytes read equal PLAY_PAUSE_SEQUENCE...
			else if(Static.compareBytes(read, PLAY_PAUSE_SEQUENCE))
			{
				runCommand = true;
				// Play or pause what is playing.
				pausePlay();
			}
			// Check if bytes read equal STOP_FILE_SEQUENCE...
			else if(Static.compareBytes(read, STOP_FILE_SEQUENCE))
			{
				runCommand = true;
				// Stop current media from playing.
				stopPlaying();
			}
			// Check if bytes read equal FAST_FORWARD_SEQUENCE...
			else if(Static.compareBytes(read, FAST_FORWARD_SEQUENCE))
			{
				runCommand = true;
				// Enable/Disable fast forward on current media.
				fastForward();
			}
			// Check if bytes read equal FAST_REWIND_SEQUENCE...
			else if(Static.compareBytes(read, FAST_REWIND_SEQUENCE))
			{
				runCommand = true;
				// Skip back 5 seconds on current media.
				fastRewind();
			}
			else if(Static.compareBytes(read, PLAY_TUBE_SEQUENCE))
			{
				runCommand = true;
				play(path);
			}
			else if(Static.compareBytes(read, SHOW_FRAME_SEQUENCE))
			{
				runCommand = true;
				setVideoVisible();
			}
			else if(Static.compareBytes(read, HIDE_FRAME_SEQUENCE))
			{
				runCommand = true;
				setVideoInvisible();
			}
			// Check if bytes read equal ADJUST MEDIA TIME LOCATION...
			else if(read[0] == 98)
			{
				runCommand = true;
				// Will skip current media time to whatever percentage was passed as second byte in read array.(MAX: 100%)
				skip(read[1]);
			}
			// Check if bytes read equal ADJUST VOLUME...
			else if(read[0] == 99)
			{
				runCommand = true;
				// Will adjust volume of media player by using the second byte in read array as percentage.(MAX: 100%)
				adjustVolume(read[1]);
			}
		}
		
		/**
		 * Skips time using a percentage.
		 * @param Integer percentage - Percentage of time you what to skip to.
		 */
		private static void skip(int percentage)
		{
			// If command is enabled to run and media player not null....
			if(runCommand && player != null)
				synchronized (player) {
					// Skip to location in media.
					player.skip(percentage);
				}
		}
		
		/**
		 * Enables/Disables fast forward.
		 */
		private static void fastForward()
		{
			// If command is enabled to run and media player not null....
			if(runCommand && player != null)
				synchronized (player) {
					// Enable/Disable fast forward.
					player.forward();
				}
		}
		
		/**
		 * Skips back five seconds in current media.
		 */
		private static void fastRewind()
		{
			// If command is enabled to run and media player not null....
			if(runCommand && player != null)
				synchronized (player) {
					// Skip back five seconds.
					player.rewind();
				}
		}
		
		/**
		 * Plays the received media file.
		 */
		private static void play()
		{
			// If command is enabled to run..
			if(runCommand)
			{
				// If media file not null....
				if(path != null)
					synchronized (player) {
						// Run media file.
						player.play(path.toCharArray(), index);
					}
			}
		}
		
		/**
		 * Plays media at the given link. Intended for YouTube.
		 * @param mrl - MRL of the file to play.
		 */
		private static void play(String mrl)
		{
			System.out.println("Starting mrl:"+mrl);
			if(player == null)
				
				player = new MediaPlayer();
			if(mrl != null)
				player.play(mrl);
		}
		
		/**
		 * Attempts to play/pause current media.
		 */
		private static void pausePlay()
		{
			// If command is enabled to run and media player not null....
			if(runCommand && player != null)
				try
				{
					synchronized (player) {
						// Pause/play media.
						player.pauseOrPlay();
					}
				}
				catch (Exception e)
				{
					// If can't play/pause, attempt to play last received media file.
					play();
				}
		}
		
		/**
		 * Attempts to stop current media.
		 */
		private static void stopPlaying()
		{
			// If command is enabled to run and media player not null....
			if(runCommand && player != null)
				synchronized (player) {
					// Stop currently playing media.
					player.stop();
				}
		}
		
		/**
		 * Adjusts volume via the passed parameter. It accepts it as a percentage with 100 being the max volume.
		 * @param Integer volume_level - Percentage of volume out of 100.
		 */
		private static void adjustVolume(int volume_level)
		{
			// If command is enabled to run and media player not null....
			if(runCommand && player != null)
				synchronized (player) {
					// Adjust volume.
					player.volume(volume_level);
				}
		}
		
		private static void setVideoInvisible()
		{
			if(runCommand && player != null)
				synchronized (player) {
					player.setFrameInvisible();
				}
		}
		
		private static void setVideoVisible()
		{
			if(runCommand && player != null)
				synchronized (player) {
					player.setFrameActive();
				}
		}
		
		/**
		 * Checks if the read byte array is equal to the PLAY_FILE_SEQUENCE, if so. This means that a media file wrapper object
		 * will be sent through the stream straight after the command. The file will be read and kept for use in running the command.
		 * @throws ClassNotFoundException
		 * @throws IOException
		 */
		private static void checkForFilePath() throws ClassNotFoundException, IOException
		{
			Logger.append(new StringBuffer("Checking for file path..."));
			// Check if read equals PLAY_FILE_SEQUENCE, if so...
			if(Static.compareBytes(read, PLAY_FILE_SEQUENCE))
			{
				// Read in MediaFile path from client.
				path = input.readUTF();
				index = input.readInt();
			}
		}
		
		/**
		 * Checks if the read byte array is equal to the PLAY_TUBE_SEQUENCE, if so. This means that a MRL string object
		 * will be sent through the stream straight after the command. The string will be read and kept for use in running the command.
		 * @throws IOException
		 */
		@SuppressWarnings("unused")
		private static void checkForMRL() throws IOException
		{
			Logger.append(new StringBuffer("Checking for MRL path..."));
			// Check if read equals PLAY_FILE_SEQUENCE, if so...
			if(Static.compareBytes(read, PLAY_TUBE_SEQUENCE))
			{
				// Read in string from client.
				path =  input.readUTF();
				index = -2;
			}
		}
		
		/**
		 * Initializes Object out/in(put) streams. Object output stream must be initialized first.
		 * @throws IOException - Will throw error if client connection is NULL/CLOSED/DISCONNECTED.
		 */
		private static void initialiseStreams()
		{
			try
			{
				output = new ObjectOutputStream(socket.getOutputStream());
				input = new ObjectInputStream(socket.getInputStream());
			}
			catch(IOException e)
			{
				e.printStackTrace();
				System.err.println();
				Logger.append(new StringBuffer("Disconnected or closed"));
				Logger.append(new StringBuffer(e.getMessage()));
			}
		}
	}
}
