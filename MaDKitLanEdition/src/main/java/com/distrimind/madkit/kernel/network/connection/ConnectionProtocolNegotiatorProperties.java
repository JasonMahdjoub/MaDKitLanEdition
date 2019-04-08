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
package com.distrimind.madkit.kernel.network.connection;

import com.distrimind.madkit.exceptions.BlockParserException;
import com.distrimind.madkit.exceptions.ConnectionException;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Properties of negotiator of connection protocols
 * It is impossible to use sub protocols into connection associated to protocol negotiator.
 * Use instead several protocol negotiators as sub protocols of this instance,
 * and whose negotiated protocols do not contains sub protocols
 *
 * @author Jason Mahdjoub
 * @version 1.0
 * @since MadkitLanEdition 1.8
 *
 */
public class ConnectionProtocolNegotiatorProperties extends ConnectionProtocolProperties<ConnectionProtocolNegotiator>
{
    public static final short MAXIMUM_NUMBER_OF_CONNECTION_PROTOCOLS=500;
    private int lastIdentifier;
    private HashMap<Integer, ConnectionProtocolProperties<?>> connectionProtocolProperties;
    private HashMap<Integer, Boolean> validationOfConnectionProtocols;
    private HashMap<Integer, Integer> connectionProtocolsPriorities;
    private volatile transient Map<Integer, Integer> validPriorities=null;

    public ConnectionProtocolNegotiatorProperties() {
        super(ConnectionProtocolNegotiator.class);

        lastIdentifier=0;
        connectionProtocolProperties=new HashMap<>();
        validationOfConnectionProtocols=new HashMap<>();
        connectionProtocolsPriorities=new HashMap<>();
    }
    public ConnectionProtocolNegotiatorProperties(ConnectionProtocolNegotiatorProperties c) {
        super(ConnectionProtocolNegotiator.class);

        lastIdentifier=c.lastIdentifier;
        connectionProtocolProperties=new HashMap<>(c.connectionProtocolProperties);
        validationOfConnectionProtocols=new HashMap<>(c.validationOfConnectionProtocols);
        connectionProtocolsPriorities=new HashMap<>(c.connectionProtocolsPriorities);
    }

    /**
     * Add a possible connection protocol that this peer can accept.
     * The given connection protocol will be chosen according the given priority
     * @param cpp the connection protocol
     * @param priority the priority (higher number means higher priority)
     * @return the connection protocol identifier
     * @throws ConnectionException if the limit of connection protocols have been reached
     * @throws IllegalArgumentException if the connection protocol is incompatible with presents connection protocols (see {@link #needsServerSocketImpl()}, {@link #supportBidirectionalConnectionInitiativeImpl()}, {@link #needsServerSocketImpl()}, {@link #canBeServer()})
     */
    @SuppressWarnings("UnusedReturnValue")
    public int addConnectionProtocol(ConnectionProtocolProperties<?> cpp, int priority) throws ConnectionException {
        return this.addConnectionProtocol(generateIdentifier(), cpp, priority);
    }

    private int generateIdentifier()
    {
        int id=lastIdentifier+1;

        for (int i : connectionProtocolProperties.keySet()) {
            if (i >= id) {
                id=i+1;
            }
        }

        return id;
    }

