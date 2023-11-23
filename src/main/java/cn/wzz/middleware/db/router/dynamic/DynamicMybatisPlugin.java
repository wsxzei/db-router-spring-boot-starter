package cn.wzz.middleware.db.router.dynamic;

import cn.wzz.middleware.db.router.annotation.RouterStrategy;
import cn.wzz.middleware.db.router.context.DBContextHolder;
import cn.wzz.middleware.db.router.util.StringUtils;
import org.apache.ibatis.executor.statement.RoutingStatementHandler;
import org.apache.ibatis.executor.statement.StatementHandler;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.plugin.Interceptor;
import org.apache.ibatis.plugin.Intercepts;
import org.apache.ibatis.plugin.Invocation;
import org.apache.ibatis.plugin.Signature;
import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.reflection.SystemMetaObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.sql.Connection;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Mybatis拦截器, 通过对 SQL 语句改写, 完成分表操作
 * StatementHandler#prepare方法使用 BoundSql + MappedStatement配置信息 在连接上创建 Statement 对象
 */
@Intercepts({@Signature(type = StatementHandler.class, method = "prepare", args = {Connection.class, Integer.class})})
public class DynamicMybatisPlugin implements Interceptor {
    // \s匹配空白字符; \w匹配‘[A-Za-z0-9_], 字母、数字、下划线
    private Pattern pattern = Pattern.compile("(from|into|update)[\\s]{1,}`?(\\w{1,})`?", Pattern.CASE_INSENSITIVE);

    private Logger logger = LoggerFactory.getLogger(DynamicMybatisPlugin.class);

    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        if (StringUtils.isEmpty(DBContextHolder.getTBKey())) {
            // 不需要执行分表操作
            return invocation.proceed();
        }

        RoutingStatementHandler statementHandler = (RoutingStatementHandler) invocation.getTarget();
        MetaObject handlerMeta = SystemMetaObject.forObject(statementHandler);

        // 先判断 DAO 接口操作的数据表是否需要进行路由
        MappedStatement ms = (MappedStatement) handlerMeta.getValue("delegate.mappedStatement");
        String id = ms.getId();  // <namespace>.<statement_id>
        String namespace = id.substring(0, id.lastIndexOf('.'));
        Class<?> clazz = Class.forName(namespace);
        RouterStrategy routerStrategy = clazz.getAnnotation(RouterStrategy.class);
        if (routerStrategy != null && !routerStrategy.splitTable()) {
            // 默认分库+分表, 只分库不分表需要显示指定
            logger.info("[DynamicMybatisPlugin.intercept] don't need to split table");
            // 不进行分表操作
            return invocation.proceed();
        }

        // 获取BoundSql对象, 进行正则匹配, 修改逻辑表名为实际表名, 随后反射修改 sql
        BoundSql boundSql = (BoundSql) handlerMeta.getValue("delegate.boundSql");
        String sql = boundSql.getSql();
        Matcher matcher = pattern.matcher(sql);

        boolean find = matcher.find();
        logger.info("[DynamicMybatisPlugin.intercept] original sql: {}, matcher.find()={}, tbKey: {}",
                sql, find, DBContextHolder.getTBKey());

        if (find) {
            String group1 = matcher.group(1);
            String tbName = matcher.group(2);
            tbName = tbName + "_" + DBContextHolder.getTBKey();
            sql = matcher.replaceAll(group1 + " " + tbName);

            logger.info("[DynamicMybatisPlugin.intercept] table name: {}, sql: {}", tbName, sql);

            // 反射修改
            Field field = boundSql.getClass().getDeclaredField("sql");
            field.setAccessible(true);
            field.set(boundSql, sql);
            field.setAccessible(false);
        }

        return invocation.proceed();
    }

    @Override
    public Object plugin(Object target) {
        // 仅对需要拦截的对象进行反射判断, 并生成动态代理对象
        if(target instanceof StatementHandler){
            return Interceptor.super.plugin(target);
        }
        return target;
    }

    @Override
    public void setProperties(Properties properties) {
        Interceptor.super.setProperties(properties);
    }
}
