package other.jh.qys;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.rkhd.platform.sdk.api.annotations.RequestMethod;
import com.rkhd.platform.sdk.api.annotations.RestApi;
import com.rkhd.platform.sdk.api.annotations.RestMapping;
import com.rkhd.platform.sdk.api.annotations.RestQueryParam;
import com.rkhd.platform.sdk.data.model.Order;
import com.rkhd.platform.sdk.exception.ScriptBusinessException;
import com.rkhd.platform.sdk.http.RkhdBlob;
import org.apache.commons.lang.StringUtils;

@RestApi(baseUrl = "/jh")
public class FQQS_API extends XObjectUtils {
    //（1）发起签署
    @RestMapping(value = "/FQQS_API", method = RequestMethod.GET)
    public String FQQS(
            @RestQueryParam(name = "id")Long id) throws ScriptBusinessException{
       logger.info("开始执行合同发起电子签章接口：");
       init();
        //查询合同
        Order order = new Order();
        order.setId(id);
        //不是光纤合同
        if(!gx.contains(order.getEntityType())){
            if(order.getZZM__c()!=null && order.getZZM__c().equals("长飞光坊")){
                tenantName="长飞光坊（武汉）科技有限公司";

            }else{
                tenantName="南京光坊科技有限公司";
            }
        }else{
            tenantName="长飞光坊（武汉）科技有限公司";
        }

        //合同流程id不为空且状态为签署中
        if(StringUtils.isNotBlank(order.getContractld__c())&& order.getStatus__c()==3){
            String status = Status3(order.getContractld__c());
            if(status!= null && status.equals("COMPLETE")){
                order.setStatus__c(1);
                UpdateOrder(order);
                return setResult(500,"","已签署完毕，操作失效，状态已刷新");
            }else if ( status!= null && status.equals("SIGNING") ) {//签署中
                String url = HQCSYM(order.getContractld__c(), user.getPhone(), "COMPANY", tenantName);
                if(StringUtils.isNotBlank(url)){
                    //反写url地址
//                    order.setUrl__c(url);
//                    UpdateOrder(order);
                    //更新合同
                    return setResult(200,url,"已在签署中，是否打开签署页面");
                }else{
                    return setResult(500,"","重新获取签署页面失败");
                }
            }

        }
        Boolean GZ = false;
        //判断当前节点是否在光坊盖章
        JSONArray spls = SPLS(order.getId());
        for (int i = 0; i < spls.size(); i++) {
            JSONObject sp = (JSONObject)spls.get(i);
            if(sp.getString("usertaskInstName").equals("光坊盖章")&&sp.getInteger("usertaskInstStatus")==1){
                GZ=true;
            }
        }
        if(!GZ){
            return setResult(500,"","未在盖章节点，不支持发起");
        }
        RkhdBlob xzmb = new RkhdBlob();
        //判断是否是客户提供模板
        if(order.getYYWJ__c()==null||order.getYYWJ__c().size()==0){
            //  Long entityType = order.getEntityType();
            //下载合同打印模板
            Long entityType = order.getEntityType();
            String zzm__c = order.getZZM__c();
            String mbid = null;
            if(zzm__c!=null && zzm__c.equals("长飞光坊")){
                mbid = DYMBIDWH.get(String.valueOf(entityType));
            }else{
                mbid = DYMBID.get(String.valueOf(entityType));
            }
            xzmb = XZMB(order.getApiKey(), String.valueOf(order.getId()), mbid);

        }else{
            Long fileid = (Long) order.getYYWJ__c().get(0).get("id");
            xzmb = XZWJ(String.valueOf(fileid));
            String fileName = xzmb.getFileName();
            int lastIndex = fileName.lastIndexOf(".");
            fileType = fileName.substring(lastIndex + 1);

        }
        String filename = order.getPo()+order.getKHMC__c();
        //创建签署文档
        String documentId = CreatMB(filename, fileType, xzmb);
        //发起签署流程
        String contractId  = FQQS(documentId, order.getPo(),filename,htyyid__c,user.getName(),user.getPhone(),tenantName);
        //获取签署页面
        String url = HQCSYM(contractId, user.getPhone(), "COMPANY", tenantName);
        if(StringUtils.isNotBlank(url)){
            //反写contractId和url地址
            order.setContractld__c(contractId);
//         order.setUrl__c(url);
            //更改状态为签署中
            order.setStatus__c(3);
            order.setQswj__c(null);
            UpdateOrder(order);
            //更新合同
            return setResult(200,url,"发起签署成功，是否打开签署页面");
        }else{
            return setResult(500,"","发起签署失败");
        }

    }


}
