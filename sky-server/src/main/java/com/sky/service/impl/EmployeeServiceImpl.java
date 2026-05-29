package com.sky.service.impl;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.sky.constant.MessageConstant;
import com.sky.constant.PasswordConstant;
import com.sky.constant.StatusConstant;
import com.sky.context.BaseContext;
import com.sky.dto.EmployeeDTO;
import com.sky.dto.EmployeeLoginDTO;
import com.sky.dto.EmployeePageQueryDTO;
import com.sky.entity.Employee;
import com.sky.exception.AccountLockedException;
import com.sky.exception.AccountNotFoundException;
import com.sky.exception.PasswordErrorException;
import com.sky.mapper.EmployeeMapper;
import com.sky.result.PageResult;
import com.sky.service.EmployeeService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class EmployeeServiceImpl implements EmployeeService {



    @Autowired
    private EmployeeMapper employeeMapper;

    /**
     * 员工登录
     *
     * @param employeeLoginDTO
     * @return
     */
    public Employee login(EmployeeLoginDTO employeeLoginDTO) {
        String username = employeeLoginDTO.getUsername();
        String password = employeeLoginDTO.getPassword();

        //1、根据用户名查询数据库中的数据
        Employee employee = employeeMapper.getByUsername(username);

        //2、处理各种异常情况（用户名不存在、密码不对、账号被锁定）
        if (employee == null) {
            //账号不存在
            throw new AccountNotFoundException(MessageConstant.ACCOUNT_NOT_FOUND);
        }

        //密码比对
        // TODO 后期需要进行md5加密，然后再进行比对
        //DigestUtils.md5DigestAsHex方法的作用是对输入的字符串进行MD5加密，
        // 并将加密后的结果转换为十六进制字符串。
        // 它接受一个字节数组作为输入，并返回一个表示MD5哈希值的十六进制字符串。
        //password.getBytes()方法将字符串转换为字节数组，
        // 默认使用平台的默认字符集（通常是UTF-8）
        password = DigestUtils.md5DigestAsHex(password.getBytes());


        if (!password.equals(employee.getPassword())) {
            //密码错误
            throw new PasswordErrorException(MessageConstant.PASSWORD_ERROR);
        }

        if (employee.getStatus() == StatusConstant.DISABLE) {
            //账号被锁定
            throw new AccountLockedException(MessageConstant.ACCOUNT_LOCKED);
        }

        //3、返回实体对象
        return employee;
    }

    /**
     * 新增员工
     * @param employeeDTO
     */
    @Override
    public void save(EmployeeDTO employeeDTO) {
        Employee employee = new Employee();
        //对象属性拷贝
        //将employeeDTO中的属性值复制到employee对象中
        BeanUtils.copyProperties(employeeDTO, employee);//(源对象，目标对象)属性名必须是一致的才行

        //设置账号状态，默认为正常的
        employee.setStatus(StatusConstant.ENABLE);//这个是定义的常量类

        //设置密码，默认密码123456
        //getBytes()作用是将字符串转换为字节数组，默认使用平台的默认字符集（通常是UTF-8）
        //PasswordConstant.DEFAULT_PASSWORD是一个常量，表示默认密码123456
        employee.setPassword(DigestUtils.md5DigestAsHex(PasswordConstant.DEFAULT_PASSWORD.getBytes()));


        //以下的赋值由AutoFill实现的自动填充进行处理了
        //设置当前记录的创建实际和修改时间
//        employee.setCreateTime(LocalDateTime.now());
//        employee.setUpdateTime(LocalDateTime.now());
//
//        //设置当前记录创建人id和修改人id
////        employee.setCreateUser(10L);
////        employee.setUpdateUser(10L);
//        employee.setCreateUser(BaseContext.getCurrentId());
//        employee.setUpdateUser(BaseContext.getCurrentId());

        employeeMapper.insert(employee);


    }

    /**
     * 分页查询
     * @param employeePageQueryDTO
     * @return
     */
    @Override
    public PageResult pageQuery(EmployeePageQueryDTO employeePageQueryDTO) {
        //select * from employee limit 0,10分页查询
        //Mybatis有一个插件pagehelper，作用是
        //开始分页查询
        // 使用 PageHelper 开始分页：PageHelper 会把 page 和 pageSize 保存到 ThreadLocal 中，
        //随后的 MyBatis 查询会被 PageHelper 的拦截器拦截，自动在生成的 SQL 上增加 LIMIT / OFFSET（即物理分页）。
        // 注意：必须在执行 mapper 查询方法之前调用 startPage，且 startPage 与 mapper 查询要在同一线程中。
        PageHelper.startPage(employeePageQueryDTO.getPage(),employeePageQueryDTO.getPageSize());

        // 调用 mapper 的分页查询方法，触发实际的数据库查询。
        // 由于上面已经调用了 PageHelper.startPage，这次查询会返回一个 Page 对象（实现了 List）
        // Page 中除了当前页的数据集合外，还包含总记录数、页数、当前页码、每页大小等分页元信息，
        // 可以通过 page.getTotal()、page.getResult()（或 page.getPageNum()/getPageSize()/getPages() 等）访问。
        Page<Employee> page= employeeMapper.pageQuery(employeePageQueryDTO);

        long total = page.getTotal();
        List<Employee> record = page.getResult();

        return new PageResult(total,record);
    }

    @Override
    public void startOrStop(Integer status, Long id) {
        //update employee set status = ? where id = ?

        //下面这三行是传统派
//        Employee employee = new Employee();
//        employee.setStatus(status);
//        employee.setUpdateTime(LocalDateTime.now());

        //
        Employee employee = Employee.builder()
                .status(status)
                .id(id)
                .build();


        employeeMapper.update(employee);

    }

    /**
     * 根据ID查询员工
     * @param id
     * @return
     */
    @Override
    public Employee getById(Long id) {
        Employee employee = employeeMapper.getById(id);
        //返回密码
        employee.setPassword("****");
        return employee;
    }

    @Override
    public void update(EmployeeDTO employeeDTO) {
        Employee employee = new Employee();
        //拷贝DTO属性
        BeanUtils.copyProperties(employeeDTO, employee);

        //这里的赋值也由AutoFill自动填充
//        employee.setUpdateTime(LocalDateTime.now());
//        employee.setUpdateUser(BaseContext.getCurrentId());

        employeeMapper.update(employee);
    }


}
