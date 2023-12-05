package cn.wzz.middleware.db.router.aop;

import cn.wzz.middleware.db.router.context.ConnectionContext;
import cn.wzz.middleware.db.router.context.TransactionContext;
import cn.wzz.middleware.db.router.dynamic.CustomConnection;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Aspect
public class MultiDSTransactionJoinPoint {

    private Logger logger = LoggerFactory.getLogger(MultiDSTransactionJoinPoint.class);

    @Pointcut("@annotation(cn.wzz.middleware.db.router.annotation.MultiDSTransaction)")
    public void pointcut(){}

    @Around("pointcut()")
    public Object multiTxAop(ProceedingJoinPoint jp) throws Throwable {
        // 打开事务开关
        TransactionContext.openTx();
        logger.info("开启跨连接事务!");
        try {
            // 执行业务
            Object result = jp.proceed();

            // 提交事务并释放连接并释放连接
            for(CustomConnection conn: ConnectionContext.getConnectionList()) {
                conn.commitMultiTx();
                conn.closeMultiTx();
            }
            logger.info("跨连接事务提交成功!");
            return result;
        } catch (Throwable e) {
            // 回滚事务
            for(CustomConnection conn: ConnectionContext.getConnectionList()) {
                conn.rollbackMultiTx();
                conn.closeMultiTx();
            }
            logger.info("跨连接事务回滚成功!");
            throw e;
        } finally {
            // 清除事务标记, 情况连接集合
            ConnectionContext.clear();
            TransactionContext.closeTx();
        }
    }
}
