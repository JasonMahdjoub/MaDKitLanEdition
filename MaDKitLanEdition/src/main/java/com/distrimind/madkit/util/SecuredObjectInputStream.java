package com.distrimind.madkit.util;
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

import com.distrimind.madkit.exceptions.MessageSerializationException;
import com.distrimind.madkit.kernel.network.SystemMessage;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * @author Jason Mahdjoub
 * @version 1.0
 * @since MaDKitLanEdition 1.11.0
 */
public class SecuredObjectInputStream extends InputStream  {

	DataInputStream objectInput;

	public SecuredObjectInputStream(DataInputStream objectInput) {
		this.objectInput = objectInput;
	}

	@Override
	public int read() throws IOException {
		return objectInput.read();
	}
	@Override
	public long skip(long n) throws IOException {
		return objectInput.skip(n);
	}
	@Override
	public int available() throws IOException {
		return objectInput.available();
	}

	public int skipBytes(int n) throws IOException {
		return objectInput.skipBytes(n);
	}

	public boolean readBoolean() throws IOException {
		return objectInput.readBoolean();
	}

	public byte readByte() throws IOException {
		return objectInput.readByte();
	}

	public int readUnsignedByte() throws IOException {
		return objectInput.readUnsignedByte();
	}

	public short readShort() throws IOException {
		return objectInput.readShort();
	}

	public int readUnsignedShort() throws IOException {
		return objectInput.readUnsignedShort();
	}

	public char readChar() throws IOException {
		return objectInput.readChar();
	}

	public int readInt() throws IOException {
		return objectInput.readInt();
	}

	public long readLong() throws IOException {
		return objectInput.readLong();
	}

	public float readFloat() throws IOException {
		return objectInput.readFloat();
	}

	public double readDouble() throws IOException {
		return objectInput.readDouble();
	}
	@Override
	public int read(byte[] b) throws IOException {
		return objectInput.read(b);
	}
	@Override
	public int read(byte[] b, int off, int len) throws IOException {
		return objectInput.read(b, off, len);
	}

	public void readFully(byte[] b) throws IOException {
		objectInput.readFully(b);
	}

	public void readFully(byte[] b, int off, int len) throws IOException {
		objectInput.readFully(b, off, len);
	}


	@Override
	public void close() throws IOException {
		objectInput.close();
	}

	public byte[][] read2DBytesArray(boolean nullAcceptedForLevel1, boolean nullAcceptedForLevel2, int maxLevel1SizeInByte, int maxLevel2SizeInByte) throws IOException {
		return SerializationTools.readBytes2D(this, maxLevel1SizeInByte, maxLevel2SizeInByte, nullAcceptedForLevel1, nullAcceptedForLevel2);
	}

	public byte[] readBytesArray(boolean nullAccepted, int maxSizeInBytes) throws IOException {
		return SerializationTools.readBytes(this, nullAccepted, null, 0, maxSizeInBytes);
	}
	public byte[] readBytesArray(byte[] array, boolean nullAccepted) throws IOException {
		return SerializationTools.readBytes(this, nullAccepted, array, 0, array.length);
	}

	public byte[] readBytesArray(byte[] array, int offset, int len, boolean nullAccepted) throws IOException {
		return SerializationTools.readBytes(this, nullAccepted, array, offset, len);
	}

	public String readString(boolean nullAccepted, int maxSizeInBytes) throws IOException {
		return SerializationTools.readString(this, maxSizeInBytes, nullAccepted);
	}

	public Object readObject(boolean nullAccepted) throws IOException, ClassNotFoundException {
		return readObject(nullAccepted, -1);
	}
	public Object readObject(boolean nullAccepted, int maxSizeInBytes) throws IOException, ClassNotFoundException {
		return SerializationTools.readObject(this, maxSizeInBytes, nullAccepted);
	}
	public <TK> TK readObject(boolean nullAccepted, Class<TK> classType) throws IOException, ClassNotFoundException {
		return readObject(nullAccepted, -1, classType);
	}
	@SuppressWarnings("unchecked")
	public <TK> TK readObject(boolean nullAccepted, int maxSizeInBytes, Class<TK> classType) throws IOException, ClassNotFoundException {
		if (classType==null)
			throw new NullPointerException();
		Object e=readObject(nullAccepted, maxSizeInBytes);
		if (e==null)
		{
			return null;
		}
		if (!classType.isAssignableFrom(e.getClass()))
			throw new MessageSerializationException(SystemMessage.Integrity.FAIL_AND_CANDIDATE_TO_BAN);

		return (TK)e;
	}
	public Class<?> readClass(boolean nullAccepted) throws IOException, ClassNotFoundException {
		return readClass(nullAccepted, Object.class);
	}
	public <CR> Class<? extends CR> readClass(boolean nullAccepted, Class<CR> rootClass) throws IOException, ClassNotFoundException {
		return SerializationTools.readClass(this, nullAccepted, rootClass);
	}


}
