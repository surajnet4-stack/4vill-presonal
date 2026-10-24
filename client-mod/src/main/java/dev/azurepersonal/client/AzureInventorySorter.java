package dev.azurepersonal.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.text.Text;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.List;

public final class AzureInventorySorter {
    private record Swap(int first, int second) {
    }

    private static final Deque<Swap> PENDING_SWAPS = new ArrayDeque<>();
    private static int syncId = -1;
    private static int tickDelay;

    private AzureInventorySorter() {
    }

    public static void sort(MinecraftClient client) {
        if (client.player == null || client.interactionManager == null ||
            !(client.currentScreen instanceof InventoryScreen)) return;
        if (!client.player.currentScreenHandler.getCursorStack().isEmpty()) {
            client.inGameHud.setOverlayMessage(Text.literal("Put the carried item down before sorting."), false);
            return;
        }
        if (!PENDING_SWAPS.isEmpty()) {
            client.inGameHud.setOverlayMessage(Text.literal("Inventory sorting is already running."), false);
            return;
        }

        List<Integer> slotIds = new ArrayList<>();
        List<Integer> currentOrder = new ArrayList<>();
        List<Integer> desiredOrder = new ArrayList<>();
        for (int slot = 9; slot <= 35; slot++) {
            slotIds.add(slot);
            currentOrder.add(slot - 9);
            desiredOrder.add(slot - 9);
        }

        desiredOrder.sort(Comparator
            .comparing((Integer index) -> sortKey(client, slotIds.get(index)))
            .thenComparingInt(index -> -client.player.currentScreenHandler.getSlot(slotIds.get(index)).getStack().getCount()));
        for (int target = 0; target < desiredOrder.size(); target++) {
            int wanted = desiredOrder.get(target);
            int source = currentOrder.indexOf(wanted);
            if (source == target) continue;
            PENDING_SWAPS.addLast(new Swap(slotIds.get(target), slotIds.get(source)));
            currentOrder.set(source, currentOrder.get(target));
            currentOrder.set(target, wanted);
        }

        if (PENDING_SWAPS.isEmpty()) {
            client.inGameHud.setOverlayMessage(Text.literal("Inventory is already sorted."), false);
            return;
        }
        syncId = client.player.currentScreenHandler.syncId;
        tickDelay = 0;
        client.inGameHud.setOverlayMessage(Text.literal("Sorting inventory…"), false);
    }

    private static String sortKey(MinecraftClient client, int slot) {
        ItemStack stack = client.player.currentScreenHandler.getSlot(slot).getStack();
        if (stack.isEmpty()) return "\uffff";
        return Registries.ITEM.getId(stack.getItem()) + "|" +
            (stack.hasNbt() ? stack.getNbt().toString() : "");
    }

    public static void tick(MinecraftClient client) {
        if (PENDING_SWAPS.isEmpty()) return;
        if (client.player == null || client.interactionManager == null ||
            !(client.currentScreen instanceof InventoryScreen) ||
            client.player.currentScreenHandler.syncId != syncId ||
            !client.player.currentScreenHandler.getCursorStack().isEmpty()) {
            PENDING_SWAPS.clear();
            syncId = -1;
            return;
        }
        if (tickDelay++ < 1) return;
        tickDelay = 0;
        Swap swap = PENDING_SWAPS.removeFirst();
        int handlerId = client.player.currentScreenHandler.syncId;
        client.interactionManager.clickSlot(handlerId, swap.first(), 0, SlotActionType.PICKUP, client.player);
        client.interactionManager.clickSlot(handlerId, swap.second(), 0, SlotActionType.PICKUP, client.player);
        client.interactionManager.clickSlot(handlerId, swap.first(), 0, SlotActionType.PICKUP, client.player);
        if (PENDING_SWAPS.isEmpty()) {
            syncId = -1;
            client.inGameHud.setOverlayMessage(Text.literal("Inventory sorted."), false);
        }
    }
}
