package com.distrimind.madkit.kernel.network.database;
/*
Copyright or © or Copr. Jason Mahdjoub (01/04/2013)

jason.mahdjoub@distri-mind.fr

This software (Object Oriented Database (OOD)) is a computer program 
whose purpose is to manage a local database with the object paradigm 
and the java langage 

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
import com.distrimind.madkit.kernel.Agent;
import com.distrimind.madkit.kernel.JunitMadkit;
import com.distrimind.madkit.kernel.MadkitProperties;
import com.distrimind.madkit.kernel.network.*;
import com.distrimind.madkit.kernel.network.connection.access.*;
import com.distrimind.madkit.kernel.network.connection.secured.P2PSecuredConnectionProtocolPropertiesWithKeyAgreement;
import com.distrimind.ood.database.DatabaseConfiguration;
import com.distrimind.ood.database.DatabaseSchema;
import com.distrimind.ood.database.DatabaseWrapper;
import com.distrimind.ood.database.exceptions.DatabaseException;
import com.distrimind.util.DecentralizedIDGenerator;
import com.distrimind.util.DecentralizedValue;
import com.distrimind.util.FileTools;
import com.distrimind.util.crypto.*;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.io.File;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;


/**
 * @author Jason Mahdjoub
 * @version 1.0
 * @since MaDKitLanEdition 2.0.0
 */
@RunWith(Parameterized.class)
public class MKDatabaseSynchronizerTest extends JunitMadkit{
	private static class EncryptionProfileCollection extends com.distrimind.util.crypto.EncryptionProfileCollection
	{
		public EncryptionProfileCollection() {
			super();
		}
	}



	final NetworkEventListener eventListener1;
	final NetworkEventListener eventListener2;
	final DecentralizedValue localIdentifier;
	final DecentralizedValue localIdentifierOtherSide;
	final LoginData loginData1, loginData2;
	final File databaseFile1, databaseFile2;
	final SymmetricSecretKey secretKeyForSignature1,secretKeyForSignature2;
	final AbstractSecureRandom random;

	@Parameterized.Parameters
	public static Object[][] data() {
		return new Object[10][0];
	}

