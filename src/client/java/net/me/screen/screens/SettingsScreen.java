package net.me.screen.screens;

import net.me.config.GlobalConfigManager;
import net.me.screen.MQSScreen;
import net.me.screen.component.WidgetLayoutHelper;
import net.me.screen.component.components.BooleanSettingEntryWidget;
import net.me.utils.UIConstants;
import net.minecraft.client.gui.DrawContext;

import java.util.ArrayList;
import java.util.List;

public class SettingsScreen extends MQSScreen {

    private static final int WIDGET_WIDTH = 250;

    private final GlobalConfigManager globalConfigManager;
    private final List<BooleanSettingEntryWidget> settingWidgets = new ArrayList<>();

    public SettingsScreen(MQSScreen parent, GlobalConfigManager globalConfigManager) {
        super("Settings", 300, 280, parent);
        this.globalConfigManager = globalConfigManager;
    }

    @Override
    protected void init() {
        super.init();
        this.settingWidgets.clear();

        BooleanSettingEntryWidget logRedirectWidget = BooleanSettingEntryWidget.builder()
                .name("Log Redirection")
                .description("Redirects logs to the MQS console.")
                .getter(globalConfigManager::isLogRedirectEnabled)
                .setter(globalConfigManager::setLogRedirectEnabled)
                .size(WIDGET_WIDTH, UIConstants.ENTRY_HEIGHT)
                .build();

        this.settingWidgets.add(logRedirectWidget);
        this.addDrawableChild(logRedirectWidget);

        BooleanSettingEntryWidget allowAllClassesWidget = BooleanSettingEntryWidget.builder()
                .name("Allow All Classes")
                .description("Allows scripts to access any Java class.")
                .getter(globalConfigManager::areAllClassesAllowed)
                .setter(globalConfigManager::setAllClassesAllowed)
                .size(WIDGET_WIDTH, UIConstants.ENTRY_HEIGHT)
                .build();

        this.settingWidgets.add(allowAllClassesWidget);
        this.addDrawableChild(allowAllClassesWidget);

        BooleanSettingEntryWidget enableDisableToastWidget = BooleanSettingEntryWidget.builder()
                .name("Enable/Disable Toasts")
                .description("Show a toast on script state change.")
                .getter(globalConfigManager::isEnableDisableToastEnabled)
                .setter(globalConfigManager::setEnableDisableToastEnabled)
                .size(WIDGET_WIDTH, UIConstants.ENTRY_HEIGHT)
                .build();

        this.settingWidgets.add(enableDisableToastWidget);
        this.addDrawableChild(enableDisableToastWidget);

        WidgetLayoutHelper.layoutVertically(
                getMiddlePoint().x() - WIDGET_WIDTH / 2,
                getMiddlePoint().y() - 80,
                UIConstants.WIDGET_SPACING,
                this.settingWidgets.toArray(new BooleanSettingEntryWidget[0])
        );
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
    }
}