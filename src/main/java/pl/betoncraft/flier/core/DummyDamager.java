/** This program is free software. It comes without any warranty, to
 * the extent permitted by applicable law. You can redistribute it
 * and/or modify it under the terms of the Do What The Fuck You Want
 * To Public License, Version 2, as published by Sam Hocevar. See
 * http://www.wtfpl.net/ for more details.
 */
package pl.betoncraft.flier.core;

import pl.betoncraft.flier.api.Damager;

/**
 * A dummy Damager.
 *
 * @author Jakub Sapalski
 */
public class DummyDamager implements Damager {
	
	public static final DummyDamager DUMMY = new DummyDamager();
	
	private DummyDamager() {}

	@Override
	public double getDamage() {
		return 0;
	}

	@Override
	public boolean wingsOff() {
		return false;
	}

	@Override
	public boolean killsOnGround() {
		return false;
	}

	@Override
	public boolean friendlyFire() {
		return false;
	}

	@Override
	public boolean suicidal() {
		return false;
	}

	@Override
	public double getPhysical() {
		return 0;
	}

}