package net.eureka.couchcast.player;

import java.awt.Canvas;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowEvent;
import java.awt.event.WindowFocusListener;
import java.awt.event.WindowListener;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.Properties;
import java.util.Timer;
import java.util.TimerTask;

import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JOptionPane;

import net.eureka.couchcast.foundation.init.ApplicationGlobals;
import net.eureka.couchcast.mediaserver.NetworkInfo;
import net.eureka.couchcast.player.control.ActivateWindow;
import net.eureka.couchcast.player.control.DesktopControl;
import uk.co.caprica.vlcj.binding.internal.libvlc_state_t;
import uk.co.caprica.vlcj.player.MediaPlayerEventAdapter;
import uk.co.caprica.vlcj.player.MediaPlayerFactory;
import uk.co.caprica.vlcj.player.embedded.EmbeddedMediaPlayer;
import uk.co.caprica.vlcj.player.embedded.videosurface.CanvasVideoSurface;
import uk.co.caprica.vlcj.player.embedded.windows.Win32FullScreenStrategy;

/**
 * *** BE AWARE THE SUMMARY DOC HERE IS VASTLY OUTDATED, THE NOTES ARE STILL VALID THOUGH. 
 * 
 * VLC implemented media player object. Instantiated when {@link MediaReceiver} server socket has binded. 
 * Handles the creation of the full-screen canvas, media player functions/controls and the updating of 
 * media player info through a TimerTask/{@link Timer} scheduled to run every one second.
 * Once a command comes through the {@link MediaReceiver} from the Android client, the {@link MediaReceiver} 
 * interprets what command it is then passes it to the MediaPlayer object for execution. Whatever changes
 * are reflected within the {@link NetworkInfo} object every one second update. That info object is then sent
 * back to the Android Client through the {@link MediaBroadcaster}.
 * <br>
 * <pre>
 * <h2>NOTES:</h2>
 * 
 * 		Static block (below), is used to find the required VLC install directory. The media player won't run
 * 		without this. CAUTION: Must be set up before the media player is constructed. 
 * 
 * 		LIB_OPTIONS, used to specify preferences to VLC.
 * 
 * 		--vout=direct2d, a video output module preference. This had to be set to direct2d from directX because
 * 		the directX module had conflicts with other programs that were running from the directX libraries.
 * 
 *		An internal media player timer had to be created to circumvent issues regarding the retrieval of the
 *		current time-stamp within certain media formats. (So far only found within some MKV files.) This bug
 *		would delay the retrieval of a time-stamp (for up to ten seconds at times); thus making real time 
 *		updating useless. The delay seems to build up over each time-stamp call, so maybe it's a work flow block. 
 *		This timer is updated every second from the TimerTask updateMediaInfo method. Whenever a VLCJ
 *		function is used that has to do with time manipulation, on that action the current media player time is
 *		refreshed with the current media time-stamp. From the refreshed time, the timer continues at that point
 *		provided no buffering is needed.
 *		(Time-stamp call = EmbeddedMediaPlayer.getTime())
 *
 *		To ensure that the media player frame/canvas is the focused window; A number of functions were created
 *		to minimize all other windows and activate the media player frame from the O.S level. Essentially, if
 *		the window is detected to have lost focus or never had it, the media player will attempt to minimize
 *		every other window. Then will select the media players frame in an attempt to bring it to the front.
 *		Works in almost every use case, some O.S functions may still take precedence though. 
 *		If this is not a preferable solution, it can simply be deactivated. 
 *		Window Handle Function objects: {@link ActivateWindow}, {@link DesktopControl}, {@link WindowSearcher}.
 *
 *
 *		Once the full-screen window has been 'initialized/focused upon', the default cursor is replaced with
 *		a blank image for cinematic purposes until out of full-screen again. 
 *
 *		For mouse input to work on the {@link Canvas}, setEnableMouseInputHandling and setEnableKeyInputHandling must both
 *		be set to false on the {@link EmbeddedMediaPlayer} object. This is done within the initialiseMediaPlayer method.
 * 
 * </pre> 
 * 
 * This object heavily uses VLCJ (A extension of libVLC for use with Java.) for accessing the available
 * libVLC bindings. For more information on VLCJ please visit their Github link https://github.com/caprica/vlcj 
 * 
 * 
 * @author Owen McMonagle
 * 
 * @see MediaReceiver
 * @see NetworkInfo
 * @see MediaBroadcaster
 * @see ActivateWindow
 * @see DesktopControl
 * @see WindowSearcher 
 *
 */
