package com.mcmiddleearth.entities.command;

import static com.mojang.brigadier.arguments.StringArgumentType.greedyString;
import static com.mojang.brigadier.arguments.StringArgumentType.word;

import com.mcmiddleearth.command.McmeCommandSender;
import com.mcmiddleearth.command.builder.HelpfulLiteralBuilder;
import com.mcmiddleearth.command.builder.HelpfulRequiredArgumentBuilder;
import com.mcmiddleearth.entities.Permission;
import com.mcmiddleearth.entities.ai.goal.GoalType;
import com.mcmiddleearth.entities.api.VirtualEntityGoalFactory;
import com.mcmiddleearth.entities.command.argument.AttributeTypeArgument;
import com.mcmiddleearth.entities.command.argument.GoalTypeArgument;
import com.mcmiddleearth.entities.entities.McmeEntity;
import com.mcmiddleearth.entities.entities.RealPlayer;
import com.mcmiddleearth.entities.entities.VirtualEntity;
import com.mcmiddleearth.entities.entities.simple.SimpleHorse;
import com.mcmiddleearth.entities.exception.InvalidDataException;
import com.mcmiddleearth.entities.exception.InvalidLocationException;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.ComponentBuilder;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

public class SetCommand extends McmeEntitiesCommandHandler {

    public SetCommand(String command) {
        super(command);
    }

    @Override
    protected HelpfulLiteralBuilder createCommandTree(HelpfulLiteralBuilder commandNodeBuilder) {
        commandNodeBuilder
            .requires(sender -> (sender instanceof RealPlayer)
                                && ((RealPlayer) sender).getBukkitPlayer().hasPermission(Permission.USER.getNode())
            )
            .then(HelpfulLiteralBuilder.literal("goal")
                .then(HelpfulRequiredArgumentBuilder.argument("type", new GoalTypeArgument())
                    .executes(context -> this.setGoal(context.getSource(), context.getArgument("type", String.class), false))
                    .then(HelpfulLiteralBuilder.literal("loop")
                        .executes(context -> this.setGoal(context.getSource(), context.getArgument("type", String.class), true))
                    )
                )
            )
            .then(HelpfulLiteralBuilder.literal("displayname")
                .then(HelpfulRequiredArgumentBuilder.argument("displayname", word())
                    .executes(context -> this.setDisplayName(context.getSource(), context.getArgument("displayname", String.class)))
                )
            )
            .then(HelpfulLiteralBuilder.literal("item")
                .then(HelpfulRequiredArgumentBuilder.argument("slot", word())
                    .then(HelpfulRequiredArgumentBuilder.argument("item", word())
                        .executes(context -> this.setItem(context.getSource(), context.getArgument("slot", String.class),
                                context.getArgument("item", String.class)
                            )
                        )
                    )
                )
            )
            .then(HelpfulLiteralBuilder.literal("attribute")
                .then(HelpfulRequiredArgumentBuilder.argument("type", new AttributeTypeArgument())
                    .then(HelpfulRequiredArgumentBuilder.argument("value", word())
                        .executes(context -> this.setAttribute(context.getSource(), context.getArgument("type", String.class),
                            context.getArgument("value", String.class)
                        ))
                    )
                )
            )
            .then(HelpfulLiteralBuilder.literal("viewingdistance")
                .then(HelpfulRequiredArgumentBuilder.argument("distance", IntegerArgumentType.integer(1, Integer.MAX_VALUE))
                    .executes(
                        context -> this.setViewDistance(context.getSource(), IntegerArgumentType.getInteger(context, "distance"))
                    )
                )
            )
            .then(HelpfulLiteralBuilder.literal("sound")
                .then(HelpfulLiteralBuilder.literal("add")
                    .then(HelpfulRequiredArgumentBuilder.argument("sound", greedyString())
                        .executes(
                            context -> this.addSound(context.getSource(), context.getArgument("sound", String.class))
                        )
                        .then(HelpfulRequiredArgumentBuilder.argument("sequence", BoolArgumentType.bool())
                            .executes(context -> this.addSound(
                                context.getSource(),
                                context.getArgument("sound", String.class),
                                context.getArgument("sequence", Boolean.class)
                            ))
                        )
                    )
                )
                .then(HelpfulLiteralBuilder.literal("clear")
                    .executes(
                        context -> this.clearSounds(context.getSource())
                    )
                )
            )
            .then(HelpfulLiteralBuilder.literal("subtitle")
                .then(HelpfulLiteralBuilder.literal("add")
                    .then(HelpfulRequiredArgumentBuilder.argument("text", greedyString())
                        .executes(context ->
                            this.addSubtitle(
                                context.getSource(),
                                context.getArgument("text", String.class)
                            )
                        )

                    )
                )
                .then(HelpfulLiteralBuilder.literal("clear")
                    .executes(
                        context -> this.clearSubtitles(context.getSource())
                    )
                )
            );
        return commandNodeBuilder;
    }

