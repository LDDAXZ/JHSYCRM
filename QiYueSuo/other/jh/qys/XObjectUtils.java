package other.jh.qys;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.alibaba.fastjson.util.Base64;
import com.rkhd.platform.sdk.exception.ApiEntityServiceException;
import com.rkhd.platform.sdk.exception.ScriptBusinessException;
import com.rkhd.platform.sdk.exception.XsyHttpException;
import com.rkhd.platform.sdk.http.*;
import com.rkhd.platform.sdk.http.handler.ResponseBodyHandlers;
import com.rkhd.platform.sdk.log.Logger;
import com.rkhd.platform.sdk.log.LoggerFactory;
import com.rkhd.platform.sdk.model.BatchOperateResult;
import com.rkhd.platform.sdk.model.OperateResult;
import com.rkhd.platform.sdk.model.QueryResult;
import com.rkhd.platform.sdk.model.XObject;
import com.rkhd.platform.sdk.service.XObjectService;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

public class XObjectUtils {



    protected final Logger logger = LoggerFactory.getLogger();

    protected RkhdHttpClient rkhdHttpClient;

    protected CommonHttpClient commonHttpClient;

    protected  Map<String,JSONObject> descripionMap = new HashMap<>();

    protected void init() throws ScriptBusinessException {
        getRkhdHttpClient();
        getCommonHttpClient();

    }
    protected void getCommonHttpClient() {
        this.commonHttpClient = CommonHttpClient.instance(35000, 35000);
    }

    protected void getRkhdHttpClient() throws ScriptBusinessException {
        RkhdHttpClient rkhdHttpClient;
        try {
            rkhdHttpClient = RkhdHttpClient.instance();
            this.rkhdHttpClient = rkhdHttpClient;
        } catch (Exception e) {
            throw new ScriptBusinessException("初始化销售易API接口异常");
        }
    }

    protected String setResult(int code, String message, Object obj) {
        JSONObject result = new JSONObject();
        result.put("code", code);
        result.put("message", message);
        result.put("data", obj);
        return result.toString(SerializerFeature.WriteMapNullValue);
    }

    //---------------------通用方法区-------------------------------------------------
    //-----------------------------------------------------------------------------

    /**
     * <H1 style:color="blue">下载签署合同</H1>
     */
    protected RkhdBlob XZHT(String urls,String filename){
        logger.info("开始执行下载文件操作");
        RkhdBlob blob = null;
        try {
            // 使用CommonHttpClient下载图片
            CommonData data = CommonData.newBuilder()
                    .callString(urls)
                    .callType("GET")
                    .build();
            blob = commonHttpClient.downBlob(data);
                // fileName 不能为空;应该包括文件名和扩展名
                blob.setFileName(filename);

            logger.info("blob name is " + blob.getFileName() + "  blob size is " + blob.size());
        } catch (XsyHttpException e) {
            logger.error("下载图片失败："+e.getMessage());
        }

        return blob;
    }



    /**
     * <h1 style:color="blue">单选通过value获取Label</h1>
     */
    protected  String Dan(Map<String,JSONObject> map, String apiKey, Integer value, String objectKey){
        if(map.isEmpty()) {
            JSONArray fields = getDescription(rkhdHttpClient, objectKey);
            for (int i = 0; i < fields.size(); i++) {
                JSONObject object = fields.getJSONObject(i);
                map.put(object.getString("apiKey"), object);
            }
        }
        if(value ==null){
            return null;
        }
        JSONObject jsonObject = map.get(apiKey);
        JSONArray selectionArray = jsonObject.getJSONArray("selectitem");
        for (int i = 0; i < selectionArray.size(); i++) {
            JSONObject object = selectionArray.getJSONObject(i);
            if (Objects.equals(value, object.getInteger("value"))) {
                return object.getString("label");
            }

        }
        return null;
    }
    protected JSONArray getDescription(RkhdHttpClient rkhdHttpClient, String objectKey) {
        try {
            String url = "/rest/data/v2.0/xobjects/" + objectKey + "/description";
            RkhdHttpData rkhdHttpData = RkhdHttpData.newBuilder().callType("GET").callString(url).build();
            JSONObject jsonObject = rkhdHttpClient.execute(rkhdHttpData, ResponseBodyHandlers.ofJSON());
            JSONObject data = jsonObject.getJSONObject("data");
            return data.getJSONArray("fields");
        } catch (Exception e) {
            logger.error("获取客诉描述信息失败", e);
            return new JSONArray();
        }
    }

    /**
     * <H1 style:color="blue">获取XObject对象信息</H1>
     */
    protected <T extends XObject> T getXObject(T t, String name){
        T t1 = null;
        try {
            t1 = XObjectService.instance().get(t,true);
        } catch (ApiEntityServiceException e) {
            logger.error("查询"+name+"失败");
        }
        return t1;
    }

