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


package org.anyline.pdf.util;

import org.anyline.entity.DataSet;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.io.RandomAccessFile;
import org.apache.pdfbox.pdfparser.PDFParser;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDResources;
import org.apache.pdfbox.pdmodel.graphics.PDXObject;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.pdfbox.text.PDFTextStripper;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class PDFUtil {

    public static String read(File file){
        FileInputStream in = null;
        String result = null;
        try {
            in = new FileInputStream(file);
            PDFParser parser = new PDFParser(new RandomAccessFile(file,"rw"));
            parser.parse();
            PDDocument doc = parser.getPDDocument();
            PDFTextStripper stripper = new PDFTextStripper();
            stripper .setSortByPosition(true);
            result = stripper.getText(doc);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e1) {
                }
            }
        }
        return result;
    }

    /**
     * 读取pdf文件图片<br/>
     * 可以通过 ImageIO.write(BufferedImage, "JPEG", File)写入文件
     * @param file 源文件
     * @return BufferedImage
     */
    public static List<BufferedImage> images(File file){
        List<BufferedImage> list = new ArrayList<>();
        PDDocument doc = null;
        try {
            doc = PDDocument.load(file);
            int pages = doc.getNumberOfPages();
            for (int i = 0; i < pages; i++) {
                PDPage page = doc.getPage(i);
                PDResources resources = page.getResources();
                for (COSName cosName : resources.getXObjectNames()) {
                    PDXObject pdxObject = resources.getXObject(cosName);
                    // 判断是不是图片对象
                    if (pdxObject instanceof PDImageXObject) {
                        BufferedImage image = ((PDImageXObject) pdxObject).getImage();
                        list.add(image);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }finally {
            try {
                doc.close();
            } catch (IOException e) {}
            ;
        }
        return list;
    }
    /**
     * 按页读取文本
     * @return List
     */
    public static List<String> pages(){
        List<String> list = new ArrayList<>();
        return list;
    }
    /**
     * 读取表格
     * @return DataSet
     */
    public static DataSet table(){
        DataSet set = new DataSet();
        return set;
    }

    /**
     * 读取所有表格
     * @return List
     */
    public static List<DataSet> tables(){
        List<DataSet> list = new ArrayList<>();
        return list;
    }
}