	public MKDatabaseSynchronizerTest() throws UnknownHostException, DatabaseException, NoSuchAlgorithmException, NoSuchProviderException {
		random=SecureRandomType.DEFAULT.getInstance(null);
		secretKeyForSignature1=SymmetricAuthenticatedSignatureType.HMAC_SHA2_384.getKeyGenerator(random).generateKey();
		secretKeyForSignature2=SymmetricAuthenticatedSignatureType.HMAC_SHA2_384.getKeyGenerator(random).generateKey();
		P2PSecuredConnectionProtocolPropertiesWithKeyAgreement p2pprotocol=new P2PSecuredConnectionProtocolPropertiesWithKeyAgreement();
		p2pprotocol.isServer = true;
		p2pprotocol.symmetricEncryptionType= SymmetricEncryptionType.AES_CTR;
		p2pprotocol.symmetricSignatureType= SymmetricAuthenticatedSignatureType.HMAC_SHA2_384;
		databaseFile1=new File("tmpDatabaseFile1");
		databaseFile2=new File("tmpDatabaseFile2");
		if(databaseFile1.exists())
			FileTools.deleteDirectory(databaseFile1);
		if(databaseFile2.exists())
			FileTools.deleteDirectory(databaseFile2);

		AbstractAccessProtocolProperties app = new AccessProtocolWithP2PAgreementProperties();
		ArrayList<IdentifierPassword> idpws=AccessDataMKEventListener
				.getClientOrPeerToPeerLogins(AccessDataMKEventListener.getCustomHostIdentifier(0), 4);

		ListGroupsRoles defaultGroupAccess=new ListGroupsRoles();

		defaultGroupAccess.addGroupsRoles(JunitMadkit.DEFAULT_NETWORK_GROUP_FOR_ACCESS_DATA);

		EncryptionProfileCollection encryptionProfileCollectionForP2PSignature1=new EncryptionProfileCollection();
		encryptionProfileCollectionForP2PSignature1.putProfile((short)1, null, null, null, secretKeyForSignature1, null, false, true);
		encryptionProfileCollectionForP2PSignature1.putProfile((short)2, null, null, null, secretKeyForSignature2, null, false, true);

		loginData1=AccessDataMKEventListener.getDefaultLoginData(
					idpws,
				null, defaultGroupAccess, true, Assert::fail, Assert::fail);
		localIdentifier=loginData1.getDecentralizedDatabaseID(idpws.get(0).getIdentifier(), null);
		this.eventListener1 = new NetworkEventListener(true, false, false,
				databaseFile1,null, null, encryptionProfileCollectionForP2PSignature1,
				SecureRandomType.DEFAULT,
				new ConnectionsProtocolsMKEventListener(p2pprotocol), new AccessProtocolPropertiesMKEventListener(app),
				new AccessDataMKEventListener(loginData1), 5000,
				Collections.singletonList(new DoubleIP(5000, (Inet4Address) InetAddress.getByName("127.0.0.1"),
						(Inet6Address) InetAddress.getByName("::1"))),
				InetAddress.getByName("0.0.0.0")) {

			@Override
			public void onMaDKitPropertiesLoaded(MadkitProperties _properties) {
				super.onMaDKitPropertiesLoaded(_properties);
				_properties.networkProperties.networkLogLevel = Level.INFO;
				_properties.networkProperties.maxBufferSize=Short.MAX_VALUE*4;


			}
		};

		P2PSecuredConnectionProtocolPropertiesWithKeyAgreement u = new P2PSecuredConnectionProtocolPropertiesWithKeyAgreement();
		u.isServer = false;
		u.symmetricEncryptionType=SymmetricEncryptionType.AES_CTR;
		u.symmetricSignatureType= SymmetricAuthenticatedSignatureType.HMAC_SHA2_384;

		app = new AccessProtocolWithP2PAgreementProperties();
		idpws=AccessDataMKEventListener
				.getClientOrPeerToPeerLogins(AccessDataMKEventListener.getCustomHostIdentifier(1), 4);

		EncryptionProfileCollection encryptionProfileCollectionForP2PSignature2=new EncryptionProfileCollection();
		encryptionProfileCollectionForP2PSignature2.putProfile((short)1, null, null, null, secretKeyForSignature1, null, false, true);
		encryptionProfileCollectionForP2PSignature2.putProfile((short)2, null, null, null, secretKeyForSignature2, null, false, true);

		loginData2=AccessDataMKEventListener.getDefaultLoginData(
				idpws,
				null, defaultGroupAccess, true, Assert::fail, Assert::fail);
		this.eventListener2 = new NetworkEventListener(true, false, false, databaseFile2,
				null,null, encryptionProfileCollectionForP2PSignature2,SecureRandomType.DEFAULT,
				new ConnectionsProtocolsMKEventListener(u), new AccessProtocolPropertiesMKEventListener(app),
				new AccessDataMKEventListener(loginData2), 5000,
				Collections.singletonList(new DoubleIP(5000, (Inet4Address) InetAddress.getByName("127.0.0.1"),
						(Inet6Address) InetAddress.getByName("::1"))),
				InetAddress.getByName("0.0.0.0")) {

			@Override
			public void onMaDKitPropertiesLoaded(MadkitProperties _properties) {
				super.onMaDKitPropertiesLoaded(_properties);
				_properties.networkProperties.networkLogLevel = Level.INFO;
				_properties.networkProperties.maxBufferSize=Short.MAX_VALUE*4;


			}
		};
		localIdentifierOtherSide=loginData2.getDecentralizedDatabaseID(idpws.get(0).getIdentifier(), null);
	}



	private static class DatabaseAgent extends Agent
	{
		final DecentralizedValue localIdentifier;
		final DecentralizedValue localIdentifierOtherSide;
		final ArrayList<Table1.Record> myListToAdd;
		final ArrayList<Table1.Record> otherListToAdd;
		final AtomicReference<Boolean> finished;
		final boolean integrator;



