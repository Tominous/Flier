/** This program is free software. It comes without any warranty, to
 * the extent permitted by applicable law. You can redistribute it
 * and/or modify it under the terms of the Do What The Fuck You Want
 * To Public License, Version 2, as published by Sam Hocevar. See
 * http://www.wtfpl.net/ for more details.
 */
package pl.betoncraft.flier.action;

import java.util.Optional;
import java.util.Set;

import org.bukkit.configuration.ConfigurationSection;

import com.google.common.collect.Sets;

import pl.betoncraft.flier.api.core.InGamePlayer;
import pl.betoncraft.flier.api.core.LoadingException;
import pl.betoncraft.flier.api.core.Kit.AddResult;
import pl.betoncraft.flier.api.core.SetApplier;
import pl.betoncraft.flier.api.core.UsableItem;
import pl.betoncraft.flier.core.DefaultSetApplier;

/**
 * Action which applies an ItemSet.
 *
 * @author Jakub Sapalski
 */
public class ItemSetAction extends DefaultAction {

	private SetApplier applier;
	private Set<AddResult> accepted = Sets.newHashSet(
			AddResult.ADDED,
			AddResult.FILLED,
			AddResult.REMOVED,
			AddResult.REPLACED
	);

	public ItemSetAction(ConfigurationSection section) throws LoadingException {
		super(section, false, false);
		applier = new DefaultSetApplier(section);
	}

	@Override
	public boolean act(Optional<InGamePlayer> creator, Optional<InGamePlayer> source,
			InGamePlayer target, Optional<UsableItem> item) {
		if (accepted.contains(applier.isSaving() ?
				target.getKit().addStored(applier) :
				target.getKit().addCurrent(applier))) {
			target.updateKit();
			return true;
		}
		return false;
	}

}
