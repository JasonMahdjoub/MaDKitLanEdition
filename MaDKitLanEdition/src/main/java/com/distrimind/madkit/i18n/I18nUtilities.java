/*
 * Copyright or © or Copr. Fabien Michel, Olivier Gutknecht, Jacques Ferber (1997)
 * 
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
package com.distrimind.madkit.i18n;

import com.distrimind.madkit.kernel.AbstractGroup;
import com.distrimind.madkit.kernel.Group;
import com.distrimind.madkit.kernel.MadkitClassLoader;
import com.distrimind.madkit.kernel.Role;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Internationalization help class containing only static methods.
 * 
 * @author Fabien Michel
 * @since MaDKit 5.0.0.10
 * @version 0.91
 * 
 */
public class I18nUtilities {

	public static File i18nDirectory = new File("com/distrimind/madkit/i18n/");
	//public static File relativeI18nDirectory = new File("i18n/");

	public static ResourceBundle getResourceBundle(String baseName) {

		// The below is a copy of the default implementation.

		/*try (InputStream stream  = MadkitClassLoader.getLoader().getResourceAsStream(relativeI18nDirectory.toString() + File.separator + baseName)) {
			if (stream != null)
				return new PropertyResourceBundle(new InputStreamReader(stream, StandardCharsets.UTF_8));
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;*/

		ResourceBundle rb= ResourceBundle.getBundle(i18nDirectory.toString() + File.separator + baseName);

		if (rb==null)
			return null;

		HashMap<String, String> res=new HashMap<>();
		for (String k: rb.keySet())
			res.put(k, new String(rb.getString(k).getBytes(StandardCharsets.ISO_8859_1), StandardCharsets.UTF_8));
		return new ResourceBundle() {
			@Override
			protected Object handleGetObject(String key) {
				return res.get(key);
			}

			@Override
			public Enumeration<String> getKeys() {
				return Collections.enumeration(res.keySet());
			}
		};
	}

	public static String getCGRString(final String community) {
		return Words.COMMUNITY + " <" + community + "> ";
	}

	public static String getCGRString(final Group group) {
		return getCGRString(group, null);
	}

	public static String getCGRString(final AbstractGroup group) {
		return getCGRString(group, null);
	}

	public static String getCGRString(Role _role) {
		return getCGRString(_role.getGroup(), _role.getRole());
	}

	public static String getCGRString(Group _group, final String role) {
		if (role != null)
			return Words.ROLE + " <" + _group + "," + role + "> ";
		else
			return Words.GROUP + " <" + _group + "> ";

	}

	public static String getCGRString(AbstractGroup _group, final String role) {
		if (role != null)
			return Words.ROLE + " <" + _group + "," + role + "> ";
		else
			return Words.GROUP + " <" + _group + "> ";

	}

	/**
	 * @param i18nDirectory
	 *            the i18nDirectory to set
	 */
	public static void setI18nDirectory(File i18nDirectory) {
		I18nUtilities.i18nDirectory = i18nDirectory;
		ResourceBundle.clearCache();
	}
}