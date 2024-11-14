package other.jh.xt;

import com.rkhd.platform.sdk.api.annotations.RequestMethod;
import com.rkhd.platform.sdk.api.annotations.RestApi;
import com.rkhd.platform.sdk.api.annotations.RestMapping;
import com.rkhd.platform.sdk.api.annotations.RestQueryParam;
import com.rkhd.platform.sdk.data.model.*;
import com.rkhd.platform.sdk.exception.ScriptBusinessException;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Stark on 2024-11-13 15:54
 *
 * todo 合同-》订单
 * 合同-》报价
 * todo  报价-》订单
 */
@RestApi(baseUrl = "/jh")
public class PushContractToOrder extends other.jh.xt.XObjectUtils {

    //合同->订单
    @RestMapping(value = "/PushOrder_API", method = RequestMethod.GET)
    public String PushQuote(@RestQueryParam(name = "id") Long id) throws ScriptBusinessException {
        //加载接口
        init();
        Order contract = new Order();
        contract.setId(id);
        contract=getXObject(contract, "合同");

        //合同->订单
        Po__c order = new Po__c();
        order.setCustomItem1__c(contract.getId());//销售合同

        String htmx="select id,orderId,name,wfcdyf__c,bzgg__c,dw__c,ggxh__c,unitPrice,quantity,priceTotal,comment  from OrderProduct where orderId="+"'"+id+"'";
        List<OrderProduct> orderProducts = Query(new OrderProduct(), htmx, "合同明细");
        if (orderProducts.isEmpty()){
            return setResult(-300,"合同明细为空，变更失败！","");
        }
        //set业务类型...
        order.setEntityType(3530992200351898L);//业务类型
        order.setOwnerId(contract.getOwnerId());//所有人
        //客户 联系人 销售员 销售组织 是否内部 分销渠道 产品组 销售部门 交货方式 付款条件 订单日期
        //订单截止日期 价格截止日期 是否
        order.setProNum__c(contract.getProductsAmount());//产品总量
        order.setSfnb__c(contract.getSfnb__c());
        order.setFxqd__c(contract.getFxqd__c());
        order.setXszz__c(contract.getXszz__c());
        order.setJhfs__c(contract.getJhfs__c());
        order.setFktj__c(contract.getFktj__c());
        order.setXsbm__c(contract.getXsbm__c());
        order.setCpz__c(contract.getCpz__c());
        order.setSfnd__c(contract.getSfsnd__c());
        order.setBzfs__c(contract.getBzfs__c());
        order.setYftk__c(contract.getYftk__c());
        order.setJsfs__c(contract.getJsfs__c());
        order.setXsy__c(contract.getXsy__c());
        order.setLxr__c(contract.getLxr__c());
        order.setLxrdh__c(contract.getLxrdh__c());
        order.setShdz__c(contract.getContactAddress());
        order.setKhmc__c(contract.getAccountId());

        Long l = InsertXObject(order, "订单");


        List<OrderDetail__c> orderDetail__cList= new ArrayList<>();
        //合同明细->订单明细
        for (OrderProduct orderProduct : orderProducts){
            OrderDetail__c quoteDetail=new OrderDetail__c();
            quoteDetail.setOrder__c(l);//主子明细
            quoteDetail.setEntityType(3550657407516808L);
            quoteDetail.setCpmc__c(orderProduct.getProductId());//产品
            quoteDetail.setHsdj__c(orderProduct.getUnitPrice());//含税单价
            quoteDetail.setNumber__c(orderProduct.getQuantity());//销售数量
            quoteDetail.setDw__c(orderProduct.getDw__c());//单位
            quoteDetail.setCustomItem7__c(orderProduct.getPriceTotal());//总价
            orderDetail__cList.add(quoteDetail);
        }
        InsertXObject(orderDetail__cList,"订单明细");


        return setResult(200, "https://crm.xiaoshouyi.com/bff/neoweb#/entityDetail/po__c/"+l+"?objectApiKey=po__c&recordId="+l+"&objectId=3530997788229761&busiTypeId=3530992200351898" , l.toString());


    }


    }
