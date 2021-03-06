/*
 *  ====================================================================
 *    Licensed to the Apache Software Foundation (ASF) under one or more
 *    contributor license agreements.  See the NOTICE file distributed with
 *    this work for additional information regarding copyright ownership.
 *    The ASF licenses this file to You under the Apache License, Version 2.0
 *    (the "License"); you may not use this file except in compliance with
 *    the License.  You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 * ====================================================================
 */

package org.apache.poi.xssf.streaming;

import org.apache.poi.ss.usermodel.BaseTestXWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.util.CellReference;
import org.apache.poi.xssf.SXSSFITestDataProvider;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.After;
import org.junit.Ignore;
import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.*;

public final class TestEmittingSXSSFWorkbook extends BaseTestXWorkbook {
    
    public TestEmittingSXSSFWorkbook() {
        super(SXSSFITestDataProvider.instance);
    }
    
    @After
    public void tearDown() {
        ((SXSSFITestDataProvider) _testDataProvider).cleanup();
    }
    
    /**
     * cloning of sheets is not supported in SXSSF
     */
    @Override
    @Test
    public void cloneSheet() throws IOException {
        try {
            super.cloneSheet();
            fail("expected exception");
        } catch (RuntimeException e) {
            assertEquals("Not Implemented", e.getMessage());
        }
    }
    
    /**
     * cloning of sheets is not supported in SXSSF
     */
    @Override
    @Test
    public void sheetClone() throws IOException {
        try {
            super.sheetClone();
            fail("expected exception");
        } catch (RuntimeException e) {
            assertEquals("Not Implemented", e.getMessage());
        }
    }
    
    /**
     * Skip this test, as SXSSF doesn't update formulas on sheet name changes.
     */
    @Override
    @Ignore("SXSSF doesn't update formulas on sheet name changes, as most cells probably aren't in memory at the time")
    @Test
    public void setSheetName() {
    }
    
    @Test
    public void existingWorkbook() throws IOException {
        XSSFWorkbook xssfWb1 = new XSSFWorkbook();
        xssfWb1.createSheet("S1");
        EmittingSXSSFWorkbook wb1 = new EmittingSXSSFWorkbook(xssfWb1);
        XSSFWorkbook xssfWb2 = SXSSFITestDataProvider.instance.writeOutAndReadBack(wb1);
        assertTrue(wb1.dispose());
        
        EmittingSXSSFWorkbook wb2 = new EmittingSXSSFWorkbook(xssfWb2);
        assertEquals(1, wb2.getNumberOfSheets());
        Sheet sheet = wb2.getStreamingSheetAt(0);
        assertNotNull(sheet);
        assertEquals("S1", sheet.getSheetName());
        assertTrue(wb2.dispose());
        xssfWb2.close();
        xssfWb1.close();
        
        wb2.close();
        wb1.close();
    }
    
    @Test
    public void useSharedStringsTable() throws Exception {
        // not supported with EmittingSXSSF
    }
    
    @Test
    public void addToExistingWorkbook() throws IOException {
        XSSFWorkbook xssfWb1 = new XSSFWorkbook();
        xssfWb1.createSheet("S1");
        Sheet sheet = xssfWb1.createSheet("S2");
        Row row = sheet.createRow(1);
        Cell cell = row.createCell(1);
        cell.setCellValue("value 2_1_1");
        EmittingSXSSFWorkbook wb1 = new EmittingSXSSFWorkbook(xssfWb1);
        XSSFWorkbook xssfWb2 = SXSSFITestDataProvider.instance.writeOutAndReadBack(wb1);
        assertTrue(wb1.dispose());
        xssfWb1.close();
        
        EmittingSXSSFWorkbook wb2 = new EmittingSXSSFWorkbook(xssfWb2);
        // Add a row to the existing empty sheet
        EmittingSXSSFSheet ssheet1 = wb2.getStreamingSheetAt(0);
        ssheet1.setRowGenerator((ssxSheet) -> {
            Row row1_1 = ssxSheet.createRow(1);
            Cell cell1_1_1 = row1_1.createCell(1);
            cell1_1_1.setCellValue("value 1_1_1");
        });
        
        // Add a row to the existing non-empty sheet
        EmittingSXSSFSheet ssheet2 = wb2.getStreamingSheetAt(1);
        ssheet2.setRowGenerator((ssxSheet) -> {
            Row row2_2 = ssxSheet.createRow(2);
            Cell cell2_2_1 = row2_2.createCell(1);
            cell2_2_1.setCellValue("value 2_2_1");
        });
        // Add a sheet with one row
        EmittingSXSSFSheet ssheet3 = wb2.createSheet("S3");
        ssheet3.setRowGenerator((ssxSheet) -> {
            Row row3_1 = ssxSheet.createRow(1);
            Cell cell3_1_1 = row3_1.createCell(1);
            cell3_1_1.setCellValue("value 3_1_1");
        });
        
        XSSFWorkbook xssfWb3 = SXSSFITestDataProvider.instance.writeOutAndReadBack(wb2);
        wb2.close();
        
        assertEquals(3, xssfWb3.getNumberOfSheets());
        // Verify sheet 1
        XSSFSheet sheet1 = xssfWb3.getSheetAt(0);
        assertEquals("S1", sheet1.getSheetName());
        assertEquals(1, sheet1.getPhysicalNumberOfRows());
        XSSFRow row1_1 = sheet1.getRow(1);
        assertNotNull(row1_1);
        XSSFCell cell1_1_1 = row1_1.getCell(1);
        assertNotNull(cell1_1_1);
        assertEquals("value 1_1_1", cell1_1_1.getStringCellValue());
        // Verify sheet 2
        XSSFSheet sheet2 = xssfWb3.getSheetAt(1);
        assertEquals("S2", sheet2.getSheetName());
        assertEquals(2, sheet2.getPhysicalNumberOfRows());
        Row row2_1 = sheet2.getRow(1);
        assertNotNull(row2_1);
        Cell cell2_1_1 = row2_1.getCell(1);
        assertNotNull(cell2_1_1);
        assertEquals("value 2_1_1", cell2_1_1.getStringCellValue());
        XSSFRow row2_2 = sheet2.getRow(2);
        assertNotNull(row2_2);
        XSSFCell cell2_2_1 = row2_2.getCell(1);
        assertNotNull(cell2_2_1);
        assertEquals("value 2_2_1", cell2_2_1.getStringCellValue());
        // Verify sheet 3
        XSSFSheet sheet3 = xssfWb3.getSheetAt(2);
        assertEquals("S3", sheet3.getSheetName());
        assertEquals(1, sheet3.getPhysicalNumberOfRows());
        XSSFRow row3_1 = sheet3.getRow(1);
        assertNotNull(row3_1);
        XSSFCell cell3_1_1 = row3_1.getCell(1);
        assertNotNull(cell3_1_1);
        assertEquals("value 3_1_1", cell3_1_1.getStringCellValue());
        
        xssfWb2.close();
        xssfWb3.close();
        wb1.close();
    }
    
