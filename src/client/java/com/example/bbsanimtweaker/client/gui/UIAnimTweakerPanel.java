package com.example.bbsanimtweaker.client.gui;

import com.example.bbsanimtweaker.client.logic.AnimTweakerEngine;
import mchorse.bbs_mod.BBSModClient;
import mchorse.bbs_mod.ui.dashboard.UIDashboard;
import mchorse.bbs_mod.ui.dashboard.panels.UIDashboardPanel;
import mchorse.bbs_mod.ui.framework.elements.UIElement;
import mchorse.bbs_mod.ui.framework.elements.UIScrollView;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIButton;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIToggle;
import mchorse.bbs_mod.ui.framework.elements.input.list.UIStringList;
import mchorse.bbs_mod.ui.framework.elements.input.text.UITextbox;
import mchorse.bbs_mod.ui.forms.editors.utils.UIFormRenderer;
import mchorse.bbs_mod.ui.utils.UI;
import mchorse.bbs_mod.l10n.keys.IKey;
import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.utils.colors.Colors;

import java.io.File;
import java.util.List;

public class UIAnimTweakerPanel extends UIDashboardPanel
{
    public UIFormRenderer renderer;
    private final mchorse.bbs_mod.forms.forms.ModelForm form = new mchorse.bbs_mod.forms.forms.ModelForm();
    public String currentModelId;

    public UIScrollView leftPanel;
    public UIScrollView rightPanel;

    // Left Panel
    public UITextbox searchModelsInput;
    public UIStringList modelsList;

    public UIButton restoreBackupButton;
    public UITextbox searchAnimationsInput;
    public UIStringList animationsList;
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
    
    public UIToggle allAnimationsToggle;

    // Right Panel
    public UIButton openInButton;
    public UIButton pickTextureButton;
    public UITextbox searchBodyPartsInput;
    public UIStringList bodyPartsList;
    public mchorse.bbs_mod.ui.framework.elements.input.UIPropTransform transformPanel;

    private UIElement leftContent;
    private UIElement rightContent;

    // Resizing & Browsing logic
    public int leftPanelWidth = 200;
    public int rightPanelWidth = 200;
    private int dragMode = 0; // 0 = none, 1 = left panel edge, 2 = right panel edge
    private String currentPath = ""; // Virtual directory path for models

    // The live transform data for the UIPropTransform widget
    private final mchorse.bbs_mod.utils.pose.Transform liveTransform = new mchorse.bbs_mod.utils.pose.Transform();

    public UIAnimTweakerPanel(UIDashboard dashboard)
    {
        super(dashboard);

        /* Left Panel - Tweaks */
        this.leftPanel = new UIScrollView();
        this.leftPanel.relative(this).w(this.leftPanelWidth).h(1F);
        this.leftPanel.scroll.scrollItemSize = 16;
        this.setupLeftPanel();

        /* Right Panel - Model/Bones */
        this.rightPanel = new UIScrollView();
        this.rightPanel.relative(this).x(1F, -this.rightPanelWidth).w(this.rightPanelWidth).h(1F);
        this.rightPanel.scroll.scrollItemSize = 16;
        this.setupRightPanel();

        /* Center Panel - 3D Renderer */
        this.renderer = new UIFormRenderer();
        this.renderer.form = this.form;
        this.renderer.relative(this).x(this.leftPanelWidth).w(1F, -(this.leftPanelWidth + this.rightPanelWidth)).h(1F);

        this.add(this.renderer, this.leftPanel, this.rightPanel);
        
        this.leftPanel.setVisible(true);
        this.rightPanel.setVisible(false);
        this.renderer.setVisible(false);

        this.updateModelsList("");
    }

    private mchorse.bbs_mod.cubic.ModelInstance getModelInstance()
    {
        mchorse.bbs_mod.forms.renderers.FormRenderer r = mchorse.bbs_mod.forms.FormUtilsClient.getRenderer(this.form);
        if (r instanceof mchorse.bbs_mod.forms.renderers.ModelFormRenderer modelRenderer)
        {
            return modelRenderer.getModel();
        }
        return null;
    }