public final class MediaPlayer extends TimerTask
{
	/** STATIC BLOCK **
	 * Used to load the libVLC libraries at compilation of this class. Uses JNA in combination with VLCJ to
	 * locate the VLC install directory. MUST BE LOADED BEFORE MEDIA PLAYER CONSTRUCTION.
	 */
	static
	{	
		//StringBuffer path = new StringBuffer(WindowsRuntimeUtil.getVlcInstallDir()); 
		//Logger.append(path);
		//Show the API where VLC library is located.
		Properties props = System.getProperties();
		props.setProperty("jna.library.path", ApplicationGlobals.getInstallPath()+"lib");
		
		// Old way.
		//NativeLibrary.addSearchPath(RuntimeUtil.getLibVlcLibraryName(), WindowsRuntimeUtil.getVlcInstallDir());
		//NativeLibrary.addSearchPath(RuntimeUtil.getLibVlcLibraryName(), "C:\\Program Files (x86)\\Android Cast Media Server\\");
	}
	
			
	/**
	 * Combines with Program files string above to create a full path to the installed Tray
	 * icon.
	 */
	private static final String ICON_PATH = ApplicationGlobals.getInstallPath()+"logo.png";
	
	/**
	 * Library options for libVLC. Any preferences can be set here that are to do with base library functions. e.g
	 * Video/Audio output modules.
	 * 
	 * --vout=direct2d | Switches the video output module from directX to direct2d to prevent conflicts.
	 */
	private final String[] LIB_OPTIONS = new String[]{ "--vout=direct2d"};
	
	/**
	 * Invisible media player frame to contain our video canvas.
	 */
	private JFrame ourFrame = null;
	
	/**
	 * VLCJ media player factory, used to create an appropriate media player object from the passed library options.
	 */
	// mediaPlayerFactory.getMediaMeta("", true) <--- Can be used to create more in depth media descriptions.
	private MediaPlayerFactory mediaPlayerFactory = null;
	
	/**
	 * The VLCJ libVLC media player object that is created from the {@link MediaPlayerFactory}. 
	 */
	private EmbeddedMediaPlayer ourMediaPlayer = null;
	
	/**
	 * VLCJ video wrapper canvas. Used to attach a video surface using the {@link MediaPlayerFactory} and a {@link Canvas}.
	 */
	private CanvasVideoSurface videoSurface = null;
	
	/**
	 * VLCJ event listener, handles media player events/reactions such as when to change {@link NetworkInfo} in response to video
	 * buffering for example.
	 */
	private MediaPlayerEventAdapter eventListener = new MediaPlayerEventAdapter()
    {	
		
		/**
		 * Whenever the media is changed, the media player info is set to pause by default. This is because of presumed buffering.
		 */
		public final void mediaChanged(uk.co.caprica.vlcj.player.MediaPlayer mediaPlayer, uk.co.caprica.vlcj.binding.internal.libvlc_media_t media, String mrl) 
		{
			// Set media info paused.
			setPaused();
		};
		
		/**
		 * When the media is buffering, this method is called several times with the new percentage buffered. Sets
		 * pause if percentage is less than 100%, sets playing otherwise.
		 * 
		 *  @param mediaPlayer - Media player object that is buffering the current media.
		 *  @param new_cache_percentage_completed - Percentage as float, of the current buffering completed. 
		 */
		public final void buffering(uk.co.caprica.vlcj.player.MediaPlayer mediaPlayer, float new_cache_percentage_completed) 
		{
			//System.out.println("Buffering:"+ new_cache_percentage_completed);
			// If the new percentage buffered is less than 100%...
        	if(new_cache_percentage_completed < 100.0f)
        		// Set media info paused.
				setPaused();
        	// If media player is playing ....
			else if(ourMediaPlayer.isPlaying())
				// Set media info playing.
				setPlaying();
		};
        
		/**
		 * When the medias state changes, this method is called with the new player state. This state is then sent
		 * to the statusDelegate method, where the state will be interpreted and the corresponding {@link NetworkInfo}
		 * change will be made.
		 * 
		 *  <pre>
		 *  	RECORDED STATES:
		 *  
		 *  	PAUSED
		 *  	PLAYING
		 *  	ENDED
		 *  </pre>
		 * 
		 *  @param mediaPlayer - Current Media player object which state has changed. 
		 *  @param Integer new_player_state - New state of the media player. 
		 */
        public final void mediaStateChanged(uk.co.caprica.vlcj.player.MediaPlayer mediaPlayer, int new_player_state)
        {
        	// Delegate state for interpretation.
			this.statusDelegate(new_player_state);
        };
        
        /**
         * Whenever the seek option is used to seek a new media time, the {@link NetworkInfo} will pause until the presumed
         * buffering is completed.
         */
        public final void seekableChanged(uk.co.caprica.vlcj.player.MediaPlayer mediaPlayer, int newSeekable) 
        {
        	// Set media info to pause state.
        	setPaused();
        };
        
        /**
         * Sets the new {@link NetworkInfo} state from the passed state integer. State changes come from mediaStateChanged.
         *  <pre>
		 *  	RECORDED STATES:
		 *  
		 *  	PAUSED
		 *  	PLAYING
		 *  	ENDED
		 *  </pre>
         * @param Integer new_media_state - The new state of the media player.
         */
        private final void statusDelegate(int new_media_state)
        {
        	//System.out.println(streaming_preparing);
        	// If the new media player state is paused...
        	if(new_media_state == libvlc_state_t.libvlc_Paused.intValue())
        		// Set media info paused.
        		setPaused();
        	// If the new media player state is playing...
			else if(new_media_state == libvlc_state_t.libvlc_Playing.intValue())
				// Set media info playing.
				setPlaying();
        	// If the new media player state is ended...
			else if(new_media_state == libvlc_state_t.libvlc_Ended.intValue() && !streaming_preparing)
			{
				System.out.println("ENDED.");
				// Stop and hide media player
				stop();
				// Set media info paused.
	        	setPaused();
			}
        }
        
        public void mediaSubItemAdded(uk.co.caprica.vlcj.player.MediaPlayer mediaPlayer, uk.co.caprica.vlcj.binding.internal.libvlc_media_t subItem) 
        {
        	System.out.println("Sub item added: "+mediaPlayer.mrl(subItem));
        };
        
        public void finished(uk.co.caprica.vlcj.player.MediaPlayer mediaPlayer) 
        {
        	System.out.println("At finished. :"+streaming);
        	this.checkForStreaming();
        };
        
        private void checkForStreaming()
        {
        	if(streaming)
        	{
    			System.out.println("Inside finished.");
        		List<String> subItems = ourMediaPlayer.subItems();
        		if(subItems != null && !subItems.isEmpty())
        		{
        			ourMediaPlayer.startMedia(subItems.get(0));
        			// Check for minimize option.
    				checkMinimizeFrames();
    				// Check if frame is visible & to front.
    				checkFrameVisible();
    				streaming_preparing = false;
    				wasLastMediaStreaming = true;
        		}	 
        	}
        }
        
        public void error(uk.co.caprica.vlcj.player.MediaPlayer mediaPlayer) 
        {
        	System.out.println("Detected error.");
        };
        
    };
    
