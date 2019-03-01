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
package com.distrimind.madkit.kernel.network.connection.access;

import java.io.File;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.*;

import com.distrimind.ood.database.EmbeddedH2DatabaseFactory;
import com.distrimind.ood.database.EmbeddedH2DatabaseWrapper;
import com.distrimind.util.crypto.P2PLoginAgreementType;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import com.distrimind.madkit.database.KeysPairs;
import com.distrimind.madkit.kernel.JunitMadkit;
import com.distrimind.madkit.kernel.KernelAddress;
import com.distrimind.madkit.kernel.KernelAddressTest;
import com.distrimind.madkit.kernel.MadkitProperties;
import com.distrimind.madkit.kernel.network.AccessDataMKEventListener;
import com.distrimind.madkit.kernel.network.ConnectionsProtocolsTests;
import com.distrimind.ood.database.DatabaseConfiguration;
import com.distrimind.ood.database.exceptions.DatabaseException;

import gnu.vm.jgnu.security.NoSuchAlgorithmException;
import gnu.vm.jgnu.security.NoSuchProviderException;

/**
 * 
 * @author Jason Mahdjoub
 * @version 1.2
 * @since MadkitLanEdition 1.0
 */
@SuppressWarnings({"SameParameterValue"})
@RunWith(Parameterized.class)
public class AccessProtocolWithP2PAgreementTests implements AccessGroupsNotifier, LoginEventsTrigger {
	private static final int numberMaxExchange = 100;
	final ArrayList<AccessData> adasker;
	final ArrayList<AccessData> adreceiver;
	final MadkitProperties mpasker;
	final MadkitProperties mpreceiver;
	AbstractAccessProtocol apasker;
	AbstractAccessProtocol apreceiver;
	ArrayList<Identifier> acceptedAskerIdentifiers;
	ArrayList<Identifier> acceptedReceiverIdentifiers;
	final ArrayList<IdentifierPassword> identifierPassordsAsker;
	final ArrayList<IdentifierPassword> identifierPassordsReceiver;
	final ArrayList<Identifier> initialAcceptedAskerIdentifiers;
	final ArrayList<Identifier> initialAcceptedReceiverIdentifiers;
	final ArrayList<IdentifierPassword> initialIdentifierPassordsAsker;
	final ArrayList<IdentifierPassword> initialIdentifierPassordsReceiver;

	static final File dbfileasker = new File("testaccessasker.database");
	static final File dbfilereceiver = new File("testaccessreceiver.database");


	@Parameters
	public static Collection<Object[]> data() {
		Collection<Object[]> res=null;
		for (P2PLoginAgreementType agreement : P2PLoginAgreementType.values()) {
			for (boolean databaseEnabled : new boolean[]{true, false}) {
				for (boolean identifierEncrypted : new boolean[]{true, false}) {
					for (boolean loginInitiativeAsker : new boolean[]{false, true}) {
						for (boolean loginInitiativeReceiver : new boolean[]{true, false}) {
							AccessProtocolWithP2PAgreementProperties app2 = new AccessProtocolWithP2PAgreementProperties();
							app2.encryptIdentifiersBeforeSendingToDistantPeer = identifierEncrypted;
							app2.p2pLoginAgreementType=agreement;
							Collection<Object[]> r = data(databaseEnabled, app2, loginInitiativeAsker, loginInitiativeReceiver);
							if (res == null)
								res = r;
							else
								res.addAll(r);
						}
					}
				}
			}
		}

		return res;
	}
	
	