    public int setItem(McmeCommandSender sender, String slotName, String itemMaterial) {
        final RealPlayer player = (RealPlayer) sender;
        final Set<McmeEntity> selectedEntities = player.getSelectedEntities();

        if (selectedEntities.isEmpty()) {
            sender.sendMessage(new ComponentBuilder("You need to select at least one entity to set an item").color(ChatColor.RED).create());
            return 0;
        }

        if (slotName.equalsIgnoreCase("saddle")) {
            final Set<SimpleHorse> horses = selectedEntities.stream()
                .filter(SimpleHorse.class::isInstance)
                .map(SimpleHorse.class::cast)
                .collect(Collectors.toSet());
            if (horses.isEmpty()) {
                sender.sendMessage(new ComponentBuilder("Not implemented for custom entities.").color(ChatColor.RED).create());
                return 0;
            }

            final AtomicInteger saddledEntities = new AtomicInteger(0);
            final AtomicInteger unSaddledEntities = new AtomicInteger(0);
            for (final SimpleHorse horse : horses) {
                if (itemMaterial.equalsIgnoreCase("saddle")) {
                    horse.setSaddled(true);

                    saddledEntities.incrementAndGet();
                } else {
                    horse.setSaddled(false);
                    unSaddledEntities.incrementAndGet();
                }
            }

            if (saddledEntities.get() > 0) {
                sender.sendMessage(new ComponentBuilder(Integer.toString(saddledEntities.get())).color(ChatColor.YELLOW)
                    .append(new ComponentBuilder(" entities saddled").color(ChatColor.GREEN).create()).create());
            }

            if (unSaddledEntities.get() > 0) {
                sender.sendMessage(new ComponentBuilder(Integer.toString(unSaddledEntities.get())).color(ChatColor.YELLOW)
                    .append(new ComponentBuilder(" entities unsaddled").color(ChatColor.GREEN).create()).create());
            }

        } else {
            EquipmentSlot slot = EquipmentSlot.HAND;
            try {
                slot = EquipmentSlot.valueOf(slotName.toUpperCase());
            } catch (IllegalArgumentException ex) {
                sender.sendMessage(new ComponentBuilder("Can't parse equipment slot. Using main hand.").color(ChatColor.RED).create());
            }
            Material material = Material.LEATHER_CHESTPLATE;
            try {
                material = Material.valueOf(itemMaterial.toUpperCase());
            } catch (IllegalArgumentException ex) {
                sender.sendMessage(new ComponentBuilder("Can't parse item material. Using leather chest plate.").color(ChatColor.RED).create());
            }
            final ItemStack item = new ItemStack(material);

            final EquipmentSlot finalSlot = slot;
            selectedEntities.forEach(mcmeEntity -> mcmeEntity.setEquipment(finalSlot, item));
            sender.sendMessage(
                new ComponentBuilder(slot.name().toLowerCase() + " item set to " + material.name().toLowerCase() + ".").color(ChatColor.GREEN).create());
        }
        return 0;
    }