		public DatabaseAgent(DecentralizedValue localIdentifier, DecentralizedValue localIdentifierOtherSide, ArrayList<Table1.Record> myListToAdd, ArrayList<Table1.Record> otherListToAdd, AtomicReference<Boolean> finished, boolean isIntegrator) {
			this.localIdentifier = localIdentifier;
			this.localIdentifierOtherSide = localIdentifierOtherSide;
			this.myListToAdd = myListToAdd;
			this.otherListToAdd = otherListToAdd;
			this.finished = finished;
			integrator=isIntegrator;
		}

		@Override
		protected void liveCycle() {
			try {
				getMadkitConfig().getDatabaseWrapper().setNetworkLogLevel(Level.FINEST);
				sleep(2500);
				DatabaseWrapper wrapper=getMadkitConfig().getDatabaseWrapper();
				Assert.assertNotNull(wrapper);
				Assert.assertNull(getMadkitConfig().getDatabaseWrapper().getDatabaseConfigurationsBuilder().getConfigurations().getLocalPeer());
				Assert.assertNull(getMadkitConfig().getDatabaseWrapper().getDatabaseConfigurationsBuilder().getConfigurations().getLocalPeerString());
				wrapper.getDatabaseConfigurationsBuilder()
						.setLocalPeerIdentifier(localIdentifier, true, false)
						.addConfiguration(
							new DatabaseConfiguration(new DatabaseSchema(Table1.class.getPackage()), DatabaseConfiguration.SynchronizationType.DECENTRALIZED_SYNCHRONIZATION),false, true )
						.commit();
				Assert.assertFalse(wrapper.getSynchronizer().isInitialized(localIdentifierOtherSide));

				Assert.assertNotNull(getMadkitConfig().getDatabaseWrapper().getDatabaseConfigurationsBuilder().getConfigurations().getLocalPeer());
				Assert.assertNotNull(getMadkitConfig().getDatabaseWrapper().getDatabaseConfigurationsBuilder().getConfigurations().getLocalPeerString());
				sleep(300);
				Assert.assertEquals(localIdentifier, wrapper.getSynchronizer().getLocalHostID());

				if (integrator) {
					wrapper.getDatabaseConfigurationsBuilder()
							.synchronizeDistantPeersWithGivenAdditionalPackages(Collections.singletonList(localIdentifierOtherSide), Table1.class.getPackage().getName())
							.commit();

					DatabaseConfiguration dc = wrapper.getDatabaseConfigurationsBuilder().getDatabaseConfiguration(Table1.class.getPackage());
					Assert.assertEquals(DatabaseConfiguration.SynchronizationType.DECENTRALIZED_SYNCHRONIZATION, dc.getSynchronizationType());
					Assert.assertNotNull(dc.getDistantPeersThatCanBeSynchronizedWithThisDatabase());
					Assert.assertTrue(dc.getDistantPeersThatCanBeSynchronizedWithThisDatabase().contains(localIdentifierOtherSide));
				}

				sleep(100);
				Assert.assertTrue(wrapper.getSynchronizer().isInitialized());
				System.out.println("check paired");
				int nb=0;
				do {
					if (wrapper.getSynchronizer().isPairedWith(localIdentifierOtherSide))
						break;
					sleep(1000);
					++nb;
				} while(nb<10);
				if (nb==10) {
					System.err.println("Distant pair not paired");
					finished.set(false);
					return;
				}
				System.out.println("peer paired !");
				System.out.println("check pair connected");
				nb=0;
				do {
					if (wrapper.getSynchronizer().isInitialized(localIdentifierOtherSide))
						break;
					sleep(1000);
					++nb;
				} while(nb<10);
				if (nb==10) {
					System.err.println("Distant pair not initialized");
					finished.set(false);
					return;
				}
				System.out.println("peer connected !");
				System.out.println("check that database synchronization is activated with other peer");
				nb=0;
				do {
					DatabaseConfiguration dc = wrapper.getDatabaseConfigurationsBuilder().getDatabaseConfiguration(Table1.class.getPackage());
					if (DatabaseConfiguration.SynchronizationType.DECENTRALIZED_SYNCHRONIZATION==dc.getSynchronizationType()
						&& dc.getDistantPeersThatCanBeSynchronizedWithThisDatabase()!=null
						&& dc.getDistantPeersThatCanBeSynchronizedWithThisDatabase().contains(localIdentifierOtherSide))
						break;
					sleep(1000);
					++nb;
				} while(nb<10);
				if (nb==10) {
					System.err.println("database synchronization is not activated with other peer");
					finished.set(false);
					return;
				}
				Table1 table=wrapper.getTableInstance(Table1.class);
				System.out.println("add records");
				int total=0;
				while(total<myListToAdd.size())
				{
					nb=(int)(Math.random()*(myListToAdd.size()-total)+1);

					for (int j=total;j<nb+total;j++)
					{
						table.addRecord(myListToAdd.get(j));
					}
					total+=nb;
					sleep(1000);
				}
				if (checkDistantRecords(this, table, otherListToAdd, finished))
				{
					System.err.println("Distant records not synchronized");
					finished.set(false);
					return;
				}
				total=0;
				System.out.println("update records");
				while(total<myListToAdd.size())
				{
					nb=(int)(Math.random()*(myListToAdd.size()-total)+1);

					for (int j=total;j<nb+total;j++)
					{
						Table1.Record r=myListToAdd.get(j);
						r.setValue("value"+Math.random());
						table.updateRecord(r);
					}
					total+=nb;
					sleep(1000);
				}
				if (checkDistantRecords(this, table, otherListToAdd, finished))
				{
					System.err.println("Distant records not synchronized after updating records");
					finished.set(false);
					return;
				}
				finished.set(true);
			} catch (DatabaseException | InterruptedException e) {
				e.printStackTrace();
			}
			catch(Throwable e)
			{
				e.printStackTrace();
				finished.set(false);
				throw e;
			}
			finally {
				this.killAgent(this);
			}
		}
	}
	private static boolean checkDistantRecords(AbstractAgent agent, Table1 table, ArrayList<Table1.Record> otherListToAdd, AtomicReference<Boolean> finished) throws DatabaseException, InterruptedException {
		System.out.println("check synchronization");
		ArrayList<Table1.Record> l=new ArrayList<>(otherListToAdd);
		int nb=0;
		do {
			for (Iterator<Table1.Record> it=l.iterator();it.hasNext();)
			{
				Table1.Record r=it.next();
				Table1.Record r2=table.getRecord("decentralizedID", r.getDecentralizedID());
				if (r2!=null) {
					Assert.assertEquals(r2.getDecentralizedID(), r.getDecentralizedID());
					if (!r2.getValue().equals(r.getValue()))
						continue;
					it.remove();
				}
			}
			if (l.size()>0)
				agent.sleep(1000);
			++nb;
		} while(l.size()>0 && nb<10);
		return l.size()>0;
	}