    /**
     * <H1 style:color="blue">更新XObject对象信息</H1>
     */
    protected <T extends XObject> Boolean UpdateXObjects(List<T> TList, String name){
        try {
            BatchOperateResult update = XObjectService.instance().update(TList, false,true);
            if(!update.getSuccess()){
                logger.info(update.getErrorMessage());
                throw  new RuntimeException("更新"+name+"失败");
            }
        } catch (ApiEntityServiceException e) {
            logger.error("更新"+name+"失败");
        }
        return true;
    }
    protected <T extends XObject> Boolean UpdateXObject(T t, String name){
        try {
            OperateResult update = XObjectService.instance().update(t,true);
            if(!update.getSuccess()){
                logger.info(update.getErrorMessage());
                return false;
            }
        } catch (ApiEntityServiceException e) {
            logger.error("更新"+name+"失败");
            return false;
        }
        return true;
    }


    /**
     * <H1 style:color="blue">新增XObject对象信息</H1>
     */
    protected <T extends XObject> List<Long>  InsertXObject(List<T> TList,String name){
        List<Long> idlist = new ArrayList<>();
        try {
            BatchOperateResult insert = XObjectService.instance().insert(TList, false, true);
            if(!insert.getSuccess()){
                logger.error(insert.getErrorMessage());
                throw  new RuntimeException("新增"+name+"失败");
            }
            for (OperateResult operateResult : insert.getOperateResults()) {
                Long dataId = operateResult.getDataId();
                idlist.add(dataId);
            }
            logger.info("新增："+name+"成功"+"申请量："+TList.size()+"成功量："+idlist.size());
        } catch (ApiEntityServiceException e) {
            logger.error("新增"+name+"失败");
        }

        return idlist;
    }

    /**
     * <H1 style:color="blue">新增XObject对象信息</H1>
     */
    protected <T extends XObject> Long   InsertXObject(T t,String name){
       Long id = null;
        try {
            OperateResult insert = XObjectService.instance().insert(t,true);
            id = insert.getDataId();
        } catch (ApiEntityServiceException e) {
            logger.error("新增"+name+"失败");
            throw  new RuntimeException("新增"+name+"失败");
        }
        return  id;
    }
    /**
     * <H1 style:color="blue">删除XObject对象信息</H1>
     */
    protected <T extends XObject> Boolean DeleteXObject(List<T> TList,String name){
        try {
            BatchOperateResult delete = XObjectService.instance().delete(TList,  true);
            if(!delete.getSuccess()){
                logger.info(delete.getErrorMessage());
                throw  new RuntimeException("删除"+name+"失败");
            }
        } catch (ApiEntityServiceException e) {
            logger.error("删除"+name+"失败");
            throw  new RuntimeException("删除"+name+"失败");
        }
        return true;
    }

    /**
     * <H1 style:color="blue">解锁XObject对象信息</H1>
     */
    protected <T extends XObject> Boolean UnlockXObject(T t,String name){
        try {
            OperateResult unlock = XObjectService.instance().unlock(t, true);
            if(!unlock.getSuccess()){
                logger.info(unlock.getErrorMessage());
                return false;
            }
        } catch (ApiEntityServiceException e) {
            logger.error("解锁"+name+"失败");
            return false;
        }
        return true;
    }
    /**
     * <h1 style:color="blue">SQL查询</h1>
     */
    protected  <T extends XObject> List<T> Query(T t , String sql,String name){
        List<T> wls = new ArrayList<>();
        int limitRow = 0;
        List<T> wl = null;
        String sql2;
        try {
            do {
                sql2=sql +" limit "+limitRow+" ,300" ;
                logger.info(sql2);
                QueryResult<T> result = XObjectService.instance().query(sql2,true);
                if (result.getSuccess()) {
                    wl = result.getRecords();
                    wls.addAll(wl);
                    limitRow = limitRow + 300;
                }else {
                    logger.error("查询"+name+"失败. " + result.getErrorMessage());
                }
            } while (wl.size() == 300);
        } catch (Exception e){
            logger.error("查询"+name+"失败. " + e.getMessage());
        }
        logger.info("查询"+name+"成功,数量"+wls.size());
        return wls;
    }

