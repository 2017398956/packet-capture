package personal.nfl.vpn.utils;

import java.util.LinkedHashMap;

/**
 * @author fuli.niu
 */
public class MyLRUCache<K, V> extends LinkedHashMap<K, V> {

    private int maxSize;
    private transient CleanupCallback<V> callback;

    public MyLRUCache(int maxSize, CleanupCallback<V> callback) {
        super(maxSize + 1, 1, true);
        this.maxSize = maxSize;
        this.callback = callback;
    }


    @Override
    protected boolean removeEldestEntry(Entry<K, V> eldest) {
        if (size() > maxSize) {
            callback.cleanUp(eldest.getValue());
            return true;
        }
        return false;
    }

    public interface CleanupCallback<V> {
        /**
         * 清除对象
         *
         * @param v
         */
        void cleanUp(V v);
    }
}
