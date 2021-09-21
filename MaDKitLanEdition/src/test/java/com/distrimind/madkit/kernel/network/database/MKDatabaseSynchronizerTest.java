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
import com.distrimind.madkit.kernel.network.connection.access.*;
import com.distrimind.madkit.kernel.network.connection.secured.P2PSecuredConnectionProtocolPropertiesWithKeyAgreement;
import com.distrimind.ood.database.*;
import com.distrimind.ood.database.centraldatabaseapi.EncryptedBackupPartReferenceTable;
import com.distrimind.ood.database.exceptions.DatabaseException;
import com.distrimind.util.Bits;
import com.distrimind.util.DecentralizedIDGenerator;
import com.distrimind.util.DecentralizedValue;
import com.distrimind.util.FileTools;
import com.distrimind.util.crypto.*;
import com.distrimind.util.data_buffers.WrappedData;
import com.distrimind.util.io.*;
import org.testng.Assert;
import org.testng.AssertJUnit;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;


/**
 * @author Jason Mahdjoub
 * @version 1.0
 * @since MaDKitLanEdition 2.0.0
 */
public class MKDatabaseSynchronizerTest extends JunitMadkit{
	private static class EncryptionProfileCollection extends com.distrimind.util.crypto.EncryptionProfileCollection
	{
		public EncryptionProfileCollection() {
			super();
		}
	}
	final static File centralDatabaseFilesDirectory=new File("centralDatabaseFilesDirectory");
	static class CentralDatabaseBackupCertificate extends com.distrimind.ood.database.centraldatabaseapi.CentralDatabaseBackupCertificate
	{
		private IASymmetricPublicKey centralDatabaseBackupPublicKey, certifiedAccountPublicKey;
		private byte[] certificateIdentifier;
		private long certificateExpirationTimeUTCInMs;
		private final transient byte[] certificateExpirationTimeUTCInMsArray=new byte[8];
		private byte[] signature;
		@SuppressWarnings("unused")
		CentralDatabaseBackupCertificate()
		{

		}
		public CentralDatabaseBackupCertificate(AbstractKeyPair<?, ?> centralDatabaseBackupKeyPair, IASymmetricPublicKey certifiedAccountPublicKey, long certificateExpirationTimeUTCInMs) throws IOException, NoSuchProviderException, NoSuchAlgorithmException {
			this.centralDatabaseBackupPublicKey = centralDatabaseBackupKeyPair.getASymmetricPublicKey();
			this.certifiedAccountPublicKey = certifiedAccountPublicKey;
			this.certificateExpirationTimeUTCInMs=certificateExpirationTimeUTCInMs;
			this.certificateIdentifier=generateCertificateIdentifier();
			setCertificateExpirationTimeUTCInMsArray();
			ASymmetricAuthenticatedSignerAlgorithm signer=new ASymmetricAuthenticatedSignerAlgorithm(centralDatabaseBackupKeyPair.getASymmetricPrivateKey());
			signer.init();
			WrappedData wd=certifiedAccountPublicKey.encode();
			signer.update(wd.getBytes() );
			signer.update(certificateIdentifier);
			signer.update(certificateExpirationTimeUTCInMsArray);
			signature= signer.getSignature();
		}
		private void setCertificateExpirationTimeUTCInMsArray()
		{
			Bits.putLong(certificateExpirationTimeUTCInMsArray, 0,  certificateExpirationTimeUTCInMs);
		}


		@Override
		public Integrity isValidCertificate(long accountID, IASymmetricPublicKey externalAccountID, DecentralizedValue hostID, DecentralizedValue centralID) {

			try {
				ASymmetricAuthenticatedSignatureCheckerAlgorithm checker=new ASymmetricAuthenticatedSignatureCheckerAlgorithm(centralDatabaseBackupPublicKey);
				checker.init(signature);
				WrappedData wd=certifiedAccountPublicKey.encode();
				checker.update(wd.getBytes() );
				checker.update(certificateIdentifier);
				checker.update(certificateExpirationTimeUTCInMsArray);
				if (checker.verify())
					return Integrity.OK;
				else
					return Integrity.FAIL;
			}
			catch (MessageExternalizationException e)
			{
				return e.getIntegrity();
			}
			catch (NoSuchProviderException | NoSuchAlgorithmException | IOException e) {
				return Integrity.FAIL;
			}
		}


		@Override
		public long getCertificateExpirationTimeUTCInMs() {
			return certificateExpirationTimeUTCInMs;
		}




		@Override
		public IASymmetricPublicKey getCertifiedAccountPublicKey() {
			return certifiedAccountPublicKey;
		}

		@Override
		public byte[] getCertificateIdentifier() {
			return certificateIdentifier;
		}

		@Override
		public int getInternalSerializedSize() {
			return 8+SerializationTools.getInternalSize(centralDatabaseBackupPublicKey)
					+SerializationTools.getInternalSize(certifiedAccountPublicKey)
					+SerializationTools.getInternalSize(signature, ASymmetricAuthenticatedSignatureType.MAX_ASYMMETRIC_SIGNATURE_SIZE)
					+SerializationTools.getInternalSize(certificateIdentifier, MAX_SIZE_IN_BYTES_OF_CERTIFICATE_IDENTIFIER);
		}

		@Override
		public void writeExternal(SecuredObjectOutputStream out) throws IOException {
			out.writeObject(centralDatabaseBackupPublicKey, false);
			out.writeObject(certifiedAccountPublicKey, false);
			out.writeBytesArray(certificateIdentifier, false, MAX_SIZE_IN_BYTES_OF_CERTIFICATE_IDENTIFIER);
			out.writeLong(certificateExpirationTimeUTCInMs);
			out.writeBytesArray(signature, false, ASymmetricAuthenticatedSignatureType.MAX_ASYMMETRIC_SIGNATURE_SIZE);
		}

		@Override
		public void readExternal(SecuredObjectInputStream in) throws IOException, ClassNotFoundException {
			centralDatabaseBackupPublicKey=in.readObject(false);
			certifiedAccountPublicKey=in.readObject(false);
			certificateIdentifier=in.readBytesArray(false, MAX_SIZE_IN_BYTES_OF_CERTIFICATE_IDENTIFIER);
			certificateExpirationTimeUTCInMs=in.readLong();
			signature=in.readBytesArray(false, ASymmetricAuthenticatedSignatureType.MAX_ASYMMETRIC_SIGNATURE_SIZE);
			setCertificateExpirationTimeUTCInMsArray();
		}
	}
	public static class FileReference extends com.distrimind.ood.database.filemanager.FileReference
	{
		private transient File file;
		private static final int MAX_FILE_NAME_LENGTH=2000;

		public FileReference(File file) {
			this.file = file;
		}

		@SuppressWarnings("unused")
		private FileReference() {
		}

		@Override
		protected boolean equalsImplementation(Object o) {
			return (o==this || (o instanceof FileReference && ((FileReference) o).file.equals(file)));
		}

		@Override
		public int hashCode() {
			return file.hashCode();
		}

		@Override
		public String toString() {
			return "FileReference{" +
					"file=" + file +
					'}';
		}

		@Override
		public long lengthInBytes() {
			return file.length();
		}

		@Override
		protected boolean deleteImplementation() {
			return file.delete();
		}

		@Override
		protected RandomInputStream getRandomInputStreamImplementation() throws IOException {
			return new RandomFileInputStream(file);
		}

		@Override
		protected RandomOutputStream getRandomOutputStreamImplementation() throws IOException {
			return new RandomFileOutputStream(file);
		}

		@Override
		public int getInternalSerializedSize() {
			return SerializationTools.getInternalSize(file.getAbsolutePath(), MAX_FILE_NAME_LENGTH);
		}

		@Override
		public void writeExternal(SecuredObjectOutputStream out) throws IOException {
			out.writeString(file.getAbsolutePath(), false, MAX_FILE_NAME_LENGTH);
		}

		@Override
		public void readExternal(SecuredObjectInputStream in) throws IOException, ClassNotFoundException {
			String path=in.readString(false, MAX_FILE_NAME_LENGTH);
			file=new File(path);
		}
	}
	public static class FileReferenceFactory implements com.distrimind.madkit.kernel.FileReferenceFactory
	{

		static File getPackageFolder(long accountID, DecentralizedValue peerID, String packageString)
		{
			File accountFolder=new File(centralDatabaseFilesDirectory, "a"+accountID);
			File peerFolder=new File(accountFolder, peerID.encodeString().toString());
			return new File(peerFolder, packageString.replace('.', '_'));

		}