    private void updateTransformPanel()
    {
        if (this.transformPanel == null) return;

        mchorse.bbs_mod.cubic.ModelInstance instance = this.getModelInstance();
        String bone = this.targetBoneInput.getText();
        if (instance == null || bone == null || bone.isEmpty()) return;

        mchorse.bbs_mod.utils.pose.PoseTransform pt = this.form.pose.get().transforms.get(bone);

        this.liveTransform.translate.set(pt != null ? pt.translate : new org.joml.Vector3f());
        this.liveTransform.scale.set(pt != null ? pt.scale : new org.joml.Vector3f(1, 1, 1));
        this.liveTransform.rotate.set(pt != null ? pt.rotate : new org.joml.Vector3f());
        this.liveTransform.rotate2.set(pt != null ? pt.rotate2 : new org.joml.Vector3f());

        this.transformPanel.setTransform(this.liveTransform);
    }

    public IKey getTitle()
    {
        return IKey.raw("Anim Tweaker V2");
    }

    private File getModelFile()
    {
        if (this.currentModelId == null) return null;
        
        mchorse.bbs_mod.resources.Link bbsLink = mchorse.bbs_mod.resources.Link.assets("models/" + this.currentModelId + "/model.bbs.json");
        File bbsFile = mchorse.bbs_mod.BBSMod.getProvider().getFile(bbsLink);
        if (bbsFile != null && bbsFile.exists()) {
            return bbsFile;
        }

        mchorse.bbs_mod.resources.Link animFolderLink = mchorse.bbs_mod.resources.Link.assets("models/" + this.currentModelId + "/animations");
        File animFolder = mchorse.bbs_mod.BBSMod.getProvider().getFile(animFolderLink);
        if (animFolder != null && animFolder.isDirectory()) {
            File[] files = animFolder.listFiles((dir, name) -> name.endsWith(".animation.json"));
            if (files != null && files.length > 0) {
                return files[0];
            }
        }
        
        return bbsFile;
    }

    private void triggerReload()
    {
        if (this.currentModelId == null) return;
        BBSModClient.getModels().loadModel(this.currentModelId);
        
        if (mchorse.bbs_mod.forms.FormUtilsClient.getRenderer(this.form) instanceof mchorse.bbs_mod.forms.renderers.ModelFormRenderer r)
        {
            r.resetAnimator();
        }
        
        this.updateAnimationsList(this.searchAnimationsInput.getText());
        this.updateBodyPartsList(this.searchBodyPartsInput.getText());
        
        this.getContext().notifySuccess(IKey.raw("Changes Applied! Model Reloaded."));
    }

    private void updateModelsList(String filter)
    {
        this.modelsList.clear();
        List<String> keys = new java.util.ArrayList<>(BBSModClient.getModels().getAvailableKeys());
        keys.sort(String::compareToIgnoreCase);

        if (!filter.isEmpty())
        {
            // Global search when filter is active
            for (String modelId : keys)
            {
                if (modelId.toLowerCase().contains(filter.toLowerCase()))
                {
                    this.modelsList.add(modelId);
                }
            }
        }
        else
        {
            // Folder-based navigation when search is empty
            java.util.Set<String> folders = new java.util.LinkedHashSet<>();
            java.util.Set<String> files = new java.util.LinkedHashSet<>();

            for (String modelId : keys)
            {
                if (modelId.startsWith(this.currentPath))
                {
                    String relative = modelId.substring(this.currentPath.length());
                    int slash = relative.indexOf('/');
                    if (slash != -1)
                    {
                        folders.add(relative.substring(0, slash + 1));
                    }
                    else
                    {
                        files.add(relative);
                    }
                }
            }

            // Back button
            if (!this.currentPath.isEmpty())
            {
                this.modelsList.add("< ..");
            }

            // Add sub-folders
            for (String folder : folders)
            {
                this.modelsList.add("> " + folder);
            }

            // Add model files
            for (String file : files)
            {
                this.modelsList.add("  " + file);
            }
        }
    }