	public static Collection<Object[]> data(boolean databaseEnabled, AbstractAccessProtocolProperties accessProtocolProperties, boolean loginInitiativeAsker, boolean loginInitiativeReceiver) {
		ArrayList<Object[]> res = new ArrayList<>();
		ArrayList<AccessData> adasker = new ArrayList<>();
		ArrayList<AccessData> adreceiver = new ArrayList<>();
		ArrayList<Identifier> acceptedAskerIdentifiers = new ArrayList<>();
		ArrayList<Identifier> acceptedReceiverIdentifiers = new ArrayList<>();
		ArrayList<IdentifierPassword> identifierPassordsAsker;
		ArrayList<IdentifierPassword> identifierPassordsReceiver;

		Object[] o = new Object[8];
		adasker.add(AccessDataMKEventListener.getDefaultAccessData(JunitMadkit.DEFAULT_NETWORK_GROUP_FOR_ACCESS_DATA));
		adreceiver
				.add(AccessDataMKEventListener.getDefaultAccessData(JunitMadkit.DEFAULT_NETWORK_GROUP_FOR_ACCESS_DATA));
		o[0] = adasker;
		o[1] = adreceiver;
		o[2] = acceptedAskerIdentifiers;
		o[3] = acceptedReceiverIdentifiers;
		o[4] = null;
		o[5] = null;
		o[6] = databaseEnabled;
		o[7] = accessProtocolProperties;
		res.add(o);



		o = new Object[8];
		adasker = new ArrayList<>();
		adreceiver = new ArrayList<>();
		acceptedAskerIdentifiers = new ArrayList<>();
		acceptedReceiverIdentifiers = new ArrayList<>();
		adasker.add(AccessDataMKEventListener.getDefaultLoginData(
				identifierPassordsAsker = AccessDataMKEventListener
						.getClientOrPeerToPeerLogins(AccessDataMKEventListener.getCustumHostIdentifier(0), 4, 5, 6, 10),
				null, JunitMadkit.NETWORK_GROUP_FOR_LOGIN_DATA, loginInitiativeAsker, new Runnable() {

					@Override
					public void run() {
						Assert.fail();
					}
				}));
		adreceiver.add(AccessDataMKEventListener.getDefaultLoginData(
				identifierPassordsReceiver = AccessDataMKEventListener
						.getClientOrPeerToPeerLogins(AccessDataMKEventListener.getCustumHostIdentifier(1), 2, 5, 6, 12),
				null, JunitMadkit.NETWORK_GROUP_FOR_LOGIN_DATA, loginInitiativeReceiver, new Runnable() {

					@Override
					public void run() {
						Assert.fail();
					}
				}));
		if (loginInitiativeAsker || loginInitiativeReceiver) {
			acceptedAskerIdentifiers
					.add(AccessDataMKEventListener.getIdentifier(AccessDataMKEventListener.getCustumHostIdentifier(0), 5));
			acceptedAskerIdentifiers
					.add(AccessDataMKEventListener.getIdentifier(AccessDataMKEventListener.getCustumHostIdentifier(0), 6));
			acceptedReceiverIdentifiers
					.add(AccessDataMKEventListener.getIdentifier(AccessDataMKEventListener.getCustumHostIdentifier(1), 5));
			acceptedReceiverIdentifiers
					.add(AccessDataMKEventListener.getIdentifier(AccessDataMKEventListener.getCustumHostIdentifier(1), 6));
		}
		o[0] = adasker;
		o[1] = adreceiver;
		o[2] = acceptedAskerIdentifiers;
		o[3] = acceptedReceiverIdentifiers;
		o[4] = identifierPassordsAsker;
		o[5] = identifierPassordsReceiver;
		o[6] = databaseEnabled;
		o[7] = accessProtocolProperties;
		res.add(o);


		o = new Object[8];
		adasker = new ArrayList<>();
		adreceiver = new ArrayList<>();
		acceptedAskerIdentifiers = new ArrayList<>();
		acceptedReceiverIdentifiers = new ArrayList<>();
		adasker.add(AccessDataMKEventListener.getDefaultLoginData(
				identifierPassordsAsker = AccessDataMKEventListener
						.getClientOrPeerToPeerLogins(AccessDataMKEventListener.getCustumHostIdentifier(0), 2, 5, 6, 9),
				null, JunitMadkit.NETWORK_GROUP_FOR_LOGIN_DATA, loginInitiativeAsker, new Runnable() {

					@Override
					public void run() {
						Assert.fail();
					}
				}));
		adreceiver.add(AccessDataMKEventListener.getDefaultLoginData(
				identifierPassordsReceiver = AccessDataMKEventListener
						.getClientOrPeerToPeerLogins(AccessDataMKEventListener.getCustumHostIdentifier(1), 3, 5, 6, 12),
				null, JunitMadkit.NETWORK_GROUP_FOR_LOGIN_DATA, loginInitiativeReceiver, new Runnable() {

					@Override
					public void run() {
						Assert.fail();
					}
				}));
		if (loginInitiativeAsker || loginInitiativeReceiver) {
			acceptedAskerIdentifiers
					.add(AccessDataMKEventListener.getIdentifier(AccessDataMKEventListener.getCustumHostIdentifier(0), 5));
			acceptedAskerIdentifiers
					.add(AccessDataMKEventListener.getIdentifier(AccessDataMKEventListener.getCustumHostIdentifier(0), 6));
			if (loginInitiativeReceiver)
				acceptedAskerIdentifiers
						.add(new Identifier(AccessDataMKEventListener.getIdentifier(AccessDataMKEventListener.getCustumHostIdentifier(0), 3).getCloudIdentifier(), HostIdentifier.getNullHostIdentifierSingleton()));
			if (loginInitiativeAsker)
				acceptedAskerIdentifiers
						.add(AccessDataMKEventListener.getIdentifier(AccessDataMKEventListener.getCustumHostIdentifier(0), 9));
			acceptedReceiverIdentifiers
					.add(AccessDataMKEventListener.getIdentifier(AccessDataMKEventListener.getCustumHostIdentifier(1), 5));
			acceptedReceiverIdentifiers
					.add(AccessDataMKEventListener.getIdentifier(AccessDataMKEventListener.getCustumHostIdentifier(1), 6));
			if (loginInitiativeReceiver)
				acceptedReceiverIdentifiers
						.add(AccessDataMKEventListener.getIdentifier(AccessDataMKEventListener.getCustumHostIdentifier(1), 3));
			if (loginInitiativeAsker)
				acceptedReceiverIdentifiers
						.add(new Identifier(AccessDataMKEventListener.getIdentifier(AccessDataMKEventListener.getCustumHostIdentifier(1), 9).getCloudIdentifier(), HostIdentifier.getNullHostIdentifierSingleton()));
		}
		o[0] = adasker;
		o[1] = adreceiver;
		o[2] = acceptedAskerIdentifiers;
		o[3] = acceptedReceiverIdentifiers;
		o[4] = identifierPassordsAsker;
		o[5] = identifierPassordsReceiver;
		o[6] = databaseEnabled;
		o[7] = accessProtocolProperties;
		res.add(o);

		o = new Object[8];
		adasker = new ArrayList<>();
		adreceiver = new ArrayList<>();
		acceptedAskerIdentifiers = new ArrayList<>();
		acceptedReceiverIdentifiers = new ArrayList<>();
		adasker.add(AccessDataMKEventListener.getDefaultLoginData(
				identifierPassordsAsker = AccessDataMKEventListener
						.getClientOrPeerToPeerLogins(AccessDataMKEventListener.getCustumHostIdentifier(-1), 1),
				null, JunitMadkit.NETWORK_GROUP_FOR_LOGIN_DATA, loginInitiativeAsker, new Runnable() {

					@Override
					public void run() {
						Assert.fail();
					}
				}));
		adreceiver.add(AccessDataMKEventListener.getDefaultLoginData(
				identifierPassordsReceiver = AccessDataMKEventListener
						.getClientOrPeerToPeerLogins(AccessDataMKEventListener.getCustumHostIdentifier(-1)),
				null, JunitMadkit.NETWORK_GROUP_FOR_LOGIN_DATA, loginInitiativeReceiver, new Runnable() {

					@Override
					public void run() {
						Assert.fail();
					}
				}));
		if (loginInitiativeAsker || loginInitiativeReceiver) {
			if (loginInitiativeAsker)
				acceptedAskerIdentifiers
						.add(AccessDataMKEventListener.getIdentifier(AccessDataMKEventListener.getCustumHostIdentifier(-1), 1));
			if (loginInitiativeAsker)
				acceptedReceiverIdentifiers
						.add(new Identifier(AccessDataMKEventListener.getIdentifier(AccessDataMKEventListener.getCustumHostIdentifier(-1), 1).getCloudIdentifier(), HostIdentifier.getNullHostIdentifierSingleton()));
		}
		o[0] = adasker;
		o[1] = adreceiver;
		o[2] = acceptedAskerIdentifiers;
		o[3] = acceptedReceiverIdentifiers;
		o[4] = identifierPassordsAsker;
		o[5] = identifierPassordsReceiver;
		o[6] = databaseEnabled;
		o[7] = accessProtocolProperties;
		res.add(o);

		o = new Object[8];
		adasker = new ArrayList<>();
		adreceiver = new ArrayList<>();
		acceptedAskerIdentifiers = new ArrayList<>();
		acceptedReceiverIdentifiers = new ArrayList<>();
		adasker.add(AccessDataMKEventListener.getDefaultLoginData(
				identifierPassordsAsker = AccessDataMKEventListener
						.getClientOrPeerToPeerLogins(AccessDataMKEventListener.getCustumHostIdentifier(0), 2, 8, 6, 10),
				JunitMadkit.DEFAULT_NETWORK_GROUP_FOR_ACCESS_DATA, JunitMadkit.NETWORK_GROUP_FOR_LOGIN_DATA, loginInitiativeAsker,
				new Runnable() {

					@Override
					public void run() {
						Assert.fail();
					}
				}));
		adreceiver.add(AccessDataMKEventListener.getDefaultLoginData(
				identifierPassordsReceiver = AccessDataMKEventListener
						.getClientOrPeerToPeerLogins(AccessDataMKEventListener.getCustumHostIdentifier(1), 0, 8, 6, 12),
				JunitMadkit.DEFAULT_NETWORK_GROUP_FOR_ACCESS_DATA, JunitMadkit.NETWORK_GROUP_FOR_LOGIN_DATA, loginInitiativeReceiver,
				new Runnable() {

					@Override
					public void run() {
						Assert.fail();
					}
				}));
		if (loginInitiativeAsker || loginInitiativeReceiver) {
			acceptedAskerIdentifiers
					.add(AccessDataMKEventListener.getIdentifier(AccessDataMKEventListener.getCustumHostIdentifier(0), 8));
			acceptedAskerIdentifiers
					.add(AccessDataMKEventListener.getIdentifier(AccessDataMKEventListener.getCustumHostIdentifier(0), 6));
			acceptedReceiverIdentifiers
					.add(AccessDataMKEventListener.getIdentifier(AccessDataMKEventListener.getCustumHostIdentifier(1), 8));
			acceptedReceiverIdentifiers
					.add(AccessDataMKEventListener.getIdentifier(AccessDataMKEventListener.getCustumHostIdentifier(1), 6));
		}
		o[0] = adasker;
		o[1] = adreceiver;
		o[2] = acceptedAskerIdentifiers;
		o[3] = acceptedReceiverIdentifiers;
		o[4] = identifierPassordsAsker;
		o[5] = identifierPassordsReceiver;
		o[6] = databaseEnabled;
		o[7] = accessProtocolProperties;
		res.add(o);




		return res;
	}

