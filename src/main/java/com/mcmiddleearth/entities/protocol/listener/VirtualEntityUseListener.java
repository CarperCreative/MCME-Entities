package com.mcmiddleearth.entities.protocol.listener;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.wrappers.EnumWrappers;
import com.mcmiddleearth.entities.EntitiesPlugin;
import com.mcmiddleearth.entities.ai.goal.GoalMimic;
import com.mcmiddleearth.entities.api.ActionType;
import com.mcmiddleearth.entities.command.AnimateCommand;
import com.mcmiddleearth.entities.entities.McmeEntity;
import com.mcmiddleearth.entities.entities.RealPlayer;
import com.mcmiddleearth.entities.entities.VirtualEntity;
import com.mcmiddleearth.entities.events.events.player.VirtualPlayerAttackEvent;
import com.mcmiddleearth.entities.events.events.player.VirtualPlayerInteractAtEvent;
import com.mcmiddleearth.entities.events.events.player.VirtualPlayerInteractEvent;
import com.mcmiddleearth.entities.protocol.packets.simple.SimpleEntityAnimationPacket;
import com.mcmiddleearth.entities.server.EntityServer;
import net.md_5.bungee.api.chat.ComponentBuilder;
import org.bukkit.Material;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.Vector;

import java.util.logging.Logger;

public class VirtualEntityUseListener extends EntityListener {

    public VirtualEntityUseListener(Plugin plugin, EntityServer entityServer) {
        super(plugin, entityServer, PacketType.Play.Client.USE_ENTITY);
    }

    @Override
    public void onPacketReceiving(PacketEvent event) {
        PacketContainer packet = event.getPacket();
        RealPlayer player = EntitiesPlugin.getEntityServer().getOrCreateMcmePlayer(event.getPlayer());
        int entityId = packet.getIntegers().read(0);
        McmeEntity entity = entityServer.getEntity(entityId);
        if(entity instanceof VirtualEntity) {
            EnumWrappers.EntityUseAction action = packet.getEntityUseActions().read(0);
            if(entity.getGoal() instanceof GoalMimic && ((GoalMimic)entity.getGoal()).getMimic().equals(player)) {
//Logger.getGlobal().info("Mimic Packet recieved! "+action.name());
                switch(action) {
                    case INTERACT:
                    case INTERACT_AT:
                        entity.playAnimation(ActionType.INTERACT);
                        break;
                    case ATTACK:
                        entity.playAnimation(ActionType.ATTACK);
                        break;
                }
                return;
            }
            event.setCancelled(true);
            EquipmentSlot hand;
            if(!action.equals(EnumWrappers.EntityUseAction.ATTACK)
                    && packet.getHands().read(0).equals(EnumWrappers.Hand.MAIN_HAND)) {
                hand = EquipmentSlot.HAND;
            } else {
                hand = EquipmentSlot.OFF_HAND;
            }
            boolean isSneaking = packet.getBooleans().read(0);
            switch(action) {
                case INTERACT_AT:
//Logger.getGlobal().info("interact at!");
                    Vector vector = packet.getVectors().read(0);
//Logger.getLogger(this.getClass().getName()).info("X: " + vector.getX());
//Logger.getLogger(this.getClass().getName()).info("Y: " + vector.getY());
//Logger.getLogger(this.getClass().getName()).info("Z: " + vector.getZ());
                    throwEvent(new VirtualPlayerInteractAtEvent(player, (VirtualEntity)entity, vector, hand, isSneaking));
//Logger.getGlobal().info("interact! "+player.getName());
                    if(player.getBukkitPlayer().getInventory().getItemInMainHand().getType().equals(Material.STICK)) {
//Logger.getGlobal().info("hand! "+hand.name());
                        if(hand.equals(EquipmentSlot.HAND)) {
//Logger.getGlobal().info("off hand!");
                            if (isSneaking) {
//Logger.getGlobal().info("sneak!");
                                player.addToSelectedEntities(entity);
                                player.sendMessage(new ComponentBuilder("Entity added to your selection.").create());
                            } else {
//Logger.getGlobal().info("no sneak!");
                                player.clearSelectedEntities();
                                player.addToSelectedEntities(entity);
                                player.sendMessage(new ComponentBuilder("Entity selected.").create());
                            }
                        }
                        break;
                    }

                    this.triggerSound((VirtualEntity) entity, player);
                    break;
                case INTERACT:
                    throwEvent(new VirtualPlayerInteractEvent(player, (VirtualEntity)entity, hand, isSneaking));
                    break;
                case ATTACK:
//Logger.getGlobal().info("attack!");
                    VirtualPlayerAttackEvent entityEvent = new VirtualPlayerAttackEvent(player, (VirtualEntity)entity, isSneaking);
                    throwEvent(entityEvent);
                    if(!entityEvent.isCancelled()) {
                        player.attack(entity);
                    }
                    break;
            }
        }
    }

    private void triggerSound(VirtualEntity entity, RealPlayer player) {
        if(!entity.getSubtitles().isEmpty()) {
            entity.sendSubtitle(player);
        }

        if(!entity.getSounds().isEmpty()) {
            entity.playSound(player);
            return;
        }

        if(entity.getSequencedSounds().isEmpty()) {
            return;
        }

        entity.playSequencedSound(player);
    }
}
