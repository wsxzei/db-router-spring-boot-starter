package cn.wzz.middleware.db.router.strategy.algorithm.impl;

import cn.wzz.middleware.db.router.context.DBContextHolder;
import cn.wzz.middleware.db.router.strategy.IDBRouterStrategy;
import cn.wzz.middleware.db.router.strategy.algorithm.IShardingAlgorithm;
import cn.wzz.middleware.db.router.strategy.algorithm.ShardingResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.lang.reflect.*;

@Component
public class DBRouterStrategyAlgorithm implements IDBRouterStrategy {

    private String algorithmClassName;

    private Logger logger = LoggerFactory.getLogger(DBRouterStrategyAlgorithm.class);

    @Override
    public void dbRouter(Object routingKey) {
        try {
            Class<?> algorithmClass = Class.forName(algorithmClassName);
            Class<?> shardingKeyClass = null;
            Type[] interfaces = algorithmClass.getGenericInterfaces();
            for(Type type : interfaces) {
                if(type instanceof ParameterizedType) {
                    ParameterizedType parameterizedType = (ParameterizedType) type;
                    if(parameterizedType.getRawType().equals(IShardingAlgorithm.class)){
                        shardingKeyClass = ((Class<?>) parameterizedType.getActualTypeArguments()[0]);
                        break;
                    }
                }
            }

            Class<?> routingKeyClass = routingKey.getClass();
            // routingKeyCLass 是 shardingKeyClass 的子类
            if(shardingKeyClass == null) {
                logger.error("分片算法类 {} 未实现 cn.wzz.middleware.db.router.strategy.algorithm.IShardingAlgorithm 接口", algorithmClassName);
                return;
            }else if (!shardingKeyClass.isAssignableFrom(routingKeyClass)) {
                logger.error("路由键类型 {} 无法转换为 {}", routingKeyClass, shardingKeyClass);
                return;
            }

            // 反射调用 doSharding
            Method doSharding = algorithmClass.getDeclaredMethod("doSharding", shardingKeyClass);
            doSharding.setAccessible(true);

            Constructor<?> constructor = algorithmClass.getDeclaredConstructor();
            constructor.setAccessible(true);

            Object target = constructor.newInstance();
            ShardingResult shardingResult = ((ShardingResult) doSharding.invoke(target, routingKey));

            constructor.setAccessible(false);
            doSharding.setAccessible(false);

            if(shardingResult.dbIndex != null) {
                DBContextHolder.setDBKey(String.format("%02d", shardingResult.dbIndex));
            }
            if(shardingResult.tbIndex != null) {
                DBContextHolder.setTBKey(String.format("%03d", shardingResult.tbIndex));
            }
            logger.info("数据库路由 dbIdx: {}, tbIdx: {}", shardingResult.dbIndex, shardingResult.tbIndex);

        } catch (ClassNotFoundException e) {
            logger.error("分片算法类 {} 未找到, 错误信息: {}", algorithmClassName, e.getMessage());
        } catch (Exception e) {
            logger.error("dbRouter failed, error message: {}", e.getMessage());
        }
    }

    @Override
    public void clear() {
        DBContextHolder.clearDBKey();
        DBContextHolder.clearTBKey();
    }

    public String getAlgorithmClassName() {
        return algorithmClassName;
    }

    public void setAlgorithmClassName(String algorithmClassName) {
        this.algorithmClassName = algorithmClassName;
    }
}
