package co.neeve.nae2.common.items.cells;

import appeng.api.AEApi;
import appeng.api.config.FuzzyMode;
import appeng.api.implementations.guiobjects.IGuiItem;
import appeng.api.implementations.guiobjects.IGuiItemObject;
import appeng.api.implementations.items.IItemGroup;
import appeng.api.implementations.items.IStorageCell;
import appeng.api.storage.ICellInventoryHandler;
import appeng.api.storage.IStorageChannel;
import appeng.api.storage.channels.IItemStorageChannel;
import appeng.api.storage.data.IAEItemStack;
import appeng.api.util.AEPartLocation;
import appeng.core.localization.GuiText;
import appeng.core.sync.GuiBridge;
import appeng.items.contents.CellConfig;
import appeng.items.contents.CellUpgrades;
import appeng.items.contents.PortableCellViewer;
import appeng.items.tools.powered.powersink.AEBasePoweredItem;
import appeng.util.Platform;
import co.neeve.nae2.common.sync.NAE2Tooltip;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.items.IItemHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Set;

public class DensePortableCell extends AEBasePoweredItem implements IStorageCell<IAEItemStack>, IGuiItem, IItemGroup {
    private static final int TOTAL_TYPES = 63;
    private static final String AUTO_PICKUP_KEY = "autoPickup";
    private static final String FUZZY_MODE_KEY = "FuzzyMode";

    protected final int capacity;
    protected final double idleDrain;
    protected final int bytesPerType;

    public DensePortableCell(double batteryCapacity, int bytes, int bytesPerType, double idleDrain) {
        super(batteryCapacity);
        this.capacity = bytes;
        this.bytesPerType = bytesPerType;
        this.idleDrain = idleDrain;

    }

    @Override
    public @NotNull ActionResult<ItemStack> onItemRightClick(@NotNull World world, @NotNull EntityPlayer player, @NotNull EnumHand hand) {
        ItemStack item = player.getHeldItem(hand);
        if (player.isSneaking()) {
            toggleAutoPickup(player, item);
            return ActionResult.newResult(EnumActionResult.SUCCESS, item);
        }

        Platform.openGUI(player, null, AEPartLocation.INTERNAL, GuiBridge.GUI_PORTABLE_CELL);
        return ActionResult.newResult(EnumActionResult.SUCCESS, item);
    }

    @SideOnly(Side.CLIENT)
    @Override
    public void addCheckedInformation(@NotNull ItemStack stack, @NotNull World world, @NotNull List<String> lines, ITooltipFlag advancedTooltips) {
        super.addCheckedInformation(stack, world, lines, advancedTooltips);

        boolean isAutoPickupEnabled = isAutoPickupEnabled(stack);
        lines.add(NAE2Tooltip.AUTO_PICKUP.getLocalWithSpaceAtEnd() + getAutoPickupState(isAutoPickupEnabled));
        lines.add(NAE2Tooltip.AUTO_PICKUP_HOW_TO_ENABLE.getLocal());
        lines.add(NAE2Tooltip.AUTO_PICKUP_TIP.getLocal());

        addCellInformation(stack, lines);
    }

    @Override
    public int getBytes(@NotNull ItemStack cellItem) {
        return capacity;
    }

    @Override
    public int getBytesPerType(@NotNull ItemStack cellItem) {
        return bytesPerType;
    }

    @Override
    public int getTotalTypes(@NotNull ItemStack cellItem) {
        return TOTAL_TYPES;
    }

    @Override
    public boolean isBlackListed(@NotNull ItemStack cellItem, @NotNull IAEItemStack requestedAddition) {
        return false;
    }

    @Override
    public boolean storableInStorageCell() {
        return false;
    }

    @Override
    public boolean isStorageCell(@NotNull ItemStack item) {
        return true;
    }

    @Override
    public double getIdleDrain() {
        return idleDrain;
    }

    @NotNull
    @Override
    public IStorageChannel<IAEItemStack> getChannel() {
        return AEApi.instance().storage().getStorageChannel(IItemStorageChannel.class);
    }

    @Override
    public String getUnlocalizedGroupName(Set<ItemStack> others, ItemStack is) {
        return GuiText.StorageCells.getUnlocalized();
    }

    @Override
    public boolean isEditable(@NotNull ItemStack is) {
        return true;
    }

    @NotNull
    @Override
    public IItemHandler getUpgradesInventory(@NotNull ItemStack is) {
        return new CellUpgrades(is, 2);
    }

    @NotNull
    @Override
    public IItemHandler getConfigInventory(@NotNull ItemStack is) {
        return new CellConfig(is);
    }

    @NotNull
    @Override
    public FuzzyMode getFuzzyMode(@NotNull ItemStack is) {
        String fz = getNbtData(is).getString(FUZZY_MODE_KEY);
        return parseFuzzyMode(fz);
    }

    @Override
    public void setFuzzyMode(@NotNull ItemStack is, @NotNull FuzzyMode fzMode) {
        getNbtData(is).setString(FUZZY_MODE_KEY, fzMode.name());
    }

    @NotNull
    @Override
    public IGuiItemObject getGuiObject(@NotNull ItemStack is, @NotNull World world, @NotNull BlockPos pos) {
        return new PortableCellViewer(is, pos.getX());
    }

    @Override
    public boolean shouldCauseReequipAnimation(@NotNull ItemStack oldStack, @NotNull ItemStack newStack, boolean slotChanged) {
        return slotChanged;
    }


    private void toggleAutoPickup(@NotNull EntityPlayer player, @NotNull ItemStack item) {
        NBTTagCompound nbt = getNbtData(item);
        boolean currentState = nbt.getBoolean(AUTO_PICKUP_KEY);
        boolean newState = !currentState;
        nbt.setBoolean(AUTO_PICKUP_KEY, newState);
        if (player.world.isRemote) {
            player.sendStatusMessage(new TextComponentString(
                    NAE2Tooltip.AUTO_PICKUP.getLocalWithSpaceAtEnd() + getAutoPickupState(newState)), true);
        }    }

    private boolean isAutoPickupEnabled(@NotNull ItemStack item) {
        NBTTagCompound nbt = item.getTagCompound();
        return nbt != null && nbt.getBoolean(AUTO_PICKUP_KEY);
    }

    @NotNull
    private String getAutoPickupState(boolean isEnabled) {
        return isEnabled ? NAE2Tooltip.ENABLED_LOWERCASE.getLocal() : NAE2Tooltip.DISABLED_LOWERCASE.getLocal();
    }

    @NotNull
    private NBTTagCompound getNbtData(@NotNull ItemStack item) {
        return Platform.openNbtData(item);
    }

    @SideOnly(Side.CLIENT)
    @Override
    public boolean isFull3D() {
        return false;
    }

    @SideOnly(Side.CLIENT)
    private void addCellInformation(@NotNull ItemStack stack, @NotNull List<String> lines) {
        ICellInventoryHandler<IAEItemStack> cellInventory = AEApi.instance()
                .registries()
                .cell()
                .getCellInventory(stack, null, getChannel());
        AEApi.instance().client().addCellInformation(cellInventory, lines);
    }

    @NotNull
    private FuzzyMode parseFuzzyMode(@Nullable String fz) {
        try {
            return fz != null && !fz.isEmpty() ? FuzzyMode.valueOf(fz) : FuzzyMode.IGNORE_ALL;
        } catch (IllegalArgumentException e) {
            return FuzzyMode.IGNORE_ALL;
        }
    }
}