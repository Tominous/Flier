/** This program is free software. It comes without any warranty, to
 * the extent permitted by applicable law. You can redistribute it
 * and/or modify it under the terms of the Do What The Fuck You Want
 * To Public License, Version 2, as published by Sam Hocevar. See
 * http://www.wtfpl.net/ for more details.
 */
package pl.betoncraft.flier.api.core;

import java.util.List;

/**
 * Represents an item which can be used by the player to run an action.
 *
 * @author Jakub Sapalski
 */
public interface UsableItem extends Item {

	/**
	 * Uses this item once.
	 * 
	 * @param player
	 *            the player for which the item will be used
	 * @return if the item was used or not
	 */
	public boolean use(InGamePlayer player);

	/**
	 * @return the list of all usages of this item
	 */
	public List<Usage> getUsages();

	/**
	 * @return whenever this item is ready to use
	 */
	public boolean isReady();

	/**
	 * @return whole cooldown time for this item when it started
	 */
	public int getWholeCooldown();

	/**
	 * @return the current remaining cooldown time
	 */
	public int getCooldown();

	/**
	 * @return whenever this item should be consumed on use
	 */
	public boolean isConsumable();

	/**
	 * @return maximum ammunition amount for this item; 0 means it doesn't use
	 *         ammunition
	 */
	public int getMaxAmmo();

	/**
	 * @return the current amount of ammunition
	 */
	public int getAmmo();

	/**
	 * Sets the ammunition of the UsableItem. This won't go above max ammunition
	 * nor below 0.
	 * 
	 * @param ammo
	 *            the amount of ammunition to set
	 */
	public void setAmmo(int ammo);

	/**
	 * @return the amount of this Item
	 */
	public int getAmount();

	/**
	 * @param amount
	 *            the new amount
	 * @return true when it's possible to set that many items, false if the new
	 *         amount is illegal (less than 0 or greater than maximum)
	 */
	public boolean setAmount(int amount);

	/**
	 * @return the maximum amount of these Items
	 */
	public int getMaxAmount();

	/**
	 * @return the minimum amount of these Items (in case you want to sell
	 *         too much)
	 */
	public int getMinAmount();

	/**
	 * @return the default amount of these Items
	 */
	public int getDefAmount();

	/**
	 * Refills the UsableItem, making it as new.
	 */
	public void refill();

}
