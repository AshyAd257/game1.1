package com.Hecate.puppet.editor;
import javax.swing.*;
import java.awt.*;

/** Foreground modal forms for the editor's native GLFW window. Call on the EDT. */
final class EditorDialogs {
    private static JDialog active;
    private EditorDialogs(){}
    private static Object show(JOptionPane pane,String title){
        if(active!=null&&active.isShowing()){active.toFront();active.requestFocus();return null;}
        JDialog dialog=pane.createDialog(null,title);active=dialog;dialog.setAlwaysOnTop(true);dialog.setLocationRelativeTo(null);
        dialog.addWindowListener(new java.awt.event.WindowAdapter(){public void windowOpened(java.awt.event.WindowEvent e){dialog.toFront();dialog.requestFocus();}});
        try{dialog.setVisible(true);return pane.getValue();}finally{active=null;dialog.dispose();}
    }
    static int confirm(Object content,String title){Object result=show(new JOptionPane(content,JOptionPane.PLAIN_MESSAGE,JOptionPane.OK_CANCEL_OPTION),title);return result instanceof Integer?(Integer)result:JOptionPane.CLOSED_OPTION;}
    static int confirm(Object content,String title,String accept,String cancel){
        Object result=show(new JOptionPane(content,JOptionPane.PLAIN_MESSAGE,JOptionPane.OK_CANCEL_OPTION,null,new String[]{accept,cancel},accept),title);
        return accept.equals(result)?JOptionPane.OK_OPTION:cancel.equals(result)?JOptionPane.CANCEL_OPTION:JOptionPane.CLOSED_OPTION;
    }
    static String input(String title,String initial){JOptionPane pane=new JOptionPane(title,JOptionPane.QUESTION_MESSAGE,JOptionPane.OK_CANCEL_OPTION);pane.setWantsInput(true);pane.setInitialSelectionValue(initial);Object result=show(pane,title);Object input=pane.getInputValue();return Integer.valueOf(JOptionPane.OK_OPTION).equals(result)&&input!=JOptionPane.UNINITIALIZED_VALUE&&input!=null?input.toString():null;}
    static void message(String message){show(new JOptionPane(message,JOptionPane.INFORMATION_MESSAGE),"提示");}
}
