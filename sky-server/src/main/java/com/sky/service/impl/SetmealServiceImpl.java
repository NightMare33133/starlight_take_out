package com.sky.service.impl;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.sky.constant.MessageConstant;
import com.sky.constant.StatusConstant;
import com.sky.dto.SetmealDTO;
import com.sky.dto.SetmealPageQueryDTO;
import com.sky.entity.Setmeal;
import com.sky.entity.SetmealDish;
import com.sky.exception.DeletionNotAllowedException;
import com.sky.mapper.SetmealDishMapper;
import com.sky.mapper.SetmealMapper;
import com.sky.result.PageResult;
import com.sky.service.SetmealService;
import com.sky.vo.DishVO;
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

    /**
     * 分页查询
     * @param setmealPageQueryDTO
     * @return
     */
    public PageResult pageQuery(SetmealPageQueryDTO setmealPageQueryDTO) {
        PageHelper.startPage(setmealPageQueryDTO.getPage(), setmealPageQueryDTO.getPageSize());
        Page<DishVO> page = setmealMapper.pageQuery(setmealPageQueryDTO);

        return new PageResult(page.getTotal(), page.getResult());
    }

    /**
     * 批量删除套餐
     * @param ids
     */
    @Transactional
    public void deleteBatch(List<Long> ids) {
        //业务规则：
        //- 可以一次删除一个套餐，也可以批量删除套餐
        //- 起售中的套餐不能删除

        //所以需要先判断状态是否为起售中
        for (Long id : ids) {
            //基于ID先用SQL查一下状态
            Setmeal setmeal = setmealMapper.getById(id);
            if(setmeal.getStatus() == StatusConstant.ENABLE ){
                //如果是起售中的套餐，抛出一个业务异常
                throw new DeletionNotAllowedException(MessageConstant.SETMEAL_ON_SALE);
            }
        }
        //里面如果没有包含起售状态的套餐则可以开始进行删除，
        // 但是删除的时候既要在setmeal中删除，也要在setmealDish中把相关联的记录删除
        for(Long setmealId : ids){
            //删除套餐表中的数据
            setmealMapper.deleteById(setmealId);
            //删除套餐菜品关系表中的数据
            setmealDishMapper.deleteBySetmealId(setmealId);
        }

    }
}
