package mage.player.ai.encoder;

import mage.abilities.Ability;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.lang.Math.abs;

/**
 * Maps priority actions and targets to policy-head indices.
 *
 * Upstream behaviour (no vocabulary configured): a few reserved names, everything else hashed
 * into 1..127 of a 128-wide head. Distinct cards collide there, and a whole set cannot fit.
 *
 * Set vocabulary (MageZero-Experiments patch): when the system property {@code mz.actionVocab}
 * or env var {@code MZ_ACTION_VOCAB} points at a vocabulary file, every action/target string in
 * it gets its own index, the head width becomes {@link #ACTION_DIM} (from the file), and strings
 * not in the file are hashed into the reserved tail [vocabSize, ACTION_DIM). Both players share
 * the same indices. File format (UTF-8, tab separated, "\n" escaped as backslash-n):
 *   dim<TAB>1024
 *   A<TAB><index><TAB><ability.toString()>
 *   T<TAB><index><TAB><entity name>
 * See experiments/action_vocab/README.md.
 */
public class ActionEncoder {
    public enum ActionType {
        PRIORITY, CHOOSE_NUM, BLANK, CHOOSE_TARGET, MAKE_CHOICE, CHOOSE_USE
    }

    /** Width of the priority / opponent / target policy vectors. 128 unless a vocabulary is loaded. */
    public static final int ACTION_DIM;
    public static final String VOCAB_PATH;
    private static final Map<String, Integer> VOCAB_ACTIONS = new HashMap<>();
    private static final Map<String, Integer> VOCAB_TARGETS = new HashMap<>();
    private static final int ACTION_HASH_START;
    private static final int TARGET_HASH_START;

    static {
        String path = System.getProperty("mz.actionVocab");
        if (path == null || path.isEmpty()) {
            path = System.getenv("MZ_ACTION_VOCAB");
        }
        int dim = 128;
        int aMax = -1, tMax = -1;
        if (path != null && !path.isEmpty()) {
            try (BufferedReader r = Files.newBufferedReader(Paths.get(path), StandardCharsets.UTF_8)) {
                String line;
                while ((line = r.readLine()) != null) {
                    if (line.isEmpty() || line.startsWith("#")) continue;
                    String[] parts = line.split("\t", 3);
                    if (parts[0].equals("dim")) {
                        dim = Integer.parseInt(parts[1].trim());
                    } else if (parts.length == 3) {
                        int idx = Integer.parseInt(parts[1]);
                        String key = parts[2].replace("\\n", "\n");
                        if (parts[0].equals("A")) {
                            VOCAB_ACTIONS.put(key, idx);
                            aMax = Math.max(aMax, idx);
                        } else if (parts[0].equals("T")) {
                            VOCAB_TARGETS.put(key, idx);
                            tMax = Math.max(tMax, idx);
                        }
                    }
                }
            } catch (IOException e) {
                throw new RuntimeException("could not read action vocabulary " + path, e);
            }
            if (aMax >= dim - 1 || tMax >= dim - 1) {
                throw new IllegalStateException("action vocabulary " + path + " does not fit dim " + dim
                        + " with a hash tail (max action idx " + aMax + ", max target idx " + tMax + ")");
            }
        } else {
            path = null;
        }
        ACTION_DIM = dim;
        VOCAB_PATH = path;
        ACTION_HASH_START = aMax + 1;
        TARGET_HASH_START = tMax + 1;
    }

    public static boolean vocabLoaded() {
        return VOCAB_PATH != null;
    }

    public  Map<String, Integer> playerActionMap = new HashMap<>();
    public  Map<String, Integer> opponentActionMap = new HashMap<>();
    public  Map<String, Integer> targetMap = new HashMap<>();

    public ActionEncoder() {
        if (vocabLoaded()) {
            playerActionMap.putAll(VOCAB_ACTIONS);
            opponentActionMap.putAll(VOCAB_ACTIONS);
            targetMap.putAll(VOCAB_TARGETS);
            return;
        }
        List<Map<String, Integer>> maps = new ArrayList<>(); maps.add(playerActionMap); maps.add(opponentActionMap);
        for(Map<String, Integer> map : maps) {
            map.put("Pass", 0);
            map.put("{T}: Add {B}.", 1);
            map.put("{T}: Add {G}.", 2);
            map.put("{T}: Add {R}.", 3);
            map.put("{T}: Add {U}.", 4);
            map.put("{T}: Add {W}.", 5);
            map.put("{T}: Add {C}.", 6);
        }

        targetMap.put("Stop Choosing", 0);
        targetMap.put("PlayerA", 1);
        targetMap.put("PlayerB", 2);

    }

    public int getActionIndex(Ability sa, boolean isPlayer) {
        String name = sa.toString();
        Map<String, Integer> map;
        if(isPlayer) {
            map = playerActionMap;
        } else {
            map = opponentActionMap;
        }
        if(map.containsKey(name)) {
            return map.get(name);
        } else if (vocabLoaded()) {
            return ACTION_HASH_START + Math.floorMod(name.hashCode(), ACTION_DIM - ACTION_HASH_START);
        } else {
            return (abs(name.hashCode()) % 127)+1; //pass is fully reserved
        }
    }
    public int getTargetIndex(String name) {
        if(targetMap.containsKey(name)) {
            return targetMap.get(name);
        } else if (vocabLoaded()) {
            return TARGET_HASH_START + Math.floorMod(name.hashCode(), ACTION_DIM - TARGET_HASH_START);
        } else {
            return (abs(name.hashCode()) % 127)+1; //stop choosing fully reserved
        }
    }
}
