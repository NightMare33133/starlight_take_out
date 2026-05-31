package com.sky.mapper;


import java.util.List;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface SetmealDishMapper {


    /**
     * 根据菜品ID查询对应的套餐ID
     * @param dishIds
     * @return
     */
    //select setmeal_id from setmeal_dish where dish_id in (1,2,3) -->动态SQL所以需要Mybatis
    List<Long> getSetmealIdsByDishIds(List<Long> dishIds);
}
