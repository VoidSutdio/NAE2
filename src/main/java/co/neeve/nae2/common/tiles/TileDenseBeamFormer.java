
package co.neeve.nae2.common.tiles;

import appeng.api.AEApi;
import appeng.api.exceptions.FailedConnectionException;
import appeng.api.implementations.IPowerChannelState;
import appeng.api.networking.GridFlags;
import appeng.api.networking.IGridConnection;
import appeng.api.networking.IGridNode;
import appeng.api.networking.events.MENetworkBootingStatusChange;
import appeng.api.networking.events.MENetworkChannelsChanged;
import appeng.api.networking.events.MENetworkEventSubscribe;
import appeng.api.networking.events.MENetworkPowerStatusChange;
import appeng.api.networking.ticking.IGridTickable;
import appeng.api.networking.ticking.TickRateModulation;
import appeng.api.networking.ticking.TickingRequest;
import appeng.api.parts.IPartHost;
import appeng.api.util.AECableType;
import appeng.api.util.AEColor;
import appeng.api.util.AEPartLocation;
import appeng.core.AELog;
import appeng.me.GridAccessException;
import appeng.tile.grid.AENetworkTile;
import appeng.util.Platform;
import co.neeve.nae2.client.rendering.helpers.BeamFormerRenderHelper;
import co.neeve.nae2.common.interfaces.IBeamFormer;
import co.neeve.nae2.server.IBlockStateListener;
import co.neeve.nae2.server.WorldListener;
import io.netty.buffer.ByteBuf;
import it.unimi.dsi.fastutil.longs.Long2ObjectLinkedOpenHashMap;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.Optional;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.EnumSet;
import java.util.LinkedHashSet;

@Optional.Interface(iface = "gregtech.client.utils.IBloomEffect", modid = "gregtech")
public class TileDenseBeamFormer extends AENetworkTile implements IGridTickable, IBeamFormer, IBlockStateListener, IPowerChannelState {

    private int beamLength = 0;
    private TileDenseBeamFormer otherBeamFormer = null;
    private IGridConnection connection = null;
    private Long2ObjectLinkedOpenHashMap<BlockPos> listenerLinkedList = null;
    private boolean hideBeam = false;
    private int oldLightValue = -1;

    @SideOnly(Side.CLIENT)
    private boolean paired = false;

    @SideOnly(Side.CLIENT)
    private boolean rendererRegistered = false;

    private static final int POWERED_FLAG = 1;
    private static final int ACTIVE_FLAG = 2;
    private int clientFlags = 0;

    public TileDenseBeamFormer() {
        this.getProxy().setFlags(GridFlags.DENSE_CAPACITY);
        this.getProxy().setValidSides(EnumSet.noneOf(EnumFacing.class));
        this.updateEnergyConsumption();
    }

    @Override
    public void setOrientation(EnumFacing forward, EnumFacing up) {
        super.setOrientation(forward, up);
        this.getProxy().setValidSides(EnumSet.of(forward.getOpposite()));
    }

    @Override
    public @NotNull AECableType getCableConnectionType(AEPartLocation dir) {
        return AECableType.DENSE_SMART;
    }

    @Override
    public AEColor getColor() {
        BlockPos cablePos = this.pos.offset(this.getForward().getOpposite());
        TileEntity te = this.world.getTileEntity(cablePos);
        if (te instanceof IPartHost) {
            AEColor color = ((IPartHost) te).getColor();
            if (color != null && color != AEColor.TRANSPARENT) {
                return color;
            }
        }
        return AEColor.TRANSPARENT;
    }

    private void updateEnergyConsumption() {
        this.getProxy().setIdlePowerUsage(Math.pow(Math.max(2d, this.beamLength), 1.05) * 2d);
    }

    @Override
    public int getBeamLength() {
        return this.beamLength;
    }

    @Override
    public EnumFacing getDirection() {
        return this.getForward();
    }

    @Override
    public @NotNull World getWorld() {
        return this.world;
    }

