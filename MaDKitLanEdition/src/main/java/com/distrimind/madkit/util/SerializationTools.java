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
package com.distrimind.madkit.util;

import com.distrimind.madkit.action.AgentAction;
import com.distrimind.madkit.action.GUIManagerAction;
import com.distrimind.madkit.action.SchedulingAction;
import com.distrimind.madkit.exceptions.MessageExternalizationException;
import com.distrimind.madkit.kernel.*;
import com.distrimind.madkit.kernel.network.*;
import com.distrimind.madkit.kernel.network.WithoutInnerSizeControl.Integrity;
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
import com.distrimind.util.AbstractDecentralizedID;
import com.distrimind.util.OS;
import com.distrimind.util.OSVersion;
import com.distrimind.util.crypto.*;
import com.distrimind.util.sizeof.ObjectSizer;
import com.distrimind.util.version.Version;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.*;

import static com.distrimind.madkit.util.ReflectionTools.*;

/**
 * 
 * @author Jason Mahdjoub
 * @since MaDKitLanEdition 1.7
 * @version 2.0
 * 
 */

public class SerializationTools {
	private static final int MAX_CHAR_BUFFER_SIZE=Short.MAX_VALUE*5;
	
	static void writeString(final SecuredObjectOutputStream oos, String s, int sizeMax, boolean supportNull) throws IOException
	{
		if (sizeMax<0)
			throw new IllegalArgumentException();

		if (s==null)
		{
			if (!supportNull)
				throw new IOException();
			if (sizeMax>Short.MAX_VALUE)
				oos.writeInt(-1);
			else
				oos.writeShort(-1);
			return;
			
		}
			
		if (s.length()>sizeMax)
			throw new IOException();
		if (sizeMax>Short.MAX_VALUE)
			oos.writeInt(s.length());
		else
			oos.writeShort(s.length());
		oos.writeChars(s);
	}
	private static final Object stringLocker=new Object();
	
	private static char[] chars=null;

	static String readString(final SecuredObjectInputStream ois, int sizeMax, boolean supportNull) throws IOException
	{
		if (sizeMax<0)
			throw new IllegalArgumentException();
		int size;
		if (sizeMax>Short.MAX_VALUE)
			size=ois.readInt();
		else
			size=ois.readShort();
		if (size==-1)
		{
			if (!supportNull)
				throw new MessageExternalizationException(Integrity.FAIL_AND_CANDIDATE_TO_BAN);
			return null;
		}
		if (size<0 || size>sizeMax)
			throw new MessageExternalizationException(Integrity.FAIL_AND_CANDIDATE_TO_BAN, "size="+size+", sizeMax="+sizeMax);
		if (sizeMax<MAX_CHAR_BUFFER_SIZE)
		{
			synchronized(stringLocker)
			{
				if (chars==null || chars.length<sizeMax)
					chars=new char[sizeMax];
				for (int i=0;i<size;i++)
					chars[i]=ois.readChar();
				return new String(chars, 0, size);
			}
		}
		else
		{
			char []chars=new char[sizeMax];
			for (int i=0;i<size;i++)
				chars[i]=ois.readChar();
			return new String(chars, 0, size);
			
		}
	}
	
	@SuppressWarnings("SameParameterValue")
	static void writeBytes(final SecuredObjectOutputStream oos, byte[] tab, int sizeMax, boolean supportNull) throws IOException
	{
		writeBytes(oos, tab, 0, tab==null?0:tab.length, sizeMax, supportNull);
	}
	@SuppressWarnings("SameParameterValue")
	static void writeBytes(final SecuredObjectOutputStream oos, byte[] tab, int off, int size, int sizeMax, boolean supportNull) throws IOException
	{
		if (sizeMax<0)
			throw new IllegalArgumentException();

		if (tab==null)
		{
			if (!supportNull)
				throw new IOException();
			if (sizeMax>Short.MAX_VALUE)
				oos.writeInt(-1);
			else
				oos.writeShort(-1);
			return;
			
		}
		if (size>sizeMax)
			throw new IOException();
		if (sizeMax>Short.MAX_VALUE)
			oos.writeInt(size);
		else
			oos.writeShort(size);
		oos.write(tab, off, size);
	}
	@SuppressWarnings("SameParameterValue")
	static void writeBytes2D(final SecuredObjectOutputStream oos, byte[][] tab, int sizeMax1, int sizeMax2, boolean supportNull1, boolean supportNull2) throws IOException
	{
		writeBytes2D(oos, tab, 0, tab==null?0:tab.length, sizeMax1, sizeMax2, supportNull1, supportNull2);
	}
	@SuppressWarnings("SameParameterValue")
	static void writeBytes2D(final SecuredObjectOutputStream oos, byte[][] tab, int off, int size, int sizeMax1, int sizeMax2, boolean supportNull1, boolean supportNull2) throws IOException
	{
		if (sizeMax1<0)
			throw new IllegalArgumentException();
		if (sizeMax2<0)
			throw new IllegalArgumentException();

		if (tab==null)
		{
			if (!supportNull1)
				throw new IOException();
			if (sizeMax1>Short.MAX_VALUE)
				oos.writeInt(-1);
			else
				oos.writeShort(-1);
			return;
			
		}
		if (size>sizeMax1)
			throw new IOException();
		if (sizeMax1>Short.MAX_VALUE)
			oos.writeInt(size);
		else
			oos.writeShort(size);
		for (int i=off;i<size;i++) {
			byte[] b=tab[i];
			SerializationTools.writeBytes(oos, b, 0, b==null?0:b.length, sizeMax2, supportNull2);
		}
	}
	@SuppressWarnings("SameParameterValue")
	static byte[][] readBytes2D(final SecuredObjectInputStream ois, int sizeMax1, int sizeMax2, boolean supportNull1, boolean supportNull2) throws IOException
	{
		if (sizeMax1<0)
			throw new IllegalArgumentException();
		if (sizeMax2<0)
			throw new IllegalArgumentException();


		int size;
		if (sizeMax1>Short.MAX_VALUE)
			size=ois.readInt();
		else
			size=ois.readShort();
		if (size==-1)
		{
			if (!supportNull1)
				throw new MessageExternalizationException(Integrity.FAIL_AND_CANDIDATE_TO_BAN);
			return null;
		}
		if (size<0 || size>sizeMax1)
			throw new MessageExternalizationException(Integrity.FAIL_AND_CANDIDATE_TO_BAN);
		
		byte [][]tab=new byte[size][];
		for (int i=0;i<size;i++)
			tab[i]=readBytes(ois, supportNull2, null, 0, sizeMax2);
		
		
		return tab;
		
	}
	@SuppressWarnings("SameParameterValue")
	static byte[] readBytes(final SecuredObjectInputStream ois, boolean supportNull, byte[] tab, int off, int sizeMax) throws IOException
	{
		if (sizeMax<0)
			throw new IllegalArgumentException();

		int size;
		if (sizeMax>Short.MAX_VALUE)
			size=ois.readInt();
		else
			size=ois.readShort();
		if (size==-1)
		{
			if (!supportNull)
				throw new MessageExternalizationException(Integrity.FAIL_AND_CANDIDATE_TO_BAN);
			return null;
		}
		if (size<0 || size>sizeMax)
			throw new MessageExternalizationException(Integrity.FAIL_AND_CANDIDATE_TO_BAN);
		if (tab==null) {
			tab = new byte[size];
			off = 0;
		}


		ois.readFully(tab, off, size);
		
		return tab;
		
	}
	
