package cn.wzz.middleware.db.router.annotation;


import java.lang.annotation.*;

/**
 * 注解在方法上, 支持跨库事务
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD})
public @interface MultiDSTransaction {
}
