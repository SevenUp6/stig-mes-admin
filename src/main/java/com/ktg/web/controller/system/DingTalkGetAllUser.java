package com.ktg.web.controller.system;

import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.dingtalk.api.DefaultDingTalkClient;
import com.dingtalk.api.DingTalkClient;
import com.dingtalk.api.request.OapiDepartmentListRequest;
import com.dingtalk.api.request.OapiUserGetDeptMemberRequest;
import com.dingtalk.api.response.OapiDepartmentListResponse;
import com.dingtalk.api.response.OapiUserGetDeptMemberResponse;
import com.dingtalk.api.response.OapiV2DepartmentListsubResponse;
import com.dingtalk.api.response.OapiV2UserListResponse;
import com.ktg.common.core.domain.entity.SysDept;
import com.ktg.common.core.domain.entity.SysUser;
import com.ktg.common.utils.SecurityUtils;
import com.ktg.common.utils.StringUtils;
import com.ktg.system.service.ISysDeptService;
import com.ktg.system.service.ISysUserService;
import com.ktg.web.controller.common.DeleteMarkEnum;
import com.ktg.web.controller.common.EnabledMarkEnum;
import com.taobao.api.ApiException;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

import static com.ktg.web.controller.system.DingTalkUtils.getDeptUser;
import static com.ktg.web.controller.system.DingTalkUtils.getUserToken;


@AllArgsConstructor
@RestController
@RequestMapping("/dingtalk")
@Api(value = "/dingtalk",tags = "钉钉相关")
public class DingTalkGetAllUser {

    protected static final Logger logger = LoggerFactory.getLogger(DingTalkGetAllUser.class);

//    @Autowired
//    private IXjrBaseDepartmentService iXjrBaseDepartmentService;

    public List<OapiV2DepartmentListsubResponse.DeptBaseResponse> deptBaseResponselist = new ArrayList<>();
    @Autowired
    private ISysUserService userService;