	public static final int MAX_KEY_SIZE=Short.MAX_VALUE;
	@SuppressWarnings("SameParameterValue")
	static void writeKey(final SecuredObjectOutputStream oos, Key key, boolean supportNull) throws IOException
	{
		if (supportNull)
			oos.writeBoolean(key!=null);

		if (key==null)
		{
			if (!supportNull)
				throw new IOException();

			return;
			
		}

		writeBytes(oos, key.encode(), MAX_KEY_SIZE, false);
	}

	@SuppressWarnings("SameParameterValue")
	static Key readKey(final SecuredObjectInputStream in, boolean supportNull) throws IOException
	{
		if (!supportNull || in.readBoolean())
		{
			byte[] k=readBytes(in, false, null, 0, MAX_KEY_SIZE);
			try
			{
				if (k == null)
					throw new MessageExternalizationException(Integrity.FAIL_AND_CANDIDATE_TO_BAN);
				return Key.decode(k);
			}
			catch(Exception e)
			{
				throw new MessageExternalizationException(Integrity.FAIL_AND_CANDIDATE_TO_BAN);
			}
		}
		else
		{
			return null;
		}
	}
	
	
	@SuppressWarnings("SameParameterValue")
	static void writeKeyPair(final SecuredObjectOutputStream oos, ASymmetricKeyPair keyPair, boolean supportNull) throws IOException
	{
		if (supportNull)
			oos.writeBoolean(keyPair!=null);

		if (keyPair==null)
		{
			if (!supportNull)
				throw new IOException();

			return;
			
		}

		
		writeBytes(oos, keyPair.encode(), MAX_KEY_SIZE*2, false);
	}

	@SuppressWarnings("SameParameterValue")
	static ASymmetricKeyPair readKeyPair(final SecuredObjectInputStream in, boolean supportNull) throws IOException
	{
		if (!supportNull || in.readBoolean())
		{
			byte[] k=readBytes(in, false, null, 0, MAX_KEY_SIZE*2);
			try
			{
				if (k==null)
					throw new MessageExternalizationException(Integrity.FAIL);
				return ASymmetricKeyPair.decode(k);
			}
			catch(Exception e)
			{
				throw new MessageExternalizationException(Integrity.FAIL_AND_CANDIDATE_TO_BAN);
			}
		}
		else
		{
			return null;
		}
	}
	@SuppressWarnings("SameParameterValue")
	static void writeObjects(final SecuredObjectOutputStream oos, Object[] tab, int sizeMax, boolean supportNull) throws IOException
	{
		if (sizeMax<0)
			throw new IllegalArgumentException();

		if (tab==null)
		{
			if (!supportNull)
				throw new IOException();
			if (sizeMax>Short.MAX_VALUE)
				oos.writeInt(-1);
			else
				oos.writeShort(-1);
			return;

		}
		if (tab.length>sizeMax)
			throw new IOException();
		if (sizeMax>Short.MAX_VALUE)
			oos.writeInt(tab.length);
		else
			oos.writeShort(tab.length);
		sizeMax-=tab.length;
		for (Object o : tab)
		{
			writeObject(oos, o, sizeMax, true);
		}
	}

	@SuppressWarnings("SameParameterValue")
	static Object[] readObjects(final SecuredObjectInputStream ois, int sizeMax, boolean supportNull) throws IOException, ClassNotFoundException
	{
		if (sizeMax<0)
			throw new IllegalArgumentException();

		int size;
		if (sizeMax>Short.MAX_VALUE)
			size=ois.readInt();
		else
			size=ois.readShort();
		if (size==-1)
		{
			if (!supportNull)
				throw new MessageExternalizationException(Integrity.FAIL_AND_CANDIDATE_TO_BAN);
			return null;
		}
		if (size<0 || size>sizeMax)
			throw new MessageExternalizationException(Integrity.FAIL_AND_CANDIDATE_TO_BAN);

		Object []tab=new Object[size];
		sizeMax-=tab.length;
		for (int i=0;i<size;i++)
		{
			tab[i]=readObject(ois, sizeMax, true);
		}

		return tab;

	}
	@SuppressWarnings("SameParameterValue")
	static ArrayList<Object> readListObjects(final SecuredObjectInputStream ois, int sizeMax, boolean supportNull) throws IOException, ClassNotFoundException
	{
		if (sizeMax<0)
			throw new IllegalArgumentException();

		int size;
		if (sizeMax>Short.MAX_VALUE)
			size=ois.readInt();
		else
			size=ois.readShort();
		if (size==-1)
		{
			if (!supportNull)
				throw new MessageExternalizationException(Integrity.FAIL_AND_CANDIDATE_TO_BAN);
			return null;
		}
		if (size<0 || size>sizeMax)
			throw new MessageExternalizationException(Integrity.FAIL_AND_CANDIDATE_TO_BAN);

		ArrayList<Object> tab=new ArrayList<>(size);
		sizeMax-=size;
		for (int i=0;i<size;i++)
		{
			tab.add(readObject(ois, sizeMax, true));
		}

		return tab;

	}
	/*public static void writeExternalizableAndSizables(final SecuredObjectOutputStream oos, ExternalizableAndSizable[] tab, int sizeMaxBytes, boolean supportNull) throws IOException
	{
		if (tab==null)
		{
			if (!supportNull)
				throw new IOException();
			oos.writeInt(-1);
			return;

		}
		if (tab.length*4>sizeMaxBytes)
			throw new IOException();
		oos.writeInt(tab.length);
		int total=4;

		for (ExternalizableAndSizable o : tab)
		{
			writeExternalizableAndSizable(oos, o, true);
			total+=o.getInternalSerializedSize();

			if (total>=sizeMaxBytes)
				throw new IOException();
		}
	}*/

