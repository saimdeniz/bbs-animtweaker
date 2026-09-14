package com.example.bbsanimtweaker.client;

import com.example.bbsanimtweaker.client.gui.UIAnimTweakerPanel;
import mchorse.bbs_mod.api.BBSAddonMod;
import mchorse.bbs_mod.api.Subscribe;
import mchorse.bbs_mod.api.client.events.RegisterDashboardPanelsEvent;
import mchorse.bbs_mod.l10n.keys.IKey;
import mchorse.bbs_mod.ui.utils.icons.Icons;

public class BBSAnimTweakerClientAddon implements BBSAddonMod
{
    @Subscribe
    public void onDashboardPanels(RegisterDashboardPanelsEvent event)
    {
        event.dashboard.getPanels().registerPanel(
            new UIAnimTweakerPanel(event.dashboard),
            IKey.raw("Anim Tweaker"),
            Icons.WRENCH
        );
    }
}
