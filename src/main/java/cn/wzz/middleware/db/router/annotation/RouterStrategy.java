package cn.wzz.middleware.db.router.annotation;

import java.lang.annotation.*;

@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
public @interface RouterStrategy {
    // 是否需要进行分表, 默认需要分表
    // false: 只分库, 不分表; true: 即分库, 也分表
    boolean splitTable() default true;
}