	public AccessProtocolWithP2PAgreementTests(ArrayList<AccessData> adasker, ArrayList<AccessData> adreceiver,
											   ArrayList<Identifier> acceptedAskerIdentifiers, ArrayList<Identifier> acceptedReceiverIdentifiers,
											   ArrayList<IdentifierPassword> identifierPassordsAsker,
											   ArrayList<IdentifierPassword> identifierPassordsReceiver, boolean databaseEnabled, AbstractAccessProtocolProperties accessProtocolProperties)
			throws IllegalArgumentException, DatabaseException {
		this.adasker = adasker;
		this.adreceiver = adreceiver;
		this.identifierPassordsAsker = identifierPassordsAsker;
		this.identifierPassordsReceiver = identifierPassordsReceiver;
		this.mpasker = new MadkitProperties();
		this.mpreceiver = new MadkitProperties();
		for (AccessData ad : adasker)
			this.mpasker.networkProperties.addAccessData(ad);
		for (AccessData ad : adreceiver)
			this.mpreceiver.networkProperties.addAccessData(ad);
		this.mpasker.networkProperties.addAccessProtocolProperties(accessProtocolProperties);
		this.mpreceiver.networkProperties.addAccessProtocolProperties(accessProtocolProperties);
		this.acceptedAskerIdentifiers = null;
		this.acceptedReceiverIdentifiers = null;
		this.initialAcceptedAskerIdentifiers=acceptedAskerIdentifiers;
		this.initialAcceptedReceiverIdentifiers=acceptedReceiverIdentifiers;
		this.initialIdentifierPassordsAsker=identifierPassordsAsker==null?null:new ArrayList<IdentifierPassword>();
		if (initialIdentifierPassordsAsker!=null)
		{
			initialIdentifierPassordsAsker.addAll(identifierPassordsAsker);
		}
		this.initialIdentifierPassordsReceiver=identifierPassordsAsker==null?null:new ArrayList<IdentifierPassword>();
		if (initialIdentifierPassordsReceiver!=null)
		{
			initialIdentifierPassordsReceiver.addAll(identifierPassordsReceiver);
		}
		if (databaseEnabled) {
			mpasker.setDatabaseFactory(new EmbeddedH2DatabaseFactory(dbfileasker));
			mpreceiver.setDatabaseFactory(new EmbeddedH2DatabaseFactory(dbfilereceiver));
		}
		//System.out.println(accessProtocolProperties.getClass());
	}

