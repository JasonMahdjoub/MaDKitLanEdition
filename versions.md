MaDKitLanEdition
================
2.2.1 STABLE (Build: 3686) (from 22/05/2015 to 13/10/2021)

# Creator(s):
Jason MAHDJOUB

# Developer(s):
Fabien MICHEL (Entered in the team at 01/02/1997)
Jason MAHDJOUB (Entered in the team at 22/05/2015)

# Modifications:


### 2.2.1 STABLE (13/10/2021)
* Update Utils to 5.20.3 STABLE
* Update OOD to 3.1.2 STABLE
* Better log formatting
* Fix possible infinite loop between peer and server, when an error occurs
* Do not use group manager role when sending message without specifying role
* Peers can now synchronize their database through several database backup servers
* Pool executor : fix bad use of maximum number of threads, and permit to create more threads when the maximum of threads was not reached and when tasks are waiting to be executed.


### 2.2.0 STABLE (01/10/2021)
* Update Utils to 5.19.7 STABLE
* Update OOD to 3.1.1 STABLE
* Disable useless server stream into Upnp IGD
* UPNP IGD test pass
* Complete filter of network interfaces
* Fix issue when determining if a local ip is compatible with another ip
* Fix XXE security issue with Cling dependency : https://github.com/4thline/cling/issues/243
* Fix DDOS and SSRF security issues with Cling dependency : https://github.com/4thline/cling/issues/253
* Fix issue with AgentExecutor when an agent is launched and terminated too quickly
* Add the possibility for a scheduled task to be executed even if the agent who launched the task was killed
* Finish closing connexions even if NIO agent was killed
* Fix issue with agent fake thread and avoid reception of two messages (or more) at the same time


### 2.2.0 BETA 1 (07/07/2021)
* Update Utils to 5.18.5 STABLE
* Update OOD to 3.1.0 Beta 2
* Make Utils and OOD compatible with Android
* Reimplement connection protocol using new Utils classes
* Permit MaDKit to be a central database backup server in order to synchronize database of distant peers
* Check distant and not only local ports with filters of connection protocols and with filter of access protocols
* Fix unexpected high CPU usage due to a bad socket channel using


### 2.1.10 STABLE (15/02/2020)
* Update Utils to 4.10.2
* Update OOD to 2.4.2


### 2.1.8 STABLE (11/02/2020)
* Update Utils to 4.9.0
* Update OOD to 2.4.0


### 2.1.7 STABLE (25/01/2020)
* Generate random messages only if messages are lower than network block size


### 2.1.6 STABLE (24/01/2020)
* Update Utils to 4.8.5
* Update OOD to 2.3.20
* Rewrite agent thread executors and thread pool executors.
* Fiw issue when purging messages of a killed agent.


### 2.1.5 STABLE (10/01/2020)
* Update OOD to 2.3.14 STABLE


### 2.1.4 STABLE (17/12/2019)
* Update Utils to 4.7.1 STABLE
* Update OOD to 2.3.13 STABLE


### 2.1.3 STABLE (22/11/2019)
* Update Utils to 4.7.0 STABLE
* Update OOD to 2.3.11 STABLE
* Use LoginData.invalidCloudPassword function


### 2.1.1 STABLE (19/11/2019)
* Rewrite PairOfIdentifiers class


