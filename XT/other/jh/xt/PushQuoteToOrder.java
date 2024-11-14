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
 *  合同-》订单
 * 合同-》报价
 * todo  报价-》订单
 */
@RestApi(baseUrl = "/jh")
public class PushQuoteToOrder extends other.jh.xt.XObjectUtils {
    @RestMapping(value = "/PushOrder2_API", method = RequestMethod.GET)
    public String PushQuote(@RestQueryParam(name = "id") Long id) throws ScriptBusinessException {
        //加载接口
        init();
        //1.先查询出报价单信息
        Quote__c quote = new Quote__c();
        quote.setId(id);
        quote=getXObject(quote, "报价单");


        Po__c order = new Po__c();
        order.setBjd__c(quote.getId());



        //2.查询出报价单明细信息

        //合同-》报价单
        //查询报价单明细
        String bjdmx="select id,name,product__c,materialCode__c,contactDetail__c,hsdj__c,unit__c,payMode__c,jsfs__c," +
                "jhfs__c,yftk__c,xhfs__c,bjd__c,sumMoney__c,note__c,hb__c,xssl__c  from QuoteDetail__c where bjd__c= "+"'"+id+"'";
        List<QuoteDetail__c> orderProducts = Query(new QuoteDetail__c(), bjdmx, "报价单明细");



        if (orderProducts.isEmpty()){
            return setResult(-300,"报价单明细为空，变更失败！","");
        }
        //set 订单信息
        order.setEntityType(3530992200351898L);//业务类型
        order.setOwnerId(quote.getOwnerId());//所有人
        order.setKhmc__c(quote.getAccount__c());//客户名称

        order.setCustomItem1__c(quote.getContract__c());//合同

        //联系人电话
        Long l = InsertXObject(order, "订单");


        List<OrderDetail__c> orderDetail__cList= new ArrayList<>();
        //报价单明细QuoteDetail__c->订单明细
        for (QuoteDetail__c orderProduct : orderProducts){
            OrderDetail__c quoteDetail=new OrderDetail__c();
            quoteDetail.setOrder__c(l);//主子明细
            quoteDetail.setEntityType(3550657407516808L);


            quoteDetail.setCpmc__c(orderProduct.getProduct__c());//产品
            quoteDetail.setHsdj__c(orderProduct.getHsdj__c());//含税单价
            quoteDetail.setNumber__c(orderProduct.getXssl__c());//销售数量
            quoteDetail.setDw__c(orderProduct.getUnit__c());//单位
            quoteDetail.setCustomItem7__c(orderProduct.getSumMoney__c());//总价
            orderDetail__cList.add(quoteDetail);
        }
        InsertXObject(orderDetail__cList,"订单明细");


        return setResult(200, "https://crm.xiaoshouyi.com/bff/neoweb#/entityDetail/po__c/"+l+"?objectApiKey=po__c&recordId="+l+"&objectId=3530997788229761&busiTypeId=3530992200351898" , l.toString());


    }


    }
