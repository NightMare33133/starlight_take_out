package com.sky.service.impl;

import com.sky.dto.DishDTO;
import com.sky.entity.Dish;
import com.sky.entity.DishFlavor;
import com.sky.mapper.DishFlavorMapper;
import com.sky.mapper.DishMapper;
import com.sky.service.DishService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;


@Service
public class DishServiceImpl implements DishService {

    @Autowired
    private DishMapper dishMapper;
    @Autowired
    private DishFlavorMapper dishFlavorMapper;

    /**
     * 新增菜品和对应口味
     * @param dishDTO
     */
    @Transactional//这个需要在application中开启注解方式的事务管理
    //因为既要加入菜品表，还要加入口味表，所以需要事务管理，要么都成功，要么都失败
    public void saveWithFlavor(DishDTO dishDTO) {

        Dish dish = new Dish();
        BeanUtils.copyProperties(dishDTO, dish);//把dishDTO中的属性值复制到dish对象中

        //向菜品表加入一条数据
        dishMapper.insert(dish);

        //获取insert语句生成的主键值
        Long dishId = dish.getId();

        //向口味表插入n条数据
        List<DishFlavor> flavors = dishDTO.getFlavors();
        //用户可能没有提交口味
        if(flavors!=null&&flavors.size()>0){
            flavors.forEach(dishFlavor->{
                dishFlavor.setDishId(dishId);
            });
            //向口味表添加n条口味
            dishFlavorMapper.insertBatch(flavors);


        }


    }
}
