package com.sky.context;


//这个类的作用是作用：为每个请求的处理线程保存“当前用户/员工 id”
// （或其他跟请求相关的上下文值）
// ，这样在同一请求处理流程中的任意代码（Service、DAO、Utils）
// 都可以通过 BaseContext.getCurrentId() 访问当前登录用户 id，
// 而无需每个方法都传递该参数。
public class BaseContext {

    public static ThreadLocal<Long> threadLocal = new ThreadLocal<>();

    public static void setCurrentId(Long id) {
        threadLocal.set(id);
    }

    public static Long getCurrentId() {
        return threadLocal.get();
    }

    public static void removeCurrentId() {
        threadLocal.remove();
    }

}
