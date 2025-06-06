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
import org.apache.pdfbox.io.RandomAccessBuffer;
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
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

public class PDFUtil {

    public static String read(File file){
        String result = null;
        try {
            PDFParser parser = new PDFParser(new RandomAccessFile(file,"rw"));
            parser.parse();
            PDDocument doc = parser.getPDDocument();
            PDFTextStripper stripper = new PDFTextStripper();
            stripper .setSortByPosition(true);
            result = stripper.getText(doc);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }

    public static String read(InputStream in){
        String result = null;
        try {
            PDFParser parser = new PDFParser(new RandomAccessBuffer(in));
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
    public static List<String> reads(File file) throws Exception {
        return reads(file, false);
    }
    /**
     * 按页读取
     * @param file 文件
     * @param position 是否按位置排序(true:上先,false:左先)
     * @return strings
     * @throws Exception Exception
     */
    public static List<String> reads(File file, boolean position) throws Exception {
        // 是否排序
        // 内存中存储的PDF Document
        PDDocument doc = null;
        //输入流
        InputStream is = Files.newInputStream(file.toPath());
        try {
            doc = PDDocument.load(is);
            // 获取页码
            int endPage = doc.getNumberOfPages();
            PDFTextStripper stripper = null;
            stripper = new PDFTextStripper();
            // 设置是否排序
            stripper.setSortByPosition(position);

            List<String> texts=new ArrayList<>();
            for (int i = 0; i < endPage; i++) {
                int page=i+1;
                // 设置起始页
                stripper.setStartPage(page);
                // 设置结束页
                stripper.setEndPage(page);
                texts.add(stripper.getText(doc));
            }
            return texts;
        } finally {
            if (is != null) {
                // 关闭输出流
                is.close();
            }
            if (doc != null) {
                // 关闭PDF Document
                doc.close();
            }
        }
    }
    public static void remove(File file, File target, int ... pages) throws Exception{
        PDDocument doc = null;
        //输入流
        InputStream is = Files.newInputStream(file.toPath());
        try {
            doc = PDDocument.load(is);
            int size = pages.length;
            for(int i=size-1;i>=0;i--){
                int page = pages[i];
                doc.removePage(page);
            }
            doc.save(target);
        } finally {
            is.close();
            if (doc != null) {
                // 关闭PDF Document
                doc.close();
            }
        }
    }
    public static void remove(File file, File target, List<Integer> pages) throws Exception{
        PDDocument doc = null;
        //输入流
        InputStream is = Files.newInputStream(file.toPath());
        try {
            doc = PDDocument.load(is);
            int size = pages.size();
            for(int i=size-1;i>=0;i--){
                int page = pages.get(i);
                doc.removePage(page);
            }
            doc.save(target);
        } finally {
            is.close();
            if (doc != null) {
                // 关闭PDF Document
                doc.close();
            }
        }
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
