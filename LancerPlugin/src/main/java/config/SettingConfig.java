package config;

import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.project.Project;
import com.intellij.util.xmlb.XmlSerializerUtil;
import indexer.LuceneIndexer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;

@State(name = "SettingConfig", storages = {@Storage("LAMPSettings.xml")})
public class SettingConfig implements PersistentStateComponent<SettingConfig> {
    private static SettingConfig config;
    private String LOCAL_REP_DIR = "";
    private boolean ENABLE_REMORE_MODE = true;
    private boolean ENABLE_LOCAL_MODE = true;
    private boolean ENABLE_DEEP_SEMANTIC = false;
    private boolean AUTO_TRIGGER = true;
    private int MAX_SIZE = 10;
    private int TIME_OUT = 200;

    @Nullable
    @Override
    public SettingConfig getState() {
        return this;
    }

    @Override
    public void loadState(@NotNull SettingConfig state) {
        XmlSerializerUtil.copyBean(state, this);
    }


    @NotNull
    public static SettingConfig getInstance() {
        if (config == null) {
            config = ServiceManager.getService(SettingConfig.class);
        }
        return config;
    }

    public String getLOCAL_REP_DIR() {
        return LOCAL_REP_DIR;
    }

    public void changeLOCAL_REP_DIR(String LOCAL_REP_DIR) {
        this.setLOCAL_REP_DIR(LOCAL_REP_DIR);
        // FIXME: 2019/5/20 use Observation Mode
        LuceneIndexer.getIndexer().indexDir(new File(this.LOCAL_REP_DIR));
    }

    public void setLOCAL_REP_DIR(String LOCAL_REP_DIR) {
        this.LOCAL_REP_DIR = LOCAL_REP_DIR;
    }

    public boolean isENABLE_REMORE_MODE() {
        return ENABLE_REMORE_MODE;
    }

    public void setENABLE_REMORE_MODE(boolean ENABLE_REMORE_MODE) {
        this.ENABLE_REMORE_MODE = ENABLE_REMORE_MODE;
    }

    public boolean isENABLE_LOCAL_MODE() {
        return ENABLE_LOCAL_MODE;
    }

    public void setENABLE_LOCAL_MODE(boolean ENABLE_LOCAL_MODE) {
        this.ENABLE_LOCAL_MODE = ENABLE_LOCAL_MODE;
    }

    public boolean isENABLE_DEEP_SEMANTIC() {
        return ENABLE_DEEP_SEMANTIC;
    }

    public void setENABLE_DEEP_SEMANTIC(boolean ENABLE_DEEP_SEMANTIC) {
        this.ENABLE_DEEP_SEMANTIC = ENABLE_DEEP_SEMANTIC;
    }

    public int getMAX_SIZE() {
        return MAX_SIZE;
    }

    public void setMAX_SIZE(int MAX_SIZE) {
        this.MAX_SIZE = MAX_SIZE;
    }

    public int getTIME_OUT() {
        return TIME_OUT;
    }

    public void setTIME_OUT(int TIME_OUT) {
        this.TIME_OUT = TIME_OUT;
    }

    public boolean isAUTO_TRIGGER() {
        return AUTO_TRIGGER;
    }

    public void setAUTO_TRIGGER(boolean AUTO_TRIGGER) {
        this.AUTO_TRIGGER = AUTO_TRIGGER;
    }
}