    @Test
    public void sheetdataWriter() throws IOException {
        EmittingSXSSFWorkbook wb = new EmittingSXSSFWorkbook();
        SXSSFSheet sh = wb.createSheet();
        assertSame(sh.getClass(), EmittingSXSSFSheet.class);
        SheetDataWriter wr = sh.getSheetDataWriter();
        assertNull(wr);
        wb.close();
    }
    
    @Test
    public void gzipSheetdataWriter() throws IOException {
        EmittingSXSSFWorkbook wb = new EmittingSXSSFWorkbook();
        wb.setCompressTempFiles(true);
        
        final int rowNum = 1000;
        final int sheetNum = 5;
        populateData(wb, 1000, 5);
        
        XSSFWorkbook xwb = SXSSFITestDataProvider.instance.writeOutAndReadBack(wb);
        for (int i = 0; i < sheetNum; i++) {
            Sheet sh = xwb.getSheetAt(i);
            assertEquals("sheet" + i, sh.getSheetName());
            for (int j = 0; j < rowNum; j++) {
                Row row = sh.getRow(j);
                assertNotNull("row[" + j + "]", row);
                Cell cell1 = row.getCell(0);
                assertEquals(new CellReference(cell1).formatAsString(), cell1.getStringCellValue());
                
                Cell cell2 = row.getCell(1);
                assertEquals(i, (int) cell2.getNumericCellValue());
                
                Cell cell3 = row.getCell(2);
                assertEquals(j, (int) cell3.getNumericCellValue());
            }
        }
        
        assertTrue(wb.dispose());
        xwb.close();
        wb.close();
    }
    
    private static void assertWorkbookDispose(EmittingSXSSFWorkbook wb) {
        populateData(wb, 1000, 5);
        
        for (Sheet sheet : wb) {
            EmittingSXSSFSheet sxSheet = (EmittingSXSSFSheet) sheet;
            assertNull(sxSheet.getSheetDataWriter());
        }
        
        assertTrue(wb.dispose());
        
        for (Sheet sheet : wb) {
            EmittingSXSSFSheet sxSheet = (EmittingSXSSFSheet) sheet;
            assertNull(sxSheet.getSheetDataWriter());
        }
    }
    
    private static void populateData(EmittingSXSSFWorkbook wb, final int rowNum, final int sheetNum) {
        for (int i = 0; i < sheetNum; i++) {
            EmittingSXSSFSheet sheet = wb.createSheet("sheet" + i);
            int index = i;
            sheet.setRowGenerator((sh) -> {
                for (int j = 0; j < rowNum; j++) {
                    Row row = sh.createRow(j);
                    Cell cell1 = row.createCell(0);
                    cell1.setCellValue(new CellReference(cell1).formatAsString());
                    
                    Cell cell2 = row.createCell(1);
                    cell2.setCellValue(index);
                    
                    Cell cell3 = row.createCell(2);
                    cell3.setCellValue(j);
                }
            });
        }
    }
    
    @Test
    public void workbookDispose() throws IOException {
        EmittingSXSSFWorkbook wb1 = new EmittingSXSSFWorkbook();
        // the underlying writer is SheetDataWriter
        assertWorkbookDispose(wb1);
        wb1.close();
        
        EmittingSXSSFWorkbook wb2 = new EmittingSXSSFWorkbook();
        wb2.setCompressTempFiles(true);
        // the underlying writer is GZIPSheetDataWriter
        assertWorkbookDispose(wb2);
        wb2.close();
    }
}
