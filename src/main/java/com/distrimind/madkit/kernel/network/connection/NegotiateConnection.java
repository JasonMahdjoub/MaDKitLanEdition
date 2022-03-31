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

import com.distrimind.util.io.Integrity;
import com.distrimind.util.io.MessageExternalizationException;
import com.distrimind.util.io.SecuredObjectInputStream;
import com.distrimind.util.io.SecuredObjectOutputStream;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author Jason Mahdjoub
 * @version 1.1
 * @since MadkitLanEdition 1.0
 */
public class NegotiateConnection extends AskConnection {
    private Map<Integer, Integer> priorities;

    @SuppressWarnings("unused")
    private NegotiateConnection()
    {

    }
    public NegotiateConnection(boolean _you_are_asking, Map<Integer, Integer> priorities) {
        super(_you_are_asking);
        if (priorities==null)
            throw new NullPointerException();
        this.priorities=priorities;
    }
    static Map<Integer, Integer> readPriorities(SecuredObjectInputStream in) throws IOException {
        short s=in.readShort();
        if (s<0 || s>ConnectionProtocolNegotiatorProperties.MAXIMUM_NUMBER_OF_CONNECTION_PROTOCOLS)
            throw new MessageExternalizationException(Integrity.FAIL_AND_CANDIDATE_TO_BAN);
        Map<Integer, Integer> priorities=new HashMap<>();
        for (int i=0;i<s;i++)
        {
            int k=in.readInt();
            int p=in.readInt();
            priorities.put(k, p);
        }
        return priorities;
    }
    @Override
    public void readExternal(SecuredObjectInputStream in) throws IOException, ClassNotFoundException {
        super.readExternal(in);
        priorities=readPriorities(in);
    }
    static void writePriorities(SecuredObjectOutputStream oos, Map<Integer, Integer> priorities) throws IOException {
        if (priorities.size()>ConnectionProtocolNegotiatorProperties.MAXIMUM_NUMBER_OF_CONNECTION_PROTOCOLS)
            throw new IOException();
        oos.writeShort(priorities.size());
        for (Map.Entry<Integer, Integer> e : priorities.entrySet())
        {
            oos.writeInt(e.getKey());
            oos.writeInt(e.getValue());
        }
    }
    @Override
    public void writeExternal(SecuredObjectOutputStream oos) throws IOException {
        super.writeExternal(oos);
        writePriorities(oos, priorities);
    }

    public Map<Integer, Integer> getPriorities() {
        return priorities;
    }

}
