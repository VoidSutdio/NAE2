package co.neeve.nae2.server.listeners;

import appeng.container.implementations.ContainerMEPortableCell;
import co.neeve.nae2.common.items.cells.ImprovedPortableCell;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.item.ItemStack;
import net.minecraftforge.event.entity.player.EntityItemPickupEvent;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

@Mod.EventBusSubscriber
public class AutoPickupItems {

    @SubscribeEvent
    public static void handleEntityItemPickup(EntityItemPickupEvent event) {
        EntityPlayer player = event.getEntityPlayer();
        if (player.openContainer instanceof ContainerMEPortableCell) {
            return;
        }

        InventoryPlayer inventory = player.inventory;
        for (int i = 0; i < inventory.getSizeInventory(); i++) {
            ItemStack stack = inventory.getStackInSlot(i);
            if (stack.getItem() instanceof ImprovedPortableCell cell && cell.isAutoPickupEnabled(stack)) {
                if (cell.onItemPickup(event, stack)) {
                    event.setCanceled(true);
                    return;
                }
            }
        }
    }

    @SubscribeEvent
    public static void handleBlockHarvestDrops(BlockEvent.HarvestDropsEvent event) {
        EntityPlayer player = event.getHarvester();
        if (player == null || player.openContainer instanceof ContainerMEPortableCell) {
            return;
        }

        InventoryPlayer inventory = player.inventory;
        for (int i = 0; i < inventory.getSizeInventory(); i++) {
            ItemStack stack = inventory.getStackInSlot(i);
            if (stack.getItem() instanceof ImprovedPortableCell cell && cell.isAutoPickupEnabled(stack)) {
                cell.onBlockHarvestDrops(event, stack);
                if (event.getDrops().isEmpty()) {
                    return;
                }
            }
        }
    }
}