/*
 * MadKitLanEdition (created by Jason MAHDJOUB (jason.mahdjoub@distri-mind.fr)) Copyright (c)
 * 2015 is a fork of MadKit and MadKitGroupExtension. 
 * 
 * Copyright or © or Copr. Jason Mahdjoub, Fabien Michel, Olivier Gutknecht, Jacques Ferber (1997)
 * 
 * jason.mahdjoub@distri-mind.fr
 * fmichel@lirmm.fr
 * olg@no-distance.net
 * ferber@lirmm.fr
 * 
 * This software is a computer program whose purpose is to
 * provide a lightweight Java library for designing and simulating Multi-Agent Systems (MAS).
 * This software is governed by the CeCILL-C license under French law and
 * abiding by the rules of distribution of free software.  You can  use,
 * modify and/ or redistribute the software under the terms of the CeCILL-C
 * license as circulated by CEA, CNRS and INRIA at the following URL
 * "http://www.cecill.info".
 * As a counterpart to the access to the source code and  rights to copy,
 * modify and redistribute granted by the license, users are provided only
 * with a limited warranty  and the software's author,  the holder of the
 * economic rights,  and the successive licensors  have only  limited
 * liability.
 * 
 * In this respect, the user's attention is drawn to the risks associated
 * with loading,  using,  modifying and/or developing or reproducing the
 * software by the user in light of its specific status of free software,
 * that may mean  that it is complicated to manipulate,  and  that  also
 * therefore means  that it is reserved for developers  and  experienced
 * professionals having in-depth computer knowledge. Users are therefore
 * encouraged to load and test the software's suitability as regards their
 * requirements in conditions enabling the security of their systems and/or
 * data to be ensured and,  more generally, to use and operate it in the
 * same conditions as regards security.
 * The fact that you are presently reading this means that you have had
 * knowledge of the CeCILL-C license and that you accept its terms.
 */
package com.distrimind.madkit.kernel.network;