    @Override
    public boolean isValid() {
        if (Platform.isClient()) {
            return this.world == Minecraft.getMinecraft().world && !this.isInvalid();
        }
        return !this.isInvalid();
    }

    @Override
    public boolean shouldRenderBeam() {
        return !this.hideBeam && this.beamLength != 0 && this.isActive() && this.isPowered();
    }

    public boolean isBeamHidden() {
        return this.hideBeam;
    }

    @Override
    public @NotNull BlockPos getPos() {
        return this.pos;
    }

    @Override
    public double getBeamThicknessMultiplier() {
        return 2.0;
    }

    @Override
    public double getReverseBeamOffSet() {
        return 1.75;
    }

    @SideOnly(Side.CLIENT)
    public void shouldRenderDynamic() {
        if (Platform.isClient()) {
            if (!this.rendererRegistered) {
                this.rendererRegistered = true;
                BeamFormerRenderHelper.init(this);
            }
        }
        BeamFormerRenderHelper.shouldRenderDynamic(this);
    }

    public boolean toggleBeamVisibility(EntityPlayer player, EnumHand hand) {
        if (Platform.isServer()) {
            this.hideBeam = !this.hideBeam;
            this.markForUpdate();
            this.saveChanges();

            if (this.otherBeamFormer != null) {
                this.otherBeamFormer.hideBeam = this.hideBeam;
                this.otherBeamFormer.markForUpdate();
                this.otherBeamFormer.saveChanges();
            }
        }

        player.swingArm(hand);
        return true;
    }

    @Override
    public boolean isPowered() {
        if (!Platform.isClient()) {
            try {
                return this.getProxy().getEnergy().isNetworkPowered();
            } catch (GridAccessException ignored) {
                return false;
            }
        }
        return (this.clientFlags & POWERED_FLAG) == POWERED_FLAG;
    }

    @Override
    public boolean isActive() {
        if (!Platform.isClient()) {
            return this.getProxy().isActive();
        }
        return this.isPowered() && (this.clientFlags & ACTIVE_FLAG) == ACTIVE_FLAG;
    }

    @SideOnly(Side.CLIENT)
    public boolean isBeaming() {
        return this.paired;
    }

    @MENetworkEventSubscribe
    public void chanRender(MENetworkChannelsChanged c) {
        this.markForUpdate();
    }

    @MENetworkEventSubscribe
    public void onPowerChange(MENetworkPowerStatusChange e) throws GridAccessException {
        this.markForUpdate();
        if (!this.getProxy().isReady()) return;
        this.getProxy().getTick().alertDevice(this.getProxy().getNode());
    }

    @MENetworkEventSubscribe
    public void onBootingChange(MENetworkBootingStatusChange e) throws GridAccessException {
        this.markForUpdate();
        if (!this.getProxy().isReady()) return;
        this.getProxy().getTick().alertDevice(this.getProxy().getNode());
    }

    private void unregisterListener() {
        WorldListener.instance.unregisterBlockStateListener(this);
    }

    @SuppressWarnings("deprecation")
    private static boolean isTranslucent(IBlockState state) {
        return !state.getMaterial().isOpaque() || state.getBlock().getLightOpacity(state) != 255;
    }

    private void connect(TileDenseBeamFormer potentialFormer, Iterable<BlockPos> locs) throws FailedConnectionException {
        var myNode = this.getProxy().getNode();
        this.connection = AEApi.instance().grid().createGridConnection(myNode, potentialFormer.getProxy().getNode());

        potentialFormer.connection = this.connection;
        this.otherBeamFormer = potentialFormer;
        potentialFormer.otherBeamFormer = this;

        if (potentialFormer.hideBeam || this.hideBeam) {
            potentialFormer.hideBeam = true;
            this.hideBeam = true;
        }

        this.unregisterListener();
        this.otherBeamFormer.unregisterListener();
        this.listenerLinkedList = new Long2ObjectLinkedOpenHashMap<>();
        for (var loc : locs) {
            this.listenerLinkedList.put(loc.toLong(), loc);
        }

        WorldListener.instance.registerBlockStateListener(this, locs);

        this.beamLength = this.listenerLinkedList.size();
        potentialFormer.beamLength = 0;

        this.updateEnergyConsumption();
        this.otherBeamFormer.updateEnergyConsumption();

        try {
            this.otherBeamFormer.getProxy().getTick().sleepDevice(this.otherBeamFormer.getProxy().getNode());
        } catch (GridAccessException ignored) {
        }

        this.markForUpdate();
        this.saveChanges();
        this.otherBeamFormer.markForUpdate();
        this.otherBeamFormer.saveChanges();
    }

