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




package org.anyline.tencent.cos.tag;


import org.anyline.tencent.cos.COSUtil;
import org.anyline.util.AnylineConfig;
import org.anyline.util.BasicUtil;
import org.anyline.util.DateUtil;
import org.anyline.web.tag.BaseBodyTag;

import javax.servlet.jsp.JspException;
import javax.servlet.jsp.JspWriter;
import java.util.Map;

public class COSConfig extends BaseBodyTag {

    private static final long serialVersionUID = 1L;
    private boolean debug = false;
    private int expire = 0;
    private String dir = "";

    private String key = AnylineConfig.DEFAULT_INSTANCE_KEY;
    private String var = "al.config.oss.tencent";

    public int doEndTag() throws JspException {
        try{
            COSUtil util = COSUtil.getInstance(key);
            if(BasicUtil.isEmpty(dir)){
                dir = util.getConfig().DIR;
            }
            if(BasicUtil.isNotEmpty(dir)){
                String yyyy = DateUtil.format("yyyy");
                String yy = DateUtil.format("yy");
                String mm = DateUtil.format("MM");
                String dd = DateUtil.format("dd");
                dir = dir.replace("{yyyy}", yyyy);
                dir = dir.replace("{yy}", yy);
                dir = dir.replace("{MM}", mm);
                dir = dir.replace("{mm}", mm);
                dir = dir.replace("{dd}", dd);
                dir = dir.replace("{y}", yyyy);
                dir = dir.replace("{m}", mm);
                dir = dir.replace("{d}", dd);
            }
            Map<String, String> map = util.signature(dir, expire);
            if(BasicUtil.isEmpty(var)){
                var = "al.config.cos['tencent']";
            }
            StringBuffer script = new StringBuffer();
            script.append("<script>\n");
            script.append(var).append(" = {\n");
            script.append("'Bucket':'").append(util.getConfig().BUCKET).append("',\n");
            script.append("'Region':'").append(util.getConfig().ENDPOINT).append("'\n");
            script.append("};\n");
            script.append("al.cos = new COS({\n");
            script.append("    SecretId: '" + map.get("SECRET_ID") + "',\n");
            script.append("    SecretKey: '").append(map.get("SECRET_KEY")).append("',\n");
            script.append("    SecurityToken: '").append(map.get("SESSION_TOKEN")).append("',\n");
            script.append("    StartTime: ").append(System.currentTimeMillis()/1000).append(",\n");
            script.append("    ExpiredTime: ").append(map.get("EXPIRE")).append("\n");//临时密钥过期时间
            script.append(" });");
            script.append("</script>\n");
            JspWriter out = pageContext.getOut();
            out.println(script);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            release();
        }
        return EVAL_PAGE;
    }
    public boolean isDebug() {
        return debug;
    }
    public void setDebug(boolean debug) {
        this.debug = debug;
    }
    public String getKey() {
        return key;
    }

    public int getExpire() {
        return expire;
    }
    public void setExpire(int expire) {
        this.expire = expire;
    }
    public void setKey(String key) {
        this.key = key;
    }
    public String getDir() {
        return dir;
    }
    public void setDir(String dir) {
        this.dir = dir;
    }
    public String getVar() {
        return var;
    }
    public void setVar(String var) {
        this.var = var;
    }

}

