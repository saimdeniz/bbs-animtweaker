package com.example.bbsanimtweaker.client.gui;

import com.example.bbsanimtweaker.client.logic.AnimTweakerEngine;
import mchorse.bbs_mod.BBSMod;
import mchorse.bbs_mod.BBSModClient;
import mchorse.bbs_mod.cubic.ModelInstance;
import mchorse.bbs_mod.forms.forms.ModelForm;
import mchorse.bbs_mod.forms.renderers.ModelFormRenderer;
import mchorse.bbs_mod.l10n.keys.IKey;
import mchorse.bbs_mod.ui.forms.editors.forms.UIForm;
import mchorse.bbs_mod.ui.forms.editors.forms.UIModelForm;
import mchorse.bbs_mod.ui.forms.editors.panels.UIFormPanel;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIButton;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIToggle;
import mchorse.bbs_mod.ui.framework.elements.input.list.UIStringList;
import mchorse.bbs_mod.ui.framework.elements.input.text.UITextbox;
import mchorse.bbs_mod.ui.utils.UI;
import mchorse.bbs_mod.ui.utils.UIConstants;
import mchorse.bbs_mod.resources.Link;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * AnimTweaker panel embedded directly inside the BBS FS Model Editor (UIModelForm).
 *
 * Reverted to the first stable version (Gradle build v26) to prevent game freezes.
 */
public class UIModelATPanel extends UIFormPanel<ModelForm>
{
    private static final Map<String, String> lastSelectedBones = new HashMap<>();

    public UITextbox searchBonesInput;
    public UIStringList bonesList;

    public UITextbox searchAnimationsInput;
    public UIStringList animationsList;
    public UIToggle allAnimationsToggle;

    public UITextbox targetBoneInput;

    public UIToggle injectXPosToggle;
    public UIToggle injectXNegToggle;
    public UIToggle injectYPosToggle;
    public UIToggle injectYNegToggle;
    public UIToggle injectZPosToggle;
    public UIToggle injectZNegToggle;

    public UITextbox mathModifierInput;
    public UIButton injectQueryButton;
    public UIButton removeQueriesButton;
    public UIButton restoreBackupButton;

