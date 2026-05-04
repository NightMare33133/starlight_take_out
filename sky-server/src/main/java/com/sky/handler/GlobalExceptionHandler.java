package com.sky.handler;

import com.sky.constant.MessageConstant;
import com.sky.exception.BaseException;
import com.sky.result.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.sql.SQLIntegrityConstraintViolationException;

/**
 * 全局异常处理器，处理项目中抛出的业务异常
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    /**
     * 捕获业务异常
     * @param ex
     * @return
     */
    @ExceptionHandler
    public Result exceptionHandler(BaseException ex){
        log.error("异常信息：{}", ex.getMessage());
        return Result.error(ex.getMessage());
    }

    @ExceptionHandler//@ExceptionHandler注解表示这个方法是一个异常处理方法，
    // 参数是要捕获的异常类型
    public Result exceptionHandler(SQLIntegrityConstraintViolationException ex){
        //下面这条是报错信息
        //Duplicate entry 'zhangsan' for key 'employy.idx_username'
        String message = ex.getMessage();
        if(message.contains("Duplicate entry")){
            String[] split = message.split(" ");
            String msg = split[2] + "已存在";//取zhangsan
            log.error("异常信息：{}", msg);
            return Result.error(msg);
        }else{
            //规范化的常量
            return Result.error(MessageConstant.UNKNOWN_ERROR);
        }

    }


}