	@Before
	public void activateDatabase() throws DatabaseException {
		if (mpasker.isDatabaseEnabled()) {
			mpasker.getDatabaseWrapper().close();
			mpreceiver.getDatabaseWrapper().close();
			if (dbfileasker.exists())
				EmbeddedH2DatabaseWrapper.deleteDatabaseFiles(dbfileasker);
			if (dbfilereceiver.exists())
				EmbeddedH2DatabaseWrapper.deleteDatabaseFiles(dbfilereceiver);
			JunitMadkit.setDatabaseFactory(mpasker, new EmbeddedH2DatabaseFactory(dbfileasker));
			mpasker.getDatabaseWrapper().loadDatabase(new DatabaseConfiguration(KeysPairs.class.getPackage()), true);
			JunitMadkit.setDatabaseFactory(mpreceiver, new EmbeddedH2DatabaseFactory(dbfilereceiver));
			mpreceiver.getDatabaseWrapper().loadDatabase(new DatabaseConfiguration(KeysPairs.class.getPackage()), true);
		}
	}

	@After
	public void removeDatabase() throws DatabaseException {
		if (mpasker.isDatabaseEnabled()) {
			mpasker.getDatabaseWrapper().close();
			mpreceiver.getDatabaseWrapper().close();
			if (dbfileasker.exists())
				EmbeddedH2DatabaseWrapper.deleteDatabaseFiles(dbfileasker);
			if (dbfilereceiver.exists())
				EmbeddedH2DatabaseWrapper.deleteDatabaseFiles(dbfilereceiver);
		}

	}

	@Test
	public void testAccessProtocol() throws AccessException, ClassNotFoundException, IOException,
			NoSuchAlgorithmException, NoSuchProviderException {
		int nb = testRegularAccessProtocol(0, -1, false);
		for (int i = 0; i < nb - 1; i++) {
			System.out.println(i+"/"+(nb-1));
			testRegularAccessProtocol(1, i, true);
			testRegularAccessProtocol(1, i, false);
			testRegularAccessProtocol(2, i, true);
			testRegularAccessProtocol(2, i, false);
		}
	}
	
	

	static class UnknownAccessMessage extends AccessMessage {
		/**
		 * 
		 */
		private static final long serialVersionUID = 3702277006786527287L;

		public UnknownAccessMessage() {
		}

		@Override
		public boolean checkDifferedMessages() {
			return false;
		}

		@Override
		public void writeExternal(ObjectOutput out) {
			
		}

		@Override
		public void readExternal(ObjectInput in) {
			
		}

		

	}

