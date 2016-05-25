//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package SyntaxUtils;

import java.io.Serializable;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.WeakHashMap;

public abstract class MapFactory<K, V> implements Serializable {
    public MapFactory() {
    }

    public abstract Map<K, V> buildMap();

    public static class HashMapFactory<K, V> extends MapFactory<K, V> {
        private static final long serialVersionUID = 1L;

        public HashMapFactory() {
        }

        public Map<K, V> buildMap() {
            return new HashMap();
        }
    }

    public static class IdentityHashMapFactory<K, V> extends MapFactory<K, V> {
        private static final long serialVersionUID = 1L;

        public IdentityHashMapFactory() {
        }

        public Map<K, V> buildMap() {
            return new IdentityHashMap();
        }
    }

    public static class TreeMapFactory<K, V> extends MapFactory<K, V> {
        private static final long serialVersionUID = 1L;

        public TreeMapFactory() {
        }

        public Map<K, V> buildMap() {
            return new TreeMap();
        }
    }

    public static class WeakHashMapFactory<K, V> extends MapFactory<K, V> {
        private static final long serialVersionUID = 1L;

        public WeakHashMapFactory() {
        }

        public Map<K, V> buildMap() {
            return new WeakHashMap();
        }
    }
}