    private void selectModel(String modelId)
    {
        this.currentModelId = modelId;
        this.form.model.set(modelId);
        
        this.updateAnimationsList("");
        this.updateBodyPartsList("");
        this.updateTransformPanel();
        
        this.rightPanel.setVisible(true);
        this.renderer.setVisible(true);
    }

    private void updateAnimationsList(String filter)
    {
        this.animationsList.clear();
        if (this.currentModelId == null) return;
        
        File modelFile = this.getModelFile();
        if (modelFile != null && modelFile.exists()) {
            List<String> animNames = AnimTweakerEngine.getAnimationNames(modelFile);
            for (String anim : animNames) {
                if (filter.isEmpty() || anim.toLowerCase().contains(filter.toLowerCase())) {
                    this.animationsList.add(anim);
                }
            }
        }
    }

    private void updateBodyPartsList(String filter)
    {
        this.bodyPartsList.clear();
        mchorse.bbs_mod.cubic.ModelInstance instance = this.getModelInstance();
        if (instance != null && instance.getModel() != null) {
            for (String bone : instance.getModel().getAllGroupKeys()) {
                if (filter.isEmpty() || bone.toLowerCase().contains(filter.toLowerCase())) {
                    this.bodyPartsList.add(bone);
                }
            }
        }
    }

    @Override
    protected boolean subMouseClicked(mchorse.bbs_mod.ui.framework.UIContext context)
    {
        int x = context.mouseX - this.area.x;
        
        if (Math.abs(x - this.leftPanelWidth) <= 5)
        {
            this.dragMode = 1;
            return true;
        }
        if (this.rightPanel.isVisible() && Math.abs(x - (this.area.w - this.rightPanelWidth)) <= 5)
        {
            this.dragMode = 2;
            return true;
        }
        
        return super.subMouseClicked(context);
    }

    @Override
    protected boolean subMouseReleased(mchorse.bbs_mod.ui.framework.UIContext context)
    {
        if (this.dragMode != 0)
        {
            this.dragMode = 0;
            return true;
        }
        return super.subMouseReleased(context);
    }

    @Override
    public void render(mchorse.bbs_mod.ui.framework.UIContext context)
    {
        if (this.dragMode == 1)
        {
            int newWidth = context.mouseX - this.area.x;
            this.leftPanelWidth = Math.max(120, Math.min(newWidth, 400));
            
            this.leftPanel.w(this.leftPanelWidth);
            this.renderer.x(this.leftPanelWidth).w(1F, -(this.leftPanelWidth + this.rightPanelWidth));
            this.resize();
        }
        else if (this.dragMode == 2)
        {
            int newWidth = this.area.x + this.area.w - context.mouseX;
            this.rightPanelWidth = Math.max(120, Math.min(newWidth, 400));
            
            this.rightPanel.x(1F, -this.rightPanelWidth).w(this.rightPanelWidth);
            this.renderer.x(this.leftPanelWidth).w(1F, -(this.leftPanelWidth + this.rightPanelWidth));
            this.resize();
        }
        
        super.render(context);

        // Draw hover / drag resize indicators (semitransparent vertical lines)
        int mx = context.mouseX - this.area.x;
        boolean hoverLeft = Math.abs(mx - this.leftPanelWidth) <= 5;
        boolean hoverRight = this.rightPanel.isVisible() && Math.abs(mx - (this.area.w - this.rightPanelWidth)) <= 5;

        if (this.dragMode == 1 || hoverLeft)
        {
            int color = this.dragMode == 1 ? 0xcc58a6ff : 0x66ffffff; // blue if dragging, white if hovering
            context.batcher.box(this.area.x + this.leftPanelWidth - 2, this.area.y, this.area.x + this.leftPanelWidth + 1, this.area.y + this.area.h, color);
        }
        if (this.dragMode == 2 || hoverRight)
        {
            int color = this.dragMode == 2 ? 0xcc58a6ff : 0x66ffffff; // blue if dragging, white if hovering
            context.batcher.box(this.area.x + this.area.w - this.rightPanelWidth - 2, this.area.y, this.area.x + this.area.w - this.rightPanelWidth + 1, this.area.y + this.area.h, color);
        }
    }

