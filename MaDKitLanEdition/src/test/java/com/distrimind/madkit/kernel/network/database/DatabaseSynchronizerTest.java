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

import com.distrimind.madkit.kernel.*;
import com.distrimind.madkit.kernel.network.*;
import com.distrimind.madkit.kernel.network.connection.access.AbstractAccessProtocolProperties;
import com.distrimind.madkit.kernel.network.connection.access.AccessProtocolWithP2PAgreementProperties;
import com.distrimind.madkit.kernel.network.connection.access.IdentifierPassword;
import com.distrimind.madkit.kernel.network.connection.access.LoginData;
import com.distrimind.madkit.kernel.network.connection.secured.P2PSecuredConnectionProtocolWithKeyAgreementProperties;
import com.distrimind.ood.database.DatabaseConfiguration;
import com.distrimind.ood.database.DatabaseWrapper;
import com.distrimind.ood.database.exceptions.DatabaseException;
import com.distrimind.util.DecentralizedIDGenerator;
import com.distrimind.util.DecentralizedValue;
import com.distrimind.util.FileTools;
import com.distrimind.util.crypto.SymmetricAuthentifiedSignatureType;
import com.distrimind.util.crypto.SymmetricEncryptionType;
import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
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
public class DatabaseSynchronizerTest extends JunitMadkit{
	final NetworkEventListener eventListener1;
	final NetworkEventListener eventListener2;
	final DecentralizedValue localIdentifier;
	final DecentralizedValue localIdentifierOtherSide;
	final LoginData loginData1, loginData2;
	final File databaseFile1, databaseFile2;

	public DatabaseSynchronizerTest() throws UnknownHostException {
		P2PSecuredConnectionProtocolWithKeyAgreementProperties p2pprotocol=new P2PSecuredConnectionProtocolWithKeyAgreementProperties();
		p2pprotocol.isServer = true;
		p2pprotocol.symmetricEncryptionType= SymmetricEncryptionType.AES_CTR;
		p2pprotocol.symmetricSignatureType= SymmetricAuthentifiedSignatureType.HMAC_SHA2_256;
		databaseFile1=new File("tmpDatabaseFile1");
		databaseFile2=new File("tmpDatabaseFile2");
		if(databaseFile1.exists())
			FileTools.deleteDirectory(databaseFile1);
		if(databaseFile2.exists())
			FileTools.deleteDirectory(databaseFile2);

		AbstractAccessProtocolProperties app = new AccessProtocolWithP2PAgreementProperties();
		ArrayList<IdentifierPassword> idpws=AccessDataMKEventListener
				.getClientOrPeerToPeerLogins(AccessDataMKEventListener.getCustomHostIdentifier(0), 4);

		loginData1=AccessDataMKEventListener.getDefaultLoginData(
					idpws,
				null, JunitMadkit.NETWORK_GROUP_FOR_LOGIN_DATA, true, new Runnable() {

					@Override
					public void run() {
						Assert.fail();
					}
				},new Runnable() {

					@Override
					public void run() {
						Assert.fail();
					}
				});
		localIdentifier=loginData1.getDecentralizedDatabaseID(idpws.get(0).getIdentifier());
		this.eventListener1 = new NetworkEventListener(true, false, false, databaseFile1,
				new ConnectionsProtocolsMKEventListener(p2pprotocol), new AccessProtocolPropertiesMKEventListener(app),
				new AccessDataMKEventListener(loginData1), 5000,
				Collections.singletonList((AbstractIP) new DoubleIP(5000, (Inet4Address) InetAddress.getByName("127.0.0.1"),
						(Inet6Address) InetAddress.getByName("::1"))),
				InetAddress.getByName("0.0.0.0")) {

			@Override
			public void onMadkitPropertiesLoaded(MadkitProperties _properties) {
				super.onMadkitPropertiesLoaded(_properties);
				_properties.networkProperties.networkLogLevel = Level.INFO;
				_properties.networkProperties.maxBufferSize=Short.MAX_VALUE*4;

			}
		};

		P2PSecuredConnectionProtocolWithKeyAgreementProperties u = new P2PSecuredConnectionProtocolWithKeyAgreementProperties();
		u.isServer = false;
		u.symmetricEncryptionType=SymmetricEncryptionType.AES_CTR;
		u.symmetricSignatureType= SymmetricAuthentifiedSignatureType.HMAC_SHA2_256;

		app = new AccessProtocolWithP2PAgreementProperties();
		idpws=AccessDataMKEventListener
				.getClientOrPeerToPeerLogins(AccessDataMKEventListener.getCustomHostIdentifier(1), 4);
		loginData2=AccessDataMKEventListener.getDefaultLoginData(
				idpws,
				null, JunitMadkit.NETWORK_GROUP_FOR_LOGIN_DATA, true, new Runnable() {

					@Override
					public void run() {
						Assert.fail();
					}
				},new Runnable() {

					@Override
					public void run() {
						Assert.fail();
					}
				});
		this.eventListener2 = new NetworkEventListener(true, false, false, databaseFile2,
				new ConnectionsProtocolsMKEventListener(u), new AccessProtocolPropertiesMKEventListener(app),
				new AccessDataMKEventListener(loginData2), 5000,
				Collections.singletonList((AbstractIP) new DoubleIP(5000, (Inet4Address) InetAddress.getByName("127.0.0.1"),
						(Inet6Address) InetAddress.getByName("::1"))),
				InetAddress.getByName("0.0.0.0")) {

			@Override
			public void onMadkitPropertiesLoaded(MadkitProperties _properties) {
				super.onMadkitPropertiesLoaded(_properties);
				_properties.networkProperties.networkLogLevel = Level.INFO;
				_properties.networkProperties.maxBufferSize=Short.MAX_VALUE*4;


			}
		};
		localIdentifierOtherSide=loginData1.getDecentralizedDatabaseID(idpws.get(0).getIdentifier());
	}



