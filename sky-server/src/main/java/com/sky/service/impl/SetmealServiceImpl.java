package com.sky.service.impl;

import com.sky.dto.SetmealDTO;
import com.sky.entity.Setmeal;
import com.sky.entity.SetmealDish;
import com.sky.mapper.SetmealDishMapper;
import com.sky.mapper.SetmealMapper;
import com.sky.service.SetmealService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class SetmealServiceImpl implements SetmealService {

    @Autowired
    private SetmealMapper setmealMapper;
    @Autowired
    private SetmealDishMapper setmealDishMapper;
    //业务规则：
    //- 套餐名称唯一
    //- 套餐必须属于某个分类
    //- 套餐必须包含菜品
    //- 名称、分类、价格、图片为必填项
    //- 添加菜品窗口需要根据分类类型来展示菜品
    //- 新增的套餐默认为停售状态
    //
    //接口设计（共涉及到4个接口）：
    //
    //- 根据类型查询分类（已完成）
    //- 根据分类id查询菜品
    //- 图片上传（已完成）
    //- 新增套餐
    /**
     * 新增套餐
     * @param setmealDTO
     * @return
     */
    @Transactional
    public void saveWithDish(SetmealDTO setmealDTO) {
        Setmeal setmeal = new Setmeal();
        BeanUtils.copyProperties(setmealDTO, setmeal);
        //向套餐表加入一条数据
        setmealMapper.insert(setmeal);
        //因为需要再存到setmealdish表中记录套餐关系
        //获取生成的套餐的ID
        Long setmealId = setmeal.getId();
        //setmealDTO中携带一个套餐菜品关系数组 数据类型为List<SetmealDish>所以需要提取出来
        // 1. 获取套餐关联的菜品集合
        List<SetmealDish> setmealDishes = setmealDTO.getSetmealDishes();
        // 2. 判断集合是否为空（严谨一点总是好的）
        if (setmealDishes != null && setmealDishes.size() > 0) {

            // 3. 遍历集合，为每一个菜品对象绑定套餐 ID
            setmealDishes.forEach(setmealDish -> {
                setmealDish.setSetmealId(setmealId);
            });

            // 4. 批量保存套餐和菜品的关联关系（性能拉满！）
            setmealDishMapper.insertBatch(setmealDishes);
        }
    }
}
