package cn.wzz.middleware.db.router.context;

public class DBContextHolder {

    private static Integer dbCount;

    private static Integer tbCount;

    private static final ThreadLocal<String> dbKey = new ThreadLocal<String>();
    private static final ThreadLocal<String> tbKey = new ThreadLocal<String>();

    public static void setDBKey(String dbKeyIdx){
        dbKey.set(dbKeyIdx);
    }

    public static String getDBKey(){
        return dbKey.get();
    }

    public static void setTBKey(String tbKeyIdx){
        tbKey.set(tbKeyIdx);
    }

    public static String getTBKey(){
        return tbKey.get();
    }

    public static void clearDBKey(){
        dbKey.remove();
    }

    public static void clearTBKey(){
        tbKey.remove();
    }


    public static Integer getDbCount() {
        return dbCount;
    }

    public static void setDbCount(Integer dbCount) {
        DBContextHolder.dbCount = dbCount;
    }

    public static Integer getTbCount() {
        return tbCount;
    }

    public static void setTbCount(Integer tbCount) {
        DBContextHolder.tbCount = tbCount;
    }
}