    /**
     * Listens for window events. The only events listened for are window closed, which will stop
     * the media player and window activated which will request input focus from the O.S.
     */
    private final WindowListener windowListener = new WindowListener()
	{
    	/**
    	 * Listens for when the window is closed. Stops the media player is running.
    	 */
    	public void windowClosed(WindowEvent e)
		{
    		// Stop and hide media player.
			stop();
		}
    	
    	/**
    	 * Listens for when the window is activated by the O.S/User. Requests input focus.
    	 */
    	public void windowActivated(WindowEvent e)
		{
    		// Request input focus for our window frame.
			ourFrame.requestFocus();
		}
		
    	// UNUSED
		public void windowOpened(WindowEvent e){}
		public void windowIconified(WindowEvent e){}
		public void windowDeiconified(WindowEvent e){}
		public void windowDeactivated(WindowEvent e){}
		public void windowClosing(WindowEvent e){}
	};
	
	/**
	 * Listens for mouse input events. If the media player is full-screen and is playing when the mouse
	 * is clicked, then the window will be minimized and the media player paused. 
	 */
	private final MouseAdapter mouseListener = new MouseAdapter()
	{
		public void mouseClicked(MouseEvent e) 
		{
			// If the mouse click is left or right, media player is not null and is playing while in full-screen...
			if((e.getButton() == MouseEvent.BUTTON1 || e.getButton() == MouseEvent.BUTTON3) && (ourMediaPlayer != null && ourMediaPlayer.isFullScreen() && ourMediaPlayer.isPlaying()))
				// Pause the media player.
				ourMediaPlayer.pause();
			else if(e.getButton() == MouseEvent.BUTTON3 && ourMediaPlayer != null && ourMediaPlayer.isFullScreen() && !ourMediaPlayer.isPlaying())
				// Play the media player.
				ourMediaPlayer.play();;
				
			if(e.getButton() == MouseEvent.BUTTON1 || e.getButton() == MouseEvent.BUTTON2)
				// Minimize player window.
				ourFrame.setExtendedState(JFrame.ICONIFIED);
		};
	};
	
