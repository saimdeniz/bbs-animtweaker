package com.example.bbsanimtweaker.client;

import net.fabricmc.api.ClientModInitializer;

public class BBSAnimTweakerClientInit implements ClientModInitializer
{
    @Override
    public void onInitializeClient()
    {
        mchorse.bbs_mod.BBSMod.events.register(new com.example.bbsanimtweaker.client.BBSAnimTweakerClientSub());
    }
}
