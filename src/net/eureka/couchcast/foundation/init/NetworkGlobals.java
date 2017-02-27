package net.eureka.couchcast.foundation.init;

import java.net.InetAddress;


public final class NetworkGlobals 
{
	
	private static InetAddress dhcpNetwork = null;
	
	public static void setDhcpNetwork(InetAddress dhcp_network)
	{
		NetworkGlobals.dhcpNetwork = dhcp_network;
	}
	
	public static InetAddress getDHCPInterface()
	{
		return dhcpNetwork;
	}

}