    public boolean disconnect(@Nullable BlockPos breakPos) {
        if (this.connection == null) {
            this.updateEnergyConsumption();
            return false;
        }

        int newBeamA = 0, newBeamB = 0;

        if (breakPos != null && this.listenerLinkedList != null) {
            var it = this.listenerLinkedList.long2ObjectEntrySet().fastIterator();
            long hash = breakPos.toLong();
            while (it.hasNext()) {
                if (it.next().getLongKey() == hash) break;
                newBeamA++;
            }
            while (it.hasNext()) {
                it.next();
                newBeamB++;
            }
        }

        this.beamLength = newBeamA;

        if (this.connection != null) {
            try {
                this.connection.destroy();
            } catch (Throwable ignored) {
            }
            this.connection = null;
        }

        this.updateEnergyConsumption();
        this.markForUpdate();
        this.saveChanges();

        if (this.otherBeamFormer != null && this.otherBeamFormer.otherBeamFormer == this) {
            this.otherBeamFormer.beamLength = newBeamB;
            this.otherBeamFormer.connection = null;
            this.otherBeamFormer.otherBeamFormer = null;
            this.otherBeamFormer.updateEnergyConsumption();
            this.otherBeamFormer.markForUpdate();
            this.otherBeamFormer.saveChanges();
            this.otherBeamFormer = null;
        }

        return true;
    }


    @NotNull
    @Override
    public TickingRequest getTickingRequest(@NotNull IGridNode node) {
        return new TickingRequest(20, 300, false, true);
    }

    @NotNull
    @Override
    public TickRateModulation tickingRequest(@NotNull IGridNode node, int ticksSinceLastCall) {
        if (!this.getProxy().isReady()) return TickRateModulation.SAME;

        final boolean isConnectionValid = this.connection != null;
        final EnumFacing dir = this.getForward();
        final EnumFacing opposite = dir.getOpposite();
        final World world = this.world;
        BlockPos loc = this.pos;
        final LinkedHashSet<BlockPos> blockSet = new LinkedHashSet<>();

        for (int i = 0; i < 96; i++) {
            loc = loc.offset(dir);

            final TileEntity te = world.getTileEntity(loc);
            if (te instanceof TileDenseBeamFormer potential) {
                final EnumFacing pf = potential.getForward();

                if (pf == opposite) {
                    if (isConnectionValid && potential == this.otherBeamFormer && this.otherBeamFormer.otherBeamFormer == this) {
                        return TickRateModulation.SLEEP;
                    }

                    final boolean disconnected = this.disconnect(loc);
                    if (potential.getProxy().isReady() && potential.otherBeamFormer == null) {
                        try {
                            this.connect(potential, blockSet);
                            return TickRateModulation.SLEEP;
                        } catch (final FailedConnectionException | NullPointerException e) {
                            AELog.error(e);
                        }
                    }

                    return disconnected ? TickRateModulation.URGENT : TickRateModulation.SLOWER;
                }

                if (pf == dir) {
                    return this.disconnect(loc) ? TickRateModulation.URGENT : TickRateModulation.SLOWER;
                }
            }

            final IBlockState block = world.getBlockState(loc);
            if (!isTranslucent(block)) {
                return this.disconnect(loc) ? TickRateModulation.URGENT : TickRateModulation.SLOWER;
            }

            blockSet.add(loc);
        }

        return TickRateModulation.SLOWER;
    }