    private void setupLeftPanel()
    {
        // Models List & Search
        this.searchModelsInput = new UITextbox(1000, (str) -> {
            this.updateModelsList(str);
        });
        this.searchModelsInput.tooltip(IKey.raw("Search Models..."));

        this.modelsList = new UIStringList((l) -> {
            if (l.size() > 0) {
                String selected = l.get(0);
                String filter = this.searchModelsInput.getText();
                if (!filter.isEmpty())
                {
                    this.selectModel(selected);
                }
                else
                {
                    if (selected.equals("< .."))
                    {
                        String pathWithoutTrailingSlash = this.currentPath.substring(0, this.currentPath.length() - 1);
                        int lastSlash = pathWithoutTrailingSlash.lastIndexOf('/');
                        if (lastSlash != -1)
                        {
                            this.currentPath = pathWithoutTrailingSlash.substring(0, lastSlash + 1);
                        }
                        else
                        {
                            this.currentPath = "";
                        }
                        this.updateModelsList("");
                    }
                    else if (selected.startsWith("> "))
                    {
                        String folderName = selected.substring(2);
                        this.currentPath += folderName;
                        this.updateModelsList("");
                    }
                    else if (selected.startsWith("  "))
                    {
                        String fileName = selected.substring(2);
                        this.selectModel(this.currentPath + fileName);
                    }
                }
            }
        }) {
            @Override
            protected void renderElementPart(UIContext context, String element, int i, int x, int y, boolean hover, boolean selected) {
                String filter = UIAnimTweakerPanel.this.searchModelsInput.getText();
                if (!filter.isEmpty()) {
                    int lastSlash = element.lastIndexOf('/');
                    int textY = y + (this.scroll.scrollItemSize - context.batcher.getFont().getHeight()) / 2;
                    int color = hover ? Colors.HIGHLIGHT : Colors.WHITE;

                    if (lastSlash != -1) {
                        String name = element.substring(lastSlash + 1);
                        String path = element.substring(0, lastSlash);

                        context.batcher.textShadow(name, x + 4, textY, color);
                        int nameWidth = context.batcher.getFont().getWidth(name);
                        context.batcher.textShadow(" / " + path, x + 4 + nameWidth, textY, 0xff888888);
                    } else {
                        context.batcher.textShadow(element, x + 4, textY, color);
                    }
                } else {
                    super.renderElementPart(context, element, i, x, y, hover, selected);
                }
            }
        };
        this.modelsList.background().h(120);

        // Backup
        this.restoreBackupButton = new UIButton(IKey.raw("Restore Backup"), (b) -> {
            File targetFile = this.getModelFile();
            if (targetFile != null) {
                if (AnimTweakerEngine.restoreBackup(targetFile)) {
                    this.triggerReload();
                } else {
                    this.getContext().notifyError(IKey.raw("No Backup Found!"));
                }
            }
        });
        this.restoreBackupButton.color(0xff4444);

        // Animations Search & List
        this.searchAnimationsInput = new UITextbox(1000, (str) -> {
            this.updateAnimationsList(str);
        });
        this.searchAnimationsInput.tooltip(IKey.raw("Search Animations..."));

        this.animationsList = new UIStringList((l) -> {});
        this.animationsList.multi();
        this.animationsList.background().h(120);

        this.allAnimationsToggle = new UIToggle(IKey.raw("All Animations"), true, (b) -> {});
        this.allAnimationsToggle.tooltip(IKey.raw("If enabled, applies to all animations."));

        // Target Bone
        this.targetBoneInput = new UITextbox(1000, (str) -> {
            this.updateTransformPanel();
        });
        this.targetBoneInput.setText("head");

        // Restore the 6 toggles for independent axis multi-selection
        this.injectXPosToggle = new UIToggle(IKey.raw("X (Pitch) (+)"), false, (b) -> {
            if(b.getValue()) this.injectXNegToggle.setValue(false);
        });
        this.injectXNegToggle = new UIToggle(IKey.raw("X (Pitch) (-)"), false, (b) -> {
            if(b.getValue()) this.injectXPosToggle.setValue(false);
        });
        this.injectYPosToggle = new UIToggle(IKey.raw("Y (Yaw) (+)"), false, (b) -> {
            if(b.getValue()) this.injectYNegToggle.setValue(false);
        });
        this.injectYNegToggle = new UIToggle(IKey.raw("Y (Yaw) (-)"), false, (b) -> {
            if(b.getValue()) this.injectYPosToggle.setValue(false);
        });
        this.injectZPosToggle = new UIToggle(IKey.raw("Z (Yaw) (+)"), false, (b) -> {
            if(b.getValue()) this.injectZNegToggle.setValue(false);
        });
        this.injectZNegToggle = new UIToggle(IKey.raw("Z (Yaw) (-)"), false, (b) -> {
            if(b.getValue()) this.injectZPosToggle.setValue(false);
        });

        this.mathModifierInput = new UITextbox(1000, (str) -> {});
        this.mathModifierInput.tooltip(IKey.raw("Custom Math Modifier (e.g. * 0.5)"));

        // Inject / Remove
        this.injectQueryButton = new UIButton(IKey.raw("Inject Query"), (b) -> {
            String bone = this.targetBoneInput.getText();
            if (bone.isEmpty()) return;
            List<String> targetAnims = this.allAnimationsToggle.getValue() ? null : this.animationsList.getCurrent();
            if (!this.allAnimationsToggle.getValue() && (targetAnims == null || targetAnims.isEmpty())) return;

            File f = this.getModelFile();
            if (f != null && f.exists()) {
                AnimTweakerEngine.createBackup(f);
                boolean success = false;
                String modifier = this.mathModifierInput.getText();

                String xQuery = null;
                if (this.injectXPosToggle.getValue()) xQuery = "+query.head_pitch";
                else if (this.injectXNegToggle.getValue()) xQuery = "-query.head_pitch";

                String yQuery = null;
                if (this.injectYPosToggle.getValue()) yQuery = "+query.head_yaw";
                else if (this.injectYNegToggle.getValue()) yQuery = "-query.head_yaw";

                String zQuery = null;
                if (this.injectZPosToggle.getValue()) zQuery = "+query.head_yaw";
                else if (this.injectZNegToggle.getValue()) zQuery = "-query.head_yaw";

                success = AnimTweakerEngine.injectQueries(f, bone, targetAnims, xQuery, yQuery, zQuery, modifier);

                if (success) this.triggerReload();
            }
        });

        this.removeQueriesButton = new UIButton(IKey.raw("Remove Query"), (b) -> {
            String bone = this.targetBoneInput.getText();
            if (bone.isEmpty()) return;
            List<String> targetAnims = this.allAnimationsToggle.getValue() ? null : this.animationsList.getCurrent();
            if (!this.allAnimationsToggle.getValue() && (targetAnims == null || targetAnims.isEmpty())) return;

            File f = this.getModelFile();
            if (f != null && f.exists()) {
                AnimTweakerEngine.createBackup(f);
                if (AnimTweakerEngine.removeQueries(f, bone, targetAnims)) {
                    this.triggerReload();
                }
            }
        });

        this.leftContent = UI.column(5,
            UI.row(UI.label(IKey.raw("Models:")).marginTop(2)),
            this.searchModelsInput,
            this.modelsList,
            UI.row(UI.label(IKey.raw("Animations:")).marginTop(2), this.allAnimationsToggle),
            this.searchAnimationsInput,
            this.animationsList,
            UI.row(UI.label(IKey.raw("Target Bone:")).marginTop(2)),
            this.targetBoneInput,
            UI.row(this.injectXPosToggle, this.injectXNegToggle),
            UI.row(this.injectYPosToggle, this.injectYNegToggle),
            UI.row(this.injectZPosToggle, this.injectZNegToggle),
            UI.row(UI.label(IKey.raw("Math Modifier:")).marginTop(2)),
            this.mathModifierInput,
            this.injectQueryButton,
            this.removeQueriesButton,
            this.restoreBackupButton
        );

        this.leftContent.relative(this.leftPanel).x(10).y(10).w(1F, -20).h(0);
        this.leftPanel.add(this.leftContent);
    }