	private AccessMessage[] getAccessMessages(AccessMessage m, AccessMessage[] old)
	{
		ArrayList<AccessMessage> res=new ArrayList<>();
		if (m==null)
			return old;
		Collections.addAll(res, old);
		if (m instanceof AccessMessagesList)
		{
			Collections.addAll(res, ((AccessMessagesList) m).getMessages());
		}
		else
			res.add(m);
		return toArray(res);
	}
	private AccessMessage[] getAccessMessages(AccessMessage m)
	{
		if (m==null)
			return new AccessMessage[0];
		if (m instanceof AccessMessagesList)
		{
			return ((AccessMessagesList) m).getMessages();
		}
		else
			return new AccessMessage[] {m};
	}
	private AccessMessage[] toArray(List<AccessMessage> list)
	{
		AccessMessage[] res=new AccessMessage[list.size()];
		for (int i=0;i<list.size();i++)
			res[i]=list.get(i);
		return res;
	}
	private boolean infoScreened=false;
	public int testRegularAccessProtocol(int type, int index, boolean asker)
			throws AccessException, ClassNotFoundException, IOException, NoSuchAlgorithmException,
			NoSuchProviderException {
		
		this.acceptedAskerIdentifiers= new ArrayList<>();
		this.acceptedAskerIdentifiers.addAll(this.initialAcceptedAskerIdentifiers);
		this.acceptedReceiverIdentifiers= new ArrayList<>();
		this.acceptedReceiverIdentifiers.addAll(this.initialAcceptedReceiverIdentifiers);
		if (identifierPassordsAsker!=null)
		{
			this.identifierPassordsAsker.clear();
			this.identifierPassordsAsker.addAll(this.initialIdentifierPassordsAsker);
		}
		if (identifierPassordsReceiver!=null)
		{
			this.identifierPassordsReceiver.clear();
			this.identifierPassordsReceiver.addAll(this.initialIdentifierPassordsReceiver);
		}
		
		boolean allCannotTakeInitiatives = identifierPassordsAsker != null
				&& !((LoginData) this.mpasker.networkProperties.getAccessData(
						new InetSocketAddress(InetAddress.getByName("56.41.158.221"), 5000),
						new InetSocketAddress(InetAddress.getByName("192.168.0.55"), 5000))).canTakesLoginInitiative()
				&& !((LoginData) this.mpreceiver.networkProperties.getAccessData(
						new InetSocketAddress(InetAddress.getByName("56.41.158.221"), 5000),
						new InetSocketAddress(InetAddress.getByName("192.168.0.55"), 5000))).canTakesLoginInitiative();
		AbstractAccessProtocolProperties app=this.mpasker.networkProperties.getAccessProtocolProperties(new InetSocketAddress(InetAddress.getByName("56.41.158.221"), 5000),
				new InetSocketAddress(InetAddress.getByName("192.168.0.55"), 5000));
		apasker = app
				.getAccessProtocolInstance(new InetSocketAddress(InetAddress.getByName("56.41.158.221"), 5000),
						new InetSocketAddress(InetAddress.getByName("192.168.0.55"), 5000), this, mpasker);
		apreceiver = this.mpreceiver.networkProperties.getAccessProtocolProperties(new InetSocketAddress(InetAddress.getByName("56.41.158.221"), 5000),
				new InetSocketAddress(InetAddress.getByName("192.168.0.55"), 5000))
				.getAccessProtocolInstance(new InetSocketAddress(InetAddress.getByName("56.41.158.221"), 5000),
				new InetSocketAddress(InetAddress.getByName("192.168.0.55"), 5000), this, mpreceiver);
		if (!infoScreened) {
			System.out.println("agreement : "+((AccessProtocolWithP2PAgreementProperties)app).p2pLoginAgreementType);
			System.out.println("encrypted : " + app.encryptIdentifiersBeforeSendingToDistantPeer);
			System.out.println("login data asker : " + (apasker.access_data instanceof LoginData));
			System.out.println("login data receiver : " + (apreceiver.access_data instanceof LoginData));
			System.out.println("login initiative asker : " + ((apasker.access_data instanceof LoginData) && ((LoginData) apasker.access_data).canTakesLoginInitiative()));
			System.out.println("login initiative receiver : " + ((apreceiver.access_data instanceof LoginData) && ((LoginData) apreceiver.access_data).canTakesLoginInitiative()));
			infoScreened=true;
		}



		KernelAddress kaasker=KernelAddressTest.getKernelAddressInstance();
		KernelAddress kareceiver=KernelAddressTest.getKernelAddressInstance();
		apasker.setKernelAddress(kaasker, true);
		//apreceiver.setDistantKernelAddress(kaasker);
		apreceiver.setKernelAddress(kareceiver, false);
		//apasker.setDistantKernelAddress(kareceiver);

		Assert.assertFalse(apasker.isAccessFinalized());
		Assert.assertFalse(apreceiver.isAccessFinalized());

		AccessMessage[] masker = getAccessMessages(apasker.setAndGetNextMessage(new AccessAskInitiliazation()));
		AccessMessage[] mreceiver = getAccessMessages(apreceiver.setAndGetNextMessage(new AccessAskInitiliazation()));
		boolean askerAsNotifiedGroupsChangements = false;
		boolean receiverAsNotifiedGroupsChangements = false;
		int cycles = 0;
		do {
			/*
			 * System.out.println("asker="+masker);
			 * System.out.println("receiver="+mreceiver);
			 */
			ArrayList<AccessMessage> mreceiver2 = new ArrayList<>();
			ArrayList<AccessMessage> masker2 = new ArrayList<>();

			if (cycles == index && asker && type == 1) {
				masker = getAccessMessages(new UnknownAccessMessage());
			}
			for (AccessMessage m : masker)
			{
				if (m != null && !(m instanceof DoNotSendMessage)) {
					if (cycles == index && asker && type == 2) {
						m.corrupt();
					}
	
					m = (AccessMessage) ConnectionsProtocolsTests
							.unserialize(ConnectionsProtocolsTests.serialize(m));
					mreceiver2.addAll(Arrays.asList(getAccessMessages(apreceiver.setAndGetNextMessage(m))));
					receiverAsNotifiedGroupsChangements |= apreceiver.isNotifyAccessGroupChangements();
				}
			}
			if (cycles == index && !asker && type == 1) {
				mreceiver = getAccessMessages(new UnknownAccessMessage());
			}
			for (AccessMessage m : mreceiver)
			{
				if (m != null && !(m instanceof DoNotSendMessage)) {
					if (cycles == index && !asker && type == 2) {
						m.corrupt();
					}
					m = (AccessMessage) ConnectionsProtocolsTests
							.unserialize(ConnectionsProtocolsTests.serialize(m));
					masker2.addAll(Arrays.asList(getAccessMessages(apasker.setAndGetNextMessage(m))));
					askerAsNotifiedGroupsChangements |= apasker.isNotifyAccessGroupChangements();
				}
			}
			mreceiver = toArray(mreceiver2);
			masker = toArray(masker2);
			int nberror = 0;
			for (AccessMessage m : mreceiver)
				if (m instanceof AccessErrorMessage)
					nberror++;
			for (AccessMessage m : masker)
				if (m instanceof AccessErrorMessage)
					nberror++;
			if (((mreceiver.length==0 || masker.length==0) && nberror > 0) || nberror == 2) {
				/*
				 * if (masker!=null) {
				 * masker=(AccessMessage)ConnectionsProtocolsTests.unserialize(
				 * ConnectionsProtocolsTests.serialize(masker));
				 * Assert.assertEquals(masker.checkDataIntegrity(), Integrity.OK);
				 * mreceiver2=apreceiver.setAndGetNextMessage(masker); }
				 */
				mreceiver = new AccessMessage[0];
				masker = new AccessMessage[0];
			}
			cycles++;
		} while ((masker.length>0 || mreceiver.length>0) && cycles < numberMaxExchange);
		Assert.assertTrue(cycles < numberMaxExchange);
		//Assert.assertTrue(masker.length==0 && mreceiver.length==0);
		if (allCannotTakeInitiatives || type == 1) {
			Assert.assertFalse(apreceiver.isAccessFinalized());
			Assert.assertFalse(apasker.isAccessFinalized());
			return -1;
		}
		Assert.assertTrue(apreceiver.isAccessFinalized());
		Assert.assertTrue(apasker.isAccessFinalized());
		if (type == 0) {
			Assert.assertTrue(askerAsNotifiedGroupsChangements);
			Assert.assertTrue(receiverAsNotifiedGroupsChangements);
		}
		testExpectedLogins();
		if (identifierPassordsAsker != null && ((LoginData) this.mpasker.networkProperties.getAccessData(
				new InetSocketAddress(InetAddress.getByName("56.41.158.221"), 5000),
				new InetSocketAddress(InetAddress.getByName("192.168.0.55"), 5000))).canTakesLoginInitiative()) {
			testAddingOneNewIdentifier(10);
			boolean testASymmetricLogin=apasker instanceof AccessProtocolWithP2PAgreement;
			if (testASymmetricLogin) {
				testAddingTwoNewIdentifier(11, 12, false);
				testAddingTwoNewIdentifier(13, 14, true);

				testRemovingOneNewIdentifier(10);
				testRemovingTwoNewIdentifier(11, 12, false);
				testRemovingTwoNewIdentifier(13, 15, true);
			}
			else
			{
				testAddingTwoNewIdentifier(12, 14, false);
				testAddingTwoNewIdentifier(16, 18, true);

				testRemovingOneNewIdentifier(10);
				testRemovingTwoNewIdentifier(12, 14, false);
				testRemovingTwoNewIdentifier(16, 20, true);
			}
			testAddingOneNewIdentifierNonUsable(16);
			testAddingTwoNewIdentifierNonUsable(18, 20, false);
			testAddingTwoNewIdentifierNonUsable(22, 24, true);
		}
		return cycles;
	}

