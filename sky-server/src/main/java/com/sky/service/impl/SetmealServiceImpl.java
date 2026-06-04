package com.sky.service.impl;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.sky.constant.MessageConstant;
import com.sky.constant.StatusConstant;
import com.sky.dto.SetmealDTO;
import com.sky.dto.SetmealPageQueryDTO;
import com.sky.entity.Dish;
import com.sky.entity.Setmeal;
import com.sky.entity.SetmealDish;
import com.sky.exception.DeletionNotAllowedException;
import com.sky.exception.SetmealEnableFailedException;
import com.sky.mapper.DishMapper;
import com.sky.mapper.SetmealDishMapper;
import com.sky.mapper.SetmealMapper;
import com.sky.result.PageResult;
import com.sky.service.SetmealService;
import com.sky.vo.DishItemVO;
import com.sky.vo.DishVO;
import com.sky.vo.SetmealVO;
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
    @Autowired
    private DishMapper dishMapper;
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

    /**
     * 根据id查询套餐，用于修改页面回显数据
     * @param id
     * @return
     */
    public SetmealVO getByIdWithDish(Long id) {
        //首先这里是传入了套餐ID，所以我们要基于ID拿到套餐的基本信息
        Setmeal setmeal = setmealMapper.getById(id);
        //接着还要获取到相应的套餐信息
        List<SetmealDish> setmealDishes = setmealDishMapper.getBySetmealId(id);

        //但是这些内容应该通过VO返回
        SetmealVO setmealVO = new SetmealVO();
        BeanUtils.copyProperties(setmeal, setmealVO);
        setmealVO.setSetmealDishes(setmealDishes);
        return setmealVO;
    }

    /**
     * 修改套餐
     * @param setmealDTO
     * @return
     */
    @Transactional
    public void updateWithDish(SetmealDTO setmealDTO) {
        //这个其实应该会比较类似新增来着？总之先把传入的内容覆盖到setmeal对象中
        Setmeal setmeal = new Setmeal();
        BeanUtils.copyProperties(setmealDTO, setmeal);
        //更新套餐表中的基本信息
        setmealMapper.update(setmeal);

        //获取套餐的ID --这是为了处理setmeal_dish表中的相关联信息
        Long setmealId = setmeal.getId();

        //比较好的逻辑方式就是把原本的内容删掉，然后再把新得到的覆盖上去
        setmealDishMapper.deleteBySetmealId(setmealId);

        //这个就是DTO传入的新的内容
        List<SetmealDish>  setmealDishes = setmealDTO.getSetmealDishes();
        //先判断一下List是否为空？
        if(setmealDishes!=null&&setmealDishes.size()>0){
            //非空则挨个将当前的setmealId再次绑定一下（因为之前删除了，所以现在需要重新绑定）
            setmealDishes.forEach(setmealDish -> {
                setmealDish.setSetmealId(setmealId);
            });
            //执行具体SQL
            setmealDishMapper.insertBatch(setmealDishes);
        }


    }

    /**
     * 启用/禁用套餐
     * @param status
     * @param id
     * @return
     */
    public void startOrStop(Integer status, Long id) {
        //业务规则：
        //- 可以对状态为起售的套餐进行停售操作，可以对状态为停售的套餐进行起售操作
        //- 起售的套餐可以展示在用户端，停售的套餐不能展示在用户端
        //- 起售套餐时，如果套餐内包含停售的菜品，则不能起售
        //总之我们现在拿到的是状态和这个套餐的ID，我们还没有这个套餐的相关信息，
        //这里就是基于将要变更的状态来进行不同的处理
        if (status == StatusConstant.ENABLE) {
            //select a.* from dish a left join setmeal_dish b on a.id = b.dish_id where b.setmeal_id = ?
            //- 因为起售套餐时，如果套餐内包含停售的菜品，则不能起售 所以我们需要看一下菜品中是否包含停售菜品
            //这里是通过套餐的ID拿到相关的所有dish
            List<Dish> dishes = dishMapper.getBySetmealId(id);
            if (dishes != null && dishes.size() > 0) {
                dishes.forEach(dish -> {
                    if(StatusConstant.DISABLE == (dish.getStatus())){
                        throw new SetmealEnableFailedException(MessageConstant.SETMEAL_ENABLE_FAILED);
                    }
                });
            }
        }
        //- -总之就是起售的时候需要特殊处理一下，两种状态共性就是要改的内容是一致的
        Setmeal setmeal = Setmeal.builder()
                .id(id)
                .status(status)
                .build();
        setmealMapper.update(setmeal);
    }


    /**
     * 条件查询
     * @param setmeal
     * @return
     */
    public List<Setmeal> list(Setmeal setmeal) {
        List<Setmeal> list = setmealMapper.list(setmeal);
        return list;
    }


    /**
     * 根据id查询菜品选项
     * @param id
     * @return
     */
    public List<DishItemVO> getDishItemById(Long id) {
        return setmealMapper.getDishItemBySetmealId(id);
    }


}