    @Override
    public void notifyBlockUpdate(World w, BlockPos pos, IBlockState oldS, IBlockState newS, int flags) {
        if (w != this.world) return;
        if (this.listenerLinkedList == null || !this.listenerLinkedList.containsKey(pos.toLong())) return;

        try {
            var isValid = isTranslucent(newS);

            if (isValid) {
                var te = w.getTileEntity(pos);
                if (te instanceof TileDenseBeamFormer f) {
                    EnumFacing dir = this.getForward(), pf = f.getForward();
                    if (pf != dir.getOpposite()) {
                        isValid = false;
                    }
                }
            }

            if (this.connection != null && !isValid) {
                this.disconnect(pos);
                this.getProxy().getTick().alertDevice(this.getProxy().getNode());
            } else if (isValid && this.connection == null) {
                this.getProxy().getTick().alertDevice(this.getProxy().getNode());
            }
        } catch (GridAccessException ignored) {
        }
    }

    public int getLightValue() {
        return !this.hideBeam
                && ((Platform.isClient() && this.paired) || this.beamLength != 0 || this.otherBeamFormer != null)
                && (this.isActive() && this.isPowered()) ? 15 : 0;
    }

    @Override
    public void markForUpdate() {
        if (this.world != null) {
            int newLightValue = this.getLightValue();
            if (oldLightValue != newLightValue) {
                this.oldLightValue = newLightValue;
                this.world.checkLight(this.pos);
            }

        }

        super.markForUpdate();
    }

    @Override
    public void readFromNBT(NBTTagCompound compound) {
        super.readFromNBT(compound);

        this.beamLength = compound.getInteger("beamLength");
        this.hideBeam = compound.getBoolean("hideBeam");
    }

    @Override
    public @NotNull NBTTagCompound writeToNBT(NBTTagCompound compound) {
        super.writeToNBT(compound);

        if (this.beamLength > 0) {
            compound.setInteger("beamLength", this.beamLength);
        }
        compound.setBoolean("hideBeam", this.hideBeam);
        return compound;
    }

    @Override
    public void writeToStream(ByteBuf data) throws IOException {
        super.writeToStream(data);
        data.writeInt(this.beamLength);
        data.writeBoolean(this.otherBeamFormer != null);
        data.writeBoolean(this.hideBeam);

        this.clientFlags = 0;

        try {
            if (this.getProxy().getEnergy().isNetworkPowered()) {
                this.clientFlags |= POWERED_FLAG;
            }
            if (this.getProxy().isActive()) {
                this.clientFlags |= ACTIVE_FLAG;
            }
        } catch (GridAccessException ignored) {
        }

        data.writeByte((byte) this.clientFlags);
    }

    @Override
    public boolean readFromStream(ByteBuf data) throws IOException {
        var shouldRedraw = super.readFromStream(data);

        this.beamLength = data.readInt();
        var wasPaired = this.paired;
        this.paired = data.readBoolean();

        if (this.paired != wasPaired) {
            var pos = this.getPos();
            var x = pos.getX();
            var y = pos.getY();
            var z = pos.getZ();
            Minecraft.getMinecraft().renderGlobal.markBlockRangeForRenderUpdate(x, y, z, x, y, z);
        }
        this.hideBeam = data.readBoolean();

        var oldFlags = this.clientFlags;
        this.clientFlags = data.readByte();


        final int newLightValue = this.getLightValue();
        if (newLightValue != this.oldLightValue) {
            this.oldLightValue = newLightValue;
            this.world.checkLight(this.pos);
            shouldRedraw = true;
        }

        return shouldRedraw || oldFlags != this.clientFlags;
    }


    @Override
    public void onReady() {
        super.onReady();
        this.updateEnergyConsumption();
        this.markForUpdate();
        try {
            this.getProxy().getTick().alertDevice(this.getProxy().getNode());
        } catch (GridAccessException ignored) {
        }


        if (Platform.isClient()) {
            this.shouldRenderDynamic();
        }
    }

    @Override
    public void onChunkUnload() {
        this.disconnect(null);
        super.onChunkUnload();
    }

    @Override
    public void invalidate() {
        this.disconnect(null);
        this.paired = false;
        this.beamLength = 0;
        super.invalidate();
    }
}