	/**
	 * Listens for window focus events. <br>
	 * <h2>Losing focus:</h2> 
	 * Once the window has lost focus by either the user navigating away from the player or an unforeseen pop 
	 * up stealing focus, then the 'focused' flag will indicate that focus has been lost and the next time the 
	 * play command is sent the window should be reactivated. This is done by minimizing all other windows and
	 * trying to reactivate the media player window.  {@link DesktopControl}, {@link WindowSearcher}, {@link ActivateWindow}. 
	 *
	 * <h2>Gaining focus:</h2>
	 * Once the window focus has been gained 
	 */
	private final  WindowFocusListener focusListener = new WindowFocusListener()
	{
		
		/**
		 * Once the window has lost focus by either the user navigating away from the player or an unforeseen pop 
	 	 * up stealing focus, then the 'focused' flag will indicate that focus has been lost and the next time the 
	 	 * play command is sent the window should be reactivated. This is done by minimizing all other windows and
	 	 * trying to reactivate the media player window.  {@link DesktopControl}, {@link WindowSearcher}, {@link ActivateWindow}.
		 */
		public void windowLostFocus(WindowEvent e) 
		{
			focused = false;
		}
		
		public void windowGainedFocus(WindowEvent e) 
		{
			focused = true;
		}
	};
	
	/**
	 * {@link NetworkInfo} object. Contains all the relevant media info that needs to be transported to the Android 
	 * client via the {@link MediaBroadcaster}. 
	 */
	private static NetworkInfo info = new NetworkInfo(0, 0);
	
	/**
	 * {@link Timer} that manages this media players TimerTask. Used to update media info every one second.
	 */
	private Timer timer = null;
	
	/**
	 * Last link streamed from YouTube.
	 */
	private String lastStreamingLink = "";
	
	/**
	 * Current time elapsed by the currently playing media.
	 */
	private long time = 0L;
	
	/**
	 * Media player pause flag. Indicates whether the media is paused or not.
	 */
	private boolean paused = false;
	
	/**
	 * Media preparation flag. Used to indicate whether the media player is still loading the file it needs to play.
	 */
	private boolean preparing = false;
	
	/**
	 * Media file end flag. Used to indicate whether the media file has ended.
	 */
	private boolean finished = false;
	
	/**
	 * Media player window flag. Used to indicate whether the video window has focus or not.
	 */
	private boolean focused = false;
	
	/**
	 * Media player streaming flag. Used to indicate streaming video/audio.
	 */
	private boolean streaming = false;
	
	/**
	 * Media player streaming preparation flag. Used to stop the window from prematurely ending before streaming
	 * starts.
	 */
	private boolean streaming_preparing = false;
	
	private boolean wasLastMediaStreaming = false;
	
	private int mediaIndex = -1;
	
	private String mediaTitle = "";
	
	/**
	 * Background image for the video frame if no video is playing.
	 */
	private Image overlayImage = null;
	
	/**
	 * Only needs constructed once. Starts by creating the invisible {@link JFrame} that will contain our video canvas.
	 * Then creates the canvas that will be our video layer and adds it to the {@link JFrame}. After our {@link JFrame} and 
	 * {@link Canvas} are ready the {@link EmbeddedMediaPlayer} is created and ready to be used. Finally a timer is created
	 * and set to update the media info every one second.
	 */
	public MediaPlayer()
	{
		// Create the JFrame.
		initialiseFrame();
		// Create the video canvas.
		initialiseFullscreen();
		// Create the media player object.
		initialiseMediaPlayer();
		// Instantiate timer.
		timer = new Timer();
		// Schedule TimerTask to be updated every one second.
		timer.scheduleAtFixedRate(this, 1000, 1000);
	}
	
	/**
	 * Creates the {@link JFrame} that will contain the video {@link Canvas}. The frame is set to undecorated so it will be
	 * invisible. An Icon is set to represent the media player. The frame component is set to visible. A {@link WindowFocusListener}
	 * and a {@link WindowListener} are both attached. Finally the cursor is replaced to an invisible one for the {@link JFrame}s content
	 * pane and the default window close operation is set to HIDE_ON_CLOSE so the whole application won't close.  
	 */
	private void initialiseFrame()
	{
		// If the window does not already exist...
		if(ourFrame == null)
		{
			// Import overlay image.
			this.importOverlayLogo();
			// Create the window.
			ourFrame = new JFrame();
			// Set the window to hide on close. 
			ourFrame.setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
			// Set the window frame to invisible.
			ourFrame.setUndecorated(true);
			// Set the window icon.
			ourFrame.setIconImage(new ImageIcon(ICON_PATH).getImage());
			// Set the window component visible.
			ourFrame.setVisible(true);
			// Add a window focus listener to monitor window focus changes.
			ourFrame.addWindowFocusListener(focusListener);
			// Add a window listener to monitor window events.
			ourFrame.addWindowListener(windowListener);
			// Replace the window cursor with an invisible one.
			this.replaceCursor();
			
			ourFrame.setVisible(false);
		}
	}
	
