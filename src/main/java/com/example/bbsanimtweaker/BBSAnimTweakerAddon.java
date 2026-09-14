package com.example.bbsanimtweaker;

import mchorse.bbs_mod.api.BBSAddonMod;
import mchorse.bbs_mod.api.BBSApi;
import mchorse.bbs_mod.api.Subscribe;
import mchorse.bbs_mod.api.events.RegisterSourcePacksEvent;

public class BBSAnimTweakerAddon implements BBSAddonMod
{
    public static final String MOD_ID = "bbs_animtweaker_addon";

    @Subscribe
    public void onSourcePacks(RegisterSourcePacksEvent event)
    {
        BBSApi.requireVersion(MOD_ID, 1);
        event.registerAddon(MOD_ID, BBSAnimTweakerAddon.class);
    }
}
