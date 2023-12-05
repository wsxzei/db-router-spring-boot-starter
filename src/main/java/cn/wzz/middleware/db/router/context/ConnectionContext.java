package cn.wzz.middleware.db.router.context;

import cn.wzz.middleware.db.router.dynamic.CustomConnection;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ConnectionContext {
    private static final ThreadLocal<List<CustomConnection>> MULTI_TX_CONN = new ThreadLocal<>();

    // 连接缓存器, 在事务内相同数据源使用同一个连接对象, 保证事务的隔离性
    private static final ThreadLocal<Map<Object, Connection>> CONN_CACHE = new ThreadLocal<>();

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
        CONN_CACHE.get().clear();
        CONN_CACHE.remove();
    }

    public static Map<Object, Connection> getConnMap() {
        Map<Object, Connection> connMap = CONN_CACHE.get();
        if (connMap == null) {
            CONN_CACHE.set(new HashMap<Object, Connection>());
        }
        return CONN_CACHE.get();
    }
}