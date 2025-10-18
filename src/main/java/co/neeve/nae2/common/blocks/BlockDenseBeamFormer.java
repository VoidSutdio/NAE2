package co.neeve.nae2.common.blocks;

import appeng.block.AEBaseTileBlock;
import appeng.helpers.AEGlassMaterial;
import appeng.helpers.ICustomCollision;
import appeng.util.Platform;
import co.neeve.nae2.common.tiles.TileDenseBeamFormer;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.properties.PropertyEnum;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.IStringSerializable;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;

public class BlockDenseBeamFormer extends AEBaseTileBlock implements ICustomCollision {

    public enum State implements IStringSerializable {
        OFF,
        ON,
        HAS_CHANNEL,
        BEAMING;

        @Override
        public @NotNull String getName() {
            return this.name().toLowerCase();
        }
    }

    public static final PropertyEnum<State> STATE = PropertyEnum.create("state", State.class);

    public BlockDenseBeamFormer() {
        super(AEGlassMaterial.INSTANCE);
        this.setTileEntity(TileDenseBeamFormer.class);
        this.setLightOpacity(0);
        this.setFullSize(false);
        this.setOpaque(false);
        this.setDefaultState(this.getDefaultState().withProperty(STATE, State.OFF));
    }

    @Override
    public void onBlockPlacedBy(World worldIn, BlockPos pos, IBlockState state, EntityLivingBase placer, ItemStack stack) {
        super.onBlockPlacedBy(worldIn, pos, state, placer, stack);

        TileDenseBeamFormer te = this.getTileEntity(worldIn, pos);
        if (te != null) {
            EnumFacing forward = EnumFacing.getDirectionFromEntityLiving(pos, placer);
            EnumFacing up = EnumFacing.UP;

            if (forward == EnumFacing.UP || forward == EnumFacing.DOWN) {
                up = EnumFacing.fromAngle(placer.rotationYaw);
            }

            te.setOrientation(forward, up);
        }
    }

    @Override
    public @NotNull BlockRenderLayer getRenderLayer() {
        return BlockRenderLayer.CUTOUT;
    }

    @Override
    public @NotNull IBlockState getActualState(@NotNull IBlockState state, @NotNull IBlockAccess worldIn, @NotNull BlockPos pos) {
        State teState = State.OFF;

        TileDenseBeamFormer te = this.getTileEntity(worldIn, pos);
        if (te != null) {
            if (te.isBeaming()) {
                teState = State.BEAMING;
            } else if (te.isActive()) {
                teState = State.HAS_CHANNEL;
            } else if (te.isPowered()) {
                teState = State.ON;
            }
        }

        return super.getActualState(state, worldIn, pos).withProperty(STATE, teState);
    }

    @Override
    protected IProperty[] getAEStates() {
        return new IProperty[]{STATE};
    }

    @Override
    public boolean onBlockActivated(World w, BlockPos pos, IBlockState state, EntityPlayer player, EnumHand hand, EnumFacing side, float hitX, float hitY, float hitZ) {
        TileDenseBeamFormer tile = this.getTileEntity(w, pos);

        if (tile != null && Platform.isWrench(player, player.getHeldItem(hand), pos)) {
            if (player.isSneaking()) {
                return super.onBlockActivated(w, pos, state, player, hand, side, hitX, hitY, hitZ);
            }


            boolean handled = tile.toggleBeamVisibility(player, hand);
            if (handled && !w.isRemote) {
                player.sendMessage(new TextComponentTranslation(tile.isBeamHidden() ? "nae2.part.beam_former.hide" : "nae2.part.beam_former.show"));
            }

            return true;
        }

        return super.onBlockActivated(w, pos, state, player, hand, side, hitX, hitY, hitZ);
    }

    @Override
    public boolean rotateBlock(World world, BlockPos pos, EnumFacing axis) {
        return false;
    }

