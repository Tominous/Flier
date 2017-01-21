/** This program is free software. It comes without any warranty, to
 * the extent permitted by applicable law. You can redistribute it
 * and/or modify it under the terms of the Do What The Fuck You Want
 * To Public License, Version 2, as published by Sam Hocevar. See
 * http://www.wtfpl.net/ for more details.
 */
package pl.betoncraft.flier.lobby;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.scheduler.BukkitRunnable;

import net.md_5.bungee.api.ChatColor;
import pl.betoncraft.flier.Flier;
import pl.betoncraft.flier.api.Engine;
import pl.betoncraft.flier.api.Game;
import pl.betoncraft.flier.api.InGamePlayer;
import pl.betoncraft.flier.api.Item;
import pl.betoncraft.flier.api.Lobby;
import pl.betoncraft.flier.api.PlayerClass;
import pl.betoncraft.flier.api.Wings;
import pl.betoncraft.flier.core.DefaultPlayerClass;
import pl.betoncraft.flier.core.Utils;
import pl.betoncraft.flier.core.ValueLoader;
import pl.betoncraft.flier.exception.LoadingException;

/**
 * Physical lobby with fixed classes selected by clicking on blocks.
 *
 * @author Jakub Sapalski
 */
public class PhysicalLobby implements Lobby, Listener {

	private Map<String, Game> games = new HashMap<>();
	private Game currentGame;
	private List<Block> join = new ArrayList<>();
	private Location spawn;
	private Block start;
	private Block leave;
	private PlayerClass defClass;
	private Map<Block, ItemBlock> blocks = new HashMap<>();
	private Map<InGamePlayer, List<ItemBlock>> unlocked = new HashMap<>();
	private List<UUID> blocked = new LinkedList<>();

	public PhysicalLobby(ConfigurationSection section) throws LoadingException {
		spawn = ValueLoader.loadLocation(section, "spawn");
		for (String loc : section.getStringList("join")) {
			join.add(Utils.parseLocation(loc).getBlock());
		}
		start = ValueLoader.loadLocation(section, "start").getBlock();
		leave = ValueLoader.loadLocation(section, "leave").getBlock();
		try {
			ConfigurationSection playerClass = section.getConfigurationSection("default_class");
			if (playerClass == null) {
				throw new LoadingException("Player class is not defined.");
			}
			defClass = new DefaultPlayerClass(playerClass);
		} catch (LoadingException e) {
			throw (LoadingException) new LoadingException("Error in player class.").initCause(e);
		}
		ConfigurationSection itemsSection = section.getConfigurationSection("items");
		if (itemsSection != null) for (String i : itemsSection.getKeys(false)) {
			ConfigurationSection itemSection = itemsSection.getConfigurationSection(i);
			try {
				blocks.put(ValueLoader.loadLocation(itemSection, "block").getBlock(), new ItemBlock(itemSection));
			} catch (LoadingException e) {
				throw (LoadingException) new LoadingException(String.format("Error in '%s' item set.", i)).initCause(e);
			}
		}
		List<String> gameNames = section.getStringList("games");
		for (String gameName : gameNames) {
			try {
				Game game = Flier.getInstance().getGame(gameName); // TODO game starting/stopping
				game.setLobby(this);
				games.put(gameName, game);
			} catch (LoadingException e) {
				throw (LoadingException) new LoadingException(String.format("Error in '%s' game.", gameName))
						.initCause(e);
			}
		}
		if (games.isEmpty()) {
			throw new LoadingException("Game list is empty.");
		}
		currentGame = games.get(gameNames.get(0));
		Bukkit.getPluginManager().registerEvents(this, Flier.getInstance());
	}

	private enum AddType {
		RESET, CLEAR, REPLACE, ADD, TAKE
	}

	private class ItemBlock {

		private final AddType addType;
		private String name;
		private int buyCost = 0;
		private int unlockCost = 0;
		private Engine engine;
		private Wings wings;
		private Map<Item, Integer> items = new HashMap<>();