	@SuppressWarnings("SameParameterValue")
	static void writeExternalizables(final SecuredObjectOutputStream objectOutput, SecureExternalizable[] tab, int sizeMaxBytes, boolean supportNull) throws IOException
	{
		if (sizeMaxBytes<0)
			throw new IllegalArgumentException();

		if (tab==null)
		{
			if (!supportNull)
				throw new IOException();
			objectOutput.writeInt(-1);
			return;

		}
		if (tab.length*4>sizeMaxBytes)
			throw new IOException();
		objectOutput.writeInt(tab.length);
		int total=4;

		for (SecureExternalizable o : tab)
		{
			writeExternalizable(objectOutput, o, true);
			total+=o==null?0:getInternalSize(o.getClass().getName(), MAX_CLASS_LENGTH);
			total+=o==null?1:o.getInternalSerializedSize();

			if (total>=sizeMaxBytes)
				throw new IOException();
		}
	}


	/*public static ExternalizableAndSizable[] readExternalizableAndSizables(final SecuredObjectInputStream ois, int sizeMaxBytes, boolean supportNull) throws IOException, ClassNotFoundException
	{
		int size=ois.readInt();
		if (size==-1)
		{
			if (!supportNull)
				throw new MessageExternalizationException(Integrity.FAIL_AND_CANDIDATE_TO_BAN);
			return null;
		}
		if (size<0 || size*4>sizeMaxBytes)
			throw new MessageExternalizationException(Integrity.FAIL_AND_CANDIDATE_TO_BAN);

		ExternalizableAndSizable []tab=new ExternalizableAndSizable[size];
		sizeMaxBytes-=4;
		for (int i=0;i<size;i++)
		{
			Externalizable o=readExternalizableAndSizable(ois, true);
			if (!(o instanceof ExternalizableAndSizable))
				throw new MessageExternalizationException(Integrity.FAIL_AND_CANDIDATE_TO_BAN);
			ExternalizableAndSizable s=(ExternalizableAndSizable)o;
			sizeMaxBytes-=s.getInternalSerializedSize();
			if (sizeMaxBytes<0)
				throw new MessageExternalizationException(Integrity.FAIL_AND_CANDIDATE_TO_BAN);
			tab[i]=s;
		}

		return tab;

	}*/

	@SuppressWarnings("SameParameterValue")
	static SecureExternalizable[] readExternalizables(final SecuredObjectInputStream ois, int sizeMaxBytes, boolean supportNull) throws IOException, ClassNotFoundException
	{
		if (sizeMaxBytes<0)
			throw new IllegalArgumentException();

		int size=ois.readInt();
		if (size==-1)
		{
			if (!supportNull)
				throw new MessageExternalizationException(Integrity.FAIL_AND_CANDIDATE_TO_BAN);
			return null;
		}
		if (size<0 || size*4>sizeMaxBytes)
			throw new MessageExternalizationException(Integrity.FAIL_AND_CANDIDATE_TO_BAN);

		SecureExternalizable []tab=new SecureExternalizable[size];
		sizeMaxBytes-=4;
		for (int i=0;i<size;i++)
		{
			SecureExternalizableWithoutInnerSizeControl s=readExternalizable(ois, true);
			if (s!=null) {
				if (!(s instanceof SecureExternalizable))
					throw new MessageExternalizationException(Integrity.FAIL_AND_CANDIDATE_TO_BAN);
				sizeMaxBytes -= ((SecureExternalizable)s).getInternalSerializedSize();
				if (sizeMaxBytes < 0)
					throw new MessageExternalizationException(Integrity.FAIL_AND_CANDIDATE_TO_BAN);
			}
			tab[i]=(SecureExternalizable)s;
		}

		return tab;

	}
	public static int MAX_URL_LENGTH=8000;
	@SuppressWarnings("SameParameterValue")
	static void writeInetAddress(final SecuredObjectOutputStream oos, InetAddress inetAddress, boolean supportNull) throws IOException
	{
		if (supportNull)
			oos.writeBoolean(inetAddress!=null);

		if (inetAddress==null)
		{
			if (!supportNull)
				throw new IOException();

			return;

		}

		writeBytes(oos, inetAddress.getAddress(), 20, false);
	}
	@SuppressWarnings("SameParameterValue")
	static void writeDate(final SecuredObjectOutputStream oos, Date date, boolean supportNull) throws IOException
	{
		if (supportNull)
			oos.writeBoolean(date!=null);

		if (date==null)
		{
			if (!supportNull)
				throw new IOException();

			return;

		}
		oos.writeLong(date.getTime());
	}

	@SuppressWarnings("SameParameterValue")
	static void writeDecentralizedID(final SecuredObjectOutputStream oos, AbstractDecentralizedID id, boolean supportNull) throws IOException
	{
		if (supportNull)
			oos.writeBoolean(id!=null);

		if (id==null)
		{
			if (!supportNull)
				throw new IOException();

			return;

		}

		writeBytes(oos, id.getBytes(), 513, false);
	}
	@SuppressWarnings("SameParameterValue")
	static AbstractDecentralizedID readDecentralizedID(final SecuredObjectInputStream in, boolean supportNull) throws IOException
	{
		if (!supportNull || in.readBoolean())
		{
			try
			{
				return AbstractDecentralizedID.instanceOf(Objects.requireNonNull(readBytes(in, false, null, 0, 513)));
			}
			catch(Exception e)
			{
				throw new MessageExternalizationException(Integrity.FAIL_AND_CANDIDATE_TO_BAN);
			}
		}
		else
			return null;
	}

