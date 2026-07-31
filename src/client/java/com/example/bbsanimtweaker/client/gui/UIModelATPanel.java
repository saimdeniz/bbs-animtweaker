package com.example.bbsanimtweaker.client.gui;

import com.example.bbsanimtweaker.client.logic.AnimTweakerEngine;
import com.example.bbsanimtweaker.client.logic.UndoManager;
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
import mchorse.bbs_mod.ui.framework.elements.input.UITrackpad;
import mchorse.bbs_mod.ui.framework.elements.input.list.UIStringList;
import mchorse.bbs_mod.ui.framework.elements.input.text.UITextbox;
import mchorse.bbs_mod.ui.utils.UI;
import mchorse.bbs_mod.ui.utils.UIConstants;
import mchorse.bbs_mod.resources.Link;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * AnimTweaker panel embedded directly inside the BBS FS Model Editor (UIModelForm).
 */
public class UIModelATPanel extends UIFormPanel<ModelForm>
{
    private static final Map<String, String> lastSelectedBones = new HashMap<>();

    public UITextbox searchBonesInput;
    public UIStringList bonesList;

    public UITextbox searchAnimationsInput;
    public UIStringList animationsList;
    public UIToggle onlyTweakedToggle;
    public UIToggle allAnimationsToggle;

    public UITextbox targetBoneInput;

    public UIButton presetHeadPosBtn;
    public UIButton presetHeadNegBtn;
    public UIButton presetBreathingBtn;
    public UIButton presetClearBtn;

    public UIToggle injectXPosToggle;
    public UIToggle injectXNegToggle;
    public UIToggle injectYPosToggle;
    public UIToggle injectYNegToggle;
    public UIToggle injectZPosToggle;
    public UIToggle injectZNegToggle;

    public UITextbox mathModifierInput;
    public UITrackpad multiplierTrackpad;
    public UIButton quickModHalfBtn;
    public UIButton quickModInvBtn;
    public UIButton quickModSinBtn;

    public UIButton injectQueryButton;
    public UIButton removeQueriesButton;
    public UIButton undoButton;
    public UIButton redoButton;
    public UIButton resetOriginalButton;
    public UIButton acceptOriginalButton;

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
            if (selected == null || selected.isEmpty()) return;

            String joined = String.join(", ", selected);
            this.setTargetBone(joined);

