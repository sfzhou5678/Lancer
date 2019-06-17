package gui;

import config.SettingConfig;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.io.File;

public class SettingsGUI {
    private SettingConfig config;

    private JPanel rootPanel;

    private JTextField txtLocalRepDir;
    private JButton btnBrowseLocalRep;

    private JCheckBox ckbEnableOnline;
    private JCheckBox ckbEnableLocal;
    private JCheckBox ckbSemantic;

    private JComboBox cbxTimeOut;
    private JComboBox cbxMaxSize;
    private JCheckBox ckbAutoTrigger;

    public SettingsGUI(SettingConfig config) {
        this.config = config;
        txtLocalRepDir.setText(config.getLOCAL_REP_DIR());
        ckbEnableOnline.setSelected(config.isENABLE_REMORE_MODE());
        ckbEnableLocal.setSelected(config.isENABLE_LOCAL_MODE());
        ckbSemantic.setSelected(config.isENABLE_DEEP_SEMANTIC());

        btnBrowseLocalRep.addActionListener(e -> chooseDir(e));
    }

    public void chooseDir(ActionEvent e) {
        JFileChooser jfc = new JFileChooser();
        jfc.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
        jfc.showDialog(new JLabel(), "Choose");

        File file = jfc.getSelectedFile();
        String dirPath = file.getAbsolutePath();
        txtLocalRepDir.setText(dirPath);

    }

    public JComponent getRootPanel() {
        return rootPanel;
    }

    public boolean isModified() {
        boolean modified = false;
        modified |= !txtLocalRepDir.getText().equals(config.getLOCAL_REP_DIR());
        modified |= !ckbEnableOnline.isSelected() == config.isENABLE_REMORE_MODE();
        modified |= !ckbEnableLocal.isSelected() == config.isENABLE_LOCAL_MODE();
        modified |= !ckbSemantic.isSelected() == config.isENABLE_DEEP_SEMANTIC();
        modified |= !ckbAutoTrigger.isSelected() == config.isAUTO_TRIGGER();

//        modified |= !ckbSemantic.isSelected() == config.getMAX_SIZE();
//        modified |= !ckbSemantic.isSelected() == config.getTIME_OUT();
        return modified;
    }

    public void apply() {
        config.changeLOCAL_REP_DIR(txtLocalRepDir.getText());
        config.setENABLE_REMORE_MODE(ckbEnableOnline.isSelected());
        config.setENABLE_LOCAL_MODE(ckbEnableLocal.isSelected());
        config.setENABLE_DEEP_SEMANTIC(ckbSemantic.isSelected());
        config.setAUTO_TRIGGER(ckbAutoTrigger.isSelected());

//        config.setMAX_SIZE();
//        config.setTIME_OUT();

    }
}
