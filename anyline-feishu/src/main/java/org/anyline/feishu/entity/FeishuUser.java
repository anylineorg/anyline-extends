package org.anyline.feishu.entity;

public class FeishuUser {
    private String name	;//	用户姓名
    private String enName	;//	用户英文名称
    private String avatarUrl	;//	用户头像
    private String avatarThumb	;//	用户头像 72x72
    private String avatarMiddle	;//	用户头像 240x240
    private String avatarBig	;//	用户头像 640x640
    private String openid	;//	用户在应用内的唯一标识
    private String unionid	;//	用户对ISV的唯一标识，对于同一个ISV，用户在其名下所有应用的union_id相同
    private String email	;//	用户邮箱 
    private String enterpriseEmail	;//	企业邮箱，请先确保已在管理后台启用飞书邮箱服务
    private String id	;//	用户 user_id
    private String mobile	;//	用户手机号 
    private String tenantKey	;//	当前企业标识
    private String employeeNo	;//	用户工号

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
