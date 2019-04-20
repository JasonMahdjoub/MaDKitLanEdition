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

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

/**
 * @author Jason Mahdjoub
 * @version 1.1
 * @since MaDKitLanEdition 1.11.0
 */
public interface SecureExternalizableSystemMessage {
	/**
	 * The object implements the writeExternal method to save its contents
	 * by calling the methods of DataOutput for its primitive values or
	 * calling the writeObject method of ObjectOutput for objects, strings,
	 * and arrays.
	 *
	 * @serialData Overriding methods should use this tag to describe
	 *             the data layout of this Externalizable object.
	 *             List the sequence of element types and, if possible,
	 *             relate the element to a public/protected field and/or
	 *             method of this Externalizable class.
	 *
	 * @param out the stream to write the object to
	 * @exception IOException Includes any I/O exceptions that may occur
	 */
	void writeExternal(SecuredObjectOutputStream out) throws IOException;

	/**
	 * The object implements the readExternal method to restore its
	 * contents by calling the methods of DataInput for primitive
	 * types and readObject for objects, strings and arrays.  The
	 * readExternal method must read the values in the same sequence
	 * and with the same types as were written by writeExternal.
	 *
	 * @param in the stream to read data from in order to restore the object
	 * @exception IOException if I/O errors occur
	 * @exception ClassNotFoundException If the class for an object being
	 *              restored cannot be found.
	 */
	void readExternal(SecuredObjectInputStream in) throws IOException, ClassNotFoundException;
}