	/**
	 * Imports overlay image to display on the Frame when there is no video playing.
	 */
	private void importOverlayLogo()
	{
		
		try
		{
			
			String  image_name = "window_logo.png",
					image_location = ApplicationGlobals.getInstallPath() + image_name;
			
			overlayImage = new ImageIcon(image_location).getImage();
			//if(overlayImage == null)
				//importBackupOverlayLogo();
		}
		catch (NullPointerException e)
		{
			
		} 
		
	}
	
	/**
	 * Replaces the {@link JFrame}s cursor with a blank one.
	 */
	private void replaceCursor()
	{
		// Transparent 16 x 16 pixel cursor image.
		BufferedImage cursorImg = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);

		// Create a new blank cursor.
		Cursor blankCursor = Toolkit.getDefaultToolkit().createCustomCursor(cursorImg, new Point(0, 0), "blank cursor");

		// Set the blank cursor to the JFrame.
		ourFrame.getContentPane().setCursor(blankCursor);
	}
	
	/**
	 * Creates the video canvas from the {@link MediaPlayerFactory} using a AWT {@link Canvas} component.
	 * Then adds the {@link Canvas} to the {@link JFrame}.
	 */
	private void initialiseFullscreen()
	{	
		mediaPlayerFactory = new MediaPlayerFactory(LIB_OPTIONS);
		// Create the AWT Canvas.
		Canvas canvas = new Canvas()
		{
			private static final long serialVersionUID = 2414466573758080014L;
			private static final String LOGO_NAME = "logo_text_huge.png";
			private final Image LOGO_IMAGE = new ImageIcon(ApplicationGlobals.getInstallPath() + LOGO_NAME).getImage();
			
			@Override
			public void paint(Graphics g) 
			{
				try
				{
					g.setColor(Color.black);
					g.fillRect(0, 0, this.getWidth(), this.getHeight());
					g.drawImage(overlayImage, 0, (this.getHeight()-585), null);
					g.drawImage(LOGO_IMAGE, (this.getWidth()/2 - 350), (this.getHeight()/2-37), null);
				}
				catch(Exception e)
				{
					e.printStackTrace();
				}
			}
		};
		// Add a mouse listener for input.
		canvas.addMouseListener(mouseListener);
		// Create a video surface for our canvas.
		videoSurface = mediaPlayerFactory.newVideoSurface(canvas);
		// Add the Canvas to the JFrame.
		ourFrame.add(canvas);
	}
	
	/**
	 * Creates the {@link EmbeddedMediaPlayer} that is used to play/control media files from LibVLC. Starts by creating the {@link EmbeddedMediaPlayer}
	 * object from the {@link MediaPlayerFactory} and implements a {@link Win32FullScreenStrategy} which can only be used on a Windows O.S.
	 * Then a {@link MediaPlayerEventAdapter} is attached to listen for media events such as buffering/play/pause/skip. For mouse input 
	 * to work on the {@link Canvas}, setEnableMouseInputHandling and setEnableKeyInputHandling must both  be set to false on the 
	 * {@link EmbeddedMediaPlayer} object. Finally the video surface (Which is on the {@link Canvas}) is attached to the {@link EmbeddedMediaPlayer}
	 * and full-screen is enabled. 
	 */
	private void initialiseMediaPlayer()
	{
		// Create the media player object and full-screen strategy attached to the JFrame. 
		ourMediaPlayer = mediaPlayerFactory.newEmbeddedMediaPlayer(new Win32FullScreenStrategy(ourFrame));
		// Add media player event adapter to listen for media events.
        ourMediaPlayer.addMediaPlayerEventListener(eventListener);
        // Must be set to false for mouse input on the Canvas to work.
        ourMediaPlayer.setEnableMouseInputHandling(false);
        // Must be set to false for mouse input on the Canvas to work.
        ourMediaPlayer.setEnableKeyInputHandling(false);
        // Attach the video surface to the media player.
        ourMediaPlayer.setVideoSurface(videoSurface);
       	// Set full-screen enabled.
       	ourMediaPlayer.setFullScreen(true);
	}
	
	private void checkMediaPlayerCreated()
	{
		if(ourMediaPlayer == null)
			initialiseMediaPlayer();
	}
	
	
	/**
	 * Used to play a {@link MediaFile} on LibVLC. File does not play until it is fully prepared.
	 * @param {@link MediaFile} file_to_play - Media file that is to be played.
	 */
	public void play(char[] path, int index)
	{
		//Native.getDirectBufferPointer()
		checkMediaPlayerCreated();
		// If a media file is not already being prepared...
		if(!preparing)
		{
			mediaIndex = index;
			wasLastMediaStreaming = false;
			// Start preparing media.
			preparing = true;
			// Indicate not streaming.
			streaming = false;
			try
			{ 
				// Store file path as string.
				final String path_string = new String(path);
				// Store name retrieved from path.
				mediaTitle = path_string.substring(path_string.lastIndexOf("\\")+1);
				// Set title of the JFrame to the media title.
				ourFrame.setTitle(mediaTitle);
				this.volume(25);
				// Wait until the media file is prepared to play...
				ourMediaPlayer.startMedia(path_string);
				// Reset media player time.
				time = 0;
				// Reset finished state.
				finished = false;
				if(!ApplicationGlobals.isMusicMode())
				{
					// Check for minimize option.
					this.checkMinimizeFrames();
					// Check if frame is visible & to front.
					this.checkFrameVisible();
				}
				else
					this.setFrameInvisible();
			}
			catch(Exception e)
			{
				e.printStackTrace();
				JOptionPane.showMessageDialog(null, e.toString());
			}
			finally
			{
				// Finished preparing.
				preparing = false;
			}
		}
	}
	
	public void play(String mrl)
	{
		checkMediaPlayerCreated();
		// If a media file is not already being prepared...
		System.out.println("Starting mrl:"+mrl);
		if(!preparing)
		{
			lastStreamingLink = mrl;
			ourFrame.setTitle(lastStreamingLink);
			// Start preparing media.
			preparing = true;
			streaming_preparing = true;
			try
			{
				time = 0;
				// Reset finished state.
				finished = false;
				// Start media streaming.
				streaming = true;
				System.out.println("Starting mrl:"+mrl);
				this.volume(25);
				// Play media file.
				ourMediaPlayer.startMedia(mrl);
				// Check for minimize option.
				this.checkMinimizeFrames();
				// Check if frame is visible & to front.
				this.checkFrameVisible();
			}
			catch(Exception e)
			{
				e.printStackTrace();
				JOptionPane.showMessageDialog(null, e.toString());
			}
			finally
			{
				// Finished preparing.
				preparing = false;
			}
		}
	}
	
	/**
	 * Updates media info every one second. First it updates the media time, then updates the {@link NetworkInfo} object.
	 * Which is sent to Android client by the {@link MediaBroadcaster}.
	 */
	public void updateMediaInfo()
	{
		if(ourMediaPlayer != null && ourMediaPlayer.isPlayable())
		{
			// Update server time.
			handleTime();
			// Update media info object.
			handleInfo();
		}
	}
	
	/**
	 * Handles the updating of the {@link NetworkInfo} object. This object is sent to the Android Clients player as a 
	 * representation of the media players state. The retrieval of the object is at the clients discretion via {@link MediaBroadcaster}.
	 * At the moment the client retrieves it every 850ms, leaving 150ms to update the UI. 
	 */
	private void handleInfo()
	{
		try
		{
			synchronized (info) 
			{
				// If the media info object is equal to null...
				if(info == null)
					// Create a new media info object with initial length, current length and name.
					info = new NetworkInfo(ourMediaPlayer.getMediaMeta().getLength(), time);
				// If the media info object is already created...
				else
				{
					//System.out.println("Updating time...");
					// Update length of the media.
					info.setLength(ourMediaPlayer.getMediaMeta().getLength());
					// Update current length of the media.
					info.setTime(time);
				}
				short index;
				if(!streaming)
				{
					// Check if the media player is finished. If so reset the index to -1 (Signalling nothing is playing).
					// If not update the media index.
					index = (short) ((finished) ? -1 : mediaIndex);
				}
				else
					// I believe this was for the experimental YouTube streaming state.
					index = -2;
					
				// Set media index.
				info.setIndex(index);
				// Update media fast forward.
				info.setForward(isFastFoward());
				// Update media playing.
				info.setPlaying(isPlaying());
				// Update current volume.
				info.setVolume((byte)(ourMediaPlayer.getVolume()/2));
				if(ApplicationGlobals.isMusicMode())
					info.setMusic(true);
				else
					info.setMusic(false);
				
			}
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
	}
	
	/**
	 * Handles the updating of time while the media player is playing. Updates it every second.
	 * If the media player function fast forward is enabled, then the time-stamp is taken instead
	 * because the rate of length is unpredictable with formats that don't support fast forward as 
	 * well. 
	 */
	private void handleTime()
	{
		// If the media player is not paused and not finished... 
		if(!paused && !finished)
		{
			// if the media player is not in fast forward...
			if(!isFastFoward())
				// progress time normally every one second.
				time += 1000;
			// If the media player is in fast forward...
			else
				// retrieve the time-stamp from the media player.
				time = this.ourMediaPlayer.getTime();
			
			
			synchronized (info)
			{
				if(streaming && info != null && !preparing && !streaming_preparing && (time >= info.getLength()))
					this.stop();
			}
				
		}
	}
	
	/**
	 * Controls the pause and play actions of the {@link EmbeddedMediaPlayer}. If the media player
	 * is paused then calling this will play the media and vice versa. Also checks if fast forward
	 * is activated and if so disables it.
	 *  
	 */
	public void pauseOrPlay()
	{
		// Check if media player is fast forward enabled.
		this.checkFastForward();
		// If the media player is playing...
		if(!paused)
		{
			// Pause the media player.
			ourMediaPlayer.pause();
			// Set the pause flag.
			paused = true;
		}
		// If the media player is pause...
		else
			// Prepare frame for playing.
			initialiseFrameForPlaying();
	}
	
	/**
	 * Checks if the media player is in fast forward mode and resets it.
	 */
	private void checkFastForward()
	{
		// If the media player rate is > 1....
		if(isFastFoward())
			// Reset the media player rate.
			ourMediaPlayer.setRate(1);
	}
	
	/**
	 * Handles the {@link JFrame} preparation before playing media. First it checks if the media is finished and
	 * if so, resets it. Then verifies if the frame is visible and if not, enables it. The frame is then checked if
	 * it has focus, if not the all other programs on the desktop are minimized by using {@link DesktopControl} to
	 * ensure focus. Finally the window is activated via {@link ActivateWindow} and the media is played.
	 */
	private void initialiseFrameForPlaying()
	{
		// Check if the media has just finished and if so reset it.
		this.checkFinished();
		this.checkMinimizeFrames();
		if(!ApplicationGlobals.isMusicMode())
			// Verify the frame has focus and activate the window.
			this.setFrameActive();
		if(!wasLastMediaStreaming)
			// Play the currently selected media.
			ourMediaPlayer.play();
		else
			this.play(lastStreamingLink);
	}
	
	private void checkMinimizeFrames()
	{
		// If the frame is not focused or the media player is preparing a file...
		if(ApplicationGlobals.isMinimizeWindows() && (!focused || preparing))
		{
			// Minimize all desktop windows.
			DesktopControl.minimizeAll();
			// Wait 250ms.
			this.waitForABit();
		}
	}
	
	public void setFrameActive()
	{
		this.checkFrameVisible();
		String title_of_window = !streaming ? mediaTitle : lastStreamingLink;
		//System.out.println(title_of_window);
		// Set the new activated window with the file name.
		ActivateWindow.setFocusedWindow(title_of_window);
	}
	
	/**
	 * Checks if the {@link JFrame} is not visible and if so enables it.
	 */
	public void checkFrameVisible()
	{
		// If the frame is not visible...
		if(!ourFrame.isVisible() && !finished)
			// Enable frames visibility.
			ourFrame.setVisible(true);
	}
	
	public void setFrameInvisible()
	{
		if(ourFrame.isVisible())
			ourFrame.setVisible(false);
	}
	
	/**
	 * Checks if the current media is finished. If so the {@link EmbeddedMediaPlayer} is
	 * stopped and the finished flag is reset.
	 */
	private void checkFinished()
	{
		// If the media player is finished...
		if(this.finished)
		{
			// Stop the media player.
			ourMediaPlayer.stop();
			// Reset finished flag.
			this.finished = false;
		}
	}
	
	/**
	 * Stops the current media from continuing playing, closes the {@link JFrame} and resets
	 * the media player variables such as time, finished flag and {@link NetworkInfo}.
	 */
	public void stop()
	{
		System.out.println("Stopped!");
		if(ourMediaPlayer != null)
		{
			// Close the JFrame.
			this.closeVideoView();
			// Check if paused and pause it.
			this.checkPaused();
			// Reset media player variables.
			this.signalMediaReset();
		}
	}
	
	/**
	 * Sets the {@link JFrame} containing the video {@link Canvas} to invisible. 
	 */
	private void closeVideoView()
	{
		// If the frame is not null and is visible
		if(ourFrame != null && ourFrame.isVisible())
			// Set invisible.
			ourFrame.setVisible(false);
	}
	
	/**
	 * Checks if the media player is not paused and if so pauses it. 
	 */
	private void checkPaused()
	{
		// If the media player is not paused....
		if(!paused)
			// Pause media player.
			ourMediaPlayer.pause();
	}
	
	/**
	 * Resets media player time, sets finished flag to true and {@link NetworkInfo}. Also signals
	 * Garbage Collection to make a pass(Not guaranteed).
	 */
	private void signalMediaReset()
	{
		// Reset time.
		time = 0;
		// If player is in fast forward...
		if(isFastFoward())
			// Reset player rate.
			ourMediaPlayer.setRate(1f);
		// Set media finished.
		finished = true;
		// Set streaming finished.
		streaming = false;
		streaming_preparing = false;
		// Nullify current media info.
		synchronized (info) 
		{
			info = null;
		}
		// Reset VLCJ resources.
		ourMediaPlayer.removeMediaPlayerEventListener(eventListener);
		ourMediaPlayer.setVideoSurface(null);
		ourMediaPlayer.getMediaMeta().release();
		ourMediaPlayer.stop();
		ourMediaPlayer.release();
		mediaPlayerFactory.release();
		ourMediaPlayer = null;
		// Exit the OOP Player.
		System.exit(0);
	}
	
	/**
	 * Enables/Disables fast forward. 
	 */
	public void forward()
	{
		// If the media player is not paused...
		if(!paused && ourMediaPlayer != null)
		{
			// If not fast forward enabled...
			if(ourMediaPlayer.getRate() == 1f)
				// Enable fast forward.
				ourMediaPlayer.setRate(2);
			// If fast forward enabled...
			else if(ourMediaPlayer.getRate() == 2f)
				// Disable fast forward.
				ourMediaPlayer.setRate(1);
		}
	}
	
	/**
	 * Rewinds current media by five seconds and re-syncs time.
	 */
	public void rewind()
	{
		if(ourMediaPlayer != null)
		{
			// Rewind time five seconds.
			ourMediaPlayer.skip(-5000);
			// time = time - 5000;
			// Re-sync time. 
			time = this.ourMediaPlayer.getTime();
		}
	}
	
	/**
	 * Skips to a certain percentage of the current playing media.
	 *  
	 * @param Integer percentage_to_skip - Percentage of the time to skip to. (MAX:100%)
	 */
	public void skip(int percentage_to_skip)
	{
		// If the media player is playable...
		if(ourMediaPlayer != null && ourMediaPlayer.isPlayable())
		{
			// Translate percentage into milliseconds.
			long percentage_position = getPositionFromPercentage(percentage_to_skip);
			// Set new time-stamp.
			ourMediaPlayer.setTime(percentage_position);
		}
	}
	
	/**
	 * Sets the media player status to paused and updates the {@link NetworkInfo}.
	 */
	private void setPaused()
	{
		// Set status to paused.
		paused = true;
		synchronized (info) 
		{
			// Update media info.
			info.setPlaying(paused);
		}
	}
	
	/**
	 * Sets the media player status to playing and updates the {@link NetworkInfo}.
	 */
	private void setPlaying()
	{
		// Set status to playing.
		paused = false;
		synchronized (info) 
		{
			// Update media info.
			info.setPlaying(paused);
		}
	}
	
	/**
	 * Adjusts the media player volume. 
	 * 
	 * @param Integer volume_level - Volume up to 100%. (200 on VLC so it's x2)
	 */
	public void volume(int volume_level)
	{
		// If the media player is playable...
		if(ourMediaPlayer != null && ourMediaPlayer.isPlayable())
			// Set the current volume of the media player.
			ourMediaPlayer.setVolume(volume_level*2);
	}
	
	/**
	 * Retrieves if the media player is playing.
	 * @return Boolean - True if playing, false otherwise.
	 */
	public boolean isPlaying()
	{
		return !paused;
	}
	
	/**
	 * Retrieves if the media player is in fast forward.
	 * @return Boolean - True if fast forward, false otherwise.
	 */
	private boolean isFastFoward()
	{
		return (ourMediaPlayer != null) && ourMediaPlayer.getRate() > 1f;
	}
	
	/**
	 * Retrieves a time-stamp position using the percentage of the seeker bar(Android client) position.
	 * @param Integer current_percentage - Current position of the seeker bar in relation to the whole time frame of the media.
	 * @return Long - Time in milliseconds.
	 */
    private final long getPositionFromPercentage(int current_percentage)
    {
    	// Retrieve time in milliseconds by multiplying the current percentage with the total length and dividing by 100.
    	long time_from_percentage = (current_percentage * ourMediaPlayer.getMediaMeta().getLength())/100;
    	// Sync time status.
    	time = time_from_percentage;
    	// Return translated time.
        return time_from_percentage;
    }
    
    /**
     * TimerTask method, runs every one second to update media info.
     */
    @Override
    public void run() 
    {
    	Thread.currentThread().setName("Media Player");
    	//System.out.println("Updating");
		updateMediaInfo();
    }
    
    /**
     * Retrieves the media info.
     * Used by the {@link MediaBroadcaster}.
     * @return {@link NetworkInfo} - Media info representing the status of the media player.
     */
    public static NetworkInfo getMediaInfo()
    {
		synchronized (info) 
		{
			return info;
		}
    }
    
    /**
     * Instructs the thread to wait for 250ms.
     */
    private synchronized void waitForABit()
    {
    	try
    	{
    		this.wait(250);
    	}
    	catch (InterruptedException e)
    	{
    		e.printStackTrace();
    	}
    }
}
