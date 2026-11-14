package com.itmecasey.barelyprospecting.dev;

import com.itmecasey.barelyprospecting.core.Drop;
import com.itmecasey.barelyprospecting.core.script.ModJsBridge;
import com.itmecasey.barelyprospecting.core.script.ProspectApi;
import com.itmecasey.barelyprospecting.core.script.ScriptEvents;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;

/**
 * Development only. When ModJS is on the classpath, runs a real script against the BarelyProspecting global through ModJS's own
 * binding hook. Returns null when ModJS is absent, "" when everything worked, and an error text otherwise.
 */
public final class ModJsProbe {
    private ModJsProbe() {
    }

    public static String run(ProspectApi api) {
        Class<?> extra;
        try {
            extra = Class.forName("com.itmecasey.modjs.script.ExtraBindings");
        } catch (ClassNotFoundException e) {
            return null;
        }
        try {
            api.clearListeners();
            if (!ModJsBridge.install(api)) {
                return "the bridge did not install";
            }
            Field f = extra.getDeclaredField("PROVIDERS");
            f.setAccessible(true);
            List<?> providers = (List<?>) f.get(null);
            Object provider = providers.get(providers.size() - 1);
            Class<?> ctxClass = Class.forName("org.mozilla.javascript.Context");
            Object cx = ctxClass.getMethod("enter").invoke(null);
            try {
                Object scope = ctxClass.getMethod("initStandardObjects").invoke(cx);
                Class<?> typeClass = Class.forName("com.itmecasey.modjs.script.ScriptType");
                Object type = typeClass.getMethod("valueOf", String.class).invoke(null, "SERVER");
                Method install = null;
                for (Method m : provider.getClass().getMethods()) {
                    if (m.getName().equals("install") && m.getParameterCount() == 3) {
                        install = m;
                    }
                }
                install.invoke(provider, type, cx, scope);
                Method eval = null;
                for (Method m : ctxClass.getMethods()) {
                    if (m.getName().equals("evaluateString") && m.getParameterCount() == 5 && m.getParameterTypes()[1] == String.class) {
                        eval = m;
                    }
                }
                String js = "var got = 0; var who = '';"
                        + "BarelyProspecting.on('panned', function (e) { got = e.count(); who = e.player(); });"
                        + "BarelyProspecting.setDrop('silt', 'minecraft:emerald', 1, 1, 1, 0, 0);"
                        + "BarelyProspecting.removeDrop('silt', 'minecraft:emerald');"
                        + "BarelyProspecting.eventNames().size() + ':' + BarelyProspecting.blockAt('minecraft:overworld', 0, 0, 0).get('kind');";
                Object first = eval.invoke(cx, scope, js, "probe", 1, null);
                ScriptEvents.fire("panned", "Steve", "minecraft:overworld", 0, 0, 0, "x", "", 7, "", List.of(new Drop("minecraft:flint", 7)));
                Object got = eval.invoke(cx, scope, "got + ':' + who", "probe", 1, null);
                if (!String.valueOf(first).equals("8:none") || !String.valueOf(got).equals("7:Steve")) {
                    return "unexpected script results " + first + " / " + got;
                }
                return "";
            } finally {
                ctxClass.getMethod("exit").invoke(null);
                api.clearListeners();
                api.resetContent();
            }
        } catch (Throwable t) {
            return "probe failed: " + t;
        }
    }
}
