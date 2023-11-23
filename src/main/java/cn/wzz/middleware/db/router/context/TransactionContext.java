package cn.wzz.middleware.db.router.context;

public class TransactionContext {
    private static final ThreadLocal<Boolean> TX_SWITCH = new ThreadLocal<>();

    public static void openTx() {
        TX_SWITCH.set(true);
    }

    public static void closeTx() {
        TX_SWITCH.remove();
    }

    public static Boolean txIsOpen() {
        return TX_SWITCH.get() != null && TX_SWITCH.get();
    }
}
