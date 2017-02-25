package net.eureka.androidcast.player;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.ByteOrder;
import java.util.Enumeration;

/**
 * Contains useful methods that could be common to the whole media server.
 * 
 * @author Owen McMonagle
 *
 */
public abstract class Static 
{
	
	/**
	 * Imperial Megabyte unit of measurement.
	 */
	private static final int IMPERIAL_MB_UNIT = 1024000;
			
	/**
	 * Imperial Gigabyte unit of measurement.
	 */
	private static final int IMPERIAL_GB_UNIT = 1073741824;
	
	/**
	 * Helps in retrieval of the last four digits in the MAC address. Extra one digit is for the
	 * included dash. e.g "##-##"   
	 */
	private static final int MAC_OFFSET = 5;
	
	/**
	 * Identifier for signaling the type of address wanted from the network interface. In this case
	 * the I.PV4 assigned to the current DHCP activated network interface. This is used in the method below 
	 * called getAddress(String address_type). 
	 */
	private static final String IP_ADDRESS_IDENTIFIER = new String("ip");
			
	/**
	 * Identifier for signaling the type of address wanted from the network interface. In this case
	 * the MAC address assigned to the current DHCP activated network interface. This is used in the method below 
	 * called getAddress(String address_type). 
	 */
	private static final String MAC_ADDRESS_IDENTIFIER = new String("mac"); 
	
	/**
	 * Used as a delimiter to retrieve the last four digits of the MAC address; So it can be assigned to the default
	 * server name.
	 */
	private static final String ADDRESS_DELIMITER = new String("-");
	
	/**
	 * Used to compare two byte sequences against each other that each have the length of 2.
	 * 
	 * @param byte[] data - First two bytes.  
	 * @param byte[] against - Second two bytes.
	 * @return Boolean - True if each sequence matches, false otherwise.
	 */
	public static boolean compareBytes(final byte[] data, final byte[] against)
	{
		return(data[0] == against[0] && data[1] == against[1]);
	}
	
	/**
	 * Used to determine if the computer O.S is 64 or 32 bit. 
	 * @return Boolean - True if 64bit. False otherwise.
	 */
	public static boolean is64BitArch()
	{
		boolean is64bit = false;
		// Checks if O.S is windows...
		if (System.getProperty("os.name").contains("Windows"))
			// if so check for 32bit program files which will confirm architecture.
		    is64bit = (System.getenv("ProgramFiles(x86)") != null);
		else
			// if the String 64 doesn't exist in property os.arch then 32bit is confirmed. Otherwise the arch is 64 bit.
		    is64bit = (System.getProperty("os.arch").indexOf("64") != -1);
		
		// Return arch calculation.
		return is64bit;
	}
	
	/**
	 * Used to determine the byte order of the System Architecture.
	 * @return Boolean - True if Architecture is Big Endian, false otherwise. 
	 */
	public static boolean isEndianessBig()
	{
		return ByteOrder.nativeOrder().equals(ByteOrder.BIG_ENDIAN);
	}
	
	/**
	 * Retrieves the user language from the O.S.
	 * @return String - Language locale code for O.S.
	 */
	public static String getSystemLocale()
	{
		return System.getProperty("user.language");
	}
	
	/**
	 * Retrieves the MAC address associated with current DHCP activated network interface. Then parses it into
	 * a format that can be used for the end of the default server name.
	 * 
	 * WARNING: Having Multiple DHCP interfaces (such as tunnelling software) may interfere with this and return
	 * a false positive resulting in the wrong address. In this case the network adapter associated with that 
	 * particular DHCP address must be disabled.
	 *  
	 * @return StringBuffer - Last four digits of the DHCP MAC address.
	 */
	public static StringBuffer getModifiedMac()
	{
		// Retrieve MAC address from interface.
		final String mac_address = getAddress(MAC_ADDRESS_IDENTIFIER);
		// Initialize beginning parse position.
		final int begin_position = (mac_address.length()-MAC_OFFSET);
		// Retrieve the modified MAC address, from the last five index positions.
		String modified_address = mac_address.substring(begin_position);
		// Split the modified MAC address using the dash in between the last four digits. 
		final String[] split_modified_address = modified_address.split(ADDRESS_DELIMITER);
		// Concatenate the last four digits together.
		modified_address = new String(split_modified_address[0]+split_modified_address[1]);
		// Return MAC as StringBuffer.
		return new StringBuffer(modified_address);
	}
	
	/**
	 * Retrieves either the current DHCP MAC/IP address from the network interface. Used to get
	 * the MAC address for the server name.
	 * 
	 * Using the identifiers (IP_ADDRESS_IDENTIFIER and MAC_ADDRESS_IDENTIFIER above as parameters,
	 * either MAC/IP can be retrieved.
	 * 
	 * WARNING: Having Multiple DHCP interfaces (such as tunneling software) may interfere with this and return
	 * a false positive resulting in the wrong address. In this case the network adapter associated with that 
	 * particular DHCP address must be disabled.
	 * 
	 * @param address_type - IP_ADDRESS_IDENTIFIER or MAC_ADDRESS_IDENTIFIER. 
	 * @return String - IP or MAC of current DHCP activated interface, null if no DHCP interface available.
	 */
	private static String getAddress(String address_type)
	{
		// String for address storage.
	    String address = "";
	    // Attempts to retrieve DHCP address.
	    InetAddress lan_ip = getInetAddress();
	    // If no DHCP address is available then return with no address.
        if(lan_ip == null) return null;
        // If identifier equals I.P retrieval... 
        if(address_type.equals(IP_ADDRESS_IDENTIFIER))
        	// Parse I.P address and store.
        	address = lan_ip.toString().replaceAll("^/+", "");
        // Else if the identifier is MAC retrieval... 
        else if(address_type.equals(MAC_ADDRESS_IDENTIFIER))
        	// Retrieve MAC address using InetAddress of the Interface.
        	address = getMacAddress(lan_ip);
        // Else throw exception stating the option is not valid.
		else
			try
        	{
				throw new Exception("Specify \"ip\" or \"mac\"");
			} 
        	catch (Exception e) 
        	{
				e.printStackTrace();
			}

        // Return IP or MAC.
		return address;
	}
	