    @Override
    public Iterable<AxisAlignedBB> getSelectedBoundingBoxesFromPool(World w, BlockPos pos, Entity thePlayer, boolean b) {
        TileDenseBeamFormer tile = this.getTileEntity(w, pos);
        if (tile != null) {
            EnumFacing forward = tile.getForward();

            double minX = 0;
            double minY = 0;
            double minZ = 0;
            double maxX = 1;
            double maxY = 1;
            double maxZ = 1;

            switch (forward) {
                case DOWN:
                    minZ = minX = 3.0 / 16.0;
                    maxZ = maxX = 13.0 / 16.0;
                    maxY = 1.0;
                    minY = 5.0 / 16.0;
                    break;
                case EAST:
                    minZ = minY = 3.0 / 16.0;
                    maxZ = maxY = 13.0 / 16.0;
                    maxX = 11.0 / 16.0;
                    minX = 0.0;
                    break;
                case NORTH:
                    minY = minX = 3.0 / 16.0;
                    maxY = maxX = 13.0 / 16.0;
                    maxZ = 1.0;
                    minZ = 5.0 / 16.0;
                    break;
                case SOUTH:
                    minY = minX = 3.0 / 16.0;
                    maxY = maxX = 13.0 / 16.0;
                    maxZ = 11.0 / 16.0;
                    minZ = 0.0;
                    break;
                case UP:
                    minZ = minX = 3.0 / 16.0;
                    maxZ = maxX = 13.0 / 16.0;
                    maxY = 11.0 / 16.0;
                    minY = 0.0;
                    break;
                case WEST:
                    minZ = minY = 3.0 / 16.0;
                    maxZ = maxY = 13.0 / 16.0;
                    maxX = 1.0;
                    minX = 5.0 / 16.0;
                    break;
                default:
                    break;
            }

            return Collections.singletonList(new AxisAlignedBB(minX, minY, minZ, maxX, maxY, maxZ));
        }
        return Collections.singletonList(new AxisAlignedBB(0.0, 0, 0.0, 1.0, 1.0, 1.0));
    }

    @Override
    public void addCollidingBlockToList(World w, BlockPos pos, AxisAlignedBB bb, List<AxisAlignedBB> out, Entity e) {
        TileDenseBeamFormer tile = this.getTileEntity(w, pos);
        if (tile != null) {
            EnumFacing forward = tile.getForward();

            double minX = 0;
            double minY = 0;
            double minZ = 0;
            double maxX = 1;
            double maxY = 1;
            double maxZ = 1;

            switch (forward) {
                case DOWN:
                    minZ = minX = 3.0 / 16.0;
                    maxZ = maxX = 13.0 / 16.0;
                    maxY = 1.0;
                    minY = 5.0 / 16.0;
                    break;
                case EAST:
                    minZ = minY = 3.0 / 16.0;
                    maxZ = maxY = 13.0 / 16.0;
                    maxX = 11.0 / 16.0;
                    minX = 0.0;
                    break;
                case NORTH:
                    minY = minX = 3.0 / 16.0;
                    maxY = maxX = 13.0 / 16.0;
                    maxZ = 1.0;
                    minZ = 5.0 / 16.0;
                    break;
                case SOUTH:
                    minY = minX = 3.0 / 16.0;
                    maxY = maxX = 13.0 / 16.0;
                    maxZ = 11.0 / 16.0;
                    minZ = 0.0;
                    break;
                case UP:
                    minZ = minX = 3.0 / 16.0;
                    maxZ = maxX = 13.0 / 16.0;
                    maxY = 11.0 / 16.0;
                    minY = 0.0;
                    break;
                case WEST:
                    minZ = minY = 3.0 / 16.0;
                    maxZ = maxY = 13.0 / 16.0;
                    maxX = 1.0;
                    minX = 5.0 / 16.0;
                    break;
                default:
                    break;
            }

            out.add(new AxisAlignedBB(minX, minY, minZ, maxX, maxY, maxZ));
        } else {
            out.add(new AxisAlignedBB(0.0, 0.0, 0.0, 1.0, 1.0, 1.0));
        }
    }

    @Override
    public boolean isFullCube(@NotNull IBlockState state) {
        return false;
    }

    @Override
    public int getLightValue(@NotNull IBlockState state, @NotNull IBlockAccess world, @NotNull BlockPos pos) {
        var te = this.getTileEntity(world, pos);
        if (te instanceof TileDenseBeamFormer beamFormer) {
            if (beamFormer.shouldRenderBeam() || beamFormer.isBeaming()) {
                return 15;
            }
        }
        return 0;
    }

    @Override
    public void breakBlock(World world, BlockPos pos, IBlockState state) {
        TileEntity te = world.getTileEntity(pos);
        if (te instanceof TileDenseBeamFormer f) {
            f.disconnect(null);
        }
        super.breakBlock(world, pos, state);
    }

}

