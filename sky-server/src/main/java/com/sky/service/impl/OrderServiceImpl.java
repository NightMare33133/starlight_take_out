package com.sky.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.github.xiaoymin.knife4j.core.util.CollectionUtils;
import com.sky.constant.MessageConstant;
import com.sky.context.BaseContext;
import com.sky.dto.OrdersPageQueryDTO;
import com.sky.dto.OrdersPaymentDTO;
import com.sky.dto.OrdersSubmitDTO;
import com.sky.entity.*;
import com.sky.exception.AddressBookBusinessException;
import com.sky.exception.OrderBusinessException;
import com.sky.exception.ShoppingCartBusinessException;
import com.sky.mapper.*;
import com.sky.result.PageResult;
import com.sky.service.OrderService;
import com.sky.utils.WeChatPayUtil;
import com.sky.vo.OrderPaymentVO;
import com.sky.vo.OrderSubmitVO;
import com.sky.vo.OrderVO;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class OrderServiceImpl implements OrderService {

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private OrderDetailMapper orderDetailMapper;
    @Autowired
    private AddressBookMapper addressBookMapper;
    @Autowired
    private ShoppingCartMapper shoppingCartMapper;
    @Autowired
    private WeChatPayUtil weChatPayUtil;

    @Autowired
    private UserMapper userMapper;

    /**
     * 用户下单
     * @param ordersSubmitDTO
     * @return
     */
    @Transactional
    public OrderSubmitVO submitOrder(OrdersSubmitDTO ordersSubmitDTO) {

        //1.处理各种业务异常（判断地址库为空，购物车数据为空）
        AddressBook addressBook = addressBookMapper.getById(ordersSubmitDTO.getAddressBookId());
        if (addressBook == null) {
            //抛出业务异常
            throw new AddressBookBusinessException(MessageConstant.ADDRESS_BOOK_IS_NULL);
        }

        //查询用户当前的购物车数据
        Long userId = BaseContext.getCurrentId();

        ShoppingCart shoppingCart = new ShoppingCart();
        shoppingCart.setUserId(userId);
        List<ShoppingCart> shoppingCartList = shoppingCartMapper.list(shoppingCart);//这里需要传入ShoppingCart对象

        if(shoppingCartList == null || shoppingCartList.size() == 0){
            //抛出业务异常
            throw new ShoppingCartBusinessException(MessageConstant.SHOPPING_CART_IS_NULL);
        }


        //2.向订单表加入一条数据
        //先构造订单对象
        Orders orders = new Orders();
        BeanUtils.copyProperties(ordersSubmitDTO, orders);
        //从DTO拷贝过来之后就是要开始设置属性，你要知道DTO里哪些是没有的
        orders.setOrderTime(LocalDateTime.now());
        orders.setPayStatus(Orders.UN_PAID);
        orders.setStatus(Orders.PENDING_PAYMENT);
        orders.setNumber(String.valueOf(System.currentTimeMillis()));
        orders.setPhone(addressBook.getPhone());//地址簿里面会有
        orders.setConsignee(addressBook.getConsignee());
        orders.setUserId(userId);
        //拼接一下地址
        String address = addressBook.getProvinceName()+addressBook.getCityName()+addressBook.getDistrictName()+addressBook.getDetail();
        orders.setAddress(address);

        orderMapper.insert(orders);

        List<OrderDetail> orderDetailList = new ArrayList<>();
        //3.向订单明细表插入n条数据
        for(ShoppingCart cart : shoppingCartList){
            OrderDetail orderDetail = new OrderDetail();
            BeanUtils.copyProperties(cart, orderDetail);
            orderDetail.setOrderId(orders.getId());//设置当前订单明细关联的订单id 这个是Mapper操作回显的
            orderDetailList.add(orderDetail);
        }
        orderDetailMapper.insertBatch(orderDetailList);

        //4.清空当前用户的购物车数据
        shoppingCartMapper.deleteByUserId(userId);

        //5.封装VO返回结果
        OrderSubmitVO orderSubmitVO = OrderSubmitVO.builder()
                .id(orders.getId())
                .orderTime(orders.getOrderTime())
                .orderNumber(orders.getNumber())
                .orderAmount(orders.getAmount())
                .build();
        return orderSubmitVO;
    }


    /**
     * 订单支付
     *
     * @param ordersPaymentDTO
     * @return
     */
    public OrderPaymentVO payment(OrdersPaymentDTO ordersPaymentDTO) throws Exception {
        // 当前登录用户id
        Long userId = BaseContext.getCurrentId();
        User user = userMapper.getById(userId);

//        //调用微信支付接口，生成预支付交易单
//        JSONObject jsonObject = weChatPayUtil.pay(
//                ordersPaymentDTO.getOrderNumber(), //商户订单号
//                new BigDecimal(0.01), //支付金额，单位 元
//                "苍穹外卖订单", //商品描述
//                user.getOpenid() //微信用户的openid
//        );
        //这里我是没有那个码- -所以就不具体调用了
        JSONObject jsonObject = new JSONObject();

        if (jsonObject.getString("code") != null && jsonObject.getString("code").equals("ORDERPAID")) {
            throw new OrderBusinessException("该订单已支付");
        }

        OrderPaymentVO vo = jsonObject.toJavaObject(OrderPaymentVO.class);
        vo.setPackageStr(jsonObject.getString("package"));

        return vo;
    }

    /**
     * 支付成功，修改订单状态
     *
     * @param outTradeNo
     */
    public void paySuccess(String outTradeNo) {

        // 根据订单号查询订单
        Orders ordersDB = orderMapper.getByNumber(outTradeNo);

        // 根据订单id更新订单的状态、支付方式、支付状态、结账时间
        Orders orders = Orders.builder()
                .id(ordersDB.getId())
                .status(Orders.TO_BE_CONFIRMED)
                .payStatus(Orders.PAID)
                .checkoutTime(LocalDateTime.now())
                .build();

        orderMapper.update(orders);
    }

    /**
     * 查询历史订单
     * @param page
     * @param pageSize
     * @param status
     * @return
     */
    public PageResult pageQueryOrder(Integer page, Integer pageSize, Integer status) {
        //- 分页查询历史订单
        //- 可以根据订单状态查询
        //- 展示订单数据时，需要展示的数据包括：下单时间、订单状态、订单金额、订单明细（商品名称、图片）
        //使用PageHelper设置分页查询
        PageHelper.startPage(page, pageSize);
        //传入的内容是订单分页信息以及的订单状态，
        // 但是我们是需要根据订单的状态展示：下单时间、订单状态、订单金额、订单明细（商品名称、图片）
        // 那么这些内容其实就需要用户的本身信息，所以是要基于BaseContext来拿信息
        // 因为项目中有OrdersPageQueryDTO,我们也可以就基于这个来准备一下数据
        OrdersPageQueryDTO ordersPageQueryDTO = new OrdersPageQueryDTO();
        ordersPageQueryDTO.setUserId(BaseContext.getCurrentId());
        ordersPageQueryDTO.setStatus(status);

        //总之基于当前ID和状态进行分页查询得到批量的order
        Page<Orders> pages = orderMapper.pageQuery(ordersPageQueryDTO);

        //这个Page对象里面就包含了分页查询的结果以及分页信息（总条数等），我们需要返回的内容还缺少订单详情
        //这个VO是用来装我们最终的返回对象的
        List<OrderVO> orderVOList = new ArrayList<>();

        if(pages != null && pages.getTotal() > 0){
            //遍历page对象取出每个订单信息的详情
            for(Orders orders : pages){
                Long orderId = orders.getId();

                //根据订单id查询订单详情
                //通过接口文档以及OrderVO可以知道，我们需要一个List<OrderDetail>的数据类型装订单详情
                List<OrderDetail> orderDetails = orderDetailMapper.getByOrderId(orderId);
                //获取之后需要装入OrderVO对象中
                OrderVO orderVO = new OrderVO();
                //首先吧基础信息存入orderVO
                BeanUtils.copyProperties(orders, orderVO);
                //再把订单详情存入
                orderVO.setOrderDetailList(orderDetails);
                //这样OrderVO的内容就完整了
                orderVOList.add(orderVO);
            }
        }

        //在Dish中的写法是PageResult(page.getTotal(), page.getResult());
        //但是这里需要返回的对象我们是重新装填的
        return new PageResult(pages.getTotal(), orderVOList);
    }

    /**
     * 查询订单详情
     * @param id
     * @return
     */
    public OrderVO detail(Long id) {
        //根据ID查询具体订单
        Orders orders = orderMapper.getById(id);
        //根据订单id查询订单详情
        List<OrderDetail> orderDetails = orderDetailMapper.getByOrderId(id);
        //获取之后需要装入OrderVO对象中
        OrderVO orderVO = new OrderVO();
        BeanUtils.copyProperties(orders, orderVO);
        //再把订单详情存入
        orderVO.setOrderDetailList(orderDetails);

        return orderVO;
    }

    /**
     * 用户取消订单
     * @param id
     */
    public void userCancelById(Long id) {
        //业务规则：
        //- 待支付和待接单状态下，用户可直接取消订单
        //- 商家已接单状态下，用户取消订单需电话沟通商家
        //- 派送中状态下，用户取消订单需电话沟通商家
        //- 如果在待接单状态下取消订单，需要给用户退款
        //- 取消订单后需要将订单状态修改为“已取消”
        // 传入的信息只有id所以要基于id来操作
        // 所以首先根据id查询订单
        Orders ordersDB = orderMapper.getById(id);
        // 判断订单是否存在
        if(ordersDB == null){
            //--这个是参考的
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
        }

        //判断订单状态 1待付款 2待接单 3已接单 4派送中 5已完成 6已取消
        //待支付和待接单状态下，用户可直接取消订单
        //这里是对于1待付款 2待接单状态的订单可以直接取消，所以如果订单状态大于2就说明不满足条件
        if(ordersDB.getStatus() > 2){
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }
        Orders orders = new Orders();
        orders.setId(ordersDB.getId());

        //对于订单处于待接单状态的订单需要给用户退款，所以需要判断一下订单状态是否为2
        if(ordersDB.getStatus().equals(Orders.TO_BE_CONFIRMED)){
            //- -这里肯定是无法调用wx的退款功能
//            //调用微信支付退款接口
//            weChatPayUtil.refund(
//                    ordersDB.getNumber(), //商户订单号
//                    ordersDB.getNumber(), //商户退款单号
//                    new BigDecimal(0.01),//退款金额，单位 元
//                    new BigDecimal(0.01));//原订单金额
            //支付状态修改为 退款
            orders.setPayStatus(Orders.REFUND);
        }
        //更新订单状态、取消原因、取消时间
        orders.setStatus(Orders.CANCELLED);
        orders.setCancelReason("用户取消订单");
        orders.setCancelTime(LocalDateTime.now());
        orderMapper.update(orders);

    }

    /**
     * 基于订单id再来一单
     * @param id
     */
    public void repetition(Long id) {
        //因为传入的只是订单id，这肯定是不够的，你需要知道是那个用户先- -
        Long userId = BaseContext.getCurrentId();

        // 根据订单id查询当前的订单详情
        List<OrderDetail> orderDetails = orderDetailMapper.getByOrderId(id);
        //业务规则：
        //- 再来一单就是将原订单中的商品重新加入到购物车中
        // 所以这里提取了具体的商品信息，自然是需要商品详情信息对象转换为购物车对象的
        List<ShoppingCart> shoppingCartList = new ArrayList<>();

        for (OrderDetail x : orderDetails) {
            // 1. 每遍历到一个订单商品，就创建一个新的购物车对象
            ShoppingCart shoppingCart = new ShoppingCart();

            // 2. 将原订单详情里面的菜品信息（比如菜名、价格、图片等）复制到购物车对象中
            // 注意最后那个 "id"，意思是：不要复制 id 字段！因为购物车需要生成自己新的主键 id。
            BeanUtils.copyProperties(x, shoppingCart, "id");

            // 3. 补充一些订单明细里没有，但购物车里必须要有的字段
            shoppingCart.setUserId(userId);
            shoppingCart.setCreateTime(LocalDateTime.now());

            // 4. 把做好的购物车对象塞进你准备好的 List 里面
            shoppingCartList.add(shoppingCart);
        }
        //这样shoppingCartList的list就做好了，可以批量存入数据库了
        shoppingCartMapper.insertBatch(shoppingCartList);

    }

    /**
     * 条件搜索查询
     * @param ordersPageQueryDTO
     * @return
     */
    public PageResult conditionSearch(OrdersPageQueryDTO ordersPageQueryDTO) {
        //传入的是OrdersPageQueryDTO对象所以具有page和pageSize这些分页信息
        //同时也有一些具体订单条件如number,phone,status和userId等
        //业务规则：
        //- 输入订单号/手机号进行搜索，支持模糊搜索
        //- 根据订单状态进行筛选
        //- 下单时间进行时间筛选
        //- 搜索内容为空，提示未找到相关订单
        //- 搜索结果页，展示包含搜索关键词的内容
        //- 分页展示搜索到的订单数据
        //总之PageHelper肯定得先的调用
        PageHelper.startPage(ordersPageQueryDTO.getPage(), ordersPageQueryDTO.getPageSize());
        //这里的pageQuery方法是我们自己写的，所以需要传入OrdersPageQueryDTO对象，里面就包含了我们需要的搜索条件
        Page<Orders> pages = orderMapper.pageQuery(ordersPageQueryDTO);
        //原本只做到上一条代码，调试前端发现没有返回地址，估计是还有VO的对象要再补一下封装对象的内容- -
        //结果发现原来在submit那里地址就没做好。。
        //这里是参考了一下答案的做法
        List<OrderVO>  orderVOList = getOrderVOList(pages);

        return new PageResult(pages.getTotal(), orderVOList);
    }

    private List<OrderVO> getOrderVOList(Page<Orders> page) {
        // 需要返回订单菜品信息，自定义OrderVO响应结果
        List<OrderVO> orderVOList = new ArrayList<>();

        List<Orders> ordersList = page.getResult();
        if (!CollectionUtils.isEmpty(ordersList)) {
            for (Orders orders : ordersList) {
                // 将共同字段复制到OrderVO
                OrderVO orderVO = new OrderVO();
                BeanUtils.copyProperties(orders, orderVO);
                String orderDishes = getOrderDishesStr(orders);

                // 将订单菜品信息封装到orderVO中，并添加到orderVOList
                orderVO.setOrderDishes(orderDishes);
                orderVOList.add(orderVO);
            }
        }
        return orderVOList;
    }

    /**
     * 根据订单id获取菜品信息字符串
     *
     * @param orders
     * @return
     */
    private String getOrderDishesStr(Orders orders) {
        // 查询订单菜品详情信息（订单中的菜品和数量）
        List<OrderDetail> orderDetailList = orderDetailMapper.getByOrderId(orders.getId());

        // 将每一条订单菜品信息拼接为字符串（格式：宫保鸡丁*3；）
        List<String> orderDishList = orderDetailList.stream().map(x -> {
            String orderDish = x.getName() + "*" + x.getNumber() + ";";
            return orderDish;
        }).collect(Collectors.toList());

        // 将该订单对应的所有菜品信息拼接在一起
        return String.join("", orderDishList);
    }
}