	private void testAddingOneNewIdentifier(int newid) throws AccessException, ClassNotFoundException, IOException {
		HostIdentifier hostIDAsker=AccessDataMKEventListener.getCustumHostIdentifier(0);
		HostIdentifier hostIDReceiver=AccessDataMKEventListener.getCustumHostIdentifier(1);
		IdentifierPassword idpwAsker = AccessDataMKEventListener
				.getIdentifierPassword(hostIDAsker, newid);
		IdentifierPassword idpwReceiver = AccessDataMKEventListener
				.getIdentifierPassword(hostIDReceiver, newid);
		ArrayList<Identifier> addedForAsker = new ArrayList<>();
		//ArrayList<Identifier> addedForReceiver = new ArrayList<>();
		identifierPassordsAsker.add(idpwAsker);
		acceptedAskerIdentifiers.add(idpwAsker.getIdentifier());
		addedForAsker.add(idpwAsker.getIdentifier());
		identifierPassordsReceiver.add(idpwReceiver);
		acceptedReceiverIdentifiers.add(idpwReceiver.getIdentifier());
		//addedForReceiver.add(idpwReceiver.getIdentifier());

		testAddingNewIdentifier(addedForAsker/*, addedForReceiver*/);
	}
	private void testAddingOneNewIdentifierNonUsable(int newid) throws AccessException, ClassNotFoundException, IOException {
		IdentifierPassword idpwAsker = AccessDataMKEventListener
				.getIdentifierPassword(AccessDataMKEventListener.getCustumHostIdentifier(0), newid);
		identifierPassordsAsker.add(idpwAsker);
		ArrayList<Identifier> addedForAsker = new ArrayList<>();
		//ArrayList<Identifier> addedForReceiver = new ArrayList<>();
		addedForAsker.add(idpwAsker.getIdentifier());
		
		testAddingNewIdentifier(addedForAsker/*, addedForReceiver*/);
	}
	private void testAddingTwoNewIdentifierNonUsable(int newid1, int newid2, boolean differed) throws AccessException,
	ClassNotFoundException, IOException {
		IdentifierPassword idpwAsker = AccessDataMKEventListener
				.getIdentifierPassword(AccessDataMKEventListener.getCustumHostIdentifier(0), newid1);
		identifierPassordsAsker.add(idpwAsker);
		ArrayList<Identifier> addedForAsker = new ArrayList<>();
		//ArrayList<Identifier> addedForReceiver = new ArrayList<>();
		addedForAsker.add(idpwAsker.getIdentifier());

		idpwAsker = AccessDataMKEventListener
				.getIdentifierPassword(AccessDataMKEventListener.getCustumHostIdentifier(0), newid2);
		identifierPassordsAsker.add(idpwAsker);
		addedForAsker.add(idpwAsker.getIdentifier());

		if (differed)
			testDifferedAddingNewIdentifier(addedForAsker/*, addedForReceiver*/);
		else
			testAddingNewIdentifier(addedForAsker/*, addedForReceiver*/);
}	

	private void testRemovingOneNewIdentifier(int newid) throws AccessException, ClassNotFoundException, IOException {
		IdentifierPassword idpwAsker = AccessDataMKEventListener
				.getIdentifierPassword(AccessDataMKEventListener.getCustumHostIdentifier(0), newid);
		IdentifierPassword idpwReceiver = AccessDataMKEventListener
				.getIdentifierPassword(AccessDataMKEventListener.getCustumHostIdentifier(1), newid);
		identifierPassordsAsker.remove(idpwAsker);
		identifierPassordsReceiver.remove(idpwReceiver);
		acceptedAskerIdentifiers.remove(idpwAsker.getIdentifier());
		acceptedReceiverIdentifiers.remove(idpwReceiver.getIdentifier());
		ArrayList<Identifier> addedForAsker = new ArrayList<>();
		//ArrayList<Identifier> addedForReceiver = new ArrayList<>();
		addedForAsker.add(idpwAsker.getIdentifier());
		//addedForReceiver.add(idpwReceiver.getIdentifier());
		testRemovingNewIdentifier(addedForAsker/*, addedForReceiver*/);
	}

