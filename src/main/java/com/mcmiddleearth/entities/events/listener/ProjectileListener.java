package com.mcmiddleearth.entities.events.listener;

import com.mcmiddleearth.entities.EntitiesPlugin;
import com.mcmiddleearth.entities.api.McmeEntityType;
import com.mcmiddleearth.entities.api.VirtualEntityFactory;
import com.mcmiddleearth.entities.entities.McmeEntity;
import com.mcmiddleearth.entities.exception.InvalidDataException;
import com.mcmiddleearth.entities.exception.InvalidLocationException;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.AbstractArrow;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.ProjectileLaunchEvent;

import java.util.Collections;
import java.util.HashSet;
import java.util.UUID;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class ProjectileListener implements Listener {

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onProjectileLaunch(ProjectileLaunchEvent event) {
        Projectile projectile = event.getEntity();
        if(!projectile.getScoreboardTags().contains("ignore_virtual_entity")){

            Location location = projectile.getLocation();
            location.setDirection(projectile.getVelocity());
            McmeEntityType type = new McmeEntityType(projectile.getType());
            double damage = 0;
            double knockback = 0;
            if(projectile instanceof AbstractArrow) {
                damage = ((AbstractArrow) projectile).getDamage();
                knockback = ((AbstractArrow) projectile).getKnockbackStrength();
            }
            McmeEntity shooter = null;
            if(projectile.getShooter() instanceof Player) {
                shooter = EntitiesPlugin.getEntityServer().getOrCreateMcmePlayer((Player) projectile.getShooter());
            }

            // Hacky solution to consistently assign shooter to projectile from Witchcraft and Wizardry spells.
            // The problem is the shooter is assigned after the projectile is spawned
            if(projectile.getScoreboardTags().contains("spell_ini")){

                // Find the closest player and assign them as the shooter
                Player closestPlayer = null;
                double closestDistance = Double.POSITIVE_INFINITY;

                for (Player player : Bukkit.getOnlinePlayers()) {
                    double distance = player.getLocation().distanceSquared(projectile.getLocation());
                    if(distance < closestDistance){
                        closestPlayer = player;
                        closestDistance = distance;
                    }
                }
                projectile.setShooter(closestPlayer);
                shooter = EntitiesPlugin.getEntityServer().getOrCreateMcmePlayer(closestPlayer);
            }

            VirtualEntityFactory factory = new VirtualEntityFactory(type, location)
                    .withShooter(shooter)
                    .withProjectileVelocity((float)projectile.getVelocity().length())
                    .withProjectileDamage((float)damage)
                    .withKnockBackBase((float)knockback)
                    .withKnockBackPerDamage(0)
                    .withWhitelist(Collections.singleton(UUID.randomUUID()))
                    .withDependingEntity(projectile)
                    .withCopyOriginalProjectile(true);
            try {
                EntitiesPlugin.getEntityServer().spawnEntity(factory);
            } catch (InvalidLocationException | InvalidDataException e) {
                e.printStackTrace();
            }
            //event.setCancelled(true);
        }
    }
}