import com.distrimind.madkit.agr.LocalCommunity;
import com.distrimind.madkit.kernel.*;
import com.distrimind.madkit.message.KernelMessage;
import com.distrimind.util.OS;
import com.distrimind.util.OSVersion;
import com.distrimind.util.properties.DocumentBuilderFactoryWithNonDTD;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import org.fourthline.cling.UpnpService;
import org.fourthline.cling.UpnpServiceImpl;
import org.fourthline.cling.binding.xml.*;
import org.fourthline.cling.controlpoint.ControlPoint;
import org.fourthline.cling.controlpoint.ControlPointImpl;
import org.fourthline.cling.model.Namespace;
import org.fourthline.cling.model.UnsupportedDataException;
import org.fourthline.cling.model.ValidationException;
import org.fourthline.cling.model.action.ActionInvocation;
import org.fourthline.cling.model.message.Connection;
import org.fourthline.cling.model.message.*;
import org.fourthline.cling.model.message.control.ActionRequestMessage;
import org.fourthline.cling.model.message.control.ActionResponseMessage;
import org.fourthline.cling.model.message.gena.OutgoingEventRequestMessage;
import org.fourthline.cling.model.message.header.CallbackHeader;
import org.fourthline.cling.model.message.header.HostHeader;
import org.fourthline.cling.model.message.header.LocationHeader;
import org.fourthline.cling.model.message.header.UpnpHeader;
import org.fourthline.cling.model.meta.*;
import org.fourthline.cling.model.profile.RemoteClientInfo;
import org.fourthline.cling.model.types.*;
import org.fourthline.cling.protocol.ProtocolFactory;
import org.fourthline.cling.protocol.ProtocolFactoryImpl;
import org.fourthline.cling.protocol.RetrieveRemoteDescriptors;
import org.fourthline.cling.protocol.async.ReceivingNotification;
import org.fourthline.cling.protocol.async.ReceivingSearchResponse;
import org.fourthline.cling.registry.DefaultRegistryListener;
import org.fourthline.cling.registry.RegistrationException;
import org.fourthline.cling.registry.Registry;
import org.fourthline.cling.registry.RegistryImpl;
import org.fourthline.cling.support.igd.callback.GetExternalIP;
import org.fourthline.cling.support.igd.callback.GetStatusInfo;
import org.fourthline.cling.support.igd.callback.PortMappingAdd;
import org.fourthline.cling.support.igd.callback.PortMappingDelete;
import org.fourthline.cling.support.model.Connection.Status;
import org.fourthline.cling.support.model.Connection.StatusInfo;
import org.fourthline.cling.support.model.PortMapping;
import org.fourthline.cling.support.model.PortMapping.Protocol;
import org.fourthline.cling.transport.RouterException;
import org.fourthline.cling.transport.impl.*;
import org.fourthline.cling.transport.spi.*;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.FactoryConfigurationError;
import java.io.IOException;
import java.io.StringReader;
import java.lang.reflect.Method;
import java.net.*;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This class agent aims to analyze network interfaces, local networks, and
 * local routers. Than to the UPNP IGD protocol, it is able to give external ip
 * address behind a router, open ports, and get connection status.
 * 
 * When a router is detected, a message {@link IGDRouterFoundMessage} is sent to the
 * agents taking the role
 * {@link LocalCommunity.Roles#LOCAL_NETWORK_AFFECTATION_ROLE} and the group
 * {@link LocalCommunity.Groups#NETWORK}. When a router is removed, a message
 * {@link IGDRouterFoundMessage} is sent to the same agents.
 * 
 * To ask for a connection status message, you need to send a
 * {@link AskForConnectionStatusMessage} message to the group
 * {@link LocalCommunity.Groups#NETWORK} and to the role
 * {@link LocalCommunity.Roles#LOCAL_NETWORK_EXPLORER_ROLE}. Then a
 * {@link ConnexionStatusMessage} will be returned. Changes over time can be
 * notified.
 * 
 * To ask for the external IP message, you need to send a
 * {@link AskForExternalIPMessage} to the same agent. Then a
 * {@link ExternalIPMessage} will be returned. Changes over time can be
 * notified.
 * 
 * To add a port mapping into a specific router, you need to send a
 * {@link AskForPortMappingAddMessage} to the same agent. Then a
 * {@link PortMappingAnswerMessage} will be returned.
 * 
 * To remove a port mapping into a specific router, you need to send a
 * {@link AskForPortMappingDeleteMessage} to the same agent.
 * 
 * @author Jason Mahdjoub
 * @since MadKitLanEdition 1.0
 * @version 1.0
 *
 */
@SuppressWarnings({"SameParameterValue", "UnusedReturnValue"})
class UpnpIGDAgent extends AgentFakeThread {

	private static final String[] sub_loggers_names = { Registry.class.getName(), UpnpServiceImpl.class.getName(),
			org.fourthline.cling.DefaultUpnpServiceConfiguration.class.getName(), DatagramProcessor.class.getName(),
			SOAPActionProcessor.class.getName(), GENAEventProcessor.class.getName(),
			DeviceDescriptorBinder.class.getName(), ServiceDescriptorBinder.class.getName(), Namespace.class.getName(),
			Registry.class.getName(), org.fourthline.cling.transport.Router.class.getName(),
			ControlPointImpl.class.getName(), ProtocolFactory.class.getName(), MulticastReceiver.class.getName(),
			StreamServer.class.getName(), DatagramIO.class.getName() };

	protected static volatile UpnpService upnpService = null;
	protected static int pointedUpnpServiceNumber = 0;

	final static ThreadPoolExecutor serviceExecutor = new ThreadPoolExecutor(1, Integer.MAX_VALUE, 1, TimeUnit.SECONDS, new SynchronousQueue<>());
	//final static PoolExecutor serviceExecutor = new PoolExecutor(4, 5, 1, TimeUnit.SECONDS,new CircularArrayList<Runnable>(16, true));

	private final RegistryListener registryListener = new RegistryListener();
	private final HashMap<InetAddress, Router> upnp_igd_routers = new HashMap<>();
	private final ArrayList<AskForRouterDetectionInformation> callers_for_router_detection = new ArrayList<>();



	static DocumentBuilderFactory newDocumentBuilderFactoryWithNonDTDInstance()
	{
		return DocumentBuilderFactoryWithNonDTD.newDocumentBuilderFactoryWithNonDTDInstance();
	}
	/*
	 * Fix DDOS and SSRF issue : https://github.com/4thline/cling/issues/253
	 */
	static NetworkAddressFactory createNetworkAddressFactory(int streamListenPort, int multicastPort) {

		return new NetworkAddressFactoryImpl(streamListenPort) {
			@Override
			public int getMulticastPort() {
				return multicastPort;
			}
			public InetAddress getLocalAddress(NetworkInterface networkInterface, boolean isIPv6, InetAddress remoteAddress) {

				// First try to find a local IP that is in the same subnet as the remote IP
				return getBindAddressInSubnetOf(remoteAddress);
			}
		};
	}
	static boolean isNotValidRemoteAddress(URL u, NetworkAddressFactory networkAddressFactory)
	{
		if (u==null)
			return false;
		return isNotValidRemoteAddress(u.getHost(), networkAddressFactory);
	}
	static boolean isNotValidRemoteAddress(String host, NetworkAddressFactory networkAddressFactory)
	{
		try {
			InetAddress ia = InetAddress.getByName(host);
			ia = networkAddressFactory.getLocalAddress(
					null,
					ia instanceof Inet6Address,
					ia
			);
			if (ia == null)
				return true;
		} catch (Exception ignored) {
			return true;
		}
		return false;
	}

	private static final Set<UpnpHeader.Type> allowedUpnpHeaders=new HashSet<>(Arrays.asList(UpnpHeader.Type.EXT, UpnpHeader.Type.ST, UpnpHeader.Type.SERVER, UpnpHeader.Type.USN, UpnpHeader.Type.LOCATION, UpnpHeader.Type.MAX_AGE));
	/*
	 * Fix DDOS and SSRF issue : https://github.com/4thline/cling/issues/253
	 */
	static IncomingDatagramMessage<?> getValidIncomingDatagramMessage(IncomingDatagramMessage<?> idm, NetworkAddressFactory networkAddressFactory)
	{
		for (UpnpHeader.Type t : UpnpHeader.Type.values()) {
			if (allowedUpnpHeaders.contains(t))
				continue;
			if (idm.getHeaders().containsKey(t))
				return null;
		}
		@SuppressWarnings("rawtypes") List<UpnpHeader> luh=idm.getHeaders().get(UpnpHeader.Type.CALLBACK);

		if (luh!=null) {
			for (UpnpHeader<?> uh : luh) {
				if (CallbackHeader.class.isAssignableFrom(uh.getClass())) {
					CallbackHeader ch = (CallbackHeader) uh;
					for (URL u : ch.getValue()) {
						if (isNotValidRemoteAddress(u, networkAddressFactory))
							return null;
					}
				}
			}
		}
		luh=idm.getHeaders().get(UpnpHeader.Type.HOST);
		if (luh!=null) {
			for (UpnpHeader<?> uh : luh) {
				if (HostHeader.class.isAssignableFrom(uh.getClass())) {
					HostHeader hh = (HostHeader) uh;
					if (isNotValidRemoteAddress(hh.getValue().getHost(), networkAddressFactory))
						return null;
				}
			}
		}
		luh=idm.getHeaders().get(UpnpHeader.Type.LOCATION);
		if (luh!=null) {
			for (UpnpHeader<?> uh : luh) {
				if (LocationHeader.class.isAssignableFrom(uh.getClass())) {
					LocationHeader hh = (LocationHeader) uh;
					if (isNotValidRemoteAddress(hh.getValue().getHost(), networkAddressFactory))
						return null;
				}
			}
		}
		return idm;
	}

	static MulticastReceiver<?> createMulticastReceiver(NetworkAddressFactory networkAddressFactory) {
		return new MulticastReceiverImpl(
				new MulticastReceiverConfigurationImpl(
						networkAddressFactory.getMulticastGroup(),
						networkAddressFactory.getMulticastPort()
				)
		){

			public void run() {

				while (true) {

					try {
						byte[] buf = new byte[getConfiguration().getMaxDatagramBytes()];
						DatagramPacket datagram = new DatagramPacket(buf, buf.length);

						socket.receive(datagram);
						InetAddress receivedOnLocalAddress =
								networkAddressFactory.getLocalAddress(
										multicastInterface,
										multicastAddress.getAddress() instanceof Inet6Address,
										datagram.getAddress()
								);
						if (receivedOnLocalAddress==null)
							continue;
						/*
						 * Fix DDOS and SSRF issue : https://github.com/4thline/cling/issues/253
						 */
						IncomingDatagramMessage<?> idm=getValidIncomingDatagramMessage(datagramProcessor.read(receivedOnLocalAddress, datagram),networkAddressFactory);
						if (idm==null)
							continue;
						router.received(idm);

					} catch (SocketException ex) {
						break;
					}
					catch (UnsupportedDataException ignored)
					{

					}
					catch (Exception ex) {
						throw new RuntimeException(ex);
					}
				}
				try {
					if (!socket.isClosed()) {
						socket.close();
					}
				} catch (Exception ex) {
					throw new RuntimeException(ex);
				}
			}
		};
	}

	static class RequestHttpHandler implements HttpHandler {

		private final org.fourthline.cling.transport.Router router;
		private final NetworkAddressFactory networkAddressFactory;

		RequestHttpHandler(org.fourthline.cling.transport.Router router, NetworkAddressFactory networkAddressFactory) {
			this.router = router;
			this.networkAddressFactory=networkAddressFactory;
		}

		// This is executed in the request receiving thread!
		public void handle(final HttpExchange httpExchange) throws IOException {
			InetSocketAddress isa=httpExchange.getRemoteAddress();
			if (isa==null)
				return;
			InetAddress receivedOnLocalAddress =
					networkAddressFactory.getLocalAddress(
							null,
							isa.getAddress() instanceof Inet6Address,
							isa.getAddress()
					);
			if (receivedOnLocalAddress==null)
				return;
			router.received(
					new HttpExchangeUpnpStream(router.getProtocolFactory(), httpExchange) {
						@Override
						protected Connection createConnection() {
							return new HttpServerConnection(httpExchange);
						}
					}
			);
		}
	}
	private static class HttpServerConnection implements Connection {

		protected HttpExchange exchange;

		public HttpServerConnection(HttpExchange exchange) {
			this.exchange = exchange;
		}

		@Override
		public boolean isOpen() {
			return true;
		}

		@Override
		public InetAddress getRemoteAddress() {
			return exchange.getRemoteAddress() != null
					? exchange.getRemoteAddress().getAddress()
					: null;
		}

		@Override
		public InetAddress getLocalAddress() {
			return exchange.getLocalAddress() != null
					? exchange.getLocalAddress().getAddress()
					: null;
		}
	}

	/*
	 * Fix DDOS and SSRF issue : https://github.com/4thline/cling/issues/253
	 */
	static DatagramIO<?> createDatagramIO(NetworkAddressFactory networkAddressFactory) {
		return new DatagramIOImpl(new DatagramIOConfigurationImpl()){
			@Override
			public void run() {

				while (true) {

					try {
						byte[] buf = new byte[getConfiguration().getMaxDatagramBytes()];
						DatagramPacket datagram = new DatagramPacket(buf, buf.length);

						socket.receive(datagram);
						InetAddress receivedOnLocalAddress =
								networkAddressFactory.getLocalAddress(
										null,
										datagram.getAddress() instanceof Inet6Address,
										datagram.getAddress()
								);
						if (receivedOnLocalAddress==null)
							continue;
						/*
						 * Fix DDOS and SSRF issue : https://github.com/4thline/cling/issues/253
						 */
						IncomingDatagramMessage<?> idm=getValidIncomingDatagramMessage(datagramProcessor.read(localAddress.getAddress(), datagram), networkAddressFactory);
						if (idm==null)
							continue;
						router.received(idm);

					} catch (SocketException ex) {
						break;
					} catch (UnsupportedDataException ignored) {

					} catch (Exception ex) {
						throw new RuntimeException(ex);
					}
				}
				try {
					if (!socket.isClosed()) {
						socket.close();
					}
				} catch (Exception ex) {
					throw new RuntimeException(ex);
				}
			}
		};
	}

	/*
	 * FIX XXE issue : https://github.com/4thline/cling/issues/243
	 */
	static GENAEventProcessor createGENAEventProcessor() {
		return new GENAEventProcessorImpl(){
			@Override
			protected DocumentBuilderFactory createDocumentBuilderFactory() throws FactoryConfigurationError {
				return UpnpIGDAgent.newDocumentBuilderFactoryWithNonDTDInstance();
			}
			@Override
			public void writeBody(OutgoingEventRequestMessage requestMessage) throws UnsupportedDataException {

				try {

					DocumentBuilderFactory factory = createDocumentBuilderFactory();
					factory.setNamespaceAware(true);
					Document d = factory.newDocumentBuilder().newDocument();
					Element propertysetElement = writePropertysetElement(d);

					writeProperties(d, propertysetElement, requestMessage);

					requestMessage.setBody(UpnpMessage.BodyType.STRING, toString(d));

				} catch (Exception ex) {
					throw new UnsupportedDataException("Can't transform message payload: " + ex.getMessage(), ex);
				}
			}

		};
	}
	/*
	 * FIX XXE issue : https://github.com/4thline/cling/issues/243
	 */
	static SOAPActionProcessor createSOAPActionProcessor() {
		return new SOAPActionProcessorImpl()
		{
			@Override
			protected DocumentBuilderFactory createDocumentBuilderFactory() throws FactoryConfigurationError {
				return UpnpIGDAgent.newDocumentBuilderFactoryWithNonDTDInstance();
			}
			@Override
			public void writeBody(ActionRequestMessage requestMessage, ActionInvocation actionInvocation) throws UnsupportedDataException {

				try {

					DocumentBuilderFactory factory = createDocumentBuilderFactory();
					factory.setNamespaceAware(true);
					Document d = factory.newDocumentBuilder().newDocument();
					Element body = writeBodyElement(d);

					writeBodyRequest(d, body, requestMessage, actionInvocation);


				} catch (Exception ex) {
					throw new UnsupportedDataException("Can't transform message payload: " + ex, ex);
				}
			}
			@Override
			public void writeBody(ActionResponseMessage responseMessage, ActionInvocation actionInvocation) throws UnsupportedDataException {

				try {

					DocumentBuilderFactory factory = createDocumentBuilderFactory();
					factory.setNamespaceAware(true);
					Document d = factory.newDocumentBuilder().newDocument();
					Element body = writeBodyElement(d);

					if (actionInvocation.getFailure() != null) {
						writeBodyFailure(d, body, responseMessage, actionInvocation);
					} else {
						writeBodyResponse(d, body, responseMessage, actionInvocation);
					}

				} catch (Exception ex) {
					throw new UnsupportedDataException("Can't transform message payload: " + ex, ex);
				}
			}
		};
	}
	/*
	 * FIX XXE issue : https://github.com/4thline/cling/issues/243
	 */
	static DeviceDescriptorBinder createDeviceDescriptorBinderUDA10(NetworkAddressFactory networkAddressFactory) {
		//noinspection rawtypes
		return new UDA10DeviceDescriptorBinderImpl()
		{
			@Override
			public <D extends Device> D describe(D undescribedDevice, String descriptorXml) throws DescriptorBindingException, ValidationException {

				if (descriptorXml == null || descriptorXml.length() == 0) {
					throw new DescriptorBindingException("Null or empty descriptor");
				}

				try {
					DocumentBuilderFactory factory = newDocumentBuilderFactoryWithNonDTDInstance();
					factory.setNamespaceAware(true);
					DocumentBuilder documentBuilder = factory.newDocumentBuilder();
					documentBuilder.setErrorHandler(this);

					Document d = documentBuilder.parse(
							new InputSource(
									new StringReader(descriptorXml.trim())
							)
					);

					D res=describe(undescribedDevice, d);
					if (res.getDetails()!=null && isNotValidRemoteAddress(res.getDetails().getBaseURL(), networkAddressFactory))
						return null;

					return res;
				} catch (ValidationException ex) {
					throw ex;
				} catch (Exception ex) {
					throw new DescriptorBindingException("Could not parse device descriptor: " + ex, ex);
				}
			}
			@Override
			public Document buildDOM(Device deviceModel, RemoteClientInfo info, Namespace namespace) throws DescriptorBindingException {

				try {
					DocumentBuilderFactory factory = newDocumentBuilderFactoryWithNonDTDInstance();
					factory.setNamespaceAware(true);

					Document d = factory.newDocumentBuilder().newDocument();
					generateRoot(namespace, deviceModel, d, info);

					return d;

				} catch (Exception ex) {
					throw new DescriptorBindingException("Could not generate device descriptor: " + ex.getMessage(), ex);
				}
			}
		};
	}

	/*
	 * FIX XXE issue : https://github.com/4thline/cling/issues/243
	 */
	static ServiceDescriptorBinder createServiceDescriptorBinderUDA10(NetworkAddressFactory networkAddressFactory) {
		//noinspection rawtypes
		return new UDA10ServiceDescriptorBinderImpl(){
			@Override
			public <S extends Service> S describe(S undescribedService, String descriptorXml) throws DescriptorBindingException, ValidationException {
				if (descriptorXml == null || descriptorXml.length() == 0) {
					throw new DescriptorBindingException("Null or empty descriptor");
				}

				try {
					DocumentBuilderFactory factory = newDocumentBuilderFactoryWithNonDTDInstance();
					factory.setNamespaceAware(true);
					DocumentBuilder documentBuilder = factory.newDocumentBuilder();
					documentBuilder.setErrorHandler(this);

					Document d = documentBuilder.parse(
							new InputSource(
									new StringReader(descriptorXml.trim())
							)
					);

					S res= describe(undescribedService, d);
					if (res.getDevice()!=null && res.getDevice().getDetails()!=null && isNotValidRemoteAddress(res.getDevice().getDetails().getBaseURL(), networkAddressFactory))
						return null;
					return res;

				} catch (ValidationException ex) {
					throw ex;
				} catch (Exception ex) {
					throw new DescriptorBindingException("Could not parse service descriptor: " + ex, ex);
				}
			}
			@Override
			public Document buildDOM(Service service) throws DescriptorBindingException {

				try {
					DocumentBuilderFactory factory = newDocumentBuilderFactoryWithNonDTDInstance();
					factory.setNamespaceAware(true);

					Document d = factory.newDocumentBuilder().newDocument();
					Method m=UDA10ServiceDescriptorBinderImpl.class.getDeclaredMethod("generateScpd", Service.class, Document.class);
					AccessController.doPrivileged((PrivilegedAction<Object>) () -> {
						m.setAccessible(true);
						return null;
					});

					return d;

				} catch (Exception ex) {
					throw new DescriptorBindingException("Could not generate service descriptor: " + ex.getMessage(), ex);
				}
			}
		};

	}

	protected void addRouter(InetAddress ia, Router router) {
		if (ia == null)
			throw new NullPointerException("ia");
		if (router == null)
			throw new NullPointerException("router");

		//removeRouter(ia, false);
		synchronized (upnp_igd_routers) {
			if (upnp_igd_routers.containsKey(ia))
				return;
			upnp_igd_routers.put(ia, router);
			for (AskForRouterDetectionInformation m : callers_for_router_detection)
				sendReply(m, new IGDRouterFoundMessage(router.internal_address));
		}
	}

	protected void updateRouter(RemoteDevice router) {
		if (router == null)
			throw new NullPointerException("router");
        RemoteService connectionService;
        if ((connectionService=discoverConnectionService(router)) == null)
            return;
		boolean unknownDeviceDetected = false;
		synchronized (upnp_igd_routers) {
			try {
				InetAddress ia = InetAddress.getByName(router.getIdentity().getDescriptorURL().getHost());
				Router r = upnp_igd_routers.remove(ia);
				if (r == null) {
					for (Iterator<Router> it = upnp_igd_routers.values().iterator(); it.hasNext();) {
						Router ro = it.next();
						if (ro.device.getIdentity().equals(router.getIdentity())) {
							r = ro;
							it.remove();
							break;
						}
					}
				}
				if (r == null)
					unknownDeviceDetected = true;
				else {



					if (!r.internal_address.equals(ia)) {
						for (AskForRouterDetectionInformation m : callers_for_router_detection) {
							sendReply(m, new IGDRouterLostMessage(r.internal_address));
							sendReply(m, new IGDRouterFoundMessage(ia));
						}
						r.internal_address = ia;
					}
					r.service = connectionService;
					upnp_igd_routers.put(ia, r);

				}
			} catch (UnknownHostException e) {
				if (getLogger1() != null)
					getLogger1().severeLog("Device updated but impossible to access to its ip address : ", e);
				else
					e.printStackTrace();
			}
		}
		if (unknownDeviceDetected)
			remoteDeviceDetected(router);
	}

	protected void remoteDeviceDetected(RemoteDevice device) {
		try {
			RemoteService connectionService;
			if ((connectionService = discoverConnectionService(device)) == null) {
				return;
			}

			if (getLogger1() != null && getLogger1().isLoggable(Level.FINE))
				getLogger1().fine("Device added : " + device);

			InetAddress ip = InetAddress.getByName(device.getIdentity().getDescriptorURL().getHost());

			addRouter(ip, new Router(ip, device, connectionService));
		} catch (UnknownHostException e) {
			if (getLogger1() != null)
				getLogger1().severeLog("Device added but impossible to access to its ip address : ", e);
			else
				e.printStackTrace();
		}

	}

	protected RemoteService discoverConnectionService(RemoteDevice device) {

		if (!device.getType().getType().equals(IGD)) {
			return null;
		}

		RemoteDevice[] connectionDevices = device.findDevices(CONNECTION_DEVICE_TYPE);
		if (connectionDevices.length == 0) {
			if (getLogger1() != null && getLogger1().isLoggable(Level.FINE))
				getLogger1().fine("IGD doesn't support '" + CONNECTION_DEVICE_TYPE + "': " + device);

			return null;
		}

		RemoteDevice connectionDevice = connectionDevices[0];
		if (getLogger1() != null && getLogger1().isLoggable(Level.FINE))
			getLogger1().fine("Using first discovered WAN connection device: " + connectionDevice);

		RemoteService ipConnectionService = connectionDevice.findService(IP_SERVICE_TYPE);
		if (ipConnectionService == null)
			ipConnectionService = connectionDevice.findService(IP_SERVICE_TYPE_BIS);
		if (ipConnectionService == null)
			ipConnectionService = connectionDevice.findService(IP_SERVICE_TYPE2);
		if (ipConnectionService == null)
			ipConnectionService = connectionDevice.findService(IP_SERVICE_TYPE_BIS2);
		RemoteService pppConnectionService = connectionDevice.findService(PPP_SERVICE_TYPE);
		if (pppConnectionService==null)
			pppConnectionService = connectionDevice.findService(PPP_SERVICE_TYPE2);

		if (ipConnectionService == null && pppConnectionService == null && getLogger1() != null
				&& getLogger1().isLoggable(Level.FINE)) {
			getLogger1().fine("IGD doesn't support IP or PPP WAN connection service: " + device);
		}

		return ipConnectionService != null ? ipConnectionService : pppConnectionService;
	}

	protected Router removeRouter(InetAddress ia, boolean manual) {
		Router res;
		synchronized (upnp_igd_routers) {
			res = upnp_igd_routers.remove(ia);
		}
		if (res != null)
			res.setRemoved(manual);
		return res;
	}

	protected void removeAllRouters(boolean manual) {
		synchronized (upnp_igd_routers) {
			for (Router r : upnp_igd_routers.values()) {
				r.setRemoved(manual);
			}
			upnp_igd_routers.clear();
		}
	}

	protected class Router {
		protected volatile InetAddress internal_address;
		protected final AtomicReference<InetAddress> external_address = new AtomicReference<>(null);
		protected final RemoteDevice device;
		protected volatile RemoteService service;
		protected final AtomicReference<StatusInfo> status = new AtomicReference<>(null);
		private final AtomicBoolean removed = new AtomicBoolean(false);

		private Task<Void> status_task_updater = null;
		private TaskID status_task_id = null;
		private Task<Void> external_address_task_updater = null;
		private TaskID external_address_task_id = null;

		protected final ArrayList<AskForConnectionStatusMessage> asks_for_status = new ArrayList<>();
		protected final ArrayList<AskForExternalIPMessage> asks_for_external_ip = new ArrayList<>();
		protected final ArrayList<PortMapping> desired_mappings = new ArrayList<>();

		protected Router(InetAddress _internal_address, RemoteDevice _remote_device, RemoteService _service) {
			if (_internal_address == null)
				throw new NullPointerException("_internal_address");
			if (_remote_device == null)
				throw new NullPointerException("_remote_device");
			if (_service == null)
				throw new NullPointerException("_service");

			internal_address = _internal_address;
			device = _remote_device;
			service = _service;
		}

		@Override
		public boolean equals(Object o) {
			if (o == null)
				return false;
			if (o instanceof Router)
				return internal_address.equals(((Router) o).internal_address);
			else
				return false;
		}

		@Override
		public int hashCode() {
			return internal_address.hashCode();
		}

		void setRemoved(boolean manual) {
			if (status_task_id != null) {
				cancelTask(status_task_id, false);
				status_task_id = null;
			}
			if (external_address_task_id != null) {
				cancelTask(external_address_task_id, false);
				external_address_task_id = null;
			}
			if (!removed.get()) {
				removed.set(true);

				ConnexionStatusMessage answer = new ConnexionStatusMessage(internal_address,
						new StatusInfo(Status.Disconnected, 0, null), status.get());
				synchronized (asks_for_status) {
					for (AskForConnectionStatusMessage m : asks_for_status) {
						UpnpIGDAgent.this.sendReply(m, answer.clone());
					}
				}

				UpnpIGDAgent.this.broadcastMessage(LocalCommunity.Groups.NETWORK,
						LocalCommunity.Roles.LOCAL_NETWORK_AFFECTATION_ROLE, new IGDRouterLostMessage(internal_address),
						false);

				if (manual) {
					removeAllPointMapping();
				} else {
					synchronized (desired_mappings) {
						if (desired_mappings.size() > 0)
							handleFailureMessage(
									"Device disappeared, couldn't delete port mappings: " + desired_mappings.size());
					}

				}
			}
		}

		public void newMessage(final AskForConnectionStatusMessage m) {
			if (UpnpIGDAgent.upnpService != null && !removed.get()) {
				boolean cancel = false;
				boolean update_repetitive_task = false;
				long referenced_delay = -1;
				if (m.isRepetitive()) {
					synchronized (asks_for_status) {
						asks_for_status.add(m);
						if (status_task_id != null && (status_task_updater == null
								|| status_task_updater.getDurationBetweenEachRepetitionInMilliSeconds() > m.getDelayInMs())) {
							UpnpIGDAgent.this.cancelTask(status_task_id, false);
							status_task_id = null;
							status_task_updater = null;
						}
						if (status_task_updater == null) {
							update_repetitive_task = true;
							referenced_delay = m.getDelayInMs();
						}
					}
				} else {
					synchronized (asks_for_status) {
						long delay = -1;
						Iterator<AskForConnectionStatusMessage> it = asks_for_status.iterator();
						while (it.hasNext()) {
							AskForConnectionStatusMessage tmpm = it.next();
							if (tmpm.getSender().equals(m.getSender())) {
								delay = tmpm.getDelayInMs();
								it.remove();
								cancel = true;
								break;
							}
						}

						if (cancel) {
							if (delay == status_task_updater.getDurationBetweenEachRepetitionInMilliSeconds()
									|| asks_for_status.size() == 0) {
								UpnpIGDAgent.this.cancelTask(status_task_id, false);
								status_task_id = null;
								status_task_updater = null;
							}
							if (asks_for_status.size() > 0) {
								for (AskForConnectionStatusMessage tmpm : asks_for_status) {
									if (tmpm.getDelayInMs() > referenced_delay)
										referenced_delay = tmpm.getDelayInMs();
								}
								update_repetitive_task = true;
							}
						}
					}
				}
				if (update_repetitive_task) {
					status_task_updater = new Task<Void>(System.currentTimeMillis() + referenced_delay, referenced_delay) {
						@Override
						public Void call() {
							upnpService.getControlPoint().execute(new GetStatusInfo(service) {

								@Override
								public void failure(ActionInvocation _invocation,
													UpnpResponse _operation, String _defaultMsg) {
									if (getLogger1() != null)
										getLogger1().warning(_defaultMsg);
								}

								@Override
								protected void success(StatusInfo _statusInfo) {
									StatusInfo old = status.get();
									if (old == null || !old.equals(_statusInfo)) {
										ConnexionStatusMessage answer = new ConnexionStatusMessage(internal_address,
												_statusInfo, old);
										status.set(_statusInfo);
										synchronized (asks_for_status) {
											asks_for_status.removeIf(m1 -> !UpnpIGDAgent.this.sendReply(m1, answer.clone())
													.equals(ReturnCode.SUCCESS));
										}
									}

								}
							});
							return null;
						}
					};
					status_task_id = UpnpIGDAgent.this.scheduleTask(status_task_updater);
				}

				upnpService.getControlPoint().execute(new GetStatusInfo(service) {

					@Override
					public void failure(ActionInvocation _invocation,
							UpnpResponse _operation, String _defaultMsg) {

						if (getLogger1() != null)
							getLogger1().warning(_defaultMsg);
						ConnexionStatusMessage answer = new ConnexionStatusMessage(internal_address, null,
								status.get());
						answer.setMessage(_defaultMsg);
						UpnpIGDAgent.this.sendReply(m, answer);
					}

					@Override
					protected void success(StatusInfo _statusInfo) {
						UpnpIGDAgent.this.sendReply(m, new ConnexionStatusMessage(internal_address, _statusInfo, null));
					}
				});
			}
		}

		public void newMessage(final AskForExternalIPMessage m) {
			if (UpnpIGDAgent.upnpService != null && !removed.get()) {
				boolean cancel = false;
				boolean update_repetitive_task = false;
				long referenced_delay = -1;
				if (m.isRepetitive()) {
					synchronized (asks_for_external_ip) {
						asks_for_external_ip.add(m);
						if (external_address_task_updater != null
								&& external_address_task_updater.getDurationBetweenEachRepetitionInMilliSeconds() > m.getDelayInMs()) {
							UpnpIGDAgent.this.cancelTask(external_address_task_id, false);
							external_address_task_id = null;
							external_address_task_updater = null;
						}
						if (external_address_task_updater == null) {
							update_repetitive_task = true;
							referenced_delay = m.getDelayInMs();
						}
					}
				} else {
					synchronized (asks_for_external_ip) {
						long delay = -1;
						Iterator<AskForExternalIPMessage> it = asks_for_external_ip.iterator();
						while (it.hasNext()) {
							AskForExternalIPMessage tmpm = it.next();
							if (tmpm.getSender().equals(m.getSender())) {
								delay = tmpm.getDelayInMs();
								it.remove();
								cancel = true;
								break;
							}
						}

						if (cancel) {
							if (delay == external_address_task_updater.getDurationBetweenEachRepetitionInMilliSeconds()
									|| asks_for_external_ip.size() == 0) {
								UpnpIGDAgent.this.cancelTask(external_address_task_id, false);
								external_address_task_id = null;
								external_address_task_updater = null;
							}
							if (asks_for_external_ip.size() > 0) {
								for (AskForExternalIPMessage tmpm : asks_for_external_ip) {
									if (tmpm.getDelayInMs() > referenced_delay)
										referenced_delay = tmpm.getDelayInMs();
								}
								update_repetitive_task = true;
							}
						}
					}
				}
				if (update_repetitive_task) {
					external_address_task_updater = new Task<Void>(System.currentTimeMillis() + referenced_delay, referenced_delay) {
						@Override
						public Void call()  {
							upnpService.getControlPoint().execute(new GetExternalIP(service) {

								@Override
								public void failure(ActionInvocation _invocation,
													UpnpResponse _operation, String _defaultMsg) {
									if (getLogger1() != null)
										getLogger1().warning(_defaultMsg);
								}

								@Override
								protected void success(String _externalIPAddress) {
									try {
										InetAddress old = external_address.get();
										InetAddress newAddress = InetAddress.getByName(_externalIPAddress);
										if (old == null || !old.equals(newAddress)) {
											ExternalIPMessage answer = new ExternalIPMessage(internal_address,
													newAddress, old);
											external_address.set(newAddress);
											synchronized (asks_for_external_ip) {
												asks_for_external_ip.removeIf(m1 -> !UpnpIGDAgent.this.sendReply(m1, answer.clone())
														.equals(ReturnCode.SUCCESS));
											}
										}
									} catch (Exception e) {
										if (getLogger1() != null) {
											getLogger1().severeLog("Unexpected exception :", e);
										}
									}
								}

							});
							return null;
						}
					};
					external_address_task_id = UpnpIGDAgent.this.scheduleTask(external_address_task_updater);
				}

				upnpService.getControlPoint().execute(new GetExternalIP(service) {

					@Override
					public void failure(ActionInvocation _invocation,
							UpnpResponse _operation, String _defaultMsg) {

						if (getLogger1() != null)
							getLogger1().warning(_defaultMsg);
						ExternalIPMessage answer = new ExternalIPMessage(internal_address, null,
								external_address.get());
						answer.setMessage(_defaultMsg);
						UpnpIGDAgent.this.sendReply(m, answer);
					}

					@Override
					protected void success(String _externalIPAddress) {
						try {
							UpnpIGDAgent.this.sendReply(m, new ExternalIPMessage(internal_address,
									InetAddress.getByName(_externalIPAddress), null));
						} catch (Exception e) {
							if (getLogger1() != null) {
								getLogger1().severeLog("Unexpected exception :", e);
							}
						}
					}
				});
			}
		}

		public void newMessage(final AskForPortMappingAddMessage m) {
			if (UpnpIGDAgent.upnpService != null && !removed.get()) {
				synchronized (desired_mappings) {
					for (PortMapping pm : desired_mappings) {
						if (pm.getInternalPort().getValue() == m.getInternalPort()) {
							for (int i : m.getExternalPortsRange()) {
								if (i == pm.getExternalPort().getValue()) {
									UpnpIGDAgent.this.sendReply(m,
											new PortMappingAnswerMessage(m.getConcernedRouter(),
													i, m.getInternalPort(),
													m.getDescription(), m.getProtocol(), MappingReturnCode.SUCCESS));
									return;
								}
							}
						}
					}
				}
				final AtomicInteger index = new AtomicInteger(0);
				final PortMapping pm = new PortMapping();
				pm.setDescription(m.getDescription());
				pm.setEnabled(true);
				pm.setExternalPort(new UnsignedIntegerTwoBytes(m.getExternalPortsRange()[index.getAndIncrement()]));
				pm.setInternalPort(new UnsignedIntegerTwoBytes(m.getInternalPort()));
				pm.setProtocol(m.getProtocol());
				pm.setInternalClient(m.getConcernedLocalAddress().getHostAddress());

				upnpService.getControlPoint().execute(new PortMappingAdd(service, pm) {

					@Override
					public void success(ActionInvocation _invocation) {
						synchronized (desired_mappings) {
							desired_mappings.add(pm);
						}
						UpnpIGDAgent.this.sendReply(m,
								new PortMappingAnswerMessage(m.getConcernedRouter(),
										m.getExternalPortsRange()[index.get() - 1], m.getInternalPort(),
										m.getDescription(), m.getProtocol(), MappingReturnCode.SUCCESS));
					}

					@Override
					public void failure(ActionInvocation _invocation,
							UpnpResponse _operation, String _defaultMsg) {
						if (index.get() < m.getExternalPortsRange().length) {
							pm.setExternalPort(
									new UnsignedIntegerTwoBytes(m.getExternalPortsRange()[index.getAndIncrement()]));
							upnpService.getControlPoint().execute(this);
						} else {
							if (_defaultMsg.toLowerCase().contains("authorized"))
								UpnpIGDAgent.this.sendReply(m,
										new PortMappingAnswerMessage(m.getConcernedRouter(),
												-1, m.getInternalPort(),
												m.getDescription(), _defaultMsg, m.getProtocol(), MappingReturnCode.ACCESS_DENIED));
							else
								UpnpIGDAgent.this.sendReply(m, new PortMappingAnswerMessage(m.getConcernedRouter(),
										-1, m.getInternalPort(), m.getDescription(),_defaultMsg,
										m.getProtocol(), MappingReturnCode.CONFLICTUAL_PORT_AND_IP));
						}

					}
				});
			}
		}

		private void removeAllPointMapping() {
			if (UpnpIGDAgent.upnpService != null) {
				synchronized (desired_mappings) {

					for (final PortMapping pm : desired_mappings) {
						upnpService.getControlPoint().execute(new PortMappingDelete(service, pm) {

							@Override
							public void success(ActionInvocation _invocation) {
								synchronized (desired_mappings) {
									desired_mappings.remove(pm);
								}
								
							}

							@Override
							public void failure(ActionInvocation _invocation,
									UpnpResponse _operation, String _defaultMsg) {
								handleFailureMessage("Impossible to remove port mapping : " + _defaultMsg);
							}
						});
					}
					desired_mappings.clear();
				}
			}

		}

		public void newMessage(final AskForPortMappingDeleteMessage m) {

			if (UpnpIGDAgent.upnpService != null && !removed.get()) {
				PortMapping pmfound = null;

				synchronized (desired_mappings) {

					for (PortMapping pm : desired_mappings) {
						if (pm.getExternalPort().getValue() == m.getExternalPort()
								&& pm.getProtocol().equals(m.getProtocol())) {
							pmfound = pm;
							break;
						}
					}
				}
				if (pmfound != null) {
					final PortMapping pm = pmfound;

					upnpService.getControlPoint().execute(new PortMappingDelete(service, pm) {

						@Override
						public void success(ActionInvocation _invocation) {
							synchronized (desired_mappings) {
								desired_mappings.remove(pm);
							}
							try
							{
								UpnpIGDAgent.this.sendReply(m,
									new PortMappingAnswerMessage(m.getConcernedRouter(),
											m.getExternalPort(), pm.getInternalPort().getValue().intValue(),
											null, m.getProtocol(), MappingReturnCode.REMOVED));
							}
							catch (Exception e) {
								if (getLogger1() != null) {
									getLogger1().severeLog("Unexpected exception :", e);
								}
							}
						}

						@Override
						public void failure(ActionInvocation _invocation,
								UpnpResponse _operation, String _defaultMsg) {
							handleFailureMessage("Impossible to remove port mapping : " + _defaultMsg);
							try
							{
								UpnpIGDAgent.this.sendReply(m,
									new PortMappingAnswerMessage(m.getConcernedRouter(),
											m.getExternalPort(), pm.getInternalPort().getValue().intValue(),
											_defaultMsg, m.getProtocol(), MappingReturnCode.UNKNOWN));
							}
							catch (Exception e) {
								if (getLogger1() != null) {
									getLogger1().severeLog("Unexpected exception :", e);
								}
							}
						}
					});
				}
			}
		}

		public boolean networkInterfaceRemoved(NetworkInterface ni) {
			for (InterfaceAddress ia : ni.getInterfaceAddresses())
			{
				if ((ia.getAddress() instanceof Inet4Address && this.internal_address instanceof Inet4Address)
					||
						(ia.getAddress() instanceof Inet6Address && this.internal_address instanceof Inet6Address))
					if (InetAddressFilter.isSameLocalNetwork(ia.getAddress().getAddress(), this.internal_address.getAddress(), ia.getNetworkPrefixLength()))
						return true;
			}
			return false;

		}


	}

	private UpnpIGDAgent() {
		super();
		// setName("UpnpIGDAgent");
	}

	/**
	 * Returns an instance of UpnpIGDAgent.
	 * 
	 * @return an instance of UpnpIGDAgent.
	 */
	public static UpnpIGDAgent getInstance() {
		return new UpnpIGDAgent();
	}

	private void networkInterfaceRemoved(NetworkInterface ni)
	{
		ArrayList<InetAddress> removed=new ArrayList<>();
		for (Router r : this.upnp_igd_routers.values())
		{
			if (r.networkInterfaceRemoved(ni))
				removed.add(r.internal_address);
		}
		for (InetAddress ia : removed) {
			removeRouter(ia, false);
		}
	}

	protected void activate() {
		setLogLevel(getMadkitConfig().networkProperties.upnpIGDLogLevel);
		// loggerModified(logger);
		if (logger != null && logger.isLoggable(Level.FINE))
			logger.fine("Launching UPNPIGDAgent ...");
		requestRole(LocalCommunity.Groups.NETWORK, LocalCommunity.Roles.LOCAL_NETWORK_EXPLORER_ROLE);
		if (getMadkitConfig().networkProperties.upnpIGDEnabled) {
			synchronized (UpnpIGDAgent.class) {
				if (upnpService == null) {
					upnpService = new UpnpServiceImpl(new DefaultUpnpServiceConfiguration(getMadkitConfig().networkProperties.upnpStreamIDGPort, getMadkitConfig().networkProperties.upnpMulticastIDGPort))
					{
						private RetrieveRemoteDescriptors newRetrieveRemoteDescriptorsInstance(RemoteDevice rd)
						{
							return new RetrieveRemoteDescriptors(this, rd)
							{
								@Override
								protected void describe(String descriptorXML) throws RouterException {

									boolean notifiedStart = false;
									RemoteDevice describedDevice = null;
									try {

										DeviceDescriptorBinder deviceDescriptorBinder =
												getUpnpService().getConfiguration().getDeviceDescriptorBinderUDA10();

										describedDevice = deviceDescriptorBinder.describe(
												rd,
												descriptorXML
										);
										if (describedDevice==null)
											return;

										notifiedStart = getUpnpService().getRegistry().notifyDiscoveryStart(describedDevice);

										RemoteDevice hydratedDevice = describeServices(describedDevice);
										if (hydratedDevice == null) {
											if(!errorsAlreadyLogged.contains(rd.getIdentity().getUdn())) {
												errorsAlreadyLogged.add(rd.getIdentity().getUdn());
											}
											if (notifiedStart)
												getUpnpService().getRegistry().notifyDiscoveryFailure(
														describedDevice,
														new DescriptorBindingException("Device service description failed: " + rd)
												);
										} else {
											// The registry will do the right thing: A new root device is going to be added, if it's
											// already present or we just received the descriptor again (because we got an embedded
											// devices' notification), it will simply update the expiration timestamp of the root
											// device.
											getUpnpService().getRegistry().addDevice(hydratedDevice);
										}

									} catch (ValidationException ex) {
										// Avoid error log spam each time device is discovered, errors are logged once per device.
										if(!errorsAlreadyLogged.contains(rd.getIdentity().getUdn())) {
											errorsAlreadyLogged.add(rd.getIdentity().getUdn());
											if (describedDevice != null && notifiedStart)
												getUpnpService().getRegistry().notifyDiscoveryFailure(describedDevice, ex);
										}

									} catch (DescriptorBindingException | RegistrationException ex) {
										if (describedDevice != null && notifiedStart)
											getUpnpService().getRegistry().notifyDiscoveryFailure(describedDevice, ex);

									}
								}
							};
						}

						@Override
						protected Registry createRegistry(ProtocolFactory protocolFactory) {


							return new RegistryImpl(this)
							{
								@Override
								synchronized public boolean notifyDiscoveryStart(final RemoteDevice device) {
									// Exit if we have it already, this is atomic inside this method, finally
									if (device==null)
										return false;
									if (device.getIdentity()!=null && getUpnpService()!=null && getUpnpService().getRegistry()!=null && getUpnpService().getRegistry().getRemoteDevice(device.getIdentity().getUdn(), true) != null) {
										return false;
									}
									RegistryImpl This=this;
									for (final org.fourthline.cling.registry.RegistryListener listener : getListeners()) {
										getConfiguration().getRegistryListenerExecutor().execute(
												() -> listener.remoteDeviceDiscoveryStarted(This, device)
										);
									}
									return true;
								}
							};
						}

						@Override
						protected ProtocolFactory createProtocolFactory() {
							return new ProtocolFactoryImpl(this)
							{
								@Override
								protected ReceivingSearchResponse createReceivingSearchResponse(IncomingDatagramMessage<UpnpResponse> incomingResponse) {
									return new ReceivingSearchResponse(getUpnpService(), incomingResponse)
									{
										@Override
										protected void execute() {
											if (!getInputMessage().isSearchResponseMessage()) {
												return;
											}

											UDN udn = getInputMessage().getRootDeviceUDN();
											if (udn == null) {
												return;
											}

											RemoteDeviceIdentity rdIdentity = new RemoteDeviceIdentity(getInputMessage());

											if (getUpnpService().getRegistry().update(rdIdentity)) {
												return;
											}

											RemoteDevice rd;
											try {
												rd = new RemoteDevice(rdIdentity);
											} catch (ValidationException ex) {
												ex.printStackTrace();
												return;
											}

											if (rdIdentity.getDescriptorURL() == null) {
												return;
											}

											if (rdIdentity.getMaxAgeSeconds() == null) {
												return;
											}

											// Unfortunately, we always have to retrieve the descriptor because at this point we
											// have no idea if it's a root or embedded device
											getUpnpService().getConfiguration().getAsyncProtocolExecutor().execute(
													newRetrieveRemoteDescriptorsInstance(rd)
											);
										}
									};
								}

								@Override
								protected ReceivingNotification createReceivingNotification(IncomingDatagramMessage<UpnpRequest> incomingRequest) {

									return new ReceivingNotification(getUpnpService(), incomingRequest)
									{
										@Override
										protected void execute() {

											UDN udn = getInputMessage().getUDN();
											if (udn == null) {
												return;
											}

											RemoteDeviceIdentity rdIdentity = new RemoteDeviceIdentity(getInputMessage());

											RemoteDevice rd;
											try {
												rd = new RemoteDevice(rdIdentity);
											} catch (ValidationException ex) {
												ex.printStackTrace();
												return;
											}

											if (getInputMessage().isAliveMessage()) {


												if (rdIdentity.getDescriptorURL() == null) {
													return;
												}

												if (rdIdentity.getMaxAgeSeconds() == null) {
													return;
												}

												if (getUpnpService().getRegistry().update(rdIdentity)) {
													return;
												}

												// Unfortunately, we always have to retrieve the descriptor because at this point we
												// have no idea if it's a root or embedded device
												getUpnpService().getConfiguration().getAsyncProtocolExecutor().execute(
														newRetrieveRemoteDescriptorsInstance(rd)
												);

											} else if (getInputMessage().isByeByeMessage()) {

												getUpnpService().getRegistry().removeDevice(rd);

											}

										}
									};
								}
							};
						}
					};

				}

				pointedUpnpServiceNumber++;
				upnpService.getRegistry().addListener(registryListener);
				for (RemoteDevice d : upnpService.getRegistry().getRemoteDevices()) {
					remoteDeviceDetected(d);
				}
				if (pointedUpnpServiceNumber == 1) {
					upnpService.getControlPoint().search();
				}

			}
		}
		if (logger != null && logger.isLoggable(Level.FINE))
			logger.fine("UPNPIGDAgent launched !");
	}

	@Override
	protected void loggerModified(AgentLogger logger) {

		if (logger == null) {
			for (String ls : sub_loggers_names) {
				Logger log = Logger.getLogger(ls);
				log.setParent(Logger.getGlobal());
				log.setUseParentHandlers(false);
				log.setLevel(Level.OFF);
			}
		} else {
			for (String ls : sub_loggers_names) {
				Logger log = Logger.getLogger(ls);
				log.setParent(logger);
				log.setUseParentHandlers(true);
				log.setLevel(logger.getLevel());
			}
		}

	}

	@Override
	protected void end() {
		removeAllRouters(true);
		network_interface_info.shutdown();
		if (getMadkitConfig().networkProperties.upnpIGDEnabled && upnpService != null) {
			synchronized (UpnpIGDAgent.class) {
				upnpService.getRegistry().removeListener(registryListener);
				if (--pointedUpnpServiceNumber == 0) {

					try {
						sleep(2000);
					} catch (InterruptedException e) {
						if (logger != null)
							logger.severeLog("", e);
						else
							e.printStackTrace();
					}
					upnpService.shutdown();
					upnpService = null;
				}
			}
		}
		if (logger != null)
			logger.fine("UPNPIGDAgent killed !");

	}



	protected class NetworkInterfaceInfo {

		ArrayList<NetworkInterface> network_interfaces = new ArrayList<>();
		long minNanoDelay = -1;
		HashMap<AgentAddress, AskForNetworkInterfacesMessage> callers = new HashMap<>();
		TaskID task_id = null;
		Task<Void> task = null;
		private boolean shutdown = false;

		ArrayList<NetworkInterface> init() {

			try {
				Enumeration<NetworkInterface> e = NetworkInterface.getNetworkInterfaces();
				ArrayList<NetworkInterface> network_interfaces = new ArrayList<>();
				while (e.hasMoreElements()) {
					NetworkInterface ni = e.nextElement();
					if (isValid(ni)) {
						network_interfaces.add(ni);
					}
				}
				return network_interfaces;
			} catch (SocketException ignored) {
			}
			return new ArrayList<>();
		}

		boolean isValid(NetworkInterface ni) throws SocketException {
			return InetAddressFilter.isValidNetworkInterface(ni);
		}

		void addCaller(AskForNetworkInterfacesMessage _message) {
			if (!getMadkitConfig().networkProperties.networkInterfaceScan)
				return;
			synchronized (this) {
				if (shutdown)
					return;
				boolean removed = false;
				final long oldNanoDelay = minNanoDelay;
				if (callers.remove(_message.getSender()) != null) {
					minNanoDelay = -1;
					removed = true;
				}
				if (_message.isRepetitive()) {
					callers.put(_message.getSender(), _message);
					minNanoDelay = -1;
				}
				if (minNanoDelay == -1 && callers.size() > 0) {
					minNanoDelay = Long.MAX_VALUE;

					for (AskForNetworkInterfacesMessage afni : callers.values()) {
						if (minNanoDelay > afni.getDelayIsMs()*1000000L)
							minNanoDelay = afni.getDelayIsMs()*1000000L;
					}
					if (minNanoDelay < 0)
						minNanoDelay = -1;
				}
				if (oldNanoDelay == -1 || oldNanoDelay > minNanoDelay) {
					if (oldNanoDelay != -1) {
						cancelTask(task_id, false);
						task_id = null;
					}

					if (minNanoDelay != -1) {

						task = new Task<Void>((oldNanoDelay != -1 ? oldNanoDelay : 0)/1000000L+System.currentTimeMillis(), minNanoDelay/1000000L)
						{
							long oldNanoTime=System.nanoTime()+(oldNanoDelay!=-1?oldNanoDelay:0);
							final long maxDelayInMsBeforeDetectingOSWakeUp =minNanoDelay*2;//Math.min(min_delay*2, getMadkitConfig().networkProperties.connectionTimeOut);
							@Override
							public Void call() {


								boolean scanRouters=false;
								synchronized (UpnpIGDAgent.NetworkInterfaceInfo.this) {
									ArrayList<NetworkInterface> cur_nis = init();
									ArrayList<NetworkInterface> new_nis = new ArrayList<>();
									ArrayList<NetworkInterface> del_nis = new ArrayList<>();
									long newNanoTime=System.nanoTime();
									if (newNanoTime>oldNanoTime+ maxDelayInMsBeforeDetectingOSWakeUp*1000000L) {//detect OS wake up
										del_nis.addAll(network_interfaces);

										if (del_nis.size() > 0) {
											for (NetworkInterface ni : del_nis)
											{
												networkInterfaceRemoved(ni);
											}
											network_interfaces = new ArrayList<>();
											for (Iterator<AskForNetworkInterfacesMessage> it = callers.values()
													.iterator(); it.hasNext(); ) {
												AskForNetworkInterfacesMessage m = it.next();
												if (!sendReply(m, new NetworkInterfaceInformationMessage(
														new_nis, del_nis)).equals(ReturnCode.SUCCESS))
													it.remove();
											}
											del_nis = new ArrayList<>();
										}
										scanRouters=true;
										try {
											sleep(1000);
										} catch (InterruptedException e) {
											e.printStackTrace();
										}
									}
									oldNanoTime = newNanoTime;

									for (NetworkInterface ni : network_interfaces) {
										boolean found = false;
										for (NetworkInterface ni2 : cur_nis) {
											if (ni.equals(ni2)) {
												found = true;
												break;
											}
										}
										if (!found)
											del_nis.add(ni);
									}

									for (NetworkInterface ni : cur_nis) {
										boolean found = false;
										for (NetworkInterface ni2 : network_interfaces) {
											if (ni.equals(ni2)) {
												found = true;
												break;
											}
										}
										if (!found)
											new_nis.add(ni);
									}

									network_interfaces = cur_nis;
									if (new_nis.size() != 0 || del_nis.size() != 0) {
										for (Iterator<AskForNetworkInterfacesMessage> it = callers.values()
												.iterator(); it.hasNext(); ) {
											AskForNetworkInterfacesMessage m = it.next();
											if (!sendReply(m, new NetworkInterfaceInformationMessage(
													new_nis, del_nis)).equals(ReturnCode.SUCCESS))
												it.remove();

										}
									}
									if (new_nis.size()>0 && scanRouters && upnpService!=null) {

										ControlPoint cp=upnpService.getControlPoint();
										if (cp!=null) {
											cp.search();
											for (RemoteDevice d : upnpService.getRegistry().getRemoteDevices()) {
												remoteDeviceDetected(d);
											}
										}
									}
									return null;
								}
							}
						};
						task_id = scheduleTask(task);
					}
				}

				if (_message.isRepetitive()) {
					UpnpIGDAgent.this.sendReply(_message, new NetworkInterfaceInformationMessage(
							network_interfaces, new ArrayList<>()));
				} else if (!removed) {
					Collection<NetworkInterface> c = init();
					UpnpIGDAgent.this.sendReply(_message,
							new NetworkInterfaceInformationMessage(c, new ArrayList<>()));
				}
			}
		}

		void shutdown() {
			synchronized (this) {
				if (task_id != null)
					cancelTask(this.task_id, false);
				this.callers.clear();
				this.minNanoDelay = -1;
				this.task = null;
				this.task_id = null;
				shutdown = true;
			}
		}
	}

	private final NetworkInterfaceInfo network_interface_info = new NetworkInterfaceInfo();

	AgentLogger getLogger1() {
		return logger;
	}

	@SuppressWarnings("unused")
	public void stopNetwork() {
		if (this.getState().compareTo(State.ENDING) < 0)
			this.killAgent(this);
	}

	@Override
	protected void liveByStep(Message _message) {
		if (_message == null)
			return;
		if (_message.getClass() == KernelMessage.class) {
			proceedEnumMessage((KernelMessage) _message);
		} else if (_message instanceof UpnpIGDAgent.AskForConnectionStatusMessage) {
			AskForConnectionStatusMessage m = (AskForConnectionStatusMessage) _message;

			Router r;
			synchronized (upnp_igd_routers) {
				r = upnp_igd_routers.get(m.getConcernedRouter());
			}
			if (r != null) {
				if (logger != null && logger.isLoggable(Level.FINER))
					logger.finer("Managing message : " + m);
				r.newMessage(m);
			} else {
				handleFailureMessage("Trying to ask connection status for a router which does not exists : " + m);
				sendReply(m, new ConnexionStatusMessage(m.getConcernedRouter(),
						new StatusInfo(Status.Unconfigured, 0, null), new StatusInfo(Status.Unconfigured, 0, null)));
			}
		} else if (_message instanceof UpnpIGDAgent.AskForExternalIPMessage) {
			AskForExternalIPMessage m = (AskForExternalIPMessage) _message;
			Router r;
			synchronized (upnp_igd_routers) {
				r = upnp_igd_routers.get(m.getConcernedRouter());
			}
			if (r != null) {
				if (logger != null && logger.isLoggable(Level.FINER))
					logger.finer("Managing message : " + m);
				r.newMessage(m);
			} else {
				handleFailureMessage(
						"Trying to ask external ip message considering a router which does not exists : " + m);
			}
		} else if (_message instanceof UpnpIGDAgent.AskForNetworkInterfacesMessage) {
			if (logger != null && logger.isLoggable(Level.FINER))
				logger.finer("Managing message : " + _message);

			network_interface_info.addCaller((UpnpIGDAgent.AskForNetworkInterfacesMessage) _message);
		} else if (_message instanceof UpnpIGDAgent.AskForPortMappingAddMessage) {
			AskForPortMappingAddMessage m = (AskForPortMappingAddMessage) _message;
			Router r;
			synchronized (upnp_igd_routers) {
				r = upnp_igd_routers.get(m.getConcernedRouter());
			}
			if (r != null) {
				if (logger != null && logger.isLoggable(Level.FINER))
					logger.finer("Managing message : " + m);

				r.newMessage(m);
			} else {
				handleFailureMessage("Trying to add a port mapping considering a router which does not exists : " + m);
				sendReply(m, new PortMappingAnswerMessage(m.getConcernedRouter(), -1,
						m.getInternalPort(), m.getDescription(), m.getProtocol(), MappingReturnCode.UNKNOWN));
			}

		} else if (_message instanceof UpnpIGDAgent.AskForPortMappingDeleteMessage) {
			AskForPortMappingDeleteMessage m = (AskForPortMappingDeleteMessage) _message;
			Router r;
			synchronized (upnp_igd_routers) {
				r = upnp_igd_routers.get(m.getConcernedRouter());
			}
			if (r != null) {
				if (logger != null && logger.isLoggable(Level.FINER))
					logger.finer("Managing message : " + m);

				r.newMessage(m);
			} else {
				handleFailureMessage(
						"Trying to remove a port mapping considering a router which does not exists : " + m);
			}
		} else if (_message instanceof AskForRouterDetectionInformation) {
			synchronized (upnp_igd_routers) {
				AskForRouterDetectionInformation m = (AskForRouterDetectionInformation) _message;
				if (logger != null && logger.isLoggable(Level.FINER))
					logger.finer("Managing message : " + m);

				for (Router r : upnp_igd_routers.values())
					sendReply(m, new IGDRouterFoundMessage(r.internal_address));

				if (m.permanent_request) {
					boolean found = false;
					for (AskForRouterDetectionInformation m2 : callers_for_router_detection) {
						if (m2.getSender().equals(m.getSender())) {
							found = true;
							break;
						}
					}
					if (!found)
						callers_for_router_detection.add(m);
				} else {
					callers_for_router_detection.removeIf(askForRouterDetectionInformation -> askForRouterDetectionInformation.getSender().equals(m.getSender()));
				}
			}
		} else if (_message instanceof NetworkAgent.StopNetworkMessage) {
			this.killAgent(this);
		}
	}

	protected void handleFailureMessage(String s) {
		if (logger != null)
			logger.warning(s);
	}

	protected static final String IGD = "InternetGatewayDevice";
	protected static final DeviceType CONNECTION_DEVICE_TYPE = new UDADeviceType("WANConnectionDevice", 1);

	protected static final ServiceType IP_SERVICE_TYPE = new UDAServiceType("WANIPConnection", 1);
	protected static final ServiceType IP_SERVICE_TYPE_BIS = new UDAServiceType("WANIPConn", 1);
	protected static final ServiceType PPP_SERVICE_TYPE = new UDAServiceType("WANPPPConnection", 1);
	protected static final ServiceType IP_SERVICE_TYPE2 = new UDAServiceType("WANIPConnection", 2);
	protected static final ServiceType IP_SERVICE_TYPE_BIS2 = new UDAServiceType("WANIPConn", 2);
	protected static final ServiceType PPP_SERVICE_TYPE2 = new UDAServiceType("WANPPPConnection", 2);

	protected class RegistryListener extends DefaultRegistryListener {

		public RegistryListener() {
			super();
		}

		@Override
		public void remoteDeviceUpdated(Registry registry, RemoteDevice device) {

			updateRouter(device);
		}

		@Override
		public void remoteDeviceAdded(Registry registry, RemoteDevice device) {
			remoteDeviceDetected(device);
		}

		@Override
		public void remoteDeviceRemoved(Registry registry, RemoteDevice device) {
			if (device != null) {

				try {
					InetAddress ia = InetAddress.getByName(device.getIdentity().getDescriptorURL().getHost());
					removeRouter(ia, false);
				} catch (UnknownHostException e) {
					if (getLogger1() != null)
						getLogger1().severeLog("Device removed but impossible to access to its ip address : ", e);
					else
						e.printStackTrace();
				}
			}
		}

		@Override
		synchronized public void beforeShutdown(Registry registry) {
			removeAllRouters(false);
		}

	}

	enum MappingReturnCode {
		SUCCESS, CONFLICTUAL_PORT_AND_IP, ACCESS_DENIED, UNKNOWN, REMOVED
	}

	public static abstract class AbstractRouterMessage extends Message {

		private final InetAddress concerned_router;
		private String message;

		protected AbstractRouterMessage(InetAddress _concerned_router) {
			if (_concerned_router == null)
				throw new NullPointerException("_concerned_router");
			concerned_router = _concerned_router;
		}

		public InetAddress getConcernedRouter() {
			return concerned_router;
		}

		protected void setMessage(String _message) {
			message = _message;
		}

		public String getMessage() {
			return message;
		}

		@Override
		public String toString() {
			return getClass().getSimpleName() + "[concernedRouter=" + concerned_router + ", message=" + message + "]";
		}

	}

	public static class AskForPortMappingAddMessage extends AbstractRouterMessage {

		private final InetAddress concerned_local_ip;
		private final int[] external_ports_range;
		private final int internal_port;
		private final String description;
		private final Protocol protocol;

		@Override
		public String toString() {
			return getClass().getSimpleName() + "[concernedRouter=" + getConcernedRouter() + ", message=" + getMessage()
					+ ", concerned_local_ip" + concerned_local_ip + ", internalPort=" + internal_port + ", protocol="
					+ protocol + ", externalPortRange=" + Arrays.toString(external_ports_range) + "]";
		}

		public AskForPortMappingAddMessage(InetAddress _concerned_router, InetAddress _concerned_local_ip,
				List<Integer> external_ports_range, int _internal_port, String _description, Protocol _protocol) {
			super(_concerned_router);
			if (_concerned_local_ip == null)
				throw new NullPointerException("_concerned_local_ip");

			concerned_local_ip = _concerned_local_ip;
			if (external_ports_range == null)
				throw new NullPointerException("external_ports_range");
			if (external_ports_range.isEmpty())
				throw new IllegalArgumentException("external_ports_range is empty");
			int nb = 0;
			for (Integer i : external_ports_range) {
				if (i != null)
					nb++;
			}
			if (nb == 0)
				throw new IllegalArgumentException("external_ports_range is empty or has no valid port");
			this.external_ports_range = new int[nb];
			int index = 0;
			for (Integer i : external_ports_range) {
				if (i != null)
					this.external_ports_range[index++] = i;
			}

			internal_port = _internal_port;
			description = _description;
			protocol = _protocol;
		}

		public int[] getExternalPortsRange() {
			return external_ports_range;
		}

		public InetAddress getConcernedLocalAddress() {
			return concerned_local_ip;
		}

		public int getInternalPort() {
			return internal_port;
		}

		public String getDescription() {
			return description;
		}

		public Protocol getProtocol() {
			return protocol;
		}

	}

	public static class AskForPortMappingDeleteMessage extends AbstractRouterMessage {

		private final int external_port;
		private final Protocol protocol;

		public AskForPortMappingDeleteMessage(InetAddress _concerned_router, int _external_port, Protocol _protocol) {
			super(_concerned_router);
			external_port = _external_port;
			protocol = _protocol;
		}

		public int getExternalPort() {
			return external_port;
		}

		public Protocol getProtocol() {
			return protocol;
		}

		@Override
		public String toString() {
			return getClass().getSimpleName() + "[concernedRouter=" + getConcernedRouter() + ", message=" + getMessage()
					+ ", protocol=" + protocol + ", externalPort=" + external_port + "]";
		}

	}

	public static class PortMappingAnswerMessage extends AbstractRouterMessage {

		private final int external_port;
		private final int internal_port;
		private final String description;
		private final MappingReturnCode return_code;
		private final Protocol protocol;

		public PortMappingAnswerMessage(InetAddress _concerned_router,
										int _external_port, int _internal_port, String _description, Protocol _protocol,
										MappingReturnCode _return_code) {
			this(_concerned_router, _external_port, _internal_port, _description, null, _protocol, _return_code);
		}
			
		public PortMappingAnswerMessage(InetAddress _concerned_router,
										int _external_port, int _internal_port, String _description, String message, Protocol _protocol,
										MappingReturnCode _return_code) {
			super(_concerned_router);
			external_port = _external_port;
			internal_port = _internal_port;
			description = _description;
			return_code = _return_code;
			protocol = _protocol;
			this.setMessage(message);
		}

		public int getInternalPort() {
			return internal_port;
		}

		public int getExternalPort() {
			return external_port;
		}

		public String getDescription() {
			return description;
		}

		public MappingReturnCode getReturnCode() {
			return return_code;
		}

		public Protocol getProtocol() {
			return protocol;
		}
	}

	public static abstract class RepetitiveRouterRequest extends AbstractRouterMessage {

		private final long delayInMs;

		protected RepetitiveRouterRequest(InetAddress _concerned_router, long _delay_between_each_check) {
			super(_concerned_router);
			delayInMs = _delay_between_each_check;
		}

		public long getDelayInMs() {
			return delayInMs;
		}

		public boolean isRepetitive() {
			return delayInMs > 0;
		}

		@Override
		public String toString() {
			return getClass().getSimpleName() + "[concernedRouter=" + getConcernedRouter() + ", delay=" + delayInMs + "]";
		}
	}

	public static class AskForConnectionStatusMessage extends RepetitiveRouterRequest {

		public AskForConnectionStatusMessage(InetAddress _concerned_router, long _delay_between_each_check) {
			super(_concerned_router, _delay_between_each_check);
		}

	}

	public static class ConnexionStatusMessage extends AbstractRouterMessage {

		private final StatusInfo status;
		private final StatusInfo old_status;

		public ConnexionStatusMessage(InetAddress _concerned_router, StatusInfo _status, StatusInfo _old_status) {
			super(_concerned_router);
			status = _status;
			old_status = _old_status;
		}

		public StatusInfo getStatus() {
			return status;
		}

		@Override
		public String toString() {
			return getClass().getSimpleName() + "[concernedRouter=" + getConcernedRouter() + ", message=" + getMessage()
					+ ", status=" + status + ", oldStatus=" + old_status + "]";
		}

	}

	public static class AskForExternalIPMessage extends RepetitiveRouterRequest {

		public AskForExternalIPMessage(InetAddress _concerned_router, long _delay_between_each_check) {
			super(_concerned_router, _delay_between_each_check);
		}
	}

	public static class ExternalIPMessage extends AbstractRouterMessage {

		private final InetAddress external_ip, old_ip;

		public ExternalIPMessage(InetAddress _concerned_router, InetAddress _external_ip, InetAddress _old_ip) {
			super(_concerned_router);
			external_ip = _external_ip;
			old_ip = _old_ip;
		}

		public InetAddress getExternalIP() {
			return external_ip;
		}

		@Override
		public String toString() {
			return getClass().getSimpleName() + "[concernedRouter=" + getConcernedRouter() + ", message=" + getMessage()
					+ "externalIP=" + external_ip + ", oldIP=" + old_ip + "]";
		}

	}

	public static class AskForNetworkInterfacesMessage extends Message {

		private final long delayIsMs;

		public AskForNetworkInterfacesMessage(long _delay_between_each_check) {
			delayIsMs = _delay_between_each_check;
		}

		public long getDelayIsMs() {
			return delayIsMs;
		}

		public boolean isRepetitive() {
			return delayIsMs > 0;
		}

		@Override
		public String toString() {
			return "AskForNetworkInterfacesMessage[delay=" + delayIsMs + "]";
		}

	}

	public static class NetworkInterfaceInformationMessage extends Message {


		private final Collection<NetworkInterface> new_connected_interfaces;
		private final Collection<NetworkInterface> new_disconnected_interfaces;

		public NetworkInterfaceInformationMessage(Collection<NetworkInterface> _new_connected_interfaces,
												  Collection<NetworkInterface> _new_disconnected_interfaces) {
			new_connected_interfaces = _new_connected_interfaces;
			new_disconnected_interfaces = _new_disconnected_interfaces;
		}

		public Collection<NetworkInterface> getNewConnectedInterfaces() {
			return new_connected_interfaces;
		}

		public Collection<NetworkInterface> getNewDisconnectedInterfaces() {
			return new_disconnected_interfaces;
		}

		@Override
		public String toString() {
			return "NetworkInterfaceInformationMessage[new_connected_interfaces=" + new_connected_interfaces
					+ ", new_disconnected_interfaces=" + new_disconnected_interfaces + "]";
		}
	}

	/*
	 * protected class NewDeviceReceived extends Message { private static final long
	 * serialVersionUID = 1L;
	 * 
	 * final RemoteDevice device; final RemoteService service; final InetAddress
	 * local_ip;
	 * 
	 * NewDeviceReceived(RemoteDevice _device, RemoteService _service, InetAddress
	 * _local_ip) { device=_device; service=_service; local_ip=_local_ip; } }
	 * 
	 * protected class NewDeviceRemoved extends Message { private static final long
	 * serialVersionUID = 1L;
	 * 
	 * final RemoteDevice device;
	 * 
	 * NewDeviceRemoved(RemoteDevice _device) { device=_device; } }
	 */

	public static class AskForRouterDetectionInformation extends Message {

		final boolean permanent_request;

		public AskForRouterDetectionInformation(boolean permanent_request) {
			this.permanent_request = permanent_request;
		}

		@Override
		public String toString() {
			return "AskForRouterDetectionInformation[permanentRequest=" + permanent_request + "]";
		}
	}

	public static class IGDRouterFoundMessage extends AbstractRouterMessage {


		public IGDRouterFoundMessage(InetAddress _concerned_router) {
			super(_concerned_router);
		}
	}

	public static class IGDRouterLostMessage extends AbstractRouterMessage {

		protected IGDRouterLostMessage(InetAddress _concerned_router) {
			super(_concerned_router);
		}

	}

}

class NONAndroidUpnpServiceConfiguration extends org.fourthline.cling.DefaultUpnpServiceConfiguration {
	/**
	 * Defaults to port '0', ephemeral.
	 */
	private final int multicastPort;
	private NetworkAddressFactory networkAddressFactory=null;

	public NONAndroidUpnpServiceConfiguration(int streamListenPort, int multicastPort) {
		super(streamListenPort);
		this.multicastPort=multicastPort;
	}

	protected ExecutorService createDefaultExecutorService() {
		return UpnpIGDAgent.serviceExecutor;
	}
	
	@Override
    protected NetworkAddressFactory createNetworkAddressFactory(int streamListenPort) {
		return networkAddressFactory=UpnpIGDAgent.createNetworkAddressFactory(streamListenPort, NONAndroidUpnpServiceConfiguration.this.multicastPort);
    }

	/*
	 * FIX XXE issue : https://github.com/4thline/cling/issues/243
	 */
    @Override
	protected GENAEventProcessor createGENAEventProcessor() {
		return UpnpIGDAgent.createGENAEventProcessor();
	}
	/*
	 * FIX XXE issue : https://github.com/4thline/cling/issues/243
	 */
	@Override
	protected SOAPActionProcessor createSOAPActionProcessor() {
		return UpnpIGDAgent.createSOAPActionProcessor();
	}

	/*
	 * FIX XXE issue : https://github.com/4thline/cling/issues/243
	 */
	@Override
	public DeviceDescriptorBinder createDeviceDescriptorBinderUDA10() {
		return UpnpIGDAgent.createDeviceDescriptorBinderUDA10(networkAddressFactory);
	}

	/*
	 * FIX XXE issue : https://github.com/4thline/cling/issues/243
	 */
	@Override
	public ServiceDescriptorBinder createServiceDescriptorBinderUDA10() {
		return UpnpIGDAgent.createServiceDescriptorBinderUDA10(networkAddressFactory);
	}

	@Override
	public MulticastReceiver<?> createMulticastReceiver(NetworkAddressFactory networkAddressFactory) {
		return UpnpIGDAgent.createMulticastReceiver(networkAddressFactory);
	}
	@Override
	public DatagramIO<?> createDatagramIO(NetworkAddressFactory networkAddressFactory) {
		return UpnpIGDAgent.createDatagramIO(networkAddressFactory);
	}
	@Override
	public StreamServer<?> createStreamServer(NetworkAddressFactory networkAddressFactory) {
		return new StreamServerImpl(
				new StreamServerConfigurationImpl(
						networkAddressFactory.getStreamListenPort()
				)
		) {
			@Override
			synchronized public void init(InetAddress bindAddress, org.fourthline.cling.transport.Router router) throws InitializationException {
				try {
					InetSocketAddress socketAddress = new InetSocketAddress(bindAddress, configuration.getListenPort());

					server = HttpServer.create(socketAddress, configuration.getTcpConnectionBacklog());
					server.createContext("/", new UpnpIGDAgent.RequestHttpHandler(router, networkAddressFactory));

				} catch (Exception ex) {
					throw new InitializationException("Could not initialize " + getClass().getSimpleName() + ": " + ex, ex);
				}
			}
		};
	}
}

class AndroidUpnpServiceConfiguration extends org.fourthline.cling.android.AndroidUpnpServiceConfiguration {
	private final int multicastPort;
	private NetworkAddressFactory networkAddressFactory=null;

	public AndroidUpnpServiceConfiguration(int streamListenPort, int multicastPort) {
		super(streamListenPort);
		this.multicastPort=multicastPort;
	}

	protected ExecutorService createDefaultExecutorService() {
		return UpnpIGDAgent.serviceExecutor;
	}
	
	@Override
    protected NetworkAddressFactory createNetworkAddressFactory(int streamListenPort) {
		return networkAddressFactory=UpnpIGDAgent.createNetworkAddressFactory(streamListenPort, AndroidUpnpServiceConfiguration.this.multicastPort);
    }
	/*
	 * FIX XXE issue : https://github.com/4thline/cling/issues/243
	 */
	@Override
	protected GENAEventProcessor createGENAEventProcessor() {
		return UpnpIGDAgent.createGENAEventProcessor();
	}
	/*
	 * FIX XXE issue : https://github.com/4thline/cling/issues/243
	 */
	@Override
	protected SOAPActionProcessor createSOAPActionProcessor() {
		return UpnpIGDAgent.createSOAPActionProcessor();
	}

	/*
	 * FIX XXE issue : https://github.com/4thline/cling/issues/243
	 */
	@Override
	public DeviceDescriptorBinder createDeviceDescriptorBinderUDA10() {

		return UpnpIGDAgent.createDeviceDescriptorBinderUDA10(networkAddressFactory);
	}

	/*
	 * FIX XXE issue : https://github.com/4thline/cling/issues/243
	 */
	@Override
	public ServiceDescriptorBinder createServiceDescriptorBinderUDA10() {
		return UpnpIGDAgent.createServiceDescriptorBinderUDA10(networkAddressFactory);
	}
	@Override
	public MulticastReceiver<?> createMulticastReceiver(NetworkAddressFactory networkAddressFactory) {
		return UpnpIGDAgent.createMulticastReceiver(networkAddressFactory);
	}
	@Override
	public DatagramIO<?> createDatagramIO(NetworkAddressFactory networkAddressFactory) {
		return UpnpIGDAgent.createDatagramIO(networkAddressFactory);
	}

}

class DefaultUpnpServiceConfiguration implements org.fourthline.cling.UpnpServiceConfiguration {
	private final org.fourthline.cling.UpnpServiceConfiguration usc;

	/**
	 * Defaults to port '0', ephemeral.
	 */

	public DefaultUpnpServiceConfiguration(int streamListenPort, int multicastPort) {
		if (OSVersion.getCurrentOSVersion().getOS()==OS.ANDROID) {
			usc = new AndroidUpnpServiceConfiguration(streamListenPort, multicastPort);
		} else
			usc = new NONAndroidUpnpServiceConfiguration(streamListenPort, multicastPort);
	}

	@Override
	public NetworkAddressFactory createNetworkAddressFactory() {
		return usc.createNetworkAddressFactory();
	}

	@Override
	public DatagramProcessor getDatagramProcessor() {
		return usc.getDatagramProcessor();
	}

	@Override
	public SOAPActionProcessor getSoapActionProcessor() {
		return usc.getSoapActionProcessor();
	}

	@Override
	public GENAEventProcessor getGenaEventProcessor() {
		return usc.getGenaEventProcessor();
	}

	@Override
	public StreamClient<?> createStreamClient() {
		return usc.createStreamClient();
	}

	@Override
	public MulticastReceiver<?> createMulticastReceiver(NetworkAddressFactory _networkAddressFactory) {
		return usc.createMulticastReceiver(_networkAddressFactory);
	}

	@Override
	public DatagramIO<?> createDatagramIO(NetworkAddressFactory _networkAddressFactory) {
		return usc.createDatagramIO(_networkAddressFactory);
	}

	@Override
	public StreamServer<?> createStreamServer(NetworkAddressFactory _networkAddressFactory) {
		return null;
		//return usc.createStreamServer(_networkAddressFactory);
	}

	@Override
	public Executor getMulticastReceiverExecutor() {
		return usc.getMulticastReceiverExecutor();
	}

	@Override
	public Executor getDatagramIOExecutor() {
		return usc.getDatagramIOExecutor();
	}

	@Override
	public ExecutorService getStreamServerExecutorService() {
		return usc.getStreamServerExecutorService();
	}

	@Override
	public DeviceDescriptorBinder getDeviceDescriptorBinderUDA10() {
		return usc.getDeviceDescriptorBinderUDA10();
	}

	@Override
	public ServiceDescriptorBinder getServiceDescriptorBinderUDA10() {
		return usc.getServiceDescriptorBinderUDA10();
	}

	@Override
	public ServiceType[] getExclusiveServiceTypes() {
		return usc.getExclusiveServiceTypes();
	}

	@Override
	public int getRegistryMaintenanceIntervalMillis() {
		return usc.getRegistryMaintenanceIntervalMillis();
	}

	@Override
	public int getAliveIntervalMillis() {
		return usc.getAliveIntervalMillis();
	}

	@Override
	public boolean isReceivedSubscriptionTimeoutIgnored() {
		return usc.isReceivedSubscriptionTimeoutIgnored();
	}

	@Override
	public Integer getRemoteDeviceMaxAgeSeconds() {
		return usc.getRemoteDeviceMaxAgeSeconds();
	}

	@Override
	public UpnpHeaders getDescriptorRetrievalHeaders(RemoteDeviceIdentity _identity) {
		return usc.getDescriptorRetrievalHeaders(_identity);
	}

	@Override
	public UpnpHeaders getEventSubscriptionHeaders(RemoteService _service) {
		return usc.getEventSubscriptionHeaders(_service);
	}

	@Override
	public Executor getAsyncProtocolExecutor() {
		return usc.getAsyncProtocolExecutor();
	}

	@Override
	public ExecutorService getSyncProtocolExecutorService() {
		return usc.getSyncProtocolExecutorService();
	}

	@Override
	public Namespace getNamespace() {
		return usc.getNamespace();
	}

	@Override
	public Executor getRegistryMaintainerExecutor() {
		return usc.getRegistryMaintainerExecutor();
	}

	@Override
	public Executor getRegistryListenerExecutor() {
		return usc.getRegistryListenerExecutor();
	}

	@Override
	public void shutdown() {

		usc.shutdown();
	}

}