	ArrayList<Table1.Record> getRecordsToAdd()
	{
		ArrayList<Table1.Record> l=new ArrayList<>();
		for (int i=0;i<10;i++)
		{
			l.add(new Table1.Record(new DecentralizedIDGenerator(), "value"+Math.random()));
		}
		return l;
	}

	private static class SecondConnexionAgent extends Agent
	{
		final DecentralizedValue localIdentifier;
		final DecentralizedValue localIdentifierOtherSide;
		final ArrayList<Table1.Record> myListToAdd;
		final ArrayList<Table1.Record> otherListToAdd;
		final AtomicReference<Boolean> finished;


		public SecondConnexionAgent(DecentralizedValue localIdentifier, DecentralizedValue localIdentifierOtherSide, ArrayList<Table1.Record> myListToAdd, ArrayList<Table1.Record> otherListToAdd, AtomicReference<Boolean> finished) {
			this.localIdentifier = localIdentifier;
			this.localIdentifierOtherSide = localIdentifierOtherSide;
			this.myListToAdd = myListToAdd;
			this.otherListToAdd = otherListToAdd;
			this.finished = finished;

		}
		@Override
		protected void liveCycle() {
			try {
				sleep(1900);
				DatabaseWrapper wrapper=getMadkitConfig().getDatabaseWrapper();
				Assert.assertNotNull(wrapper);

				wrapper.getDatabaseConfigurationsBuilder()
						.setLocalPeerIdentifier(localIdentifier, true, false)
						.addConfiguration(
							new DatabaseConfiguration(new DatabaseSchema(Table1.class.getPackage()), DatabaseConfiguration.SynchronizationType.DECENTRALIZED_SYNCHRONIZATION, Collections.singletonList(localIdentifierOtherSide)),false, false )
						.commit();
				Assert.assertNotNull(wrapper.getDatabaseConfigurationsBuilder().getConfigurations().getLocalPeer());
				Assert.assertNotNull(wrapper.getDatabaseConfigurationsBuilder().getConfigurations().getLocalPeerString());
				Assert.assertEquals(wrapper.getSynchronizer().getLocalHostID(), localIdentifier);
				Assert.assertTrue(wrapper.getSynchronizer().isPairedWith(localIdentifierOtherSide));
				sleep(100);
				Assert.assertTrue(wrapper.getSynchronizer().isInitialized());
				System.out.println("check paired");
				int nb=0;
				do {
					if (wrapper.getSynchronizer().isPairedWith(localIdentifierOtherSide))
						break;
					sleep(1000);
					++nb;
				} while(nb<10);
				if (nb==10) {

					finished.set(false);
					return;
				}
				System.out.println("peer paired !");
				System.out.println("check pair connected");
				nb=0;
				do {
					if (wrapper.getSynchronizer().isInitialized(localIdentifierOtherSide))
						break;
					sleep(1000);
					++nb;
				} while(nb<10);
				if (nb==10) {

					finished.set(false);
					return;
				}
				System.out.println("peer connected !");


				Table1 table=wrapper.getTableInstance(Table1.class);
				if (checkDistantRecords(this, table, otherListToAdd, finished))
				{
					finished.set(false);
					return;
				}
				sleep(1000);
				wrapper.getDatabaseConfigurationsBuilder()
						.desynchronizeDistantPeerWithGivenAdditionalPackages(localIdentifierOtherSide, Table1.class.getPackage().getName())
						.removeDistantPeer(localIdentifierOtherSide)
						.commit();
				sleep(100);

				nb=0;
				System.out.println("check peer disconnected ");
				do {
					if (!wrapper.getSynchronizer().isInitialized(localIdentifierOtherSide))
						break;
					sleep(1000);
					++nb;
				} while(nb<10);
				if (nb==10) {

					finished.set(false);
					return;
				}
				System.out.println("peer disconnected ");
				wrapper.getDatabaseConfigurationsBuilder()
						.resetSynchronizerAndRemoveAllHosts()
						.commit();

				sleep(3000);
				Assert.assertNull(wrapper.getDatabaseConfigurationsBuilder().getConfigurations().getLocalPeer());
				Assert.assertNull(wrapper.getDatabaseConfigurationsBuilder().getConfigurations().getLocalPeerString());
				finished.set(true);
			} catch (DatabaseException | InterruptedException e) {
				e.printStackTrace();
			}
			catch(AssertionError e)
			{
				e.printStackTrace();
				finished.set(false);
				throw e;
			}
			finally {
				this.killAgent(this);
			}
		}
	}
	private static class ThirdConnexionAgent extends Agent
	{
		final DecentralizedValue localIdentifier;
		final AtomicReference<Boolean> finished;


