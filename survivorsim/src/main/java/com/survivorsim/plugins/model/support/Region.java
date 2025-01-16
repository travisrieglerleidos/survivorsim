package com.survivorsim.plugins.model.support;

import net.jcip.annotations.Immutable;
import gov.hhs.aspr.ms.gcm.simulation.plugins.regions.support.RegionId;

@Immutable
public final class Region implements RegionId {

	private final int id;

	public Region(int id) {
		this.id = id;
	}

	public int getValue() {
		return id;
	}

	@Override
	public int hashCode() {
		return id;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof Region)) {
			return false;
		}
		Region other = (Region) obj;
		if (id != other.id) {
			return false;
		}
		return true;
	}

	@Override
	public String toString() {
		return "Region_" + id;
	}
}