		private ItemBlock(ConfigurationSection section) throws LoadingException {
			addType = ValueLoader.loadEnum(section, "type", AddType.class);
			if (addType == AddType.RESET) {
				return;
			}
			name = section.getString("name");
			buyCost = section.getInt("buy_cost", buyCost);
			unlockCost = section.getInt("unlock_cost", unlockCost);
			String engineName = section.getString("engine");
			if (engineName == null) {
				engine = null;
			} else {
				engine = Flier.getInstance().getEngine(engineName);
			}
			String wingsName = section.getString("wings");
			if (wingsName == null) {
				wings = null;
			} else {
				wings = Flier.getInstance().getWing(wingsName);
			}
			try {
				List<String> itemNames = section.getStringList("items");
				for (String item : itemNames) {
					item = item.trim();
					int amount = 1;
					if (item.contains(" ")) {
						String[] parts = item.split(" ");
						if (parts.length != 2) {
							throw new LoadingException(String.format("Item format in '%s' is incorrect.", item));
						}
						try {
							amount = Integer.parseInt(parts[0]);
						} catch (NumberFormatException e) {
							throw new LoadingException(String.format("Cannot parse item amount in '%s'.", item));
						}
						item = parts[1];
					}
					if (amount <= 0) {
						throw new LoadingException(String.format("Item amount in '%s' must be positive.", item));
					}
					items.put(Flier.getInstance().getItem(item), amount);
				}
			} catch (LoadingException e) {
				throw (LoadingException) new LoadingException("Error in items.").initCause(e);
			}
		}

		private int getBuyCost() {
			return buyCost;
		}

		private int getUnlockCost() {
			return unlockCost;
		}

		private boolean apply(PlayerClass c) {
			c.save();
			if (name != null) {
				c.setStoredName(name);
			}
			switch (addType) {
			case RESET:
				c.reset();
				break;
			case CLEAR:
				c.setStoredEngine(engine);
				c.setStoredWings(wings);
				c.setStoredItems(items);
				break;
			case REPLACE:
				if (engine != null) {
					c.setStoredEngine(engine);
				}
				if (wings != null) {
					c.setStoredWings(wings);
				}
				Map<Item, Integer> storedItems = c.getStoredItems();
				for (Iterator<Entry<Item, Integer>> i = items.entrySet().iterator(); i.hasNext();) {
					Entry<Item, Integer> e = i.next();
					for (Iterator<Entry<Item, Integer>> si = storedItems.entrySet().iterator(); si.hasNext();) {
						Entry<Item, Integer> se = si.next();
						if (e.getKey().slot() == se.getKey().slot()) {
							si.remove();
						}
					}
					storedItems.put(e.getKey(), e.getValue());
				}
				c.setStoredItems(storedItems);
				break;
			case ADD:
				if (engine != null && c.getStoredEngine() != null) {
					c.save();
					return false;
				}
				if (engine != null) {
					c.setStoredEngine(engine);
				}
				if (wings != null && c.getStoredWings() != null) {
					c.save();
					return false;
				}
				if (wings != null) {
					c.setStoredWings(wings);
				}
				Map<Item, Integer> storedItems2 = c.getStoredItems();
				for (Iterator<Entry<Item, Integer>> i = items.entrySet().iterator(); i.hasNext();) {
					Entry<Item, Integer> e = i.next();
					int oldAmount = 0;
					for (Iterator<Entry<Item, Integer>> si = storedItems2.entrySet().iterator(); si.hasNext();) {
						Entry<Item, Integer> se = si.next();
						if (e.getKey().slot() == se.getKey().slot()) {
							si.remove();
							if (e.getKey().equals(se.getKey())) {
								oldAmount = se.getValue();
								break;
							} else {
								c.save();
								return false;
							}
						}
					}
					storedItems2.put(e.getKey(), oldAmount + e.getValue());
				}
				c.setStoredItems(storedItems2);
				break;
			case TAKE:
				if (engine != null && engine.equals(c.getStoredEngine())) {
					c.setStoredEngine(null);
				}
				if (wings != null && wings.equals(c.getStoredWings())) {
					c.setStoredWings(null);
				}
				Map<Item, Integer> storedItems3 = c.getStoredItems();
				for (Iterator<Entry<Item, Integer>> i = items.entrySet().iterator(); i.hasNext();) {
					Entry<Item, Integer> e = i.next();
					int oldAmount = 0;
					for (Iterator<Entry<Item, Integer>> si = storedItems3.entrySet().iterator(); si.hasNext();) {
						Entry<Item, Integer> se = si.next();
						if (e.getKey().slot() == se.getKey().slot()) {
							si.remove();
							if (e.getKey().equals(se.getKey())) {
								oldAmount = se.getValue();
								break;
							}
						}
					}
					int newAmount = oldAmount - e.getValue();
					if (newAmount > 0) {
						storedItems3.put(e.getKey(), newAmount);
					} else if (newAmount < 0) {
						c.save();
						return false;
					}
				}
				c.setStoredItems(storedItems3);
				break;
			}
			c.load();
			return true;
		}

	}

