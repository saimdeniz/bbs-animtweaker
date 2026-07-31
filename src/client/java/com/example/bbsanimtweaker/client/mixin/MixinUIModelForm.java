package com.example.bbsanimtweaker.client.mixin;

import com.example.bbsanimtweaker.client.gui.UIModelATPanel;
import mchorse.bbs_mod.l10n.keys.IKey;
import mchorse.bbs_mod.ui.forms.editors.forms.UIModelForm;
import mchorse.bbs_mod.ui.utils.icons.Icons;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = UIModelForm.class, remap = false)
public class MixinUIModelForm
{
    @Inject(method = "<init>", at = @At("TAIL"))
    private void injectAnimTweakerTab(CallbackInfo ci)
    {
        UIModelForm self = (UIModelForm) (Object) this;
        self.registerPanel(
            new UIModelATPanel(self),
            IKey.raw("BBS AT"),
            Icons.WRENCH
        );
    }
}