    private int setAttribute(McmeCommandSender sender, String type, String valueString) {
        try {
            final double value = Double.parseDouble(valueString);
            final RealPlayer player = (RealPlayer) sender;
            final Set<McmeEntity> selectedEntities = player.getSelectedEntities();

            if (selectedEntities.isEmpty()) {
                sender.sendMessage(new ComponentBuilder("You need to select at least one entity to set new attributes").color(ChatColor.RED).create());
                return 0;
            }
            final Attribute attributeType = Attribute.valueOf(type.toUpperCase());

            final AtomicBoolean sentAttributeNotFoundMessage = new AtomicBoolean(false);
            final AtomicBoolean sentAttributeSetMessage = new AtomicBoolean(false);

            selectedEntities.stream()
                .filter(VirtualEntity.class::isInstance)
                .map(VirtualEntity.class::cast)
                .forEach(entity -> {
                    AttributeInstance attribute = entity.getAttribute(attributeType);
                    if (attribute == null) {
                        entity.registerAttribute(attributeType);
                        attribute = entity.getAttribute(attributeType);
                    }

                    if (attribute == null) {
                        if (sentAttributeNotFoundMessage.get()) {
                            return;
                        }

                        sender.sendMessage(new ComponentBuilder("Attribute not found!").color(ChatColor.RED).create());
                        sentAttributeNotFoundMessage.set(true);
                        return;
                    }

                    attribute.setBaseValue(value);

                    if (sentAttributeSetMessage.get()) {
                        return;
                    }

                    sender.sendMessage(new ComponentBuilder("Attribute '" + type + "' set to " + value).create());
                    sentAttributeSetMessage.set(true);
                });

        } catch (NumberFormatException ex) {
            sender.sendMessage(new ComponentBuilder("Attribute value must be  a decimal number!").color(ChatColor.RED).create());
        } catch (IllegalArgumentException ex) {
            sender.sendMessage(new ComponentBuilder("Invalid attribute type!").color(ChatColor.RED).create());
        }
        return 0;
    }

    private int setDisplayName(McmeCommandSender source, String displayName) {
        final RealPlayer player = (RealPlayer) source;
        final Set<McmeEntity> selectedEntities = player.getSelectedEntities();

        if (selectedEntities.isEmpty()) {
            source.sendMessage(new ComponentBuilder("You need to select at least one entity to set a new display name").color(ChatColor.RED).create());
            return 0;
        }

        selectedEntities.stream()
            .filter(VirtualEntity.class::isInstance)
            .map(VirtualEntity.class::cast)
            .forEach(entity -> entity.setDisplayName(displayName));

        source.sendMessage(new ComponentBuilder("Set display name to: ").color(ChatColor.YELLOW).append(new ComponentBuilder(displayName).create()).create());
        return 0;
    }


    private int setGoal(McmeCommandSender sender, String type, boolean loop) {
        try {
            final RealPlayer player = (RealPlayer) sender;
            final Set<McmeEntity> selectedEntities = player.getSelectedEntities();
            if (selectedEntities.isEmpty()) {
                sender.sendMessage(new ComponentBuilder("You need to select at least one entity to apply the goal to.").color(ChatColor.RED).create());
                return 0;
            }
            final GoalType goalType = GoalType.valueOf(type.toUpperCase());
            final VirtualEntityGoalFactory factory = new VirtualEntityGoalFactory(goalType)
                .withLoop(loop)
                .withTargetEntity(player.getSelectedTargetEntity())
                .withTargetLocation(player.getSelectedPoints()
                    .stream().findFirst().orElse(null))
                .withCheckpoints(player.getSelectedPoints().toArray(new Location[0]));

            final AtomicBoolean sentInvalidLocationException = new AtomicBoolean(false);
            final AtomicBoolean sentInvalidDataException = new AtomicBoolean(false);
            selectedEntities.stream().filter(VirtualEntity.class::isInstance).map(VirtualEntity.class::cast).forEach(entity -> {
                try {
                    entity.setGoal(factory.build(entity));
                } catch (InvalidLocationException e) {
                    if (sentInvalidLocationException.get()) {
                        return;
                    }
                    sender.sendMessage(new ComponentBuilder("Invalid location. All location must be same world!").color(ChatColor.RED).create());

                    sentInvalidLocationException.set(true);
                } catch (InvalidDataException e) {
                    if (sentInvalidDataException.get()) {
                        return;
                    }
                    sender.sendMessage(new ComponentBuilder(e.getMessage()).color(ChatColor.RED).create());

                    sentInvalidDataException.set(true);
                }
            });

            sender.sendMessage(new ComponentBuilder("Goal has been updated").color(ChatColor.YELLOW).create());

        } catch (IllegalArgumentException ex) {
            sender.sendMessage(new ComponentBuilder("Invalid goal type!").color(ChatColor.RED).create());
        }
        return 0;
    }

