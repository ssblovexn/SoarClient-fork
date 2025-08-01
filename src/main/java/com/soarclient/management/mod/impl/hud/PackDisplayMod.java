package com.soarclient.management.mod.impl.hud;

import com.soarclient.event.EventBus;
import com.soarclient.event.client.RenderSkiaEvent;
import com.soarclient.management.mod.api.hud.SimpleHUDMod;
import com.soarclient.management.mod.settings.impl.BooleanSetting;
import com.soarclient.management.mod.settings.impl.ComboSetting;
import com.soarclient.skia.Skia;
import com.soarclient.skia.font.Fonts;
import com.soarclient.skia.font.Icon;
import com.soarclient.utils.ColorUtils;
import com.soarclient.utils.TimerUtils;
import io.github.humbleui.skija.*;
import io.github.humbleui.skija.Font;
import io.github.humbleui.types.Rect;
import net.minecraft.client.MinecraftClient;
import net.minecraft.resource.InputSupplier;
import net.minecraft.resource.ResourcePack;
import net.minecraft.resource.ResourcePackManager;
import net.minecraft.resource.ResourcePackProfile;
import net.minecraft.resource.ResourceType;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Color;
import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.stream.Collectors;

public class PackDisplayMod extends SimpleHUDMod {
    private static final Logger LOGGER = LoggerFactory.getLogger(PackDisplayMod.class);
    private final ComboSetting typeSetting = new ComboSetting("setting.type", "setting.type.description", Icon.FORMAT_LIST_BULLETED, this, List.of("setting.simple", "setting.normal", "setting.cover"), "setting.simple");
    private final BooleanSetting backgroundSetting = new BooleanSetting("setting.background", "setting.background.description", Icon.IMAGE, this, true) {
        @Override
        public boolean isVisible() {
            return typeSetting.getOption().equals("setting.normal");
        }
    };

    private final MinecraftClient mc = MinecraftClient.getInstance();
    private final TimerUtils animationTimer = new TimerUtils();
    private float mx, my, dx, dy;

    private File cachedPackIcon = null;
    private String cachedPackId = "";

    public PackDisplayMod() {
        super("mod.PackDisplayMod.name", "mod.PackDisplayMod.description", Icon.TEXTURE);
        setEnabled(true);
        this.dx = 0.5f;
        this.dy = 0.5f;
    }

    public EventBus.EventListener<RenderSkiaEvent> onRenderSkia = event -> {
        if (!isEnabled() || mc.world == null) { return; }
        String type = typeSetting.getOption();
        if (type.equals("setting.simple")) {
            this.draw();
        } else {
            drawInfo();
        }
    };

    private void drawInfo() {
        ResourcePackManager resourcePackManager = mc.getResourcePackManager();
        List<ResourcePackProfile> enabledPacks = resourcePackManager.getEnabledProfiles().stream()
            .filter(p -> !p.isPinned())
            .collect(Collectors.toList());

        ResourcePackProfile profileToRender;
        if (enabledPacks.isEmpty()) {
            profileToRender = resourcePackManager.getProfile("vanilla");
            if (profileToRender == null) {
                this.draw();
                return;
            }
        } else {
            profileToRender = enabledPacks.getLast();
        }

        if (!profileToRender.getId().equals(cachedPackId)) {
            LOGGER.info("Resource pack changed, updating icon cache for: " + profileToRender.getId());
            try (ResourcePack resourcePack = profileToRender.createResourcePack()) {
                this.cachedPackIcon = extractPackIcon(resourcePack);
                this.cachedPackId = profileToRender.getId();
            } catch (Exception e) {
                LOGGER.error("Error while creating resource pack for caching: " + profileToRender.getId(), e);
                this.cachedPackIcon = null;
                this.cachedPackId = profileToRender.getId();
            }
        }

        // --- Start of modifications for dynamic width ---
        final float height = 45, padding = 4.5f, iconSize = height - (padding * 2), coverSize = 256;
        final Font font = Fonts.getRegular(11);

        String displayName = profileToRender.getDisplayName().getString();
        String cleanName = displayName.endsWith(".zip") ? displayName.substring(0, displayName.length() - 4) : displayName;

        Rect textBounds = Skia.getTextBounds(cleanName, font);
        final float width = iconSize + textBounds.getWidth() + padding * 3; // Calculation for dynamic width
        // --- End of modifications for dynamic width ---

        final boolean isCoverMode = typeSetting.getOption().equals("setting.cover");
        final Color textColor = isCoverMode ? Color.WHITE : getDesign().getTextColor();

        begin();
        try {
            File iconFile = this.cachedPackIcon;

            if (isCoverMode) {
                if (iconFile != null) {
                    if (animationTimer.delay(80)) { updatePosition(width, height, coverSize); animationTimer.reset(); }
                    Skia.save();
                    Skia.clip(getX(), getY(), width, height, getRadius());
                    drawBlurredImage(iconFile, getX() - mx, getY() - my, coverSize, coverSize, 20);
                    Skia.restore();
                } else {
                    this.drawBackground(getX(), getY(), width, height);
                }
            } else {
                if (backgroundSetting.isEnabled()) {
                    this.drawBackground(getX(), getY(), width, height);
                }
            }

            if (iconFile != null) {
                Skia.drawRoundedImage(iconFile, getX() + padding, getY() + padding, iconSize, iconSize, 6);
            } else {
                Skia.drawRoundedRect(getX() + padding, getY() + padding, iconSize, iconSize, 6, ColorUtils.applyAlpha(textColor, 0.2F));
            }

            String textToRender = cleanName; // No longer limiting text width
            final float textAreaX = getX() + iconSize + padding * 2;
            final float centerY = getY() + (height / 2f);

            Rect textRenderBounds = Skia.getTextBounds(textToRender, font);
            float newTextX = textAreaX + textRenderBounds.getWidth() / 2f;

            Skia.drawFullCenteredText(textToRender, newTextX, centerY, textColor, font);

        } catch (Exception e) {
            LOGGER.error("Error while rendering pack display info for pack: " + profileToRender.getId(), e);
        }
        finish();
        position.setSize(width, height);
    }

