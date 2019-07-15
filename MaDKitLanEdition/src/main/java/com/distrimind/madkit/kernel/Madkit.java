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
package com.distrimind.madkit.kernel;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.Map.Entry;
import java.util.jar.Attributes;
import java.util.jar.Manifest;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.distrimind.madkit.action.AgentAction;
import com.distrimind.madkit.action.GUIManagerAction;
import com.distrimind.madkit.action.KernelAction;
import com.distrimind.madkit.action.SchedulingAction;
import com.distrimind.madkit.i18n.I18nUtilities;
import com.distrimind.madkit.kernel.network.*;
import com.distrimind.madkit.kernel.network.connection.ConnectionProtocol;
import com.distrimind.madkit.kernel.network.connection.PointToPointTransferedBlockChecker;
import com.distrimind.madkit.kernel.network.connection.access.EncryptedCloudIdentifier;
import com.distrimind.madkit.kernel.network.connection.access.EncryptedIdentifier;
import com.distrimind.madkit.kernel.network.connection.access.EncryptedPassword;
import com.distrimind.madkit.kernel.network.connection.access.Identifier;
import com.distrimind.madkit.message.*;
import com.distrimind.madkit.message.hook.HookMessage;
import com.distrimind.ood.database.DatabaseEventType;
import com.distrimind.ood.database.TransactionIsolation;
import com.distrimind.util.OS;
import com.distrimind.util.OSVersion;
import com.distrimind.util.ReflectionTools;
import com.distrimind.util.crypto.*;
import com.distrimind.util.io.SecureExternalizableWithoutInnerSizeControl;
import com.distrimind.util.io.SerializationTools;
import com.distrimind.util.properties.PropertiesParseException;
import com.distrimind.util.version.Description;
import com.distrimind.util.version.Person;
import com.distrimind.util.version.PersonDeveloper;
import com.distrimind.util.version.Version;

/**
 * MaDKit 5 booter class.
 * 
 * <h2>MaDKit v.5 new features</h2>
 * 
 * <ul>
 * <li>One big change that comes with version 5 is how agents are identified and
 * localized within the artificial society. An agent is no longer binded to a
 * single agent address but has as many agent addresses as holden positions in
 * the artificial society. see {@link AgentAddress} for more information.</li>
 * 
 * <li>With respect to the previous change, a <code><i>withRole</i></code>
 * version of all the messaging methods has been added. See
 * {@link AbstractAgent#sendMessageWithRole(AgentAddress, Message, String)} for
 * an example of such a method.</li>
 * <li>A replying mechanism has been introduced through
 * <code><i>SendReply</i></code> methods. It enables the agent with the
 * possibility of replying directly to a given message. Also, it is now possible
 * to get the reply to a message, or to wait for a reply ( for {@link Agent}
 * subclasses only as they are threaded) See
 * {@link AbstractAgent#sendReply(Message, Message)} for more details.</li>
 * <li>Agents now have a <i>formal</i> state during a MaDKit session. See the
 * {@link AbstractAgent#getState()} method for detailed information.</li>
 * <li>One of the most convenient improvement of v.5 is the logging mechanism
 * which is provided. See the {@link AbstractAgent#logger} attribute for more
 * details.</li>
 * <li>Internationalization is being made (fr_fr and en_us for now).</li>
 * </ul>
 * 
 * @author Jason Mahdjoub
 * @author Fabien Michel
 * @author Jacques Ferber
 * @since MaDKit 4.0
 * @version 5.5
 */

@SuppressWarnings("SameParameterValue")
final public class Madkit {

	private final static String MDK_LOGGER_NAME = "[* MADKIT *] ";
	private volatile static MadkitProperties defaultConfig=null;
	final SimpleDateFormat dateFormat;






	static MadkitProperties getDefaultConfig()
	{
		getVersion();
		return defaultConfig;
	}
	
	private volatile static Version VERSION;