    private int setViewDistance(McmeCommandSender source, int distance) {
        final RealPlayer player = (RealPlayer) source;
        final Set<McmeEntity> selectedEntities = player.getSelectedEntities();

        if (selectedEntities.isEmpty()) {
            source.sendMessage(new ComponentBuilder("You need to select at least one entity to set a new view distance").color(ChatColor.RED).create());
            return 0;
        }

        selectedEntities.stream()
            .filter(VirtualEntity.class::isInstance)
            .map(VirtualEntity.class::cast)
            .forEach(entity -> entity.setViewDistance(distance));

        source.sendMessage(new ComponentBuilder("Set view distance to: " + distance).color(ChatColor.YELLOW).create());
        return 0;
    }

    private int addSound(McmeCommandSender source, String sound) {
        return this.addSound(source, sound, false);
    }

    private int addSound(McmeCommandSender source, String sound, boolean sequence) {
        final RealPlayer player = (RealPlayer) source;
        final Set<McmeEntity> selectedEntities = player.getSelectedEntities();

        if (selectedEntities.isEmpty()) {
            source.sendMessage(new ComponentBuilder("You need to select at least one entity to add a sound").color(ChatColor.RED).create());
            return 0;
        }

        selectedEntities.stream()
            .filter(VirtualEntity.class::isInstance)
            .map(VirtualEntity.class::cast)
            .forEach(entity -> entity.addSound(sound, sequence));

        source.sendMessage(new ComponentBuilder("Added sound: " + sound).color(ChatColor.YELLOW).create());
        return 0;
    }

    private int clearSounds(McmeCommandSender source) {
        final RealPlayer player = (RealPlayer) source;
        final Set<McmeEntity> selectedEntities = player.getSelectedEntities();

        if (selectedEntities.isEmpty()) {
            source.sendMessage(new ComponentBuilder("You need to select at least one entity to clear the sounds").color(ChatColor.RED).create());
            return 0;
        }

        selectedEntities.stream()
            .filter(VirtualEntity.class::isInstance)
            .map(VirtualEntity.class::cast)
            .forEach(VirtualEntity::clearSounds);

        source.sendMessage(new ComponentBuilder("Cleared sounds").color(ChatColor.YELLOW).create());
        return 0;
    }

    private int addSubtitle(McmeCommandSender source, String text) {
        final RealPlayer player = (RealPlayer) source;
        final Set<McmeEntity> selectedEntities = player.getSelectedEntities();

        if (selectedEntities.isEmpty()) {
            source.sendMessage(new ComponentBuilder("You need to select at least one entity to add a subtitle").color(ChatColor.RED).create());
            return 0;
        }

        selectedEntities.stream()
            .filter(VirtualEntity.class::isInstance)
            .map(VirtualEntity.class::cast)
            .forEach(entity -> entity.addSubtitle(text));

        source.sendMessage(new ComponentBuilder("Added subtitle: " + text).color(ChatColor.YELLOW).create());
        return 0;
    }

    private int clearSubtitles(McmeCommandSender source) {
        final RealPlayer player = (RealPlayer) source;
        final Set<McmeEntity> selectedEntities = player.getSelectedEntities();

        if (selectedEntities.isEmpty()) {
            source.sendMessage(new ComponentBuilder("You need to select at least one entity to clear the subtitles").color(ChatColor.RED).create());
            return 0;
        }

        selectedEntities.stream()
            .filter(VirtualEntity.class::isInstance)
            .map(VirtualEntity.class::cast)
            .forEach(VirtualEntity::clearSubtitles);

        source.sendMessage(new ComponentBuilder("Cleared subtitles").color(ChatColor.YELLOW).create());
        return 0;
    }

}