		@Override
		public FileReference getFileReference(long accountID, IASymmetricPublicKey externalAccountID, DecentralizedValue peerID,
											  EncryptedDatabaseBackupMetaDataPerFile encryptedDatabaseBackupMetaDataPerFile) {

			final File packageFolder=getPackageFolder(accountID, peerID, encryptedDatabaseBackupMetaDataPerFile.getPackageString());
			FileTools.checkFolderRecursive(packageFolder);
			return new FileReference(new File(packageFolder, "f"+encryptedDatabaseBackupMetaDataPerFile.getFileTimestampUTC()+(encryptedDatabaseBackupMetaDataPerFile.isReferenceFile()?".ref":".data")));
		}
	}
	public static class CentralDatabaseBackupReceiverPerPeer extends com.distrimind.madkit.kernel.CentralDatabaseBackupReceiverPerPeer
	{

		protected CentralDatabaseBackupReceiverPerPeer(com.distrimind.ood.database.centraldatabaseapi.CentralDatabaseBackupReceiver centralDatabaseBackupReceiver, DatabaseWrapper wrapper, CentralDatabaseBackupReceiverAgent agent, com.distrimind.madkit.kernel.FileReferenceFactory fileReferenceFactory) {
			super(centralDatabaseBackupReceiver, wrapper, agent, fileReferenceFactory);
		}


		@Override
		protected EncryptionProfileProvider getEncryptionProfileProviderToValidateCertificateOrGetNullIfNoValidProviderIsAvailable(com.distrimind.ood.database.centraldatabaseapi.CentralDatabaseBackupCertificate certificate) {
			return new EncryptionProfileProvider() {
				@Override
				public MessageDigestType getMessageDigest(short keyID, boolean duringDecryptionPhase) throws IOException {
					if (keyID==2)
						return MessageDigestType.DEFAULT;

					throw new IOException();
				}

				@Override
				public IASymmetricPrivateKey getPrivateKeyForSignature(short keyID)  {
					return null;
				}

				@Override
				public IASymmetricPublicKey getPublicKeyForSignature(short keyID) throws IOException {
					if (keyID==2)
						return certificate.getCertifiedAccountPublicKey();
					throw new IOException();
				}

				@Override
				public SymmetricSecretKey getSecretKeyForSignature(short keyID, boolean duringDecryptionPhase) {
					return null;
				}

				@Override
				public SymmetricSecretKey getSecretKeyForEncryption(short keyID, boolean duringDecryptionPhase) {
					return null;
				}

				@Override
				public boolean isValidProfileID(short id) {
					return id==2;
				}

				@Override
				public Short getValidProfileIDFromPublicKeyForSignature(IASymmetricPublicKey publicKeyForSignature) {
					if (certificate.getCertifiedAccountPublicKey().equals(publicKeyForSignature))
						return 2;
					return null;
				}

				@Override
				public short getDefaultKeyID() {
					return 0;
				}
			};
		}
		@Override
		public Integrity isAcceptableCertificate(com.distrimind.ood.database.centraldatabaseapi.CentralDatabaseBackupCertificate certificate) {
			if (!(certificate instanceof CentralDatabaseBackupCertificate))
				return Integrity.FAIL;
			if (!((CentralDatabaseBackupCertificate)certificate).centralDatabaseBackupPublicKey.equals(getCentralID()) || !certificate.getCertifiedAccountPublicKey().equals(getExternalAccountID()))
				return Integrity.FAIL_AND_CANDIDATE_TO_BAN;
			return Integrity.OK;
		}
	}
	public static class CentralDatabaseBackupReceiver extends com.distrimind.madkit.kernel.CentralDatabaseBackupReceiver
	{

		public CentralDatabaseBackupReceiver(DatabaseWrapper wrapper, DecentralizedValue centralID, long durationInMsBeforeRemovingDatabaseBackupAfterAnDeletionOrder, long durationInMsBeforeOrderingDatabaseBackupDeletion, long durationInMsThatPermitToCancelPeerRemovingWhenThePeerIsTryingToReconnect, long durationInMsToWaitBeforeRemovingAccountDefinitively, com.distrimind.madkit.kernel.FileReferenceFactory fileReferenceFactory) throws DatabaseException {
			super(wrapper, centralID, durationInMsBeforeRemovingDatabaseBackupAfterAnDeletionOrder, durationInMsBeforeOrderingDatabaseBackupDeletion, durationInMsThatPermitToCancelPeerRemovingWhenThePeerIsTryingToReconnect, durationInMsToWaitBeforeRemovingAccountDefinitively, fileReferenceFactory);
		}

		@Override
		protected CentralDatabaseBackupReceiverPerPeer newCentralDatabaseBackupReceiverPerPeerInstance(DatabaseWrapper wrapper) {
			return new CentralDatabaseBackupReceiverPerPeer(this, wrapper, getAgent(), getFileReferenceFactory());
		}
	}
	public static class CentralDatabaseBackupReceiverFactory extends com.distrimind.madkit.kernel.CentralDatabaseBackupReceiverFactory<CentralDatabaseBackupReceiver>
	{
		public CentralDatabaseBackupReceiverFactory() {
		}

		public CentralDatabaseBackupReceiverFactory(DecentralizedValue centralID, long durationInMsBeforeRemovingDatabaseBackupAfterAnDeletionOrder, long durationInMsBeforeOrderingDatabaseBackupDeletion, long durationInMsThatPermitToCancelPeerRemovingWhenThePeerIsTryingToReconnect, long durationInMsToWaitBeforeRemovingAccountDefinitively, Class<? extends com.distrimind.madkit.kernel.FileReferenceFactory> fileReferenceFactoryClass) {
			super(centralID, durationInMsBeforeRemovingDatabaseBackupAfterAnDeletionOrder, durationInMsBeforeOrderingDatabaseBackupDeletion, durationInMsThatPermitToCancelPeerRemovingWhenThePeerIsTryingToReconnect, durationInMsToWaitBeforeRemovingAccountDefinitively,  fileReferenceFactoryClass);
		}

