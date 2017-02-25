package net.eureka.androidcast.player;

import java.io.IOException;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Collections;
/**
 * Contains useful methods that could be common to the whole media server.
 * 
 * @author Owen McMonagle
 *
 */
public final class Static 
{
	
	private static final ArrayList<String> INTERFACES = new ArrayList<String>();

	
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
	 * Used to determine the byte order of the System Architecture.
	 * @return Boolean - True if Architecture is Big Endian, false otherwise. 
	 */
	public static boolean isEndianessBig()
	{
		return ByteOrder.nativeOrder().equals(ByteOrder.BIG_ENDIAN);
	}
	
	/**
	 * Retrieves particular network interface address by the passed parameter name.
	 * @param name - Name of network interface.
	 * @return {@link InetAddress} - Found network address, null if none was found.
	 */
	public static InetAddress getInetAddressFromName(String name)
	{
		ArrayList<InetAddress> valid_addresses = getInetAddresses();
		
		for(int i = 0; i < valid_addresses.size(); i ++)
			if(INTERFACES.get(i).equals(name))
				return valid_addresses.get(i);
		
		return null;
	}
	
	/**
	 * Searches through available network interfaces and creates a list of bindable address to return.
	 * @return {@link ArrayList} - List of network interface addresses of type {@link InetAddress}.
	 */
	public static ArrayList<InetAddress> getInetAddresses()
	{
		ArrayList<InetAddress> valid_addresses = new ArrayList<>();
		
		try 
		{
			ArrayList<NetworkInterface> interfaces = Collections.list(NetworkInterface.getNetworkInterfaces());
			
			for (NetworkInterface net_interface : interfaces) 
				if(!INTERFACES.contains(net_interface.toString()) && net_interface.isUp() && !net_interface.isVirtual() && !net_interface.isLoopback() && net_interface.supportsMulticast())
				{
					// iterate over the addresses associated with the interface
					ArrayList<InetAddress> addresses = Collections.list(net_interface.getInetAddresses());
					for (int i = 0; i < addresses.size(); i ++)
					{
						InetAddress address = addresses.get(i);
						
						// look only for ipv4 and reachable addresses. 
						if (!(address instanceof Inet6Address) && address.isReachable(3000))
						{
							String name = net_interface.toString();
							System.out.format("["+i+"] ni: %s\n", name);
							// Add valid interface address.
							valid_addresses.add(address);
							// Add name to interface name list
							INTERFACES.add(name.split(":")[1]);
							// Make i the size limit to exit loop.
							i = addresses.size();
						}
					}
				}
		}
		catch (IOException e) 
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return valid_addresses;
	}
}