    /**
     * Add a possible connection protocol that this peer can accept.
     * The given connection protocol will be chosen according the given priority
     * @param protocolIdentifier the protocol identifier
     * @param cpp the connection protocol
     * @param priority the priority (higher number means higher priority)
     * @return the connection protocol identifier
     * @throws ConnectionException if the limit of connection protocols have been reached
     * @throws IllegalArgumentException if the connection protocol is incompatible with presents connection protocols (see {@link #needsServerSocketImpl()}, {@link #supportBidirectionalConnectionInitiativeImpl()}, {@link #needsServerSocketImpl()}, {@link #canBeServer()})
     */
    @SuppressWarnings("UnusedReturnValue")
    public int addConnectionProtocol(int protocolIdentifier, ConnectionProtocolProperties<?> cpp, int priority) throws ConnectionException {
        if (cpp==null)
            throw new NullPointerException();
        for (int i : connectionProtocolProperties.keySet())
            if (i==protocolIdentifier)
                throw new IllegalArgumentException("Identifier "+i+" is already taken");
        if (!connectionProtocolProperties.isEmpty())
        {
            if (needsServerSocketImpl()!=cpp.needsServerSocketImpl() ||
                    canTakeConnectionInitiativeImpl()!=cpp.canTakeConnectionInitiativeImpl() ||
                    supportBidirectionalConnectionInitiativeImpl()!=cpp.supportBidirectionalConnectionInitiativeImpl() ||
                    canBeServer()!=cpp.canBeServer() ||
                    isEncrypted()!=cpp.isEncrypted())
                throw new IllegalArgumentException("Incompatible connection protocol with presents connection protocols");
        }
        if (connectionProtocolProperties.size()>MAXIMUM_NUMBER_OF_CONNECTION_PROTOCOLS)
            throw new ConnectionException("Limit of connection protocols is reached");

        connectionProtocolProperties.put(protocolIdentifier, cpp);
        validationOfConnectionProtocols.put(protocolIdentifier, true);
        connectionProtocolsPriorities.put(protocolIdentifier, priority);
        validPriorities=null;
        lastIdentifier=protocolIdentifier;
        return protocolIdentifier;
    }

    /**
     * Tels if the connection protocol that corresponds to the given identifier is enabled
     * @param identifier the identifier
     * @return true if the connection protocol that corresponds to the given identifier is enabled
     */
    public boolean isValidConnectionProtocol(int identifier)
    {
        Boolean v=validationOfConnectionProtocols.get(identifier);
        return v!=null && v;
    }

    /**
     * Invalidate the connection protocol that corresponds to the given identifier
     * @param identifier the identifier
     */
    public void invalidateConnectionProtocol(int identifier) {
        Boolean v=validationOfConnectionProtocols.get(identifier);
        if (v!=null)
            validationOfConnectionProtocols.put(identifier, false);
    }

    /**
     * Gets the priorities associated to connection protocol identifiers
     * @return the priorities
     */
    public Map<Integer, Integer> getValidPriorities()
    {
        if (validPriorities==null) {

            HashMap<Integer, Integer> res=new HashMap<>();
            for (Map.Entry<Integer, Integer> e : connectionProtocolsPriorities.entrySet()) {
                if (isValidConnectionProtocol(e.getKey()))
                    res.put(e.getKey(), e.getValue());
            }
            validPriorities= Collections.unmodifiableMap(res);
        }
        return validPriorities;
    }


    public ConnectionProtocolProperties<?> getConnectionProtocolProperties(Map<Integer, Integer> priorities) {
        int max=Integer.MIN_VALUE;
        Integer res=null;
        for (Map.Entry<Integer, Integer> e : priorities.entrySet()) {
            Integer v=connectionProtocolsPriorities.get(e.getKey());
            if (v!=null) {
                int s=v+e.getValue();
                if (s > max || (s==max && res!=null && res<e.getKey())) {
                    max = s;
                    res = e.getKey();
                }
            }
        }
        if (res==null)
            return null;
        else
            return connectionProtocolProperties.get(res);
    }

    /**
     * Change the connection protocol priority
     * @param identifier the connection protocol identifier
     * @param priority the priority (higher number means higher priority)
     * @return the old priority
     */
    public int setPriority(int identifier, int priority)
    {
        if (connectionProtocolsPriorities.containsKey(identifier))
            return Objects.requireNonNull(connectionProtocolsPriorities.put(identifier, priority));
        else
            throw new IllegalArgumentException();
    }


