package com.example.bbsanimtweaker.client.gui;

import com.example.bbsanimtweaker.client.logic.AnimTweakerEngine;
import com.example.bbsanimtweaker.client.logic.UndoManager;
import mchorse.bbs_mod.BBSMod;
import mchorse.bbs_mod.BBSModClient;
import mchorse.bbs_mod.cubic.ModelInstance;
import mchorse.bbs_mod.forms.forms.ModelForm;
import mchorse.bbs_mod.forms.renderers.ModelFormRenderer;
import mchorse.bbs_mod.l10n.keys.IKey;
import mchorse.bbs_mod.resources.Link;
import mchorse.bbs_mod.ui.forms.editors.forms.UIForm;
import mchorse.bbs_mod.ui.forms.editors.forms.UIModelForm;
import mchorse.bbs_mod.ui.forms.editors.panels.UIFormPanel;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIButton;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIToggle;
import mchorse.bbs_mod.ui.framework.elements.input.UISliderTrackpad;
import mchorse.bbs_mod.ui.framework.elements.input.list.UIStringList;
import mchorse.bbs_mod.ui.framework.elements.input.text.UITextbox;
import mchorse.bbs_mod.ui.framework.elements.utils.UILabel;
import mchorse.bbs_mod.ui.utils.UI;
import mchorse.bbs_mod.ui.utils.UIConstants;
import mchorse.bbs_mod.ui.utils.icons.Icons;
import mchorse.bbs_mod.utils.colors.Colors;

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
 * Redesigned for BBS FS 2.6 with compact, icon-driven, slider-based UI.
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

    // History & Snapshot Buttons
    public UIButton undoButton;
    public UIButton redoButton;
    public UIButton resetOriginalButton;
    public UIButton acceptOriginalButton;

    // Quick Presets
    public UIButton presetHeadPosBtn;
    public UIButton presetHeadNegBtn;
    public UIButton presetBreathingBtn;
    public UIButton presetClearBtn;

    // Target Bone
    public UITextbox targetBoneInput;

    // Color-Coded Axis Toggles
    public boolean isXPos = false;
    public boolean isXNeg = false;
    public boolean isYPos = false;
    public boolean isYNeg = false;
    public boolean isZPos = false;
    public boolean isZNeg = false;

    public UIButton btnXPos;
    public UIButton btnXNeg;
    public UIButton btnYPos;
    public UIButton btnYNeg;
    public UIButton btnZPos;
    public UIButton btnZNeg;

    // Multiplier Slider & Math Modifiers
    public UISliderTrackpad multiplierSlider;
    public UISliderTrackpad multiplierTrackpad; // Backward-compatibility alias
    public UITextbox mathModifierInput;
    public UIButton quickModHalfBtn;
    public UIButton quickModInvBtn;
    public UIButton quickModSinBtn;

    // Main Actions
    public UIButton injectQueryButton;
    public UIButton removeQueriesButton;

    public UIModelATPanel(UIForm editor)
    {
        super(editor);

        // 1. Bones search & list
        this.searchBonesInput = new UITextbox(1000, (str) ->
        {
            this.updateBonesList(str);
        });
        this.searchBonesInput.placeholder(IKey.raw("Search bones..."));
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
        this.bonesList.background().h(120);

        // 2. Animations search & list
        this.searchAnimationsInput = new UITextbox(1000, (str) ->
        {
            this.updateAnimationsList(str);
        });
        this.searchAnimationsInput.placeholder(IKey.raw("Search animations..."));
        this.searchAnimationsInput.h(20);

        this.animationsList = new UIStringList((l) -> {});
        this.animationsList.multi();
        this.animationsList.background().h(95);

        this.onlyTweakedToggle = new UIToggle(IKey.raw("Modded"), false, (b) ->
        {
            this.updateAnimationsList(this.searchAnimationsInput.getText());
        });
        this.onlyTweakedToggle.tooltip(IKey.raw("Show only modified animations (with \uD83D\uDEE0)"));

        this.allAnimationsToggle = new UIToggle(IKey.raw("All"), true, (b) -> {});
        this.allAnimationsToggle.tooltip(IKey.raw("If enabled, applies to all animations of this model; otherwise only selected."));

        // 3. History toolbar buttons
        this.undoButton = new UIButton(IKey.raw("Undo"), (b) -> this.undo());
        this.undoButton.tooltip(IKey.raw("Undo last change"));
        this.undoButton.color(0x282c35, 0x383e4a).textColor(Colors.WHITE, true);

        this.redoButton = new UIButton(IKey.raw("Redo"), (b) -> this.redo());
        this.redoButton.tooltip(IKey.raw("Redo last undone change"));
        this.redoButton.color(0x282c35, 0x383e4a).textColor(Colors.WHITE, true);

        this.resetOriginalButton = new UIButton(IKey.raw("Reset"), (b) -> this.resetOriginal());
        this.resetOriginalButton.tooltip(IKey.raw("Reset model file to original snapshot"));
        this.resetOriginalButton.color(0x382e28, 0x4d3e36).textColor(0xffc107, true);

        this.acceptOriginalButton = new UIButton(IKey.raw("Save Orig"), (b) -> this.acceptOriginal());
        this.acceptOriginalButton.tooltip(IKey.raw("Save current state as new original snapshot"));
        this.acceptOriginalButton.color(0x283828, 0x364d36).textColor(0x81c784, true);

        // 4. Quick Presets
        this.presetHeadPosBtn = new UIButton(IKey.raw("Look (+)"), (b) -> {
            this.isXPos = true;  this.isXNeg = false;
            this.isYPos = true;  this.isYNeg = false;
            this.isZPos = false; this.isZNeg = false;
            this.mathModifierInput.setText("");
            this.multiplierSlider.setValue(1.0);
            this.updateAxisStyles();
            if (this.targetBoneInput.getText().isEmpty()) this.setTargetBone("head");
        });
        this.presetHeadPosBtn.tooltip(IKey.raw("Preset: Head Look (+) on X and Y axes"));
        this.presetHeadPosBtn.color(0x282c35, 0x383e4a).textColor(Colors.WHITE, true);

        this.presetHeadNegBtn = new UIButton(IKey.raw("Look (-)"), (b) -> {
            this.isXPos = false; this.isXNeg = true;
            this.isYPos = false; this.isYNeg = true;
            this.isZPos = false; this.isZNeg = false;
            this.mathModifierInput.setText("");
            this.multiplierSlider.setValue(1.0);
            this.updateAxisStyles();
            if (this.targetBoneInput.getText().isEmpty()) this.setTargetBone("head");
        });
        this.presetHeadNegBtn.tooltip(IKey.raw("Preset: Head Look (-) on X and Y axes"));
        this.presetHeadNegBtn.color(0x282c35, 0x383e4a).textColor(Colors.WHITE, true);

        this.presetBreathingBtn = new UIButton(IKey.raw("Breath"), (b) -> {
            this.isXPos = false; this.isXNeg = false;
            this.isYPos = false; this.isYNeg = false;
            this.isZPos = true;  this.isZNeg = false;
            this.mathModifierInput.setText("* math.sin(query.anim_time * 150) * 0.3");
            this.updateAxisStyles();
            if (this.targetBoneInput.getText().isEmpty()) this.setTargetBone("body");
        });
        this.presetBreathingBtn.tooltip(IKey.raw("Preset: Subtle breathing motion on Z axis"));
        this.presetBreathingBtn.color(0x282c35, 0x383e4a).textColor(Colors.WHITE, true);

        this.presetClearBtn = new UIButton(IKey.raw("Clear"), (b) -> {
            this.isXPos = false; this.isXNeg = false;
            this.isYPos = false; this.isYNeg = false;
            this.isZPos = false; this.isZNeg = false;
            this.mathModifierInput.setText("");
            this.multiplierSlider.setValue(1.0);
            this.updateAxisStyles();
        });
        this.presetClearBtn.tooltip(IKey.raw("Clear all active axes and math modifier"));
        this.presetClearBtn.color(0x382626, 0x4d3232).textColor(0xff8888, true);

        // 5. Target Bone
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
        this.targetBoneInput.placeholder(IKey.raw("e.g. head"));
        this.targetBoneInput.h(UIConstants.CONTROL_HEIGHT);

        // 6. Color-Coded Axis Toggles
        this.btnXPos = new UIButton(IKey.raw("+Pos"), (b) -> {
            this.isXPos = !this.isXPos;
            if (this.isXPos) this.isXNeg = false;
            this.updateAxisStyles();
        });
        this.btnXPos.tooltip(IKey.raw("X Axis (Pitch) Positive (+query.head_pitch)"));

        this.btnXNeg = new UIButton(IKey.raw("-Neg"), (b) -> {
            this.isXNeg = !this.isXNeg;
            if (this.isXNeg) this.isXPos = false;
            this.updateAxisStyles();
        });
        this.btnXNeg.tooltip(IKey.raw("X Axis (Pitch) Negative (-query.head_pitch)"));

        this.btnYPos = new UIButton(IKey.raw("+Pos"), (b) -> {
            this.isYPos = !this.isYPos;
            if (this.isYPos) this.isYNeg = false;
            this.updateAxisStyles();
        });
        this.btnYPos.tooltip(IKey.raw("Y Axis (Yaw) Positive (+query.head_yaw)"));

        this.btnYNeg = new UIButton(IKey.raw("-Neg"), (b) -> {
            this.isYNeg = !this.isYNeg;
            if (this.isYNeg) this.isYPos = false;
            this.updateAxisStyles();
        });
        this.btnYNeg.tooltip(IKey.raw("Y Axis (Yaw) Negative (-query.head_yaw)"));

        this.btnZPos = new UIButton(IKey.raw("+Pos"), (b) -> {
            this.isZPos = !this.isZPos;
            if (this.isZPos) this.isZNeg = false;
            this.updateAxisStyles();
        });
        this.btnZPos.tooltip(IKey.raw("Z Axis (Roll) Positive (+query.head_yaw)"));

        this.btnZNeg = new UIButton(IKey.raw("-Neg"), (b) -> {
            this.isZNeg = !this.isZNeg;
            if (this.isZNeg) this.isZPos = false;
            this.updateAxisStyles();
        });
        this.btnZNeg.tooltip(IKey.raw("Z Axis (Roll) Negative (-query.head_yaw)"));

        this.updateAxisStyles();

        // 7. BBS 2.6 Native Multiplier Slider (UISliderTrackpad)
        this.multiplierSlider = new UISliderTrackpad((v) -> {
            double d = Math.round(v.doubleValue() * 100.0) / 100.0;
            if (d == 0) this.mathModifierInput.setText("");
            else this.mathModifierInput.setText("* " + d);
        });
        this.multiplierSlider.limit(0D, 2D).increment(0.05D).values(0.05D, 0.01D, 0.1D);
        this.multiplierSlider.setValue(1.0);
        this.multiplierSlider.tooltip(IKey.raw("Multiplier Slider (0.0 to 2.0 scale)"));
        this.multiplierTrackpad = this.multiplierSlider;

        // 8. Math Modifier & Quick Math Buttons
        this.mathModifierInput = new UITextbox(1000, (str) -> {
            String val = str.trim();
            if (val.isEmpty())
            {
                this.mathModifierInput.textbox.setColor(0xffffff);
                return;
            }
            boolean validChars = val.matches("^[0-9a-zA-Z_ .+*/%()-]*$");
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
                if (val.startsWith("* "))
                {
                    try
                    {
                        double parsed = Double.parseDouble(val.substring(2).trim());
                        if (parsed >= 0 && parsed <= 2.0)
                        {
                            this.multiplierSlider.setValue(parsed);
                        }
                    }
                    catch (Exception ignored) {}
                }
            }
        });
        this.mathModifierInput.placeholder(IKey.raw("e.g. * 0.5"));
        this.mathModifierInput.tooltip(IKey.raw("Custom Math Modifier (e.g. * 0.5)"));
        this.mathModifierInput.h(UIConstants.CONTROL_HEIGHT);

        this.quickModHalfBtn = new UIButton(IKey.raw("½"), (b) -> {
            this.mathModifierInput.setText("* 0.5");
            this.multiplierSlider.setValue(0.5);
        });
        this.quickModHalfBtn.tooltip(IKey.raw("Half scale (* 0.5)"));
        this.quickModHalfBtn.color(0x282c35, 0x383e4a).h(UIConstants.CONTROL_HEIGHT);

        this.quickModInvBtn = new UIButton(IKey.raw("±"), (b) -> {
            this.mathModifierInput.setText("* -1.0");
            // -1.0 is outside the slider's 0–2 range; reset slider to neutral
            // so it doesn't show a stale value
            this.multiplierSlider.setValue(1.0);
        });
        this.quickModInvBtn.tooltip(IKey.raw("Invert scale (* -1.0)"));
        this.quickModInvBtn.color(0x282c35, 0x383e4a).h(UIConstants.CONTROL_HEIGHT);

        this.quickModSinBtn = new UIButton(IKey.raw("sin"), (b) -> {
            this.mathModifierInput.setText("* math.sin(query.anim_time * 150) * 0.3");
        });
        this.quickModSinBtn.tooltip(IKey.raw("Sine oscillation (* math.sin(...))"));
        this.quickModSinBtn.color(0x282c35, 0x383e4a).h(UIConstants.CONTROL_HEIGHT);

        // 9. Main Action Buttons
        this.injectQueryButton = new UIButton(IKey.raw("Inject Query"), (b) -> this.injectQueries());
        this.injectQueryButton.tooltip(IKey.raw("Inject active Molang queries into target bone / animations"));

        this.removeQueriesButton = new UIButton(IKey.raw("Remove"), (b) -> this.removeQueries());
        this.removeQueriesButton.tooltip(IKey.raw("Remove Molang queries from target bone / animations"));
        this.removeQueriesButton.color(0x382626, 0x4d3232).textColor(0xff8888, true);

        // Section Labels
        UILabel labelBones = UI.label(IKey.raw("Bones"));
        UILabel labelAnims = UI.label(IKey.raw("Animations"), 14).labelAnchor(0, 0.5F);
        UILabel labelHistory = UI.label(IKey.raw("History"));
        UILabel labelPresets = UI.label(IKey.raw("Presets"));
        UILabel labelTarget = UI.label(IKey.raw("Target Bone"));
        UILabel labelX = UI.label(IKey.raw("X (Pitch)")).color(0xff6666).labelAnchor(0, 0.5F);
        UILabel labelY = UI.label(IKey.raw("Y (Yaw)")).color(0x55ff77).labelAnchor(0, 0.5F);
        UILabel labelZ = UI.label(IKey.raw("Z (Roll)")).color(0x66aaff).labelAnchor(0, 0.5F);
        UILabel labelSlider = UI.label(IKey.raw("Multiplier")).labelAnchor(0, 0.5F);

        // Compact right-side options layout
        this.options.add(UI.column(UIConstants.MARGIN,
            labelBones,
            this.searchBonesInput.marginTop(-2),
            this.bonesList.marginTop(-UIConstants.MARGIN),
            UI.row(labelAnims, this.onlyTweakedToggle.w(65), this.allAnimationsToggle.w(40)).h(14).marginTop(4),
            this.searchAnimationsInput.marginTop(-4),
            this.animationsList.marginTop(-UIConstants.MARGIN),
            labelHistory.marginTop(5),
            UI.row(this.undoButton, this.redoButton, this.resetOriginalButton, this.acceptOriginalButton).h(UIConstants.CONTROL_HEIGHT),
            labelPresets.marginTop(5),
            UI.row(this.presetHeadPosBtn, this.presetHeadNegBtn, this.presetBreathingBtn, this.presetClearBtn).h(UIConstants.CONTROL_HEIGHT),
            labelTarget.marginTop(5),
            this.targetBoneInput,
            UI.row(labelX, this.btnXPos.w(38), this.btnXNeg.w(38)).h(UIConstants.CONTROL_HEIGHT).marginTop(3),
            UI.row(labelY, this.btnYPos.w(38), this.btnYNeg.w(38)).h(UIConstants.CONTROL_HEIGHT),
            UI.row(labelZ, this.btnZPos.w(38), this.btnZNeg.w(38)).h(UIConstants.CONTROL_HEIGHT),
            UI.row(labelSlider, this.multiplierSlider).h(UIConstants.CONTROL_HEIGHT).marginTop(4),
            UI.row(this.mathModifierInput, this.quickModHalfBtn.w(22), this.quickModInvBtn.w(22), this.quickModSinBtn.w(28)).h(UIConstants.CONTROL_HEIGHT),
            UI.row(this.injectQueryButton, this.removeQueriesButton.w(60)).h(20).marginTop(5)
        ).relative(this.options).x(5).y(5).w(1F, -10).h(0));
    }

    public void updateAxisStyles()
    {
        updateAxisButton(this.btnXPos, this.isXPos, 0xc62828);
        updateAxisButton(this.btnXNeg, this.isXNeg, 0xb71c1c);
        updateAxisButton(this.btnYPos, this.isYPos, 0x2e7d32);
        updateAxisButton(this.btnYNeg, this.isYNeg, 0x1b5e20);
        updateAxisButton(this.btnZPos, this.isZPos, 0x1565c0);
        updateAxisButton(this.btnZNeg, this.isZNeg, 0x0d47a1);
    }

    private void updateAxisButton(UIButton btn, boolean active, int activeColor)
    {
        if (active)
        {
            btn.color(activeColor, Colors.mulRGB(activeColor, 1.15F));
            btn.textColor(Colors.WHITE, true);
        }
        else
        {
            btn.color(0x252830, 0x323742);
            btn.textColor(0x888888, false);
        }
    }

    private void undo()
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
    }

    private void redo()
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
    }

    private void resetOriginal()
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
    }

    private void acceptOriginal()
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
    }

    private void injectQueries()
    {
        String boneText = this.targetBoneInput.getText().trim();
        if (boneText.isEmpty())
        {
            this.getContext().notifyError(IKey.raw("Target bone is empty!"));
            return;
        }

        String xQuery = null;
        if (this.isXPos)      xQuery = "+query.head_pitch";
        else if (this.isXNeg) xQuery = "-query.head_pitch";

        String yQuery = null;
        if (this.isYPos)      yQuery = "+query.head_yaw";
        else if (this.isYNeg) yQuery = "-query.head_yaw";

        String zQuery = null;
        if (this.isZPos)      zQuery = "+query.head_yaw";
        else if (this.isZNeg) zQuery = "-query.head_yaw";

        if (xQuery == null && yQuery == null && zQuery == null)
        {
            this.getContext().notifyError(IKey.raw("Select at least one axis (+Pos or -Neg)!"));
            return;
        }

        String[] targetBones = boneText.split("\\s*,\\s*");

        List<String> targetAnims = this.resolveTargetAnims();
        if (targetAnims == null && !this.allAnimationsToggle.getValue())
        {
            // resolveTargetAnims returns null on error (already notified)
            return;
        }

        File f = this.getModelFile();
        if (f != null && f.exists())
        {
            String modifier = this.mathModifierInput.getText().trim();
            if (!modifier.isEmpty() && !this.isValidMathModifier(modifier))
            {
                return; // error already shown inside isValidMathModifier
            }

            UndoManager.pushState(f);

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
            if (anySuccess)
            {
                this.triggerReload();
            }
            else
            {
                this.getContext().notifyError(IKey.raw("Could not inject queries into model!"));
            }
        }
    }

    private void removeQueries()
    {
        String boneText = this.targetBoneInput.getText().trim();
        if (boneText.isEmpty())
        {
            this.getContext().notifyError(IKey.raw("Target bone is empty!"));
            return;
        }

        String[] targetBones = boneText.split("\\s*,\\s*");

        List<String> targetAnims = this.resolveTargetAnims();
        if (targetAnims == null && !this.allAnimationsToggle.getValue())
        {
            return;
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
            if (anySuccess)
            {
                this.triggerReload();
            }
            else
            {
                this.getContext().notifyError(IKey.raw("No queries found to remove!"));
            }
        }
    }

    // ---- Private helpers (DRY) ----

    /**
     * Strips the visual suffix appended to tweaked animation names in the list
     * (e.g. " 🛠" or " [Tweaked]") to recover the real animation key.
     */
    private static String stripAnimSuffix(String s)
    {
        if (s.endsWith(" \uD83D\uDEE0")) return s.substring(0, s.length() - " \uD83D\uDEE0".length());
        if (s.endsWith(" [Tweaked]"))  return s.substring(0, s.length() - " [Tweaked]".length());
        return s;
    }

    /**
     * Validates the math modifier string: only allows safe characters and balanced parentheses.
     * Shows an error notification and returns false if invalid.
     */
    private boolean isValidMathModifier(String modifier)
    {
        if (!modifier.matches("^[0-9a-zA-Z_ .+*/%()-]*$"))
        {
            this.getContext().notifyError(IKey.raw("Invalid character in math modifier!"));
            return false;
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
            return false;
        }
        return true;
    }

    /**
     * Resolves the list of target animation names from the UI state.
     * Returns null when "All Animations" is active (means apply to all).
     * Returns an empty-check-failed null (after notifying the user) when
     * "All" is off but nothing is selected.
     */
    private List<String> resolveTargetAnims()
    {
        if (this.allAnimationsToggle.getValue()) return null; // null = all animations

        List<String> rawAnims = this.animationsList.getCurrent();
        if (rawAnims == null || rawAnims.isEmpty())
        {
            this.getContext().notifyError(IKey.raw("No animations selected! Check 'All' or select animations."));
            return null;
        }

        List<String> targetAnims = new ArrayList<>();
        for (String s : rawAnims)
        {
            targetAnims.add(stripAnimSuffix(s));
        }
        return targetAnims;
    }

    @Override
    public void setVisible(boolean visible)
    {
        super.setVisible(visible);
        if (visible && this.form != null)
        {
            this.syncBoneFromPoseTab();
        }
    }

    @Override
    public void startEdit(ModelForm form)
    {
        super.startEdit(form);
        this.updateAnimationsList(this.searchAnimationsInput.getText());
        this.updateBonesList(this.searchBonesInput.getText());

        File modelFile = this.getModelFile();
        if (modelFile != null && modelFile.exists())
        {
            UndoManager.captureOriginal(modelFile);
        }

        if (form.model.get() != null)
        {
            String modelId = form.model.get();
            String lastBone = lastSelectedBones.getOrDefault(modelId, "");
            this.targetBoneInput.setText(lastBone);
            this.selectBoneInList(lastBone);
        }
    }

    public String getTargetBone()
    {
        String text = this.targetBoneInput != null ? this.targetBoneInput.getText() : null;
        if (text == null || text.trim().isEmpty())
        {
            if (this.form != null && this.form.model.get() != null)
            {
                text = lastSelectedBones.get(this.form.model.get());
            }
        }
        return text;
    }

    public void setTargetBone(String bone)
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

        try
        {
            String first = (bone != null && !bone.isEmpty()) ? bone.split(",")[0].trim() : "";
            if (!first.isEmpty())
            {
                this.boneSelection().set(first);
                if (this.editor instanceof UIModelForm modelForm && modelForm.modelPanel != null && modelForm.modelPanel.poseEditor != null)
                {
                    modelForm.modelPanel.poseEditor.selectBone(first);
                }
            }
        }
        catch (Exception ignored) {}
    }

    @Override
    public boolean pickBoneInList(String bone)
    {
        if (bone != null && !bone.isEmpty())
        {
            this.setTargetBone(bone);
            this.selectBoneInList(bone);
            return true;
        }
        return false;
    }

    @Override
    public void pickBone(String bone)
    {
        if (bone != null && !bone.isEmpty())
        {
            this.setTargetBone(bone);
            this.selectBoneInList(bone);
        }
    }

    public void syncBoneFromPoseTab()
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

        Link modelIdBbsLink = Link.assets("models/" + modelId + "/" + modelId + ".bbs.json");
        File modelIdBbsFile = BBSMod.getProvider().getFile(modelIdBbsLink);
        if (modelIdBbsFile != null && modelIdBbsFile.exists()) return modelIdBbsFile;

        Link jsonLink = Link.assets("models/" + modelId + "/model.json");
        File jsonFile = BBSMod.getProvider().getFile(jsonLink);
        if (jsonFile != null && jsonFile.exists()) return jsonFile;

        Link modelIdJsonLink = Link.assets("models/" + modelId + "/" + modelId + ".json");
        File modelIdJsonFile = BBSMod.getProvider().getFile(modelIdJsonLink);
        if (modelIdJsonFile != null && modelIdJsonFile.exists()) return modelIdJsonFile;

        Link geoLink = Link.assets("models/" + modelId + "/model.geo.json");
        File geoFile = BBSMod.getProvider().getFile(geoLink);
        if (geoFile != null && geoFile.exists()) return geoFile;

        Link modelIdGeoLink = Link.assets("models/" + modelId + "/" + modelId + ".geo.json");
        File modelIdGeoFile = BBSMod.getProvider().getFile(modelIdGeoLink);
        if (modelIdGeoFile != null && modelIdGeoFile.exists()) return modelIdGeoFile;

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
        Map<String, Boolean> animStatusMap = new HashMap<>();

        File modelFile = this.getModelFile();
        if (modelFile != null && modelFile.exists())
        {
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
                String label = isTweaked ? anim + " \uD83D\uDEE0" : anim;
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
