package co.neeve.nae2.client.rendering.tesr;

import co.neeve.nae2.client.rendering.helpers.BeamFormerRenderHelper;
import co.neeve.nae2.common.tiles.TileDenseBeamFormer;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.jetbrains.annotations.NotNull;

import java.util.WeakHashMap;

@SideOnly(Side.CLIENT)
public class TESRDenseBeamFormer extends TileEntitySpecialRenderer<TileDenseBeamFormer> {
    private static final WeakHashMap<TileDenseBeamFormer, Boolean> INITIALIZED = new WeakHashMap<>();

    @Override
    public void render(@NotNull TileDenseBeamFormer te, double x, double y, double z, float partialTicks, int destroyStage, float alpha) {
        if (!te.isValid()) {
            INITIALIZED.remove(te);
            return;
        }
        
        if (!INITIALIZED.containsKey(te)) {
            INITIALIZED.put(te, true);
            BeamFormerRenderHelper.init(te);
        }
        
        boolean shouldRenderDynamic = BeamFormerRenderHelper.shouldRenderDynamic(te);
        boolean shouldRenderBeam = te.shouldRenderBeam();
        
        if (!shouldRenderDynamic || !shouldRenderBeam) {
            return;
        }
        
        BeamFormerRenderHelper.renderDynamic(te, x, y, z, partialTicks);
    }

    @Override
    public boolean isGlobalRenderer(TileDenseBeamFormer te) {
        return te.shouldRenderBeam();
    }
}
