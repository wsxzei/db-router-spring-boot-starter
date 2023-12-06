package cn.wzz.middleware.db.router.aop;

import cn.wzz.middleware.db.router.DBRouterConfig;
import cn.wzz.middleware.db.router.RouterStrategyEnum;
import cn.wzz.middleware.db.router.annotation.DBRouter;
import cn.wzz.middleware.db.router.annotation.RouterKey;
import cn.wzz.middleware.db.router.annotation.RouterStrategy;
import cn.wzz.middleware.db.router.strategy.IDBRouterStrategy;
import cn.wzz.middleware.db.router.strategy.algorithm.impl.DBRouterStrategyAlgorithm;
import cn.wzz.middleware.db.router.util.StringUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.transaction.support.TransactionTemplate;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.Map;

@Aspect
public class DBRouterJoinPoint {

    private DBRouterConfig dbRouterConfig;

    private Map<Enum<RouterStrategyEnum>, IDBRouterStrategy> routerStrategyMap;

    private TransactionTemplate transactionTemplate;


    public DBRouterJoinPoint(DBRouterConfig dbRouterConfig, Map<Enum<RouterStrategyEnum>, IDBRouterStrategy> routerStrategyMap, TransactionTemplate transactionTemplate) {
        this.dbRouterConfig = dbRouterConfig;
        this.routerStrategyMap = routerStrategyMap;
        this.transactionTemplate = transactionTemplate;
    }

    @Pointcut("@annotation(cn.wzz.middleware.db.router.annotation.DBRouter)")
    public void pointcut() {
    }

    /**
     * 针对注解 DBRouter 标记的方法，定义 AOP 环绕增强逻辑
     * @param joinPoint
     * @param dbRouter
     * @return
     */
    @Around("@annotation(dbRouter)")
    public Object dbRouter(ProceedingJoinPoint joinPoint, DBRouter dbRouter) throws Throwable {
        // 1. 确定根据哪个字段进行路由
        String fieldName = dbRouter.key();
        // 2. 获取路由字段的值
        Object[] args = joinPoint.getArgs();
        Object fieldValue = null;

        if(!StringUtils.isEmpty(fieldName)) {
            for(Object arg: args) {
                fieldValue = getFieldValue(arg, fieldName);
                if (fieldValue != null){
                    break;
                }
            }
        }

        if (fieldValue == null){
            // 获取方法入参的注解, 解析是否存在@RouterKey标记的入参, 获取入参值作为路由键
            MethodSignature signature = (MethodSignature)joinPoint.getSignature();
            Annotation[][] parameterAnnotations = signature.getMethod().getParameterAnnotations();
            for(int i = 0; i < args.length; i++) {
                if (fieldValue != null) break;
                Annotation[] annotations = parameterAnnotations[i];
                for (Annotation annotation: annotations) {
                    if(annotation instanceof RouterKey) {
                        fieldValue = args[i];
                        break;
                    }
                }
            }
            if (fieldValue == null){
                throw new RuntimeException(String.format("sharding field %s can't be found in args", fieldName));
            }
        }

        // 3. 根据 fieldValue 和路由策略, 计算得到 dbIdx 和 tbIdx
        boolean customizeSharding = false;
        String algorithmClass = "";
        RouterStrategy routerStrategy = getRouterStrategy(joinPoint);
        if(routerStrategy != null && routerStrategy.strategy() == RouterStrategyEnum.SHARDING_ALGORITHM) {
            customizeSharding = true;
            algorithmClass = routerStrategy.algorithmClass();
        }

        IDBRouterStrategy dbRouterStrategy = null;
        if(customizeSharding && !StringUtils.isEmpty(algorithmClass)) {
            dbRouterStrategy =  routerStrategyMap.get(RouterStrategyEnum.SHARDING_ALGORITHM);
            ((DBRouterStrategyAlgorithm) dbRouterStrategy).setAlgorithmClassName(algorithmClass);
        } else {
            // 扰动哈希计算路由键
            dbRouterStrategy = routerStrategyMap.get(RouterStrategyEnum.HASH);
        }
        dbRouterStrategy.dbRouter(fieldValue);

        try {
            return joinPoint.proceed();
        }  finally {
            // 释放 ThreadLocalMap 对象
            dbRouterStrategy.clear();
        }
    }

    private Object getFieldValue(Object item, String fieldName) {
        boolean find = false;
        Class<?> clazz = item.getClass();
        Field field = null;
        while (!find && clazz != null){
            try {
                field = clazz.getDeclaredField(fieldName);
                find = true;
            } catch (NoSuchFieldException e) {
                clazz = clazz.getSuperclass();
            }
        }

        if (find) {
            field.setAccessible(true);
            Object res = null;
            try {
                res = field.get(item);
            } catch (IllegalAccessException e) {
                return null;
            }
            field.setAccessible(false);
            return res;
        }
        return null;
    }


    private RouterStrategy getRouterStrategy(ProceedingJoinPoint joinPoint) {
        // getThis 获取代理对象, getTarget 获取被代理的对象
        Class<?> proxy = joinPoint.getThis().getClass();
        Class<?>[] proxyInterfaces = proxy.getInterfaces();

        for(Class<?> inf: proxyInterfaces) {
            RouterStrategy routerStrategy = inf.getAnnotation(RouterStrategy.class);
            if(routerStrategy != null) {
                return routerStrategy;
            }
        }
        return null;
    }

}
