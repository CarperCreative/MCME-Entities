package com.mcmiddleearth.entities.events.listener;

import com.mcmiddleearth.entities.EntitiesPlugin;
import com.mcmiddleearth.entities.entities.McmeEntity;
import com.mcmiddleearth.entities.entities.RealPlayer;
import org.bukkit.FluidCollisionMode;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.ExplosionPrimeEvent;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

import java.util.Collection;

public class ExplosionListener implements Listener  {
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onExplosion(ExplosionPrimeEvent event) {
        float radius = event.getRadius();
        Location explosionLocation = event.getEntity().getLocation();

        int radiusInt = (int) Math.ceil(radius);
        Collection<McmeEntity> entitiesInRange = EntitiesPlugin.getEntityServer().getEntitiesAt(explosionLocation, radiusInt, radiusInt, radiusInt);

        if (entitiesInRange.isEmpty()) return;

        float damageRadius = radius * 2f;
        Vector explosionPosition = explosionLocation.toVector();
        World world = event.getEntity().getWorld();

        for (McmeEntity mcmeEntity : entitiesInRange) {
            // Players already get damaged by vanilla behavior - skip
            if (mcmeEntity instanceof RealPlayer) continue;

            Vector entityPosition = mcmeEntity.getLocation().toVector();
            BoundingBox boundingBox = mcmeEntity.getBoundingBox();

            // Calculate exposure. This might be wrong - the wiki was a little unclear.
            float maxX = (float) (entityPosition.getX() + boundingBox.getWidthX());
            float maxY = (float) (entityPosition.getY() + boundingBox.getHeight());
            float maxZ = (float) (entityPosition.getZ() + boundingBox.getWidthZ());
            int pointsSampled = 0;
            int pointsHit = 0;
            Vector point = new Vector();
            for (float x = (float) entityPosition.getX(); x < maxX; x += 1 / 2.96) {
                for (float y = (float) entityPosition.getY(); y < maxY; y += 1 / 2.96) {
                    for (float z = (float) entityPosition.getZ(); z < maxZ; z += 1 / 2.96) {
                        pointsSampled++;

                        point.setX(x).setY(y).setZ(z).subtract(explosionPosition);

                        RayTraceResult rayTraceResult = world.rayTraceBlocks(explosionLocation, point, radius, FluidCollisionMode.ALWAYS);

                        pointsSampled++;
                        if (rayTraceResult == null) {
                            pointsHit++;
                        }
                    }
                }
            }

            float exposure = pointsHit / (float) pointsSampled;

            float impact = Math.max(0f, (1f - (((float) entityPosition.distance(explosionPosition)) / damageRadius)) * exposure);
            float damage = (float) Math.floor((impact * impact + impact) * 7 * radius + 1);

            // TODO: i don't think any code does damage reduction from the blast resistance enchantment
            mcmeEntity.receiveAttack(null, damage, exposure);
        }
    }
}
