package com.sky.controller.admin;


import com.sky.dto.OrdersCancelDTO;
import com.sky.dto.OrdersConfirmDTO;
import com.sky.dto.OrdersPageQueryDTO;
import com.sky.dto.OrdersRejectionDTO;
import com.sky.result.PageResult;
import com.sky.result.Result;
import com.sky.service.OrderService;
import com.sky.vo.OrderStatisticsVO;
import com.sky.vo.OrderVO;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController("adminOrderController")
@Slf4j
@RequestMapping("/admin/order")
public class OrderController {

    @Autowired
    private OrderService orderService;

    /**
     * 订单条件查询
     * @param ordersPageQueryDTO
     * @return
     */
    @GetMapping("/conditionSearch")
    @ApiOperation("订单条件查询")
    public Result<PageResult> conditionSearch(OrdersPageQueryDTO ordersPageQueryDTO) {
        log.info("订单条件查询：{}", ordersPageQueryDTO);
        PageResult pageResult = orderService.conditionSearch(ordersPageQueryDTO);
        return Result.success(pageResult);

    }

    /**
     * 各个状态的订单数量统计
     *
     * @return
     */
    @GetMapping("/statistics")
    @ApiOperation("各个订单的订单统计")
    public Result<OrderStatisticsVO> statistics() {
        log.info("订单统计");
        OrderStatisticsVO orderStatisticsVO = orderService.statistics();
        return Result.success(orderStatisticsVO);
    }


    /**
     * 订单详情
     * @param id
     * @return
     */
    @GetMapping("/details/{id}")
    @ApiOperation("订单详情")
    public Result<OrderVO> details(@PathVariable("id") Long id) {
        //业务规则：
        //- 订单详情页面需要展示订单基本信息（状态、订单号、下单时间、收货人、电话、收货地址、金额等）
        //- 订单详情页面需要展示订单明细数据（商品名称、数量、单价）
        log.info("订单详情，订单id：{}", id);
        OrderVO orderVO = orderService.detail(id);
        return Result.success(orderVO);
    }

    /**
     * 接单
     * @param ordersConfirmDTO
     * @return
     */
    @PutMapping("/confirm")
    @ApiOperation("接单")
    public Result confirm(@RequestBody OrdersConfirmDTO ordersConfirmDTO) {
        log.info("接单：{}", ordersConfirmDTO);
        //业务规则：
        //- 商家接单其实就是将订单的状态修改为“已接单”
        orderService.confirm(ordersConfirmDTO);
        return Result.success();
    }
    /**
     * 拒单
     *
     * @return
     */
    @PutMapping("/rejection")
    @ApiOperation("拒单")
    public Result rejection(@RequestBody OrdersRejectionDTO ordersRejectionDTO) throws Exception {
        //业务规则：
        //- 商家拒单其实就是将订单状态修改为“已取消”
        //- 只有订单处于“待接单”状态时可以执行拒单操作
        //- 商家拒单时需要指定拒单原因
        //- 商家拒单时，如果用户已经完成了支付，需要为用户退款
        orderService.rejection(ordersRejectionDTO);
        return Result.success();
    }

    /**
     * 取消订单
     *
     * @return
     */
    @PutMapping("/cancel")
    @ApiOperation("取消订单")
    public Result cancel(@RequestBody OrdersCancelDTO ordersCancelDTO) throws Exception {
        orderService.cancel(ordersCancelDTO);
        return Result.success();
    }

    /**
     * 派送订单
     *
     * @return
     */
    @PutMapping("/delivery/{id}")
    @ApiOperation("派送订单")
    public Result delivery(@PathVariable("id") Long id) {
        orderService.delivery(id);
        return Result.success();
    }

    /**
     * 完成订单
     *
     * @return
     */
    @PutMapping("/complete/{id}")
    @ApiOperation("完成订单")
    public Result complete(@PathVariable("id") Long id) {
        orderService.complete(id);
        return Result.success();
    }



}
