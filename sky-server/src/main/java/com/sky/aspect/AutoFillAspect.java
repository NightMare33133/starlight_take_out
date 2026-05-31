package com.sky.aspect;


import com.sky.annotation.AutoFill;
import com.sky.constant.AutoFillConstant;
import com.sky.context.BaseContext;
import com.sky.enumeration.OperationType;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.time.LocalDateTime;

/**
 * 自定义切面，实现公共字段自动填充处理逻辑
 */

@Aspect
@Component
@Slf4j
public class AutoFillAspect {

    /**
     * 切入点
     */
    //execution锁定了mapper下的类
    @Pointcut("execution(* com.sky.mapper.*.*(..)) && @annotation(com.sky.annotation.AutoFill)")
    public void autoFillPointCut() {}

    /**
     * 前置通知：在通知中进行公共字段的赋值
     */
    @Before("autoFillPointCut()")
    public  void autoFill(JoinPoint joinPoint){
        log.info("公共字段自动填充处理...");

        //1.获取到当前被拦截方法上的数据库类型
        //方法签名对象
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        //获取到方法上的注解对象
        AutoFill autoFill = signature.getMethod().getAnnotation(AutoFill.class);
        //获取数据库操作类型
        OperationType operationType = autoFill.value();

        //2.获取到方法的参数-->那就需要拿到实体对象
        Object[] args = joinPoint.getArgs();//获得所有参数
        if(args == null || args.length == 0){
            return;//没有参数，直接返回
        }
        //因为实体对象是不确定的所以是Object类型的，假设第一个参数就是实体对象
         Object entity = args[0];//拿到第一个参数，第一个参数就是实体对象通常是约定俗成
        //3.为公共属性统一重新赋值
        LocalDateTime now = LocalDateTime.now();
        //BaseContext是一个工具类，提供了一个静态方法getCurrentId()，
        // 用于获取当前用户的ID，这个ID通常是在用户登录后存储在上下文中的，
        // 可以通过这个方法来获取当前用户的ID，以便在自动填充公共字段时使用
        Long currentId = BaseContext.getCurrentId();

        //4.根据不同的操作类型，为对应的属性通过反射来赋值
        if(operationType == OperationType.INSERT){
            //为4个公共字段赋值
            try {

//                Method setCreateTime =  entity.getClass().getDeclaredMethod("setCreateTime",LocalDateTime.class);
//                Method setCreateUser = entity.getClass().getDeclaredMethod("setCreateUser", Long.class);
//                Method setUpdateTime =  entity.getClass().getDeclaredMethod("setUpdateTime",LocalDateTime.class);
//                Method setUpdateUser = entity.getClass().getDeclaredMethod("setUpdateUser", Long.class);
                //上面这些方法名其实用常数写死了，防止这里敲错，且更规范？
                Method setCreateTime =  entity.getClass().getDeclaredMethod(AutoFillConstant.SET_CREATE_TIME,LocalDateTime.class);
                Method setCreateUser = entity.getClass().getDeclaredMethod(AutoFillConstant.SET_CREATE_USER, Long.class);
                Method setUpdateTime =  entity.getClass().getDeclaredMethod(AutoFillConstant.SET_UPDATE_TIME,LocalDateTime.class);
                Method setUpdateUser = entity.getClass().getDeclaredMethod(AutoFillConstant.SET_UPDATE_USER, Long.class);


                //通过反射为对象属性赋值
                setCreateTime.invoke(entity,now);
                setCreateUser .invoke(entity,currentId);
                setUpdateTime.invoke(entity,now);
                setUpdateUser.invoke(entity,currentId);

            } catch (Exception e) {
                //printStackTrace()方法是Java中Throwable类的一个方法，
                // 用于打印异常的堆栈跟踪信息。它会将异常的类型、
                // 消息以及调用堆栈的信息输出到标准错误流（通常是控制台）。
                // 这对于调试和定位问题非常有帮助，因为它提供了异常发生的位置和调用路径的信息。
                e.printStackTrace();
            }
        }
        else if(operationType == OperationType.UPDATE){
            //为两个公共字段赋值
            try {
                Method setUpdateTime =  entity.getClass().getDeclaredMethod(AutoFillConstant.SET_UPDATE_TIME,LocalDateTime.class);
                Method setUpdateUser = entity.getClass().getDeclaredMethod(AutoFillConstant.SET_UPDATE_USER, Long.class);

                //通过反射为对象属性赋值
                setUpdateTime.invoke(entity,now);
                setUpdateUser.invoke(entity,currentId);

            } catch (Exception e) {
                //printStackTrace()方法是Java中Throwable类的一个方法，
                // 用于打印异常的堆栈跟踪信息。它会将异常的类型、
                // 消息以及调用堆栈的信息输出到标准错误流（通常是控制台）。
                // 这对于调试和定位问题非常有帮助，因为它提供了异常发生的位置和调用路径的信息。
                e.printStackTrace();
            }
        }


    }

}