    private void setupRightPanel()
    {
        // Top Tools
        this.openInButton = new UIButton(IKey.raw("Open folder"), (b) -> {
            File f = this.getModelFile();
            if (f != null && f.exists()) {
                mchorse.bbs_mod.ui.utils.UIUtils.openFolder(f.getParentFile());
            }
        });

        this.pickTextureButton = new UIButton(IKey.raw("Pick a texture..."), (b) -> {
            mchorse.bbs_mod.resources.Link link = this.form.texture.get();
            mchorse.bbs_mod.ui.framework.elements.input.UITexturePicker.open(this.getContext(), link, (l) -> {
                this.form.texture.set(l);
                this.triggerReload();
            });
        });

        // Body Parts List
        this.searchBodyPartsInput = new UITextbox(1000, (str) -> {
            this.updateBodyPartsList(str);
        });
        this.searchBodyPartsInput.tooltip(IKey.raw("Search Body Parts..."));

        this.bodyPartsList = new UIStringList((l) -> {
            if (l.size() > 0) {
                String boneName = l.get(0);
                this.targetBoneInput.setText(boneName);
                this.updateTransformPanel();
            }
        });
        this.bodyPartsList.background().h(180);

        // Transform Panel
        this.transformPanel = new mchorse.bbs_mod.ui.framework.elements.input.UIPropTransform();
        this.transformPanel.setTransform(this.liveTransform);
        this.transformPanel.callbacks(null, () -> {
            mchorse.bbs_mod.cubic.ModelInstance inst = this.getModelInstance();
            if (inst == null) return;
            String bone = this.targetBoneInput.getText();
            if (bone == null || bone.isEmpty()) return;
            mchorse.bbs_mod.utils.pose.PoseTransform pt = this.form.pose.get().get(bone);
            pt.translate.set(this.liveTransform.translate);
            pt.scale.set(this.liveTransform.scale);
            pt.rotate.set(this.liveTransform.rotate);
            pt.rotate2.set(this.liveTransform.rotate2);
        });

        this.rightContent = UI.column(5,
            this.openInButton,
            this.pickTextureButton,
            UI.row(UI.label(IKey.raw("Body Parts:")).marginTop(2)),
            this.searchBodyPartsInput,
            this.bodyPartsList,
            UI.row(UI.label(IKey.raw("Transform:")).marginTop(2)),
            this.transformPanel
        );

        this.rightContent.relative(this.rightPanel).x(10).y(10).w(1F, -20).h(0);
        this.rightPanel.add(this.rightContent);
    }

    @Override
    public void resize()
    {
        super.resize();
        if (this.leftContent != null)
        {
            this.leftContent.resize();
            this.leftPanel.scroll.scrollSize = this.leftContent.area.h + 20;
            this.leftPanel.scroll.clamp();
        }
        if (this.rightContent != null)
        {
            this.rightContent.resize();
            this.rightPanel.scroll.scrollSize = this.rightContent.area.h + 20;
            this.rightPanel.scroll.clamp();
        }
    }
}
