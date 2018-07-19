package personal.nfl.vpn;

import java.nio.channels.SelectionKey;

/**
 * @author nfl
 * {@link SelectionKey} 分发接口
 */

public interface KeyHandler {

    /**
     * 当 {@link SelectionKey} 准备好时
     *
     * @param key
     */
    void onKeyReady(SelectionKey key);
}