	private void testAddingNewIdentifier(ArrayList<Identifier> addedForAsker/*, ArrayList<Identifier> addedForReceiver*/)
			throws AccessException, ClassNotFoundException, IOException {
		Assert.assertTrue(apasker.isAccessFinalized());
		Assert.assertTrue(apreceiver.isAccessFinalized());

		AccessMessage[] masker = getAccessMessages(apasker.setAndGetNextMessage(new NewLocalLoginAddedMessage(addedForAsker)));
		AccessMessage[] mreceiver = new AccessMessage[0];
		testSubNewAddingRemovingIdentifier(masker, mreceiver);

	}

	private void testRemovingNewIdentifier(ArrayList<Identifier> addedForAsker/*, ArrayList<Identifier> addedForReceiver*/)
			throws AccessException, ClassNotFoundException, IOException {
		Assert.assertTrue(apasker.isAccessFinalized());
		Assert.assertTrue(apreceiver.isAccessFinalized());

		AccessMessage[] masker = getAccessMessages(apasker.setAndGetNextMessage(new NewLocalLoginRemovedMessage(addedForAsker)));
		AccessMessage[] mreceiver = new AccessMessage[0];
		testSubNewAddingRemovingIdentifier(masker, mreceiver);
	}

	private void testAddingTwoNewIdentifier(int newid1, int newid2, boolean differed) throws AccessException,
			ClassNotFoundException, IOException {
		HostIdentifier hostIDAsker=AccessDataMKEventListener.getCustumHostIdentifier(0);
		HostIdentifier hostIDReceiver=AccessDataMKEventListener.getCustumHostIdentifier(1);
		IdentifierPassword idpwAsker = AccessDataMKEventListener
				.getIdentifierPassword(hostIDAsker, newid1);
		IdentifierPassword idpwReceiver = AccessDataMKEventListener
				.getIdentifierPassword(hostIDReceiver, newid1);

		identifierPassordsAsker.add(idpwAsker);
		acceptedAskerIdentifiers.add(idpwAsker.getIdentifier());
		identifierPassordsReceiver.add(idpwReceiver);
		acceptedReceiverIdentifiers.add(idpwReceiver.getIdentifier());


		hostIDAsker=AccessDataMKEventListener.getCustumHostIdentifier(0);
		hostIDReceiver=AccessDataMKEventListener.getCustumHostIdentifier(1);

		ArrayList<Identifier> addedForAsker = new ArrayList<>();
		//ArrayList<Identifier> addedForReceiver = new ArrayList<>();
		addedForAsker.add(idpwAsker.getIdentifier());
		//addedForReceiver.add(idpwReceiver.getIdentifier());
		idpwAsker = AccessDataMKEventListener
				.getIdentifierPassword(hostIDAsker, newid2);
		idpwReceiver = AccessDataMKEventListener
				.getIdentifierPassword(hostIDReceiver, newid2);
		identifierPassordsAsker.add(idpwAsker);
		acceptedAskerIdentifiers.add(idpwAsker.getIdentifier());
		identifierPassordsReceiver.add(idpwReceiver);
		acceptedReceiverIdentifiers.add(idpwReceiver.getIdentifier());

		addedForAsker.add(idpwAsker.getIdentifier());
		//addedForReceiver.add(idpwReceiver.getIdentifier());

		if (differed)
			testDifferedAddingNewIdentifier(addedForAsker/*, addedForReceiver*/);
		else
			testAddingNewIdentifier(addedForAsker/*, addedForReceiver*/);
	}

	private void testRemovingTwoNewIdentifier(int newid1, int newid2, boolean differed) throws AccessException,
			ClassNotFoundException, IOException {
		IdentifierPassword idpwAsker = AccessDataMKEventListener
				.getIdentifierPassword(AccessDataMKEventListener.getCustumHostIdentifier(0), newid1);
		IdentifierPassword idpwReceiver = AccessDataMKEventListener
				.getIdentifierPassword(AccessDataMKEventListener.getCustumHostIdentifier(1), newid1);
		identifierPassordsAsker.remove(idpwAsker);
		identifierPassordsReceiver.remove(idpwReceiver);
		acceptedAskerIdentifiers.remove(idpwAsker.getIdentifier());
		acceptedReceiverIdentifiers.remove(idpwReceiver.getIdentifier());
		ArrayList<Identifier> addedForAsker = new ArrayList<>();
		//ArrayList<Identifier> addedForReceiver = new ArrayList<>();
		addedForAsker.add(idpwAsker.getIdentifier());
		//addedForReceiver.add(idpwReceiver.getIdentifier());
		idpwAsker = AccessDataMKEventListener
				.getIdentifierPassword(AccessDataMKEventListener.getCustumHostIdentifier(0), newid2);
		idpwReceiver = AccessDataMKEventListener
				.getIdentifierPassword(AccessDataMKEventListener.getCustumHostIdentifier(1), newid2);
		identifierPassordsAsker.remove(idpwAsker);
		identifierPassordsReceiver.remove(idpwReceiver);
		acceptedAskerIdentifiers.remove(idpwAsker.getIdentifier());
		acceptedReceiverIdentifiers.remove(idpwReceiver.getIdentifier());
		addedForAsker.add(idpwAsker.getIdentifier());
		//addedForReceiver.add(idpwReceiver.getIdentifier());

		if (differed)
			testDifferedRemovingNewIdentifier(addedForAsker/*, addedForReceiver*/);
		else
			testRemovingNewIdentifier(addedForAsker/*, addedForReceiver*/);
	}

