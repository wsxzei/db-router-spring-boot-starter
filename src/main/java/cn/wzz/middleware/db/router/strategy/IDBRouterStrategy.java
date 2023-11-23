package cn.wzz.middleware.db.router.strategy;

public interface IDBRouterStrategy {
    void dbRouter(String routingKey);

    void clear();
}
