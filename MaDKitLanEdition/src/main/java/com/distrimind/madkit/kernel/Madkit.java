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
import com.distrimind.madkit.action.KernelAction;
import com.distrimind.madkit.action.SchedulingAction;
import com.distrimind.madkit.i18n.I18nUtilities;
import com.distrimind.madkit.kernel.network.*;
import com.distrimind.madkit.kernel.network.connection.ConnectionProtocol;
import com.distrimind.madkit.kernel.network.connection.PointToPointTransferedBlockChecker;
import com.distrimind.madkit.kernel.network.connection.access.EncryptedPassword;
import com.distrimind.madkit.kernel.network.connection.access.GroupsRoles;
import com.distrimind.madkit.kernel.network.connection.access.Identifier;
import com.distrimind.madkit.kernel.network.connection.access.ListGroupsRoles;
import com.distrimind.madkit.message.*;
import com.distrimind.madkit.message.hook.HookMessage;
import com.distrimind.util.OS;
import com.distrimind.util.OSVersion;
import com.distrimind.util.ReflectionTools;
import com.distrimind.util.io.SecureExternalizableWithoutInnerSizeControl;
import com.distrimind.util.io.SerializationTools;
import com.distrimind.util.properties.PropertiesParseException;
import com.distrimind.util.version.Description;
import com.distrimind.util.version.Person;
import com.distrimind.util.version.PersonDeveloper;
import com.distrimind.util.version.Version;
import static com.distrimind.util.version.DescriptionType.*;
/**
 * MaDKit 5 starter class.
 * 
 * <h2>MaDKit v.5 new features</h2>
 * 
 * <ul>
 * <li>One big change that comes with version 5 is how agents are identified and
 * localized within the artificial society. An agent is no longer bound to a
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
 * @version 5.6
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
	
	private volatile static Version VERSION=null;

	static Version getNewVersionInstance()
	{
		Version VERSION = new Version("MaDKitLanEdition", "MKLE", "2015-05-22");
		try {

			InputStream is = Madkit.class.getResourceAsStream("build.txt");
			if (is!=null)
				VERSION.loadBuildNumber(is);

			VERSION.addCreator(new Person("Mahdjoub", "Jason"))
					.addDeveloper(new PersonDeveloper("Mahdjoub", "Jason", "2015-05-22"))
					.addDeveloper(new PersonDeveloper("Michel", "Fabien", "1997-02-01"))
					.addDeveloper(new PersonDeveloper("Gutknecht", "Olivier", "1997-02-01"))
					.addDeveloper(new PersonDeveloper("Ferber", "Jacques", "1997-02-01"))
					.addDescription(new Description((short)2, (short)4, (short)0, Version.Type.STABLE, (short)0, "2022-03-03")
							.addItem(INTERNAL_CHANGE, "Use a forked version of Cling (UPNP IGD)")
							.addItem(INTERNAL_CHANGE, "UPNP IGD : fix issue with JDK17")
							.addItem(NEW_FEATURE, "Separate Madkit GUI to a different library in order to remove Swing dependencies, and in order to make MKLE compatible with Android")
							.addItem(NEW_FEATURE, "Test MaDKitLanEdition with Android (API 26+)")
					)
					.addDescription(new Description((short)2, (short)3, (short)7, Version.Type.STABLE, (short)0, "2022-02-03")
							.addItem(INTERNAL_CHANGE, "Update URLs")
					)
					.addDescription(new Description((short)2, (short)3, (short)6, Version.Type.STABLE, (short)0, "2022-01-31")
							.addItem(INTERNAL_CHANGE, "Do not replace MaDKit properties reference when loading Madkit kernel")
					)
					.addDescription(new Description((short)2, (short)3, (short)5, Version.Type.STABLE, (short)0, "2022-01-25")
							.addItem(INTERNAL_CHANGE, "Update Utils to 5.22.1 STABLE")
							.addItem(INTERNAL_CHANGE, "Update OOD to 3.1.16 STABLE")
							.addItem(INTERNAL_CHANGE, "Confirm chosen connection protocol with ConnectionProtocolNegotiator after chosen connection protocol has established the connection")
					)
					.addDescription(new Description((short)2, (short)3, (short)4, Version.Type.STABLE, (short)0, "2021-12-29")
							.addItem(INTERNAL_CHANGE, "Optimize group deserialization")
							.addItem(INTERNAL_CHANGE, "Better manage exception when loading XML properties files")
					)
					.addDescription(new Description((short)2, (short)3, (short)3, Version.Type.STABLE, (short)0, "2021-12-21")
							.addItem(INTERNAL_CHANGE, "Add possibility to disable hash checking when encryption algorithm is authenticated")
							.addItem(SECURITY_FIX, "Replay was not activated when transmitting non encrypted data. Fix it. Encrypted messages was not concerned by this issue.")
							.addItem(SECURITY_FIX, "Fix bad arrays comparison when comparing secret messages. Comparison where not done in constant time.")
							.addItem(SECURITY_FIX, "Update OOD to 3.1.14 STABLE. ")
							.addItem(SECURITY_FIX, "Update Utils to 5.21.7 STABLE. Fix bad arrays comparison, for example when comparing signatures. Comparison where not done in constant time. This should not produce necessary security issue, but if it does, this is a serious problem since secret message can be deduced. In MKLE, no severe leak was detected.")
							.addItem(BUG_FIX, "Fix bad filtering of roles events over network")
					)
					.addDescription(new Description((short)2, (short)3, (short)2, Version.Type.STABLE, (short)0, "2021-12-19")
							.addItem(INTERNAL_CHANGE, "Reimplement Task<?> class with simplifications/optimizations")
					)
					.addDescription(new Description((short)2, (short)3, (short)1, Version.Type.STABLE, (short)0, "2021-12-16")
							.addItem(INTERNAL_CHANGE, "Update Utils to 5.21.5 STABLE")
							.addItem(INTERNAL_CHANGE, "Update OOD to 3.1.12 STABLE")
							.addItem(INTERNAL_CHANGE, "Better changelog generation")
					)
					.addDescription(new Description((short)2, (short)3, (short)0, Version.Type.STABLE, (short)0, "2021-12-16")
							.addItem(NEW_FEATURE, "Update OOD to 3.1.11 STABLE")
							.addItem(NEW_FEATURE, "Update Utils to 5.21.4 STABLE")
							.addItem(INTERNAL_CHANGE, "Update Gradle to 7.3.1")
							.addItem(NEW_FEATURE, "Add possibility to send differed big data messages")
							.addItem(NEW_FEATURE, "Add possibility to cancel big message sending, during its emission")
							.addItem(INTERNAL_CHANGE, "Better cancel big message when connexion was lost")
							.addItem(INTERNAL_CHANGE, "Optimize locking during the sending and the reception of messages")
							.addItem(NEW_FEATURE, "Permit to cancel asynchronous messages sending if a time out has been reached")
							.addItem(NEW_FEATURE, "Add possibility to schedule obsolete data cleaning into MaDKit database")
							.addItem(INTERNAL_CHANGE, "Minimize use of garbage collector during transfers")
							.addItem(BUG_FIX, "Fix bad synchronization of distant available agents")
							.addItem(BUG_FIX, "Fix bad asynchronous messages sending when using network")
							.addItem(BUG_FIX, "Fix deadlocks during asynchronous message sending")
							.addItem(BUG_FIX, "Fix potential deadlocks during network message sending, when an error occurs, or during peer disconnection")
							.addItem(BUG_FIX, "Fix potential deadlocks when canceling message receiving. Maximum data loaded into memory was not decremented when a transfer was canceled")
							.addItem(NEW_FEATURE, "Add function AgentFakeThread.synchronizeActionWithLiveByCycleFunction")
							.addItem(NEW_FEATURE, "Add function RealTimeTransferStat.getNumberOfIdentifiedBytesFromCreationOfTheseStatistics()")
							.addItem(NEW_FEATURE, "Add function RealTimeTransferStat.getDurationInMsFromCreationTimeOfTheseStatistics()")
							.addItem(BUG_FIX, "Fix issue with calculation of written data during network transfer")
							.addItem(BUG_FIX, "Base delay calculation on System.nanoTime() function and not on System.currentTimeMillis() for better results if UTC time change during the process running.")
							.addItem(NEW_FEATURE, "Add possibility to get distant system information in order to plan direct connection from other peer. Add function Connection.getConnectionInfoSystemMessage().")
							.addItem(NEW_FEATURE, "Add possibility to suspend big data transfers that concern one kernel address or all kernel address. Other transfer like messages, or decentralized message transfers through intermediate peer are allowed. See functions AbstractAgent.setBigDataTransfersOfAGivenKernelPaused(KernelAddress, boolean) or AbstractAgent.setAllBigDataTransfersPaused(boolean).")
							.addItem(NEW_FEATURE, "Add functions AbstractAgent.startNetwork() and AbstractAgent.stopNetwork()")
					)
					.addDescription(new Description((short)2, (short)2, (short)3, Version.Type.STABLE, (short)0, "2021-11-02")
							.addItem(INTERNAL_CHANGE, "Update Utils to 5.21.0 STABLE")
							.addItem(INTERNAL_CHANGE, "Update OOD to 3.1.7 STABLE")
							.addItem(INTERNAL_CHANGE, "Optimizations")
					)
					.addDescription(new Description((short)2, (short)2, (short)2, Version.Type.STABLE, (short)0, "2021-10-18")
							.addItem(INTERNAL_CHANGE, "Update Utils to 5.20.6 STABLE")
							.addItem(INTERNAL_CHANGE, "Update OOD to 3.1.6 STABLE")
							.addItem(INTERNAL_CHANGE, "Clean code")
							.addItem(INTERNAL_CHANGE, "Use CircularArrayList for agent's message box instead of LinkedList")
							.addItem(INTERNAL_CHANGE, "Load agent message box only if necessary")
							.addItem(BUG_FIX, "Fix bad encryption when using secret counter to avoid replay. The bug was not producing security consequences.")
							.addItem(INTERNAL_CHANGE, "Make some optimizations with messages filtering")
					)
					.addDescription(new Description((short)2, (short)2, (short)1, Version.Type.STABLE, (short)0, "2021-10-13")
							.addItem(INTERNAL_CHANGE, "Update Utils to 5.20.3 STABLE")
							.addItem(INTERNAL_CHANGE, "Update OOD to 3.1.2 STABLE")
							.addItem(INTERNAL_CHANGE, "Better log formatting")
							.addItem(BUG_FIX, "Fix possible infinite loop between peer and server, when an error occurs")
							.addItem(BUG_FIX, "Do not use group manager role when sending message without specifying role")
							.addItem(BUG_FIX, "Peers can now synchronize their database through several database backup servers")
							.addItem(BUG_FIX, "Pool executor : fix bad use of maximum number of threads, and permit to create more threads when the maximum of threads was not reached and when tasks are waiting to be executed.")
					)
					.addDescription(new Description((short)2, (short)2, (short)0, Version.Type.STABLE, (short)0, "2021-10-01")
							.addItem(INTERNAL_CHANGE, "Update Utils to 5.19.7 STABLE")
							.addItem(INTERNAL_CHANGE, "Update OOD to 3.1.1 STABLE")
							.addItem(INTERNAL_CHANGE, "Disable useless server stream into Upnp IGD")
							.addItem(BUG_FIX, "UPNP IGD test pass")
							.addItem(INTERNAL_CHANGE, "Complete filter of network interfaces")
							.addItem(BUG_FIX, "Fix issue when determining if a local ip is compatible with another ip")
							.addItem(SECURITY_FIX, "Fix XXE security issue with Cling dependency : https://github.com/4thline/cling/issues/243")
							.addItem(SECURITY_FIX, "Fix DDOS and SSRF security issues with Cling dependency : https://github.com/4thline/cling/issues/253")
							.addItem(BUG_FIX, "Fix issue with AgentExecutor when an agent is launched and terminated too quickly")
							.addItem(NEW_FEATURE, "Add the possibility for a scheduled task to be executed even if the agent who launched the task was killed")
							.addItem(BUG_FIX, "Finish closing connexions even if NIO agent was killed")
							.addItem(BUG_FIX, "Fix issue with agent fake thread and avoid reception of two messages (or more) at the same time")
					)
					.addDescription(new Description((short)2, (short)2, (short)0, Version.Type.BETA, (short)1, "2021-07-07")
							.addItem(INTERNAL_CHANGE, "Update Utils to 5.18.5 STABLE")
							.addItem(INTERNAL_CHANGE, "Update OOD to 3.1.0 Beta 2")
							.addItem(BUG_FIX, "Make Utils and OOD compatible with Android")
							.addItem(INTERNAL_CHANGE, "Reimplement connection protocol using new Utils classes")
							.addItem(NEW_FEATURE, "Permit MaDKit to be a central database backup server in order to synchronize database of distant peers")
							.addItem(INTERNAL_CHANGE, "Check distant and not only local ports with filters of connection protocols and with filter of access protocols")
							.addItem(BUG_FIX, "Fix unexpected high CPU usage due to a bad socket channel using")
					)
					.addDescription(new Description((short)2, (short)1, (short)10, Version.Type.STABLE, (short)1, "2020-02-15")
							.addItem(INTERNAL_CHANGE, "Update Utils to 4.10.2")
							.addItem(INTERNAL_CHANGE, "Update OOD to 2.4.2")
					);

			Calendar c = Calendar.getInstance();
			c.set(2020, Calendar.FEBRUARY, 11);
			Description d = new Description((short)2, (short)1, (short)8, Version.Type.STABLE, (short)1, c.getTime());
			d.addItem(INTERNAL_CHANGE, "Update Utils to 4.9.0");
			d.addItem(INTERNAL_CHANGE, "Update OOD to 2.4.0");
			VERSION.addDescription(d);

			c = Calendar.getInstance();
			c.set(2020, Calendar.JANUARY, 25);
			d = new Description((short)2, (short)1, (short)7, Version.Type.STABLE, (short)1, c.getTime());
			d.addItem(INTERNAL_CHANGE, "Generate random messages only if messages are lower than network block size");
			VERSION.addDescription(d);

			c = Calendar.getInstance();
			c.set(2020, Calendar.JANUARY, 24);
			d = new Description((short)2, (short)1, (short)6, Version.Type.STABLE, (short)1, c.getTime());
			d.addItem(INTERNAL_CHANGE,"Update Utils to 4.8.5");
			d.addItem(INTERNAL_CHANGE,"Update OOD to 2.3.20");
			d.addItem(INTERNAL_CHANGE,"Rewrite agent thread executors and thread pool executors.");
			d.addItem(BUG_FIX, "Fiw issue when purging messages of a killed agent.");
			VERSION.addDescription(d);

			c = Calendar.getInstance();
			c.set(2020, Calendar.JANUARY, 10);
			d = new Description((short)2, (short)1, (short)5, Version.Type.STABLE, (short)1, c.getTime());
			d.addItem(INTERNAL_CHANGE,"Update OOD to 2.3.14 STABLE");
			VERSION.addDescription(d);

			c = Calendar.getInstance();
			c.set(2019, Calendar.DECEMBER, 17);
			d = new Description((short)2, (short)1, (short)4, Version.Type.STABLE, (short)1, c.getTime());
			d.addItem(INTERNAL_CHANGE,"Update Utils to 4.7.1 STABLE");
			d.addItem(INTERNAL_CHANGE, "Update OOD to 2.3.13 STABLE");
			VERSION.addDescription(d);

			c = Calendar.getInstance();
			c.set(2019, Calendar.NOVEMBER, 22);
			d = new Description((short)2, (short)1, (short)3, Version.Type.STABLE, (short)1, c.getTime());
			d.addItem(INTERNAL_CHANGE, "Update Utils to 4.7.0 STABLE");
			d.addItem(INTERNAL_CHANGE, "Update OOD to 2.3.11 STABLE");
			d.addItem(INTERNAL_CHANGE, "Use LoginData.invalidCloudPassword function");
			VERSION.addDescription(d);

			c = Calendar.getInstance();
			c.set(2019, Calendar.NOVEMBER, 19);
			d = new Description((short)2, (short)1, (short)1, Version.Type.STABLE, (short)1, c.getTime());
			d.addItem(INTERNAL_CHANGE, "Rewrite PairOfIdentifiers class");
			VERSION.addDescription(d);

			c = Calendar.getInstance();
			c.set(2019, Calendar.NOVEMBER, 18);
			d = new Description((short)2, (short)1, (short)0, Version.Type.STABLE, (short)1, c.getTime());
			d.addItem(INTERNAL_CHANGE, "Update Utils to 4.6.5 STABLE");
			d.addItem(INTERNAL_CHANGE, "Update OOD to 2.3.10 STABLE");
			d.addItem(INTERNAL_CHANGE, "Update JDKRewriteUtils to 1.0.4 STABLE");
			d.addItem(INTERNAL_CHANGE, "Filter distant network roles, and not only distant network groups");
			d.addItem(INTERNAL_CHANGE, "Compile with openjdk 13 (compatibility set to Java 7");
			d.addItem(BUG_FIX, "Fix network broadcast message issue when no local agent is present, and when network agents does not received broadcast message");
			VERSION.addDescription(d);

			c = Calendar.getInstance();
			c.set(2019, Calendar.OCTOBER, 31);
			d = new Description((short)2, (short)0, (short)4, Version.Type.STABLE, (short)1, c.getTime());
			d.addItem(INTERNAL_CHANGE, "Update Utils to 4.6.2 STABLE");
			d.addItem(INTERNAL_CHANGE, "Update OOD to 2.3.7 STABLE");
			d.addItem(INTERNAL_CHANGE, "Update documentation");
			VERSION.addDescription(d);

			c = Calendar.getInstance();
			c.set(2019, Calendar.OCTOBER, 19);
			d = new Description((short)2, (short)0, (short)3, Version.Type.STABLE, (short)1, c.getTime());
			d.addItem(INTERNAL_CHANGE, "Update Utils to 4.6.1 STABLE");
			d.addItem(INTERNAL_CHANGE, "Update OOD to 2.3.6 STABLE");
			d.addItem(INTERNAL_CHANGE, "Update dependencies");
			VERSION.addDescription(d);

			c = Calendar.getInstance();
			c.set(2019, Calendar.OCTOBER, 18);
			d = new Description((short)2, (short)0, (short)0, Version.Type.STABLE, (short)1, c.getTime());
			d.addItem(INTERNAL_CHANGE, "Update Utils to 4.6.0 STABLE");
			d.addItem(INTERNAL_CHANGE, "Update OOD to 2.3.4 STABLE");
			d.addItem(NEW_FEATURE, "Cloud identifiers can be individually anonymous thanks to an encryption process.");
			d.addItem(INTERNAL_CHANGE, "Host identifiers are sent only if the cloud identifier authentication process succeeded.");
			d.addItem(NEW_FEATURE, "Authentication can be done automatically with public key, through a shared password/key, or both");
			d.addItem(NEW_FEATURE, "P2PSecuredConnectionProtocolWithKeyAgreement algorithm permit to make client/server authentication throw asymmetric signatures");
			d.addItem(NEW_FEATURE, "An identifier is composed of a cloud identifier, and a host identifier. In the past, one authentication concerned both cloud and host identifiers. Now it is possible to have two authentications : one for the cloud identifier, and one another for the host identifier. If one of them fails, than identifier is rejected.");
			d.addItem(NEW_FEATURE, "Use hybrid connexion protocols that enables to use at the same time non post quantum algorithms and post quantum algorithms. It is to prevent the quantum supremacy without loosing the benefits of stable encryption algorithms. For client/server connexion, two asymmetric key pairs are then used : one for a non post quantum algorithm like RSA and one for a post quantum algorithm like Mc Eliece");
			d.addItem(NEW_FEATURE, "Synchronize local database with other peers");
			VERSION.addDescription(d);

			c = Calendar.getInstance();
			c.set(2019, Calendar.APRIL, 24);
			d = new Description((short)1, (short)11, (short)1, Version.Type.STABLE, (short)1, c.getTime());
			d.addItem(INTERNAL_CHANGE, "Minimal modification into SecuredObjectOutputStream and SecuredObjectInputStream classes. ");
			d.addItem(INTERNAL_CHANGE, "Fix bad use of garbage collector with ConversationID. ");
			VERSION.addDescription(d);

			c = Calendar.getInstance();
			c.set(2019, Calendar.APRIL, 23);
			d = new Description((short)1, (short)11, (short)0, Version.Type.STABLE, (short)1, c.getTime());
			d.addItem(SECURITY_FIX, "Add SecuredObjectOutputStream and SecuredObjectInputStream classes. Do not use native ObjectInputStream.");
			d.addItem(NEW_FEATURE, "Add possibility to send asynchronous messages (AbstractAgent.sendMessageWithRoleOrDifferSendingUntilRecipientWasFound).");
			VERSION.addDescription(d);

			c = Calendar.getInstance();
			c.set(2019, Calendar.APRIL, 8);
			d = new Description((short)1, (short)10, (short)2, Version.Type.STABLE, (short)1, c.getTime());
			d.addItem(INTERNAL_CHANGE, "Add hashCode function into class AbstractIP.");
			d.addItem(INTERNAL_CHANGE, "Better manage externalization.");
			d.addItem(INTERNAL_CHANGE, "Better manage identifiers of connection protocol negotiator.");
			d.addItem(NEW_FEATURE, "Add possibility to change connection protocols during MaDKit life, and not only during MaDKit loading.");
			d.addItem(NEW_FEATURE, "Add possibility to change access protocols during MaDKit life, and not only during MaDKit loading.");
			d.addItem(NEW_FEATURE, "Add possibility to change access data during MaDKit life, and not only during MaDKit loading.");
			d.addItem(NEW_FEATURE, "Add possibility to trigger connection/disconnection only if these were not present into the list of connection to attempt at the MaDKit starting defined into NetworkProperties class. " +
					"Concerned functions are AbstractAgent.manageDirectConnection, AbstractAgent.manageDirectConnections, AbstractAgent.manageDirectConnectionAndAddItToNetworkProperties and AbstractAgent.manageDirectConnectionsAndAddThemToNetworkProperties.");
			VERSION.addDescription(d);

			c = Calendar.getInstance();
			c.set(2019, Calendar.MARCH, 26);
			d = new Description((short)1, (short)10, (short)1, Version.Type.STABLE, (short)1, c.getTime());
			d.addItem(INTERNAL_CHANGE, "Do not do useless thread locking when CGR groups are synchronized with distant peers.");
			d.addItem(BUG_FIX, "Restore BigDataTransferID to previous previous (new version caused issues with Junit tests).");
			d.addItem(BUG_FIX, "Fix issue during MaDKit ending.");
			VERSION.addDescription(d);

			c = Calendar.getInstance();
			c.set(2019, Calendar.MARCH, 25);
			d = new Description((short)1, (short)10, (short)0, Version.Type.STABLE, (short)1, c.getTime());
			d.addItem(INTERNAL_CHANGE, "Update OOD to 2.0.0 BETA 104.");
			d.addItem(INTERNAL_CHANGE, "Update Utils to 3.25.5 STABLE.");
			d.addItem(NEW_FEATURE, "Add new connection protocol with symmetric keys: P2PSecuredConnectionProtocolWithKnownSymmetricKeys.");
			d.addItem(BUG_FIX, "Better tests for network transfers.");
			d.addItem(INTERNAL_CHANGE, "Improve synchronization of list of distant agents.");
			d.addItem(SECURITY_FIX, "control that received distant agents list synchronization concerns the good distant kernel address.");
			d.addItem(BUG_FIX, "Insure that list of distant agents are removed when peers are disconnected.");
			d.addItem(NEW_FEATURE, "When a connection fail, try another ip.");
			d.addItem(BUG_FIX, "Change information sending order when a new connection was established (fix a problem with synchronization of CGR that is done after network messages are done).");
			d.addItem(BUG_FIX, "Fix issue with replies not sent with killed agents : killed agent send now empty reply for messages that need reply.");
			VERSION.addDescription(d);


			c = Calendar.getInstance();
			c.set(2019, Calendar.MARCH, 1);
			d = new Description((short)1, (short)9, (short)6, Version.Type.STABLE, (short)1, c.getTime());
			d.addItem(INTERNAL_CHANGE, "Remove obsolete code.");
			d.addItem(INTERNAL_CHANGE, "Rename function into HostIdentifier class.");
			d.addItem(INTERNAL_CHANGE, "Typo corrections.");
			d.addItem(NEW_FEATURE, "Add possibility to specify the profile identifier into class ServerSecuredProtocolPropertiesWithKnownPublicKey.");
			VERSION.addDescription(d);

			c = Calendar.getInstance();
			c.set(2019, Calendar.FEBRUARY, 8);
			d = new Description((short)1, (short)9, (short)4, Version.Type.STABLE, (short)1, c.getTime());
			d.addItem(INTERNAL_CHANGE, "Code cleaning/optimizing.");
			VERSION.addDescription(d);



			c = Calendar.getInstance();
			c.set(2019, Calendar.FEBRUARY, 6);
			d = new Description((short)1, (short)9, (short)5, Version.Type.STABLE, (short)1, c.getTime());
			d.addItem(INTERNAL_CHANGE, "Update OOD to 2.0.0 BETA 99.");
			d.addItem(INTERNAL_CHANGE, "Update Utils to 3.25.1");
			VERSION.addDescription(d);

			c = Calendar.getInstance();
			c.set(2019, Calendar.FEBRUARY, 5);
			d = new Description((short)1, (short)9, (short)2, Version.Type.STABLE, (short)1, c.getTime());
			d.addItem(INTERNAL_CHANGE, "Update OOD to 2.0.0 BETA 97.");
			d.addItem(SECURITY_FIX, "OOD - Security fix : disable cache for tables that use secret ou private keys");
			d.addItem(SECURITY_FIX, "OOD - Security improvement : add Field.disableCache property");
			VERSION.addDescription(d);


			c = Calendar.getInstance();
			c.set(2019, Calendar.JANUARY, 18);
			d = new Description((short)1, (short)9, (short)1, Version.Type.STABLE, (short)1, c.getTime());
			d.addItem(INTERNAL_CHANGE, "Update OOD to 2.0.0 BETA 95.");
			d.addItem(INTERNAL_CHANGE, "Set default OOD driver to H2 database.");
			VERSION.addDescription(d);

			c = Calendar.getInstance();
			c.set(2019, Calendar.JANUARY, 13);
			d = new Description((short)1, (short)9, (short)0, Version.Type.STABLE, (short)1, c.getTime());
			d.addItem(INTERNAL_CHANGE, "Update OOD to 2.0.0 BETA 94.");
			d.addItem(INTERNAL_CHANGE, "Update Utils to 3.24.0.");
			d.addItem(NEW_FEATURE, "Manage asymmetric auto-signed login.");
			d.addItem(INTERNAL_CHANGE, "Better computer manage sleep mode.");
			d.addItem(INTERNAL_CHANGE, "Manage moment of connection, and connection retry when failed");
			VERSION.addDescription(d);

			c = Calendar.getInstance();
			c.set(2018, Calendar.NOVEMBER, 24);
			d = new Description((short)1, (short)8, (short)2, Version.Type.STABLE, (short)1, c.getTime());
			d.addItem(SECURITY_FIX, "Improve security of Client/Server connection protocol.");
			d.addItem(BUG_FIX, "Resolve a concurrent modification exception into internal role.");
			VERSION.addDescription(d);

			c = Calendar.getInstance();
			c.set(2018, Calendar.NOVEMBER, 23);
			d = new Description((short)1, (short)8, (short)1, Version.Type.STABLE, (short)1, c.getTime());
			d.addItem(SECURITY_FIX, "Improve security of Client/Server connection protocol.");
			VERSION.addDescription(d);

			c = Calendar.getInstance();
			c.set(2018, Calendar.NOVEMBER, 21);
			d = new Description((short)1, (short)8, (short)0, Version.Type.STABLE, (short)1, c.getTime());
			d.addItem(NEW_FEATURE, "Detect security anomalies during big data transfers.");
            d.addItem(BUG_FIX, "Correction of Group.equals() with null references.");
            d.addItem(SECURITY_FIX, "Better manage ban with deserialization process.");
			d.addItem(INTERNAL_CHANGE, "Update Utils to 3.22.1.");
			d.addItem(INTERNAL_CHANGE, "Update OOD to 2.0.0 BETA 91.");
			d.addItem(NEW_FEATURE, "Add function AbstractAgent.getAccessibleKernelsFilteredByGroupsGivenByDistantPeer(Group).");
			d.addItem(NEW_FEATURE, "Add function AbstractAgent.getAccessibleKernelsFilteredByGroupsGivenToDistantPeer(Group).");
			d.addItem(BUG_FIX, "Check public key validity with client/server connexion protocol.");
            d.addItem(NEW_FEATURE, "Add ConnectionProtocolNegotiator and ConnectionProtocolNegotiatorProperties classes.");
            d.addItem(BUG_FIX, "Fix issue with client/server vs peer-to-peer protocol negotiation.");
			d.addItem(BUG_FIX, "Optimizing a problem of simultaneous network data send to several peers : data was sent peer after peer, and not to all peers at the same time.");
			d.addItem(NEW_FEATURE, "Add possibility to limit global download bandwidth and global upload bandwidth.");
			d.addItem(NEW_FEATURE, "Detect OS wake up after a sleep mode.");
			d.addItem(INTERNAL_CHANGE, "Update Cling to 2.1.2.");
			d.addItem(BUG_FIX, "Fix issue with router updating.");
			VERSION.addDescription(d);

			c = Calendar.getInstance();
			c.set(2018, Calendar.AUGUST, 1);
			d = new Description((short)1, (short)7, (short)6, Version.Type.STABLE, (short)1, c.getTime());
			d.addItem(INTERNAL_CHANGE, "Update OOD to 2.0.0 BETA 86.");
			d.addItem(INTERNAL_CHANGE, "Update Utils to 3.19.0.");
			d.addItem(NEW_FEATURE, "Add save functions into MadKit Properties.");
			d.addItem(BUG_FIX, "Fix network messages serialization problem.");
			VERSION.addDescription(d);

			c = Calendar.getInstance();
			c.set(2018, Calendar.JULY, 27);
			d = new Description((short)1, (short)7, (short)5, Version.Type.STABLE, (short)1, c.getTime());
			d.addItem(INTERNAL_CHANGE, "Update OOD to 2.0.0 BETA 85.");
			d.addItem(INTERNAL_CHANGE, "Update Utils to 3.18.0.");
			d.addItem(INTERNAL_CHANGE, "Save MKLE configuration that are different from a reference configuration. Other properties are not saved.");
			VERSION.addDescription(d);

			c = Calendar.getInstance();
			c.set(2018, Calendar.JULY, 20);
			d = new Description((short)1, (short)7, (short)3, Version.Type.STABLE, (short)1, c.getTime());
			d.addItem(INTERNAL_CHANGE, "Update OOD to 2.0.0 BETA 84.");
			d.addItem(INTERNAL_CHANGE, "Update Utils to 3.17.0.");
			d.addItem(BUG_FIX, "Fix version's control issue of distant peer.");
			VERSION.addDescription(d);

			c = Calendar.getInstance();
			c.set(2018, Calendar.JULY, 13);
			d = new Description((short)1, (short)7, (short)1, Version.Type.STABLE, (short)1, c.getTime());
			d.addItem(INTERNAL_CHANGE, "Update OOD to 2.0.0 BETA 83.");
            d.addItem(INTERNAL_CHANGE, "Update Utils to 3.16.1.");
            d.addItem(INTERNAL_CHANGE, "Improve version's control of distant peer.");
            d.addItem(INTERNAL_CHANGE, "Clean code.");
			VERSION.addDescription(d);


			c = Calendar.getInstance();
			c.set(2018, Calendar.MAY, 20);
			d = new Description((short)1, (short)7, (short)0, Version.Type.STABLE, (short)1, c.getTime());
			d.addItem(INTERNAL_CHANGE, "Update OOD to 2.0.0 BETA 82.");
			d.addItem(INTERNAL_CHANGE, "Update Utils to 3.15.0.");
			d.addItem(NEW_FEATURE, "Add P2P connection protocol that support parametrisation of key agreement.");
			d.addItem(NEW_FEATURE, "Support several key agreement (including Post Quantum Cryptography key agreement (New Hope)).");
			d.addItem(SECURITY_FIX, "when data is sent without being wrote (default memory state), fill it with zeros.");
			d.addItem(SECURITY_FIX, "sign symmetric encryption key into client/server connection protocol.");
			d.addItem(SECURITY_FIX, "with P2P key agreements, generate signature and encryption keys with two steps (instead of one), in order to sign the exchanged symmetric encryption key.");
			d.addItem(SECURITY_FIX, "class serialization are now filtered with allow list and deny list. Classes that are not into deny list must implement the interference 'SerializableAndSizable'. Messages sent to the network must implement the interface NetworkMessage.");
			d.addItem(INTERNAL_CHANGE, "Optimization : use externalization process instead of deserialization process during lan transfer.");
			d.addItem(SECURITY_FIX, "classes externalization processes control now the allocated memory during de-externalization phase.");
			d.addItem(SECURITY_FIX, "Security enhancement : initialisation vectors used with encryption has now a secret part composed of counter that is increased at each data exchange.");
			d.addItem(SECURITY_FIX, "Security enhancement : signature and encryption process use now a secret message that is increased at each data exchange.");
			d.addItem(SECURITY_FIX, "Security enhancement : P2P login agreement use now JPake and a signature authentication if secret key for signature is available (PasswordKey.getSecretKeyForSignature()).");
			d.addItem(BUG_FIX, "Fix issue with dead lock into indirect connection process.");
			d.addItem(BUG_FIX, "Fix issue with dual connection between two same kernels.");
			d.addItem(INTERNAL_CHANGE, "Externalising Java rewrote classes into JDKRewriteUtils project.");
			d.addItem(NEW_FEATURE, "Support of authenticated encryption algorithms. When use these algorithms, MKLE do not add a signature with independent MAC.");
			d.addItem(INTERNAL_CHANGE, "Add some benchmarks.");
			d.addItem(NEW_FEATURE, "Support of YAML file properties.");
			VERSION.addDescription(d);	

			
			c = Calendar.getInstance();
			c.set(2018, Calendar.FEBRUARY, 27);
			d = new Description((short)1, (short)6, (short)5, Version.Type.STABLE, (short)1, c.getTime());
			d.addItem(BUG_FIX, "Debug UPNP connexion with macOS.");
			d.addItem(BUG_FIX, "Fix issue with multiple identical router's messages : do not remove the router to recreate it.");
			VERSION.addDescription(d);	
			
			c = Calendar.getInstance();
			c.set(2018, Calendar.FEBRUARY, 26);
			d = new Description((short)1, (short)6, (short)5, Version.Type.STABLE, (short)1, c.getTime());
			d.addItem(BUG_FIX, "Fiw a problem with UPNP connexion under macOS.");
			VERSION.addDescription(d);	
			
			c = Calendar.getInstance();
			c.set(2018, Calendar.FEBRUARY, 15);
			d = new Description((short)1, (short)6, (short)4, Version.Type.STABLE, (short)1, c.getTime());
			d.addItem(BUG_FIX, "Fix problem of port unbind with Windows.");
			d.addItem(BUG_FIX, "Fix problem of simultaneous connections with Mac OS");
			d.addItem(BUG_FIX, "Fix problem with interface address filtering");
			VERSION.addDescription(d);	
			
			c = Calendar.getInstance();
			c.set(2018, Calendar.FEBRUARY, 10);
			d = new Description((short)1, (short)6, (short)3, Version.Type.STABLE, (short)1, c.getTime());
			d.addItem(INTERNAL_CHANGE, "Update OOD to 2.0.0 BETA 66.");
			d.addItem(INTERNAL_CHANGE, "Update Utils to 3.10.5");
			d.addItem(INTERNAL_CHANGE, "Change minimum public key size from 1024 to 2048");
			VERSION.addDescription(d);			
			
			c = Calendar.getInstance();
			c.set(2018, Calendar.FEBRUARY, 10);
			d = new Description((short)1, (short)6, (short)2, Version.Type.STABLE, (short)1, c.getTime());
			d.addItem(INTERNAL_CHANGE, "Update OOD to 2.0.0 BETA 65.");
			d.addItem(INTERNAL_CHANGE, "Update Utils to 3.10.4");
			d.addItem(INTERNAL_CHANGE, "Change minimum public key size from 1024 to 2048");
			VERSION.addDescription(d);
			
			c = Calendar.getInstance();
			c.set(2018, Calendar.FEBRUARY, 4);
			d = new Description((short)1, (short)6, (short)1, Version.Type.STABLE, (short)1, c.getTime());
			d.addItem(BUG_FIX, "Overlookers were not aware from new roles adding. Fix this issue.");
			d.addItem(NEW_FEATURE, "Add MadKit demos");
			VERSION.addDescription(d);
			
			c = Calendar.getInstance();
			c.set(2018, Calendar.JANUARY, 31);
			d = new Description((short)1, (short)6, (short)0, Version.Type.STABLE, (short)1, c.getTime());
			d.addItem(INTERNAL_CHANGE, "Updating OOD to 2.0.0 BETA 59");
			d.addItem(INTERNAL_CHANGE, "Updating Utils to 3.9.0");
			d.addItem(NEW_FEATURE, "Messages can now be atomically non encrypted");
			VERSION.addDescription(d);
			
			c = Calendar.getInstance();
			c.set(2017, Calendar.DECEMBER, 13);
			d = new Description((short)1, (short)5, (short)2, Version.Type.STABLE, (short)1, c.getTime());
			d.addItem(INTERNAL_CHANGE, "Updating OOD to 2.0.0 BETA 57");
			d.addItem(INTERNAL_CHANGE, "Updating Utils to 3.7.1");
			d.addItem(INTERNAL_CHANGE, "Debugging JavaDoc");
			VERSION.addDescription(d);

			c = Calendar.getInstance();
			c.set(2017, Calendar.NOVEMBER, 13);
			d = new Description((short)1, (short)5, (short)0, Version.Type.STABLE, (short)1, c.getTime());
			d.addItem(INTERNAL_CHANGE, "Updating OOD to 2.0.0 BETA 55");
			d.addItem(INTERNAL_CHANGE, "Packets can now have sizes greater than Short.MAX_VALUE");
			VERSION.addDescription(d);

			c = Calendar.getInstance();
			c.set(2017, Calendar.NOVEMBER, 2);
			d = new Description((short)1, (short)4, (short)5, Version.Type.STABLE, (short)1, c.getTime());
			d.addItem(INTERNAL_CHANGE, "Updating OOD to 2.0.0 BETA 54");
			VERSION.addDescription(d);

			c = Calendar.getInstance();
			c.set(2017, Calendar.OCTOBER, 13);
			d = new Description((short)1, (short)4, (short)0, Version.Type.STABLE, (short)1, c.getTime());
			d.addItem(INTERNAL_CHANGE, "Updating OOD to 2.0.0 BETA 48");
			d.addItem(INTERNAL_CHANGE, "Several modifications into connection and access protocols");
			d.addItem(NEW_FEATURE, "Adding approved randoms parameters into MaDKitProperties");
			d.addItem(NEW_FEATURE, "Adding point to point transfer connection signature and verification");
			d.addItem(NEW_FEATURE, "Saving automatically random seed to be reload with the next application loading");
			VERSION.addDescription(d);

			c = Calendar.getInstance();
			c.set(2017, Calendar.AUGUST, 31);
			d = new Description((short)1, (short)2, (short)1, Version.Type.STABLE, (short)1, c.getTime());
			d.addItem(INTERNAL_CHANGE, "Including resources in jar files");
			VERSION.addDescription(d);

			c = Calendar.getInstance();
			c.set(2017, Calendar.AUGUST, 5);
			d = new Description((short)1, (short)2, (short)0, Version.Type.STABLE, (short)1, c.getTime());
			d.addItem(INTERNAL_CHANGE, "Correction a problem with database");
			d.addItem(NEW_FEATURE, "Adding P2PSecuredConnectionProtocolWithECDHAlgorithm connection protocol (speediest)");
			d.addItem(NEW_FEATURE, "Adding Client/ServerSecuredConnectionProtocolWithKnownPublicKeyWithECDHAlgorithm connection protocol (speediest)");
			d.addItem(SECURITY_FIX, "Now all connection protocols use different keys for encryption and for signature");
			d.addItem(NEW_FEATURE, "Adding AccessProtocolWithP2PAgreement (speediest)");
			d.addItem(BUG_FIX, "Debugging desktop JFrame closing (however the JMV still become opened when all windows are closed)");
			d.addItem(BUG_FIX, "Several minimal bug fix");
			d.addItem(INTERNAL_CHANGE, "Correction of JavaDoc");
			d.addItem(INTERNAL_CHANGE, "Updating OOD to 2.0.0 BETA 20 version");
			VERSION.addDescription(d);

			c = Calendar.getInstance();
			c.set(2017, Calendar.AUGUST, 5);
			d = new Description((short)1, (short)1, (short)3, Version.Type.STABLE, (short)1, c.getTime());
			d.addItem(INTERNAL_CHANGE,"Updating OOD to 2.0.0 BETA 15");
			VERSION.addDescription(d);

			c = Calendar.getInstance();
			c.set(2017, Calendar.AUGUST, 5);
			d = new Description((short)1, (short)1, (short)2, Version.Type.STABLE, (short)1, c.getTime());
			d.addItem(INTERNAL_CHANGE,"Updating OOD to 2.0.0 BETA 14");
			d.addItem(INTERNAL_CHANGE,"Optimizing some memory leak tests");
			VERSION.addDescription(d);

			c = Calendar.getInstance();
			c.set(2017, Calendar.AUGUST, 4);
			d = new Description((short)1, (short)1, (short)0, Version.Type.STABLE, (short)1, c.getTime());
			d.addItem(INTERNAL_CHANGE,"Convert project to Gradle project");
			VERSION.addDescription(d);

			c = Calendar.getInstance();
			c.set(2017, Calendar.JUNE, 4);
			d = new Description((short)1, (short)0, (short)0, Version.Type.STABLE, (short)1, c.getTime());
			d.addItem(BUG_FIX, "Correction of a bug with database disconnection");
			d.addItem(BUG_FIX, "Debugging indirect connections");
			d.addItem(BUG_FIX, "Solving a memory leak problem with ConversationID");
			d.addItem(BUG_FIX, "Solving a memory leak problem with TransferAgent (not killed)");
			d.addItem(BUG_FIX, "Solving problem when deny BigDataProposition and kill agent just after");
			d.addItem(NEW_FEATURE, "Indirect connection send now ping message");
			d.addItem(NEW_FEATURE, "Adding allow list for InetAddresses in network properties");
			d.addItem(BUG_FIX, "Correcting problems of internal group/role references/dereferences");
			VERSION.addDescription(d);

			c = Calendar.getInstance();
			c.set(2017, Calendar.MAY, 27);
			d = new Description((short)1, (short)0, (short)0, Version.Type.BETA, (short)4, c.getTime());
			d.addItem(INTERNAL_CHANGE, "Agents are now identified by a long (and not int)");
			d.addItem(INTERNAL_CHANGE,"Adding the function AbstractAgent.getAgentID()");
			d.addItem(INTERNAL_CHANGE,"Removing static elements in Conversation ID");
			VERSION.addDescription(d);

			c = Calendar.getInstance();
			c.set(2017, Calendar.MAY, 23);
			d = new Description((short)1, (short)0, (short)0, Version.Type.BETA, (short)3, c.getTime());
			d.addItem(INTERNAL_CHANGE,"Update Utils to 2.7.1");
			d.addItem(INTERNAL_CHANGE,"Update OOD to 2.0.0 BETA 1");
			d.addItem(INTERNAL_CHANGE,"Minimum java version is now Java 7");
			VERSION.addDescription(d);

			c = Calendar.getInstance();
			c.set(2017, Calendar.MARCH, 7);
			d = new Description((short)1, (short)0, (short)0, Version.Type.BETA, (short)2, c.getTime());
			d.addItem(SECURITY_FIX, "Reinforce secret identifier/password exchange");
			d.addItem(NEW_FEATURE, "Add agent to launch into MKDesktop windows");
			VERSION.addDescription(d);

			c = Calendar.getInstance();
			c.set(2017, Calendar.MARCH, 4);
			d = new Description((short)1, (short)0, (short)0, Version.Type.BETA, (short)0, c.getTime());
			d.addItem(NEW_FEATURE, "First MaDKitLanEdition release, based on MaDKit");
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

					// just in case (like ctrl+c)
					Runtime.getRuntime().addShutdownHook(new Thread(AgentLogger::resetLoggers));

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
	final private MadkitKernel myKernel;
	private Logger logger;
	// TODO Remove unused code found by UCDetector
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
	public static void main(String[] options) throws IOException {
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
	 *
	 * 
	 * 
	 * @param options
	 *            the options which should be used to launch Madkit. If
	 *            <code>null</code>, the desktop mode is automatically used.
	 * 
	 * @see MadkitProperties
	 * @see NetworkProperties
	 * 
	 */
	public Madkit(String... options) throws IOException {
		this(_properties -> {

		}, options);
	}

	/**
	 * Launch a new kernel with predefined options. The call returns when the new
	 * kernel has finished to take care of all options. Moreover the kernel
	 * automatically ends when all the agents living on this kernel are done.
	 * <p>
	 * 
	 * Here is an example of use:
	 *
	 * 
	 * 
	 * @param eventListener
	 *            the event listener called when events occurs during Madkit life
	 *            cycle
	 * @param options
	 *            the options which should be used to launch Madkit. If
	 *            <code>null</code>, the desktop mode is automatically used.
	 * 
	 * @see MadkitProperties
	 * @see NetworkProperties
	 */
	public Madkit(MadkitEventListener eventListener, String... options) throws IOException {
		this(generateDefaultMadkitConfig(), null, eventListener, options);
	}
    /**
     * Launch a new kernel with predefined options. The call returns when the new
     * kernel has finished to take care of all options. Moreover the kernel
     * automatically ends when all the agents living on this kernel are done.
     * <p>
     *
     * Here is an example of use:
     *
     *
     * @param madkitConfig the initial MadKit configuration
     * @param eventListener
     *            the event listener called when events occurs during Madkit life
     *            cycle
     * @param options
     *            the options which should be used to launch Madkit. If
     *            <code>null</code>, the desktop mode is automatically used.
     *
     * @see MadkitProperties
     * @see NetworkProperties
     */
    public Madkit(MadkitProperties madkitConfig, MadkitEventListener eventListener, String... options) throws IOException {
        this(madkitConfig, null, eventListener, options);
    }
    /**
     * Launch a new kernel with predefined options. The call returns when the new
     * kernel has finished to take care of all options. Moreover the kernel
     * automatically ends when all the agents living on this kernel are done.
     * <p>
     *
     * Here is an example of use:
     *
     *
     *
     * @param madkitConfig
     *            the initial MadKit configuration
     * @param options
     *            the options which should be used to launch Madkit. If
     *            <code>null</code>, the desktop mode is automatically used.
     *
     * @see MadkitProperties
     * @see NetworkProperties
     */
    public Madkit(MadkitProperties madkitConfig, String... options) throws IOException {
        this(madkitConfig, null, _properties -> {

		}, options);
    }
	Madkit(MadkitProperties madkitProperties, KernelAddress kernelAddress, MadkitEventListener eventListener, String... options) throws IOException {
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

		this.madkitConfig=madkitProperties.clone();
		if (this.madkitConfig.getReference()==null)
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

		eventListener.onMaDKitPropertiesLoaded(madkitConfig);

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

	private boolean loadConfigFiles() throws IOException {
		boolean ok = false;
		ArrayList<File> configFiles = madkitConfig.configFiles;
		if (configFiles != null) {
			for (File f : configFiles) {
				madkitConfig.load(f);
				ok = true;
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
		return myKernel.toString() + myKernel.getKernelAddress();
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
					+ "\n\t\t\t\t\t\t\t    MadkitLanEdition\n" + "\n\t Version: " + VERSION.getMajor() + "."
					+ VERSION.getMinor() + "." + VERSION.getRevision() + " " + VERSION.getType()
					+ (VERSION.getType().equals(Version.Type.STABLE) ? "" : (" " + VERSION.getAlphaBetaRCVersion()))
					+ "\n\t MadkitLanEdition Team (c) " + startCal.get(Calendar.YEAR) + "-" + endCal.get(Calendar.YEAR)
					+ "\n\t Forked from MaDKit 1997-2016"
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
					(Class<? extends SecureExternalizableWithoutInnerSizeControl>) Class.forName("com.distrimind.madkit.kernel.network.TransferAgent$DirectConnectionSucceeded"),
					(Class<? extends SecureExternalizableWithoutInnerSizeControl>) Class.forName("com.distrimind.madkit.kernel.network.TransferAgent$TryDirectConnection"),
					BigDataTransferID.class,
					(Class<? extends SecureExternalizableWithoutInnerSizeControl>) Class.forName("com.distrimind.madkit.kernel.network.CGRSynchrosSystemMessage"),
					(Class<? extends SecureExternalizableWithoutInnerSizeControl>) Class.forName("com.distrimind.madkit.kernel.network.CGRSynchroSystemMessage"),
					CGRSynchro.class,
					(Class<? extends SecureExternalizableWithoutInnerSizeControl>) Class.forName("com.distrimind.madkit.kernel.network.ConnectionInfoSystemMessage"),
					(Class<? extends SecureExternalizableWithoutInnerSizeControl>) Class.forName("com.distrimind.madkit.kernel.network.DataToBroadcast"),
					DistantKernelAddressValidated.class,
					(Class<? extends SecureExternalizableWithoutInnerSizeControl>) Class.forName("com.distrimind.madkit.kernel.network.connection.access.IdentifiersPropositionMessage"),
					Identifier.class,
					(Class<? extends SecureExternalizableWithoutInnerSizeControl>) Class.forName("com.distrimind.madkit.kernel.network.connection.access.JPakeMessageForAuthenticationOfCloudIdentifiers"),
					(Class<? extends SecureExternalizableWithoutInnerSizeControl>) Class.forName("com.distrimind.madkit.kernel.network.connection.access.JPakeAccessInitialized"),
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
					(Class<? extends SecureExternalizableWithoutInnerSizeControl>) Class.forName("com.distrimind.madkit.kernel.InternalRole"),
					EncryptedPassword.class,
					(Class<? extends SecureExternalizableWithoutInnerSizeControl>) Class.forName("com.distrimind.madkit.kernel.network.DatagramLocalNetworkPresenceMessage"),
					(Class<? extends SecureExternalizableWithoutInnerSizeControl>) Class.forName("com.distrimind.madkit.kernel.network.connection.access.EncryptedCloudIdentifier"),
					(Class<? extends SecureExternalizableWithoutInnerSizeControl>) Class.forName("com.distrimind.madkit.kernel.network.connection.access.WrappedCloudIdentifier"),
					(Class<? extends SecureExternalizableWithoutInnerSizeControl>) Class.forName("com.distrimind.madkit.kernel.network.connection.access.CloudIdentifiersPropositionMessage"),
					(Class<? extends SecureExternalizableWithoutInnerSizeControl>) Class.forName("com.distrimind.madkit.kernel.network.connection.access.JPakeMessageForAuthenticationOfCloudIdentifiers"),
					ListGroupsRoles.class, GroupsRoles.class,
					CancelBigDataTransferMessage.class,
					CancelBigDataSystemMessage.class,
					MadkitNetworkAccess.PauseBigDataTransferSystemMessageClass,
					BigDataToRestartMessage.class));
			for (Class<?> c : classes)
				assert !Modifier.isAbstract(c.getModifiers()):""+c;

			//noinspection unchecked
			ArrayList<Class<? extends Enum<?>>> enums = new ArrayList<>(new HashSet<>(Arrays.asList(
					AbstractAgent.ReturnCode.class,
					AbstractAgent.State.class,
					(Class<? extends Enum<?>>) Class.forName("com.distrimind.madkit.kernel.CGRSynchro$Code"),
					ConnectionProtocol.ConnectionState.class,
					Version.Type.class,
					AgentAction.class,
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
					HookMessage.AgentActionEvent.class,
					EncryptionRestriction.class,
					Identifier.AuthenticationMethod.class
			)));

			SerializationTools.addPredefinedClasses(classes, enums);
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
			System.exit(-1);
		}
	}

}
