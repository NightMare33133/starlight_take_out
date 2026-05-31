package com.sky.controller.admin;


import com.sky.dto.DishDTO;
import com.sky.dto.DishPageQueryDTO;
import com.sky.result.PageResult;
import com.sky.result.Result;
import com.sky.service.DishService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/admin/dish")
@Api(tags = "菜品管理接口")
@Slf4j
public class DishController {

    @Autowired
    private DishService dishService;

    /**
     * 新增菜品
     * @param dishDTO
     * @return
     */
    @PostMapping
    @ApiOperation("新增菜品")
    public Result save(@RequestBody DishDTO dishDTO) {
        log.info("新增菜品：{}", dishDTO);
        dishService.saveWithFlavor(dishDTO);
        return Result.success();
    }

    /**
     * 菜品分页查询
     * @param dishPageQueryDTO
     * @return
     */
    //这里是query方式所以不需要用RequestBody注解
    @GetMapping("/page")
    @ApiOperation("菜品列表分页查询")
    public Result <PageResult> page(DishPageQueryDTO dishPageQueryDTO) {
        log.info("菜品列表分页查询：{}", dishPageQueryDTO);
        PageResult pageResult=  dishService.pageQuery(dishPageQueryDTO);
        return Result.success(pageResult);
    }

    /**
     * 菜品批量删除
     * @param ids
     * @return
     */
    //需求文档说是query类型 也就是在url中传递参数 例如：/admin/dish?ids=1,2,3
    //@RequestParam注解表示从请求参数中获取ids的值，并将其转换为List<Long>类型
    @DeleteMapping
    @ApiOperation("批量删除菜品")
    public Result delete(@RequestParam List<Long> ids){
        log.info("批量删除菜品：ids={}", ids);
        dishService.deleteBatch(ids);
        return Result.success();
    }

}
