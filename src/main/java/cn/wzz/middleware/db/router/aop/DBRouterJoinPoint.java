package cn.wzz.middleware.db.router.aop;

import cn.wzz.middleware.db.router.DBRouterConfig;
import cn.wzz.middleware.db.router.annotation.DBRouter;
import cn.wzz.middleware.db.router.annotation.RouterKey;
import cn.wzz.middleware.db.router.strategy.IDBRouterStrategy;
import cn.wzz.middleware.db.router.util.StringUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.transaction.support.TransactionTemplate;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;

@Aspect
public class DBRouterJoinPoint {

    private DBRouterConfig dbRouterConfig;

    private IDBRouterStrategy dbRouterStrategy;

    private TransactionTemplate transactionTemplate;


    public DBRouterJoinPoint(DBRouterConfig dbRouterConfig, IDBRouterStrategy dbRouterStrategy, TransactionTemplate transactionTemplate) {
        this.dbRouterConfig = dbRouterConfig;
        this.dbRouterStrategy = dbRouterStrategy;
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

        // 3. 通过路由字段计算 dbIdx 和 tbIdx
        String routingKey = String.valueOf(fieldValue);
        dbRouterStrategy.dbRouter(routingKey);

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


}
