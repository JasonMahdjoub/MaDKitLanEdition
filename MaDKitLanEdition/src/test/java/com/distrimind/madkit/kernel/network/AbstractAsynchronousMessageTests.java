package com.distrimind.madkit.kernel.network;
/*
Copyright or © or Copr. Jason Mahdjoub (01/04/2013)

jason.mahdjoub@distri-mind.fr

This software (Object Oriented Database (OOD)) is a computer program 
whose purpose is to manage a local database with the object paradigm 
and the java language 

This software is governed by the CeCILL-C license under French law and
abiding by the rules of distribution of free software.  You can  use, 
modify and/ or redistribute the software under the terms of the CeCILL-C
license as circulated by CEA, CNRS and INRIA at the following URL
"http://www.cecill.info". 

As a counterpart to the access to the source code and  rights to copy,
modify and redistribute granted by the license, users are provided only
with a limited warranty  and the software's author,  the holder of the
economic rights,  and the successive licensors  have only  limited
liability. 

In this respect, the user's attention is drawn to the risks associated
with loading,  using,  modifying and/or developing or reproducing the
software by the user in light of its specific status of free software,
that may mean  that it is complicated to manipulate,  and  that  also
therefore means  that it is reserved for developers  and  experienced
professionals having in-depth computer knowledge. Users are therefore
encouraged to load and test the software's suitability as regards their
requirements in conditions enabling the security of their systems and/or 
data to be ensured and,  more generally, to use and operate it in the 
same conditions as regards security. 

The fact that you are presently reading this means that you have had
knowledge of the CeCILL-C license and that you accept its terms.
 */

import com.distrimind.madkit.kernel.AbstractAgent;
import com.distrimind.madkit.kernel.Madkit;
import com.distrimind.madkit.kernel.MadkitProperties;
import com.distrimind.madkit.kernel.TestNGMadkit;
import com.distrimind.madkit.kernel.network.connection.access.AbstractAccessProtocolProperties;
import com.distrimind.madkit.kernel.network.connection.access.AccessProtocolWithP2PAgreementProperties;
import com.distrimind.madkit.kernel.network.connection.access.ListGroupsRoles;
import com.distrimind.madkit.kernel.network.connection.secured.P2PSecuredConnectionProtocolPropertiesWithKeyAgreement;
import com.distrimind.ood.database.InMemoryEmbeddedH2DatabaseFactory;
import com.distrimind.ood.database.exceptions.DatabaseException;
import com.distrimind.util.crypto.SymmetricAuthenticatedSignatureType;
import com.distrimind.util.crypto.SymmetricEncryptionType;
import org.testng.AssertJUnit;

import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;

/**
 * @author Jason Mahdjoub
 * @version 1.0
 * @since MaDKitLanEdition 2.3.0
 */
public class AbstractAsynchronousMessageTests extends TestNGMadkit {
	final NetworkEventListener eventListener1;
	final NetworkEventListener eventListener2;
	final AtomicInteger counter=new AtomicInteger(0);
	public AbstractAsynchronousMessageTests() throws UnknownHostException {
		P2PSecuredConnectionProtocolPropertiesWithKeyAgreement p2pprotocol=new P2PSecuredConnectionProtocolPropertiesWithKeyAgreement();
		p2pprotocol.isServer = true;
		p2pprotocol.symmetricEncryptionType= SymmetricEncryptionType.AES_CTR;
		p2pprotocol.symmetricSignatureType= SymmetricAuthenticatedSignatureType.HMAC_SHA2_384;
		ListGroupsRoles defaultGroupAccess=new ListGroupsRoles();
		defaultGroupAccess.addGroupsRoles(TestNGMadkit.GROUP,TestNGMadkit.ROLE, TestNGMadkit.ROLE2);
		defaultGroupAccess.addGroupsRoles(TestNGMadkit.GROUP2,TestNGMadkit.ROLE, TestNGMadkit.ROLE2);


		AbstractAccessProtocolProperties app = new AccessProtocolWithP2PAgreementProperties();
		this.eventListener1 = new NetworkEventListener(true, false, false, null,
				new ConnectionsProtocolsMKEventListener(p2pprotocol), new AccessProtocolPropertiesMKEventListener(app),
				new AccessDataMKEventListener(AccessDataMKEventListener.getDefaultAccessData(defaultGroupAccess)), 5000,
				Collections.emptyList(),
				InetAddress.getByName("127.0.0.1"), InetAddress.getByName("::1")) {

			@Override
			public void onMaDKitPropertiesLoaded(MadkitProperties _properties) {
				super.onMaDKitPropertiesLoaded(_properties);
				_properties.networkProperties.networkLogLevel = Level.INFO;
				try {
					_properties.setDatabaseFactory(new InMemoryEmbeddedH2DatabaseFactory("dbmem"+counter.getAndIncrement()));
				} catch (DatabaseException e) {
					e.printStackTrace();
				}
			}
		};
		P2PSecuredConnectionProtocolPropertiesWithKeyAgreement u = new P2PSecuredConnectionProtocolPropertiesWithKeyAgreement();
		u.isServer = false;
		u.symmetricEncryptionType=p2pprotocol.symmetricEncryptionType;
		u.symmetricSignatureType= p2pprotocol.symmetricSignatureType;

		app = new AccessProtocolWithP2PAgreementProperties();

		this.eventListener2 = new NetworkEventListener(true, false, false, null,
				new ConnectionsProtocolsMKEventListener(u), new AccessProtocolPropertiesMKEventListener(app),
				new AccessDataMKEventListener(AccessDataMKEventListener.getDefaultAccessData(defaultGroupAccess)), 5000,
				Collections.singletonList(new DoubleIP(5000, (Inet4Address) InetAddress.getByName("127.0.0.1"), (Inet6Address) InetAddress.getByName("::1")))
		) {

			@Override
			public void onMaDKitPropertiesLoaded(MadkitProperties _properties) {
				super.onMaDKitPropertiesLoaded(_properties);
				_properties.networkProperties.networkLogLevel = Level.INFO;
				try {
					_properties.setDatabaseFactory(new InMemoryEmbeddedH2DatabaseFactory("dbmem"+counter.getAndIncrement()));
				} catch (DatabaseException e) {
					e.printStackTrace();
				}
			}
		};
	}
	public void launchPeers(AbstractAgent launcher, AbstractAgent sender, AbstractAgent receiver) throws InterruptedException {
		launchServer(launcher, receiver);
		launchClient(launcher, sender);
	}

	public void launchClient(AbstractAgent launcher, AbstractAgent sender) throws InterruptedException {
		launchThreadedMKNetworkInstance(Level.INFO, AbstractAgent.class, sender, eventListener2);
		launcher.sleep(400);
	}
	public void launchServer(AbstractAgent launcher, AbstractAgent receiver) throws InterruptedException {
		launchThreadedMKNetworkInstance(Level.INFO, AbstractAgent.class, receiver, eventListener1);
		launcher.sleep(400);
	}



}