    public UIModelATPanel(UIForm editor)
    {
        super(editor);

        // Bones search & list
        this.searchBonesInput = new UITextbox(1000, (str) ->
        {
            this.updateBonesList(str);
        });
        this.searchBonesInput.placeholder(IKey.raw("Search..."));
        this.searchBonesInput.h(20);

        this.bonesList = new UIStringList((selected) ->
        {
            String bone = selected == null || selected.isEmpty() ? null : selected.get(0);
            if (bone == null || bone.isEmpty()) return;

            // Update target bone field and save to memory map
            this.setTargetBone(bone);

            // Also forward selection to Pose tab's poseEditor so both stay in sync
            try
            {
                if (this.editor instanceof UIModelForm modelForm)
                {
                    modelForm.modelPanel.poseEditor.selectBone(bone);
                }
            }
            catch (Exception ignored) {}
        });
        this.bonesList.background().h(130);

        // Animations search & list
        this.searchAnimationsInput = new UITextbox(1000, (str) ->
        {
            this.updateAnimationsList(str);
        });
        this.searchAnimationsInput.placeholder(IKey.raw("Search..."));
        this.searchAnimationsInput.h(20);

        this.animationsList = new UIStringList((l) -> {});
        this.animationsList.multi();
        this.animationsList.background().h(100);

        this.allAnimationsToggle = new UIToggle(IKey.raw(""), true, (b) -> {});
        this.allAnimationsToggle.tooltip(IKey.raw("If enabled, applies to all animations."));

        // Target Bone
        this.targetBoneInput = new UITextbox(1000, (str) -> {
            if (this.form != null)
            {
                String modelId = this.form.model.get();
                if (modelId != null && !modelId.isEmpty())
                {
                    lastSelectedBones.put(modelId, str);
                }
            }
        });
        this.targetBoneInput.setText("");

        // 6-axis toggles (mutual exclusion within each axis)
        this.injectXPosToggle = new UIToggle(IKey.raw("X (Pitch) (+)"), false, (b) ->
        {
            if (b.getValue()) this.injectXNegToggle.setValue(false);
        });
        this.injectXNegToggle = new UIToggle(IKey.raw("X (Pitch) (-)"), false, (b) ->
        {
            if (b.getValue()) this.injectXPosToggle.setValue(false);
        });
        this.injectYPosToggle = new UIToggle(IKey.raw("Y (Yaw) (+)"), false, (b) ->
        {
            if (b.getValue()) this.injectYNegToggle.setValue(false);
        });
        this.injectYNegToggle = new UIToggle(IKey.raw("Y (Yaw) (-)"), false, (b) ->
        {
            if (b.getValue()) this.injectYPosToggle.setValue(false);
        });
        this.injectZPosToggle = new UIToggle(IKey.raw("Z (Roll) (+)"), false, (b) ->
        {
            if (b.getValue()) this.injectZNegToggle.setValue(false);
        });
        this.injectZNegToggle = new UIToggle(IKey.raw("Z (Roll) (-)"), false, (b) ->
        {
            if (b.getValue()) this.injectZPosToggle.setValue(false);
        });

        // Math Modifier
        this.mathModifierInput = new UITextbox(1000, (str) -> {});
        this.mathModifierInput.tooltip(IKey.raw("Custom Math Modifier (e.g. * 0.5)"));

        // Inject button
        this.injectQueryButton = new UIButton(IKey.raw("Inject Query"), (b) ->
        {
            String bone = this.targetBoneInput.getText();
            if (bone.isEmpty()) return;

            List<String> targetAnims = this.allAnimationsToggle.getValue()
                    ? null
                    : this.animationsList.getCurrent();
            if (!this.allAnimationsToggle.getValue() && (targetAnims == null || targetAnims.isEmpty())) return;

            File f = this.getModelFile();
            if (f != null && f.exists())
            {
                AnimTweakerEngine.createBackup(f);

                String xQuery = null;
                if (this.injectXPosToggle.getValue())      xQuery = "+query.head_pitch";
                else if (this.injectXNegToggle.getValue()) xQuery = "-query.head_pitch";

                String yQuery = null;
                if (this.injectYPosToggle.getValue())      yQuery = "+query.head_yaw";
                else if (this.injectYNegToggle.getValue()) yQuery = "-query.head_yaw";

                String zQuery = null;
                if (this.injectZPosToggle.getValue())      zQuery = "+query.head_yaw";
                else if (this.injectZNegToggle.getValue()) zQuery = "-query.head_yaw";

                String modifier = this.mathModifierInput.getText().trim();
                if (!modifier.isEmpty())
                {
                    if (!modifier.matches("^[0-9a-zA-Z_\\s.+\\-*/%()]*$"))
                    {
                        this.getContext().notifyError(IKey.raw("Invalid character in math modifier!"));
                        return;
                    }
                    int balance = 0;
                    for (char c : modifier.toCharArray())
                    {
                        if (c == '(') balance++;
                        if (c == ')') balance--;
                        if (balance < 0) break;
                    }
                    if (balance != 0)
                    {
                        this.getContext().notifyError(IKey.raw("Unbalanced parentheses in math modifier!"));
                        return;
                    }
                }

                boolean success = AnimTweakerEngine.injectQueries(f, bone, targetAnims, xQuery, yQuery, zQuery, modifier);
                if (success) this.triggerReload();
            }
        });

        // Remove button
        this.removeQueriesButton = new UIButton(IKey.raw("Remove Query"), (b) ->
        {
            String bone = this.targetBoneInput.getText();
            if (bone.isEmpty()) return;

            List<String> targetAnims = this.allAnimationsToggle.getValue()
                    ? null
                    : this.animationsList.getCurrent();
            if (!this.allAnimationsToggle.getValue() && (targetAnims == null || targetAnims.isEmpty())) return;

            File f = this.getModelFile();
            if (f != null && f.exists())
            {
                AnimTweakerEngine.createBackup(f);
                if (AnimTweakerEngine.removeQueries(f, bone, targetAnims)) this.triggerReload();
            }
        });

        // Restore backup button
        this.restoreBackupButton = new UIButton(IKey.raw("Restore Backup"), (b) ->
        {
            File f = this.getModelFile();
            if (f != null)
            {
                if (AnimTweakerEngine.restoreBackup(f))
                {
                    this.triggerReload();
                }
                else
                {
                    this.getContext().notifyError(IKey.raw("No Backup Found!"));
                }
            }
        });
        this.restoreBackupButton.color(0xff4444);

        // Layout in the right-side options scroll view provided by UIFormPanel
        this.options.add(UI.column(UIConstants.MARGIN,
            UI.label(IKey.raw("Bones")),
            this.searchBonesInput.marginTop(-2),
            this.bonesList.marginTop(-UIConstants.MARGIN),
            UI.row(UI.label(IKey.raw("Animations"), 14).labelAnchor(0, 0.5F), this.allAnimationsToggle).h(14).marginTop(5),
            this.searchAnimationsInput.marginTop(-4),
            this.animationsList.marginTop(-UIConstants.MARGIN),
            UI.label(IKey.raw("Target Bone")).marginTop(5),
            this.targetBoneInput,
            UI.row(this.injectXPosToggle, this.injectXNegToggle),
            UI.row(this.injectYPosToggle, this.injectYNegToggle),
            UI.row(this.injectZPosToggle, this.injectZNegToggle),
            UI.label(IKey.raw("Math Modifier")).marginTop(5),
            this.mathModifierInput,
            this.injectQueryButton,
            this.removeQueriesButton,
            this.restoreBackupButton
        ).relative(this.options).x(5).y(5).w(1F, -10).h(0));
    }
    @Override
    public void setVisible(boolean visible)
    {
        super.setVisible(visible);
        // Only sync when the user explicitly switches to this tab,
        // and only after the form (model) is already loaded.
        if (visible && this.form != null)
        {
            this.syncBoneFromPoseTab();
        }
    }

