package com.Hecate.puppet.newmode;

import com.Hecate.puppet.editor.PuppetEditorApp;

/** The editor's NEW mode is the cached pixel system, with the shared skeleton and timeline. */
public final class NewModePuppetEditorApp {
    private NewModePuppetEditorApp() {}
    public static PuppetEditorApp createEditor() { return PuppetEditorApp.createPixelEditor(); }
    public static void main(String[] args) { createEditor().start(); }
}
