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
import com.distrimind.madkit.exceptions.NIOException;
import com.distrimind.madkit.kernel.MadkitProperties;
import com.distrimind.madkit.kernel.network.*;
import com.distrimind.ood.database.DatabaseWrapper;

import java.net.InetSocketAddress;

/**
 * Negotiator of connection protocols
 *
 * @author Jason Mahdjoub
 * @version 1.0
 * @since MadkitLanEdition 1.8
 *
 */
public class ConnectionProtocolNegotiator extends ConnectionProtocol<ConnectionProtocolNegotiator> {
    private Status status=Status.DISCONNECTED;
    private final ConnectionProtocolNegotiatorProperties nproperties;
    private Parser parser;
    private ConnectionProtocol<?> selectedConnectionProtocol;
    private boolean needToRefreshTransferBlockChecker=true;
    private final MadkitProperties mkProperties;
    private boolean stateJustChanged=false;

    protected ConnectionProtocolNegotiator(InetSocketAddress _distant_inet_address, InetSocketAddress _local_interface_address, ConnectionProtocol<?> _subProtocol, DatabaseWrapper sql_connection, MadkitProperties _properties, ConnectionProtocolProperties<?> cpp, int subProtocolLevel, boolean isServer, boolean mustSupportBidirectionnalConnectionInitiative) throws ConnectionException {
        super(_distant_inet_address, _local_interface_address, _subProtocol, sql_connection, _properties, cpp,subProtocolLevel, isServer, mustSupportBidirectionnalConnectionInitiative);
        nproperties=(ConnectionProtocolNegotiatorProperties)getProperties();
        this.nproperties.checkProperties();
        selectedConnectionProtocol=null;
        this.parser=new Parser();
        this.mkProperties=_properties;

    }


    @Override
    protected ConnectionMessage getNextStep(ConnectionMessage _m) throws ConnectionException {
        switch(status)
        {
            case DISCONNECTED:
                if (_m instanceof NegotiateConnection)
                {
                    if (((NegotiateConnection) _m).isYouAreAsking())
                    {
                        return new UnexpectedMessage(this.getDistantInetSocketAddress());
                    }
                    else
                    {

                        ConnectionProtocolProperties<?> cpp=nproperties.getConnectionProtocolProperties(((NegotiateConnection) _m).getPriorities());
                        if (cpp==null)
                        {
                            status=Status.CLOSED_CONNECTION;
                            return new ConnectionFinished(this.getDistantInetSocketAddress(),
                                    ConnectionClosedReason.CONNECTION_PROPERLY_CLOSED);
                        }
                        else
                        {
                            try {
                                selectedConnectionProtocol=cpp.getConnectionProtocolInstance(this.getDistantInetSocketAddress(), this.getLocalInterfaceAddress(), this.getDatabaseWrapper(),this.mkProperties, this.isServer, this.mustSupportBidirectionnalConnectionInitiative, network_properties.encryptionRestrictionForConnectionProtocols);
                            } catch (NIOException e) {
                                throw new ConnectionException(e);
                            }

                            status=Status.PROTOCOL_CHOSEN;
                            needToRefreshTransferBlockChecker=true;
                            if (isCurrentServerAskingConnection())
                                return selectedConnectionProtocol.setAndGetNextMessage(new AskConnection(true));
                            else {
                                stateJustChanged=true;
                                return new NegotiateConnection(false, nproperties.getValidPriorities(network_properties.encryptionRestrictionForConnectionProtocols));
                            }
                        }
                    }
                }
                else if (_m instanceof AskConnection)
                {
                    if (((AskConnection) _m).isYouAreAsking()) {
                        return new NegotiateConnection(false, nproperties.getValidPriorities(network_properties.encryptionRestrictionForConnectionProtocols));
                    }
                    else {
                        status=Status.INVALID_CONNECTION;
                        return new UnexpectedMessage(this.getDistantInetSocketAddress());
                    }
                }
                else if (_m instanceof ConnectionFinished) {
                    needToRefreshTransferBlockChecker=true;
                    if (((ConnectionFinished) _m).getState()
                            .equals(ConnectionProtocol.ConnectionState.CONNECTION_ESTABLISHED)) {
                        status=Status.INVALID_CONNECTION;
                        return new UnexpectedMessage(this.getDistantInetSocketAddress());
                    } else {
                        status=Status.INVALID_CONNECTION;
                        return new ConnectionFinished(this.getDistantInetSocketAddress(),
                                ConnectionClosedReason.CONNECTION_ANOMALY);
                    }
                } else {
                    status=Status.INVALID_CONNECTION;
                    return new UnexpectedMessage(this.getDistantInetSocketAddress());
                }

            case PROTOCOL_CHOSEN:
                return selectedConnectionProtocol.setAndGetNextMessage(_m);
            case CLOSED_CONNECTION:
                if (selectedConnectionProtocol==null)
                    return null;
                else
                    return selectedConnectionProtocol.setAndGetNextMessage(_m);

            case INVALID_CONNECTION:
                return null;
        }
        return null;
    }

