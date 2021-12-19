MaDKitLanEdition: Multiagent Development Kit (Lan Edition)
==========================================================

[![CodeQL](https://github.com/JazZ51/MaDKitLanEdition/actions/workflows/codeql-analysis.yml/badge.svg)](https://github.com/JazZ51/MaDKitLanEdition/actions/workflows/codeql-analysis.yml)

What is MaDKit ?
-----------------

MaDKit is an open source multiagent-based development platform written in Java.
MaDKit is designed to easily build distributed applications and simulations using the multiagent paradigm and features:

* Artificial agents creation and life cycle management
* An organizational infrastructure for communication between agents, structuring the application
* High heterogeneity in agent architectures: No predefined agent model
* Multi-Agent based simulation and simulator authoring tools
* Multi-agent based distributed application authoring facilities

In contrast to conventional approaches, which are mostly agent-centered, MaDKit follows an organization-centered approach [OCMAS][1]
so that there is no predefnied agent model in MaDKit. So, MaDKit is built upon the AGR (Agent/Group/Role) organizational model:
MaDKit agents play roles in groups and thus create artificial societies.

What is MaDKitLanEdition ?
----------------------------

###### MadKitLanEdition integrates :
* MadKitGroupExtension's features in a native way
* Advanced network futures which enables to connect several peers between them, and emulate a secured multi-agent system that virtually evolves into the same environment, i.e. the same virtual machine.

Tutorials are not already available. For now, you can find inspiration with the available demos. Executable jar files are available on the corresponding section (see below).
You can also find inspiration into Junit tests.

###### Network futures of MaDKitLanEdition :
* Several network protocols
    * Peer to peer secured or unsecured connections
    * Client/server secured or unsecured connections
    * Several encryption algorithms, including authenticated encryption algorithms and Post Quantum Cryptography encryptions
    * Several signature algorithms, including  Post Quantum Cryptography signers
    * Possibility to combine pairs of encryption/signature algorithms that are non post quantum algorithm and post quantum algorithms. This possibility enable to use stable encryption algorithms that does not resist to quantum computer, with more experimental algorithm that theoritically resist to quantum computers.
    * Forward secrecy is supported only for this peer to peer secured connection protocol : P2PSecuredConnectionProtocolWithKeyAgreementAlgorithm. However it is possible to combine connetion protocols to enable Forward secrecy : for example client/server connection protocol can be used without encryption, and combined with P2PSecuredConnectionProtocolWithKeyAgreementAlgorithm with encryption enabled. Forward secrecy is than possible for client server connetions.
    * Symmetric secret key used for encryption and signature can be generated during the connection process, or can be known in advance by the concerned peers.
    * There is a possibility to choose between several key agreement algorithms, including Post Quantum Cryptography agreements
    * ConnectionProtocolNegotiator has a list of possible connection protocols annoted with level of priorities. Then During the negotitation, choosen connection protocol is the connection protocol that is common with the two peers, and that sum of the two priorities, given by the two peers, is the higher sum.
    * Sending the same packet from "the man in the middle" is impossible :
    	* Initialization vectors used with encryption has a secret part composed of counter that is increased at each data exchange.
    	* More over, signatures are computed with a secret part composed of counter that is increased at each data exchange.
* Access protocol manage peer to peer login and gives rights to specified groups of agents. 
  * An identifier is composed of a cloud identifier which identify a private subnetwork, and a host identifier which identify a machine.
  * A specific login protocol has been established in order to exchange password, secret keys or signatures, without compromising them if the distant peer is not a trusting peer. 
  * Same thing with the user login. 
  * If the user provide a symmetric secret key for signature, it is used to authentify random messages during the authentication process.
  * If is possible to auto-sign a login with an asymmetric secret key. Then the authentication is validated thanks to an auto-signed login. Autosigned identifiers concerns cloud identifiers and host identifiers. Auto-signed identifiers can be optionally used with classic password authentication. Auto-signed logins can also be rejected, even if the login is well auto-signed.
  * Authentication can be done with both auto-signed identifier and a shared password/key
  * An identifier is composed of a cloud identifier, and a host identifier. In the past, one authentication concerned both cloud and host identifiers. Now it is possible to have two authentications : one for the cloud identifier, and one another for the host identifier. If one of them fails, than identifier is rejected.
  * Cloud identifiers can be individually anonymous thanks to an encryption process. Host identifiers are sent only if the cloud identifier authentication process succeeded.
* possibility to connect randomly to one server between several available servers (simple load balancing)
* ability to ban IP's that generates a number of anomalies above a threshold. Banned IP's are stored into a database. A white ip list is possible. Anomalies can be a problem that occurs during the authentication process, the encryption and signature process, the deserialization process, and in every part of the network protocol. Anomalies considered as attacks trigger ban immediately whereas others anomalies that are suspected as attacks trigger ban only a threshold is reached.
* There are several levels of ban duration
* Ability to limit DDoS attacks (only limit). If a lot of peers ask for queries at the same time, the attacked peer will block every new query until old queries have been managed, this in order to avoid the explosion of the memory consumption. Moreover if one peer do too much queries, the attacked peer limit only the connexion of this peer. So there a global and a local limitation.
* Peers can communicate through several middle peers (Transfer connection). The system is able to make connections indirect, but secured (impossible to decode data for middle peers if the connection is secured).
* UPNP IGD futures :
    * Auto-connect with peers located into local network
    * Auto-open a router's port to enable direct connection with distant peers.
    * If no opened port is available or if no UPNP IGD rooter is available for two concerned distant peers, the system can choose to make an indirect connection between the two peers, through a dedicated server. This server is not able to decode messages between the two peers if the connection is secured.
* Permit to send simple messages between agents, or big data transfers (like files) between agents. Each connexion work in parallel. For one connexion, simple agent messages are sent according a FIFO policy (First In, First Out). However, there are some messages that are part of the MKLE protocol that are considered as a priority (i.e. Ping/Pong messages). For one connexion, big data transfers are sent in parallel with other big data transfers and with simple agent messages.
* Permit to send asynchronous messages and asynchronous big data messages. When sending asynchronous big data messages, transfer can be stopped by a peer disconnection. It will restart from the last position automatically when the two concerned peers become available. 
* Permit to cancel big message sending and asynchronous big message sending, during its emission
* Permit to suspend all current big data transfers, or big data transfers specific to a distant kernel address
* individual agent messages can be encrypted or not. By default, all agent messages are encrypted.
* Possibility to send asynchronous messages. If no receiver is available, the message to send is stored into the MaDKit database. When an agent with the targeted group/role becomes available, the message is sent to it before being removed from the database.
* multiple connections are possible between two same peers (through multiple interfaces). The system will use each connection in parallel by choosing for each packet to send the connection which offer more bandwidth or which is less busy
* IPV4 and IPV6 compatible at the same time
* The system detects hot plug of network interfaces
* middle man is limited by using an interfaced kernel address, when a peer try to use the same kernel address than an other peer.
* Conversation ID's are also interfaced for security reasons
* Agents that send distant messages are blocked until the message is sent, this in order to avoid increasing memory use while network is slower than agent's simulation
* possibility to get network metrics for each connection, and for each big data transfer.
* possibility to limit global download bandwidth and global upload bandwidth
* Connections are automatically closed if the distant peer does not answer to ping messages. Lost messages are identified, and messages which have not been sent are returned to sender agents.
* large scale conception (i.e. use of socket channel)
* Serialization of classes are now externalized. More over, there is a white list of classes that can be externalized and a black list that cannot. Moreover, over memory allocation (arrays or strings) is controlled during the deserialization process.
* Detect OS wake up after a sleep mode. Then reconnect all peers.
* Configuration files can be in several formats :
	* YAML format (default)
	* XML format
	* Properties format
* Transparent decentralized and secured database synchronization through the ORM [OOD](https://github.com/JazZ51/OOD) : 
  * Automatically synchronize database with other authorized peers, even if peers where disconnected (asynchronous mode).
  * Manage eventual conflicts during database synchronization
  * Automatically backup database and it historical to authorized servers and use end-to-end encryption : the servers are not able to read database in clear. 
  * Autotatically synchronize peers through server side database backup if peers are not connected (asynchronous mode)
  * Permit to revert database to an old version with a given time, and synchronize the restoration toward others peers and servers. It is possible to choose data source from local backup, other peers backup, or server backup.
  * Backups cannot be removed into servers from peers for a certain period. They cannot also be removed from a peer into a distant peer. This permit to garuantee the restoration even after an attack of a ransomware. 
  * Permit to restore database of peers thanks to other peers or thanks to servers
  * Automatically synchronize peers though intermediate peers when direct connections are not possible
  * Use encryption profile provider and to manage different version of secret keys in the case where a secret key was compromised
  * Possibility to schedule obsolete data cleaning into MaDKit database

All described features are tested and considered as stable.

###### Other futures of MaDKit Lan Edition :
* Agents can launch tasks (multi-threaded)
* AgentFakeThread is an agent whose react only when it receives a message. Theses kind of agents are multi-threaded.
* Possibility to define a blackboard for agents
* when broadcasting a message, possibility for an agent to get all answers in one time.
* Auto-request roles are possible. Agents can request roles only if they are requested by other agents.
* Compatible with Java 8 and newer


###### Madkit Group extension futures

On MadKit, a group contains several agents which plays several roles. In MadKitLanEdition, groups are represented as a hierarchy. So one group can be contained in a parent group, which can be contained in its turn in another parent group. In the same way, a group can contain several sub groups. That can be very usefull for the design of advanced multi-agent systems.

[<img src="mkge.png" align="center" alt="Partial MadKitGroupExtension UML representation"/>](mkge.png)

The class AbstractGroup is the super class of Group and MultiGroup. The class Group can represent one group, or one group and all its subgroups. The class MultiGroup can group several AbstractGroup which can be part of several branches of the group hierarchy. Then it is possible with the class MultiGroup to repesent a group, and all its subgroups, but also a group and another without requiring that must be part of the same branch of the tree of groups.

Note that one agent can't be attached to a MultiGroup or a Group that represents itself and its subgroups. It can be attached to a Group that represent only itself. This choice has been done to not loose the user. Indeed, if the agent could handle a set of groups, it should handle all groups represented by this set. However, this set aims to evolve over the time, especially if it represents subgroups. So the groups of the agent should evolve over the time. We have decided that this is too much ambigous for the user.

[<img src="groups.png" align="center" alt="Here you will find examples in the use of classes Group and MultiGroup"/>](groups.png)

###### Requirements under Linux :
  * Please install the package ethtool


### Changes

[See historical of changes](./changelog.md)

How to use it ?
---------------
### With Gradle :

Adapt into your build.gradle file, the next code :

	...
	repositories {
		...
		maven {
	       		url "https://artifactory.distri-mind.fr/artifactory/gradle-release"
	   	}
		...
	}
	...
	dependencies {
		...
		compile(group:'com.distrimind.madkit', name: 'MaDKitLanEdition', version: '2.3.2-STABLE')
		...
	}
	...

To know what last version has been uploaded, please refer to versions availables into [this repository](https://artifactory.distri-mind.fr/artifactory/DistriMind-Public/com/distrimind/madkit/MaDKitLanEdition/)
### With Maven :
Adapt into your pom.xml file, the next code :

	<project>
		...
		<dependencies>
			...
			<dependency>
				<groupId>com.distrimind.madkit</groupId>
				<artifactId>MaDKitLanEdition</artifactId>
				<version>2.3.2-STABLE</version>
			</dependency>
			...
		</dependencies>
		...
		<repositories>
			...
			<repository>
				<id>DistriMind-Public</id>
				<url>https://artifactory.distri-mind.fr/artifactory/gradle-release</url>
			</repository>
			...
		</repositories>
	</project>

To know what last version has been uploaded, please refer to versions availables into [this repository](https://artifactory.distri-mind.fr/artifactory/DistriMind-Public/com/distrimind/madkit/MaDKitLanEdition/)

How to get demos ?
------------------

To get MaDKitLanEditions demos, please download the lastest [repository](https://artifactory.distri-mind.fr/artifactory/DistriMind-Public/com/distrimind/madkitdemos/MaDKitLanEditionDemos/)

[1]: http://www.lirmm.fr/~fmichel/publi/pdfs/ferber04ocmas.pdf

###### Requirements under Ubuntu/Debian :
  * Please install the package ethtool, rng-tools, mtr(only debian), libcanberra-gtk-module