    /**
     * Called by UIModelForm whenever a new ModelForm is opened for editing.
     * Refreshes the animations list from the newly loaded model file.
     */
    @Override
    public void startEdit(ModelForm form)
    {
        super.startEdit(form);
        // NOTE: Do NOT call syncBoneFromPoseTab() here.
        // startEdit is called on ALL panels simultaneously during editor init,
        // before the model or poseEditor bones are ready. Calling into
        // poseEditor at that stage causes a layout deadlock freeze.
        this.updateAnimationsList(this.searchAnimationsInput.getText());
        this.updateBonesList(this.searchBonesInput.getText());

        // Load model-specific target bone from memory map
        if (form.model.get() != null)
        {
            String modelId = form.model.get();
            String lastBone = lastSelectedBones.getOrDefault(modelId, "");
            this.targetBoneInput.setText(lastBone);
            this.selectBoneInList(lastBone);
        }
    }

    private void setTargetBone(String bone)
    {
        this.targetBoneInput.setText(bone);
        if (this.form != null)
        {
            String modelId = this.form.model.get();
            if (modelId != null && !modelId.isEmpty())
            {
                lastSelectedBones.put(modelId, bone);
            }
        }
    }

    /**
     * Called by UIForm.pickBoneFromViewport() when the user clicks a bone while
     * this panel is the active panel. We update targetBoneInput here and return
     * false so that the framework still switches to the Pose tab (no freeze risk).
     */
    @Override
    public boolean pickBoneInList(String bone)
    {
        if (bone != null && !bone.isEmpty())
        {
            this.setTargetBone(bone);
            this.selectBoneInList(bone);
        }
        return false;
    }

    /**
     * Called when this panel IS the active view and a bone is picked.
     * (pickBoneInList handles the more common viewport-click case.)
     */
    @Override
    public void pickBone(String bone)
    {
        if (bone != null && !bone.isEmpty())
        {
            this.setTargetBone(bone);
            this.selectBoneInList(bone);
        }
    }

    private void syncBoneFromPoseTab()
    {
        try
        {
            if (this.editor instanceof UIModelForm modelForm)
            {
                String bone = modelForm.modelPanel.poseEditor.groups.list.getCurrentFirst();
                if (bone != null && !bone.isEmpty())
                {
                    this.setTargetBone(bone);
                    this.selectBoneInList(bone);
                }
            }
        }
        catch (Exception ignored) {}
    }

    /** Scrolls the bonesList to the given bone and highlights it without triggering callbacks. */
    private void selectBoneInList(String bone)
    {
        try
        {
            int index = this.bonesList.getList().indexOf(bone);
            if (index >= 0)
            {
                this.bonesList.setIndex(index);
            }
        }
        catch (Exception ignored) {}
    }

    private void updateBonesList(String filter)
    {
        this.bonesList.clear();
        if (this.form == null) return;

        try
        {
            ModelInstance model = ModelFormRenderer.getModel(this.form);
            if (model == null || model.model == null) return;

            Collection<String> groups = model.model.getAllGroupKeys();
            List<String> sorted = new ArrayList<>(groups);
            java.util.Collections.sort(sorted);

            for (String group : sorted)
            {
                if (filter.isEmpty() || group.toLowerCase().contains(filter.toLowerCase()))
                {
                    this.bonesList.add(group);
                }
            }
        }
        catch (Exception ignored) {}
    }


    // ---- Private helpers ----

    private File getModelFile()
    {
        if (this.form == null) return null;

        String modelId = this.form.model.get();
        if (modelId == null || modelId.isEmpty()) return null;

        Link bbsLink = Link.assets("models/" + modelId + "/model.bbs.json");
        File bbsFile = BBSMod.getProvider().getFile(bbsLink);
        if (bbsFile != null && bbsFile.exists()) return bbsFile;

        Link animFolderLink = Link.assets("models/" + modelId + "/animations");
        File animFolder = BBSMod.getProvider().getFile(animFolderLink);
        if (animFolder != null && animFolder.isDirectory())
        {
            File[] files = animFolder.listFiles((dir, name) -> name.endsWith(".animation.json"));
            if (files != null && files.length > 0) return files[0];
        }

        return bbsFile;
    }

    private void updateAnimationsList(String filter)
    {
        this.animationsList.clear();
        File modelFile = this.getModelFile();
        if (modelFile != null && modelFile.exists())
        {
            List<String> animNames = AnimTweakerEngine.getAnimationNames(modelFile);
            for (String anim : animNames)
            {
                if (filter.isEmpty() || anim.toLowerCase().contains(filter.toLowerCase()))
                {
                    this.animationsList.add(anim);
                }
            }
        }
    }

    private void triggerReload()
    {
        if (this.form == null) return;

        String modelId = this.form.model.get();
        if (modelId == null || modelId.isEmpty()) return;

        BBSModClient.getModels().loadModel(modelId);

        if (mchorse.bbs_mod.forms.FormUtilsClient.getRenderer(this.form) instanceof mchorse.bbs_mod.forms.renderers.ModelFormRenderer r)
        {
            r.resetAnimator();
        }

        this.updateAnimationsList(this.searchAnimationsInput.getText());
        this.getContext().notifySuccess(IKey.raw("Changes Applied! Model Reloaded."));
    }
}