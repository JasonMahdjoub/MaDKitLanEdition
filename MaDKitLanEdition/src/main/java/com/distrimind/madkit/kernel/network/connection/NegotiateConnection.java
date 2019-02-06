package com.distrimind.madkit.kernel.network.connection;

import com.distrimind.madkit.exceptions.MessageSerializationException;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.HashMap;
import java.util.Map;

@SuppressWarnings("ExternalizableWithoutPublicNoArgConstructor")
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

    @Override
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        super.readExternal(in);
        short s=in.readShort();
        if (s<0 || s>ConnectionProtocolNegotiatorProperties.MAXIMUM_NUMBER_OF_CONNECTION_PROTOCOLS)
            throw new MessageSerializationException(Integrity.FAIL_AND_CANDIDATE_TO_BAN);
        priorities=new HashMap<>();
        for (int i=0;i<s;i++)
        {
            int k=in.readInt();
            int p=in.readInt();
            priorities.put(k, p);
        }
    }

    @Override
    public void writeExternal(ObjectOutput oos) throws IOException {
        super.writeExternal(oos);
        if (priorities.size()>ConnectionProtocolNegotiatorProperties.MAXIMUM_NUMBER_OF_CONNECTION_PROTOCOLS)
            throw new IOException();
        oos.writeShort(priorities.size());
        for (Map.Entry<Integer, Integer> e : priorities.entrySet())
        {
            oos.writeInt(e.getKey());
            oos.writeInt(e.getValue());
        }
    }

    public Map<Integer, Integer> getPriorities() {
        return priorities;
    }
}