	/**
	 * Retrieves the InetAddress of the current DHCP activated network interface. It does
	 * this by checking through each available interface, if the InetAddress of that interface
	 * equals a Inet4Address(or IPV4) and also is a Local address then that interface is chosen.
	 * 
	 * WARNING: Having Multiple DHCP interfaces (such as tunneling software) may interfere with this and return
	 * a false positive resulting in the wrong address. In this case the network adapter associated with that 
	 * particular DHCP address must be disabled.
	 * 
	 * @return InetAddress - Address of the current activated DHCP interface.
	 */
	private static InetAddress getInetAddress()
	{
		// Used for storing a located DHCP interface address.
		InetAddress lan_ip = null;
		// Used for locating a InetAddress within a Interface.
		String ip_address = null;
		// Enumeration for containing the various network interfaces available.
		Enumeration<NetworkInterface> net = null;
		try
		{
			// Retrieves Network Interfaces available.
			net = NetworkInterface.getNetworkInterfaces();
			// Iterates through each interface...
			while(net.hasMoreElements())
			{
				// Retrieve interface.
				NetworkInterface element = net.nextElement();
				// Retrieves addresses associated with that interface.
				Enumeration<InetAddress> addresses = element.getInetAddresses();
				// Iterate through each address received...
				while (addresses.hasMoreElements())
				{
					// Get I.P from address.
					InetAddress ip = addresses.nextElement();
					// If InetAddress is an instance of the object Inet4Address (IPV4) and is a local address...
					if (ip instanceof Inet4Address && ip.isSiteLocalAddress())
					{
						// Store IP address for location.
						ip_address = ip.getHostAddress();
						// Retrieve the InetAddress associated with that verified address.
						lan_ip = InetAddress.getByName(ip_address);
					}
				}
			}
		}
		catch(UnknownHostException e)
		{
			e.printStackTrace();
		}
		catch(SocketException e)
		{
			e.printStackTrace();
		}
		
		// Return InetAddress.
		return lan_ip;
	}

	/**
	 * Retrieves MAC address of an Interface from a given InetAddress.
	 * @param InetAddress ip - Interface address from which the MAC will be taken from. 
	 * @return String - MAC address associated with the passed InetAddress Interface.
	 */
	private static String getMacAddress(InetAddress ip)
	{
		// Address for storage.
		String address = null;
		try
		{
			// Retrieve network interface from IP.
			NetworkInterface network = NetworkInterface.getByInetAddress(ip);
			// Retrieve MAC in byte form.
			byte[] mac = network.getHardwareAddress();
			// StringBuilder for delicate parsing.
			StringBuilder sb = new StringBuilder();
			// Format MAC address into a more readable form.
			for (int i = 0; i < mac.length; i++)
				sb.append(String.format("%02X%s", mac[i], (i < mac.length - 1) ? "-" : ""));
			
			// Cast builder to string for return.
			address = sb.toString();
		}
		catch (SocketException e) 
		{
			e.printStackTrace();
		}
		// Return MAC address.
		return address;
	}
	
	
	/**
	 * Calculates a passed long byte value into either Imperial -Megabytes or Gigabyte-Megabyte formats. 
	 * @param Long file_size - Amount of bytes to be translated.
	 * @return String - The amount of Megabytes or Gigabytes the file_size translates to.
	 */
	public static String dataCalculator(long file_size)
	{
		// Buffer for calculation.
		StringBuffer calculated_size = new StringBuffer();
		// If total file size is less than a Imperial Gigabyte Unit...
		if(file_size < IMPERIAL_GB_UNIT)
			// Then calculate in just Megabytes.
			calculated_size.append(file_size/IMPERIAL_MB_UNIT + "MB");
		// If total file size is greater than a Imperial Gigabyte Unit...
		else if(file_size > IMPERIAL_GB_UNIT)
		{
			// Retrieve the amount of gigabytes available.
			int gigabytes = (int) (file_size/IMPERIAL_GB_UNIT),
				// Retrieve the remaining megabytes available.
				remaining_size = (int) (file_size - (gigabytes*IMPERIAL_GB_UNIT)),
				// Calculate remaining megabytes to the Imperial Megabyte Unit.
				megabytes = (int) (remaining_size/IMPERIAL_MB_UNIT);
			// Append Calculation to buffer.
			calculated_size.append( gigabytes+"."+Integer.toString(megabytes).toCharArray()[0]+"GB");
		}
		// Return calculated result as String.
		return calculated_size.toString();
	}

}
