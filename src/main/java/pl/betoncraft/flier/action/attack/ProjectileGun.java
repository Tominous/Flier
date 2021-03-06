/**
 * Copyright (c) 2017 Jakub Sapalski
 * 
 * Permission is hereby granted, free of charge, to any person obtaining
 * a copy of this software and associated documentation files (the
 * "Software"), to deal in the Software without restriction, including
 * without limitation the rights to use, copy, modify, merge, publish,
 * distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to
 * the following conditions:
 * 
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE
 * LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
 * OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
 * WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 * 
 */
package pl.betoncraft.flier.action.attack;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Explosive;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import pl.betoncraft.flier.api.Flier;
import pl.betoncraft.flier.api.core.Attacker;
import pl.betoncraft.flier.api.core.InGamePlayer;
import pl.betoncraft.flier.api.core.LoadingException;
import pl.betoncraft.flier.api.core.Owner;
import pl.betoncraft.flier.api.core.Target;
import pl.betoncraft.flier.core.DefaultAttacker;
import pl.betoncraft.flier.event.FlierProjectileLaunchEvent;

/**
 * Burst shooting weapon with unguided projectile-based bullets.
 *
 * @author Jakub Sapalski
 */
public class ProjectileGun extends DefaultAttack {
	
	private static final String ENTITY = "entity";
	private static final String BURST_AMOUNT = "burst_amount";
	private static final String BURST_TICKS = "burst_ticks";
	private static final String PROJECTILE_SPEED = "projectile_speed";
	
	private static ProjectileListener listener;

	private final EntityType entity;
	private final int burstAmount;
	private final int burstTicks;
	private final double projectileSpeed;
	private final int range = 10 * 20;
	
	public ProjectileGun(ConfigurationSection section, Optional<Owner> owner) throws LoadingException {
		super(section, owner);
		entity = loader.loadEnum(ENTITY, EntityType.class);
		burstAmount = loader.loadPositiveInt(BURST_AMOUNT);
		burstTicks = loader.loadPositiveInt(BURST_TICKS);
		projectileSpeed = loader.loadPositiveDouble(PROJECTILE_SPEED);
		// register a single listener for all ProjectileGuns
		if (listener == null) {
			listener = new ProjectileListener();
			Bukkit.getPluginManager().registerEvents(listener, Flier.getInstance());
		}
	}
	
	@Override
	public boolean act(InGamePlayer target, InGamePlayer source) {
		Player player = target.getPlayer();
		int burstAmount = (int) modMan.modifyNumber(BURST_AMOUNT, this.burstAmount);
		Map<Projectile, Vector> projectiles = new HashMap<>(burstAmount);
		new BukkitRunnable() {
			int counter = burstAmount;
			double projectileSpeed = modMan.modifyNumber(PROJECTILE_SPEED, ProjectileGun.this.projectileSpeed);
			EntityType entity = modMan.modifyEnum(ENTITY, ProjectileGun.this.entity);
			@Override
			public void run() {
				Vector velocity = player.getLocation().getDirection().clone().multiply(projectileSpeed);
				Vector pointer = player.getLocation().getDirection().clone().multiply(player.getVelocity().length() * 3);
				Location launch = (player.isGliding() ? player.getLocation() : player.getEyeLocation())
						.clone().add(pointer);
				Projectile projectile = (Projectile) launch.getWorld().spawnEntity(launch, entity);
				projectile.setVelocity(velocity);
				projectile.setShooter(player);
				try{
					projectile.setGravity(false);
				} catch (NoSuchMethodError e) {}
				projectile.setBounce(false);
				if (projectile instanceof Explosive) {
					Explosive explosive = (Explosive) projectile;
					explosive.setIsIncendiary(false);
					explosive.setYield(0);
				}
				Attacker.saveAttacker(projectile, new DefaultAttacker(ProjectileGun.this, owner.get().getPlayer(),
						target, owner.get().getItem()));
				projectiles.put(projectile, velocity);
				counter --;
				if (counter <= 0) {
					cancel();
				}
				// call event for each projectile launched
				Bukkit.getPluginManager().callEvent(new FlierProjectileLaunchEvent(target, ProjectileGun.this));
			}
		}.runTaskTimer(Flier.getInstance(), 0, (int) modMan.modifyNumber(BURST_TICKS, burstTicks));
		new BukkitRunnable() {
			int life = 0;
			@Override
			public void run() {
				// update projectile path to prevent them from flying around
				for (Entry<Projectile, Vector> entry : projectiles.entrySet()) {
					entry.getKey().setVelocity(entry.getValue());
				}
				// cancel after the range has passed
				if (++life >= range) {
					cancel();
				}
			}
		}.runTaskTimer(Flier.getInstance(), 0, 1);
		return true;
	}
	
	public class ProjectileListener implements Listener {
		
		@EventHandler(priority=EventPriority.LOW)
		public void onHit(EntityDamageByEntityEvent event) {
			if (event.isCancelled()) {
				return;
			}
			Attacker attacker = Attacker.getAttacker(event.getDamager());
			if (attacker != null && attacker.getDamager() instanceof ProjectileGun) {
				event.setCancelled(true);
				event.getDamager().remove();
				Target target = attacker.getCreator().getGame().getTargets().get(event.getEntity().getUniqueId());
				if (target != null && target.isTargetable()) {
					attacker.getCreator().getGame().handleHit(target, attacker);
				}
			}
		}
		
	}

}
