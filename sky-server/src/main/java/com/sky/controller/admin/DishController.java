package com.sky.controller.admin;


import com.sky.dto.DishDTO;
import com.sky.dto.DishPageQueryDTO;
import com.sky.entity.Dish;
import com.sky.result.PageResult;
import com.sky.result.Result;
import com.sky.service.CategoryService;
import com.sky.service.DishService;
import com.sky.vo.DishVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/admin/dish")
@Api(tags = "菜品管理接口")
@Slf4j
public class DishController {

    @Autowired
    private DishService dishService;

    @Autowired
    private RedisTemplate redisTemplate;

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

        //清理缓存数据
        String key = "dish:" + dishDTO.getCategoryId();
        cleanCache(key);//这里是精确清理
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

        //将所有的菜品缓存数据清理掉，所有以dish_开头的key
        cleanCache("dish_*");
        return Result.success();
    }

    /**
     * 根据ID查询菜品
     * @param id
     * @return
     */
    @GetMapping("/{id}")
    @ApiOperation("根据ID查询菜品")
    //VO里面可以看到口味
    //@PathVariable的作用是从URL路径中获取id的值，例如：/admin/dish/1
    public Result<DishVO> getById(@PathVariable Long id){
        log.info("根据id查询菜品信息：id={}", id);
        DishVO dishVo = dishService.getByIdWithFlavor(id);
        return Result.success(dishVo);
    }

    /**
     * 修改菜品
     * @param dishDTO
     * @return
     */
    //和新增用的数据类型是一样的
    @PutMapping
    @ApiOperation("修改菜品")
    public Result update(@RequestBody DishDTO dishDTO){
        log.info("修改菜品：{}", dishDTO);
        dishService.updateWithFlavor(dishDTO);

        //这里有可能会因为修改分类，影响别的缓存数据
        //但是因为修改分类其实也不算常规操作，所以干脆直接全杀就好了
        //将所有的菜品缓存数据清理掉，所有以dish_开头的key
        cleanCache("dish_*");//这边是支持删除集合的
        return Result.success();

    }

    /**
     * 启用、禁用菜品
     * @param status
     * @param id
     * @return
     */
    @PostMapping("/status/{status}")
    @ApiOperation("启用、禁用菜品")
    public Result startOrStop(@PathVariable Integer status,@RequestParam Long id){
        log.info("修改菜品状态：id={}, status={}", id, status);
        dishService.startOrStop(id,status);

        //依旧皆杀
        cleanCache("dish_*");
        return Result.success();
    }

    /**
     * 根据分类id查询菜品
     * @param categoryId
     * @return
     */
    @GetMapping("/list")
    @ApiOperation("根据分类id查询菜品")
    public Result<List<Dish>> list(Long categoryId){
        log.info("根据分类id查询菜品：categoryId={}", categoryId);
        List<Dish> dishList = dishService.listByCategoryId(categoryId);
        return Result.success(dishList);

    }

    /**
     * 清理缓存数据
     * @param pattern
     */

    private void cleanCache(String pattern){
        //将所有的菜品缓存数据清理掉
        Set keys = redisTemplate.keys(pattern);
        redisTemplate.delete(keys);//这边是支持删除集合的
    }

}
