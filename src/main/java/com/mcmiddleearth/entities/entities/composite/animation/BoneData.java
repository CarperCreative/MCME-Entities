package com.mcmiddleearth.entities.entities.composite.animation;

import com.comphenix.protocol.wrappers.Pair;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.util.EulerAngle;
import org.bukkit.util.Vector;

public class BoneData {

    private final EulerAngle headPose;

    private final Vector position;

    private final ItemStack[] items;
    private final Map<String, Integer> states;

    private static final double Y_SHIFT = 0.5;

    public BoneData(EulerAngle headPose, Vector position, ItemStack[] items, Map<String, Integer> states) {
        //Logger.getGlobal().info("HeadPose: "+headPose.getX()+" "+headPose.getY()+" "+headPose.getZ());
        this.headPose = headPose;//RotationMatrix.rotateXEulerAngleDegree(headPose,45);
        //Logger.getGlobal().info("HeadPose: "+this.headPose.getX()+" "+this.headPose.getY()+" "+this.headPose.getZ());
        this.position = position;//RotationMatrix.fastRotateX(position,45);
        this.items = items;
        this.states = states;
    }

    public EulerAngle getHeadPose() {
        return headPose;
    }

    public Vector getPosition() {
        return position;
    }

    public ItemStack[] getItems() {
        return items;
    }

    public Map<String, Integer> getStates() {
        return this.states;
    }

    public static BoneData loadBoneData(JsonObject data, Material itemMaterial) {
        final Pair<ItemStack[], Map<String, Integer>> itemsAndStates = readItemsAndStates(data.get("cmd").getAsJsonObject(), itemMaterial);

        return new BoneData(
            readAngle(data.get("rot").getAsJsonArray()),
            readPosition(data.get("pos").getAsJsonArray()),
            itemsAndStates.getFirst(),
            itemsAndStates.getSecond()
        );
    }

    private static Pair<ItemStack[], Map<String,Integer>> readItemsAndStates(JsonObject data, Material itemMaterial) {
        Set<Map.Entry<String, JsonElement>> entries = data.entrySet();
        ItemStack[] result = new ItemStack[entries.size()];
        Map<String,Integer> states = new HashMap<>();

        entries.forEach(entry-> {
            ItemStack item = new ItemStack(itemMaterial);
            ItemMeta meta = item.getItemMeta();
            Integer stateId = states.computeIfAbsent(entry.getKey(), k -> states.size());

            meta.setCustomModelData(entry.getValue().getAsInt());
            item.setItemMeta(meta);
            result[stateId] = item;
        });
        return new Pair<>(result, states);
    }

    private static EulerAngle readAngle(JsonArray data) {
        return new EulerAngle(data.get(0).getAsDouble(),data.get(1).getAsDouble(),data.get(2).getAsDouble());
    }
    private static Vector readPosition(JsonArray data) {
        return new Vector(data.get(0).getAsDouble(),data.get(1).getAsDouble()+Y_SHIFT,data.get(2).getAsDouble());
    }
}
