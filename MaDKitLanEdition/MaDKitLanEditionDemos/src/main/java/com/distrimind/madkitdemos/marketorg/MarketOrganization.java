/*
 * Copyright 1997-2013 Fabien Michel, Olivier Gutknecht, Jacques Ferber
 * 
 * This file is part of MaDKit_Demos.
 * 
 * MaDKit_Demos is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * MaDKit_Demos is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with MaDKit_Demos. If not, see <http://www.gnu.org/licenses/>.
 */
package com.distrimind.madkitdemos.marketorg;

import com.distrimind.madkit.kernel.Group;

public interface MarketOrganization {

	String COMMUNITY = "travel";

	Group CLIENT_GROUP = new Group(COMMUNITY, "travel-clients");
	Group PROVIDERS_GROUP = new Group(COMMUNITY, "travel-providers");

	String CLIENT_ROLE = "client";
	String BROKER_ROLE = "broker";
	String PROVIDER_ROLE = "provider";

}