	@SuppressWarnings("SameParameterValue")
	static InetAddress readInetAddress(final SecuredObjectInputStream ois, boolean supportNull) throws IOException {
		if (!supportNull || ois.readBoolean())
		{
			byte[] address=readBytes(ois, false, null, 0, 20);
			try
			{
				if (address==null)
					throw new MessageExternalizationException(Integrity.FAIL_AND_CANDIDATE_TO_BAN);

				return InetAddress.getByAddress(address);
			}
			catch(Exception e)
			{
				throw new MessageExternalizationException(Integrity.FAIL_AND_CANDIDATE_TO_BAN, e);
			}
		}
		else
			return null;

	}
	@SuppressWarnings("SameParameterValue")
	static Date readDate(final SecuredObjectInputStream ois, boolean supportNull) throws IOException {
		if (!supportNull || ois.readBoolean())
		{
			return new Date(ois.readLong());
		}
		else
			return null;

	}

	@SuppressWarnings("SameParameterValue")
	static void writeInetSocketAddress(final SecuredObjectOutputStream oos, InetSocketAddress inetSocketAddress, boolean supportNull) throws IOException
	{
		if (supportNull)
			oos.writeBoolean(inetSocketAddress!=null);

		if (inetSocketAddress==null)
		{
			if (!supportNull)
				throw new IOException();

			return;

		}

		oos.writeInt(inetSocketAddress.getPort());
		writeInetAddress(oos, inetSocketAddress.getAddress(), false);
	}


	@SuppressWarnings("SameParameterValue")
	static InetSocketAddress readInetSocketAddress(final SecuredObjectInputStream ois, boolean supportNull) throws IOException {
		if (!supportNull || ois.readBoolean())
		{
			int port=ois.readInt();
			InetAddress ia=readInetAddress(ois, false);

			try
			{
				return new InetSocketAddress(ia, port);
			}
			catch(Exception e)
			{
				throw new MessageExternalizationException(Integrity.FAIL_AND_CANDIDATE_TO_BAN, e);
			}
		}
		else
			return null;

	}
	@SuppressWarnings("SameParameterValue")
	static void writeEnum(final SecuredObjectOutputStream oos, Enum<?> e, boolean supportNull) throws IOException
	{
		if (supportNull)
			oos.writeBoolean(e!=null);
		if (e==null)
		{
			if (!supportNull)
				throw new IOException();

			return;

		}

		SerializationTools.writeString(oos, e.getClass().getName(), MAX_CLASS_LENGTH, false);
		SerializationTools.writeString(oos, e.name(), 1000, false);
	}
	public final static int MAX_CLASS_LENGTH=2048;
	@SuppressWarnings("SameParameterValue")
	static Enum<?> readEnum(final SecuredObjectInputStream ois, boolean supportNull) throws IOException, ClassNotFoundException
	{
		if (!supportNull || ois.readBoolean())
		{
			String clazz=SerializationTools.readString(ois, MAX_CLASS_LENGTH, false);
			String value=SerializationTools.readString(ois, 1000, false);
			@SuppressWarnings("rawtypes")
			Class c=Class.forName(clazz, false, MadkitClassLoader.getSystemClassLoader());
			if (!c.isEnum())
				throw new MessageExternalizationException(Integrity.FAIL_AND_CANDIDATE_TO_BAN);
			try
			{
				if (value==null)
					throw new MessageExternalizationException(Integrity.FAIL);
				return Enum.valueOf(c, value);
			}
			catch(ClassCastException e)
			{
				throw new MessageExternalizationException(Integrity.FAIL_AND_CANDIDATE_TO_BAN);
			}

		}
		else
			return null;

	}
	/*public static void writeExternalizableAndSizable(final SecuredObjectOutputStream oos, Externalizable e, boolean supportNull) throws IOException
	{
		if (e==null)
		{
			if (!supportNull)
				throw new IOException();
			oos.writeBoolean(false);
			return;

		}
		Class<?> clazz=e.getClass();
		if (!ExternalizableAndSizable.class.isAssignableFrom(clazz) && !WithoutInnerSizeControl.class.isAssignableFrom(clazz))
			throw new IOException();

		if (oos.getClass()==oosClazz)
		{
			try
			{
				e=(Externalizable)invoke(replaceObject, oos, e);
				if (e!=null)
					clazz=e.getClass();
			}
			catch(Exception e2)
			{
				throw new IOException(e2);
			}
		}
		oos.writeBoolean(true);
		SerializationTools.writeString(oos, clazz.getName(), MAX_CLASS_LENGTH, false);
		if (e==null)
			throw new MessageExternalizationException(Integrity.FAIL_AND_CANDIDATE_TO_BAN);
		e.writeExternal(oos);

	}*/

	@SuppressWarnings("SameParameterValue")
	static void writeClass(final SecuredObjectOutputStream objectOutput, Class<?> clazz, boolean supportNull, Class<?> rootClass) throws IOException {
		if (rootClass==null)
			rootClass=Object.class;
		if (supportNull)
			objectOutput.writeBoolean(clazz!=null);
		if (clazz!=null) {
			if (!rootClass.isAssignableFrom(clazz))
				throw new IOException();
			SerializationTools.writeString(objectOutput, clazz.getName(), MAX_CLASS_LENGTH, supportNull);
		}


	}
	private static void writeExternalizable(final SecuredObjectOutputStream objectOutput, SecureExternalizableWithoutInnerSizeControl e) throws IOException
	{
		if (e==null)
			throw new MessageExternalizationException(Integrity.FAIL_AND_CANDIDATE_TO_BAN);


		e.writeExternal(objectOutput);
	}
	static void writeExternalizable(final SecuredObjectOutputStream objectOutput, SecureExternalizableWithoutInnerSizeControl e, boolean supportNull) throws IOException
	{
		if (supportNull)
			objectOutput.writeBoolean(e!=null);
		if (e==null)
		{
			if (!supportNull)
				throw new IOException();

			return;

		}
		Class<?> clazz=e.getClass();
		if (objectOutput.objectOutput.getClass()==oosClazz)
		{
			try
			{
				Object o=invoke(replaceObject, objectOutput.objectOutput, e);
				if (o==null)
					throw new IOException();
				if (!SecureExternalizableWithoutInnerSizeControl.class.isAssignableFrom(o.getClass()))
					throw new IOException();
				e=(SecureExternalizableWithoutInnerSizeControl)o;
				clazz=e.getClass();
			}
			catch(Exception e2)
			{
				throw new IOException(e2);
			}
		}

		writeClass(objectOutput, clazz, false, SecureExternalizableWithoutInnerSizeControl.class);
		writeExternalizable(objectOutput, e);

	}
	private static final HashMap<Class<?>, Constructor<?>> constructors=new HashMap<>();

