/*
 * Copyright 2006-2023 www.anyline.org
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */



package org.anyline.feishu.entity;

import java.util.Date;
import java.util.List;

public class User {
    private String name                 ;//	用户姓名
    private String enName	            ;//	用户英文名称
    private String avatarUrl	        ;//	用户头像
    private String avatarThumb	        ;//	用户头像 72x72
    private String avatarMiddle	        ;//	用户头像 240x240
    private String avatarBig	        ;//	用户头像 640x640
    private String openid	            ;//	用户在应用内的唯一标识
    private String unionid	            ;//	用户对ISV的唯一标识，对于同一个ISV，用户在其名下所有应用的union_id相同
    private String email	            ;//	用户邮箱
    private String enterpriseEmail	    ;//	企业邮箱，请先确保已在管理后台启用飞书邮箱服务
    private String id	                ;//	用户 user_id
    private String mobile	            ;//	用户手机号
    private String tenantKey	        ;//	当前企业标识
    private String employeeNo	        ;//	用户工号
    private String workStation          ;// 工位
    private String jobTitle             ;// 职务
    private String nickname             ;// 别名
    private String gender               ;// 性别
    private List<String> departmentIds  ;//部门
    private String leaderId             ;//直接上级
    private Date joinTime               ; //入职时间
    private String joinYmd              ; //入职日期
    private int frozenStatus = -1       ; //冻结 0:否 1:是 -1:未知
    private int activateStatus = -1     ; //激活 0:否 1:是 -1:未知
    private int resignStatus = -1       ; //离职 0:否 1:是 -1:未知

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public List<String> getDepartmentIds() {
        return departmentIds;
    }

    public void setDepartmentIds(List<String> departmentIds) {
        this.departmentIds = departmentIds;
    }

    public String getLeaderId() {
        return leaderId;
    }

    public void setLeaderId(String leaderId) {
        this.leaderId = leaderId;
    }

    public Date getJoinTime() {
        return joinTime;
    }

    public void setJoinTime(Date joinTime) {
        this.joinTime = joinTime;
    }

    public String getJoinYmd() {
        return joinYmd;
    }

    public void setJoinYmd(String joinYmd) {
        this.joinYmd = joinYmd;
    }

    public int getFrozenStatus() {
        return frozenStatus;
    }

    public void setFrozenStatus(int frozenStatus) {
        this.frozenStatus = frozenStatus;
    }
    public void setFrozenStatus(Boolean frozenStatus) {
        if(null != frozenStatus){
            if(frozenStatus){
                this.frozenStatus = 1;
            }else{
                this.frozenStatus = 0;
            }
        }
    }

    public int getActivateStatus() {
        return activateStatus;
    }

    public void setActivateStatus(int activateStatus) {
        this.activateStatus = activateStatus;
    }

    public void setActivateStatus(Boolean activateStatus) {
        if(null != activateStatus){
            if(activateStatus){
                this.activateStatus = 1;
            }else{
                this.activateStatus = 0;
            }
        }
    }

    public String getWorkStation() {
        return workStation;
    }

    public void setWorkStation(String workStation) {
        this.workStation = workStation;
    }

    public String getJobTitle() {
        return jobTitle;
    }

    public void setJobTitle(String jobTitle) {
        this.jobTitle = jobTitle;
    }

    public int getResignStatus() {
        return resignStatus;
    }

    public void setResignStatus(int resignStatus) {
        this.resignStatus = resignStatus;
    }


    public void setResignStatus(Boolean resignStatus) {
        if(null != resignStatus){
            if(resignStatus){
                this.resignStatus = 1;
            }else{
                this.resignStatus = 0;
            }
        }
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEnName() {
        return enName;
    }

    public void setEnName(String enName) {
        this.enName = enName;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public void setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
    }

    public String getAvatarThumb() {
        return avatarThumb;
    }

    public void setAvatarThumb(String avatarThumb) {
        this.avatarThumb = avatarThumb;
    }

    public String getAvatarMiddle() {
        return avatarMiddle;
    }

    public void setAvatarMiddle(String avatarMiddle) {
        this.avatarMiddle = avatarMiddle;
    }

    public String getAvatarBig() {
        return avatarBig;
    }

    public void setAvatarBig(String avatarBig) {
        this.avatarBig = avatarBig;
    }

    public String getOpenid() {
        return openid;
    }

    public void setOpenid(String openid) {
        this.openid = openid;
    }

    public String getUnionid() {
        return unionid;
    }

    public void setUnionid(String unionid) {
        this.unionid = unionid;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getEnterpriseEmail() {
        return enterpriseEmail;
    }

    public void setEnterpriseEmail(String enterpriseEmail) {
        this.enterpriseEmail = enterpriseEmail;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getMobile() {
        return mobile;
    }

    public void setMobile(String mobile) {
        this.mobile = mobile;
    }

    public String getTenantKey() {
        return tenantKey;
    }

    public void setTenantKey(String tenantKey) {
        this.tenantKey = tenantKey;
    }

    public String getEmployeeNo() {
        return employeeNo;
    }

    public void setEmployeeNo(String employeeNo) {
        this.employeeNo = employeeNo;
    }
}
