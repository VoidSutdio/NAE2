package co.neeve.nae2.common.sync;

import co.neeve.nae2.Tags;
import net.minecraft.util.text.translation.I18n;

public enum NAE2Tooltip {
    AUTO_PICKUP,
    AUTO_PICKUP_TIP,
    AUTO_PICKUP_HOW_TO_ENABLE,

    ENABLED_LOWERCASE,
    DISABLED_LOWERCASE;


    private final String root;

    NAE2Tooltip() {
        this.root = "tooltip." + Tags.MODID;
    }

    NAE2Tooltip(String r) {
        this.root = r;
    }

    public String getLocal() {
        return I18n.translateToLocal(this.getUnlocalized().toLowerCase());
    }

    public String getLocalWithSpaceAtEnd() {
        return I18n.translateToLocal(this.getUnlocalized().toLowerCase()) + " ";
    }

    public String getUnlocalized() {
        return this.root + '.' + this + ".name";
    }
}
