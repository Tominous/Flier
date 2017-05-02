/** This program is free software. It comes without any warranty, to
 * the extent permitted by applicable law. You can redistribute it
 * and/or modify it under the terms of the Do What The Fuck You Want
 * To Public License, Version 2, as published by Sam Hocevar. See
 * http://www.wtfpl.net/ for more details.
 */
package pl.betoncraft.flier.activator;

import org.bukkit.configuration.ConfigurationSection;

import pl.betoncraft.flier.api.core.InGamePlayer;
import pl.betoncraft.flier.api.core.LoadingException;
import pl.betoncraft.flier.api.core.UsableItem;

/**
 * Activates when the player has specified occurrence on this tick.
 *
 * @author Jakub Sapalski
 */
public class OccurrenceActivator extends DefaultActivator {
	
	private String occurrence;

	public OccurrenceActivator(ConfigurationSection section) throws LoadingException {
		super(section);
		occurrence = loader.loadString("occurrence");
	}

	@Override
	public boolean isActive(InGamePlayer player, UsableItem item) {
		return player.getOccurrences().contains(occurrence);
	}

}