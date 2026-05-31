package com.sky.service.impl;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.sky.constant.MessageConstant;
import com.sky.constant.StatusConstant;
import com.sky.dto.DishDTO;
import com.sky.dto.DishPageQueryDTO;
import com.sky.entity.Dish;
import com.sky.entity.DishFlavor;
import com.sky.exception.DeletionNotAllowedException;
import com.sky.mapper.DishFlavorMapper;
import com.sky.mapper.DishMapper;
import com.sky.mapper.SetmealDishMapper;
import com.sky.mapper.SetmealMapper;
import com.sky.result.PageResult;
import com.sky.service.DishService;
import com.sky.vo.DishVO;
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
    @Autowired
    private SetmealDishMapper setmealDishMapper;

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

    /**
     * 菜品分页查询
     * @param dishPageQueryDTO
     * @return
     */
    public PageResult pageQuery(DishPageQueryDTO dishPageQueryDTO) {
        PageHelper.startPage(dishPageQueryDTO.getPage(), dishPageQueryDTO.getPageSize());
        Page<DishVO> page = dishMapper.pageQuery(dishPageQueryDTO);

        return new PageResult(page.getTotal(), page.getResult());
    }

    /**
     * 菜品批量删除
     * @param ids
     * @return
     */
    @Transactional//设计多次SQL操作
    public void deleteBatch(List<Long> ids) {
        //1.判断当前菜品是否能够删除--- 是否存在起售中的菜品？
        for(Long id: ids){
            Dish dish = dishMapper.getById(id);
            if(dish.getStatus()== StatusConstant.ENABLE){
                //当前菜品处于起售状态，不能删除
                throw new DeletionNotAllowedException(MessageConstant.DISH_ON_SALE);
            }
        }
        //2.判断当前菜品是否能够删除---是否被套餐关联了？
        List<Long> setmealIds = setmealDishMapper.getSetmealIdsByDishIds(ids);
        if(setmealIds!=null&&setmealIds.size()>0){
            //当前菜品被套餐关联了，不能删除
            throw new DeletionNotAllowedException(MessageConstant.DISH_BE_RELATED_BY_SETMEAL);
        }

        //3.删除菜品中的菜品数据,删除菜品关联的口味数据
        //ids.for可以快捷写出
        for (Long id : ids) {
            dishMapper.deleteById(id);
            dishFlavorMapper.deleteByDishId(id);
        }

        
    }
}