    @Override
    protected void closeConnection(ConnectionClosedReason _reason) throws ConnectionException {
        needToRefreshTransferBlockChecker=true;
        status=Status.CLOSED_CONNECTION;
        if (selectedConnectionProtocol!=null)
            selectedConnectionProtocol.setConnectionClosed(_reason);
    }


    @Override
    public int getMaximumBodyOutputSizeForEncryption(int size) throws BlockParserException
    {
        if (stateJustChanged || selectedConnectionProtocol==null)
            return super.getMaximumBodyOutputSizeForEncryption(size);
        else
            return selectedConnectionProtocol.getMaximumBodyOutputSizeForEncryption(size);
    }


    @Override
    public SubBlockParser getParser() {

        if (stateJustChanged || selectedConnectionProtocol==null)
            return parser;
        else
            return selectedConnectionProtocol.getParser();
    }

    @Override
    protected TransferedBlockChecker getTransferedBlockChecker(TransferedBlockChecker subBlockChercker) throws ConnectionException {

        if (stateJustChanged || selectedConnectionProtocol==null) {
            try {
                needToRefreshTransferBlockChecker=false;
                return new ConnectionProtocol.NullBlockChecker(subBlockChercker, this.isCrypted(),
                        (short) parser.getSizeHead());
            } catch (Exception e) {
                needToRefreshTransferBlockChecker = true;
                throw new ConnectionException(e);
            }
        }
        else
            return selectedConnectionProtocol.getTransferedBlockChecker(subBlockChercker);
    }

    @Override
    protected boolean isTransferBlockCheckerChangedImpl() {
        return (stateJustChanged || selectedConnectionProtocol==null)?needToRefreshTransferBlockChecker:selectedConnectionProtocol.isTransferBlockCheckerChanged();
    }

    @Override
    public PacketCounter getPacketCounter() {
        return (stateJustChanged || selectedConnectionProtocol==null)?null:selectedConnectionProtocol.getPacketCounter();
    }

    private enum Status
    {
        DISCONNECTED,
        PROTOCOL_CHOSEN,
        CLOSED_CONNECTION,
        INVALID_CONNECTION
    }

    private class Parser extends SubBlockParser
    {

        @Override
        public SubBlockInfo getSubBlock(SubBlock _block) throws BlockParserException {
            if (selectedConnectionProtocol==null)
                return new SubBlockInfo(_block, true, false);
            else
                return selectedConnectionProtocol.getParser().getSubBlock(_block);
        }

        @Override
        public SubBlock getParentBlock(SubBlock _block, boolean excludedFromEncryption) throws BlockParserException {
            if (selectedConnectionProtocol==null || stateJustChanged)
            {
                try {
                    return getParentBlockWithNoTreatments(_block);
                }
                finally
                {
                    stateJustChanged=false;
                }
            }
            else
                return selectedConnectionProtocol.getParser().getParentBlock(_block, excludedFromEncryption);
        }

        @Override
        public int getSizeHead() throws BlockParserException {
            if (selectedConnectionProtocol==null || stateJustChanged)
                return 0;
            else
                return selectedConnectionProtocol.getParser().getSizeHead();
        }



        @Override
        public int getBodyOutputSizeForEncryption(int size) throws BlockParserException {
            if (selectedConnectionProtocol==null || stateJustChanged)
                return size;
            else
                return selectedConnectionProtocol.getParser().getBodyOutputSizeForEncryption(size);
        }


        @Override
        public int getBodyOutputSizeForDecryption(int size) throws BlockParserException {
            if (selectedConnectionProtocol==null)
                return size;
            else
                return selectedConnectionProtocol.getParser().getBodyOutputSizeForDecryption(size);
        }
        /*private SubBlock signIfPossibleOutgoingPointToPointTransferedBlockWithNoEncryption(SubBlock _block) throws BlockParserException {
            SubBlock res= new SubBlock(_block.getBytes(), _block.getOffset() - getSizeHead(),
                    _block.getSize() + getSizeHead());
            byte[] tab=res.getBytes();
            for (int i=res.getOffset();i<_block.getOffset();i++)
                tab[i]=0;
            return res;

        }
        private SubBlockInfo checkEntrantPointToPointTransferedBlockWithNoEncryptin(SubBlock _block) throws BlockParserException {
            return new SubBlockInfo(new SubBlock(_block.getBytes(), _block.getOffset() + getSizeHead(),
                    _block.getSize() - getSizeHead()), true, false);
        }*/
        @Override
        public SubBlockInfo checkIncomingPointToPointTransferedBlock(SubBlock _block) throws BlockParserException {
            /*if (selectedConnectionProtocol==null)
                return checkEntrantPointToPointTransferedBlockWithNoEncryptin(_block);
            else*/
                return selectedConnectionProtocol.getParser().checkIncomingPointToPointTransferedBlock(_block);
        }

        @Override
        public SubBlock signIfPossibleOutgoingPointToPointTransferedBlock(SubBlock _block) throws BlockParserException {
            /*if (selectedConnectionProtocol==null || stateJustChanged)
                return signIfPossibleOutgoingPointToPointTransferedBlockWithNoEncryption(_block);
            else*/
                return selectedConnectionProtocol.getParser().signIfPossibleOutgoingPointToPointTransferedBlock(_block);

        }
    }
}
