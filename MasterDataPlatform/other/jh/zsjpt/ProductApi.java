package other.jh.zsjpt;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.rkhd.platform.sdk.api.annotations.RequestMethod;
import com.rkhd.platform.sdk.api.annotations.RestApi;
import com.rkhd.platform.sdk.api.annotations.RestBeanParam;
import com.rkhd.platform.sdk.api.annotations.RestMapping;
import com.rkhd.platform.sdk.data.model.Product;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

@RestApi(baseUrl = "/jh")
public class ProductApi extends  XObjectUtils {
    @RestMapping(value = "/Product_API", method = RequestMethod.POST)
    public String DDFLCD(@RestBeanParam(name = "json") JSONObject json) {
        String sqlcp = "select id,cpbm__c from product where cpbm__c in ";
        logger.info("开始执行物料同步接口,入参为："+json.toString());
        ArrayList<Product> xz = new ArrayList<>();
        ArrayList<Product> gx = new ArrayList<>();
        ArrayList<Product> sc = new ArrayList<>();
        HashMap<String, Product> XTID_ProductHashMap = new HashMap<>();

        HashSet<String> idset = new HashSet<>();
        StringBuilder ids = new StringBuilder();

        JSONArray data = json.getJSONArray("data");
        if(data!=null && !data.isEmpty()){
             for (int i = 0; i < data.size(); i++) {
                JSONObject datum1 = (JSONObject) data.get(i);
                //物料编码
                String itemcode = datum1.getString("itemcode");
                //将存货主键拼接起来查询
                if (!idset.contains(itemcode)) {
                    idset.add(itemcode);
                    ids.append("'").append(itemcode).append("'");
                    ids.append(",");
                }
                if (ids.length() > 400 || i == (data.size() - 1)) {//批量查询系统存在的出库单
                    ids.setLength(ids.length() - 1);
                    List<Product> cps = Query(new Product(), sqlcp + "(" + ids + ")", "产品");
                    //查询完放入map
                    for (Product cp : cps) {
                        if (!XTID_ProductHashMap.containsKey(cp.getCpbm__c())) {
                            XTID_ProductHashMap.put(cp.getCpbm__c(), cp);
                        }
                    }
                    ids.setLength(0);
                }


            }
            for (Object datum : data) {
                JSONObject datum1 = (JSONObject) datum;
                //产品名称
                String productName = datum1.getString("productname");
                //物料编码
                String itemcode = datum1.getString("itemcode");
                //单位
                String unit = datum1.getString("unit");
                //同步类型
                Integer type = datum1.getInteger("type");
                //工厂描述
                String factorydescription = datum1.getString("factorydescription");
                //产品规格
                String fscProductSpec = datum1.getString("fscProductSpec");
                Product product = new Product();
                product.setProductName(productName);
                product.setCpbm__c(itemcode);
                product.setUnit(unit);
                product.setParentId(3527795314835569L);
                product.setEntityType(11010000400001L);
                product.setFscProductSpec(fscProductSpec);
                product.setGcms__c(factorydescription);

                if(type == 0){
                    //新增/更新物料
                    if(XTID_ProductHashMap.get(itemcode)!=null){
                        //更新
                        product.setId(XTID_ProductHashMap.get(itemcode).getId());
                        gx.add(product);
                    }else {
                        //新增
                        xz.add(product);
                    }

                } else{
                    //删除物料
                    if(XTID_ProductHashMap.get(itemcode)!=null){
                        //删除
                        product.setId(XTID_ProductHashMap.get(itemcode).getId());
                        sc.add(product);
                    }
                }

            }
            if(!xz.isEmpty()){
                logger.info(xz.toString());
                List<Long> cpids = InsertXObject(xz, "产品");
                if(cpids.isEmpty()){
                    throw new RuntimeException("新增产品失败，请检查数据");
                }

            }
            if(!gx.isEmpty()){
                Boolean gxcp = UpdateXObjects(gx, "产品");
                if(!gxcp){
                    throw new RuntimeException("更新产品失败，请检查数据");
                }
            }
            if(!sc.isEmpty()){
            }

            JSONObject result = new JSONObject();
            result.put("Allcount",data.size());
            result.put("AddCount",xz.size());
            result.put("UpdateCount",gx.size());
            result.put("DelCount",sc.size());


            return setResult(200,"success",result);

        }else {
            return setResult(-100,"参数为空","");
        }

    }
}
