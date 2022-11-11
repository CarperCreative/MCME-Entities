package com.mcmiddleearth.entities.entities.composite.animation;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.mcmiddleearth.entities.EntitiesPlugin;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import org.bukkit.Material;

public class BakedAnimationManager {

    private static final Map<String, BakedAnimationTree> animations = new HashMap<>();

    public static void load() {
        final List<String> files = getDataFiles();

        for (final String key : files) {
            final File animationFile = new File(EntitiesPlugin.getAnimationFolder(), key + ".json");

            animations.put(key, loadAnimation(key, animationFile));
        }
    }

    public static BakedAnimationTree loadAnimation(String key, File file) {
        final BakedAnimationTree animationTree = new BakedAnimationTree();

        try (final FileReader reader = new FileReader(file)) {
            final JsonObject data = new JsonParser().parse(reader).getAsJsonObject();

            final JsonObject modelData = data.get("model").getAsJsonObject();
            final Material itemMaterial = Material.valueOf(modelData.get("head_item").getAsString().toUpperCase());
            final JsonObject animationData = data.get("animations").getAsJsonObject();

            animationData.entrySet().forEach(entry -> {
                final String[] split;
                if (entry.getKey().contains(key + ".")) {
                    split = entry.getKey().split(key + "\\.");
                } else {
                    split = entry.getKey().split("animations\\.");
                }
                final String animationKey;
                if (split.length > 1) {
                    animationKey = split[1];
                } else {
                    animationKey = entry.getKey();
                }
                String animationName = animationKey;

                final int lastDot = animationName.lastIndexOf('.');
                if (lastDot > 0) {
                    final String lastKeyPart = animationName.substring(lastDot + 1);
                    if (lastKeyPart.matches("^\\d+$")) {
                        animationName = animationName.substring(0, lastDot);
                    }
                }

                animationTree.addAnimation(
                    animationName,
                    createAnimation(entry.getValue().getAsJsonObject(),
                        itemMaterial, animationKey, animationName
                    )
                );
            });
        } catch (IOException | JsonParseException | IllegalStateException e) {
            e.printStackTrace();
        }

        return animationTree;
    }

    public static BakedAnimationTree loadAnimation(String animationFile) {
        return loadAnimation(animationFile, new File(EntitiesPlugin.getAnimationFolder(), animationFile + ".json"));
    }

    public static BakedAnimationTree getAnimation(String animationFile) {
        return animations.get(animationFile);
    }

    public static List<String> getDataFiles() {
        return Arrays.stream(
                Objects.requireNonNull(EntitiesPlugin.getAnimationFolder().listFiles((dir, name) -> name.endsWith(".json")))
            )
            .map(file -> file.getName().substring(0, file.getName().lastIndexOf('.')))
            .collect(Collectors.toList());
    }

    private static BakedAnimation createAnimation(JsonObject data, Material itemMaterial, String name, String animationName) {
        BakedAnimationType type;
        try {
            type = BakedAnimationType.valueOf(data.get("loop").getAsString().toUpperCase());
        } catch (IllegalArgumentException ex) {
            type = BakedAnimationType.ONCE;
        }
        final int interval = (data.get("interval") == null ? 1 : data.get("interval").getAsInt());
        final String next = (data.has("next") ? data.get("next").getAsString() : null);
        final BakedAnimation animation = new BakedAnimation(type, name, animationName, next, interval);
        final JsonArray frameData = data.get("frames").getAsJsonArray();

        for (int i = 0; i < frameData.size(); i++) {
            animation.addFrame(Frame.loadFrame(frameData.get(i).getAsJsonObject(), itemMaterial));
        }
        return animation;
    }

}
