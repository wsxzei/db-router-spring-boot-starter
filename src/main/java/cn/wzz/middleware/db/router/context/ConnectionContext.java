package cn.wzz.middleware.db.router.context;

import cn.wzz.middleware.db.router.dynamic.CustomConnection;

import java.util.ArrayList;
import java.util.List;

public class ConnectionContext {
    private static final ThreadLocal<List<CustomConnection>> MULTI_TX_CONN = new ThreadLocal<>();

    public static void addConnection(CustomConnection connection) {
        if(MULTI_TX_CONN.get() == null) {
            MULTI_TX_CONN.set(new ArrayList<>());
        }
        MULTI_TX_CONN.get().add(connection);
    }

    public static List<CustomConnection> getConnectionList() {
        return MULTI_TX_CONN.get();
    }

    public static void clear() {
        MULTI_TX_CONN.get().clear();
        MULTI_TX_CONN.remove();
    }

}