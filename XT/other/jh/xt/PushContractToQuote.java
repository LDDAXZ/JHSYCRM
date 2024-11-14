package other.jh.xt;

import com.rkhd.platform.sdk.api.annotations.RequestMethod;
import com.rkhd.platform.sdk.api.annotations.RestApi;
import com.rkhd.platform.sdk.api.annotations.RestMapping;
import com.rkhd.platform.sdk.api.annotations.RestQueryParam;
import com.rkhd.platform.sdk.data.model.Order;
import com.rkhd.platform.sdk.data.model.OrderProduct;
import com.rkhd.platform.sdk.data.model.QuoteDetail__c;
import com.rkhd.platform.sdk.data.model.Quote__c;
import com.rkhd.platform.sdk.exception.ScriptBusinessException;


import java.util.ArrayList;
import java.util.List;

/**
 * Created by Stark on 2024-11-13 15:54
 *
 *
 * 合同-》报价 报价里边
 *
 */
@RestApi(baseUrl = "/jh")
public class PushContractToQuote extends other.jh.xt.XObjectUtils {
    @RestMapping(value = "/PushQuote_API", method = RequestMethod.GET)
    public String PushQuote(@RestQueryParam(name = "id") Long id) throws ScriptBusinessException {
        //加载接口
        init();
        Order contract = new Order();
        contract.setId(id);
        contract=getXObject(contract, "合同");

        //合同-》报价单

        Quote__c quote= new Quote__c();
        quote.setContract__c(contract.getId());
        String htmx="select id,orderId,name,wfcdyf__c,bzgg__c,dw__c,ggxh__c,unitPrice,quantity,priceTotal,comment  from OrderProduct where orderId="+"'"+id+"'";
        List<OrderProduct> orderProducts = Query(new OrderProduct(), htmx, "合同明细");



        if (orderProducts.isEmpty()){
            return setResult(-300,"合同明细为空，变更失败！","");
        }

        quote.setEntityType(3530960544251998L); //业务类型
        quote.setOwnerId(contract.getOwnerId());//所有人
        quote.setContract__c(contract.getId());//合同
        quote.setContractCode__c(contract.getPo());//合同编号
        quote.setAccount__c(contract.getAccountId());//客户
        quote.setContact__c(contract.getContactId());//联系人
        //联系人电话
        Long l = InsertXObject(quote, "报价单");


        List<QuoteDetail__c> quoteDetail__cList= new ArrayList<>();
        //报价单明细QuoteDetail__c
        for (OrderProduct orderProduct : orderProducts){
            QuoteDetail__c quoteDetail=new QuoteDetail__c();
            quoteDetail.setBjd__c(l);//主子明细
            quoteDetail.setEntityType(3549377430569052L);
            quoteDetail.setProduct__c(orderProduct.getProductId());//产品
            quoteDetail.setHsdj__c(orderProduct.getUnitPrice());//含税单价
            quoteDetail.setXssl__c(orderProduct.getQuantity());//销售数量
            quoteDetail.setUnit__c(orderProduct.getDw__c());//单位
            quoteDetail.setSumMoney__c(orderProduct.getPriceTotal());//总价
            quoteDetail__cList.add(quoteDetail);
        }
        InsertXObject(quoteDetail__cList,"报价单明细");


        return setResult(200, "https://crm.xiaoshouyi.com/bff/neoweb#/entityDetail/Quote__c/"+l+"?objectApiKey=Quote__c&recordId="+l+"&objectId=3530994454642760&busiTypeId=3530960544251998" , l.toString());


    }


    }
