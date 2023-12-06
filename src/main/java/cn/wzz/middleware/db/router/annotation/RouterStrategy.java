package cn.wzz.middleware.db.router.annotation;

import cn.wzz.middleware.db.router.RouterStrategyEnum;

import java.lang.annotation.*;

@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
public @interface RouterStrategy {
    // 是否需要进行分表, 默认需要分表
    // false: 只分库, 不分表; true: 即分库, 也分表
    boolean splitTable() default true;

    // 路由策略: 分片键执行Hash or 用户自定义算法类计算
    RouterStrategyEnum strategy() default RouterStrategyEnum.HASH;

    // 自定义分片算法类的全限定名
    String algorithmClass() default "";
}