	@Override
	public void setGame(Game game) {
		this.currentGame = game;
	}

	@Override
	public Game getGame() {
		return currentGame;
	}

	@Override
	public Location getSpawn() {
		return spawn;
	}

	@Override
	public PlayerClass getDefaultClass() {
		return defClass.clone();
	}

	@Override
	public void stop() {
		for (Game game : games.values()) {
			game.stop();
		}
		HandlerList.unregisterAll(this);
	}

	@EventHandler(priority = EventPriority.HIGH)
	public void onClick(PlayerInteractEvent event) {
		if (event.getAction() != Action.LEFT_CLICK_BLOCK) {
			return;
		}
		Player player = event.getPlayer();
		Block block = event.getClickedBlock();
		if (block == null) {
			return;
		}
		// this prevents double clicks on next tick
		if (blocked.contains(player.getUniqueId())) {
			return;
		} else {
			blocked.add(player.getUniqueId());
			new BukkitRunnable() {
				@Override
				public void run() {
					blocked.remove(player.getUniqueId());
				}
			}.runTaskLater(Flier.getInstance(), 5);
		}
		// handle the click
		if (join.contains(block)) {
			currentGame.addPlayer(player);
		} else if (block.equals(start)) {
			currentGame.startPlayer(player);
		} else if (block.equals(leave)) {
			currentGame.removePlayer(player);
		} else {
			handleItems(player, block);
		}
	}

	private void handleItems(Player player, Block block) {
		InGamePlayer p = currentGame.getPlayers().get(player.getUniqueId());
		if (p == null) {
			return;
		}
		PlayerClass c = p.getClazz();
		ItemBlock b = blocks.get(block);
		if (b != null) {
			List<ItemBlock> ul = unlocked.get(p);
			if (ul == null) {
				ul = new LinkedList<>();
				unlocked.put(p, ul);
			}
			if (!ul.contains(b)) {
				if (b.getUnlockCost() <= p.getMoney()) {
					ul.add(b);
					p.setMoney(p.getMoney() - b.getUnlockCost());
					if (b.getUnlockCost() != 0) {
						player.sendMessage(ChatColor.GREEN + "Unlocked!");
					}
				} else {
					player.sendMessage(ChatColor.RED + "Not enough money to unlock.");
					return;
				}
			}
			if (b.getBuyCost() <= p.getMoney()) {
				if (b.apply(c)) {
					p.setMoney(p.getMoney() - b.getBuyCost());
					p.updateClass();
					player.sendMessage(ChatColor.GREEN + "Class updated!");
				} else {
					player.sendMessage(ChatColor.RED + "You can't use this right now.");
				}
			} else {
				player.sendMessage(ChatColor.RED + "Not enough money.");
			}
		}
	}

}
