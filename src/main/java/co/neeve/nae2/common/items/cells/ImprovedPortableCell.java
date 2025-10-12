package co.neeve.nae2.common.items.cells;

import appeng.api.AEApi;
import appeng.api.config.Actionable;
import appeng.api.storage.ICellInventory;
import appeng.api.storage.ICellInventoryHandler;
import appeng.api.storage.channels.IItemStorageChannel;
import appeng.api.storage.data.IAEItemStack;
import appeng.api.storage.data.IItemList;
import appeng.core.Api;
import appeng.me.helpers.PlayerSource;
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
        if (cellHandler == null || cellHandler.getCellInv() == null) {
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

        ICellInventory<IAEItemStack> inv = cellHandler.getCellInv();
        if (!containsType(inv, aeItem)) {
            return false;
        }

        PlayerSource source = new PlayerSource(event.getEntityPlayer(), null);
        IAEItemStack overflow = inv.injectItems(aeItem, Actionable.SIMULATE, source);
        if (overflow != null) {
            return false;
        }

        try {
            inv.injectItems(aeItem, Actionable.MODULATE, source);
        } catch (Throwable t) {
            return false;
        }

        playPickupSound(event.getEntityPlayer());
        pickedItem.setCount(0);
        return true;
    }

    public void onBlockHarvestDrops(@NotNull BlockEvent.HarvestDropsEvent event, @NotNull ItemStack cellStack) {
        if (!isAutoPickupEnabled(cellStack) || event.getHarvester() == null) {
            return;
        }

        ICellInventoryHandler<IAEItemStack> cellHandler = getCellHandler(cellStack);
        if (cellHandler == null || cellHandler.getCellInv() == null) {
            return;
        }

        EntityPlayer player = event.getHarvester();
        List<ItemStack> drops = event.getDrops();
        ICellInventory<IAEItemStack> inv = cellHandler.getCellInv();

        for (int i = 0; i < drops.size(); i++) {
            ItemStack drop = drops.get(i);
            if (drop.isEmpty()) {
                continue;
            }

            AEItemStack aeDrop = AEItemStack.fromItemStack(drop);
            if (aeDrop == null || !cellHandler.canAccept(aeDrop)) {
                continue;
            }

            if (!containsType(inv, aeDrop)) {
                continue;
            }

            PlayerSource source = new PlayerSource(player, null);
            IAEItemStack overflow = inv.injectItems(aeDrop, Actionable.SIMULATE, source);
            if (overflow == null) {
                try {
                    inv.injectItems(aeDrop, Actionable.MODULATE, source);
                } catch (Throwable t) {
                    continue;
                }
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

    private boolean containsType(@NotNull ICellInventory<IAEItemStack> inv, @NotNull IAEItemStack probe) {
        try {
            IItemList<IAEItemStack> list = AEApi.instance()
                    .storage()
                    .getStorageChannel(IItemStorageChannel.class)
                    .createList();
            inv.getAvailableItems(list);
            if (list.findPrecise(probe) != null) {
                return true;
            }
            for (IAEItemStack s : list) {
                if (s != null && s.isSameType(probe)) {
                    return true;
                }
            }
            return false;
        } catch (Throwable t) {
            return false;
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