    @Autowired
    private ISysDeptService deptService;

//    @Autowired
//    private IXjrBaseUserRelationService iXjrBaseUserRelationService;
    /**
     * 获取企业下所有员工信息
     */
    @GetMapping("/getalluser")
    @ApiOperation(value="获取企业下所有员工信息")
    public  List getAllUser(String accessToken) {
        //钉钉中的用户信息
        List<OapiV2UserListResponse.ListUserResponse> userResponses = new ArrayList<>();
        List<SysDept> deptResponses = new ArrayList<>();
        //获取部门id
        OapiDepartmentListResponse resp = null;
        //获取token
        try {
            accessToken=getUserToken();
            System.out.println("#############"+accessToken);
            resp = getDeptList("1", true);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        if (!resp.isSuccess()) {
            System.err.println(resp.getMessage());
        } else {
            String finalAccessToken = accessToken;
            resp.getDepartment().forEach(d -> {
                List <OapiV2UserListResponse.ListUserResponse> userlist = getDeptUser(finalAccessToken,Long.valueOf(d.getId()));
                System.out.println(Long.valueOf(d.getId())+"@@@"+userlist.size());
                userResponses.addAll(userlist);
            });
        }
        System.out.println(userResponses.size());

        //获取数据库中的用户信息
//        List<SysUser> dbUsers = userService.list(Wrappers.<SysUser>query().lambda().eq(SysUser::getStatus, EnabledMarkEnum.ENABLED.getCode()).eq(SysUser::getDelFlag, DeleteMarkEnum.NODELETE.getCode()));
        List<SysUser> dbUsers = userService.list(Wrappers.<SysUser>query().lambda().eq(SysUser::getStatus, EnabledMarkEnum.ENABLED.getCode()).eq(SysUser::getDelFlag, DeleteMarkEnum.NODELETE.getCode()));

        Map<String, com.ktg.common.core.domain.entity.SysUser> dbUsersMap = dbUsers.stream().collect(Collectors.toMap(v -> v.getUserName(), v -> v));
        Map<String, OapiV2UserListResponse.ListUserResponse> userResponsesMap =new HashMap<>();
        if(userResponses.size()>0) {
            userResponsesMap=userResponses.stream().collect(Collectors.toMap(v -> v.getJobNumber(), v -> v,(entity1, entity2) -> entity1));
        }

//        ArrayList<XjrBaseUserRelation> addRelations = new ArrayList<>();
        //更新用户
        ArrayList<com.ktg.common.core.domain.entity.SysUser> updateBaseUsers = new ArrayList<>();
        Set<String> distinctIds = CollUtil.intersectionDistinct(dbUsersMap.keySet(), userResponsesMap.keySet());
        if(distinctIds.size()>0) {
            for (String distinctId : distinctIds) {
                com.ktg.common.core.domain.entity.SysUser baseUser = dbUsersMap.get(distinctId);
                OapiV2UserListResponse.ListUserResponse userResponse = userResponsesMap.get(distinctId);
                baseUser.setNickName(userResponse.getName());
                baseUser.setUserName(userResponse.getJobNumber()==null?userResponse.getJobNumber():userResponse.getMobile());
                baseUser.setAvatar(userResponse.getAvatar());
                baseUser.setEmail(userResponse.getEmail());
                baseUser.setPhonenumber(userResponse.getMobile());
                baseUser.setStatus(EnabledMarkEnum.ENABLED.getCode()+"");
                baseUser.setDelFlag(DeleteMarkEnum.NODELETE.getCode()+"");
                baseUser.setCreateTime(Date.from( LocalDateTime.now().atZone( ZoneId.systemDefault()).toInstant()));
                baseUser.setDingtalkId(userResponse.getUserid());
                baseUser.setDeptId(userResponse.getDeptIdList().get(0));

//                List<XjrBaseUserRelation> list = iXjrBaseUserRelationService.list(Wrappers.<XjrBaseUserRelation>query().lambda().eq(XjrBaseUserRelation::getCategory, 3).eq(XjrBaseUserRelation::getUserId, baseUser.getUserId()).eq(XjrBaseUserRelation::getObjectId, userResponse.getDeptIdList().get(0)));
//                if(list==null||list.size()<=0) {
//                    XjrBaseUserRelation xjrBaseUserRelation = new XjrBaseUserRelation();
//                    xjrBaseUserRelation.setUserId(baseUser.getUserId()).setObjectId( userResponse.getDeptIdList().get(0).toString()).setCategory(3).setCreateDate(LocalDateTimeUtil.now());
//                    addRelations.add(xjrBaseUserRelation);
//                }
            }
        }


        //添加数据库中没有的用户
        List<String> subtractIds2 = CollUtil.subtractToList(userResponsesMap.keySet(), dbUsersMap.keySet());
        if(subtractIds2.size()>0) {
            for (String subtractId : subtractIds2) {
                com.ktg.common.core.domain.entity.SysUser baseUser = new com.ktg.common.core.domain.entity.SysUser();
                OapiV2UserListResponse.ListUserResponse userResponse = userResponsesMap.get(subtractId);
                baseUser.setPassword(SecurityUtils.encryptPassword("000000"));
                baseUser.setNickName(userResponse.getName());
                baseUser.setUserName(userResponse.getJobNumber()==null?userResponse.getJobNumber():userResponse.getMobile());
                baseUser.setAvatar(userResponse.getAvatar());
                baseUser.setEmail(userResponse.getEmail());
                baseUser.setPhonenumber(userResponse.getMobile());
                baseUser.setStatus(EnabledMarkEnum.ENABLED.getCode()+"");
                baseUser.setDelFlag(DeleteMarkEnum.NODELETE.getCode()+"");
                baseUser.setCreateTime(Date.from( LocalDateTime.now().atZone( ZoneId.systemDefault()).toInstant()));
                baseUser.setDingtalkId(userResponse.getUserid());
                baseUser.setDeptId(userResponse.getDeptIdList().get(0));
                userService.save(baseUser);
//                XjrBaseUserRelation xjrBaseUserRelation = new XjrBaseUserRelation();
//                xjrBaseUserRelation.setUserId(baseUser.getUserId());
//                .setObjectId(userResponse.getDeptIdList().get(0).toString());
//                .setCategory(3);
//                .setCreateDate(LocalDateTimeUtil.now());
//                addRelations.add(xjrBaseUserRelation);
            }
        }

        if(updateBaseUsers.size()>0){
            userService.updateBatchById(updateBaseUsers);
        }
//        iXjrBaseUserRelationService.saveBatch(addRelations);


        return userResponses ;
    }

    @GetMapping("/getalluser22")
    public void getAllUser() throws ApiException {
        OapiDepartmentListResponse resp = getDeptList("1", true);
        List  userResponses = new ArrayList<>();
        List<SysDept> deptResponses = new ArrayList<>();
        deptService.remove(
                Wrappers.<SysDept>query().lambda().ne(SysDept::getDeptId, 1)
        );
        if (!resp.isSuccess()) {
            System.err.println(resp.getMessage());
        } else {
            resp.getDepartment().forEach(d -> {
                System.out.println(d.getId() + "->" + d.getName() + "##" + d.getParentid());
                SysDept sysdept=new SysDept();
                sysdept.setDeptId(d.getId());
                sysdept.setParentId(d.getParentid());
                sysdept.setDeptName(d.getName());
                sysdept.setCreateTime(Date.from( LocalDateTime.now().atZone( ZoneId.systemDefault()).toInstant()));
                deptResponses.add(sysdept);
            });
        }
                deptService.saveBatch(deptResponses);
    }
    /**
     * 根据父节点遍历所有的子节点
     * @from sanshu.cn
     * @param
     * @return
     * @throws ApiException
     */
    public OapiDepartmentListResponse getDeptList(String id,boolean  fetchChild) throws ApiException{
        DingTalkClient client = new DefaultDingTalkClient("https://oapi.dingtalk.com/department/list");
        OapiDepartmentListRequest request = new OapiDepartmentListRequest();
        request.setId(id);
        request.setHttpMethod("GET");
        request.setFetchChild(fetchChild);
        OapiDepartmentListResponse response = null;
        try {
            response = client.execute(request, getUserToken());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        System.out.println(response.getBody());
        return response;

    }

    /**
     * 根据获取用户编号
     * @from sanshu.cn
     * @param deptId 部门编号
     * @return
     * @throws ApiException
     */
    public List<String> getUsersIdByDeptId(String deptId) throws ApiException {
        DingTalkClient client = new DefaultDingTalkClient("https://oapi.dingtalk.com/user/getDeptMember");
        OapiUserGetDeptMemberRequest req = new OapiUserGetDeptMemberRequest();
        req.setHttpMethod("GET");
        req.setDeptId(deptId);
        OapiUserGetDeptMemberResponse rsp = null;
        try {
            rsp = client.execute(req,getUserToken());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        System.out.println(rsp.getBody());
        return rsp.getUserIds();
    }

//    @GetMapping("/getdept")
//    @ApiOperation(value="获取企业下所有部门")
//    public  OapiDepartmentListResponse getDept(String accessToken) {
//        //获取部门id
//        OapiDepartmentListResponse resp = null;
//        //更新部门信息
//        List<XjrBaseDepartment> baseDepartments = iXjrBaseDepartmentService.list(Wrappers.<XjrBaseDepartment>query().lambda().eq(XjrBaseDepartment::getCompanyId, "21db6a365d2f1254dac92f9ec314deaa").eq(XjrBaseDepartment::getDeleteMark, DeleteMarkEnum.NODELETE.getCode()).eq(XjrBaseDepartment::getEnabledMark, EnabledMarkEnum.ENABLED.getCode()));
//        ArrayList<XjrBaseDepartment> updateDeparts = new ArrayList<>();
//        ArrayList<XjrBaseDepartment> addDeparts = new ArrayList<>();
//
//        //获取token
//        try {
//            accessToken = getUserToken();
//            System.out.println("#############" + accessToken);
//            resp = getDeptList("1", true);
//        } catch (Exception e) {
//            throw new RuntimeException(e);
//        }
//        if (!resp.isSuccess()) {
//            System.err.println(resp.getMessage());
//        } else {
//            resp.getDepartment().forEach(d -> {
//                for (int i = 0; i < baseDepartments.size(); i++) {
//                    XjrBaseDepartment baseDepartment = baseDepartments.get(i);
//                    if (baseDepartment.getDepartmentId().equals(d.getId().toString())) {
//                        baseDepartment.setFullName(d.getName()).setParentId(d.getParentid().equals(1L) ? "" : d.getParentid().toString());
//                        updateDeparts.add(baseDepartment);
//                    }
//                }
//                XjrBaseDepartment xjrBaseDepartment = new XjrBaseDepartment();
//                xjrBaseDepartment.setDepartmentId(d.getId().toString()).setCompanyId("21db6a365d2f1254dac92f9ec314deaa").setDingTalkId(d.getId().toString())
//                        .setFullName(d.getName()).setParentId(d.getParentid().equals(1L) ? "" : d.getParentid().toString())
//                        .setDeleteMark(DeleteMarkEnum.NODELETE.getCode()).setEnabledMark(EnabledMarkEnum.ENABLED.getCode());
//                addDeparts.add(xjrBaseDepartment);
//            });
//
//            iXjrBaseDepartmentService.saveBatch(addDeparts);
//            iXjrBaseDepartmentService.updateBatchById(updateDeparts);
//        }
//        return resp ;
//    }

    /**
     * 根据父节点遍历所有的子节点
     * @from sanshu.cn
     * @param
     * @return
     * @throws ApiException
     */
    public List<OapiV2DepartmentListsubResponse.DeptBaseResponse> getDeptList2(String id,boolean  fetchChild) throws ApiException{
        DingTalkClient client = new DefaultDingTalkClient("https://oapi.dingtalk.com/department/list");
        OapiDepartmentListRequest request = new OapiDepartmentListRequest();
        request.setId(id);
        request.setHttpMethod("GET");
        request.setFetchChild(fetchChild);
        OapiDepartmentListResponse response = null;
        try {
            response = client.execute(request, getUserToken());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        System.out.println(response.getBody());
        return (List<OapiV2DepartmentListsubResponse.DeptBaseResponse>) response;

    }


    public List<OapiV2DepartmentListsubResponse.DeptBaseResponse> getDeptBaseResponselist() {
        return deptBaseResponselist;
    }

    private List<SysDept> setAncestors(List<SysDept> list, SysDept t)
    {
        List<SysDept> tlist = new ArrayList<SysDept>();
        Iterator<SysDept> it = list.iterator();
        while (it.hasNext())
        {
            SysDept n = (SysDept) it.next();
            if (StringUtils.isNotNull(n.getParentId()) && n.getParentId().longValue() == t.getDeptId().longValue())
            {
                n.setAncestors(t.getAncestors()+","+t.getDeptId());
            }
        }
        return tlist;
    }

}