	private static Constructor<?> getDefaultConstructor(final Class<?> clazz) throws NoSuchMethodException, SecurityException
	{
		synchronized(constructors)
		{
			Constructor<?> c=constructors.get(clazz);
			if (c==null)
			{
				final Constructor<?> cons=clazz.getDeclaredConstructor();
				c=AccessController.doPrivileged(new PrivilegedAction<Constructor<?>>() {

					@Override
					public Constructor<?> run() {

						cons.setAccessible(true);
						return cons;
					}
				});
				constructors.put(clazz, c);
			}
			return c;
		}
	}
	private final static Class<?> oosClazz;
	private final static Class<?> oisClazz;
	private final static Method replaceObject;
	private final static Method resolveObject;




	static
	{

		oosClazz=loadClass("com.distrimind.madkit.kernel.network.DistantKernelAgent$OOS");
		oisClazz=loadClass("com.distrimind.madkit.kernel.network.DistantKernelAgent$OIS");
		resolveObject=getMethod(oisClazz, "resolveObject", Object.class);
		replaceObject=getMethod(oosClazz, "replaceObject", Object.class);

	}





	/*public static Externalizable readExternalizableAndSizable(final SecuredObjectInputStream ois, boolean supportNull) throws IOException, ClassNotFoundException
	{
		if (ois.readBoolean())
		{
			String clazz=SerializationTools.readString(ois, MAX_CLASS_LENGTH, false);




			try
			{
				Class<?> c;
				boolean isOIS=ois.getClass()==oisClazz;
				if (isOIS)
					c= ((FilteredObjectInputStream)ois).resolveClass(clazz);
				else
					c= Class.forName(clazz, false, MadkitClassLoader.getSystemClassLoader());
				if (!ExternalizableAndSizable.class.isAssignableFrom(c) && !WithoutInnerSizeControl.class.isAssignableFrom(c))
					throw new MessageExternalizationException(Integrity.FAIL_AND_CANDIDATE_TO_BAN);
				if (!isOIS)
					c= Class.forName(clazz, true, MadkitClassLoader.getSystemClassLoader());
				Constructor<?> cons=getDefaultConstructor(c);
				Externalizable res=(Externalizable)cons.newInstance();

				res.readExternal(ois);
				if (isOIS)
				{
					res=(Externalizable)invoke(resolveObject, ois, res);
				}
				return res;
			}
			catch(InvocationTargetException | NoSuchMethodException | IllegalAccessException | InstantiationException e)
			{
				throw new MessageExternalizationException(Integrity.FAIL_AND_CANDIDATE_TO_BAN, e);
			}

		}
		else if (!supportNull)
			throw new MessageExternalizationException(Integrity.FAIL_AND_CANDIDATE_TO_BAN);
		else
			return null;

	}*/

	@SuppressWarnings("unchecked")
	static <RT> Class<? extends RT> readClass(final SecuredObjectInputStream objectInput, boolean supportNull, Class<RT> rootClass) throws IOException, ClassNotFoundException {
		if (rootClass==null)
			throw new NullPointerException();

		if (!supportNull || objectInput.readBoolean())
		{
			String clazz=SerializationTools.readString(objectInput, MAX_CLASS_LENGTH, false);

			Class<?> c;
			boolean doubleCheck=rootClass!=Object.class;
			boolean isOIS=objectInput.objectInput.getClass()==oisClazz;
			if (isOIS)
				c= ((FilteredObjectInputStream)objectInput.objectInput).resolveClass(clazz);
			else
				c= Class.forName(clazz, !doubleCheck, MadkitClassLoader.getSystemClassLoader());
			if (doubleCheck) {
				if (!rootClass.isAssignableFrom(c))
					throw new MessageExternalizationException(Integrity.FAIL_AND_CANDIDATE_TO_BAN, "rootClass : "+rootClass+" ; class="+c);
				if (!isOIS)
					c = Class.forName(clazz, true, MadkitClassLoader.getSystemClassLoader());
			}
			return (Class<? extends RT>)c;
		}
		else
			return null;

	}
	private static SecureExternalizableWithoutInnerSizeControl readExternalizable(final SecuredObjectInputStream objectInput, Class<?> c) throws IOException, ClassNotFoundException
	{
		try
		{
			boolean isOIS=objectInput.objectInput.getClass()==oisClazz;
			Constructor<?> cons=getDefaultConstructor(c);
			SecureExternalizableWithoutInnerSizeControl res=(SecureExternalizableWithoutInnerSizeControl)cons.newInstance();
			res.readExternal(objectInput);

			if (isOIS)
			{
				Object o=invoke(resolveObject, objectInput.objectInput, res);
				if (o!=null && !SecureExternalizableWithoutInnerSizeControl.class.isAssignableFrom(o.getClass()))
					throw new MessageExternalizationException(Integrity.FAIL);

				res=(SecureExternalizableWithoutInnerSizeControl)o;
			}
			return res;
		}
		catch(InvocationTargetException | NoSuchMethodException | IllegalAccessException | InstantiationException e)
		{
			throw new MessageExternalizationException(Integrity.FAIL_AND_CANDIDATE_TO_BAN, e);
		}

	}
	static SecureExternalizableWithoutInnerSizeControl readExternalizable(final SecuredObjectInputStream objectInput, boolean supportNull) throws IOException, ClassNotFoundException
	{
		if (!supportNull || objectInput.readBoolean())
		{

			Class<?> c=readClass(objectInput, false, SecureExternalizableWithoutInnerSizeControl.class);
			return readExternalizable(objectInput, c);
		}
		else
			return null;

	}
	static void writeObject(final SecuredObjectOutputStream oos, Object o, int sizeMax, boolean supportNull) throws IOException
	{
		writeObject(oos, o, sizeMax, supportNull, true);
	}
	private static void writeObject(final SecuredObjectOutputStream oos, Object o, int sizeMax, boolean supportNull, boolean OOSreplaceObject) throws IOException
	{
		Byte id;

		if (o==null)
		{
			if (!supportNull)
				throw new IOException();
			
			oos.write(0);
		}
		else //noinspection SuspiciousMethodCalls
			if (o instanceof SecureExternalizableWithoutInnerSizeControl && (id=identifiersPerClasses.get(o.getClass()))!=null)
		{
			if (OOSreplaceObject && oos.objectOutput.getClass()==oosClazz)
			{
				try
				{
					o=invoke(replaceObject, oos.objectOutput, o);
					if (o==null)
						throw new IOException();
					if (!SecureExternalizableWithoutInnerSizeControl.class.isAssignableFrom(o.getClass()))
						throw new IOException();
					writeObject(oos,o,sizeMax, false, false);
					return;
				}
				catch(Exception e2)
				{
					throw new IOException(e2);
				}
			}

			oos.write(id);
			writeExternalizable(oos, (SecureExternalizableWithoutInnerSizeControl)o);
		}
		else //noinspection SuspiciousMethodCalls
			if (o instanceof Enum && (id=identifiersPerEnums.get(o.getClass()))!=null)
		{
			oos.write(id);
			oos.writeInt(((Enum<?>)o).ordinal());
		}
		else if (o instanceof SecureExternalizableWithoutInnerSizeControl)
		{
			oos.write(1);
			writeExternalizable(oos, (SecureExternalizableWithoutInnerSizeControl) o, false);
		}
		else if (o instanceof String)
		{
			oos.write(2);
			writeString(oos, (String)o, sizeMax, false);
		}
		else if (o instanceof byte[])
		{
			oos.write(3);
			writeBytes(oos, (byte[])o, sizeMax, false);
		}
		else if (o instanceof byte[][])
		{
			oos.write(4);
			writeBytes2D(oos, (byte[][])o, sizeMax, sizeMax, false, false);
		}
		else if (o instanceof SecureExternalizable[])
		{
			oos.write(5);
			writeExternalizables(oos, (SecureExternalizable[])o, sizeMax, false);
		}
		else if (o instanceof Object[])
		{
			oos.write(6);
			writeObjects(oos, (Object[])o, sizeMax, false);
		}
		else if (o instanceof InetSocketAddress)
		{
			oos.write(7);
			writeInetSocketAddress(oos, (InetSocketAddress)o, false);
		}
		else if (o instanceof InetAddress)
		{
			oos.write(8);
			writeInetAddress(oos, (InetAddress)o, false);
		}
		else if (o instanceof AbstractDecentralizedID)
		{
			oos.write(9);
			writeDecentralizedID(oos, (AbstractDecentralizedID)o, false);
		}
		else if (o instanceof Key)
		{
			oos.write(10);
			writeKey(oos, (Key)o, false);
		}
		else if (o instanceof ASymmetricKeyPair)
		{
			oos.write(11);
			writeKeyPair(oos, (ASymmetricKeyPair)o, false);
		}
		else if (o instanceof Enum<?>)
		{
			oos.write(12);
			writeEnum(oos, (Enum<?>)o, false);
		}
		else if (o instanceof Collection)
		{
			oos.write(13);
			Collection<?> c=(Collection<?>)o;
			Object[] tab=new Object[c.size()];
			int i=0;
			for (Object r : c)
				tab[i++]=r;
			writeObjects(oos, tab, sizeMax, false);
		}
		else if (o instanceof Class)
		{
			oos.write(14);
			writeClass(oos, (Class<?>)o, false, Object.class);
		}
		else if (o instanceof Date)
		{
			oos.write(15);
			writeDate(oos, (Date)o, false);
		}
		else
		{
			throw new IOException();
			/*oos.write(Byte.MAX_VALUE);
			oos.writeObject(o);*/
		}
	}
	
