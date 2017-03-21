package net.eureka.couchcast.mediaserver;

import java.io.Serializable;

/**
 * A class that is continuously updated from the MediaPlayer object. Each of these variables are used to indicate the status of Media Player and it's
 * currently selected media.
 * </br>
 * Used by MediaPlayer object for updating.
 * </br>
 * Used by MediaBroadcaster object for sending info about the player to client.
 * 
 * @author Owen McMonagle
 * 
 * @see MediaBroadcaster
 * @see MediaPlayer
 */
public final class NetworkInfo implements Serializable
{
	/**
	 * 
	 */
	private static final long serialVersionUID = 4256627274697569094L;		
	
	
	/**
	 *	Boolean used to determine the play/pause status.
	 */
	private boolean playing = false; 
			
	/**
	 * Boolean used to determine the fast forward status. 
	 */
	private boolean forward = false;
	
	private boolean music = false;
			
	/**
	 * Byte used to determine the volume level, out of 100.
	 */
	private byte volume = 0; 
			
	/**
	 * Long used to determine the total length of the current media in milliseconds.
	 */
	private long length = 0L; 
			
	/**
	 * Long used to determine the current time of the selected media in milliseconds.
	 */
	private long time = 0L; 
	
			
	/**
	 * Integer, used to determine which media file is being played on the play-list with respect
	 * to both lists on the client/server. e.g If play-list file index 67 is playing on the server, then
	 * on the clients play-list the 67th file down must be highlighted as playing. Also used to determine
	 * the name of the media currently playing on the client side. 
	 */
	private int index = 0;
	
	/**
	 * Only constructor, requires initial media length and current time. 
	 * @param long media_length - Total length/time of media.
	 * @param long media_time - current time of media.
	 */
	public NetworkInfo(final long media_length, final long media_time)
	{
		// Set total length.
		this.setLength(media_length);
		// Set current length.
		this.setTime(media_time);
	}
	
	/**
	 * Retrieves total length(In milliseconds).
	 * @return Long - Total length of current media.
	 */
	public long getLength()
	{
		return length;
	}

	/**
	 * Sets total length(In milliseconds) of current media. 
	 * @param Long length - Total length of media in milliseconds. 
	 */
	public void setLength(long length)
	{
		this.length = length;
	}

	/**
	 * Retrieves current length(In milliseconds).
	 * @return Long - Current length of playing media.
	 */
	public long getTime()
	{
		return time;
	}

	/**
	 * Sets current length(In milliseconds) of current media.
	 * @param time - Current length of media in milliseconds. 
	 */
	public void setTime(long time) 
	{
		this.time = time;
	}

	/**
	 * Used to determine current play status of media.
	 * @return Long - Boolean used to determine the play/pause status.
	 */
	public boolean isPlaying() 
	{
		return playing;
	}

	/**
	 * Determines play/pause status of media.
	 * @param playing - Boolean used to determine the /play/pause status
	 */
	public void setPlaying(boolean playing) 
	{
		this.playing = playing;
	}

	/**
	 * Determines fast forward status of media.
	 * @return Boolean - Boolean used to determine the fast forward status. 
	 */
	public boolean isForward() 
	{
		return forward;
	}

	/**
	 * Determines fast forward status of media.
	 * @param boolean forward - Boolean used to determine the fast forward status of media.
	 */
	public void setForward(boolean forward) 
	{
		this.forward = forward;
	}

	/**
	 * Byte used to determine volume level. (MAX: 100%)
	 * @return Byte - byte used for volume level.
	 */
	public byte getVolume() 
	{
		return volume;
	}

	/**
	 * Must be byte integer. Used to determine volume level. (MAX: 100%)
	 * @param Byte volume - byte integer, used for volume control.
	 */
	public void setVolume(byte volume)
	{
		this.volume = volume;
	}

	/**
	 * Short used to determine the index of the playing media file on each play-list.
	 * This is useful for highlighting the selected playing media on the client side as
	 * well as retrieving the media name from within the play-list.
	 * @return Short - Short used for media highlighting.
	 */
	public int getIndex() 
	{
		return index;
	}

	
	/**
	 * Short used to determine the index of the playing media file on each play-list.
	 * This is useful for highlighting the selected playing media on the client side.
	 * @param Short index - Short used for media highlighting.
	 */
	public void setIndex(int index) 
	{
		this.index = index;
	}

	public boolean isMusic() {
		return music;
	}

	public void setMusic(boolean music) {
		this.music = music;
	}
}
