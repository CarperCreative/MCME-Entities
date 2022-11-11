package com.mcmiddleearth.entities.command;

import com.mcmiddleearth.command.builder.HelpfulLiteralBuilder;
import com.mcmiddleearth.entities.EntitiesPlugin;
import com.mcmiddleearth.entities.Permission;
import com.mcmiddleearth.entities.ai.movement.MovementEngine;
import com.mcmiddleearth.entities.entities.RealPlayer;
import net.md_5.bungee.api.chat.ComponentBuilder;

public class CollisionsCommand extends McmeEntitiesCommandHandler {

    public CollisionsCommand(String command) {
        super(command);
    }

    @Override
    protected HelpfulLiteralBuilder createCommandTree(HelpfulLiteralBuilder commandNodeBuilder) {
        commandNodeBuilder
            .requires(sender -> (sender instanceof RealPlayer)
                                && ((RealPlayer) sender).getBukkitPlayer().hasPermission(Permission.USER.getNode()))
            .executes(context -> {
                final boolean collisionsEnabled = !MovementEngine.collisionsEnabled;
                MovementEngine.collisionsEnabled = collisionsEnabled;

                if (collisionsEnabled) {
                    context.getSource().sendMessage(new ComponentBuilder("The collisions have been enabled").create());
                } else {
                    context.getSource().sendMessage(new ComponentBuilder("The collisions have been disabled").create());
                }

                final EntitiesPlugin instance = EntitiesPlugin.getInstance();

                instance.getConfig().set("collisions-enabled", collisionsEnabled);
                instance.saveConfig();
                return 0;
            });
        return commandNodeBuilder;
    }

}
