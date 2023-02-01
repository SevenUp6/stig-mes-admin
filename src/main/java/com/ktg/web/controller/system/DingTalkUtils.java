package com.ktg.web.controller.system;

import com.alibaba.fastjson.JSON;
import com.aliyun.teaopenapi.models.Config;
import com.dingtalk.api.DefaultDingTalkClient;
import com.dingtalk.api.DingTalkClient;
import com.dingtalk.api.request.*;
import com.dingtalk.api.response.*;
import com.ktg.web.controller.common.JsonUtil;
import com.taobao.api.ApiException;
import com.taobao.api.FileItem;
import org.apache.commons.lang.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class DingTalkUtils {
    private static String appId = "";
    private static String appSecret = "";
    //微应用的配置
    public static String agentId = "";
    private static String appKey = "dingyhlk4qgay8m8ujqt";
    private static String appSecretWei = "pAT7ex15r6uthKFh_-d0vmSIUzmYxyecf59uoBlTj2XCfOItVyil7D4hcjCSmQkV";

    public static OapiSnsGetuserinfoBycodeResponse getUserInfoByCode(String code) throws ApiException {
        DefaultDingTalkClient client = new DefaultDingTalkClient("https://oapi.dingtalk.com/sns/getuserinfo_bycode");
        OapiSnsGetuserinfoBycodeRequest req = new OapiSnsGetuserinfoBycodeRequest();
        req.setTmpAuthCode(code);
        try {
            return client.execute(req, appId, appSecret);
        } catch (ApiException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 根据unionid获取用户Id
     *
     * @param accessToken
     * @param unionid
     * @throws ApiException
     */
    public static String getUserIdByUnionid(String accessToken, String unionid) throws ApiException {
        DingTalkClient client = new DefaultDingTalkClient("https://oapi.dingtalk.com/user/getUseridByUnionid");
        OapiUserGetbyunionidRequest request = new OapiUserGetbyunionidRequest();
        request.setHttpMethod("GET");
        request.setUnionid(unionid);
        OapiUserGetbyunionidResponse response = client.execute(request, accessToken);
        if (0 != response.getErrcode()) {
            throw new RuntimeException(response.getErrmsg());
        }
        String body = response.getBody();
        Map<String, Object> map = com.ktg.web.controller.common.JsonUtil.parseJSONstr2Map(body);

        return map.get("userid").toString();
    }

    /**
     * 获取微应用Token，获取用户专用
     *
     * @return
     * @throws ApiException
     */
    public static String getUserToken() throws Exception {
// String dingTalkToken = RedisUtils.getString("dingTalkToken");
// if (StringUtils.isNotBlank(dingTalkToken)){
// return dingTalkToken;
// }
        DefaultDingTalkClient client = new DefaultDingTalkClient("https://oapi.dingtalk.com/gettoken");
        OapiGettokenRequest request = new OapiGettokenRequest();
        request.setAppkey(appKey);
        request.setAppsecret(appSecretWei);
        request.setHttpMethod("GET");
        OapiGettokenResponse response = client.execute(request);
        if (0 != response.getErrcode()) {
            throw new RuntimeException(response.getErrmsg());
        }
        String accessToken = response.getAccessToken();
// RedisUtils.set("dingTalkToken",accessToken,7200);
        return accessToken;
    }

    /**
     * @return 根据token和请求授权码获取用户id
     * @throws ApiException
     */
    public static String getUerIdByAccessTokenAndCode(String accessToken, String requestAuthCode) throws ApiException {
        DingTalkClient client = new DefaultDingTalkClient("https://oapi.dingtalk.com/user/getuserinfo");
        OapiUserGetuserinfoRequest request = new OapiUserGetuserinfoRequest();
        request.setCode(requestAuthCode);
        request.setHttpMethod("GET");
        OapiUserGetuserinfoResponse response = client.execute(request, accessToken);
        if (0 != response.getErrcode()) {
            throw new RuntimeException(response.getErrmsg());
        }
        String userId = response.getUserid();
        return userId;
    }

    /**
     * 获取用户详情
     *
     * @return
     * @throws ApiException
     */
    public static OapiUserGetResponse getUerDetailsByAccessTokenAndUserId(String accessToken, String userId) throws ApiException {
        DingTalkClient client = new DefaultDingTalkClient("https://oapi.dingtalk.com/user/get");
        OapiUserGetRequest request = new OapiUserGetRequest();
        request.setUserid(userId);
        request.setHttpMethod("GET");
        return client.execute(request, accessToken);
    }

    /**
     * 获取用户详情
     *
     * @return
     * @throws ApiException
     */
    public static OapiUserGetResponse getUerDetailsByCode(String requestAuthCode) throws Exception {
//1、获取accessToken
        String accessToken = getUserToken();
//2、根据token和requestAuthCode得userId
        String userId = getUerIdByAccessTokenAndCode(accessToken, requestAuthCode);
//3、根据token和userId获取用户详情
        OapiUserGetResponse response = getUerDetailsByAccessTokenAndUserId(accessToken, userId);
        if (0 != response.getErrcode()) {
            throw new RuntimeException(response.getErrmsg());
        }
        return response;
    }

    /**
     * 获取部门列表
     *
     * @param accessToken
     * @param departmentId
     * @return
     * @throws ApiException
     */
    public static OapiDepartmentListResponse getDepartmentList(String accessToken, String departmentId) throws ApiException {
        if (StringUtils.isBlank(departmentId)) {
            departmentId = "1";//顶级部门为1
        }
        DingTalkClient client = new DefaultDingTalkClient("https://oapi.dingtalk.com/department/list");
        OapiDepartmentListRequest request = new OapiDepartmentListRequest();
        request.setId(departmentId);
        request.setFetchChild(false);
        request.setHttpMethod("GET");
        OapiDepartmentListResponse response = client.execute(request, accessToken);
        if (0 != response.getErrcode()) {
            throw new RuntimeException(response.getErrmsg());
        }
        return response;
    }

    /**
     * 根据部门Id获取用户列表
     *
     * @param accessToken
     * @param departmentId
     * @return
     * @throws ApiException
     */
    public static OapiUserListbypageResponse getUserList(String accessToken, String departmentId) throws ApiException {
        if (StringUtils.isBlank(departmentId)) {
            departmentId = "1";//顶级部门为1
        }
        DingTalkClient client = new DefaultDingTalkClient("https://oapi.dingtalk.com/user/listbypage");
        OapiUserListbypageRequest request = new OapiUserListbypageRequest();
        request.setDepartmentId(Long.valueOf(departmentId));
        request.setOrder("entry_desc");
        request.setOffset(0L);
        request.setSize(100L);
        request.setHttpMethod("GET");
        OapiUserListbypageResponse response = client.execute(request, accessToken);
        if (0 != response.getErrcode()) {
            throw new RuntimeException(response.getErrmsg());
        }
        return response;
    }

    /**
     * 获取部门详情
     *
     * @param accessToken
     * @param departmentId
     * @return
     * @throws ApiException
     */
    public static OapiDepartmentGetResponse getDepartmentInfo(String accessToken, String departmentId) throws ApiException {
        if (StringUtils.isBlank(departmentId)) {
            departmentId = "1";
        }
        DingTalkClient client = new DefaultDingTalkClient("https://oapi.dingtalk.com/department/get");
        OapiDepartmentGetRequest request = new OapiDepartmentGetRequest();
        request.setId("2");
        request.setHttpMethod("GET");
        OapiDepartmentGetResponse response = client.execute(request, accessToken);
        if (0 != response.getErrcode()) {
            throw new RuntimeException(response.getErrmsg());
        }
        return response;
    }

    public static OapiUserGetResponse getUserByUnionid(String unionid) throws Exception {
        String token = getUserToken();
        String userId = getUserIdByUnionid(token, unionid);
        OapiUserGetResponse response = getUserByUserId(token, userId);
        return response;
    }

    public static OapiUserGetResponse getUserByUserId(String accessToken, String userId) throws ApiException {
        DingTalkClient client = new DefaultDingTalkClient("https://oapi.dingtalk.com/user/get");
        OapiUserGetRequest request = new OapiUserGetRequest();
        request.setUserid(userId);
        request.setHttpMethod("GET");
        OapiUserGetResponse response = client.execute(request, accessToken);
        if (0 != response.getErrcode()) {
            throw new RuntimeException(response.getErrmsg());
        }
        return response;
    }

    public static Long sendMessage(String unionid, String content) throws Exception {
        if (StringUtils.isEmpty(unionid)) {
            throw new RuntimeException("用户列表不能为空");
        }
        String token = getUserToken();
        String userId = getUserIdByUnionid(token, unionid);
        DingTalkClient client = new DefaultDingTalkClient("https://oapi.dingtalk.com/topapi/message/corpconversation/asyncsend_v2");
        OapiMessageCorpconversationAsyncsendV2Request request = new OapiMessageCorpconversationAsyncsendV2Request();
        request.setUseridList(userId);
        request.setAgentId(Long.valueOf(agentId));
        request.setToAllUser(false);
        OapiMessageCorpconversationAsyncsendV2Request.Msg msg = new OapiMessageCorpconversationAsyncsendV2Request.Msg();
        msg.setMsgtype("text");
        msg.setText(new OapiMessageCorpconversationAsyncsendV2Request.Text());
        msg.getText().setContent(content);
        request.setMsg(msg);
        OapiMessageCorpconversationAsyncsendV2Response response = client.execute(request, token);
        if (0 != response.getErrcode()) {
            throw new RuntimeException(response.getErrmsg());
        }
        return response.getTaskId();
    }


    public static Long sendLinkMessage(String unionid, String title, String url) throws Exception {
        if (StringUtils.isEmpty(unionid)) {
            throw new RuntimeException("用户列表不能为空");
        }
        String token = getUserToken();

        String userId = getUserIdByUnionid(token, unionid);
        DingTalkClient client = new DefaultDingTalkClient("https://oapi.dingtalk.com/topapi/message/corpconversation/asyncsend_v2");
        OapiMessageCorpconversationAsyncsendV2Request request = new OapiMessageCorpconversationAsyncsendV2Request();
        request.setUseridList(userId);
        request.setAgentId(Long.valueOf(agentId));
        request.setToAllUser(false);
        OapiMessageCorpconversationAsyncsendV2Request.Msg msg = new OapiMessageCorpconversationAsyncsendV2Request.Msg();
        msg.setMsgtype("link");
        msg.setLink(new OapiMessageCorpconversationAsyncsendV2Request.Link());
        msg.getLink().setTitle(title);
        msg.getLink().setText(title);
        msg.getLink().setMessageUrl(url);
        msg.getLink().setPicUrl("@lADOADmaWMzazQKA");
        request.setMsg(msg);
        OapiMessageCorpconversationAsyncsendV2Response response = client.execute(request, token);
        if (0 != response.getErrcode()) {
            throw new RuntimeException(response.getErrmsg());
        }
        return response.getTaskId();
    }

    /**
     * 媒体文件上传
     *
     * @param accessToken
     * @return mediaId 媒体文件上传后获取的唯一标识
     * @throws ApiException
     */
    public static String mediaUpload(String accessToken) throws Exception {
        DingTalkClient client = new DefaultDingTalkClient("https://oapi.dingtalk.com/media/upload");
        OapiMediaUploadRequest req = new OapiMediaUploadRequest();
//type image：图片 voice：语音 file：普通文件例如word、excel文件
        req.setType("image");
        req.setMedia(new FileItem("D:\\dev\\钉钉.png"));
        OapiMediaUploadResponse response = client.execute(req, accessToken);
        if (0 != response.getErrcode()) {
            throw new RuntimeException(response.getErrmsg());
        }

        return response.getMediaId();

    }

    /**
     * 获取token
     * @param Appkey
     * @param Appsecret
     * @return
     */
    public String getAccessToken(String Appkey,String Appsecret ){
        DingTalkClient client = new DefaultDingTalkClient("https://oapi.dingtalk.com/gettoken");
        OapiGettokenRequest request = new OapiGettokenRequest();
        request.setAppkey(Appkey);
        request.setAppsecret(Appsecret);
        request.setHttpMethod("GET");
        OapiGettokenResponse response = null;
        try {
            response = client.execute(request);
        } catch (ApiException e) {
            throw new RuntimeException(e);
        }
        System.out.println(response.getBody());
        System.out.println( JsonUtil.parseJSONstr2Map(response.getBody()).get("access_token"));
        return JsonUtil.parseJSONstr2Map(response.getBody()).get("access_token").toString();
    }

    /**
     * 获取部门用户详情
     *
     * @param accessToken
     * @param DeptId
     * @return
     */
    public static List<OapiV2UserListResponse.ListUserResponse> getDeptUser(String accessToken, Long DeptId) {
        DingTalkClient client = new DefaultDingTalkClient("https://oapi.dingtalk.com/topapi/v2/user/list?access_token="+accessToken);
        OapiV2UserListRequest req = new OapiV2UserListRequest();
        req.setDeptId(DeptId);
        req.setCursor(0L);
        req.setSize(10L);
        req.setOrderField("modify_desc");
        req.setContainAccessLimit(false);
        req.setLanguage("zh_CN");
        OapiV2UserListResponse rsp = null;
        try {
            rsp = client.execute(req, "");
        } catch (ApiException e) {
            throw new RuntimeException(e);
        }
        String result=JSON.parseObject(rsp.getBody()).getString("result");
        String userjson=JSON.parseObject(result).getString("list");
        List <OapiV2UserListResponse.ListUserResponse> userList = JSON.parseArray(userjson, OapiV2UserListResponse.ListUserResponse.class);
        System.out.println("stringList2 = " + userjson);
        return userList;
    }

    /**
     * 获取子部门id
     *
     * @param accessToken
     * @return
     */
    public static String[] getDeptId(String accessToken) {
        DingTalkClient client = new DefaultDingTalkClient("https://oapi.dingtalk.com/topapi/v2/department/listsubid");
        OapiV2DepartmentListsubidRequest req = new OapiV2DepartmentListsubidRequest();
        req.setDeptId(1L);
        OapiV2DepartmentListsubidResponse rsp = null;
        try {
            rsp = client.execute(req, accessToken);
        } catch (ApiException e) {
            throw new RuntimeException(e);
        }
        System.out.println(rsp.getBody());
        String deptid=rsp.getBody().split("]}")[0].split(":\\[")[1];
        String [] arr_deptid=deptid.split(",");
        return arr_deptid;
    }

    /**
     * 获取企业下所有员工信息
     */
    public static List getAllUser(String accessToken) {
        //更新用户信息
        String [] arr_deptid= getDeptId(accessToken);
        //钉钉中的用户信息
        List<OapiV2UserListResponse.ListUserResponse> userResponses = new ArrayList<>();
        for(int i=0;i<arr_deptid.length;i++){
            List <OapiV2UserListResponse.ListUserResponse> userlist = getDeptUser(accessToken,Long.valueOf(arr_deptid[i]));
            userResponses.addAll(userlist);
        }

        return userResponses ;
    }


    /**
     * 使用 Token 初始化账号Client
     * @return Client
     * @throws Exception
     */
    public static com.aliyun.dingtalkoauth2_1_0.Client createClient_oauth2_1_0() throws Exception {
        Config config = new Config();
        config.protocol = "https";
        config.regionId = "central";
        return new com.aliyun.dingtalkoauth2_1_0.Client(config);
    }

    /**
     * 使用 Token 初始化账号Client
     * @return Client
     * @throws Exception
     */
    public static com.aliyun.dingtalkrobot_1_0.Client createClient_robot_1_0() throws Exception {
        Config config = new Config();
        config.protocol = "https";
        config.regionId = "central";
        return new com.aliyun.dingtalkrobot_1_0.Client(config);
    }

    public static void main(String[] args) throws Exception {
//        String s = mediaUpload(getToken());
        List s = getAllUser(getUserToken());
        System.out.println("final:::::::::::::"+s);

    }
}