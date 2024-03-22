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


package org.anyline.office.docx.entity;

import org.anyline.metadata.BaseMetadata;
import org.anyline.office.docx.util.DocxUtil;
import org.anyline.util.BasicUtil;
import org.anyline.util.BeanUtil;
import org.anyline.util.HtmlUtil;
import org.dom4j.Element;

import java.io.InputStream;
import java.util.*;

public class Wr extends Welement{
    public Wr(WDocument doc, Element src){
        this.root = doc;
        this.src = src;
    }


    public List<Wt> getWts(){
        List<Wt> wts = new ArrayList<>();
        List<Element> ts = src.elements("t");
        for(Element t:ts){
            Wt wt = new Wt(root, t);
            wts.add(wt);
        }
        return wts;
    }
    /**
     * 字体颜色
     * @param color color
     * @return Wr
     */
    public Wr setColor(String color){
        Element pr = DocxUtil.addElement(src, "rPr");
        DocxUtil.addElement(pr, "color","val", color.replace("#",""));
        return this;
    }

    /**
     * 字体字号
     * @param size 字号
     * @param eastAsia 中文字体
     * @param ascii 英文字体
     * @param hint 默认字体
     * @return Wr
     */
    public Wr setFont(String size, String eastAsia, String ascii, String hint){
        int pt = DocxUtil.fontSize(size);
        Element pr = DocxUtil.addElement(src, "rPr");
        DocxUtil.addElement(pr, "sz","val", pt+"");
        DocxUtil.addElement(pr, "rFonts","eastAsia", eastAsia);
        DocxUtil.addElement(pr, "rFonts","ascii", ascii);
        DocxUtil.addElement(pr, "rFonts","hint", hint);

        return this;
    }

    /**
     * 字号
     * @param size size
     * @return Wr
     */
    public Wr setFontSize(String size){
        int pt = DocxUtil.fontSize(size);
        Element pr = DocxUtil.addElement(src, "rPr");
        DocxUtil.addElement(pr, "sz","val", pt+"");
        return this;
    }

    /**
     * 字体
     * @param font font
     * @return Wr
     */
    public Wr setFontFamily(String font){
        Element pr = DocxUtil.addElement(src, "rPr");
        DocxUtil.addElement(pr, "rFonts","eastAsia", font);
        DocxUtil.addElement(pr, "rFonts","ascii", font);
        DocxUtil.addElement(pr, "rFonts","hAnsi", font);
        DocxUtil.addElement(pr, "rFonts","cs", font);
        DocxUtil.addElement(pr, "rFonts","hint", font);
        return this;
    }

    /**
     * 背景色
     * @param color color
     * @return Wr
     */
    public Wr setBackgroundColor(String color){
        Element pr = DocxUtil.addElement(src, "rPr");
        DocxUtil.addElement(pr, "highlight", "val", color.replace("#",""));
        return this;
    }

    /**
     * 粗体
     * @param bold 是否
     * @return Wr
     */
    public Wr setBold(boolean bold){
        Element pr = DocxUtil.addElement(src, "rPr");
        Element b = pr.element("b");
        if(bold){
            if(null == b){
                pr.addElement("w:b");
            }
        }else{
            if(null != b){
                pr.remove(b);
            }
        }
        return this;
    }

    /**
     * 下划线
     * @param underline 是否
     * @return Wr
     */
    public Wr setUnderline(boolean underline){
        Element pr = DocxUtil.addElement(src, "rPr");
        Element u = pr.element("u");
        if(underline){
            if(null == u){
                DocxUtil.addElement(pr, "u", "val", "single");
            }
        }else{
            if(null != u){
                pr.remove(u);
            }
        }
        return this;
    }

    /**
     * 删除线
     * @param strike 是否
     * @return Wr
     */
    public Wr setStrike(boolean strike){
        Element pr = DocxUtil.addElement(src, "rPr");
        Element s = pr.element("strike");
        if(strike){
            if(null == s){
                pr.addElement("w:strike");
            }
        }else{
            if(null != s){
                pr.remove(s);
            }
        }
        return this;
    }

    /**
     * 垂直对齐方式
     * @param align 上标:superscript 下标:subscript
     * @return Wr
     */
    public Wr setVerticalAlign(String align){
        Element pr = DocxUtil.addElement(src, "rPr");
        DocxUtil.addElement(pr, "vertAlign", "val", align);
        return this;
    }
    public Wr setItalic(boolean italic){
        Element pr = DocxUtil.addElement(src, "rPr");
        DocxUtil.addElement(pr, "i","val",italic+"");
        return this;
    }
    /**
     * 清除样式
     * @return wr
     */
    public Wr removeStyle(){
        Element pr = src.element("rPr");
        if(null != pr){
            src.remove(pr);
        }
        return this;
    }
    /**
     * 清除背景色
     * @return wr
     */
    public Wr removeBackgroundColor(){
        DocxUtil.removeElement(src,"highlight");
        return this;
    }

