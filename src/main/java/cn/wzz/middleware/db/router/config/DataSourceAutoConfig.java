package cn.wzz.middleware.db.router.config;

import cn.wzz.middleware.db.router.DBRouterConfig;
import cn.wzz.middleware.db.router.aop.DBRouterJoinPoint;
import cn.wzz.middleware.db.router.aop.MultiDSTransactionJoinPoint;
import cn.wzz.middleware.db.router.dynamic.DynamicDataSource;
import cn.wzz.middleware.db.router.dynamic.DynamicMybatisPlugin;
import cn.wzz.middleware.db.router.strategy.IDBRouterStrategy;
import cn.wzz.middleware.db.router.strategy.impl.DBRouterStrategyHash;
import cn.wzz.middleware.db.router.util.PropertyUtil;
import cn.wzz.middleware.db.router.util.StringUtils;
import org.apache.ibatis.plugin.Interceptor;
import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.reflection.SystemMetaObject;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;

/**
 * 当应用需要以 jar 包的形式提供给其他应用时, 可以考虑把它们封装为一个 Spring Boot Starter.
 * 该 jar 包可以自动添加需要引用的依赖项, 也能对核心功能进行配置。
 * 自动配置的核心类是使用 @Configuration 标记的类, 自动配置类中可以定义相应的 Bean。
 * 然后, 在 Classpath 下的 META-INF/spring.factories 中,
 * 以 org.springframework.boot.autoconfigure.EnableAutoConfiguration 为Key，
 * 以对应的自动配置类为 value, 多个自动配置类之间以 ',' 隔开
 */
@Configuration
public class DataSourceAutoConfig implements EnvironmentAware {
    private int dbCount;

    private int tbCount;

    private Map<String, Map<String, Object>> dsConfigMap = new HashMap<>();

    // 在自动配置类实例化后, 初始化前, 感知 spring-boot 配置文件 application.yml
    @Override
    public void setEnvironment(Environment env) {
        String dataSourcePrefix = "router.jdbc.datasource";

        Map map = PropertyUtil.handle(env, dataSourcePrefix, Map.class);
        dbCount = (int) map.get("dbCount");
        tbCount = (int) map.get("tbCount");

        // 获取数据源名称列表 eg: db01,db02
        String dbListStr = (String) map.get("list");
        String[] dbList = dbListStr.split(",");
        for(String db: dbList) {
            Map<String, Object> configMap = (Map<String, Object>) map.get(db);
            dsConfigMap.put(db, configMap);
        }
    }

    @Bean
    public DBRouterConfig dbRouterConfig() {
        return new DBRouterConfig(dbCount, tbCount);
    }

    @Bean
    public DataSource dataSource() {
        DynamicDataSource dynamicDataSource = new DynamicDataSource();
        Map<Object, Object> dsMap = new HashMap<>();
        try {
            for(String dsName: dsConfigMap.keySet()){
                Map<String, Object> dsConfig = dsConfigMap.get(dsName);
                DataSource ds = createDataSource(dsConfig);
                dsMap.put(dsName, ds);
            }
            dynamicDataSource.setTargetDataSources(dsMap);
            return dynamicDataSource;
        } catch (ClassNotFoundException e) {
            throw new IllegalArgumentException(e + " [createDataSource] can not find datasource type-class-name");
        }
    }

    private DataSource createDataSource(Map<String, Object> dsConfig) throws ClassNotFoundException {
        DataSourceProperties dataSourceProperties = new DataSourceProperties();
        dataSourceProperties.setUrl(dsConfig.get("url").toString());
        dataSourceProperties.setUsername(dsConfig.get("username").toString());
        dataSourceProperties.setPassword(dsConfig.get("password").toString());

        // 驱动类型
        String driverClassName = dsConfig.get("driver-class-name") == null ?
                "com.mysql.cj.jdbc.Driver" : dsConfig.get("driver-class-name").toString();
        dataSourceProperties.setDriverClassName(driverClassName);

        // 通过DataSourceProperties构造指定类型的数据源
        String typeClassName = dsConfig.get("type-class-name") == null ?
                "com.zaxxer.hikari.HikariDataSource": dsConfig.get("type-class-name").toString();
        DataSource ds = dataSourceProperties.initializeDataSourceBuilder()
                .type((Class<DataSource>) Class.forName(typeClassName))
                .build();


        MetaObject dsMetaObj = SystemMetaObject.forObject(ds);
        // 配置数据库连接池
        // initialSize: 连接池的初始化连接数、minIdle: 最小空闲连接数、maxActive: 最大活跃连接数
        // maxWait: 当达到最大活跃连接数时, 应用获取连接的请求会进入等待队列, 当超出此时间后, 获取连接的请求将抛出异常
        if (dsConfig.containsKey("pool")) {
            Map<String, Object> poolConfig = (Map<String, Object>) dsConfig.get("pool");
            for(Map.Entry<String, Object> entry :poolConfig.entrySet()) {
                // 中划线转驼峰
                String fieldName = StringUtils.middleScoreToCamelCase(entry.getKey());
                if (dsMetaObj.hasSetter(fieldName)) {
                    dsMetaObj.setValue(fieldName, entry.getValue());
                }
            }
        }
        return ds;
    }

    // 执行事务时, 连接对象会被缓存到 ThreadLocal 类型的 Map 中, key 为 DataSource 对象
    // (DataSourceTransactionManager#doBegin-->TransactionSynchronizationManager#bindResource)
    // 执行事务完成后, 缓存的连接会被清除 (doCleanupAfterCompletion-->unbindResource)
    // 注: 一个事务不能跨库, 因为事务中执行的 Statement 是使用同一个连接
    @Bean
    public TransactionTemplate transactionTemplate(DataSource dataSource) {
        // 事务在 DataSource 获取的连接之上创建
        DataSourceTransactionManager dataSourceTransactionManager = new DataSourceTransactionManager();
        dataSourceTransactionManager.setDataSource(dataSource);

        TransactionTemplate transactionTemplate = new TransactionTemplate(dataSourceTransactionManager);
        // PROPAGATION_REQUIRED: 若已经存在事务, 则加入该事务; 否则, 创建新的事务
        // PROPAGATN_REQUIRES_NEW: 创建一个新事务, 两个事务之间没有依赖关系
        transactionTemplate.setPropagationBehaviorName("PROPAGATION_REQUIRED");
        return transactionTemplate;
    }

    @Bean
    public IDBRouterStrategy dbRouterStrategy(DBRouterConfig dbRouterConfig) {
        return new DBRouterStrategyHash(dbRouterConfig);
    }

    @Bean
    public DBRouterJoinPoint dbRouterJoinPoint(DBRouterConfig dbRouterConfig, IDBRouterStrategy dbRouterStrategy, TransactionTemplate transactionTemplate) {
        return new DBRouterJoinPoint(dbRouterConfig, dbRouterStrategy, transactionTemplate);
    }

    @Bean
    public MultiDSTransactionJoinPoint multiDSTransactionJoinPoint() {
        return new MultiDSTransactionJoinPoint();
    }

    @Bean
    public Interceptor interceptor() {
        return new DynamicMybatisPlugin();
    }
}
