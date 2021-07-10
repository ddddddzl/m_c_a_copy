package mca.util;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ArmorItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

public class InventoryUtils {
    public static int getFirstSlotContainingItem(SimpleInventory inv, Predicate<ItemStack> predicate) {
        for (int i = 0; i < inv.size(); i++) {
            ItemStack stack = inv.getStack(i);
            if (!predicate.test(stack)) continue;
            return i;
        }
        return -1;
    }

    public static boolean contains(SimpleInventory inv, Class<?> clazz) {
        for (int i = 0; i < inv.size(); ++i) {
            final ItemStack stack = inv.getStack(i);
            final Item item = stack.getItem();

            if (item.getClass() == clazz) return true;
        }
        return false;
    }

    /**
     * Gets the best quality (max damage) item of the specified type that is in the inventory.
     *
     * @param type The class of item that will be returned.
     * @return The item stack containing the item of the specified type with the highest max damage.
     */
    public static ItemStack getBestItemOfType(SimpleInventory inv, @Nullable Class<?> type) {
        if (type == null) return ItemStack.EMPTY;
        else return inv.getStack(getBestItemOfTypeSlot(inv, type));
    }

    public static ItemStack getBestArmorOfType(SimpleInventory inv, EquipmentSlot slot) {
        ItemStack returnStack = ItemStack.EMPTY;

        List<ItemStack> armors = new ArrayList<>();
        for (int i = 0; i < inv.size(); ++i) {
            ItemStack stack = inv.getStack(i);
            if (stack.getItem() instanceof ArmorItem) {
                ArmorItem armor = (ArmorItem) stack.getItem();
                EquipmentSlot slotOfArmor = armor.getSlotType();
                if (slotOfArmor == slot) {
                    armors.add(stack);
                }
            }
        }

        int highestMaxDamage = 0;
        for (ItemStack stack : armors) {
            if (stack.getMaxDamage() > highestMaxDamage) {
                returnStack = stack;
                highestMaxDamage = stack.getMaxDamage();
            }
        }
        return returnStack;
    }

    public static int getBestItemOfTypeSlot(SimpleInventory inv, Class<?> type) {
        int highestMaxDamage = 0;
        int best = -1;

        for (int i = 0; i < inv.size(); ++i) {
            ItemStack stackInInventory = inv.getStack(i);

            final String itemClassName = stackInInventory.getItem().getClass().getName();

            if (itemClassName.equals(type.getName()) && highestMaxDamage < stackInInventory.getMaxDamage()) {
                highestMaxDamage = stackInInventory.getMaxDamage();
                best = i;
            }
        }

        return best;
    }

    public static void dropAllItems(Entity entity, SimpleInventory inv) {
        for (int i = 0; i < inv.size(); i++) {
            ItemStack stack = inv.getStack(i);
            entity.dropStack(stack, 1.0F);
        }
        inv.clear();
    }

    public static void load(SimpleInventory inv, NbtList tagList) {
        for (int i = 0; i < inv.size(); ++i) {
            inv.setStack(i, ItemStack.EMPTY);
        }

        for (int i = 0; i < tagList.size(); ++i) {
            NbtCompound nbt = tagList.getCompound(i);
            int slot = nbt.getByte("Slot") & 255;

            if (slot < inv.size()) {
                inv.setStack(slot, ItemStack.fromNbt(nbt));
            }
        }
    }

    public static NbtList save(SimpleInventory inv) {
        NbtList tagList = new NbtList();

        for (int i = 0; i < inv.size(); ++i) {
            ItemStack itemstack = inv.getStack(i);

            if (itemstack != ItemStack.EMPTY) {
                NbtCompound nbt = new NbtCompound();
                nbt.putByte("Slot", (byte) i);
                itemstack.setTag(nbt);
                tagList.add(nbt);
            }
        }

        return tagList;
    }

    public static void saveToNBT(SimpleInventory inv, NbtCompound nbt) {
        nbt.put("Inventory", inv.toNbtList());
    }

    public static void readFromNBT(SimpleInventory inv, NbtCompound nbt) {
        inv.readNbtList(nbt.getList("Inventory", 10));
    }
}