    private void updatePosition(float width, float height, float coverSize) {
        float maxOffsetX = coverSize - width;
        float maxOffsetY = coverSize - height;
        mx += dx; my += dy;
        if (mx < 0) { mx = 0; dx = -dx; }
        if (mx > maxOffsetX) { mx = maxOffsetX; dx = -dx; }
        if (my < 0) { my = 0; dy = -dy; }
        if (my > maxOffsetY) { my = maxOffsetY; dy = -dy; }
    }

    private File extractPackIcon(ResourcePack resourcePack) {
        try {
            InputSupplier<InputStream> inputSupplier = null;
            try {
                inputSupplier = resourcePack.openRoot("pack.png");
            } catch (Exception ignored) {}

            if (inputSupplier == null) {
                try {
                    inputSupplier = resourcePack.open(ResourceType.CLIENT_RESOURCES, Identifier.of("minecraft", "pack"));
                } catch (Exception ignored) {}
            }

            if (inputSupplier == null) {
                String[] paths = {
                    "pack.png",
                    "icon.png",
                    "assets/minecraft/textures/gui/pack.png",
                    "assets/minecraft/textures/misc/pack.png"
                };

                for (String path : paths) {
                    try {
                        inputSupplier = resourcePack.openRoot(path);
                        if (inputSupplier != null) break;
                    } catch (Exception ignored) {}
                }
            }

            if (inputSupplier != null) {
                try (InputStream inputStream = inputSupplier.get()) {
                    if (inputStream != null) {
                        String fileName = "pack-icon-" + resourcePack.getId().replaceAll("[^a-zA-Z0-9.-]", "_");
                        Path tempFile = Files.createTempFile(fileName, ".png");
                        Files.copy(inputStream, tempFile, StandardCopyOption.REPLACE_EXISTING);
                        File file = tempFile.toFile();
                        file.deleteOnExit();
                        return file;
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.error("Failed to extract pack icon for " + resourcePack.getId(), e);
        }
        return null;
    }

    private void drawBlurredImage(File file, float x, float y, float width, float height, float blurRadius) {
        if(file == null) return;
        Paint blurPaint = new Paint().setImageFilter(ImageFilter.makeBlur(blurRadius, blurRadius, FilterTileMode.REPEAT));
        if (Skia.getImageHelper().load(file)) {
            Image image = Skia.getImageHelper().get(file.getName());
            if (image != null) {
                Skia.getCanvas().drawImageRect(image, Rect.makeWH(image.getWidth(), image.getHeight()), Rect.makeXYWH(x, y, width, height), blurPaint, true);
            }
        }
    }

    @Override
    protected void draw() {
        float fontSize = 9;
        float iconSize = 10.5F;
        float padding = 5;
        boolean hasIcon = getIcon() != null;
        String text = getText();

        Rect textBounds = Skia.getTextBounds(text, Fonts.getRegular(fontSize));
        Rect iconBounds = hasIcon ? Skia.getTextBounds(getIcon(), Fonts.getIcon(iconSize)) : new Rect(0, 0, 0, 0);

        float width = textBounds.getWidth() + (padding * 2) + (hasIcon ? iconBounds.getWidth() + 4 : 0);
        float height = fontSize + (padding * 2) - 1.5F;

        this.begin();
        this.drawBackground(getX(), getY(), width, height);

        if (hasIcon) {
            Skia.drawFullCenteredText(getIcon(),
                getX() + padding + (iconBounds.getWidth() / 2),
                getY() + (height / 2),
                this.getDesign().getTextColor(),
                Fonts.getIcon(iconSize));
        }

        Skia.drawFullCenteredText(text,
            getX() + padding + (hasIcon ? iconBounds.getWidth() + 4 : 0) + (textBounds.getWidth() / 2),
            getY() + (height / 2),
            this.getDesign().getTextColor(),
            Fonts.getRegular(fontSize));

        this.finish();
        position.setSize(width, height);
    }

    @Override
    public String getText() {
        ResourcePackManager resourcePackManager = mc.getResourcePackManager();
        List<ResourcePackProfile> enabledPacks = resourcePackManager.getEnabledProfiles().stream()
            .filter(p -> !p.isPinned())
            .collect(Collectors.toList());
        if (enabledPacks.isEmpty()) {
            return "Default";
        } else {
            String displayName = enabledPacks.getLast().getDisplayName().getString();
            return displayName.endsWith(".zip") ? displayName.substring(0, displayName.length() - 4) : displayName;
        }
    }

    @Override
    public String getIcon() {
        return Icon.TEXTURE;
    }

    @Override public void onEnable() { super.onEnable(); EventBus.getInstance().register(onRenderSkia); }
    @Override public void onDisable() {
        super.onDisable();
        EventBus.getInstance().unregister(onRenderSkia);
        this.cachedPackIcon = null;
        this.cachedPackId = "";
    }
}