	static Object readObject(final SecuredObjectInputStream ois, int sizeMax, boolean supportNull) throws IOException, ClassNotFoundException
	{
		byte type=ois.readByte();
		if (type>=classesStartIndex)
		{
			if (type<=classesEndIndex)
			{
				Class<?> c=classes.get(type-classesStartIndex);

				return readExternalizable(ois, c);
			}
			else if (type<=enumsEndIndex)
			{
				int ordinal=ois.readInt();
				Class<? extends Enum<?>> c=enums.get(type-enumsStartIndex);
				for (Enum<?> e : c.getEnumConstants())
				{
					if (e.ordinal()==ordinal)
						return e;
				}
				throw new MessageExternalizationException(Integrity.FAIL);
			}
			else
				throw new MessageExternalizationException(Integrity.FAIL);
		}
		else {
			switch (type) {
				case 0:
					if (!supportNull)
						throw new MessageExternalizationException(Integrity.FAIL_AND_CANDIDATE_TO_BAN);
					return null;
				case 1:
					return readExternalizable(ois, false);
				case 2:
					return readString(ois, sizeMax, false);
				case 3:
					return readBytes(ois, false, null, 0, sizeMax);
				case 4:
					return readBytes2D(ois, sizeMax, sizeMax, false, false);
				case 5:
					return readExternalizables(ois, sizeMax, false);
				case 6:
					return readObjects(ois, sizeMax, false);
				case 7:
					return readInetSocketAddress(ois, false);
				case 8:
					return readInetAddress(ois, false);
				case 9:
					return readDecentralizedID(ois, false);
				case 10:
					return readKey(ois, false);
				case 11:
					return readKeyPair(ois, false);
				case 12:
					return readEnum(ois, false);
				case 13:
					return readListObjects(ois, sizeMax, false);
				case 14:
					return readClass(ois, false, Object.class);
				case 15:
					return readDate(ois, false);
		/*case Byte.MAX_VALUE:
			return ois.readObject();*/
				default:
					throw new MessageExternalizationException(Integrity.FAIL);
			}
		}
		
	}

