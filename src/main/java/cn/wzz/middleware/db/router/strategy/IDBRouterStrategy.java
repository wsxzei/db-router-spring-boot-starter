package cn.wzz.middleware.db.router.strategy;

public interface IDBRouterStrategy {
    void dbRouter(Object routingKey);

    void clear();
}
