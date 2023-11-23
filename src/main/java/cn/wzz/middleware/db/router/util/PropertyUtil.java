package cn.wzz.middleware.db.router.util;

import org.springframework.boot.context.properties.bind.BindResult;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.core.env.Environment;

import java.util.Map;

public class PropertyUtil {

    public static <T> T handle(final Environment env, final String prefix, final Class<T> targetClass) {
        Binder binder = Binder.get(env);
        BindResult<T> bindResult = binder.bind(prefix, targetClass);
        return bindResult.get();
    }

}