	private static final byte lastObjectCode=15;
	private static final byte classesStartIndex=lastObjectCode+1;
	private static byte classesEndIndex=0;
	private static byte enumsStartIndex=0;
	private static byte enumsEndIndex=0;
	private static final ArrayList<Class<? extends SecureExternalizableWithoutInnerSizeControl>> classes=new ArrayList<>();
	private static final Map<Class<? extends SecureExternalizableWithoutInnerSizeControl>, Byte> identifiersPerClasses=new HashMap<>();
	private static final ArrayList<Class<? extends Enum<?>>> enums=new ArrayList<>();
	private static final Map<Class<? extends Enum<?>>, Byte> identifiersPerEnums=new HashMap<>();
	static
	{

		try {
			int currentID=lastObjectCode;
			//noinspection unchecked
			classes.addAll(new HashSet<>(Arrays.asList(
					KernelAddress.class, KernelAddressInterfaced.class, AgentAddress.class, ConversationID.class, MultiGroup.class, Group.class, DoubleIP.class, MultipleIP.class,
					HostIP.class, (Class<? extends SecureExternalizableWithoutInnerSizeControl>)Class.forName("com.distrimind.madkit.kernel.network.AcceptedGroups"),
					(Class<? extends SecureExternalizableWithoutInnerSizeControl>)Class.forName("com.distrimind.madkit.kernel.network.BroadcastLanMessage"),
					(Class<? extends SecureExternalizableWithoutInnerSizeControl>)Class.forName("com.distrimind.madkit.kernel.network.LocalLanMessage"),
					(Class<? extends SecureExternalizableWithoutInnerSizeControl>)Class.forName("com.distrimind.madkit.kernel.network.TransferClosedSystemMessage"),
					(Class<? extends SecureExternalizableWithoutInnerSizeControl>)Class.forName("com.distrimind.madkit.kernel.network.TransferConfirmationSystemMessage"),
					(Class<? extends SecureExternalizableWithoutInnerSizeControl>)Class.forName("com.distrimind.madkit.kernel.network.TransferImpossibleSystemMessage"),
					(Class<? extends SecureExternalizableWithoutInnerSizeControl>)Class.forName("com.distrimind.madkit.kernel.network.TransferImpossibleSystemMessageFromMiddlePeer"),
					(Class<? extends SecureExternalizableWithoutInnerSizeControl>)Class.forName("com.distrimind.madkit.kernel.network.TransferBlockCheckerSystemMessage"),
					(Class<? extends SecureExternalizableWithoutInnerSizeControl>)Class.forName("com.distrimind.madkit.kernel.network.TransferPropositionSystemMessage"),
					(Class<? extends SecureExternalizableWithoutInnerSizeControl>)Class.forName("com.distrimind.madkit.kernel.network.TransferAgent$IDTransfer"),
					(Class<? extends SecureExternalizableWithoutInnerSizeControl>)Class.forName("com.distrimind.madkit.kernel.network.TransferAgent$DirectConnection"),
					(Class<? extends SecureExternalizableWithoutInnerSizeControl>)Class.forName("com.distrimind.madkit.kernel.network.TransferAgent$DirectConnectionFailed"),
					(Class<? extends SecureExternalizableWithoutInnerSizeControl>)Class.forName("com.distrimind.madkit.kernel.network.TransferAgent$DirectConnectionSuceeded"),
					(Class<? extends SecureExternalizableWithoutInnerSizeControl>)Class.forName("com.distrimind.madkit.kernel.network.TransferAgent$TryDirectConnection"),
					BigDataTransferID.class,
					(Class<? extends SecureExternalizableWithoutInnerSizeControl>)Class.forName("com.distrimind.madkit.kernel.network.CGRSynchrosSystemMessage"),
					(Class<? extends SecureExternalizableWithoutInnerSizeControl>)Class.forName("com.distrimind.madkit.kernel.network.CGRSynchroSystemMessage"),
					CGRSynchro.class,
					(Class<? extends SecureExternalizableWithoutInnerSizeControl>)Class.forName("com.distrimind.madkit.kernel.network.ConnectionInfoSystemMessage"),
					(Class<? extends SecureExternalizableWithoutInnerSizeControl>)Class.forName("com.distrimind.madkit.kernel.network.DataToBroadcast"),
					DistantKernelAddressValidated.class,
					(Class<? extends SecureExternalizableWithoutInnerSizeControl>)Class.forName("com.distrimind.madkit.kernel.network.connection.access.IdentifiersPropositionMessage"),
					Identifier.class, EncryptedIdentifier.class,
					(Class<? extends SecureExternalizableWithoutInnerSizeControl>)Class.forName("com.distrimind.madkit.kernel.network.connection.access.JPakeMessage"),
					(Class<? extends SecureExternalizableWithoutInnerSizeControl>)Class.forName("com.distrimind.madkit.kernel.network.connection.secured.KeyAgreementDataMessage"),
					(Class<? extends SecureExternalizableWithoutInnerSizeControl>)Class.forName("com.distrimind.madkit.kernel.network.connection.access.LoginConfirmationMessage"),
					(Class<? extends SecureExternalizableWithoutInnerSizeControl>)Class.forName("com.distrimind.madkit.kernel.network.connection.access.NewLocalLoginAddedMessage"),
					(Class<? extends SecureExternalizableWithoutInnerSizeControl>)Class.forName("com.distrimind.madkit.kernel.network.connection.access.NewLocalLoginRemovedMessage"),
					(Class<? extends SecureExternalizableWithoutInnerSizeControl>)Class.forName("com.distrimind.madkit.kernel.network.PingMessage"),
					(Class<? extends SecureExternalizableWithoutInnerSizeControl>)Class.forName("com.distrimind.madkit.kernel.network.PongMessage"),
					PointToPointTransferedBlockChecker.class,
					(Class<? extends SecureExternalizableWithoutInnerSizeControl>)Class.forName("com.distrimind.madkit.kernel.network.ValidateBigDataProposition"),
					EnumMessage.class,
					NetworkObjectMessage.class,TaskID.class,
					ACLMessage.class, ActMessage.class,BigDataPropositionMessage.class,BigDataResultMessage.class,
					KernelMessage.class, StringMessage.class, (Class<? extends SecureExternalizableWithoutInnerSizeControl>)Class.forName("com.distrimind.madkit.message.hook.OrganizationEvent"),
					SchedulingMessage.class, KQMLMessage.class, IntegerMessage.class, BooleanMessage.class,
					GUIMessage.class, (Class<? extends SecureExternalizableWithoutInnerSizeControl>)Class.forName("com.distrimind.madkit.kernel.InternalRole"),
					EncryptedPassword.class, GUIMessage.class, (Class<? extends SecureExternalizableWithoutInnerSizeControl>)Class.forName("com.distrimind.madkit.kernel.network.DatagramLocalNetworkPresenceMessage"),
					EncryptedCloudIdentifier.class)));
			for (Class<?> c : classes)
				assert !Modifier.isAbstract(c.getModifiers()):""+c;
			assert currentID+classes.size()<255;
			for (Class<? extends SecureExternalizableWithoutInnerSizeControl> c : classes)
			{
				byte id=(byte)(++currentID);
				identifiersPerClasses.put(c, id);
			}
			classesEndIndex=(byte)currentID;

			//noinspection unchecked
			enums.addAll(new HashSet<>(Arrays.asList(
					AbstractAgent.ReturnCode.class,
					AbstractAgent.State.class,
					(Class<? extends Enum<?>>)Class.forName("com.distrimind.madkit.kernel.CGRSynchro$Code"),
					ConnectionProtocol.ConnectionState.class,
					MessageDigestType.class,
					SecureRandomType.class,
					SymmetricEncryptionType.class,
					SymmetricAuthentifiedSignatureType.class,
					ASymmetricEncryptionType.class,
					ASymmetricAuthentifiedSignatureType.class,
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
					(Class<? extends Enum<?>>)Class.forName("com.distrimind.madkit.kernel.MultiGroup$CONTAINS"),
					(Class<? extends Enum<?>>)Class.forName("com.distrimind.madkit.kernel.NetCode"),
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
			assert currentID+enums.size()<255;
			enumsStartIndex=(byte)(currentID+1);
			for (Class<? extends Enum<?>> c : enums)
			{
				byte id=(byte)(++currentID);
				identifiersPerEnums.put(c, id);
			}
			enumsEndIndex=(byte)currentID;

		} catch (ClassNotFoundException e) {
			e.printStackTrace();
			System.exit(-1);
		}
	}


	public static int getInternalSize(Key key)
	{
		if (key==null)
			return 1;
		return getInternalSize(key, 0);
	}
	public static int getInternalSize(ASymmetricKeyPair keyPair)
	{
		if (keyPair==null)
			return 1;
		return getInternalSize(keyPair, 0);
	}
	public static int getInternalSize(InetAddress inetAddress)
	{
		if (inetAddress==null)
			return 1;
		return getInternalSize(inetAddress, 0);
	}
	public static int getInternalSize(InetSocketAddress inetSocketAddress)
	{
		if (inetSocketAddress==null)
			return 1;
		return getInternalSize(inetSocketAddress, 0);
	}
	public static int getInternalSize(AbstractDecentralizedID abstractDecentralizedID)
	{
		if (abstractDecentralizedID==null)
			return 1;
		return getInternalSize(abstractDecentralizedID, 0);
	}
	public static int getInternalSize(SecureExternalizable secureExternalizable)
	{
		if (secureExternalizable==null)
			return 1;

		return getInternalSize(secureExternalizable, 0);
	}
	public static int getInternalSize(Enum<?> e)
	{
		if (e==null)
			return 1;
		return getInternalSize(e, 0);
	}
	public static int getInternalSize(byte[] array, int maxSizeInBytes)
	{
		if (array==null)
			return maxSizeInBytes>Short.MAX_VALUE?4:2;
		return getInternalSize((Object)array, maxSizeInBytes);
	}
	public static int getInternalSize(Object[] array, int maxSizeInBytes)
	{
		if (array==null)
			return maxSizeInBytes>Short.MAX_VALUE?4:2;
		return getInternalSize((Object)array, maxSizeInBytes);
	}
	public static int getInternalSize(String text, int maxSizeInBytes)
	{
		if (text==null)
			return maxSizeInBytes>Short.MAX_VALUE?4:2;
		return getInternalSize((Object)text, maxSizeInBytes);
	}
	public static int getInternalSize(Collection<Object> array, int maxSizeInBytes)
	{
		if (array==null)
			return maxSizeInBytes>Short.MAX_VALUE?4:2;
		return getInternalSize((Object)array, maxSizeInBytes);
	}
	public static int getInternalSize(byte[][] array, int maxSizeInBytes1, int maxSizeInBytes2)
	{
		int res=maxSizeInBytes1>Short.MAX_VALUE?4:2;
		for (byte[] b : array)
			res+=(maxSizeInBytes2>Short.MAX_VALUE?4:2)+(b==null?0:b.length);
		return res;
	}


	public static int getInternalSize(Object o, int sizeMax)
	{
		if (o ==null)
			return 0;
		if (o instanceof String)
		{
			return ((String)o).length()*2+sizeMax>Short.MAX_VALUE?5:3;
		}
		else if (o instanceof byte[])
		{
			return ((byte[])o).length+sizeMax>Short.MAX_VALUE?5:3;
		}
		else if (o instanceof Key)
		{
			return 4+((Key)o).encode().length;
		}
		else if (o instanceof ASymmetricKeyPair)
		{
			return 4+((ASymmetricKeyPair)o).encode().length;
		}
		else if (o instanceof byte[][])
		{
			byte[][] tab = ((byte[][]) o);
			int res=sizeMax>Short.MAX_VALUE?5:3;
			for (byte[] b : tab) {
				res += sizeMax>Short.MAX_VALUE?5:3 + (b == null ? 0 : b.length);
			}
			return res;
		}
		else if (o instanceof SecureExternalizable)
		{
			return ((SecureExternalizable)o).getInternalSerializedSize()+getInternalSize(o.getClass().getName(), MAX_CLASS_LENGTH);
		}
		else if (o instanceof SecureExternalizable[])
		{
			int size=sizeMax>Short.MAX_VALUE?4:2;
			for (SecureExternalizable s : (SecureExternalizable[])o) {
				size+=s==null?0:getInternalSize(s.getClass().getName(), MAX_CLASS_LENGTH);
				size += s==null?1:s.getInternalSerializedSize();
			}
			return size;
		}
		else if (o instanceof Object[])
		{
			Object[] tab = (Object[]) o;
			int size=sizeMax>Short.MAX_VALUE?5:3;
			for (Object so : tab)
			{
				size+=getInternalSize(so, sizeMax-size);
			}
			return size;
		}
		else if (o instanceof Collection)
		{
			Collection<?> c=(Collection<?>)o;
			int size=sizeMax>Short.MAX_VALUE?5:3;

			for (Object so : c)
			{
				size+=getInternalSize(so, sizeMax-size);
			}
			return size;
		}
		else if (o instanceof InetAddress)
		{
			return ((InetAddress)o).getAddress().length+4;
		}
		else if (o instanceof InetSocketAddress)
		{
			return ((InetSocketAddress)o).getAddress().getAddress().length+8;
		}
		else if (o instanceof AbstractDecentralizedID)
		{
			return ((AbstractDecentralizedID) o).getBytes().length+3;
		}
		else if (o instanceof Enum<?>)
		{
			return 6+((Enum<?>)o).name().length()*2+getInternalSize(o.getClass().getName(), MAX_CLASS_LENGTH);
		}
		else
			return ObjectSizer.sizeOf(o);
	}
	
}