	static Version getNewVersionInstance()
	{
		Calendar c = Calendar.getInstance();
		c.set(2015, Calendar.MAY, 22);
		Calendar c2 = Calendar.getInstance();
		c2.set(2019, Calendar.MAY, 6);
		Version VERSION = new Version("MaDKitLanEdition", "MKLE", (short)2, (short)0, (short)0, Version.Type.Stable, (short)1, c.getTime(), c2.getTime());
		try {

			InputStream is = Madkit.class.getResourceAsStream("build.txt");
			if (is!=null)
				VERSION.loadBuildNumber(is);

			VERSION.addCreator(new Person("mahdjoub", "jason"));
			c = Calendar.getInstance();
			c.set(2015, Calendar.MAY, 22);
			VERSION.addDeveloper(new PersonDeveloper("mahdjoub", "jason", c.getTime()));
			c = Calendar.getInstance();
			c.set(1997, Calendar.FEBRUARY, 1);
			VERSION.addDeveloper(new PersonDeveloper("michel", "fabien", c.getTime()));
			c = Calendar.getInstance();
			c.set(1997, Calendar.FEBRUARY, 1);
			VERSION.addDeveloper(new PersonDeveloper("Gutknecht", "Olivier", c.getTime()));
			c = Calendar.getInstance();
			c.set(1997, Calendar.FEBRUARY, 1);
			VERSION.addDeveloper(new PersonDeveloper("Ferber", "Jacques", c.getTime()));

			c = Calendar.getInstance();
			c.set(2019, Calendar.MAY, 6);
			Description d = new Description((short)2, (short)0, (short)0, Version.Type.Stable, (short)1, c.getTime());
			d.addItem("Update Utils to 4.4.3 Stable");
			d.addItem("Update OOD to 2.1.0 Stable");
			VERSION.addDescription(d);

			c = Calendar.getInstance();
			c.set(2019, Calendar.APRIL, 24);
			d = new Description((short)1, (short)11, (short)1, Version.Type.Stable, (short)1, c.getTime());
			d.addItem("Minimal modification into SecuredObjectOutputStream and SecuredObjectInputStream classes. ");
			d.addItem("Fix bad use of garbage collector with ConversationID. ");
			VERSION.addDescription(d);

			c = Calendar.getInstance();
			c.set(2019, Calendar.APRIL, 23);
			d = new Description((short)1, (short)11, (short)0, Version.Type.Stable, (short)1, c.getTime());
			d.addItem("Add SecuredObjectOutputStream and SecuredObjectInputStream classes. Do not use native ObjectInputStream.");
			d.addItem("Add possibility to send asynchronous messages (AbstractAgent.sendMessageWithRoleOrDifferSendingUntilRecipientWasFound).");
			VERSION.addDescription(d);

			c = Calendar.getInstance();
			c.set(2019, Calendar.APRIL, 8);
			d = new Description((short)1, (short)10, (short)2, Version.Type.Stable, (short)1, c.getTime());
			d.addItem("Add hashCode function into class AbstractIP.");
			d.addItem("Better manage externalization.");
			d.addItem("Better manage identifiers of connection protocol negotiator.");
			d.addItem("Add possibility to change connection protocols during MaDKit life, and not only during MaDKit loading.");
			d.addItem("Add possibility to change access protocols during MaDKit life, and not only during MaDKit loading.");
			d.addItem("Add possibility to change access data during MaDKit life, and not only during MaDKit loading.");
			d.addItem("Add possibility to trigger connection/disconnection only if these were not present into the list of connection to attempt at the MaDKit starting defined into NetworkProperties class. " +
					"Concerned functions are AbstractAgent.manageDirectConnection, AbstractAgent.manageDirectConnections, AbstractAgent.manageDirectConnectionAndAddItToNetworkProperties and AbstractAgent.manageDirectConnectionsAndAddThemToNetworkProperties.");
			VERSION.addDescription(d);

			c = Calendar.getInstance();
			c.set(2019, Calendar.MARCH, 26);
			d = new Description((short)1, (short)10, (short)1, Version.Type.Stable, (short)1, c.getTime());
			d.addItem("Do not do useless thread locking when CGR groups are synchronized with distant peers.");
			d.addItem("Restore BigDataTransferID to previous previous (new version caused issues with Junit tests).");
			d.addItem("Fix issue during MaDKit ending.");
			VERSION.addDescription(d);

			c = Calendar.getInstance();
			c.set(2019, Calendar.MARCH, 25);
			d = new Description((short)1, (short)10, (short)0, Version.Type.Stable, (short)1, c.getTime());
			d.addItem("Update OOD to 2.0.0 Beta 104.");
			d.addItem("Update Utils to 3.25.5 Stable.");
			d.addItem("Add new connection protocol with symmetric keys: P2PSecuredConnectionProtocolWithKnownSymmetricKeys.");
			d.addItem("Better tests for network transfers.");
			d.addItem("Improve synchronization of list of distant agents.");
			d.addItem("Security issue : control that received distant agents list synchronization concerns the good distant kernel address.");
			d.addItem("Insure that list of distant agents are removed when peers are disconnected.");
			d.addItem("When a connection fail, try another ip.");
			d.addItem("Change information sending order when a new connection was established (fix a problem with synchronization of CGR that is done after network messages are done).");
			d.addItem("Fix issue with replies not sent with killed agents : killed agent send now empty reply for messages that need reply.");
			VERSION.addDescription(d);


			c = Calendar.getInstance();
			c.set(2019, Calendar.MARCH, 1);
			d = new Description((short)1, (short)9, (short)6, Version.Type.Stable, (short)1, c.getTime());
			d.addItem("Remove obsolete code.");
			d.addItem("Rename function into HostIdentifier class.");
			d.addItem("Typo corrections.");
			d.addItem("Add possibility to specify the profile identifier into class ServerSecuredProtocolPropertiesWithKnownPublicKey.");
			VERSION.addDescription(d);

			c = Calendar.getInstance();
			c.set(2019, Calendar.FEBRUARY, 8);
			d = new Description((short)1, (short)9, (short)4, Version.Type.Stable, (short)1, c.getTime());
			d.addItem("Code cleaning/optimizing.");
			VERSION.addDescription(d);



			c = Calendar.getInstance();
			c.set(2019, Calendar.FEBRUARY, 6);
			d = new Description((short)1, (short)9, (short)5, Version.Type.Stable, (short)1, c.getTime());
			d.addItem("Update OOD to 2.0.0 Beta 99.");
			d.addItem("Update Utils to 3.25.1");
			VERSION.addDescription(d);

			c = Calendar.getInstance();
			c.set(2019, Calendar.FEBRUARY, 5);
			d = new Description((short)1, (short)9, (short)2, Version.Type.Stable, (short)1, c.getTime());
			d.addItem("Update OOD to 2.0.0 Beta 97.");
			d.addItem("OOD - Security fix : disable cache for tables that use secret ou private keys");
			d.addItem("OOD - Security improvement : add Field.disableCache property");
			VERSION.addDescription(d);


			c = Calendar.getInstance();
			c.set(2019, Calendar.JANUARY, 18);
			d = new Description((short)1, (short)9, (short)1, Version.Type.Stable, (short)1, c.getTime());
			d.addItem("Update OOD to 2.0.0 Beta 95.");
			d.addItem("Set default OOD driver to H2 database.");
			VERSION.addDescription(d);

			c = Calendar.getInstance();
			c.set(2019, Calendar.JANUARY, 13);
			d = new Description((short)1, (short)9, (short)0, Version.Type.Stable, (short)1, c.getTime());
			d.addItem("Update OOD to 2.0.0 Beta 94.");
			d.addItem("Update Utils to 3.24.0.");
			d.addItem("Manage asymmetric auto-signed login.");
			d.addItem("Better computer manage sleep mode.");
			d.addItem("Manage moment of connection, and connection retry when failed");
			VERSION.addDescription(d);

			c = Calendar.getInstance();
			c.set(2018, Calendar.NOVEMBER, 24);
			d = new Description((short)1, (short)8, (short)2, Version.Type.Stable, (short)1, c.getTime());
			d.addItem("Improve security of Client/Server connection protocol.");
			d.addItem("Resolve a concurrent modification exception into internal role.");
			VERSION.addDescription(d);

			c = Calendar.getInstance();
			c.set(2018, Calendar.NOVEMBER, 23);
			d = new Description((short)1, (short)8, (short)1, Version.Type.Stable, (short)1, c.getTime());
			d.addItem("Improve security of Client/Server connection protocol.");
			VERSION.addDescription(d);

			c = Calendar.getInstance();
			c.set(2018, Calendar.NOVEMBER, 21);
			d = new Description((short)1, (short)8, (short)0, Version.Type.Stable, (short)1, c.getTime());
			d.addItem("Detect security anomalies during big data transfers.");
            d.addItem("Correction of Group.equals() with null references.");
            d.addItem("Better manage ban with deserialization process.");
			d.addItem("Update Utils to 3.22.1.");
			d.addItem("Update OOD to 2.0.0 Beta 91.");
			d.addItem("Add function AbstractAgent.getAccessibleKernelsFilteredByGroupsGivenByDistantPeer(Group).");
			d.addItem("Add function AbstractAgent.getAccessibleKernelsFilteredByGroupsGivenToDistantPeer(Group).");
			d.addItem("Check public key validity with client/server connexion protocol.");
            d.addItem("Add ConnectionProtocolNegotiator and ConnectionProtocolNegotiatorProperties classes.");
            d.addItem("Fix issue with client/server vs peer-to-peer protocol negotiation.");
			d.addItem("Optimizing a problem of simultaneous network data send to several peers : data was sent peer after peer, and not to all peers at the same time.");
			d.addItem("Add possibility to limit global download bandwidth and global upload bandwidth.");
			d.addItem("Detect OS wake up after a sleep mode.");
			d.addItem("Update Cling to 2.1.2.");
			d.addItem("Fix issue with router updating.");
			VERSION.addDescription(d);

			c = Calendar.getInstance();
			c.set(2018, Calendar.AUGUST, 1);
			d = new Description((short)1, (short)7, (short)6, Version.Type.Stable, (short)1, c.getTime());
			d.addItem("Update OOD to 2.0.0 Beta 86.");
			d.addItem("Update Utils to 3.19.0.");
			d.addItem("Add save functions into MadKit Properties.");
			d.addItem("Fix network messages serialization problem.");
			VERSION.addDescription(d);

			c = Calendar.getInstance();
			c.set(2018, Calendar.JULY, 27);
			d = new Description((short)1, (short)7, (short)5, Version.Type.Stable, (short)1, c.getTime());
			d.addItem("Update OOD to 2.0.0 Beta 85.");
			d.addItem("Update Utils to 3.18.0.");
			d.addItem("Save MKLE configuration that are different from a reference configuration. Other properties are not saved.");
			VERSION.addDescription(d);

			c = Calendar.getInstance();
			c.set(2018, Calendar.JULY, 20);
			d = new Description((short)1, (short)7, (short)3, Version.Type.Stable, (short)1, c.getTime());
			d.addItem("Update OOD to 2.0.0 Beta 84.");
			d.addItem("Update Utils to 3.17.0.");
			d.addItem("Fix version's control issue of distant peer.");
			VERSION.addDescription(d);

			c = Calendar.getInstance();
			c.set(2018, Calendar.JULY, 13);
			d = new Description((short)1, (short)7, (short)1, Version.Type.Stable, (short)1, c.getTime());
			d.addItem("Update OOD to 2.0.0 Beta 83.");
            d.addItem("Update Utils to 3.16.1.");
            d.addItem("Improve version's control of distant peer.");
            d.addItem("Clean code.");
			VERSION.addDescription(d);


			c = Calendar.getInstance();
			c.set(2018, Calendar.MAY, 20);
			d = new Description((short)1, (short)7, (short)0, Version.Type.Stable, (short)1, c.getTime());
			d.addItem("Update OOD to 2.0.0 Beta 82.");
			d.addItem("Update Utils to 3.15.0.");
			d.addItem("Add P2P connection protocol that support parametrisation of key agreement.");
			d.addItem("Support several key agreement (including Post Quantum Cryptography key agreement (New Hope)).");
			d.addItem("Fix security issue : when data is sent without being writed (default memory state), fill it with zeros.");
			d.addItem("Fix security issue : sign symmetric encryption key into client/server connection protocol.");
			d.addItem("Fix security issue : with P2P key agreements, generate signature and encryption keys with two steps (instead of one), in order to sign the exchanged symmetric encryption key.");
			d.addItem("Fix security issue : class serialization are now filtered with white list and black list. Classes that are not into white list must implement the interference 'SerializableAndSizable'. Messages sent to the network must implement the interface NetworkMessage.");
			d.addItem("Optimization : use externalization process instead of deserialization process during lan transfer.");
			d.addItem("Fix security issue : classes externalization processes control now the allocated memory during de-externalization phase.");
			d.addItem("Security enhancement : initialisation vectors used with encryption has now a secret part composed of counter that is increased at each data exchange.");
			d.addItem("Security enhancement : signature and encryption process use now a secret message that is increased at each data exchange.");
			d.addItem("Security enhancement : P2P login agreement use now JPAKE and a signature authentication if secret key for signature is available (PassworKey.getSecretKeyForSignature()).");
			d.addItem("Fix issue with dead lock into indirect connection process.");
			d.addItem("Fix issue with dual connection between two same kernels.");
			d.addItem("Externalising Java rewrited classes into JDKRewriteUtils project.");
			d.addItem("Support of authenticated encryption algorithms. When use these algorithms, MKLE do not add a signature with independent MAC.");
			d.addItem("Add some benchmarks.");
			d.addItem("Support of YAML file properties.");
			VERSION.addDescription(d);	

			
			c = Calendar.getInstance();
			c.set(2018, Calendar.FEBRUARY, 27);
			d = new Description((short)1, (short)6, (short)5, Version.Type.Stable, (short)1, c.getTime());
			d.addItem("Debug UPNP connexion with macOS.");
			d.addItem("Fix issue with multiple identical router's messages : do not remove the router to recreate it.");
			VERSION.addDescription(d);	
			
			c = Calendar.getInstance();
			c.set(2018, Calendar.FEBRUARY, 26);
			d = new Description((short)1, (short)6, (short)5, Version.Type.Stable, (short)1, c.getTime());
			d.addItem("Fiw a problem with UPNP connexion under macOS.");
			VERSION.addDescription(d);	
			
			c = Calendar.getInstance();
			c.set(2018, Calendar.FEBRUARY, 15);
			d = new Description((short)1, (short)6, (short)4, Version.Type.Stable, (short)1, c.getTime());
			d.addItem("Fix problem of port unbind with Windows.");
			d.addItem("Fix problem of simultaneous connections with Mac OS");
			d.addItem("Fix problem with interface address filtering");
			VERSION.addDescription(d);	
			
			c = Calendar.getInstance();
			c.set(2018, Calendar.FEBRUARY, 10);
			d = new Description((short)1, (short)6, (short)3, Version.Type.Stable, (short)1, c.getTime());
			d.addItem("Update OOD to 2.0.0 Beta 66.");
			d.addItem("Update Utils to 3.10.5");
			d.addItem("Change minimum public key size from 1024 to 2048");
			VERSION.addDescription(d);			
			
			c = Calendar.getInstance();
			c.set(2018, Calendar.FEBRUARY, 10);
			d = new Description((short)1, (short)6, (short)2, Version.Type.Stable, (short)1, c.getTime());
			d.addItem("Update OOD to 2.0.0 Beta 65.");
			d.addItem("Update Utils to 3.10.4");
			d.addItem("Change minimum public key size from 1024 to 2048");
			VERSION.addDescription(d);
			
			c = Calendar.getInstance();
			c.set(2018, Calendar.FEBRUARY, 4);
			d = new Description((short)1, (short)6, (short)1, Version.Type.Stable, (short)1, c.getTime());
			d.addItem("Overlookers were not aware from new roles adding. Fix this issue.");
			d.addItem("Add MadKit demos");
			VERSION.addDescription(d);
			
			c = Calendar.getInstance();
			c.set(2018, Calendar.JANUARY, 31);
			d = new Description((short)1, (short)6, (short)0, Version.Type.Stable, (short)1, c.getTime());
			d.addItem("Updating OOD to 2.0.0 Beta 59");
			d.addItem("Updating Utils to 3.9.0");
			d.addItem("Messages can now be atomically non encrypted");
			VERSION.addDescription(d);
			
			c = Calendar.getInstance();
			c.set(2017, Calendar.DECEMBER, 13);
			d = new Description((short)1, (short)5, (short)2, Version.Type.Stable, (short)1, c.getTime());
			d.addItem("Updating OOD to 2.0.0 Beta 57");
			d.addItem("Updating Utils to 3.7.1");
			d.addItem("Debugging JavaDoc");
			VERSION.addDescription(d);

			c = Calendar.getInstance();
			c.set(2017, Calendar.NOVEMBER, 13);
			d = new Description((short)1, (short)5, (short)0, Version.Type.Stable, (short)1, c.getTime());
			d.addItem("Updating OOD to 2.0.0 Beta 55");
			d.addItem("Packets can now have sizes greater than Short.MAX_VALUE");
			VERSION.addDescription(d);

			c = Calendar.getInstance();
			c.set(2017, Calendar.NOVEMBER, 2);
			d = new Description((short)1, (short)4, (short)5, Version.Type.Stable, (short)1, c.getTime());
			d.addItem("Updating OOD to 2.0.0 Beta 54");
			VERSION.addDescription(d);

			c = Calendar.getInstance();
			c.set(2017, Calendar.OCTOBER, 13);
			d = new Description((short)1, (short)4, (short)0, Version.Type.Stable, (short)1, c.getTime());
			d.addItem("Updating OOD to 2.0.0 Beta 48");
			d.addItem("Several modifications into connection and access protocols");
			d.addItem("Adding approved randoms parameters into MaDKitProperties");
			d.addItem("Adding point to point transfer connection signature and verification");
			d.addItem("Saving automatically random's seed to be reload with the next application loading");
			VERSION.addDescription(d);

			c = Calendar.getInstance();
			c.set(2017, Calendar.AUGUST, 31);
			d = new Description((short)1, (short)2, (short)1, Version.Type.Stable, (short)1, c.getTime());
			d.addItem("Including resources in jar files");
			VERSION.addDescription(d);

			c = Calendar.getInstance();
			c.set(2017, Calendar.AUGUST, 5);
			d = new Description((short)1, (short)2, (short)0, Version.Type.Stable, (short)1, c.getTime());
			d.addItem("Correction a problem with database");
			d.addItem("Adding P2PSecuredConnectionProtocolWithECDHAlgorithm connection protocol (speediest)");
			d.addItem("Adding Client/ServerSecuredConnectionProtocolWithKnwonPublicKeyWithECDHAlgorithm connection protocol (speediest)");
			d.addItem("Now all connection protocols use different keys for encryption and for signature");
			d.addItem("Adding AccessProtocolWithP2PAgreement (speediest)");
			d.addItem("Debugging desktop JFrame closing (however the JMV still become opened when all windows are closed)");
			d.addItem("Several minimal bug fix");
			d.addItem("Correction of JavaDoc");
			d.addItem("Updating OOD to 2.0.0 Beta 20 version");
			VERSION.addDescription(d);

			c = Calendar.getInstance();
			c.set(2017, Calendar.AUGUST, 5);
			d = new Description((short)1, (short)1, (short)3, Version.Type.Stable, (short)1, c.getTime());
			d.addItem("Updating OOD to 2.0.0 Beta 15");
			VERSION.addDescription(d);

			c = Calendar.getInstance();
			c.set(2017, Calendar.AUGUST, 5);
			d = new Description((short)1, (short)1, (short)2, Version.Type.Stable, (short)1, c.getTime());
			d.addItem("Updating OOD to 2.0.0 Beta 14");
			d.addItem("Optimizing some memory leak tests");
			VERSION.addDescription(d);

			c = Calendar.getInstance();
			c.set(2017, Calendar.AUGUST, 4);
			d = new Description((short)1, (short)1, (short)0, Version.Type.Stable, (short)1, c.getTime());
			d.addItem("Convert project to Gradle project");
			VERSION.addDescription(d);

			c = Calendar.getInstance();
			c.set(2017, Calendar.JUNE, 4);
			d = new Description((short)1, (short)0, (short)0, Version.Type.Stable, (short)1, c.getTime());
			d.addItem("Correction of a bug with database disconnection");
			d.addItem("Debugging indirect connections");
			d.addItem("Solving a memory leak problem with ConversationID");
			d.addItem("Solving a memory leak problem with TransferAgent (not killed)");
			d.addItem("Solving problem when deny BigDataProposition and kill agent just after");
			d.addItem("Indirect connection send now ping message");
			d.addItem("Adding white list for InetAddresses in network properties");
			d.addItem("Correcting problems of internal group/role references/dereferences");
			VERSION.addDescription(d);

			c = Calendar.getInstance();
			c.set(2017, Calendar.MAY, 27);
			d = new Description((short)1, (short)0, (short)0, Version.Type.Beta, (short)4, c.getTime());
			d.addItem("Agents are now identified by a long (and not int)");
			d.addItem("Adding the function AbstractAgent.getAgentID()");
			d.addItem("Removing static elements in Conversation ID");
			VERSION.addDescription(d);

			c = Calendar.getInstance();
			c.set(2017, Calendar.MAY, 23);
			d = new Description((short)1, (short)0, (short)0, Version.Type.Beta, (short)3, c.getTime());
			d.addItem("Update Utils to 2.7.1");
			d.addItem("Update OOD to 2.0.0 Beta 1");
			d.addItem("JDK 7 compatible");
			VERSION.addDescription(d);

			c = Calendar.getInstance();
			c.set(2017, Calendar.MARCH, 7);
			d = new Description((short)1, (short)0, (short)0, Version.Type.Beta, (short)2, c.getTime());
			d.addItem("Reinforce secret identifier/password exchange");
			d.addItem("Add agent to launch into MKDesktop windows");
			VERSION.addDescription(d);

			c = Calendar.getInstance();
			c.set(2017, Calendar.MARCH, 4);
			d = new Description((short)1, (short)0, (short)0, Version.Type.Beta, (short)0, c.getTime());
			d.addItem("First MaDKitLanEdition release, based on MaDKit");
			VERSION.addDescription(d);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return VERSION;
	}
	static MadkitProperties generateDefaultMadkitConfig()
    {
        MadkitProperties res=new MadkitProperties();

        try {
            res.loadYAML(new File("com/distrimind/madkit/kernel/madkit.yaml"));
        } catch (Exception ignored) {
            try {
                res.loadYAML(Madkit.class.getResourceAsStream("madkit.yaml"));
            } catch (Exception ignored2) {

            }
        }
        return res;
    }
	public static Version getVersion()
	{
		if (VERSION==null)
		{
			synchronized(Madkit.class)
			{
				if (VERSION==null)
				{
					VERSION=getNewVersionInstance();
					defaultConfig=generateDefaultMadkitConfig();

                    Runtime.getRuntime().addShutdownHook(new Thread() {

                        @Override
                        public void run() {// just in case (like ctrl+c)
                            AgentLogger.resetLoggers();
                        }
                    });

                    WEB=defaultConfig.madkitWeb;


				}
			}
		}
		return VERSION;
	}
	private volatile static URL WEB;

	public static URL getWEB()
	{
		getVersion();
		return WEB;
	}
	
	final private MadkitProperties madkitConfig;
	// private Element madkitXMLConfigFile = null;
	// private FileHandler madkitLogFileHandler;
	final private MadkitKernel myKernel;
	private Logger logger;
	// TODO Remove unused code found by UCDetector
	// String cmdLine;
	String[] args = null;
	final KernelAddress kernelAddress;

	/**
	 * This main could be used to launch a new kernel using predefined options. The
	 * new kernel automatically ends when all the agents living on this kernel are
	 * done. So the JVM automatically quits if there is no other remaining threads.
	 * 
	 * Basically this call just instantiates a new kernel like this:
	 * 
	 * <pre>
	 * public static void main(String[] options) {
	 * 	new Madkit(options);
	 * }
	 * </pre>
	 * 
	 * @param options
	 *            the options which should be used to launch Madkit: see {@link MadkitProperties}
	 */
	public static void main(String[] options) {
		new Madkit(options);
	}

	/**
	 * Makes the kernel do the corresponding action. This is done by sending a
	 * message directly to the kernel agent. This should not be used intensively
	 * since it is better to control the execution flow of the application using the
	 * agents running in the kernel. Still it provides a way to launch and manage a
	 * kernel from any java application as a third party service.
	 * 
	 * <pre>
	 * public void somewhereInYourCode() {
	 * 				...
	 * 				Madkit m = new Madkit(args);
	 * 				...
	 * 				m.doAction(KernelAction.LAUNCH_NETWORK); //start the network
	 * 				...
	 * 				m.doAction(KernelAction.LAUNCH_AGENT, new Agent(), true); //launch a new agent with a GUI
	 * 				...
	 * }
	 * </pre>
	 * 
	 * @param action
	 *            the action to request
	 * @param parameters
	 *            the parameters of the request
	 */
	public void doAction(KernelAction action, Object... parameters) {
		if (myKernel.isAlive()) {
			myKernel.receiveMessage(new KernelMessage(action, parameters));
		} else if (logger != null) {
			logger.severe("my kernel is terminated...");
		}
	}

	/**
	 * Launch a new kernel with predefined options. The call returns when the new
	 * kernel has finished to take care of all options. Moreover the kernel
	 * automatically ends when all the agents living on this kernel are done.
	 * <p>
	 * 
	 * 
	 * @param options
	 *            the options which should be used to launch Madkit. If
	 *            <code>null</code>, the dektop mode is automatically used.
	 * 
	 * @see MadkitProperties
	 * @see NetworkProperties
	 * 
	 */
	public Madkit(String... options) {
		this(new MadkitEventListener() {

			@Override
			public void onMadkitPropertiesLoaded(MadkitProperties _properties) {

			}
		}, options);
	}

	/**
	 * Launch a new kernel with predefined options. The call returns when the new
	 * kernel has finished to take care of all options. Moreover the kernel
	 * automatically ends when all the agents living on this kernel are done.
	 * <p>
	 * 
	 * Here is an example of use:
	 * <p>
	 * 
	 * 
	 * @param eventListener
	 *            the event listener called when events occurs during Madkit life
	 *            cycle
	 * @param options
	 *            the options which should be used to launch Madkit. If
	 *            <code>null</code>, the dektop mode is automatically used.
	 * 
	 * @see MadkitProperties
	 * @see NetworkProperties
	 */
	public Madkit(MadkitEventListener eventListener, String... options) {
		this(generateDefaultMadkitConfig(), null, eventListener, options);
	}
    /**
     * Launch a new kernel with predefined options. The call returns when the new
     * kernel has finished to take care of all options. Moreover the kernel
     * automatically ends when all the agents living on this kernel are done.
     * <p>
     *
     * Here is an example of use:
     * <p>
     *
     * @param madkitConfig the initial MadKit configuration
     * @param eventListener
     *            the event listener called when events occurs during Madkit life
     *            cycle
     * @param options
     *            the options which should be used to launch Madkit. If
     *            <code>null</code>, the dektop mode is automatically used.
     *
     * @see MadkitProperties
     * @see NetworkProperties
     */
    public Madkit(MadkitProperties madkitConfig, MadkitEventListener eventListener, String... options) {
        this(madkitConfig, null, eventListener, options);
    }
    /**
     * Launch a new kernel with predefined options. The call returns when the new
     * kernel has finished to take care of all options. Moreover the kernel
     * automatically ends when all the agents living on this kernel are done.
     * <p>
     *
     * Here is an example of use:
     * <p>
     *
     *
     * @param madkitConfig
     *            the initial MadKit configuration
     * @param options
     *            the options which should be used to launch Madkit. If
     *            <code>null</code>, the dektop mode is automatically used.
     *
     * @see MadkitProperties
     * @see NetworkProperties
     */
    public Madkit(MadkitProperties madkitConfig, String... options) {
        this(madkitConfig, null, new MadkitEventListener() {

            @Override
            public void onMadkitPropertiesLoaded(MadkitProperties _properties) {

            }
        }, options);
    }
	Madkit(MadkitProperties madkitProperties, KernelAddress kernelAddress, MadkitEventListener eventListener, String... options) {
		if (eventListener == null)
			throw new NullPointerException("eventListener");
		this.dateFormat=new SimpleDateFormat("yyyy.MM.dd.HH.mm.ss");
		this.kernelAddress = kernelAddress;
		final ArrayList<String> argsList = new ArrayList<>();

		if (options != null) {
			for (String string : options) {
				argsList.addAll(Arrays.asList(string.trim().split("\\s+")));
			}
			this.args = argsList.toArray(new String[0]);
		}

		/*try {
			madkitConfig.loadYAML(getClass().getResourceAsStream("madkit.yaml"));
		} catch (PropertiesParseException | IOException e) {
			e.printStackTrace();
		}
		*/
		this.madkitConfig=madkitProperties.clone();
		this.madkitConfig.setReference(madkitProperties);
		final Properties fromArgs = buildConfigFromArgs(args);
		try {
			madkitConfig.loadFromProperties(fromArgs);
		} catch (Exception e) {
			e.printStackTrace();
		}
		initMadkitLogging();
		logger.finest("command line args : " + fromArgs);
		loadJarFileArguments();
		if (loadConfigFiles())// overriding config file
			loadJarFileArguments();
		logger.fine("** OVERRIDING WITH COMMAND LINE ARGUMENTS **");
		try {
			madkitConfig.loadFromProperties(fromArgs);
		} catch (Exception e) {
			e.printStackTrace();
		}

		eventListener.onMadkitPropertiesLoaded(madkitConfig);

		I18nUtilities.setI18nDirectory(madkitConfig.i18nDirectory);
		logger.finest(MadkitClassLoader.getLoader().toString());
		// activating desktop if no agent at this point and desktop has not been set
		if (!madkitConfig.desktop && !madkitConfig.forceDesktop
				&& (madkitConfig.launchAgents == null || madkitConfig.launchAgents.size() == 0)
				&& madkitConfig.configFiles == null) {
			logger.fine("LaunchAgents && configFile == null : Activating desktop");
			madkitConfig.desktop = true;
		}
		logSessionConfig(madkitConfig, Level.FINER);
		myKernel = new MadkitKernel(this);

		logger.finer("**  MADKIT KERNEL CREATED **");

		printWelcomeString();
		// if(madkitClassLoader.getAvailableConfigurations().isEmpty() //TODO
		// && ! madkitConfig.get(Option.launchAgents.name()).equals("null")){
		// madkitClassLoader.addMASConfig(new MASModel(Words.INITIAL_CONFIG.toString(),
		// args, "desc"));
		// }

		// this.cmdLine =
		// System.getProperty("java.home")+File.separatorChar+"bin"+File.separatorChar+"java
		// -cp "+System.getProperty("java.class.path")+" madkit.kernel.Madkit ";

		startKernel();
	}





	/**
	 * 
	 */
	private void loadJarFileArguments() {
		String[] options;
		logger.fine("** LOADING JAR FILE ARGUMENTS **");
		try {
			for (Enumeration<URL> urls = Madkit.class.getClassLoader().getResources("META-INF/MANIFEST.MF"); urls
					.hasMoreElements();) {
				Manifest manifest = new Manifest(urls.nextElement().openStream());
				// if(logger != null)
				// logger.fine(manifest.toString());
				// for (Map.Entry<String, Attributes> e : manifest.getEntries().entrySet()) {
				// System.err.println("\n"+e.getValue().values());
				// }
				Attributes projectInfo = manifest.getAttributes("MaDKit-Project-Info");
				if (projectInfo != null) {
					logger.finest("found project info \n\t" + projectInfo.keySet() + "\n\t" + projectInfo.values());
					options = projectInfo.getValue("MaDKit-Args").trim().split("\\s+");
					logger.finer("JAR FILE ARGUMENTS = " + Arrays.deepToString(options));
					madkitConfig.loadFromProperties(buildConfigFromArgs(options));
					// madkitConfig.projectVersion=projectInfo.getValue("Project-Version");

				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void initMadkitLogging() {
		final Level l = madkitConfig.madkitLogLevel;
		logger = Logger.getLogger(MDK_LOGGER_NAME);
		logger.setUseParentHandlers(false);
		logger.setLevel(l);
		ConsoleHandler cs = new ConsoleHandler();
		cs.setLevel(l);
		cs.setFormatter(AgentLogger.AGENT_FORMATTER);
		logger.addHandler(cs);
		logger.fine("** LOGGING INITIALIZED **");
	}

	private boolean loadConfigFiles() {
		boolean ok = false;
		ArrayList<File> configFiles = madkitConfig.configFiles;
		if (configFiles != null) {
			for (File f : configFiles) {
				try {
					madkitConfig.load(f);
					ok = true;
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		return ok;
	}

	/**
	 * 
	 */
	private void startKernel() {
		// starting the kernel agent and waiting the end of its activation
		logger.fine("** LAUNCHING KERNEL AGENT **");
		myKernel.launchAgent(myKernel, myKernel, Integer.MAX_VALUE, false);
	}

	@Override
	public String toString() {
		return myKernel.toString() + " @ " + myKernel.getKernelAddress();
	}

	/**
	 * 
	 */
	private void printWelcomeString() {
		Version VERSION=getVersion();
		if (!(madkitConfig.madkitLogLevel == Level.OFF)) {
			Calendar startCal = Calendar.getInstance();
			Calendar endCal = Calendar.getInstance();
			startCal.setTime(VERSION.getProjectStartDate());
			endCal.setTime(VERSION.getProjectEndDate());
			Calendar c=new GregorianCalendar();
			c.setTime(VERSION.getProjectEndDate());
			System.out.println("\n-----------------------------------------------------------------------------"
					+ "\n\t\t\t\t    MadkitLanEdition\n" + "\n\t Version: " + VERSION.getMajor() + "."
					+ VERSION.getMinor() + "." + VERSION.getRevision() + " " + VERSION.getType()
					+ (VERSION.getType().equals(Version.Type.Stable) ? "" : (" " + VERSION.getAlphaBetaVersion()))
					+ "\n\t MaDKit Team (c) 1997-"+c.get(Calendar.YEAR)
					+ "\n\t MadkitLanEdition Team (c) " + startCal.get(Calendar.YEAR) + "-" + endCal.get(Calendar.YEAR)
					+ "\n\t Kernel " + myKernel.getNetworkID()
					+ "\n-----------------------------------------------------------------------------\n");
		}
	}

	private void logSessionConfig(MadkitProperties session, Level lvl) {
		StringBuilder message = new StringBuilder("MaDKit current configuration is\n\n");
		message.append("\t--- MaDKit regular options ---\n");
        Properties properties;
        try {
            properties = session.convertToStringProperties();
            for (Entry<Object, Object> option : properties.entrySet()) {
                message.append("\t").append(String.format("%-" + 30 + "s", option.getKey())).append(option.getValue()).append("\n");
            }
        } catch (PropertiesParseException e) {
            e.printStackTrace();
        }

		logger.log(lvl, message.toString());
	}

	Properties buildConfigFromArgs(final String[] options) {
		Properties currentMap = new Properties();
		if (options != null && options.length > 0) {
			StringBuilder parameters = new StringBuilder();
			String currentOption = null;
			for (int i = 0; i < options.length; i++) {
				if (!options[i].trim().isEmpty()) {
					if (options[i].startsWith("--")) {
						currentOption = options[i].substring(2).trim();
						currentMap.put(currentOption, "true");
						parameters = new StringBuilder();
					} else {
						if (currentOption == null) {
							System.err.println(
									"\n\t\t!!!!! MADKIT WARNING !!!!!!!!!!!\n\t\tNeeds an option with -- to start with\n\t\targs was : "
											+ Arrays.deepToString(options));
							return currentMap;
						}
						parameters.append(options[i]);
						if (i + 1 == options.length || options[i + 1].startsWith("--")) {
							String currentValue = currentMap.getProperty(currentOption);
							if (currentOption.equals("configFiles") && !currentValue.equals("true")) {
								currentMap.put(currentOption, currentValue + ';' + parameters.toString().trim());// TODO bug on "-"
																										// use
							} else {
								currentMap.put(currentOption, parameters.toString().trim());// TODO bug on "-" use
							}
						} else {
							if (currentOption.equals("launchAgents")) {
								parameters.append(",");
							} else
								parameters.append(" ");
						}

					}
				}
			}
		}
		return currentMap;
	}

	MadkitProperties getConfigOption() {
		return madkitConfig;
	}

	/**
	 * only for junit
	 * 
	 * @return the kernel
	 */
	MadkitKernel getKernel() {
		return myKernel;
	}

	KernelAddress getKernelAddress()
	{
		return this.getKernel().getKernelAddress();
	}

	static
	{
		ReflectionTools.setClassLoader(MadkitClassLoader.getSystemClassLoader());
		initPreloadedClasses();
	}

	private static void initPreloadedClasses()
	{
		try {
			//noinspection unchecked
			ArrayList<Class<? extends SecureExternalizableWithoutInnerSizeControl>> classes = new ArrayList<>(Arrays.asList(
					KernelAddress.class, KernelAddressInterfaced.class, AgentAddress.class, ConversationID.class, MultiGroup.class, Group.class, DoubleIP.class, MultipleIP.class,
					HostIP.class, (Class<? extends SecureExternalizableWithoutInnerSizeControl>) Class.forName("com.distrimind.madkit.kernel.network.AcceptedGroups"),
					(Class<? extends SecureExternalizableWithoutInnerSizeControl>) Class.forName("com.distrimind.madkit.kernel.network.BroadcastLanMessage"),
					(Class<? extends SecureExternalizableWithoutInnerSizeControl>) Class.forName("com.distrimind.madkit.kernel.network.LocalLanMessage"),
					(Class<? extends SecureExternalizableWithoutInnerSizeControl>) Class.forName("com.distrimind.madkit.kernel.network.TransferClosedSystemMessage"),
					(Class<? extends SecureExternalizableWithoutInnerSizeControl>) Class.forName("com.distrimind.madkit.kernel.network.TransferConfirmationSystemMessage"),
					(Class<? extends SecureExternalizableWithoutInnerSizeControl>) Class.forName("com.distrimind.madkit.kernel.network.TransferImpossibleSystemMessage"),
					(Class<? extends SecureExternalizableWithoutInnerSizeControl>) Class.forName("com.distrimind.madkit.kernel.network.TransferImpossibleSystemMessageFromMiddlePeer"),
					(Class<? extends SecureExternalizableWithoutInnerSizeControl>) Class.forName("com.distrimind.madkit.kernel.network.TransferBlockCheckerSystemMessage"),
					(Class<? extends SecureExternalizableWithoutInnerSizeControl>) Class.forName("com.distrimind.madkit.kernel.network.TransferPropositionSystemMessage"),
					(Class<? extends SecureExternalizableWithoutInnerSizeControl>) Class.forName("com.distrimind.madkit.kernel.network.TransferAgent$IDTransfer"),
					(Class<? extends SecureExternalizableWithoutInnerSizeControl>) Class.forName("com.distrimind.madkit.kernel.network.TransferAgent$DirectConnection"),
					(Class<? extends SecureExternalizableWithoutInnerSizeControl>) Class.forName("com.distrimind.madkit.kernel.network.TransferAgent$DirectConnectionFailed"),
					(Class<? extends SecureExternalizableWithoutInnerSizeControl>) Class.forName("com.distrimind.madkit.kernel.network.TransferAgent$DirectConnectionSuceeded"),
					(Class<? extends SecureExternalizableWithoutInnerSizeControl>) Class.forName("com.distrimind.madkit.kernel.network.TransferAgent$TryDirectConnection"),
					BigDataTransferID.class,
					(Class<? extends SecureExternalizableWithoutInnerSizeControl>) Class.forName("com.distrimind.madkit.kernel.network.CGRSynchrosSystemMessage"),
					(Class<? extends SecureExternalizableWithoutInnerSizeControl>) Class.forName("com.distrimind.madkit.kernel.network.CGRSynchroSystemMessage"),
					CGRSynchro.class,
					(Class<? extends SecureExternalizableWithoutInnerSizeControl>) Class.forName("com.distrimind.madkit.kernel.network.ConnectionInfoSystemMessage"),
					(Class<? extends SecureExternalizableWithoutInnerSizeControl>) Class.forName("com.distrimind.madkit.kernel.network.DataToBroadcast"),
					DistantKernelAddressValidated.class,
					(Class<? extends SecureExternalizableWithoutInnerSizeControl>) Class.forName("com.distrimind.madkit.kernel.network.connection.access.IdentifiersPropositionMessage"),
					Identifier.class, EncryptedIdentifier.class,
					(Class<? extends SecureExternalizableWithoutInnerSizeControl>) Class.forName("com.distrimind.madkit.kernel.network.connection.access.JPakeMessage"),
					(Class<? extends SecureExternalizableWithoutInnerSizeControl>) Class.forName("com.distrimind.madkit.kernel.network.connection.secured.KeyAgreementDataMessage"),
					(Class<? extends SecureExternalizableWithoutInnerSizeControl>) Class.forName("com.distrimind.madkit.kernel.network.connection.access.LoginConfirmationMessage"),
					(Class<? extends SecureExternalizableWithoutInnerSizeControl>) Class.forName("com.distrimind.madkit.kernel.network.connection.access.NewLocalLoginAddedMessage"),
					(Class<? extends SecureExternalizableWithoutInnerSizeControl>) Class.forName("com.distrimind.madkit.kernel.network.connection.access.NewLocalLoginRemovedMessage"),
					(Class<? extends SecureExternalizableWithoutInnerSizeControl>) Class.forName("com.distrimind.madkit.kernel.network.PingMessage"),
					(Class<? extends SecureExternalizableWithoutInnerSizeControl>) Class.forName("com.distrimind.madkit.kernel.network.PongMessage"),
					PointToPointTransferedBlockChecker.class,
					(Class<? extends SecureExternalizableWithoutInnerSizeControl>) Class.forName("com.distrimind.madkit.kernel.network.ValidateBigDataProposition"),
					EnumMessage.class,
					NetworkObjectMessage.class, TaskID.class,
					ACLMessage.class, ActMessage.class, BigDataPropositionMessage.class, BigDataResultMessage.class,
					KernelMessage.class, StringMessage.class, (Class<? extends SecureExternalizableWithoutInnerSizeControl>) Class.forName("com.distrimind.madkit.message.hook.OrganizationEvent"),
					SchedulingMessage.class, KQMLMessage.class, IntegerMessage.class, BooleanMessage.class,
					GUIMessage.class, (Class<? extends SecureExternalizableWithoutInnerSizeControl>) Class.forName("com.distrimind.madkit.kernel.InternalRole"),
					EncryptedPassword.class, GUIMessage.class, (Class<? extends SecureExternalizableWithoutInnerSizeControl>) Class.forName("com.distrimind.madkit.kernel.network.DatagramLocalNetworkPresenceMessage"),
					EncryptedCloudIdentifier.class));
			for (Class<?> c : classes)
				assert !Modifier.isAbstract(c.getModifiers()):""+c;
			//noinspection unchecked
			ArrayList<Class<? extends Enum<?>>> enums = new ArrayList<>(new HashSet<>(Arrays.asList(
					AbstractAgent.ReturnCode.class,
					AbstractAgent.State.class,
					(Class<? extends Enum<?>>) Class.forName("com.distrimind.madkit.kernel.CGRSynchro$Code"),
					ConnectionProtocol.ConnectionState.class,
					MessageDigestType.class,
					SecureRandomType.class,
					SymmetricEncryptionType.class,
					SymmetricAuthentifiedSignatureType.class,
					ASymmetricEncryptionType.class,
					ASymmetricAuthenticatedSignatureType.class,
					KeyAgreementType.class,
					PasswordHashType.class,
					SymmetricKeyWrapperType.class,
					ASymmetricKeyWrapperType.class,
					ASymmetricLoginAgreementType.class,
					CodeProvider.class,
					EllipticCurveDiffieHellmanType.class,
					P2PLoginAgreementType.class,
					PasswordBasedKeyGenerationType.class,
					Version.Type.class,
					AgentAction.class,
					GUIManagerAction.class,
					SchedulingAction.class,
					AbstractAgent.KillingType.class,
					BigDataResultMessage.Type.class,
					(Class<? extends Enum<?>>) Class.forName("com.distrimind.madkit.kernel.MultiGroup$CONTAINS"),
					(Class<? extends Enum<?>>) Class.forName("com.distrimind.madkit.kernel.NetCode"),
					NetworkAgent.NetworkCloseReason.class,
					Scheduler.SimulationState.class,
					AskForTransferMessage.Type.class,
					ConnectionStatusMessage.Type.class,
					ConnectionProtocol.ConnectionClosedReason.class,
					OS.class,
					OSVersion.class,
					DatabaseEventType.class,
					TransactionIsolation.class,
					HookMessage.AgentActionEvent.class
			)));
			SerializationTools.setPredefinedClasses(classes, enums);
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
			System.exit(-1);
		}
	}

}