	private void testSubNewAddingRemovingIdentifier(AccessMessage[] maskerl, AccessMessage[] mreceiverl)
			throws ClassNotFoundException, IOException, AccessException {
		int cycles = 0;
		do {

			for (AccessMessage masker : maskerl)
			{
				if (masker != null && !(masker instanceof DoNotSendMessage)) {
					masker = (AccessMessage) ConnectionsProtocolsTests
							.unserialize(ConnectionsProtocolsTests.serialize(masker));
					mreceiverl = getAccessMessages(apreceiver.setAndGetNextMessage(masker), mreceiverl);
				}
			}
			maskerl = new AccessMessage[0];
			for (AccessMessage mreceiver : mreceiverl)
			{
				if (mreceiver == null) {
					mreceiver = apreceiver.manageDifferedAccessMessage();
				}
	
				if (mreceiver != null && !(mreceiver instanceof DoNotSendMessage)) {
					mreceiver = (AccessMessage) ConnectionsProtocolsTests
							.unserialize(ConnectionsProtocolsTests.serialize(mreceiver));
					maskerl = getAccessMessages(apasker.setAndGetNextMessage(mreceiver), maskerl);
				}
			}
			mreceiverl = new AccessMessage[0];
			if (maskerl.length==0) {
				maskerl = getAccessMessages(apasker.manageDifferedAccessMessage());
			}
			cycles++;
		} while (maskerl.length > 0 && cycles < numberMaxExchange);
		Assert.assertTrue(cycles < numberMaxExchange);
		//Assert.assertTrue(maskerl.length == 0 && mreceiverl.length==0);
		Assert.assertTrue(apreceiver.isAccessFinalizedMessage());
		Assert.assertTrue(apasker.isAccessFinalizedMessage());
		Assert.assertTrue(apreceiver.isAccessFinalized());
		Assert.assertTrue(apasker.isAccessFinalized());
		testExpectedLogins();
	}
	private void checkExpectedLogins(AbstractAccessProtocol ap, List<Identifier> expectedAcceptedIdentifiers, List<Identifier> expectedAcceptedIdentifiersOtherSide)
	{
		Assert.assertEquals(expectedAcceptedIdentifiers.size(), ap.getAllAcceptedIdentifiers().size());
		for (PairOfIdentifiers poi : ap.getAllAcceptedIdentifiers()) {
			boolean found = false;
			for (Identifier id : expectedAcceptedIdentifiers) {
				if (id.equals(poi.getLocalIdentifier())) {
					found = true;
					break;
				}
			}
			Assert.assertTrue(""+poi, found);
			found = false;
			for (Identifier id : expectedAcceptedIdentifiersOtherSide) {
				if (id.equals(poi.getDistantIdentifier())) {
					found = true;
					break;
				}
			}

			Assert.assertTrue(""+poi, found);
			/*if (!poi.getDistantIdentifier().getCloudIdentifier().isAutoIdentifiedCloudWithPublicKey())
				Assert.assertTrue(""+poi.getDistantIdentifier(), found);
			else
				Assert.assertTrue(""+poi.getDistantIdentifier(), !found);*/
		}
		for (Identifier id : expectedAcceptedIdentifiers) {
			boolean found=false;
			for (PairOfIdentifiers poi : ap.getAllAcceptedIdentifiers()) {
				if(poi.getLocalIdentifier().equals(id))
					found=true;
			}
			if (!found)
				Assert.fail("Impossible to found : "+id);
		}


	}
	private void testExpectedLogins() {

		checkExpectedLogins(apasker, acceptedAskerIdentifiers, acceptedReceiverIdentifiers);
		checkExpectedLogins(apreceiver, acceptedReceiverIdentifiers, acceptedAskerIdentifiers);


	}

	private void testDifferedAddingNewIdentifier(ArrayList<Identifier> addedForAsker/*,
			ArrayList<Identifier> addedForReceiver*/) throws AccessException, ClassNotFoundException, IOException
	{
		Assert.assertTrue(apasker.isAccessFinalized());
		Assert.assertTrue(apreceiver.isAccessFinalized());

		AccessMessage[] masker = new AccessMessage[0];
		for (Identifier id : addedForAsker) {
			ArrayList<Identifier> l = new ArrayList<>();
			l.add(id);
			AccessMessage[] am = getAccessMessages(apasker.setAndGetNextMessage(new NewLocalLoginAddedMessage(l)));
			if (masker.length==0)
				Assert.assertTrue((masker = am).length>0);
			else
				Assert.assertEquals(0, am.length);
		}
		AccessMessage[] mreceiver = new AccessMessage[0];
		testSubNewAddingRemovingIdentifier(masker, mreceiver);
	}

	private void testDifferedRemovingNewIdentifier(ArrayList<Identifier> addedForAsker/*,
			ArrayList<Identifier> addedForReceiver*/) throws AccessException, ClassNotFoundException, IOException
			{
		Assert.assertTrue(apasker.isAccessFinalized());
		Assert.assertTrue(apreceiver.isAccessFinalized());

		AccessMessage[] masker = new AccessMessage[0];
		for (Identifier id : addedForAsker) {
			ArrayList<Identifier> l = new ArrayList<>();
			l.add(id);
			AccessMessage[] am = getAccessMessages(apasker.setAndGetNextMessage(new NewLocalLoginRemovedMessage(l)));
			if (masker.length==0)
				Assert.assertTrue((masker = am).length>0);
			else
				Assert.assertEquals(""+ Arrays.toString(am), 0, am.length);
		}
		AccessMessage[] mreceiver = new AccessMessage[0];
		testSubNewAddingRemovingIdentifier(masker, mreceiver);
	}

	@Override
	public void notifyNewAccessChangements() {
	}

	@Override
	public void addingIdentifier(Identifier _identifier) {

	}

	@Override
	public void addingIdentifiers(Collection<Identifier> _identifiers) {

	}

	@Override
	public void removingIdentifier(Identifier _identifier) {

	}

	@Override
	public void removingIdentifiers(Collection<Identifier> _identifiers) {

	}
}
