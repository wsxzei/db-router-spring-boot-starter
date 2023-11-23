package cn.wzz.middleware.db.router.strategy.impl;

import cn.wzz.middleware.db.router.context.DBContextHolder;
import cn.wzz.middleware.db.router.DBRouterConfig;
import cn.wzz.middleware.db.router.strategy.IDBRouterStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DBRouterStrategyHash implements IDBRouterStrategy {

    private Logger logger = LoggerFactory.getLogger(DBRouterStrategyHash.class);

    private DBRouterConfig dbRouterConfig;

    public DBRouterStrategyHash(DBRouterConfig dbRouterConfig) {
        this.dbRouterConfig = dbRouterConfig;
    }

    @Override
    public void dbRouter(String routingKey) {
        // tbCount 为分表的总数目, 需要保证 tbCount 为 2 的幂次
        int tbCount = dbRouterConfig.getDbCount() * dbRouterConfig.getTbCount();

        int idx = (routingKey.hashCode() ^ (routingKey.hashCode() >>> 16)) & (tbCount - 1);

        // 以 DbCount = 2, TbCount = 4 为例
        // db01          db02
        //  |--tb_000     |--tb_000
        //  |--tb_001     |--tb_001
        //  |--tb_002     |--tb_002
        //  |--tb_003     |--tb_003
        // 若 idx = 5, 则路由至 db_02 数据源中的 tb_001 表
        int dbIdx = idx / dbRouterConfig.getTbCount() + 1;
        int tbIdx = idx % dbRouterConfig.getTbCount();

        DBContextHolder.setDBKey(String.format("%02d", dbIdx));
        DBContextHolder.setTBKey(String.format("%03d", tbIdx));
        logger.info("数据库路由 dbIdx: {}, tbIdx: {}", dbIdx, tbIdx);
    }

    @Override
    public void clear() {
        DBContextHolder.clearDBKey();
        DBContextHolder.clearTBKey();
    }
}
