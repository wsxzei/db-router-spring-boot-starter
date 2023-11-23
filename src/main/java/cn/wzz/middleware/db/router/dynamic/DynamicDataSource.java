package cn.wzz.middleware.db.router.dynamic;

import cn.wzz.middleware.db.router.context.ConnectionContext;
import cn.wzz.middleware.db.router.context.DBContextHolder;
import cn.wzz.middleware.db.router.context.TransactionContext;
import cn.wzz.middleware.db.router.util.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * AbstractRoutingDataSource 用于在特定条件选择不同的数据源数据源的动态切换
 * 继承该类后, 实现 determineCurrentLookupKey 方法, 根据特定的条件(线程上下文、用户信息、环境变量等)
 * 动态确定使用数据源的 key。
 * Mybatis SpringBoot Starter 中 SqlSessionFactory sqlSessionFactory(DataSource ds) 会注入使用该数据源对象
 */
public class DynamicDataSource extends AbstractRoutingDataSource {

    @Value("${router.jdbc.datasource.default}")
    private String defaultDataSource;

    /**
     * 重写 getConnection:
     * 1. 父类 getConnection 获取的连接, 使用装饰器包装连接对象
     * 2. 若开启了多连接事务, 设置自动提交关闭, 并添加连接到集合中
     */
    @Override
    public Connection getConnection() throws SQLException {
        CustomConnection conn = new CustomConnection(super.getConnection());
        if (TransactionContext.txIsOpen()) {
            // 设置自动提交关闭, 并且将添加到 连接集合中
            conn.setAutoCommit(false);
            ConnectionContext.addConnection(conn);
        }
        return conn;
    }

    @Override
    public Connection getConnection(String username, String password) throws SQLException {
        CustomConnection conn = new CustomConnection(super.getConnection(username, password));
        if (TransactionContext.txIsOpen()) {
            conn.setAutoCommit(false);
            ConnectionContext.addConnection(conn);
        }
        return conn;
    }

    /*
    * AbstractRoutingDataSource#getConnection
    * 1. 通过 determineTargetDataSource 决定目标数据源, 会使用 determineCurrentLookupKey
    *   方法的返回值作为key, 获取数据源对象。
    * 2. determineCurrentLookupKey 从 ThreadLocal 中获取 dbKey, 与逻辑库名("db")拼接为目标数据库
    * */
    @Override
    protected Object determineCurrentLookupKey() {
        String dbKey = DBContextHolder.getDBKey();
        if (StringUtils.isEmpty(dbKey)) {
            return defaultDataSource;
        }
        return  "db" + dbKey;
    }
}
