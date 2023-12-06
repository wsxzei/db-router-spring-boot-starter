package cn.wzz.middleware.db.router.strategy.algorithm;

public interface IShardingAlgorithm<T> {

    ShardingResult doSharding(T value);
}