### 2.1.0 STABLE (18/11/2019)
* Update Utils to 4.6.5 STABLE
* Update OOD to 2.3.10 STABLE
* Update JDKRewriteUtils to 1.0.4 STABLE
* Filter distant network roles, and not only distant network groups
* Compile with openjdk 13 (compatibility set to Java 7
* Fix network broadcast message issue when no local agent is present, and when network agents does not received broadcast message


### 2.0.4 STABLE (31/10/2019)
* Update Utils to 4.6.2 STABLE
* Update OOD to 2.3.7 STABLE
* Update documentation


### 2.0.3 STABLE (19/10/2019)
* Update Utils to 4.6.1 STABLE
* Update OOD to 2.3.6 STABLE
* Update dependencies


### 2.0.0 STABLE (18/10/2019)
* Update Utils to 4.6.0 STABLE
* Update OOD to 2.3.4 STABLE
* Cloud identifiers can be individually anonymous thanks to an encryption process.
* Host identifiers are sent only if the cloud identifier authentication process succeeded.
* Authentication can be done automatically with public key, through a shared password/key, or both
* P2PSecuredConnectionProtocolWithKeyAgreement algorithm permit to make client/server authentication throw asymmetric signatures
* An identifier is composed of a cloud identifier, and a host identifier. In the past, one authentication concerned both cloud and host identifiers. Now it is possible to have two authentications : one for the cloud identifier, and one another for the host identifier. If one of them fails, than identifier is rejected.
* Use hybrid connexion protocols that enables to use at the same time non post quantum algorithms and post quantum algorithms. It is to prevent the quantum supremacy without loosing the benefits of stable encryption algorithms. For client/server connexion, two asymmetric key pairs are then used : one for a non post quantum algorithm like RSA and one for a post quantum algorithm like Mc Eliece
* Synchronize local database with other peers


### 1.11.1 STABLE (24/04/2019)
* Minimal modification into SecuredObjectOutputStream and SecuredObjectInputStream classes. 
* Fix bad use of garbage collector with ConversationID. 


### 1.11.0 STABLE (23/04/2019)
* Add SecuredObjectOutputStream and SecuredObjectInputStream classes. Do not use native ObjectInputStream.
* Add possibility to send asynchronous messages (AbstractAgent.sendMessageWithRoleOrDifferSendingUntilRecipientWasFound).


### 1.10.2 STABLE (08/04/2019)
* Add hashCode function into class AbstractIP.
* Better manage externalization.
* Better manage identifiers of connection protocol negotiator.
* Add possibility to change connection protocols during MaDKit life, and not only during MaDKit loading.
* Add possibility to change access protocols during MaDKit life, and not only during MaDKit loading.
* Add possibility to change access data during MaDKit life, and not only during MaDKit loading.
* Add possibility to trigger connection/disconnection only if these were not present into the list of connection to attempt at the MaDKit starting defined into NetworkProperties class. Concerned functions are AbstractAgent.manageDirectConnection, AbstractAgent.manageDirectConnections, AbstractAgent.manageDirectConnectionAndAddItToNetworkProperties and AbstractAgent.manageDirectConnectionsAndAddThemToNetworkProperties.


### 1.10.1 STABLE (26/03/2019)
* Do not do useless thread locking when CGR groups are synchronized with distant peers.
* Restore BigDataTransferID to previous previous (new version caused issues with Junit tests).
* Fix issue during MaDKit ending.


### 1.10.0 STABLE (25/03/2019)
* Update OOD to 2.0.0 BETA 104.
* Update Utils to 3.25.5 STABLE.
* Add new connection protocol with symmetric keys: P2PSecuredConnectionProtocolWithKnownSymmetricKeys.
* Better tests for network transfers.
* Improve synchronization of list of distant agents.
* Security issue : control that received distant agents list synchronization concerns the good distant kernel address.
* Insure that list of distant agents are removed when peers are disconnected.
* When a connection fail, try another ip.
* Change information sending order when a new connection was established (fix a problem with synchronization of CGR that is done after network messages are done).
* Fix issue with replies not sent with killed agents : killed agent send now empty reply for messages that need reply.


### 1.9.6 STABLE (01/03/2019)
* Remove obsolete code.
* Rename function into HostIdentifier class.
* Typo corrections.
* Add possibility to specify the profile identifier into class ServerSecuredProtocolPropertiesWithKnownPublicKey.


### 1.9.5 STABLE (06/02/2019)
* Update OOD to 2.0.0 BETA 99.
* Update Utils to 3.25.1


### 1.9.4 STABLE (08/02/2019)
* Code cleaning/optimizing.


### 1.9.2 STABLE (05/02/2019)
* Update OOD to 2.0.0 BETA 97.
* OOD - Security fix : disable cache for tables that use secret ou private keys
* OOD - Security improvement : add Field.disableCache property


### 1.9.1 STABLE (18/01/2019)
* Update OOD to 2.0.0 BETA 95.
* Set default OOD driver to H2 database.


### 1.9.0 STABLE (13/01/2019)
* Update OOD to 2.0.0 BETA 94.
* Update Utils to 3.24.0.
* Manage asymmetric auto-signed login.
* Better computer manage sleep mode.
* Manage moment of connection, and connection retry when failed


### 1.8.2 STABLE (24/11/2018)
* Improve security of Client/Server connection protocol.
* Resolve a concurrent modification exception into internal role.


### 1.8.1 STABLE (23/11/2018)
* Improve security of Client/Server connection protocol.


### 1.8.0 STABLE (21/11/2018)
* Detect security anomalies during big data transfers.
* Correction of Group.equals() with null references.
* Better manage ban with deserialization process.
* Update Utils to 3.22.1.
* Update OOD to 2.0.0 BETA 91.
* Add function AbstractAgent.getAccessibleKernelsFilteredByGroupsGivenByDistantPeer(Group).
* Add function AbstractAgent.getAccessibleKernelsFilteredByGroupsGivenToDistantPeer(Group).
* Check public key validity with client/server connexion protocol.
* Add ConnectionProtocolNegotiator and ConnectionProtocolNegotiatorProperties classes.
* Fix issue with client/server vs peer-to-peer protocol negotiation.
* Optimizing a problem of simultaneous network data send to several peers : data was sent peer after peer, and not to all peers at the same time.
* Add possibility to limit global download bandwidth and global upload bandwidth.
* Detect OS wake up after a sleep mode.
* Update Cling to 2.1.2.
* Fix issue with router updating.


### 1.7.6 STABLE (01/08/2018)
* Update OOD to 2.0.0 BETA 86.
* Update Utils to 3.19.0.
* Add save functions into MadKit Properties.
* Fix network messages serialization problem.


### 1.7.5 STABLE (27/07/2018)
* Update OOD to 2.0.0 BETA 85.
* Update Utils to 3.18.0.
* Save MKLE configuration that are different from a reference configuration. Other properties are not saved.


### 1.7.3 STABLE (20/07/2018)
* Update OOD to 2.0.0 BETA 84.
* Update Utils to 3.17.0.
* Fix version's control issue of distant peer.


### 1.7.1 STABLE (13/07/2018)
* Update OOD to 2.0.0 BETA 83.
* Update Utils to 3.16.1.
* Improve version's control of distant peer.
* Clean code.


### 1.7.0 STABLE (20/05/2018)
* Update OOD to 2.0.0 BETA 82.
* Update Utils to 3.15.0.
* Add P2P connection protocol that support parametrisation of key agreement.
* Support several key agreement (including Post Quantum Cryptography key agreement (New Hope)).
* Fix security issue : when data is sent without being wrote (default memory state), fill it with zeros.
* Fix security issue : sign symmetric encryption key into client/server connection protocol.
* Fix security issue : with P2P key agreements, generate signature and encryption keys with two steps (instead of one), in order to sign the exchanged symmetric encryption key.
* Fix security issue : class serialization are now filtered with allow list and deny list. Classes that are not into deny list must implement the interference 'SerializableAndSizable'. Messages sent to the network must implement the interface NetworkMessage.
* Optimization : use externalization process instead of deserialization process during lan transfer.
* Fix security issue : classes externalization processes control now the allocated memory during de-externalization phase.
* Security enhancement : initialisation vectors used with encryption has now a secret part composed of counter that is increased at each data exchange.
* Security enhancement : signature and encryption process use now a secret message that is increased at each data exchange.
* Security enhancement : P2P login agreement use now JPake and a signature authentication if secret key for signature is available (PasswordKey.getSecretKeyForSignature()).
* Fix issue with dead lock into indirect connection process.
* Fix issue with dual connection between two same kernels.
* Externalising Java rewrote classes into JDKRewriteUtils project.
* Support of authenticated encryption algorithms. When use these algorithms, MKLE do not add a signature with independent MAC.
* Add some benchmarks.
* Support of YAML file properties.


### 1.6.5 STABLE (27/02/2018)
* Debug UPNP connexion with macOS.
* Fix issue with multiple identical router's messages : do not remove the router to recreate it.


### 1.6.4 STABLE (15/02/2018)
* Fix problem of port unbind with Windows.
* Fix problem of simultaneous connections with Mac OS
* Fix problem with interface address filtering


### 1.6.3 STABLE (10/02/2018)
* Update OOD to 2.0.0 BETA 66.
* Update Utils to 3.10.5
* Change minimum public key size from 1024 to 2048


### 1.6.2 STABLE (10/02/2018)
* Update OOD to 2.0.0 BETA 65.
* Update Utils to 3.10.4
* Change minimum public key size from 1024 to 2048


### 1.6.1 STABLE (04/02/2018)
* Overlookers were not aware from new roles adding. Fix this issue.
* Add MadKit demos


### 1.6.0 STABLE (31/01/2018)
* Updating OOD to 2.0.0 BETA 59
* Updating Utils to 3.9.0
* Messages can now be atomically non encrypted


### 1.5.2 STABLE (13/12/2017)
* Updating OOD to 2.0.0 BETA 57
* Updating Utils to 3.7.1
* Debugging JavaDoc


### 1.5.0 STABLE (13/11/2017)
* Updating OOD to 2.0.0 BETA 55
* Packets can now have sizes greater than Short.MAX_VALUE


### 1.4.5 STABLE (02/11/2017)
* Updating OOD to 2.0.0 BETA 54


### 1.4.0 STABLE (13/10/2017)
* Updating OOD to 2.0.0 BETA 48
* Several modifications into connection and access protocols
* Adding approved randoms parameters into MaDKitProperties
* Adding point to point transfer connection signature and verification
* Saving automatically random seed to be reload with the next application loading


### 1.2.1 STABLE (31/08/2017)
* Including resources in jar files


### 1.2.0 STABLE (05/08/2017)
* Correction a problem with database
* Adding P2PSecuredConnectionProtocolWithECDHAlgorithm connection protocol (speediest)
* Adding Client/ServerSecuredConnectionProtocolWithKnownPublicKeyWithECDHAlgorithm connection protocol (speediest)
* Now all connection protocols use different keys for encryption and for signature
* Adding AccessProtocolWithP2PAgreement (speediest)
* Debugging desktop JFrame closing (however the JMV still become opened when all windows are closed)
* Several minimal bug fix
* Correction of JavaDoc
* Updating OOD to 2.0.0 BETA 20 version


### 1.1.3 STABLE (05/08/2017)
* Updating OOD to 2.0.0 BETA 15


### 1.1.2 STABLE (05/08/2017)
* Updating OOD to 2.0.0 BETA 14
* Optimizing some memory leak tests


### 1.1.0 STABLE (04/08/2017)
* Convert project to Gradle project


### 1.0.0 STABLE (04/06/2017)
* Correction of a bug with database disconnection
* Debugging indirect connections
* Solving a memory leak problem with ConversationID
* Solving a memory leak problem with TransferAgent (not killed)
* Solving problem when deny BigDataProposition and kill agent just after
* Indirect connection send now ping message
* Adding allow list for InetAddresses in network properties
* Correcting problems of internal group/role references/dereferences


### 1.0.0 BETA 4 (27/05/2017)
* Agents are now identified by a long (and not int)
* Adding the function AbstractAgent.getAgentID()
* Removing static elements in Conversation ID


### 1.0.0 BETA 3 (23/05/2017)
* Update Utils to 2.7.1
* Update OOD to 2.0.0 BETA 1
* JDK 7 compatible


### 1.0.0 BETA 2 (07/03/2017)
* Reinforce secret identifier/password exchange
* Add agent to launch into MKDesktop windows


### 1.0.0 BETA 0 (04/03/2017)
* First MaDKitLanEdition release, based on MaDKit