    @Override
    public boolean needsServerSocketImpl() {
        if (connectionProtocolProperties.isEmpty())
            throw new IllegalAccessError("You must first add a connection protocol to this connection protocol negotiator !");
        return connectionProtocolProperties.get(lastIdentifier).needsServerSocketImpl();
    }

    @Override
    public boolean supportBidirectionalConnectionInitiativeImpl() {
        if (connectionProtocolProperties.isEmpty())
            throw new IllegalAccessError("You must first add a connection protocol to this connection protocol negotiator !");
        return connectionProtocolProperties.get(lastIdentifier).supportBidirectionalConnectionInitiativeImpl();

    }

    @Override
    public boolean canTakeConnectionInitiativeImpl() {
        if (connectionProtocolProperties.isEmpty())
            throw new IllegalAccessError("You must first add a connection protocol to this connection protocol negotiator !");
        return connectionProtocolProperties.get(lastIdentifier).canTakeConnectionInitiativeImpl();
    }

    @Override
    public boolean canBeServer() {
        if (connectionProtocolProperties.isEmpty())
            throw new IllegalAccessError("You must first add a connection protocol to this connection protocol negotiator !");
        return connectionProtocolProperties.get(lastIdentifier).canBeServer();
    }

    public HashMap<Integer, Integer> getConnectionProtocolsPriorities() {
        return connectionProtocolsPriorities;
    }

    @Override
    public void checkProperties() throws ConnectionException {
        if (connectionProtocolProperties==null)
            throw new ConnectionException();
        if (validationOfConnectionProtocols==null)
            throw new ConnectionException();
        if (connectionProtocolsPriorities==null)
            throw new ConnectionException();
        if (connectionProtocolProperties.size()!=validationOfConnectionProtocols.size())
            throw new ConnectionException();
        if (connectionProtocolProperties.size()!=connectionProtocolsPriorities.size())
            throw new ConnectionException();
        boolean v=false;
        for (Map.Entry<Integer, ConnectionProtocolProperties<?>> e : connectionProtocolProperties.entrySet())
        {
            Boolean val=validationOfConnectionProtocols.get(e.getKey());
            if (e.getValue().subProtocolProperties!=null)
                throw new ConnectionException("Impossible to use sub protocols into connection associated to protocol negotiator");
            if (val==null)
                throw new ConnectionException();
            if (val) {
                v = true;
                e.getValue().checkProperties();
            }
            if (connectionProtocolsPriorities.get(e.getKey())==null)
                throw new ConnectionException();


        }
        if (!v)
            throw new ConnectionException();
    }

    @Override
    public boolean needsMadkitLanEditionDatabase() {
        for (ConnectionProtocolProperties<?> cpp : connectionProtocolProperties.values())
            if (cpp.needsMadkitLanEditionDatabase())
                return true;
        return false;
    }

    @Override
    public boolean isEncrypted() {
        if (connectionProtocolProperties.isEmpty())
            throw new IllegalAccessError("You must first add a connection protocol to this connection protocol negotiator !");

        return connectionProtocolProperties.get(lastIdentifier).isEncrypted();
    }

    @Override
    public int getMaximumBodyOutputSizeForEncryption(int size) throws BlockParserException {
        int res=0;
        for (ConnectionProtocolProperties<?> cpp : connectionProtocolProperties.values())
            res=Math.max(cpp.getMaximumBodyOutputSizeForEncryption(size), res);
        return res;
    }

    private transient Integer maxHeadSize=null;
    @Override
    public int getMaximumSizeHead() throws BlockParserException {
        if (maxHeadSize==null) {
            int res = 0;
            for (ConnectionProtocolProperties<?> cpp : connectionProtocolProperties.values())
                res = Math.max(cpp.getMaximumSizeHead(), res);
            maxHeadSize=res;
        }
        return maxHeadSize;
    }

    public HashMap<Integer, ConnectionProtocolProperties<?>> getConnectionProtocolProperties() {
        return connectionProtocolProperties;
    }
}
