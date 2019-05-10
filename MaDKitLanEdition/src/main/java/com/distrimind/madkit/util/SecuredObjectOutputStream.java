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

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * @author Jason Mahdjoub
 * @version 1.1
 * @since MaDKitLanEdition 1.11.0
 */
public class SecuredObjectOutputStream extends OutputStream {
	DataOutputStream objectOutput;

	public SecuredObjectOutputStream(OutputStream objectOutput) {
		if (objectOutput==null)
			throw new NullPointerException();
		if (objectOutput instanceof DataOutputStream)
			this.objectOutput = (DataOutputStream)objectOutput;
		else
			this.objectOutput = new DataOutputStream(objectOutput);
	}
	@Override
	public void write(int b) throws IOException {
		objectOutput.write(b);
	}

	@Override
	public void write(byte[] b) throws IOException {
		objectOutput.write(b);
	}

	@Override
	public void write(byte[] b, int off, int len) throws IOException {
		objectOutput.write(b, off, len);
	}

	@Override
	public void flush() throws IOException {
		objectOutput.flush();
	}

	@Override
	public void close() throws IOException {
		objectOutput.close();
	}

	public void writeBoolean(boolean v) throws IOException {
		objectOutput.writeBoolean(v);
	}

	public void writeByte(int v) throws IOException {
		objectOutput.writeByte(v);
	}

	public void writeShort(int v) throws IOException {
		objectOutput.writeShort(v);
	}

	public void writeUnsignedShort(int v) throws IOException {
		objectOutput.writeShort(v);
	}

	public void writeUnsignedShortInt(int v) throws IOException {
		write((v >>> 16) & 0xFF);
		write((v >>> 8) & 0xFF);
		write((v) & 0xFF);
	}

	public void writeChar(int v) throws IOException {
		objectOutput.writeChar(v);
	}

	public void writeInt(int v) throws IOException {
		objectOutput.writeInt(v);
	}

	public void writeLong(long v) throws IOException {
		objectOutput.writeLong(v);
	}

	public void writeFloat(float v) throws IOException {
		objectOutput.writeFloat(v);
	}

	public void writeDouble(double v) throws IOException {
		objectOutput.writeDouble(v);
	}

	/**
	 * Do not write string size
	 * @param s the chars to write
	 * @throws IOException if a problem occurs
	 */
	public void writeChars(String s) throws IOException {
		objectOutput.writeChars(s);
	}

	public void writeString(String s, boolean nullAccepted, int maxSizeInBytes) throws IOException {
		SerializationTools.writeString(this, s, maxSizeInBytes, nullAccepted);
	}
	public void write2DBytesArray(byte[][] array, boolean nullAcceptedForLevel1, boolean nullAcceptedForLevel2, int maxLevel1SizeInByte, int maxLevel2SizeInByte) throws IOException {
		SerializationTools.writeBytes2D(this, array, maxLevel1SizeInByte, maxLevel2SizeInByte, nullAcceptedForLevel1, nullAcceptedForLevel2);
	}
	public void write2DBytesArray(byte[][] array, int offset, int len, boolean nullAcceptedForLevel1, boolean nullAcceptedForLevel2, int maxLevel1SizeInByte, int maxLevel2SizeInByte) throws IOException {
		SerializationTools.writeBytes2D(this, array, offset, len, maxLevel1SizeInByte, maxLevel2SizeInByte, nullAcceptedForLevel1, nullAcceptedForLevel2);
	}

	public void writeBytesArray(byte[] array, int offset, int len, boolean nullAccepted, int maxSizeInBytes) throws IOException {
		SerializationTools.writeBytes(this, array, offset, len, maxSizeInBytes, nullAccepted);
	}
	public void writeBytesArray(byte[] array, boolean nullAccepted, int maxSizeInBytes) throws IOException {
		SerializationTools.writeBytes(this, array, maxSizeInBytes, nullAccepted);
	}


	public void writeObject(Object object, boolean nullAccepted) throws IOException {
		writeObject(object, nullAccepted, -1);
	}
	public void writeObject(Object object, boolean nullAccepted, int maxSizeInBytes) throws IOException {
		SerializationTools.writeObject(this, object, maxSizeInBytes, nullAccepted);
	}
	public void writeClass(Class<?> clazz, boolean nullAccepted) throws IOException {
		writeClass(clazz, nullAccepted, Object.class);
	}
	public <CR> void writeClass(Class<? extends CR> clazz, boolean nullAccepted, Class<CR> rootClass) throws IOException {
		SerializationTools.writeClass(this, clazz, nullAccepted, rootClass);
	}
}
