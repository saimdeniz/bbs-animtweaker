package com.example.bbsanimtweaker.client.mixin;

import com.example.bbsanimtweaker.client.gui.UIModelATPanel;
import mchorse.bbs_mod.graphics.window.Window;
import mchorse.bbs_mod.l10n.keys.IKey;
import mchorse.bbs_mod.ui.Keys;
import mchorse.bbs_mod.ui.forms.editors.forms.UIModelForm;
import mchorse.bbs_mod.ui.forms.editors.panels.UIFormPanel;
import mchorse.bbs_mod.ui.utils.UIUtils;
import mchorse.bbs_mod.ui.utils.icons.Icons;
import mchorse.bbs_mod.utils.MathUtils;
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
        UIModelATPanel atPanel = new UIModelATPanel(self);
        self.registerPanel(
            atPanel,
            IKey.raw("BBS AT"),
            Icons.WRENCH
        );

        // Shared logic: sync bone when switching to/from the AT tab
        java.util.function.Consumer<UIFormPanel> handlePanelSwitch = (panel) ->
        {
            if (panel == self.modelPanel && self.modelPanel != null && self.modelPanel.poseEditor != null)
            {
                String targetBone = atPanel.getTargetBone();
                if (targetBone != null && !targetBone.isEmpty())
                {
                    String primary = targetBone.split(",")[0].trim();
                    if (!primary.isEmpty())
                    {
                        self.modelPanel.poseEditor.selectBone(primary);
                    }
                }
            }
            else if (panel == atPanel)
            {
                atPanel.syncBoneFromPoseTab();
            }
        };

        // Synchronize bone selection when clicking tabs in the tab strip
        self.buttons.onSelect((index) ->
        {
            UIFormPanel panel = self.panels.get(index);
            self.setPanel(panel);
            handlePanelSwitch.accept(panel);
        });

        // Also synchronize bone selection when cycling tabs via keyboard shortcut
        self.keys().register(Keys.FILM_CONTROLLER_CYCLE_EDITORS, () ->
        {
            int index = self.panels.indexOf(self.view);
            int newIndex = MathUtils.cycler(index + (Window.isShiftPressed() ? -1 : 1), self.panels);
            UIFormPanel panel = self.panels.get(newIndex);
            self.setPanel(panel);
            handlePanelSwitch.accept(panel);
            UIUtils.playClick();
        });
    }
}