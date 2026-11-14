package com.itmecasey.barelyprospecting.core.script;

/** Installs the optional script bridges. Called once from each loader's mod constructor. */
public final class Bridges {
    /** The single API object shared by every bridge. */
    public static final ProspectApi API = new ProspectApi();

    private Bridges() {
    }

    public static void install() {
        ModJsBridge.install(API);
    }
}
