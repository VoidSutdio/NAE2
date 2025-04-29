package co.neeve.nae2.common.items.cells;

import appeng.api.AEApi;
import appeng.api.config.Actionable;
import appeng.api.storage.ICellInventoryHandler;
import appeng.api.storage.channels.IItemStorageChannel;
import appeng.api.storage.data.IAEItemStack;
import appeng.core.Api;
import appeng.util.item.AEItemStack;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.ItemStack;
import net.minecraft.util.SoundCategory;
import net.minecraftforge.event.entity.player.EntityItemPickupEvent;
import net.minecraftforge.event.world.BlockEvent;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Random;

public class ImprovedPortableCell extends DensePortableCell {
    private static final float PICKUP_SOUND_VOLUME = 0.08F;
    private static final float PICKUP_PITCH_VARIATION = 0.7F;
    private static final Random rand = new Random();

    public ImprovedPortableCell(double batteryCapacity, int bytes, int bytesPerType, double idleDrain) {
        super(batteryCapacity, bytes, bytesPerType, idleDrain);
    }

    @NotNull
    @Override
    public IItemStorageChannel getChannel() {
        return Api.INSTANCE.storage().getStorageChannel(IItemStorageChannel.class);
    }

    public boolean isAutoPickupEnabled(@NotNull ItemStack cell) {
        return cell.hasTagCompound() && cell.getTagCompound() != null && cell.getTagCompound().getBoolean("autoPickup");
    }

    public boolean onItemPickup(@NotNull EntityItemPickupEvent event, @NotNull ItemStack cellStack) {
        if (!isAutoPickupEnabled(cellStack)) {
            return false;
        }

        ICellInventoryHandler<IAEItemStack> cellHandler = getCellHandler(cellStack);
        if (cellHandler == null) {
            return false;
        }

        ItemStack pickedItem = event.getItem().getItem();
        if (pickedItem.isEmpty()) {
            return false;
        }

        AEItemStack aeItem = AEItemStack.fromItemStack(pickedItem);
        if (aeItem == null || !cellHandler.canAccept(aeItem)) {
            return false;
        }

        IAEItemStack overflow = cellHandler.injectItems(aeItem, Actionable.SIMULATE, null);
        if (overflow != null) {
            return false;
        }

        cellHandler.injectItems(aeItem, Actionable.MODULATE, null);
        playPickupSound(event.getEntityPlayer());
        pickedItem.setCount(0);
        return true;
    }

    public void onBlockHarvestDrops(@NotNull BlockEvent.HarvestDropsEvent event, @NotNull ItemStack cellStack) {
        if (!isAutoPickupEnabled(cellStack) || event.getHarvester() == null) {
            return;
        }

        ICellInventoryHandler<IAEItemStack> cellHandler = getCellHandler(cellStack);
        if (cellHandler == null) {
            return;
        }

        EntityPlayer player = event.getHarvester();
        List<ItemStack> drops = event.getDrops();

        for (int i = 0; i < drops.size(); i++) {
            ItemStack drop = drops.get(i);
            if (drop.isEmpty()) {
                continue;
            }

            AEItemStack aeDrop = AEItemStack.fromItemStack(drop);
            if (aeDrop == null || !cellHandler.canAccept(aeDrop)) {
                continue;
            }

            IAEItemStack overflow = cellHandler.injectItems(aeDrop, Actionable.SIMULATE, null);
            if (overflow == null) {
                cellHandler.injectItems(aeDrop, Actionable.MODULATE, null);
                playPickupSound(player);
                drops.remove(i--);
            }
        }

        if (drops.isEmpty()) {
            event.getDrops().clear();
        }
    }

    @Nullable
    private ICellInventoryHandler<IAEItemStack> getCellHandler(@NotNull ItemStack cellStack) {
        try {
            return AEApi.instance()
                    .registries()
                    .cell()
                    .getCellInventory(cellStack, null, getChannel());
        } catch (Exception e) {
            return null;
        }
    }

    private void playPickupSound(@NotNull EntityPlayer player) {
        float pitch = (rand.nextFloat() - rand.nextFloat()) * PICKUP_PITCH_VARIATION + 1.0F;
        player.world.playSound(
                null,
                player.posX,
                player.posY,
                player.posZ,
                SoundEvents.ENTITY_ITEM_PICKUP,
                SoundCategory.PLAYERS,
                PICKUP_SOUND_VOLUME,
                pitch
        );
    }
}