		@Override
		public CentralDatabaseBackupReceiver getCentralDatabaseBackupReceiverInstance(DatabaseWrapper wrapper) throws DatabaseException {
			try {
				return new CentralDatabaseBackupReceiver(wrapper, getCentralID(), getDurationInMsBeforeRemovingDatabaseBackupAfterAnDeletionOrder(),
						getDurationInMsBeforeOrderingDatabaseBackupDeletion(),
						getDurationInMsThatPermitToCancelPeerRemovingWhenThePeerIsTryingToReconnect(),
						getDurationInMsToWaitBeforeRemovingAccountDefinitively(),
						getFileReferenceFactoryClass().getDeclaredConstructor().newInstance());
			} catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
				throw DatabaseException.getDatabaseException(e);
			}
		}
	}

	NetworkEventListener eventListener1;
	NetworkEventListener eventListener2;
	NetworkEventListener eventListenerServer;
	DecentralizedValue localIdentifier;
	DecentralizedValue localIdentifierOtherSide;
	DecentralizedValue serverIdentifier;
	LoginData loginData1, loginData2, loginDataClient1, loginDataClient2, loginDataServer;
	File databaseFile1, databaseFile2, databaseFileServer;
	SymmetricSecretKey secretKeyForSignature1,secretKeyForSignature2, secretKeyForE2EEncryption1, secretKeyForE2EEncryption2;
	AbstractKeyPair<?, ?> aSymmetricKeyPairForClientServerSignatures1, aSymmetricKeyPairForClientServerSignatures2, centralDatabaseBackupKeyPair;
	AbstractSecureRandom random;
	boolean connectCentralDatabaseBackup;
	boolean indirectSynchronizationWithCentralDatabaseBackup;
	CentralDatabaseBackupReceiverFactory centralDatabaseBackupReceiverFactory;
	private CentralDatabaseBackupCertificate centralDatabaseBackupCertificate;
	DatabaseWrapper centralDatabaseWrapper=null;


	@DataProvider
	public static Object[][] data() {
		int cycles=4;

		Object[][] res=new Object[cycles*3][2];
		int index=0;
		for (int i=0;i<cycles;i++) {
			for (boolean connectCentralDatabaseBackup : new boolean[]{false, true}) {
				for (boolean indirectSynchronizationWithCentralDatabaseBackup : connectCentralDatabaseBackup?new boolean[]{true, false}:new boolean[]{false})
				{
					res[index][0]=connectCentralDatabaseBackup;
					res[index++][1]=indirectSynchronizationWithCentralDatabaseBackup;
				}
			}
		}
		assert index==res.length;
		return res;
	}

	public void init(boolean connectCentralDatabaseBackup,
									  boolean indirectSynchronizationWithCentralDatabaseBackup) throws IOException, DatabaseException, NoSuchAlgorithmException, NoSuchProviderException {
		System.out.println("connectCentralDatabaseBackup="+connectCentralDatabaseBackup+", indirectSynchronizationWithCentralDatabaseBackup="+indirectSynchronizationWithCentralDatabaseBackup);
		if(centralDatabaseFilesDirectory.exists())
			FileTools.deleteDirectory(centralDatabaseFilesDirectory);
		this.connectCentralDatabaseBackup=connectCentralDatabaseBackup;
		indirectSynchronizationWithCentralDatabaseBackup=this.indirectSynchronizationWithCentralDatabaseBackup=indirectSynchronizationWithCentralDatabaseBackup & connectCentralDatabaseBackup;
		random=SecureRandomType.DEFAULT.getInstance(null);
		secretKeyForSignature1=SymmetricAuthenticatedSignatureType.HMAC_SHA2_384.getKeyGenerator(random).generateKey();
		secretKeyForSignature2=SymmetricAuthenticatedSignatureType.HMAC_SHA2_384.getKeyGenerator(random).generateKey();
		aSymmetricKeyPairForClientServerSignatures1 = ASymmetricAuthenticatedSignatureType.DEFAULT.getKeyPairGenerator(random).generateKeyPair();
		aSymmetricKeyPairForClientServerSignatures2 = ASymmetricAuthenticatedSignatureType.DEFAULT.getKeyPairGenerator(random).generateKeyPair();
		secretKeyForE2EEncryption1=SymmetricEncryptionType.DEFAULT.getKeyGenerator(random).generateKey();
		secretKeyForE2EEncryption2=SymmetricEncryptionType.DEFAULT.getKeyGenerator(random).generateKey();
		DecentralizedIDGenerator hostID1=new DecentralizedIDGenerator();
		DecentralizedIDGenerator hostID2=new DecentralizedIDGenerator();
		P2PSecuredConnectionProtocolPropertiesWithKeyAgreement clientProtocol1=null;
		P2PSecuredConnectionProtocolPropertiesWithKeyAgreement clientProtocol2=null;
		LoginData loginDataClient1=null, loginDataClient2=null;
		databaseFileServer = new File("tmpDatabaseFileServer");
		if (connectCentralDatabaseBackup) {
			ASymmetricKeyPair keyPairForClientServerAuthentication = ASymmetricAuthenticatedSignatureType.DEFAULT.getKeyPairGenerator(random).generateKeyPair();

			P2PSecuredConnectionProtocolPropertiesWithKeyAgreement serverProtocol = new P2PSecuredConnectionProtocolPropertiesWithKeyAgreement();
			serverProtocol.enableEncryption = true;
			serverProtocol.isServer = true;
			serverProtocol.symmetricEncryptionType = SymmetricEncryptionType.AES_CTR;
			serverProtocol.symmetricSignatureType = SymmetricAuthenticatedSignatureType.HMAC_SHA2_384;
			serverProtocol.addServerSideProfile(keyPairForClientServerAuthentication);

			clientProtocol1 = new P2PSecuredConnectionProtocolPropertiesWithKeyAgreement();
			clientProtocol1.enableEncryption = true;
			clientProtocol1.filtersForDistantPeers=new InetAddressFilters();
			clientProtocol1.filtersForDistantPeers.setDenyFilters(new DoubleIP(5000, (Inet4Address) InetAddress.getByName("127.0.0.1"), (Inet6Address) InetAddress.getByName("::1")));
			clientProtocol1.isServer = false;
			clientProtocol1.symmetricEncryptionType = SymmetricEncryptionType.AES_CTR;
			clientProtocol1.symmetricSignatureType = SymmetricAuthenticatedSignatureType.HMAC_SHA2_384;
			clientProtocol1.setClientSideProfile(serverProtocol);


			clientProtocol2 = new P2PSecuredConnectionProtocolPropertiesWithKeyAgreement();
			clientProtocol2.enableEncryption = true;
			clientProtocol2.filtersForDistantPeers=new InetAddressFilters();
			clientProtocol2.filtersForDistantPeers.setDenyFilters(new DoubleIP(5000, (Inet4Address) InetAddress.getByName("127.0.0.1"), (Inet6Address) InetAddress.getByName("::1")));
			clientProtocol2.isServer = false;
			clientProtocol2.symmetricEncryptionType = SymmetricEncryptionType.AES_CTR;
			clientProtocol2.symmetricSignatureType = SymmetricAuthenticatedSignatureType.HMAC_SHA2_384;
			clientProtocol2.setClientSideProfile(serverProtocol);

			ArrayList<IdentifierPassword> idpws=AccessDataMKEventListener
					.getClientOrPeerToPeerLogins(AccessDataMKEventListener.getCustomHostIdentifier(3), 6);
			ListGroupsRoles defaultGroupAccess=new ListGroupsRoles();
			defaultGroupAccess.addGroupsRoles(JunitMadkit.DEFAULT_NETWORK_GROUP_FOR_ACCESS_DATA);
			loginDataClient1=AccessDataMKEventListener.getDefaultLoginData(
					idpws,
					null, defaultGroupAccess, true, AssertJUnit::fail, AssertJUnit::fail);
			loginDataClient1.getFilters().setDenyFilters(new DoubleIP(5000, (Inet4Address) InetAddress.getByName("127.0.0.1"), (Inet6Address) InetAddress.getByName("::1")));
			AccessDataMKEventListener.databaseIdentifiers.put(idpws.get(0).getIdentifier(), hostID1);

			idpws=AccessDataMKEventListener
					.getClientOrPeerToPeerLogins(AccessDataMKEventListener.getCustomHostIdentifier(4), 6);
			defaultGroupAccess=new ListGroupsRoles();
			defaultGroupAccess.addGroupsRoles(JunitMadkit.DEFAULT_NETWORK_GROUP_FOR_ACCESS_DATA);
			loginDataClient2=AccessDataMKEventListener.getDefaultLoginData(
					idpws,
					null, defaultGroupAccess, true, AssertJUnit::fail, AssertJUnit::fail);
			loginDataClient2.getFilters().setDenyFilters(new DoubleIP(5000, (Inet4Address) InetAddress.getByName("127.0.0.1"), (Inet6Address) InetAddress.getByName("::1")));
			AccessDataMKEventListener.databaseIdentifiers.put(idpws.get(0).getIdentifier(), hostID2);

			if (databaseFileServer.exists())
				FileTools.deleteDirectory(databaseFileServer);

			AccessProtocolWithP2PAgreementProperties app = new AccessProtocolWithP2PAgreementProperties();
			HostIdentifier serverHostIdentifier=AccessDataMKEventListener.getCustomHostIdentifier(2);

			idpws = AccessDataMKEventListener
					.getClientOrPeerToPeerLogins(serverHostIdentifier, 6);

			defaultGroupAccess = new ListGroupsRoles();
			defaultGroupAccess.addGroupsRoles(JunitMadkit.DEFAULT_NETWORK_GROUP_FOR_ACCESS_DATA);


			loginDataServer = AccessDataMKEventListener.getDefaultLoginData(
					idpws,
					null, defaultGroupAccess, false, AssertJUnit::fail, AssertJUnit::fail);
			this.centralDatabaseBackupKeyPair=ASymmetricAuthenticatedSignatureType.DEFAULT.getKeyPairGenerator(random).generateKeyPair();
			AccessDataMKEventListener.centralIdentifiers.put(idpws.get(0).getIdentifier(), this.centralDatabaseBackupKeyPair.getASymmetricPublicKey());
			serverIdentifier = this.centralDatabaseBackupKeyPair.getASymmetricPublicKey();


			centralDatabaseBackupCertificate =new CentralDatabaseBackupCertificate(centralDatabaseBackupKeyPair, aSymmetricKeyPairForClientServerSignatures2.getASymmetricPublicKey(), Long.MAX_VALUE);
			centralDatabaseBackupReceiverFactory=new CentralDatabaseBackupReceiverFactory(centralDatabaseBackupKeyPair.getASymmetricPublicKey(),
					1000, 1000, 1000, 5000,
					FileReferenceFactory.class);
			this.eventListenerServer = new NetworkEventListener(true, false, false,
					databaseFileServer,
					new ConnectionsProtocolsMKEventListener(serverProtocol), new AccessProtocolPropertiesMKEventListener(app),
					new AccessDataMKEventListener(loginDataServer), 5001, Collections.emptyList(),
					InetAddress.getByName("127.0.0.1"), InetAddress.getByName("::1")) {

				@Override
				public void onMaDKitPropertiesLoaded(MadkitProperties _properties) {
					super.onMaDKitPropertiesLoaded(_properties);
					_properties.networkProperties.networkLogLevel = Level.INFO;
					_properties.networkProperties.maxBufferSize = Short.MAX_VALUE * 4;
					_properties.networkProperties.upnpIGDEnabled=false;
					try {
						_properties.setCentralDatabaseBackupReceiverFactory(centralDatabaseBackupReceiverFactory);
					} catch (DatabaseException e) {
						e.printStackTrace();
						System.exit(-1);
					}

				}
			};

		}
		else {
			loginDataServer=null;
			serverIdentifier=null;
			this.eventListenerServer=null;
			centralDatabaseBackupReceiverFactory=null;
			centralDatabaseBackupCertificate=null;
			centralDatabaseBackupKeyPair=null;
		}

		P2PSecuredConnectionProtocolPropertiesWithKeyAgreement p2pprotocol1=new P2PSecuredConnectionProtocolPropertiesWithKeyAgreement();
		p2pprotocol1.filtersForDistantPeers=new InetAddressFilters();
		p2pprotocol1.filtersForDistantPeers.setDenyFilters(new DoubleIP(5001,
				(Inet4Address) InetAddress.getByName("127.0.0.1"),(Inet6Address) InetAddress.getByName("::1")) );
		p2pprotocol1.isServer = true;
		p2pprotocol1.symmetricEncryptionType= SymmetricEncryptionType.AES_CTR;
		p2pprotocol1.symmetricSignatureType= SymmetricAuthenticatedSignatureType.HMAC_SHA2_384;
		databaseFile1=new File("tmpDatabaseFile1");
		databaseFile2=new File("tmpDatabaseFile2");
		if(databaseFile1.exists())
			FileTools.deleteDirectory(databaseFile1);
		if(databaseFile2.exists())
			FileTools.deleteDirectory(databaseFile2);

		ArrayList<IdentifierPassword> idpws=AccessDataMKEventListener
				.getClientOrPeerToPeerLogins(AccessDataMKEventListener.getCustomHostIdentifier(0), 4);

		ListGroupsRoles defaultGroupAccess=new ListGroupsRoles();

		defaultGroupAccess.addGroupsRoles(JunitMadkit.DEFAULT_NETWORK_GROUP_FOR_ACCESS_DATA);

		EncryptionProfileCollection encryptionProfileCollectionForP2PSignature1=new EncryptionProfileCollection();
		encryptionProfileCollectionForP2PSignature1.putProfile((short)1, MessageDigestType.DEFAULT, null, null, secretKeyForSignature1, null, false, false);
		encryptionProfileCollectionForP2PSignature1.putProfile((short)2, MessageDigestType.DEFAULT, null, null, secretKeyForSignature2, null, false, true);
		EncryptionProfileCollection encryptionProfileCollectionForE2EEncryption1=null;
		EncryptionProfileCollection clientServerProfileCollection1=null;
		if (connectCentralDatabaseBackup) {
			encryptionProfileCollectionForE2EEncryption1 = new EncryptionProfileCollection();
			encryptionProfileCollectionForE2EEncryption1.putProfile((short) 1, MessageDigestType.DEFAULT, null, null, secretKeyForSignature1, secretKeyForE2EEncryption1, false, false);
			encryptionProfileCollectionForE2EEncryption1.putProfile((short) 2, MessageDigestType.DEFAULT, null, null, secretKeyForSignature2, secretKeyForE2EEncryption2, false, true);

			clientServerProfileCollection1 = new EncryptionProfileCollection();
			clientServerProfileCollection1.putProfile((short) 1, MessageDigestType.DEFAULT, aSymmetricKeyPairForClientServerSignatures1.getASymmetricPublicKey(), aSymmetricKeyPairForClientServerSignatures1.getASymmetricPrivateKey(), null, null, false, false);
			clientServerProfileCollection1.putProfile((short) 2, MessageDigestType.DEFAULT, aSymmetricKeyPairForClientServerSignatures2.getASymmetricPublicKey(), aSymmetricKeyPairForClientServerSignatures2.getASymmetricPrivateKey(), null, null, false, true);
		}
		loginData1=AccessDataMKEventListener.getDefaultLoginData(
					idpws,
				null, defaultGroupAccess, true, AssertJUnit::fail, AssertJUnit::fail);
		loginData1.getFilters().setDenyFilters(new DoubleIP(5001,(Inet4Address) InetAddress.getByName("127.0.0.1"),(Inet6Address) InetAddress.getByName("::1")) );
		this.loginDataClient1=loginDataClient1;
		AccessDataMKEventListener.databaseIdentifiers.put(idpws.get(0).getIdentifier(), hostID1);
		localIdentifier=loginData1.getDecentralizedDatabaseID(idpws.get(0).getIdentifier(), null);

		List<AbstractIP> listIpToConnect=new ArrayList<>();
		if (connectCentralDatabaseBackup)
		{
			listIpToConnect.add(new DoubleIP(5001, (Inet4Address) InetAddress.getByName("127.0.0.1"),(Inet6Address) InetAddress.getByName("::1")));
		}

		this.eventListener1 = new NetworkEventListener(true, false, false,
				databaseFile1,clientServerProfileCollection1, encryptionProfileCollectionForE2EEncryption1, encryptionProfileCollectionForP2PSignature1,
				SecureRandomType.DEFAULT,
				clientProtocol1==null?new ConnectionsProtocolsMKEventListener(p2pprotocol1):new ConnectionsProtocolsMKEventListener(p2pprotocol1, clientProtocol1),
				new AccessProtocolPropertiesMKEventListener(new AccessProtocolWithP2PAgreementProperties()),
				loginDataClient1==null?new AccessDataMKEventListener(loginData1):new AccessDataMKEventListener(loginData1, loginDataClient1), 5000,
				listIpToConnect,
				InetAddress.getByName("127.0.0.1"), InetAddress.getByName("::1")) {

			@Override
			public void onMaDKitPropertiesLoaded(MadkitProperties _properties) {
				super.onMaDKitPropertiesLoaded(_properties);
				_properties.networkProperties.networkLogLevel = Level.INFO;
				_properties.networkProperties.maxBufferSize=Short.MAX_VALUE*4;
				_properties.networkProperties.upnpIGDEnabled=false;

			}
		};

		P2PSecuredConnectionProtocolPropertiesWithKeyAgreement p2pprotocol2 = new P2PSecuredConnectionProtocolPropertiesWithKeyAgreement();
		p2pprotocol2.filtersForDistantPeers=new InetAddressFilters();
		p2pprotocol2.filtersForDistantPeers.setDenyFilters(new DoubleIP(5001,(Inet4Address) InetAddress.getByName("127.0.0.1"),(Inet6Address) InetAddress.getByName("::1")) );
		p2pprotocol2.isServer = false;
		p2pprotocol2.symmetricEncryptionType=SymmetricEncryptionType.AES_CTR;
		p2pprotocol2.symmetricSignatureType= SymmetricAuthenticatedSignatureType.HMAC_SHA2_384;

		idpws=AccessDataMKEventListener
				.getClientOrPeerToPeerLogins(AccessDataMKEventListener.getCustomHostIdentifier(1), 4);

		EncryptionProfileCollection encryptionProfileCollectionForP2PSignature2=new EncryptionProfileCollection();
		encryptionProfileCollectionForP2PSignature2.putProfile((short)1, MessageDigestType.DEFAULT, null, null, secretKeyForSignature1, null, false, false);
		encryptionProfileCollectionForP2PSignature2.putProfile((short)2, MessageDigestType.DEFAULT, null, null, secretKeyForSignature2, null, false, true);

		EncryptionProfileCollection encryptionProfileCollectionForE2EEncryption2=null;
		EncryptionProfileCollection clientServerProfileCollection2=null;
		if (connectCentralDatabaseBackup) {
			encryptionProfileCollectionForE2EEncryption2 = new EncryptionProfileCollection();
			encryptionProfileCollectionForE2EEncryption2.putProfile((short) 1, MessageDigestType.DEFAULT, null, null, secretKeyForSignature1, secretKeyForE2EEncryption1, false, false);
			encryptionProfileCollectionForE2EEncryption2.putProfile((short) 2, MessageDigestType.DEFAULT, null, null, secretKeyForSignature2, secretKeyForE2EEncryption2, false, true);

			clientServerProfileCollection2 = new EncryptionProfileCollection();
			clientServerProfileCollection2.putProfile((short) 1, MessageDigestType.DEFAULT, aSymmetricKeyPairForClientServerSignatures1.getASymmetricPublicKey(), aSymmetricKeyPairForClientServerSignatures1.getASymmetricPrivateKey(), null, null, false, false);
			clientServerProfileCollection2.putProfile((short) 2, MessageDigestType.DEFAULT, aSymmetricKeyPairForClientServerSignatures2.getASymmetricPublicKey(), aSymmetricKeyPairForClientServerSignatures2.getASymmetricPrivateKey(), null, null, false, true);
		}

		loginData2=AccessDataMKEventListener.getDefaultLoginData(
				idpws,
				null, defaultGroupAccess, true, AssertJUnit::fail, AssertJUnit::fail);
		loginData2.getFilters().setDenyFilters(new DoubleIP(5001,(Inet4Address) InetAddress.getByName("127.0.0.1"),(Inet6Address) InetAddress.getByName("::1")) );
		AccessDataMKEventListener.databaseIdentifiers.put(idpws.get(0).getIdentifier(), hostID2);
		this.loginDataClient2=loginDataClient2;
		listIpToConnect=new ArrayList<>();
		if (!indirectSynchronizationWithCentralDatabaseBackup)
			listIpToConnect.add(new DoubleIP(5000, (Inet4Address) InetAddress.getByName("127.0.0.1"),(Inet6Address) InetAddress.getByName("::1")));
		if (connectCentralDatabaseBackup)
		{
			listIpToConnect.add(new DoubleIP(5001, (Inet4Address) InetAddress.getByName("127.0.0.1"),(Inet6Address) InetAddress.getByName("::1")));
		}
		this.eventListener2 = new NetworkEventListener(true, false, false, databaseFile2,
				clientServerProfileCollection2, encryptionProfileCollectionForE2EEncryption2, encryptionProfileCollectionForP2PSignature2,SecureRandomType.DEFAULT,
				clientProtocol2==null?new ConnectionsProtocolsMKEventListener(p2pprotocol2):new ConnectionsProtocolsMKEventListener(p2pprotocol2, clientProtocol2),
				new AccessProtocolPropertiesMKEventListener(new AccessProtocolWithP2PAgreementProperties()),
				loginDataClient2==null?new AccessDataMKEventListener(loginData2):new AccessDataMKEventListener(loginData2, loginDataClient2), 5000,
				listIpToConnect) {

			@Override
			public void onMaDKitPropertiesLoaded(MadkitProperties _properties) {
				super.onMaDKitPropertiesLoaded(_properties);
				_properties.networkProperties.networkLogLevel = Level.INFO;
				_properties.networkProperties.maxBufferSize=Short.MAX_VALUE*4;
				_properties.networkProperties.upnpIGDEnabled=false;

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
		final DatabaseConfiguration.SynchronizationType synchronizationType;
		final boolean indirect;
		final CentralDatabaseBackupCertificate centralDatabaseBackupCertificate;
		final CentralDatabaseBackupReceiver centralDatabaseBackupReceiver;
		static volatile int numberOfInitialBackupFilesIntoIntegrator=0;
		final int numberOfRecordsFirstAdded;



		public DatabaseAgent(DecentralizedValue localIdentifier,
							 DecentralizedValue localIdentifierOtherSide,
							 ArrayList<Table1.Record> myListToAdd,
							 ArrayList<Table1.Record> otherListToAdd,
							 AtomicReference<Boolean> finished,
							 boolean isIntegrator,
							 boolean central,
							 boolean indirect,
							 final CentralDatabaseBackupCertificate centralDatabaseBackupCertificate,
							 CentralDatabaseBackupReceiver centralDatabaseBackupReceiver) {
			this.localIdentifier = localIdentifier;
			this.localIdentifierOtherSide = localIdentifierOtherSide;
			this.myListToAdd = myListToAdd;
			this.otherListToAdd = otherListToAdd;
			this.finished = finished;
			integrator=isIntegrator;
			this.indirect=indirect;
			this.synchronizationType=central?DatabaseConfiguration.SynchronizationType.DECENTRALIZED_SYNCHRONIZATION_AND_SYNCHRONIZATION_WITH_CENTRAL_BACKUP_DATABASE:DatabaseConfiguration.SynchronizationType.DECENTRALIZED_SYNCHRONIZATION;
			this.centralDatabaseBackupCertificate=centralDatabaseBackupCertificate;
			this.centralDatabaseBackupReceiver=centralDatabaseBackupReceiver;
			this.numberOfRecordsFirstAdded=!(!integrator && indirect && synchronizationType== DatabaseConfiguration.SynchronizationType.DECENTRALIZED_SYNCHRONIZATION_AND_SYNCHRONIZATION_WITH_CENTRAL_BACKUP_DATABASE)?1:0;
		}

		@Override
		protected void liveCycle() {
			try {
				getMadkitConfig().getDatabaseWrapper().setNetworkLogLevel(Level.FINEST);
				sleep(2500);
				DatabaseWrapper wrapper=getMadkitConfig().getDatabaseWrapper();
				AssertJUnit.assertNotNull(wrapper);
				AssertJUnit.assertNull(getMadkitConfig().getDatabaseWrapper().getDatabaseConfigurationsBuilder().getConfigurations().getLocalPeer());
				AssertJUnit.assertNull(getMadkitConfig().getDatabaseWrapper().getDatabaseConfigurationsBuilder().getConfigurations().getLocalPeerString());
				if (synchronizationType== DatabaseConfiguration.SynchronizationType.DECENTRALIZED_SYNCHRONIZATION_AND_SYNCHRONIZATION_WITH_CENTRAL_BACKUP_DATABASE) {
					AssertJUnit.assertNotNull(centralDatabaseBackupCertificate);
					wrapper.getDatabaseConfigurationsBuilder()
							.setCentralDatabaseBackupCertificate(centralDatabaseBackupCertificate);
				}
				wrapper.getDatabaseConfigurationsBuilder()
						.setLocalPeerIdentifier(localIdentifier, true, false)
						.addConfiguration(
							new DatabaseConfiguration(new DatabaseSchema(Table1.class.getPackage()), synchronizationType,
									null, synchronizationType== DatabaseConfiguration.SynchronizationType.DECENTRALIZED_SYNCHRONIZATION_AND_SYNCHRONIZATION_WITH_CENTRAL_BACKUP_DATABASE?new BackupConfiguration(10000, 30000, 32000,1000, null):null),false, true )
						.commit();
				Table1 table=wrapper.getTableInstance(Table1.class);
				if (numberOfRecordsFirstAdded>0) {
					System.out.println(table.getDatabaseWrapper().getSynchronizer().getLocalHostID()+", add first record : "+myListToAdd.get(0));
					Long maxBackupFileAge=synchronizationType== DatabaseConfiguration.SynchronizationType.DECENTRALIZED_SYNCHRONIZATION_AND_SYNCHRONIZATION_WITH_CENTRAL_BACKUP_DATABASE?wrapper.getDatabaseConfigurationsBuilder().getConfigurations().getDatabaseConfiguration(table.getClass().getPackage()).getBackupConfiguration().getMaxBackupFileAgeInMs():null;
					for (int i=0;i<numberOfRecordsFirstAdded;i++) {
						table.addRecord(myListToAdd.get(i));
						if (maxBackupFileAge!=null && i+1<numberOfRecordsFirstAdded)
							sleep(maxBackupFileAge);
					}
					if (synchronizationType== DatabaseConfiguration.SynchronizationType.DECENTRALIZED_SYNCHRONIZATION_AND_SYNCHRONIZATION_WITH_CENTRAL_BACKUP_DATABASE)
						numberOfInitialBackupFilesIntoIntegrator=table.getDatabaseWrapper().getBackupRestoreManager(Table1.class.getPackage()).getFileTimeStamps().size();
					else
						numberOfInitialBackupFilesIntoIntegrator=0;
				}
				if (indirect && synchronizationType== DatabaseConfiguration.SynchronizationType.DECENTRALIZED_SYNCHRONIZATION_AND_SYNCHRONIZATION_WITH_CENTRAL_BACKUP_DATABASE)
					sleep(2000);
				AssertJUnit.assertFalse(wrapper.getSynchronizer().isInitialized(localIdentifierOtherSide));

				AssertJUnit.assertNotNull(getMadkitConfig().getDatabaseWrapper().getDatabaseConfigurationsBuilder().getConfigurations().getLocalPeer());
				AssertJUnit.assertNotNull(getMadkitConfig().getDatabaseWrapper().getDatabaseConfigurationsBuilder().getConfigurations().getLocalPeerString());
				sleep(1200);
				AssertJUnit.assertEquals(localIdentifier, wrapper.getSynchronizer().getLocalHostID());

				if (integrator) {
					wrapper.getDatabaseConfigurationsBuilder()
							.synchronizeDistantPeersWithGivenAdditionalPackages(Collections.singletonList(localIdentifierOtherSide), Table1.class.getPackage().getName())
							.commit();

					DatabaseConfiguration dc = wrapper.getDatabaseConfigurationsBuilder().getDatabaseConfiguration(Table1.class.getPackage());
					AssertJUnit.assertEquals(synchronizationType, dc.getSynchronizationType());
					AssertJUnit.assertNotNull(dc.getDistantPeersThatCanBeSynchronizedWithThisDatabase());
					AssertJUnit.assertTrue(dc.getDistantPeersThatCanBeSynchronizedWithThisDatabase().contains(localIdentifierOtherSide));
				}

				sleep(100);
				if (!indirect) {
					AssertJUnit.assertTrue(wrapper.getSynchronizer().isInitialized());
					System.out.println("check paired");
					int nb = 0;
					do {
						if (wrapper.getSynchronizer().isPairedWith(localIdentifierOtherSide))
							break;
						sleep(1000);
						++nb;
					} while (nb < 10);
					if (nb == 10) {
						System.err.println("Distant pair not paired");
						finished.set(false);
						return;
					}
					System.out.println("peer paired !");
					System.out.println("check pair connected");
					nb = 0;
					do {
						if (wrapper.getSynchronizer().isInitialized(localIdentifierOtherSide))
							break;
						sleep(1000);
						++nb;
					} while (nb < 10);
					if (nb == 10) {
						System.err.println("Distant pair not initialized");
						finished.set(false);
						return;
					}
					System.out.println("peer connected !");
				}
				System.out.println("check that database synchronization is activated with other peer");
				int nb=0;
				do {
					DatabaseConfiguration dc = wrapper.getDatabaseConfigurationsBuilder().getDatabaseConfiguration(Table1.class.getPackage());
					if (synchronizationType==dc.getSynchronizationType()
						&& dc.getDistantPeersThatCanBeSynchronizedWithThisDatabase()!=null
						&& dc.getDistantPeersThatCanBeSynchronizedWithThisDatabase().contains(localIdentifierOtherSide)
					 	&& (synchronizationType!= DatabaseConfiguration.SynchronizationType.DECENTRALIZED_SYNCHRONIZATION_AND_SYNCHRONIZATION_WITH_CENTRAL_BACKUP_DATABASE || centralDatabaseBackupReceiver.isConnectedIntoOneOfCentralDatabaseBackupServers(this.localIdentifier)))
						break;
					sleep(1000);
					++nb;
				} while(nb<10);
				if (nb==10) {
					System.err.println("database synchronization is not activated with other peer");
					finished.set(false);
					return;
				}
				if (indirect && synchronizationType== DatabaseConfiguration.SynchronizationType.DECENTRALIZED_SYNCHRONIZATION_AND_SYNCHRONIZATION_WITH_CENTRAL_BACKUP_DATABASE) {
					if (!integrator) {
						if (checkDistantRecords(this, table, Collections.singletonList(otherListToAdd.get(0)))) {
							System.err.println("Single distant record not synchronized with peers");
							finished.set(false);
							return;
						}
					}
					else
						sleep(2000);
				}
				if (checkSynchronizationWithCentralDB())
				{
					System.err.println("Distant records not synchronized with central DB");
					finished.set(false);
					return;
				}
				System.out.println(getMadkitConfig().getDatabaseWrapper().getSynchronizer().getLocalHostID()+" : add records");
				int total=numberOfRecordsFirstAdded;
				while(total<myListToAdd.size())
				{
					nb=(int)((Math.random()*((double)(myListToAdd.size()-total)))+1);

					for (int j=total;j<nb+total;j++)
					{
						table.addRecord(myListToAdd.get(j));
					}
					total+=nb;
					sleep(1000);
				}
				if (checkSynchronizationWithCentralDB())
				{
					System.err.println("Distant records not synchronized with central DB");
					finished.set(false);
					return;
				}
				if (checkDistantRecords(this, table, otherListToAdd))
				{
					System.err.println("Distant records not synchronized with peers");
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
				if (checkDistantRecords(this, table, otherListToAdd))
				{
					System.err.println("Distant records not synchronized after updating records");
					finished.set(false);
					return;
				}
				if (checkSynchronizationWithCentralDB())
				{
					System.err.println("Distant records not synchronized with central DB");
					finished.set(false);
					return;
				}
				sleep(1000);
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
		boolean checkSynchronizationWithCentralDB() throws DatabaseException, InterruptedException {
			if (synchronizationType== DatabaseConfiguration.SynchronizationType.DECENTRALIZED_SYNCHRONIZATION_AND_SYNCHRONIZATION_WITH_CENTRAL_BACKUP_DATABASE)
			{
				boolean ok;
				int count=0;
				BackupRestoreManager brm=getMadkitConfig().getDatabaseWrapper().getBackupRestoreManager(Table1.class.getPackage());
				//sleep(brm.getBackupConfiguration().getMaxBackupFileAgeInMs()+10);
				do {
					ok=true;
					int number=brm.getFinalFiles().size();

					File f=FileReferenceFactory.getPackageFolder(1, getMadkitConfig().getDatabaseWrapper().getSynchronizer().getLocalHostID(), Table1.class.getPackage().getName());
					if (!f.exists()) {
						if (!(indirect && !integrator))
						{
							System.out.println(getMadkitConfig().getDatabaseWrapper().getSynchronizer().getLocalHostID() + ", Synchronization with central database backup has not started ! ");
							ok = false;
						}
					}
					else {
						File[] fs = f.listFiles();
						int number2 = fs == null ? 0 : fs.length;
						if (number != number2) {
							if (!(indirect && !integrator && (number==number2+numberOfInitialBackupFilesIntoIntegrator))) {
								System.out.println(getMadkitConfig().getDatabaseWrapper().getSynchronizer().getLocalHostID() + ", Bad synchronized files number (peer files number=" + number + ", central database backup files number=" + number2
										+ "\n\tPeer side : " + brm.getFinalFiles().stream()
										.map(File::getName)
										.reduce((s1, s2) -> s1 + " ; " + s2)
										.orElse("None")
										+ "\n\tServer side : " + (fs == null ? "null" : (Arrays.stream(fs)
										.map(File::getName)
										.reduce((s1, s2) -> s1 + " ; " + s2))
										.orElse("None"))
								);

								ok = false;
							}
						}
					}
					if (!ok)
						sleep(brm.getBackupConfiguration().getMaxBackupFileAgeInMs());
				} while (++count<5 && !ok);
				if (ok)
					System.out.println(getMadkitConfig().getDatabaseWrapper().getSynchronizer().getLocalHostID()+" : synchronization with central database ok !");
				else
					System.out.println(getMadkitConfig().getDatabaseWrapper().getSynchronizer().getLocalHostID()+" : synchronization with central database NOT ok !");

				return !ok;
			}
			else
				return false;
		}
	}
	private static boolean checkDistantRecords(AbstractAgent agent, Table1 table, List<Table1.Record> otherListToAdd) throws DatabaseException, InterruptedException {
		System.out.println("check synchronization, "+table.getDatabaseWrapper().getSynchronizer().getLocalHostID()+", lacking = "+otherListToAdd.size()+", table.recordsNumber="+table.getRecordsNumber());
		ArrayList<Table1.Record> l=new ArrayList<>(otherListToAdd);
		int nb=0;
		do {
			for (Iterator<Table1.Record> it=l.iterator();it.hasNext();)
			{
				Table1.Record r=it.next();
				Table1.Record r2=table.getRecord("decentralizedID", r.getDecentralizedID());
				if (r2!=null) {
					AssertJUnit.assertEquals(r2.getDecentralizedID(), r.getDecentralizedID());
					if (!r2.getValue().equals(r.getValue()))
						continue;
					it.remove();
				}
			}
			if (l.size()>0) {
				System.out.println(table.getDatabaseWrapper().getSynchronizer().getLocalHostID()+", lacking = "+l.size()+", table.recordsNumber="+table.getRecordsNumber());
				agent.sleep(1000);
			}
			++nb;
		} while(l.size()>0 && nb<10);
		System.out.println("check synchronization, lacking = "+l.size()+", table.recordsNumber="+table.getRecordsNumber());
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
		final boolean indirect;
		final DatabaseConfiguration.SynchronizationType synchronizationType;
		final CentralDatabaseBackupCertificate centralDatabaseBackupCertificate;
		final CentralDatabaseBackupReceiver centralDatabaseBackupReceiver;


		public SecondConnexionAgent(DecentralizedValue localIdentifier, DecentralizedValue localIdentifierOtherSide, ArrayList<Table1.Record> myListToAdd,
									ArrayList<Table1.Record> otherListToAdd, AtomicReference<Boolean> finished,
									boolean central,
									boolean indirect,
									final CentralDatabaseBackupCertificate centralDatabaseBackupCertificate,
									CentralDatabaseBackupReceiver centralDatabaseBackupReceiver) {
			this.localIdentifier = localIdentifier;
			this.localIdentifierOtherSide = localIdentifierOtherSide;
			this.myListToAdd = myListToAdd;
			this.otherListToAdd = otherListToAdd;
			this.finished = finished;
			this.indirect=indirect;
			this.synchronizationType=central?DatabaseConfiguration.SynchronizationType.DECENTRALIZED_SYNCHRONIZATION_AND_SYNCHRONIZATION_WITH_CENTRAL_BACKUP_DATABASE:DatabaseConfiguration.SynchronizationType.DECENTRALIZED_SYNCHRONIZATION;
			this.centralDatabaseBackupCertificate=centralDatabaseBackupCertificate;
			this.centralDatabaseBackupReceiver=centralDatabaseBackupReceiver;
		}
		@Override
		protected void liveCycle() {
			try {
				getMadkitConfig().getDatabaseWrapper().setNetworkLogLevel(Level.FINEST);
				sleep(1900);
				DatabaseWrapper wrapper=getMadkitConfig().getDatabaseWrapper();
				AssertJUnit.assertNotNull(wrapper);

				if (synchronizationType== DatabaseConfiguration.SynchronizationType.DECENTRALIZED_SYNCHRONIZATION_AND_SYNCHRONIZATION_WITH_CENTRAL_BACKUP_DATABASE)
					wrapper.getDatabaseConfigurationsBuilder()
							.setCentralDatabaseBackupCertificate(centralDatabaseBackupCertificate);
				wrapper.getDatabaseConfigurationsBuilder()
						.setLocalPeerIdentifier(localIdentifier, true, false)
						.addConfiguration(
							new DatabaseConfiguration(new DatabaseSchema(Table1.class.getPackage()), synchronizationType, Collections.singletonList(localIdentifierOtherSide), synchronizationType== DatabaseConfiguration.SynchronizationType.DECENTRALIZED_SYNCHRONIZATION_AND_SYNCHRONIZATION_WITH_CENTRAL_BACKUP_DATABASE?new BackupConfiguration(10000, 30000, 32000,1000, null):null),false, false );


				wrapper.getDatabaseConfigurationsBuilder()
						.commit();

				AssertJUnit.assertNotNull(wrapper.getDatabaseConfigurationsBuilder().getConfigurations().getLocalPeer());
				AssertJUnit.assertNotNull(wrapper.getDatabaseConfigurationsBuilder().getConfigurations().getLocalPeerString());
				AssertJUnit.assertEquals(wrapper.getSynchronizer().getLocalHostID(), localIdentifier);
				AssertJUnit.assertTrue(wrapper.getSynchronizer().isPairedWith(localIdentifierOtherSide));
				sleep(100);
				AssertJUnit.assertTrue(wrapper.getSynchronizer().isInitialized());
				if (!indirect) {
					System.out.println("check paired");
					int nb = 0;
					do {
						if (wrapper.getSynchronizer().isPairedWith(localIdentifierOtherSide))
							break;
						sleep(1000);
						++nb;
					} while (nb < 10);
					if (nb == 10) {

						finished.set(false);
						return;
					}
					System.out.println("peer paired !");
					System.out.println("check pair connected");
					nb = 0;
					do {
						if (wrapper.getSynchronizer().isInitialized(localIdentifierOtherSide))
							break;
						sleep(1000);
						++nb;
					} while (nb < 10);
					if (nb == 10) {

						finished.set(false);
						return;
					}
					System.out.println("peer connected !");
				}
				System.out.println("check that database synchronization is connected with central database backup");
				int nb=0;
				do {
					DatabaseConfiguration dc = wrapper.getDatabaseConfigurationsBuilder().getDatabaseConfiguration(Table1.class.getPackage());
					if (synchronizationType==dc.getSynchronizationType()
							&& dc.getDistantPeersThatCanBeSynchronizedWithThisDatabase()!=null
							&& dc.getDistantPeersThatCanBeSynchronizedWithThisDatabase().contains(localIdentifierOtherSide)
							&& (synchronizationType!= DatabaseConfiguration.SynchronizationType.DECENTRALIZED_SYNCHRONIZATION_AND_SYNCHRONIZATION_WITH_CENTRAL_BACKUP_DATABASE || centralDatabaseBackupReceiver.isConnectedIntoOneOfCentralDatabaseBackupServers(this.localIdentifier)))
						break;
					sleep(1000);
					++nb;
				} while(nb<10);
				if (nb==10) {
					System.err.println("database synchronization is not connected with central database backup");
					finished.set(false);
					return;
				}

				Table1 table=wrapper.getTableInstance(Table1.class);
				if (checkDistantRecords(this, table, otherListToAdd))
				{
					finished.set(false);
					return;
				}

				sleep(1000);
				System.out.println("desynchronize distant peer");
				wrapper.getDatabaseConfigurationsBuilder()
						.desynchronizeDistantPeerWithGivenAdditionalPackages(localIdentifierOtherSide, Table1.class.getPackage().getName())
						.removeDistantPeer(localIdentifierOtherSide)
						.commit();
				sleep(1000);

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

				System.out.println("check that database synchronization is not connected with central database backup");
				nb=0;
				do {
					if (synchronizationType!= DatabaseConfiguration.SynchronizationType.DECENTRALIZED_SYNCHRONIZATION_AND_SYNCHRONIZATION_WITH_CENTRAL_BACKUP_DATABASE || !centralDatabaseBackupReceiver.isConnectedIntoOneOfCentralDatabaseBackupServers(this.localIdentifier))
						break;
					sleep(1000);
					++nb;
				} while(nb<10);
				if (nb==10) {
					System.err.println("database synchronization is connected with central database backup");
					finished.set(false);
					return;
				}

				sleep(3000);
				AssertJUnit.assertNull(wrapper.getDatabaseConfigurationsBuilder().getConfigurations().getLocalPeer());
				AssertJUnit.assertNull(wrapper.getDatabaseConfigurationsBuilder().getConfigurations().getLocalPeerString());
				finished.set(true);
			} catch(AssertionError e)
			{
				e.printStackTrace();
				finished.set(false);
				throw e;
			} catch (Throwable e) {
				e.printStackTrace();
				finished.set(false);
			} finally {
				this.killAgent(this);
			}
		}
	}
	private static class ThirdConnexionAgent extends Agent
	{
		final DecentralizedValue localIdentifier;
		final AtomicReference<Boolean> finished;
		final DatabaseConfiguration.SynchronizationType synchronizationType;
		final CentralDatabaseBackupReceiver centralDatabaseBackupReceiver;


		public ThirdConnexionAgent(DecentralizedValue localIdentifier, AtomicReference<Boolean> finished,
								   boolean central,
										   final CentralDatabaseBackupReceiver centralDatabaseBackupReceiver) {
			this.localIdentifier = localIdentifier;
			this.finished = finished;
			this.synchronizationType=central?DatabaseConfiguration.SynchronizationType.DECENTRALIZED_SYNCHRONIZATION_AND_SYNCHRONIZATION_WITH_CENTRAL_BACKUP_DATABASE:DatabaseConfiguration.SynchronizationType.DECENTRALIZED_SYNCHRONIZATION;
			this.centralDatabaseBackupReceiver=centralDatabaseBackupReceiver;

		}

		@Override
		protected void liveCycle() throws InterruptedException {
			try {
				getMadkitConfig().getDatabaseWrapper().setNetworkLogLevel(Level.FINEST);
				sleep(1500);
				DatabaseWrapper wrapper=getMadkitConfig().getDatabaseWrapper();
				AssertJUnit.assertNotNull(wrapper);
				wrapper.getDatabaseConfigurationsBuilder()
						.addConfiguration(
								new DatabaseConfiguration(new DatabaseSchema(Table1.class.getPackage())),false, true )
						.commit();
				sleep(800);
				AssertJUnit.assertNull(wrapper.getDatabaseConfigurationsBuilder().getConfigurations().getLocalPeer());
				AssertJUnit.assertNull(wrapper.getDatabaseConfigurationsBuilder().getConfigurations().getLocalPeerString());
				System.out.println("check that database synchronization is not connected with central database backup");
				int nb=0;
				do {
					if (synchronizationType!= DatabaseConfiguration.SynchronizationType.DECENTRALIZED_SYNCHRONIZATION_AND_SYNCHRONIZATION_WITH_CENTRAL_BACKUP_DATABASE || !centralDatabaseBackupReceiver.isConnectedIntoOneOfCentralDatabaseBackupServers(this.localIdentifier))
						break;
					sleep(1000);
					++nb;
				} while(nb<10);
				if (nb==10) {
					System.err.println("database synchronization is connected with central database backup");
					finished.set(false);
					return;
				}
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


	@Test(dataProvider = "data")
	public void testDatabaseSynchronization(boolean connectCentralDatabaseBackup,
											boolean indirectSynchronizationWithCentralDatabaseBackup) throws IOException, NoSuchAlgorithmException, NoSuchProviderException, DatabaseException {
		init(connectCentralDatabaseBackup, indirectSynchronizationWithCentralDatabaseBackup);
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
					try {
						AssertJUnit.assertFalse(databaseFile1.exists());
						AssertJUnit.assertFalse(databaseFile2.exists());
						ArrayList<Table1.Record> recordsToAdd = getRecordsToAdd();
						ArrayList<Table1.Record> recordsToAddOtherSide = getRecordsToAdd();
						if (connectCentralDatabaseBackup) {
							launchThreadedMKNetworkInstance(Level.INFO, AbstractAgent.class, new AbstractAgent() {
								@Override
								protected void activate() {
									try {
										centralDatabaseWrapper = getMadkitConfig().getDatabaseWrapper();
										centralDatabaseWrapper.setCentralDatabaseLogLevel(Level.FINER);
										getMadkitConfig().getCentralDatabaseBackupReceiver().addClient((short) 10, aSymmetricKeyPairForClientServerSignatures2.getASymmetricPublicKey());
									} catch (DatabaseException e) {
										e.printStackTrace();
									}
								}
							}, eventListenerServer);
							sleep(600);

						}

						AbstractAgent agentChecker = new DatabaseAgent(localIdentifier, localIdentifierOtherSide, recordsToAdd, recordsToAddOtherSide, finished1, true, connectCentralDatabaseBackup, indirectSynchronizationWithCentralDatabaseBackup, centralDatabaseBackupCertificate, centralDatabaseBackupReceiverFactory==null?null:centralDatabaseBackupReceiverFactory.getCentralDatabaseBackupReceiverInstance(centralDatabaseWrapper));
						launchThreadedMKNetworkInstance(Level.INFO, AbstractAgent.class, agentChecker, eventListener1);
						sleep(600);
						AbstractAgent agentCheckerOtherSide = new DatabaseAgent(localIdentifierOtherSide, localIdentifier, recordsToAddOtherSide, recordsToAdd, finished2, false, connectCentralDatabaseBackup, indirectSynchronizationWithCentralDatabaseBackup, centralDatabaseBackupCertificate, centralDatabaseBackupReceiverFactory==null?null:centralDatabaseBackupReceiverFactory.getCentralDatabaseBackupReceiverInstance(centralDatabaseWrapper));
						launchThreadedMKNetworkInstance(Level.INFO, AbstractAgent.class, agentCheckerOtherSide, eventListener2);

						while (finished1.get() == null || finished2.get() == null) {

							sleep(1000);
						}

						Assert.assertTrue(finished1.get());
						Assert.assertTrue(finished2.get());

						cleanHelperMDKs(this);
						AssertJUnit.assertEquals(getHelperInstances(this, 0).size(), 0);
						finished1.set(null);
						finished2.set(null);

						System.out.println("Second step");
						AssertJUnit.assertTrue(databaseFile1.exists());
						AssertJUnit.assertTrue(databaseFile2.exists());

						if (connectCentralDatabaseBackup) {
							AssertJUnit.assertTrue(centralDatabaseFilesDirectory.exists());
							launchThreadedMKNetworkInstance(Level.INFO, AbstractAgent.class, new AbstractAgent() {
								@Override
								protected void activate() throws InterruptedException {
									super.activate();
									try {
										centralDatabaseWrapper = getMadkitConfig().getDatabaseWrapper();
									} catch (DatabaseException e) {
										e.printStackTrace();
									}
								}
							}, eventListenerServer);
							sleep(600);
						}
						agentChecker = new SecondConnexionAgent(localIdentifier, localIdentifierOtherSide, recordsToAdd, recordsToAddOtherSide, finished1, connectCentralDatabaseBackup, indirectSynchronizationWithCentralDatabaseBackup, centralDatabaseBackupCertificate, centralDatabaseBackupReceiverFactory==null?null:centralDatabaseBackupReceiverFactory.getCentralDatabaseBackupReceiverInstance(centralDatabaseWrapper));
						launchThreadedMKNetworkInstance(Level.INFO, AbstractAgent.class, agentChecker, eventListener1);
						sleep(600);
						agentCheckerOtherSide = new SecondConnexionAgent(localIdentifierOtherSide, localIdentifier, recordsToAddOtherSide, recordsToAdd, finished2, connectCentralDatabaseBackup, indirectSynchronizationWithCentralDatabaseBackup, centralDatabaseBackupCertificate, centralDatabaseBackupReceiverFactory==null?null:centralDatabaseBackupReceiverFactory.getCentralDatabaseBackupReceiverInstance(centralDatabaseWrapper));
						launchThreadedMKNetworkInstance(Level.INFO, AbstractAgent.class, agentCheckerOtherSide, eventListener2);

						while (finished1.get() == null || finished2.get() == null) {

							sleep(1000);
						}

						Assert.assertTrue(finished1.get());
						Assert.assertTrue(finished2.get());
						cleanHelperMDKs(this);
						AssertJUnit.assertEquals(getHelperInstances(this, 0).size(), 0);

						finished1.set(null);
						finished2.set(null);
						System.out.println("Third step");
						if (connectCentralDatabaseBackup) {
							launchThreadedMKNetworkInstance(Level.INFO, AbstractAgent.class, new AbstractAgent() {
								@Override
								protected void activate() throws InterruptedException {
									super.activate();
									try {
										centralDatabaseWrapper = getMadkitConfig().getDatabaseWrapper();
									} catch (DatabaseException e) {
										e.printStackTrace();
									}
								}
							}, eventListenerServer);
							sleep(600);
						}
						agentChecker = new ThirdConnexionAgent(localIdentifier, finished1, connectCentralDatabaseBackup, centralDatabaseBackupReceiverFactory==null?null:centralDatabaseBackupReceiverFactory.getCentralDatabaseBackupReceiverInstance(centralDatabaseWrapper));
						launchThreadedMKNetworkInstance(Level.INFO, AbstractAgent.class, agentChecker, eventListener1);
						sleep(400);
						agentCheckerOtherSide = new ThirdConnexionAgent(localIdentifierOtherSide, finished2, connectCentralDatabaseBackup, centralDatabaseBackupReceiverFactory==null?null:centralDatabaseBackupReceiverFactory.getCentralDatabaseBackupReceiverInstance(centralDatabaseWrapper));
						launchThreadedMKNetworkInstance(Level.INFO, AbstractAgent.class, agentCheckerOtherSide, eventListener2);

						while (finished1.get() == null || finished2.get() == null) {

							sleep(1000);
						}

						Assert.assertTrue(finished1.get());
						Assert.assertTrue(finished2.get());

						cleanHelperMDKs(this);
						AssertJUnit.assertEquals(getHelperInstances(this, 0).size(), 0);
					} catch (DatabaseException e) {
						e.printStackTrace();
						Assert.assertFalse(finished1.get());
						Assert.assertFalse(finished2.get());
					}
				}
			});
		}
		finally {
			if(databaseFile1.exists())
				FileTools.deleteDirectory(databaseFile1);
			if(databaseFile2.exists())
				FileTools.deleteDirectory(databaseFile2);
			if(databaseFileServer.exists())
				FileTools.deleteDirectory(databaseFileServer);
			if(centralDatabaseFilesDirectory.exists())
				FileTools.deleteDirectory(centralDatabaseFilesDirectory);

		}

		AssertJUnit.assertTrue(finished1.get());
		AssertJUnit.assertTrue(finished2.get());

		cleanHelperMDKs();
	}

}
