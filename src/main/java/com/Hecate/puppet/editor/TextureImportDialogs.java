package com.Hecate.puppet.editor;

import java.awt.*;
import java.io.File;
import java.util.function.Consumer;
import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;

/** Swing dialogs need explicit foreground handling above the native GLFW editor window. EDT only. */
final class TextureImportDialogs {
    private JDialog active;
    private File directory;
    void choose(String title,Consumer<String> selected){
        if(focusExisting())return;
        JFileChooser chooser=new JFileChooser(directory){
            @Override protected JDialog createDialog(Component parent){
                JDialog dialog=super.createDialog(parent);prepare(dialog);return dialog;
            }
        };
        chooser.setDialogTitle(title);chooser.setFileFilter(new FileNameExtensionFilter("PNG 图片","png"));chooser.setAcceptAllFileFilterUsed(false);chooser.setMultiSelectionEnabled(false);
        String path=null;
        try {if(chooser.showOpenDialog(null)==JFileChooser.APPROVE_OPTION&&chooser.getSelectedFile()!=null){path=chooser.getSelectedFile().getAbsolutePath();directory=chooser.getCurrentDirectory();}}
        finally {close();}
        if(path!=null)selected.accept(path);
    }
    void error(String message){
        // A message queued during a chooser must not become another hidden modal dialog.
        if(focusExisting()){active.addWindowListener(new java.awt.event.WindowAdapter(){@Override public void windowClosed(java.awt.event.WindowEvent e){SwingUtilities.invokeLater(()->error(message));}});return;}
        JOptionPane pane=new JOptionPane(message,JOptionPane.ERROR_MESSAGE);JDialog dialog=pane.createDialog(null,"贴图导入失败");prepare(dialog);
        try {dialog.setVisible(true);}finally{close();}
    }
    private boolean focusExisting(){if(active==null||!active.isDisplayable())return false;active.toFront();active.requestFocus();return true;}
    private void prepare(JDialog dialog){active=dialog;dialog.setAlwaysOnTop(true);dialog.setLocationRelativeTo(null);dialog.addWindowListener(new java.awt.event.WindowAdapter(){@Override public void windowOpened(java.awt.event.WindowEvent e){dialog.toFront();dialog.requestFocus();}});}
    private void close(){JDialog dialog=active;active=null;if(dialog!=null)dialog.dispose();}
}