	private static class DatabaseAgent extends Agent
	{
		final DecentralizedValue localIdentifier;
		final DecentralizedValue localIdentifierOtherSide;
		final ArrayList<Table1.Record> myListToAdd;
		final ArrayList<Table1.Record> otherListToAdd;
		final AtomicReference<Boolean> finished;


		public DatabaseAgent(DecentralizedValue localIdentifier, DecentralizedValue localIdentifierOtherSide, ArrayList<Table1.Record> myListToAdd, ArrayList<Table1.Record> otherListToAdd, AtomicReference<Boolean> finished) {
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
				wrapper.loadDatabase(new DatabaseConfiguration(Table1.class.getPackage()), true);
				Assert.assertNull(getMadkitConfig().getLocalDatabaseHostIDString());
				setIfNotPresentLocalDatabaseHostIdentifier(localIdentifier, Table1.class.getPackage());
				sleep(100);
				Assert.assertEquals(localIdentifier, wrapper.getSynchronizer().getLocalHostID());
				Assert.assertFalse(wrapper.getSynchronizer().isPairedWith(localIdentifierOtherSide));
				addOrConfigureDistantDatabaseHost(localIdentifierOtherSide, true, Table1.class.getPackage());
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
				Table1 table=wrapper.getTableInstance(Table1.class);
				int total=0;
				while(total<myListToAdd.size())
				{
					nb=(int)(Math.random()*(myListToAdd.size()-total)+1);

					for (int j=total;j<nb+total;j++)
					{
						table.addRecord(myListToAdd.get(j));
					}
					sleep(1000);
				}
				if (checkDistantRecords(this, table, otherListToAdd, finished))
				{
					return;
				}
				total=0;
				while(total<myListToAdd.size())
				{
					nb=(int)(Math.random()*(myListToAdd.size()-total)+1);

					for (int j=total;j<nb+total;j++)
					{
						Table1.Record r=myListToAdd.get(j);
						r.setValue("value"+Math.random());
						table.updateRecord(r);
					}
					sleep(1000);
				}
				if (checkDistantRecords(this, table, otherListToAdd, finished))
				{
					return;
				}
				finished.set(true);
			} catch (DatabaseException | InterruptedException e) {
				e.printStackTrace();
			}
			catch(AssertionError e)
			{

				finished.set(false);
				throw e;
			}
		}
	}
	private static boolean checkDistantRecords(AbstractAgent agent, Table1 table, ArrayList<Table1.Record> otherListToAdd, AtomicReference<Boolean> finished) throws DatabaseException, InterruptedException {
		ArrayList<Table1.Record> l=new ArrayList<>(otherListToAdd);
		int nb=0;
		do {
			for (Iterator<Table1.Record> it=l.iterator();it.hasNext();)
			{
				Table1.Record r=it.next();
				Table1.Record r2=table.getRecord("decentralizedID", r.getDecentralizedID());
				if (r2!=null) {
					Assert.assertEquals(r2.getDecentralizedID(), r.getDecentralizedID());
					Assert.assertEquals(r2.getValue(), r.getValue());
					it.remove();
				}
			}
			if (l.size()>0)
				agent.sleep(1000);
			++nb;
		} while(l.size()>0 && nb<10);
		if (nb==10) {

			finished.set(false);
			return true;
		}
		return false;
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
				sleep(1500);
				DatabaseWrapper wrapper=getMadkitConfig().getDatabaseWrapper();
				Assert.assertNotNull(wrapper);
				wrapper.loadDatabase(new DatabaseConfiguration(Table1.class.getPackage()), true);
				Assert.assertNotNull(getMadkitConfig().getLocalDatabaseHostIDString());
				Assert.assertEquals(wrapper.getSynchronizer().getLocalHostID(), localIdentifier);
				Assert.assertTrue(wrapper.getSynchronizer().isPairedWith(localIdentifierOtherSide));
				int nb=0;
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
				Table1 table=wrapper.getTableInstance(Table1.class);
				if (checkDistantRecords(this, table, otherListToAdd, finished))
				{
					return;
				}
				sleep(1000);
				removeDistantDatabaseHostFromDatabaseSynchronizer(localIdentifierOtherSide, Table1.class.getPackage());
				sleep(100);
				Assert.assertFalse(wrapper.getSynchronizer().isPairedWith(localIdentifierOtherSide));
				nb=0;
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
				resetDatabaseSynchronizer();
				sleep(100);
				Assert.assertNull(getMadkitConfig().getLocalDatabaseHostIDString());
				finished.set(true);
			} catch (DatabaseException | InterruptedException e) {
				e.printStackTrace();
			}
			catch(AssertionError e)
			{

				finished.set(false);
				throw e;
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
				wrapper.loadDatabase(new DatabaseConfiguration(Table1.class.getPackage()), true);
				Assert.assertNull(getMadkitConfig().getLocalDatabaseHostIDString());
				finished.set(true);
			} catch (DatabaseException e) {
				e.printStackTrace();
			}
			catch(AssertionError e)
			{

				finished.set(false);
				throw e;
			}
		}
	}


	@Test
	public void testDatabaseSynchronization() {

		final AtomicReference<Boolean> finished1=new AtomicReference<>(null);
		final AtomicReference<Boolean> finished2=new AtomicReference<>(null);
		// addMadkitArgs("--kernelLogLevel",Level.INFO.toString(),"--networkLogLevel",Level.FINEST.toString());
		launchTest(new AbstractAgent() {
			@Override
			protected void end() {

			}

			@Override
			protected void activate() throws InterruptedException {

				ArrayList<Table1.Record> recordsToAdd=getRecordsToAdd();
				ArrayList<Table1.Record> recordsToAddOtherSide=getRecordsToAdd();
				AbstractAgent agentChecker=new DatabaseAgent(localIdentifier, localIdentifierOtherSide, recordsToAdd, recordsToAddOtherSide, finished1);
				launchThreadedMKNetworkInstance(Level.INFO, AbstractAgent.class, agentChecker, eventListener1);
				sleep(600);
				AbstractAgent agentCheckerOtherSide=new DatabaseAgent(localIdentifierOtherSide, localIdentifier, recordsToAddOtherSide, recordsToAdd, finished2);
				launchThreadedMKNetworkInstance(Level.INFO, AbstractAgent.class, agentCheckerOtherSide, eventListener2);

				while(finished1.get()==null || finished1.get()==null)
				{

					sleep(1000);
				}
				Assert.assertTrue(finished1.get());
				Assert.assertTrue(finished2.get());
				cleanHelperMDKs(this);
				Assert.assertEquals(getHelperInstances(0).size(), 0);
				finished1.set(null);
				finished2.set(null);
				agentChecker=new SecondConnexionAgent(localIdentifier, localIdentifierOtherSide, recordsToAdd, recordsToAddOtherSide, finished1);
				launchThreadedMKNetworkInstance(Level.INFO, AbstractAgent.class, agentChecker, eventListener1);
				sleep(400);
				agentCheckerOtherSide=new SecondConnexionAgent(localIdentifierOtherSide, localIdentifier, recordsToAddOtherSide, recordsToAdd, finished2);
				launchThreadedMKNetworkInstance(Level.INFO, AbstractAgent.class, agentCheckerOtherSide, eventListener2);

				while(finished1.get()==null || finished1.get()==null)
				{

					sleep(1000);
				}
				Assert.assertTrue(finished1.get());
				Assert.assertTrue(finished2.get());
				cleanHelperMDKs(this);
				Assert.assertEquals(getHelperInstances(0).size(), 0);

				finished1.set(null);
				finished2.set(null);
				agentChecker=new ThirdConnexionAgent(localIdentifier, finished1);
				launchThreadedMKNetworkInstance(Level.INFO, AbstractAgent.class, agentChecker, eventListener1);
				sleep(400);
				agentCheckerOtherSide=new ThirdConnexionAgent(localIdentifierOtherSide, finished2);
				launchThreadedMKNetworkInstance(Level.INFO, AbstractAgent.class, agentCheckerOtherSide, eventListener2);

				while(finished1.get()==null || finished1.get()==null)
				{

					sleep(1000);
				}
				Assert.assertTrue(finished1.get());
				Assert.assertTrue(finished2.get());
				cleanHelperMDKs(this);
				Assert.assertEquals(getHelperInstances(0).size(), 0);

			}
		});
		if(databaseFile1.exists())
			FileTools.deleteDirectory(databaseFile1);
		if(databaseFile2.exists())
			FileTools.deleteDirectory(databaseFile2);

		Assert.assertTrue(finished1.get());
		Assert.assertTrue(finished2.get());

		cleanHelperMDKs();
	}

}
