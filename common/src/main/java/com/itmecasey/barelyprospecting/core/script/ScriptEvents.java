package com.itmecasey.barelyprospecting.core.script;

import com.itmecasey.barelyprospecting.core.Drop;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
import java.util.logging.Logger;

/**
 * Events that scripts can listen to. A listener that throws is logged as one line and skipped; it never reaches the game.
 *
 * <p>Names: {@code panned}, {@code hammered}, {@code soakStarted}, {@code soakFinished}, {@code crucibleFed},
 * {@code crucibleQuenched}, {@code lureFed}, {@code lureHatched}.
 */
public final class ScriptEvents {
    public static final Set<String> NAMES = Set.of("panned", "hammered", "soakStarted", "soakFinished", "crucibleFed",
            "crucibleQuenched", "lureFed", "lureHatched");

    private static final Logger LOG = Logger.getLogger("barelyprospecting");
    private static final Map<String, List<Consumer<Event>>> LISTENERS = new ConcurrentHashMap<>();

    /**
     * What a listener receives. Not every field is set for every event: {@code player} is empty for events nobody caused (a soak
     * finishing, a lure hatching), {@code subject} is the block or item the event is about, {@code result} what it became.
     */
    public static final class Event {
        private final String name;
        private final String player;
        private final String dimension;
        private final int x;
        private final int y;
        private final int z;
        private final String subject;
        private final String result;
        private final int count;
        private final String detail;
        private final List<Drop> drops;

        Event(String name, String player, String dimension, int x, int y, int z, String subject, String result, int count,
              String detail, List<Drop> drops) {
            this.name = name;
            this.player = player == null ? "" : player;
            this.dimension = dimension == null ? "" : dimension;
            this.x = x;
            this.y = y;
            this.z = z;
            this.subject = subject == null ? "" : subject;
            this.result = result == null ? "" : result;
            this.count = count;
            this.detail = detail == null ? "" : detail;
            this.drops = drops == null ? List.of() : List.copyOf(drops);
        }

        public String name() {
            return name;
        }

        /** Name of the player who did it, or an empty string. */
        public String player() {
            return player;
        }

        public String dimension() {
            return dimension;
        }

        public int x() {
            return x;
        }

        public int y() {
            return y;
        }

        public int z() {
            return z;
        }

        public String subject() {
            return subject;
        }

        public String result() {
            return result;
        }

        public int count() {
            return count;
        }

        /** The fluid of a soak (water, lava), the kind of crucible feed (melt, fuel) or the lure table. */
        public String detail() {
            return detail;
        }

        /** What a pan or a hatch produced. Each entry has item() and count(). */
        public List<Drop> drops() {
            return drops;
        }
    }

    private ScriptEvents() {
    }

    /** Registers a listener. Unknown names are an error in the script, so a typo is noticed. */
    public static void on(String name, Consumer<Event> listener) {
        if (!NAMES.contains(name)) {
            throw new IllegalArgumentException("BarelyProspecting: unknown event " + name + ", use one of " + new java.util.TreeSet<>(NAMES));
        }
        LISTENERS.computeIfAbsent(name, k -> new CopyOnWriteArrayList<>()).add(listener);
    }

    public static boolean hasListeners(String name) {
        List<Consumer<Event>> list = LISTENERS.get(name);
        return list != null && !list.isEmpty();
    }

    /** Called by the game code. Cheap when nobody listens. */
    public static void fire(String name, String player, String dimension, int x, int y, int z, String subject, String result, int count,
                            String detail, List<Drop> drops) {
        List<Consumer<Event>> list = LISTENERS.get(name);
        if (list == null || list.isEmpty()) {
            return;
        }
        Event e = new Event(name, player, dimension, x, y, z, subject, result, count, detail, drops);
        for (Consumer<Event> l : list) {
            try {
                l.accept(e);
            } catch (RuntimeException | LinkageError ex) {
                LOG.warning("Barely Prospecting: a script listener for " + name + " failed: " + ex);
            }
        }
    }

    public static void clear() {
        LISTENERS.clear();
    }
}
