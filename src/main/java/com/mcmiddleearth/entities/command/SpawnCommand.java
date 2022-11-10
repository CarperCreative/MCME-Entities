package com.mcmiddleearth.entities.command;

import com.mcmiddleearth.command.McmeCommandSender;
import com.mcmiddleearth.command.builder.HelpfulLiteralBuilder;
import com.mcmiddleearth.command.builder.HelpfulRequiredArgumentBuilder;
import com.mcmiddleearth.entities.Permission;
import com.mcmiddleearth.entities.api.EntityAPI;
import com.mcmiddleearth.entities.api.VirtualEntityFactory;
import com.mcmiddleearth.entities.command.argument.AnimationFileArgument;
import com.mcmiddleearth.entities.command.argument.EntityTypeArgument;
import com.mcmiddleearth.entities.command.argument.GoalTypeArgument;
import com.mcmiddleearth.entities.entities.McmeEntity;
import com.mcmiddleearth.entities.entities.Projectile;
import com.mcmiddleearth.entities.entities.RealPlayer;
import com.mcmiddleearth.entities.entities.VirtualEntity;
import com.mcmiddleearth.entities.exception.InvalidDataException;
import com.mcmiddleearth.entities.exception.InvalidLocationException;
import net.md_5.bungee.api.chat.ComponentBuilder;
import org.bukkit.Location;

import static com.mojang.brigadier.arguments.StringArgumentType.word;

public class SpawnCommand extends McmeEntitiesCommandHandler {

    public SpawnCommand(String command) {
        super(command);
    }

    @Override
    protected HelpfulLiteralBuilder createCommandTree(HelpfulLiteralBuilder commandNodeBuilder) {
        commandNodeBuilder
            .requires(sender -> (sender instanceof RealPlayer)
                                && ((RealPlayer) sender).getBukkitPlayer().hasPermission(Permission.USER.getNode()))
            .then(HelpfulRequiredArgumentBuilder.argument("type", new EntityTypeArgument())
                .executes(context -> this.spawnEntity(context.getSource(),
                        context.getArgument("type", String.class),
                        null, null
                    )
                )
                .then(HelpfulRequiredArgumentBuilder.argument("goal", new GoalTypeArgument())
                    .executes(context -> this.spawnEntity(context.getSource(),
                            context.getArgument("type", String.class),
                            context.getArgument("goal", String.class), null
                        )
                    )
                    .then(HelpfulRequiredArgumentBuilder.argument("dataFile", new AnimationFileArgument())
                        .executes(context -> this.spawnEntity(context.getSource(),
                                context.getArgument("type", String.class),
                                context.getArgument("goal", String.class),
                                context.getArgument("dataFile", String.class)
                            )
                        )
                        .then(HelpfulLiteralBuilder.literal("loop")
                            .executes(context -> this.spawnEntity(context.getSource(),
                                    context.getArgument("type", String.class),
                                    null,
                                    context.getArgument("goal", String.class),
                                    context.getArgument("dataFile", String.class),
                                    true
                                )
                            )
                            .then(this.nameArgument(true))
                        )
                        .then(this.nameArgument())
                    )
                )
            );
        return commandNodeBuilder;
    }

    private int spawnEntity(McmeCommandSender sender, String type, String goal, String dataFile) {
        return this.spawnEntity(sender, type, null, goal, dataFile, false);
    }

    private int spawnEntity(McmeCommandSender sender, String type, String name, String goal, String dataFile, boolean loop) {
        final VirtualEntityFactory factory = this.getFactory(sender, type, name, goal, dataFile).withShooter((McmeEntity) sender);
        final Location loc = factory.getLocation();
        final McmeEntity spawnLocationEntity = factory.getSpawnLocationEntity();
        if (type.equalsIgnoreCase("arrow") && factory.getGoalFactory() != null) {
            if (factory.getGoalFactory().getTargetEntity() != null) {
                Projectile.takeAim(factory, factory.getGoalFactory().getTargetEntity().getLocation());
            } else if (factory.getGoalFactory().getTargetLocation() != null) {
                Projectile.takeAim(factory, factory.getGoalFactory().getTargetLocation());
            }
        }

        if (loop && factory.getGoalFactory() != null) {
            factory.getGoalFactory().withLoop(true);
        }

        try {
            final VirtualEntity entity = (VirtualEntity) EntityAPI.spawnEntity(factory);

            ((BukkitCommandSender) sender).setSelectedEntities(entity);
            sender.sendMessage(new ComponentBuilder("Spawning: " + type).create());
        } catch (InvalidLocationException e) {
            sender.sendMessage(new ComponentBuilder("Can't spawn because of invalid or missing location!").create());
        } catch (InvalidDataException e) {
            sender.sendMessage(new ComponentBuilder(e.getMessage()).create());
        }
        factory.withLocation(loc).withEntityForSpawnLocation(spawnLocationEntity);
        return 0;
    }

    private HelpfulRequiredArgumentBuilder<String> nameArgument() {
        return this.nameArgument(false);
    }

    private HelpfulRequiredArgumentBuilder<String> nameArgument(boolean loop) {
        return HelpfulRequiredArgumentBuilder.argument("name", word())
            .executes(context -> this.spawnEntity(context.getSource(),
                    context.getArgument("type", String.class),
                    context.getArgument("name", String.class),
                    context.getArgument("goal", String.class),
                    context.getArgument("dataFile", String.class),
                    loop
                )
            );
    }
}
