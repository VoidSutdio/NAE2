package co.neeve.nae2.common.tiles;

import appeng.tile.crafting.TileCraftingStorageTile;
import co.neeve.nae2.common.blocks.BlockDenseCraftingUnit;
import co.neeve.nae2.common.interfaces.IDenseCoProcessor;
import net.minecraft.block.Block;
import net.minecraft.item.ItemStack;

import javax.annotation.Nullable;

public class TileDenseCraftingUnit extends TileCraftingStorageTile implements IDenseCoProcessor {
	@Override
	protected ItemStack getItemFromTile(Object obj) {
		if (this.world != null && !this.notLoaded() && !this.isInvalid()) {
			Block block = this.world.getBlockState(this.pos).getBlock();
			if (!(block instanceof BlockDenseCraftingUnit unit)) {
				return ItemStack.EMPTY;
			}
            return unit.getType().getBlock().maybeStack(1).orElse(ItemStack.EMPTY);
		} else {
			return ItemStack.EMPTY;
		}
	}

	@Override
	public boolean isAccelerator() {
		return false;
	}

	@Override
	public boolean isStorage() {
		var block = this.getBlock();
		if (block != null) {
			return block.getType().getBytes() > 0;
		}
		return false;
	}

	public int getStorageBytes() {
		var block = this.getBlock();
		if (block != null) {
			return block.getType().getBytes();
		}
		return 0;
	}

	public @Nullable BlockDenseCraftingUnit getBlock() {
		if (this.world != null && !this.notLoaded() && !this.isInvalid()) {
			Block block = this.world.getBlockState(this.pos).getBlock();
			if (!(block instanceof BlockDenseCraftingUnit)) {
				return null;
			}
			return (BlockDenseCraftingUnit) block;
		} else {
			return null;
		}
	}

	@Override
	public int getAccelerationFactor() {
		var block = this.getBlock();
		if (block != null) {
			return block.getType().getAccelerationFactor();
		}
		return 0;
	}
}
