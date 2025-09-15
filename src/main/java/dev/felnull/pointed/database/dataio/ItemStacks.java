package dev.felnull.pointed.database.dataio;

import org.bukkit.inventory.ItemStack;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public final class ItemStacks {
    public static byte[] toBytes(ItemStack item) throws IOException {
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream();
             BukkitObjectOutputStream oos = new BukkitObjectOutputStream(bos)) {
            oos.writeObject(item);
            oos.flush();
            return bos.toByteArray();
        }
    }

    public static ItemStack fromBytes(byte[] bytes) throws IOException, ClassNotFoundException {
        try (ByteArrayInputStream bis = new ByteArrayInputStream(bytes);
             BukkitObjectInputStream ois = new BukkitObjectInputStream(bis)) {
            return (ItemStack) ois.readObject();
        }
    }

    public static List<byte[]> toBytesList(List<ItemStack> list) throws IOException {
        List<byte[]> out = new ArrayList<>(list.size());
        for (ItemStack is : list) out.add(toBytes(is));
        return out;
    }

    public static List<ItemStack> fromBytesList(List<byte[]> list) throws IOException, ClassNotFoundException {
        List<ItemStack> out = new ArrayList<>(list.size());
        for (byte[] b : list) out.add(fromBytes(b));
        return out;
    }
}