    /**
     * <h1 style:color="blue">字符流转字节</h1>
     */
    public  static byte[] base64Tobytes(String base64) {
        byte[] bytes = null;
        try {
            bytes = Base64.decodeFast(base64.replace("\r\n", "").replace("\n", ""));
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("base64转换文件时报错"+e.getMessage());
        }
        return bytes;
    }
    /**
     * <h1 style:color="blue">文件上传CRM</h1>
     */
    public Long uploadCrm2(RkhdBlob blob) {
        try {

            logger.info("blob name is " + blob.getFileName() + ",blob size is " + blob.size());

            // 利用RkhdHttpClient上传图片到crm
            // 目前仅支持单文件上传
            Map < String,Object > map = new HashMap < >(16);
            map.put("files", blob);
            map.put("isImage", false);
            map.put("needFileId", true);
            RkhdHttpData up = RkhdHttpData.newBuilder().callString("/rest/file/v2.0/file/batch").callType("POST").formData(map).build();
            up.putHeader("Content-Type", "multipart/form-data");
            JSONObject jsonObject = rkhdHttpClient.execute(up, ResponseBodyHandlers.ofJSON());
            logger.info(jsonObject.toJSONString());
            if ( jsonObject.getInteger("code") == 200 ) {
                Long feilID = jsonObject.getJSONArray("result").getJSONObject(0).getLong("fileId");
                return feilID;
            }
            return null;
        } catch(Exception e) {
            logger.error(e.getMessage());
        }
        return null;
    }
    /**
     * <h1 style:color="blue">文件上传CRM图片</h1>
     */
    public Long uploadCrmTp(RkhdBlob blob) {
//        {"msg":"200","ext":[],"result":[{"msg":"OK","code":200,"data":{"fileId":3402141627259552}}],"code":200}
        try {
            // 利用RkhdHttpClient上传图片到crm
            // 目前仅支持单文件上传
            Map < String,Object > map = new HashMap < >(16);
            map.put("files", blob);
            RkhdHttpData up = RkhdHttpData.newBuilder().callString("/rest/file/v2.0/image").callType("POST").formData(map).build();
            up.putHeader("Content-Type", "multipart/form-data");
            JSONObject jsonObject = rkhdHttpClient.execute(up, ResponseBodyHandlers.ofJSON());
            logger.info(jsonObject.toJSONString());
            String msg = jsonObject.getString("msg");
            if ( Objects.equals(msg,"200") ) {
                return jsonObject.getJSONArray("result").getJSONObject(0).getJSONObject("data").getLong("fileId");
            }
            return null;
        } catch(Exception e) {
            logger.error(e.getMessage());
        }
        return null;
    }
    /*
     * 触发自动流程
     * AutoProcessInstances(mx.getId(), rkhdHttpClient, "ORDERKPMX__c", "cf_xW0xeOuMygwuvsqWlTwm47CdZdHscPSrbRHV9WSdDA8viv8");
     */
    protected void AutoProcessInstances(Long id,RkhdHttpClient rkhdHttpClient,String api,String api_key) throws ScriptBusinessException{
        try {
            JSONObject data = new JSONObject();
            data.put("processDefinitionApiKey", api);
            data.put("entityApiKey", api_key);
            data.put("dataId", id);

            JSONObject request = new JSONObject();
            request.put("data", data);

            RkhdHttpData rkhdHttpData = RkhdHttpData.newBuilder()
                    .callType("POST").callString("/rest/data/v2.0/creekflow/autoProcessInstances")
                    .body(request.toJSONString()).build();

            JSONObject result = rkhdHttpClient.execute(rkhdHttpData, ResponseBodyHandlers.ofJSON());
            logger.info("创建批量job结果: " + result.toJSONString());
            if (result.getInteger("code") != 200) {
                throw new ScriptBusinessException("触发发货明细自动流程异常");
            }
        }catch (Exception e) {
            logger.error("执行保存收款计划异常", e);
            throw new ScriptBusinessException(e.getMessage());
        }

    }

    /**
     * <h1 style:color="blue">时间戳转文本日期</h1>
     */
    protected   String date (Long time){
        if(time ==null){
            return "";
        }
        // 创建SimpleDateFormat对象以定义日期格式
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");

        // 将毫秒时间戳转换为Date对象
        Date date = new Date(time);

        // 使用SimpleDateFormat格式化Date对象并打印日期

        return sdf.format(date);
    }
    /**
     * <h1 style:color="blue">文本转日期时间戳</h1>
     */
    protected   Long date (String time){
        Long a = null;
        // 创建SimpleDateFormat对象以定义日期格式
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");

        try {
            Date parse = sdf.parse(time);
            a = parse.getTime();
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }

        return a;
    }
    /**
     * <h1 style:color="blue">文本转日期时间戳</h1>
     */
    protected   Long datetime (String time){
        Long a = null;
        // 创建SimpleDateFormat对象以定义日期格式
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

        try {
            Date parse = sdf.parse(time);
            a = parse.getTime();
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }

        return a;
    }
    protected String getString(String s) {
        if ( s == null || s.equals("null") ) {
            return "";
        }else {
            return s;
        }
    }

}
