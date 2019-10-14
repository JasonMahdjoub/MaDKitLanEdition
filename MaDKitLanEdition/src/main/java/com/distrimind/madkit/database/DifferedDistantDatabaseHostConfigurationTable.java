package com.distrimind.madkit.database;
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

import com.distrimind.ood.database.DatabaseRecord;
import com.distrimind.ood.database.Table;
import com.distrimind.ood.database.annotations.Field;
import com.distrimind.ood.database.annotations.NotNull;
import com.distrimind.ood.database.annotations.PrimaryKey;
import com.distrimind.ood.database.exceptions.DatabaseException;
import com.distrimind.util.DecentralizedValue;
import com.distrimind.util.io.RandomByteArrayInputStream;
import com.distrimind.util.io.RandomByteArrayOutputStream;

import java.io.IOException;

/**
 * @author Jason Mahdjoub
 * @version 1.0
 * @since MaDKitLanEdition 2.0.0
 */
public final class DifferedDistantDatabaseHostConfigurationTable extends Table<DifferedDistantDatabaseHostConfigurationTable.Record> {

	protected DifferedDistantDatabaseHostConfigurationTable() throws DatabaseException {
	}

	public static class Record extends DatabaseRecord
	{
		@Field
		@NotNull
		@PrimaryKey
		private DecentralizedValue hostIdentifier;
		@Field
		private boolean conflictualRecordsReplacedByDistantRecords;
		@Field(limit = 1000000)
		@NotNull
		private byte[] packages;

		private transient volatile Package[] _packages;
		protected Record()
		{
			_packages=null;
		}

		public Record(DecentralizedValue hostIdentifier, boolean conflictualRecordsReplacedByDistantRecords, Package[] packages) throws IOException {
			this.hostIdentifier = hostIdentifier;
			this.conflictualRecordsReplacedByDistantRecords = conflictualRecordsReplacedByDistantRecords;
			RandomByteArrayOutputStream baos=new RandomByteArrayOutputStream();
			baos.writeUnsignedShortInt(packages.length);
			for (Package p : packages)
			{
				baos.writeString(p.getName(), false, Short.MAX_VALUE);
			}
			baos.flush();
			this.packages = baos.getBytes();
			this._packages=packages;
		}

		public DecentralizedValue getHostIdentifier() {
			return hostIdentifier;
		}

		public boolean isConflictualRecordsReplacedByDistantRecords() {
			return conflictualRecordsReplacedByDistantRecords;
		}



		public Package[] getPackages() throws IOException {
			if (_packages==null)
			{
				synchronized (this)
				{
					if (_packages==null)
					{
						RandomByteArrayInputStream in=new RandomByteArrayInputStream(packages);
						int s=in.readUnsignedShortInt();
						_packages=new Package[s];
						for (int i=0;i<s;i++)
						{
							String p=in.readString(false, Short.MAX_VALUE);
							_packages[i]=Package.getPackage(p);
						}
					}
				}
			}


			return _packages;
		}
	}

	public void differDistantDatabaseHostConfiguration(DecentralizedValue hostIdentifier, boolean conflictualRecordsReplacedByDistantRecords, Package[] packages) throws DatabaseException, IOException {
		Record r=getDifferedDistantDatabaseHostConfiguration(hostIdentifier);
		if (r==null)
		{
			r=new Record(hostIdentifier, conflictualRecordsReplacedByDistantRecords, packages);
			addRecord(r);
		}
		else
		{
			r=new Record(hostIdentifier, conflictualRecordsReplacedByDistantRecords, packages);
			updateRecord(r);
		}
	}

	public Record getDifferedDistantDatabaseHostConfiguration(DecentralizedValue hostIdentifier) throws DatabaseException {
		return getRecord("hostIdentifier", hostIdentifier);
	}
}