            try
            {
                if (this.editor instanceof UIModelForm modelForm)
                {
                    modelForm.modelPanel.poseEditor.selectBone(selected.get(0));
                }
            }
            catch (Exception ignored) {}
        });
        this.bonesList.multi();
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

        this.onlyTweakedToggle = new UIToggle(IKey.raw("🛠 Only"), false, (b) ->
        {
            this.updateAnimationsList(this.searchAnimationsInput.getText());
        });
        this.onlyTweakedToggle.tooltip(IKey.raw("Show only modified animations"));

        this.allAnimationsToggle = new UIToggle(IKey.raw(""), true, (b) -> {});
        this.allAnimationsToggle.tooltip(IKey.raw("If enabled, applies to all animations."));

        // Presets
        this.presetHeadPosBtn = new UIButton(IKey.raw("Head Look (+)"), (b) -> {
            this.injectXPosToggle.setValue(true);  this.injectXNegToggle.setValue(false);
            this.injectYPosToggle.setValue(true);  this.injectYNegToggle.setValue(false);
            this.injectZPosToggle.setValue(false); this.injectZNegToggle.setValue(false);
            this.mathModifierInput.setText("");
            if (this.targetBoneInput.getText().isEmpty()) this.setTargetBone("head");
        });

        this.presetHeadNegBtn = new UIButton(IKey.raw("Head Look (-)"), (b) -> {
            this.injectXPosToggle.setValue(false); this.injectXNegToggle.setValue(true);
            this.injectYPosToggle.setValue(false); this.injectYNegToggle.setValue(true);
            this.injectZPosToggle.setValue(false); this.injectZNegToggle.setValue(false);
            this.mathModifierInput.setText("");
            if (this.targetBoneInput.getText().isEmpty()) this.setTargetBone("head");
        });

        this.presetBreathingBtn = new UIButton(IKey.raw("Breathing"), (b) -> {
            this.injectXPosToggle.setValue(true);  this.injectXNegToggle.setValue(false);
            this.injectYPosToggle.setValue(false); this.injectYNegToggle.setValue(false);
            this.injectZPosToggle.setValue(false); this.injectZNegToggle.setValue(false);
            this.mathModifierInput.setText("* math.sin(query.anim_time * 150) * 0.3");
            if (this.targetBoneInput.getText().isEmpty()) this.setTargetBone("body");
        });

        this.presetClearBtn = new UIButton(IKey.raw("Clear"), (b) -> {
            this.injectXPosToggle.setValue(false); this.injectXNegToggle.setValue(false);
            this.injectYPosToggle.setValue(false); this.injectYNegToggle.setValue(false);
            this.injectZPosToggle.setValue(false); this.injectZNegToggle.setValue(false);
            this.mathModifierInput.setText("");
        });

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
        this.injectZPosToggle = new UIToggle(IKey.raw("Z (Rotate) (+)"), false, (b) ->
        {
            if (b.getValue()) this.injectZNegToggle.setValue(false);
        });
        this.injectZNegToggle = new UIToggle(IKey.raw("Z (Rotate) (-)"), false, (b) ->
        {
            if (b.getValue()) this.injectZPosToggle.setValue(false);
        });

        // Math Modifier & Trackpad Multiplier
        this.mathModifierInput = new UITextbox(1000, (str) -> {
            String val = str.trim();
            if (val.isEmpty()) return;
            boolean validChars = val.matches("^[0-9a-zA-Z_\\s.+\\-*/%()]*$");
            int balance = 0;
            for (char c : val.toCharArray())
            {
                if (c == '(') balance++;
                if (c == ')') balance--;
                if (balance < 0) break;
            }
            if (!validChars || balance != 0)
            {
                this.mathModifierInput.textbox.setColor(0xff5555);
            }
            else
            {
                this.mathModifierInput.textbox.setColor(0xffffff);
            }
        });
        this.mathModifierInput.tooltip(IKey.raw("Custom Math Modifier (e.g. * 0.5)"));

        this.multiplierTrackpad = new UITrackpad((v) -> {
            double d = Math.round(v.doubleValue() * 100.0) / 100.0;
            if (d == 0) this.mathModifierInput.setText("");
            else this.mathModifierInput.setText("* " + d);
        });
        this.multiplierTrackpad.setValue(1.0);
        this.multiplierTrackpad.tooltip(IKey.raw("Drag to adjust multiplier (e.g. * 0.5)"));

        this.quickModHalfBtn = new UIButton(IKey.raw("* 0.5"), (b) -> this.mathModifierInput.setText("* 0.5"));
        this.quickModInvBtn = new UIButton(IKey.raw("* -1"), (b) -> this.mathModifierInput.setText("* -1.0"));
        this.quickModSinBtn = new UIButton(IKey.raw("+ Sin"), (b) -> this.mathModifierInput.setText("* math.sin(query.anim_time * 200)"));

        // Inject button
        this.injectQueryButton = new UIButton(IKey.raw("Inject Query"), (b) ->
        {
            String boneText = this.targetBoneInput.getText().trim();
            if (boneText.isEmpty()) return;

            String[] targetBones = boneText.split("\\s*,\\s*");

            List<String> rawAnims = this.allAnimationsToggle.getValue()
                    ? null
                    : this.animationsList.getCurrent();
            if (!this.allAnimationsToggle.getValue() && (rawAnims == null || rawAnims.isEmpty())) return;

            List<String> targetAnims = null;
            if (rawAnims != null && !rawAnims.isEmpty())
            {
                targetAnims = new ArrayList<>();
                for (String s : rawAnims)
                {
                    if (s.endsWith(" 🛠")) s = s.substring(0, s.length() - " 🛠".length());
                    else if (s.endsWith(" [Tweaked]")) s = s.substring(0, s.length() - " [Tweaked]".length());
                    targetAnims.add(s);
                }
            }

            File f = this.getModelFile();
            if (f != null && f.exists())
            {
                UndoManager.pushState(f);

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

                boolean anySuccess = false;
                for (String bone : targetBones)
                {
                    if (!bone.isEmpty())
                    {
                        if (AnimTweakerEngine.injectQueries(f, bone, targetAnims, xQuery, yQuery, zQuery, modifier))
                        {
                            anySuccess = true;
                        }
                    }
                }
                if (anySuccess) this.triggerReload();
            }
        });

        // Remove button
        this.removeQueriesButton = new UIButton(IKey.raw("Remove Query"), (b) ->
        {
            String boneText = this.targetBoneInput.getText().trim();
            if (boneText.isEmpty()) return;

            String[] targetBones = boneText.split("\\s*,\\s*");

            List<String> rawAnims = this.allAnimationsToggle.getValue()
                    ? null
                    : this.animationsList.getCurrent();
            if (!this.allAnimationsToggle.getValue() && (rawAnims == null || rawAnims.isEmpty())) return;

            List<String> targetAnims = null;
            if (rawAnims != null && !rawAnims.isEmpty())
            {
                targetAnims = new ArrayList<>();
                for (String s : rawAnims)
                {
                    if (s.endsWith(" 🛠")) s = s.substring(0, s.length() - " 🛠".length());
                    else if (s.endsWith(" [Tweaked]")) s = s.substring(0, s.length() - " [Tweaked]".length());
                    targetAnims.add(s);
                }
            }

            File f = this.getModelFile();
            if (f != null && f.exists())
            {
                UndoManager.pushState(f);
                boolean anySuccess = false;
                for (String bone : targetBones)
                {
                    if (!bone.isEmpty())
                    {
                        if (AnimTweakerEngine.removeQueries(f, bone, targetAnims))
                        {
                            anySuccess = true;
                        }
                    }
                }
                if (anySuccess) this.triggerReload();
            }
        });

        // Undo button
        this.undoButton = new UIButton(IKey.raw("Undo"), (b) ->
        {
            File f = this.getModelFile();
            if (f != null)
            {
                if (UndoManager.undo(f))
                {
                    this.triggerReload();
                }
                else
                {
                    this.getContext().notifyError(IKey.raw("Nothing to Undo!"));
                }
            }
        });

        // Redo button
        this.redoButton = new UIButton(IKey.raw("Redo"), (b) ->
        {
            File f = this.getModelFile();
            if (f != null)
            {
                if (UndoManager.redo(f))
                {
                    this.triggerReload();
                }
                else
                {
                    this.getContext().notifyError(IKey.raw("Nothing to Redo!"));
                }
            }
        });

        // Reset to Original button
        this.resetOriginalButton = new UIButton(IKey.raw("Reset to Original"), (b) ->
        {
            File f = this.getModelFile();
            if (f != null)
            {
                if (UndoManager.resetToOriginal(f))
                {
                    this.triggerReload();
                }
                else
                {
                    this.getContext().notifyError(IKey.raw("No Original Snapshot Found!"));
                }
            }
        });
        this.resetOriginalButton.color(0xff4444);

        // Accept as New Original button
        this.acceptOriginalButton = new UIButton(IKey.raw("Accept as New Original"), (b) ->
        {
            File f = this.getModelFile();
            if (f != null)
            {
                if (UndoManager.acceptAsNewOriginal(f))
                {
                    this.getContext().notifySuccess(IKey.raw("Current state saved as new original!"));
                }
                else
                {
                    this.getContext().notifyError(IKey.raw("Failed to save new original!"));
                }
            }
        });

        // Layout in the right-side options scroll view provided by UIFormPanel
        this.options.add(UI.column(UIConstants.MARGIN,
            UI.label(IKey.raw("Bones")),
            this.searchBonesInput.marginTop(-2),
            this.bonesList.marginTop(-UIConstants.MARGIN),
            UI.row(UI.label(IKey.raw("Animations"), 14).labelAnchor(0, 0.5F), this.onlyTweakedToggle, this.allAnimationsToggle).h(14).marginTop(5),
            this.searchAnimationsInput.marginTop(-4),
            this.animationsList.marginTop(-UIConstants.MARGIN),
            UI.label(IKey.raw("Presets")).marginTop(5),
            UI.row(this.presetHeadPosBtn, this.presetHeadNegBtn),
            UI.row(this.presetBreathingBtn, this.presetClearBtn),
            UI.label(IKey.raw("Target Bone")).marginTop(5),
            this.targetBoneInput,
            UI.row(this.injectXPosToggle, this.injectXNegToggle),
            UI.row(this.injectYPosToggle, this.injectYNegToggle),
            UI.row(this.injectZPosToggle, this.injectZNegToggle),
            UI.row(UI.label(IKey.raw("Math Modifier")).labelAnchor(0, 0.5F), this.multiplierTrackpad).h(20).marginTop(5),
            this.mathModifierInput,
            UI.row(this.quickModHalfBtn, this.quickModInvBtn, this.quickModSinBtn),
            this.injectQueryButton.marginTop(5),
            this.removeQueriesButton,
            UI.row(this.undoButton, this.redoButton),
            this.resetOriginalButton,
            this.acceptOriginalButton
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

        // Capture original file state for undo/redo on first open
        File modelFile = this.getModelFile();
        if (modelFile != null && modelFile.exists())
        {
            UndoManager.captureOriginal(modelFile);
        }

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

        // 1. model.bbs.json
        Link bbsLink = Link.assets("models/" + modelId + "/model.bbs.json");
        File bbsFile = BBSMod.getProvider().getFile(bbsLink);
        if (bbsFile != null && bbsFile.exists()) return bbsFile;

        // 2. {modelId}.bbs.json (e.g. hugeplayer.bbs.json, nvl1.bbs.json)
        Link modelIdBbsLink = Link.assets("models/" + modelId + "/" + modelId + ".bbs.json");
        File modelIdBbsFile = BBSMod.getProvider().getFile(modelIdBbsLink);
        if (modelIdBbsFile != null && modelIdBbsFile.exists()) return modelIdBbsFile;

        // 3. model.json
        Link jsonLink = Link.assets("models/" + modelId + "/model.json");
        File jsonFile = BBSMod.getProvider().getFile(jsonLink);
        if (jsonFile != null && jsonFile.exists()) return jsonFile;

        // 4. {modelId}.json
        Link modelIdJsonLink = Link.assets("models/" + modelId + "/" + modelId + ".json");
        File modelIdJsonFile = BBSMod.getProvider().getFile(modelIdJsonLink);
        if (modelIdJsonFile != null && modelIdJsonFile.exists()) return modelIdJsonFile;

        // 5. model.geo.json
        Link geoLink = Link.assets("models/" + modelId + "/model.geo.json");
        File geoFile = BBSMod.getProvider().getFile(geoLink);
        if (geoFile != null && geoFile.exists()) return geoFile;

        // 6. {modelId}.geo.json
        Link modelIdGeoLink = Link.assets("models/" + modelId + "/" + modelId + ".geo.json");
        File modelIdGeoFile = BBSMod.getProvider().getFile(modelIdGeoLink);
        if (modelIdGeoFile != null && modelIdGeoFile.exists()) return modelIdGeoFile;

        // 7. Search folder for any .json or .bbs.json file
        Link modelFolderLink = Link.assets("models/" + modelId);
        File modelFolder = BBSMod.getProvider().getFile(modelFolderLink);
        if (modelFolder != null && modelFolder.isDirectory())
        {
            File[] jsonFiles = modelFolder.listFiles((dir, name) -> name.endsWith(".json") && !name.endsWith(".animation.json"));
            if (jsonFiles != null && jsonFiles.length > 0)
            {
                for (File f : jsonFiles)
                {
                    if (f.getName().endsWith(".bbs.json")) return f;
                }
                return jsonFiles[0];
            }
        }

        // 8. Search animations folder
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
        Set<String> animSet = new LinkedHashSet<>();
        Map<String, Boolean> animStatusMap = new java.util.HashMap<>();

        File modelFile = this.getModelFile();
        if (modelFile != null && modelFile.exists())
        {
            // Single file read: get both names and tweaked status at once
            animStatusMap = AnimTweakerEngine.getAnimationsWithStatus(modelFile);
            animSet.addAll(animStatusMap.keySet());
        }

        try
        {
            ModelInstance model = ModelFormRenderer.getModel(this.form);
            if (model != null && model.animations != null && model.animations.animations != null)
            {
                animSet.addAll(model.animations.animations.keySet());
            }
        }
        catch (Exception ignored) {}

        List<String> sorted = new ArrayList<>(animSet);
        java.util.Collections.sort(sorted);

        for (String anim : sorted)
        {
            boolean isTweaked = animStatusMap.getOrDefault(anim, false);
            if (this.onlyTweakedToggle != null && this.onlyTweakedToggle.getValue() && !isTweaked)
            {
                continue;
            }

            if (filter.isEmpty() || anim.toLowerCase().contains(filter.toLowerCase()))
            {
                String label = isTweaked ? anim + " 🛠" : anim;
                this.animationsList.add(label);
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