package com.sky.annotation;

import com.sky.enumeration.OperationType;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 自定义注解，用于标识某些方法需要增加功能字段进行自动填充处理
 */

@Target(ElementType.METHOD)//指定注解的使用范围，ElementType.METHOD表示该注解只能用于方法上
@Retention(RetentionPolicy.RUNTIME)//指定注解的保留策略，RetentionPolicy.RUNTIME表示该注解在运行时仍然可用，可以通过反射获取
public @interface AutoFill {

    //数据库操作类型：UPDATE.INSERT
    OperationType value(); //定义一个属性，用于指定操作类型，OperationType是一个枚举类型，表示不同的操作类型，如INSERT、UPDATE等
}
