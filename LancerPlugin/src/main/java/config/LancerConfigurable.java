package config;

import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.options.SearchableConfigurable;
import gui.SettingsGUI;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

public class LancerConfigurable implements SearchableConfigurable {
    private SettingConfig config;
    private SettingsGUI gui;


    public LancerConfigurable() {
        this.config = SettingConfig.getInstance();
    }

    @NotNull
    @Override
    public String getId() {
        return null;
    }

    @Nls(capitalization = Nls.Capitalization.Title)
    @Override
    public String getDisplayName() {
        return null;
    }

    @Nullable
    @Override
    public JComponent createComponent() {
        gui = new SettingsGUI(config);
        return gui.getRootPanel();
    }

    @Override
    public boolean isModified() {
        return gui.isModified();
    }


    @Override
    public void apply() throws ConfigurationException {
        gui.apply();
    }
}
