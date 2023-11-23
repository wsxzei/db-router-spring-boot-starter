package cn.wzz.middleware.db.router.annotation;

import java.lang.annotation.*;

/**
 * 路由注解:
 * 被该注解标记的方法存在 AOP 切点, 执行这些方法前会计算出数据源和数据表的路由键
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
public @interface DBRouter {
    // 分库分表字段
    String key() default "";
}