		public ThirdConnexionAgent(DecentralizedValue localIdentifier, AtomicReference<Boolean> finished) {
			this.localIdentifier = localIdentifier;
			this.finished = finished;

		}

		@Override
		protected void liveCycle() throws InterruptedException {
			try {
				sleep(1500);
				DatabaseWrapper wrapper=getMadkitConfig().getDatabaseWrapper();
				Assert.assertNotNull(wrapper);
				wrapper.getDatabaseConfigurationsBuilder()
						.addConfiguration(
								new DatabaseConfiguration(new DatabaseSchema(Table1.class.getPackage())),false, true )
						.commit();

				Assert.assertNull(wrapper.getDatabaseConfigurationsBuilder().getConfigurations().getLocalPeer());
				Assert.assertNull(wrapper.getDatabaseConfigurationsBuilder().getConfigurations().getLocalPeerString());
				finished.set(true);
			} catch (DatabaseException e) {
				e.printStackTrace();
			}
			catch(AssertionError e)
			{

				finished.set(false);
				throw e;
			}
			finally {
				this.killAgent(this);
			}
		}
	}


	@Test
	public void testDatabaseSynchronization() {
		final AtomicReference<Boolean> finished1=new AtomicReference<>(null);
		final AtomicReference<Boolean> finished2=new AtomicReference<>(null);
		// addMadkitArgs("--kernelLogLevel",Level.INFO.toString(),"--networkLogLevel",Level.FINEST.toString());
		try {
			launchTest(new AbstractAgent() {
				@Override
				protected void end() {

				}

				@Override
				protected void activate() throws InterruptedException {
					Assert.assertFalse(databaseFile1.exists());
					Assert.assertFalse(databaseFile2.exists());
					ArrayList<Table1.Record> recordsToAdd = getRecordsToAdd();
					ArrayList<Table1.Record> recordsToAddOtherSide = getRecordsToAdd();
					AbstractAgent agentChecker = new DatabaseAgent(localIdentifier, localIdentifierOtherSide, recordsToAdd, recordsToAddOtherSide, finished1, true);
					launchThreadedMKNetworkInstance(Level.INFO, AbstractAgent.class, agentChecker, eventListener1);
					sleep(600);
					AbstractAgent agentCheckerOtherSide = new DatabaseAgent(localIdentifierOtherSide, localIdentifier, recordsToAddOtherSide, recordsToAdd, finished2, false);
					launchThreadedMKNetworkInstance(Level.INFO, AbstractAgent.class, agentCheckerOtherSide, eventListener2);

					while (finished1.get() == null || finished2.get() == null) {

						sleep(1000);
					}

					Assert.assertEquals(true, finished1.get());
					Assert.assertEquals(true, finished2.get());

					cleanHelperMDKs(this);
					Assert.assertEquals(getHelperInstances(this, 0).size(), 0);
					finished1.set(null);
					finished2.set(null);
					System.out.println("Second step");
					Assert.assertTrue(databaseFile1.exists());
					Assert.assertTrue(databaseFile2.exists());

					agentChecker = new SecondConnexionAgent(localIdentifier, localIdentifierOtherSide, recordsToAdd, recordsToAddOtherSide, finished1);
					launchThreadedMKNetworkInstance(Level.INFO, AbstractAgent.class, agentChecker, eventListener1);
					sleep(400);
					agentCheckerOtherSide = new SecondConnexionAgent(localIdentifierOtherSide, localIdentifier, recordsToAddOtherSide, recordsToAdd, finished2);
					launchThreadedMKNetworkInstance(Level.INFO, AbstractAgent.class, agentCheckerOtherSide, eventListener2);

					while (finished1.get() == null || finished2.get() == null) {

						sleep(1000);
					}

					Assert.assertEquals(true, finished1.get());
					Assert.assertEquals(true, finished2.get());
					cleanHelperMDKs(this);
					Assert.assertEquals(getHelperInstances(this, 0).size(), 0);

					finished1.set(null);
					finished2.set(null);
					System.out.println("Third step");
					agentChecker = new ThirdConnexionAgent(localIdentifier, finished1);
					launchThreadedMKNetworkInstance(Level.INFO, AbstractAgent.class, agentChecker, eventListener1);
					sleep(400);
					agentCheckerOtherSide = new ThirdConnexionAgent(localIdentifierOtherSide, finished2);
					launchThreadedMKNetworkInstance(Level.INFO, AbstractAgent.class, agentCheckerOtherSide, eventListener2);

					while (finished1.get() == null || finished2.get() == null) {

						sleep(1000);
					}

					Assert.assertEquals(finished1.get(), true);
					Assert.assertEquals(finished2.get(), true);

					cleanHelperMDKs(this);
					Assert.assertEquals(getHelperInstances(this, 0).size(), 0);

				}
			});
		}
		finally {
			if(databaseFile1.exists())
				FileTools.deleteDirectory(databaseFile1);
			if(databaseFile2.exists())
				FileTools.deleteDirectory(databaseFile2);

		}

		Assert.assertTrue(finished1.get());
		Assert.assertTrue(finished2.get());

		cleanHelperMDKs();
	}

}
