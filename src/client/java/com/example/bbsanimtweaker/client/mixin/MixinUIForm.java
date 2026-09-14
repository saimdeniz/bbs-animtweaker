package com.example.bbsanimtweaker.client.mixin;

import com.example.bbsanimtweaker.client.gui.UIModelATPanel;
import mchorse.bbs_mod.ui.forms.editors.forms.UIForm;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = UIForm.class, remap = false)
public class MixinUIForm
{
    /**
     * Injects at the end of pickBoneFromViewport (defined in UIForm) to capture
     * any 3D viewport bone click and synchronize it into the Anim Tweaker panel's
     * target bone input field.
     */
    @Inject(method = "pickBoneFromViewport", at = @At("RETURN"))
    private void afterPickBoneFromViewport(String bone, Class<?> preferredPanel, CallbackInfo ci)
    {
        if (bone != null && !bone.isEmpty())
        {
            UIForm self = (UIForm) (Object) this;
            for (Object panel : self.panels)
            {
                if (panel instanceof UIModelATPanel atPanel)
                {
                    atPanel.setTargetBone(bone);
                    break;
                }
            }
        }
    }
}