    /**
     * 清除颜色
     * @return wr
     */
    public Wr removeColor(){
        DocxUtil.removeElement(src,"color");
        return this;
    }
    public Wr replace(String target, String replacement){
        List<Wt> wts = getWts();
        for(Wt wt:wts){
            String text = wt.getText();
            text = text.replace(target, replacement);
            if(this.root.IS_HTML_ESCAPE) {
                text = HtmlUtil.display(text);
            }
            wt.setText(text);
        }
        return this;
    }
    public LinkedHashMap<String, String> styles(){
        LinkedHashMap<String, String> styles = new LinkedHashMap<>();
        Element pr = src.element("rPr");
        if(null != pr){
            //<w:color w:val="0070C0"/>
            Element color = pr.element("color");
            if(null != color){
                String val = color(color.attributeValue("val"));
                if(null != val){
                    styles.put("color", val);
                }
            }
            //<w:highlight w:val="yellow"/>
            Element highlight = pr.element("highlight");
            if(null != highlight){
                String val = color(highlight.attributeValue("val"));
                if(null != val){
                    styles.put("background-color", val);
                }
            }

            //<w:rFonts w:hint="eastAsia"/>
            Element font = pr.element("rFonts");
            if(null != font){
                String hint = font.attributeValue("hint");
                if(null != hint){
                    styles.put("font-family", hint);
                }
            }
            //<w:sz w:val="24"/>
            Element size = pr.element("sz");
            if(null == size){
                size = pr.element("szCs");
            }
            if(null != size){
                int val = BasicUtil.parseInt(size.attributeValue("val"), 0);
                if(val > 0){
                    styles.put("font-size", val+"pt");
                }
            }
            //<w:jc w:val="center"/>
            Element jc = pr.element("jc");
            if(null != jc){
                String val = jc.attributeValue("val");
                if(null != val){
                    styles.put("text-align", val);
                }
            }
        }
        return styles;
    }
    public String html(){
        return html(0);
    }
    public String html(int lvl){
        StringBuilder builder = new StringBuilder();
        StringBuilder body = new StringBuilder();
        Iterator<Element> items = src.elementIterator();
        while (items.hasNext()){
            Element item = items.next();
            String tag = item.getName();
            if(tag.equalsIgnoreCase("t")){
                body.append(item.getText());
            }else if(tag.equalsIgnoreCase("drawing")){
                String img = img(item);
                body.append(img);
            }
        }
        t(builder, lvl);
        builder.append("<span");
        styles(builder);
        builder.append(">");
        builder.append(body);
        builder.append("</span>");
        return builder.toString();
    }
    public Element child(Element parent, String ... tags){
        if(null != tags){
            Element element = parent;
            for(String tag:tags){
                if(null != element){
                    element = element.element(tag);
                }
            }
            return element;
        }
        return null;
    }
    private String img(Element element){

/*<w:drawing>
<wp:inline distT="0" distB="0" distL="0" distR="0">
    <wp:extent cx="2171700" cy="1409700"/>
    <wp:effectExtent l="19050" t="0" r="0" b="0"/>
    <wp:docPr id="1" name="图片 1" descr="D:\a.png"/>
    <wp:cNvGraphicFramePr>
        <a:graphicFrameLocks xmlns:a="http://schemas.openxmlformats.org/drawingml/2006/main" noChangeAspect="1"></a:graphicFrameLocks>
    </wp:cNvGraphicFramePr>
    <a:graphic xmlns:a="http://schemas.openxmlformats.org/drawingml/2006/main">
        <a:graphicData uri="http://schemas.openxmlformats.org/drawingml/2006/picture">
            <pic:pic xmlns:pic="http://schemas.openxmlformats.org/drawingml/2006/picture">
                <pic:nvPicPr>
                    <pic:cNvPr id="0" name="Picture 1" descr="D:\a.png"/>
                    <pic:cNvPicPr>
                        <a:picLocks noChangeAspect="1" noChangeArrowheads="1"/>
                    </pic:cNvPicPr>
                </pic:nvPicPr>
                <pic:blipFill>
                    <a:blip r:embed="rId6"/>
                    <a:srcRect/>
                    <a:stretch>
                        <a:fillRect/>
                    </a:stretch>
                </pic:blipFill>
                <pic:spPr bwMode="auto">
                    <a:xfrm>
                        <a:off x="0" y="0"/>
                        <a:ext cx="2171700" cy="1409700"/>
                    </a:xfrm>
                    <a:prstGeom prst="rect">
                        <a:avLst/>
                    </a:prstGeom>
                    <a:noFill/>
                    <a:ln w="9525">
                        <a:noFill/>
                        <a:miter lim="800000"/>
                        <a:headEnd/>
                        <a:tailEnd/>
                    </a:ln>
                </pic:spPr>
            </pic:pic>
        </a:graphicData>
    </a:graphic>
</wp:inline>
</w:drawing>*/
        StringBuilder builder = new StringBuilder();
        Element pic = child(element, "inline", "graphic", "graphicData", "pic");
        if(null != pic){
            Element blip = child(pic, "blipFill", "blip");
            if(null != blip){
                //资源文件id
                String embed = blip.attributeValue("embed");
                if(null != embed){
                    //	<Relationship Id="rId6" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/image" Target="media/image1.jpeg"/>
                    Element rel = root.rel(embed);
                    if(null != rel){
                        String target = rel.attributeValue("Target"); //media/image1.jpeg
                        InputStream is = root.read("word/"+target);
                        try {
                            byte[] bytes = BeanUtil.stream2bytes(is);
                            String base64 = Base64.getEncoder().encodeToString(bytes);
                            int width = 0;
                            int height = 0;
                            Element ext = child(pic, "spPr", "xfrm", "ext");
                            if(null != ext){
                                width = (int)DocxUtil.emu2px(BasicUtil.parseInt(ext.attributeValue("cx"), 0));
                                height = (int)DocxUtil.emu2px(BasicUtil.parseInt(ext.attributeValue("cy"), 0));
                            }
                            builder.append("<img src='data:image/png;base64,").append(base64).append("'");
                            if(width > 0 && height > 0){
                                builder.append(" style='width:").append(width).append("px; height:").append(height).append("px;'");
                            }
                            builder.append("/>");
                        }catch (Exception e){
                            e.printStackTrace();
                        }finally {
                            try {
                                is.close();
                            }catch (Exception e){
                                e.printStackTrace();
                            }
                        }
                    }
                }
            }
        }
return builder.toString();
    }
}
