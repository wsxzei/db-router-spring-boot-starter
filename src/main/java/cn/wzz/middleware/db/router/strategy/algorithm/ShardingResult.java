package cn.wzz.middleware.db.router.strategy.algorithm;

public class ShardingResult {

    public Integer dbIndex;

    public Integer tbIndex;

    public Integer getDbIndex() {
        return dbIndex;
    }

    public void setDbIndex(Integer dbIndex) {
        this.dbIndex = dbIndex;
    }

    public Integer getTbIndex() {
        return tbIndex;
    }

    public void setTbIndex(Integer tbIndex) {
        this.tbIndex = tbIndex;
    }
}
