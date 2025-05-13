package com.example.jira.report.service;

import com.example.jira.report.model.JiraIssue;
import com.example.jira.report.model.JiraProject;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress; // Added this import
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class ExcelService {

    private static final String BULLET_SYMBOL = "Φ"; // Phi symbol for bullet points

    public File buildReportOnDisk(List<JiraProject> projects,
                                  Map<String, List<JiraIssue>> issuesByProject)
            throws IOException {
        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet("Jira Tasks");

            // Create cell styles for different elements
            CellStyle headerStyle = createHeaderStyle(wb);
            CellStyle sectionHeaderStyle = createSectionHeaderStyle(wb);
            CellStyle projectHeaderStyle = createProjectHeaderStyle(wb);
            CellStyle bulletItemStyle = createBulletItemStyle(wb);

            // Create a report info sheet
            Sheet infoSheet = wb.createSheet("Report Info");
            Row infoRow1 = infoSheet.createRow(0);
            infoRow1.createCell(0).setCellValue("Report Generated");
            infoRow1.createCell(1).setCellValue(LocalDateTime.now().toString());

            Row infoRow2 = infoSheet.createRow(1);
            infoRow2.createCell(0).setCellValue("Total Projects");
            infoRow2.createCell(1).setCellValue(projects.size());

            int totalIssues = issuesByProject.values().stream()
                    .mapToInt(List::size)
                    .sum();

            Row infoRow3 = infoSheet.createRow(2);
            infoRow3.createCell(0).setCellValue("Total Issues");
            infoRow3.createCell(1).setCellValue(totalIssues);

            // Auto-size info sheet columns
            infoSheet.autoSizeColumn(0);
            infoSheet.autoSizeColumn(1);

            // Group the issues by status and project
            Map<String, List<JiraIssue>> completedIssues = new HashMap<>();
            Map<String, List<JiraIssue>> inProgressIssues = new HashMap<>();

            // Count issues per project for Project Summary Details section
            Map<String, Integer> totalIssuesPerProject = new HashMap<>();
            Map<String, Integer> completedIssuesPerProject = new HashMap<>();

            for (JiraProject project : projects) {
                List<JiraIssue> issues = issuesByProject.get(project.getKey());
                if (issues == null || issues.isEmpty()) {
                    continue;
                }
                
                String projectName = project.getName();
                totalIssuesPerProject.put(projectName, issues.size());
                int completedCount = 0;

                // Split issues by status
                for (JiraIssue issue : issues) {
                    String status = issue.getFields().getStatus().getName();

                    // Categorize by completion status
                    if ("Done".equals(status) || "Closed".equals(status) || "Resolved".equals(status)) {
                        completedIssues.computeIfAbsent(projectName, k -> new ArrayList<>()).add(issue);
                        completedCount++;
                    } else {
                        inProgressIssues.computeIfAbsent(projectName, k -> new ArrayList<>()).add(issue);
                    }
                }
                
                completedIssuesPerProject.put(projectName, completedCount);
            }

            // Generate the report in the new format
            int rowNum = 0;

            // Main Header
            Row mainHeaderRow = sheet.createRow(rowNum++);
            Cell mainHeaderCell = mainHeaderRow.createCell(0);
            mainHeaderCell.setCellValue("JIRA STATUS REPORT");
            mainHeaderCell.setCellStyle(headerStyle);
            sheet.addMergedRegion(new CellRangeAddress(rowNum-1, rowNum-1, 0, 2));
            
            // Add empty row
            sheet.createRow(rowNum++);

            // Section: Project Summary Details
            Row projectSummaryRow = sheet.createRow(rowNum++);
            Cell projectSummaryCell = projectSummaryRow.createCell(0);
            projectSummaryCell.setCellValue("Project Summary Details");
            projectSummaryCell.setCellStyle(sectionHeaderStyle);
            sheet.addMergedRegion(new CellRangeAddress(rowNum-1, rowNum-1, 0, 2));
            
            // Project summary headers
            Row summaryHeaderRow = sheet.createRow(rowNum++);
            Cell projectNameHeader = summaryHeaderRow.createCell(0);
            projectNameHeader.setCellValue("Project Name");
            projectNameHeader.setCellStyle(projectHeaderStyle);
            
            Cell totalTasksHeader = summaryHeaderRow.createCell(1);
            totalTasksHeader.setCellValue("Total Tasks");
            totalTasksHeader.setCellStyle(projectHeaderStyle);
            
            Cell completedTasksHeader = summaryHeaderRow.createCell(2);
            completedTasksHeader.setCellValue("Completed Tasks");
            completedTasksHeader.setCellStyle(projectHeaderStyle);
            
            Cell progressHeader = summaryHeaderRow.createCell(3);
            progressHeader.setCellValue("% Complete");
            progressHeader.setCellStyle(projectHeaderStyle);
            
            // Sort projects by name for consistent ordering
            List<String> sortedProjectNames = new ArrayList<>(totalIssuesPerProject.keySet());
            Collections.sort(sortedProjectNames);
            
            // Add project summary rows
            for (String projectName : sortedProjectNames) {
                int total = totalIssuesPerProject.getOrDefault(projectName, 0);
                int completed = completedIssuesPerProject.getOrDefault(projectName, 0);
                double percentage = total > 0 ? (completed * 100.0 / total) : 0;
                
                Row projectRow = sheet.createRow(rowNum++);
                projectRow.createCell(0).setCellValue(projectName);
                projectRow.createCell(1).setCellValue(total);
                projectRow.createCell(2).setCellValue(completed);
                projectRow.createCell(3).setCellValue(String.format("%.1f%%", percentage));
            }
            
            // Add empty row
            sheet.createRow(rowNum++);

            // Section A: Completed Items
            Row sectionARow = sheet.createRow(rowNum++);
            Cell sectionACell = sectionARow.createCell(0);
            sectionACell.setCellValue("A.\tCompleted Items");
            sectionACell.setCellStyle(sectionHeaderStyle);
            sheet.addMergedRegion(new CellRangeAddress(rowNum-1, rowNum-1, 0, 2));

            // Add completed items by project
            rowNum = addProjectItemsSection(sheet, rowNum, completedIssues, projectHeaderStyle, bulletItemStyle);

            // Section B: InProgress Items
            Row sectionBRow = sheet.createRow(rowNum++);
            Cell sectionBCell = sectionBRow.createCell(0);
            sectionBCell.setCellValue("B.\tInProgress Items");
            sectionBCell.setCellStyle(sectionHeaderStyle);
            sheet.addMergedRegion(new CellRangeAddress(rowNum-1, rowNum-1, 0, 2));

            // Add in-progress items by project
            rowNum = addProjectItemsSection(sheet, rowNum, inProgressIssues, projectHeaderStyle, bulletItemStyle);

            // Auto-size columns
            for (int i = 0; i <= 3; i++) {
                sheet.autoSizeColumn(i);
            }

            // Create directory structure
            File parentDir = new File("..").getAbsoluteFile();
            File reportDir = new File(parentDir, "WSR Reports");
            if (!reportDir.exists()) {
                if (!reportDir.mkdirs()) {
                    throw new IOException("Failed to create directory: " + reportDir.getAbsolutePath());
                }
            }

            // Write to file in the WSR Reports directory with a better filename
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
            String dateString = LocalDate.now().format(formatter);
            String fileName = "JiraTasks-" + dateString + "-" + System.currentTimeMillis() + ".xlsx";
            File out = new File(reportDir, fileName);
            try (FileOutputStream fos = new FileOutputStream(out)) {
                wb.write(fos);
            }
            return out;
        }
    }

    /**
     * Adds project items to the report sheet in the desired format
     */
    private int addProjectItemsSection(Sheet sheet, int startRowNum, Map<String, List<JiraIssue>> issuesByProject,
                                      CellStyle projectStyle, CellStyle bulletStyle) {
        int rowNum = startRowNum;

        // Sort projects by name for consistent ordering
        List<String> sortedProjectNames = new ArrayList<>(issuesByProject.keySet());
        Collections.sort(sortedProjectNames);

        for (String projectName : sortedProjectNames) {
            List<JiraIssue> issues = issuesByProject.get(projectName);

            // Skip if no issues
            if (issues == null || issues.isEmpty()) {
                continue;
            }

            // Add project name
            Row projectRow = sheet.createRow(rowNum++);
            Cell projectCell = projectRow.createCell(0);
            projectCell.setCellValue("\tProject " + projectName);
            projectCell.setCellStyle(projectStyle);
            sheet.addMergedRegion(new CellRangeAddress(rowNum-1, rowNum-1, 0, 2));

            // Add issues with bullet points
            for (JiraIssue issue : issues) {
                Row issueRow = sheet.createRow(rowNum++);
                
                Cell bulletCell = issueRow.createCell(0);
                bulletCell.setCellValue("\t" + BULLET_SYMBOL);
                bulletCell.setCellStyle(bulletStyle);
                
                Cell summaryCell = issueRow.createCell(1);
                summaryCell.setCellValue(issue.getFields().getSummary());
                summaryCell.setCellStyle(bulletStyle);
                sheet.addMergedRegion(new CellRangeAddress(rowNum-1, rowNum-1, 1, 2));
            }

            // Add empty row after project
            sheet.createRow(rowNum++);
        }

        return rowNum;
    }
    
    /**
     * Creates a style for the main header
     */
    private CellStyle createHeaderStyle(XSSFWorkbook wb) {
        XSSFCellStyle style = wb.createCellStyle();
        Font headerFont = wb.createFont();
        headerFont.setBold(true);
        headerFont.setFontHeightInPoints((short) 14);
        style.setFont(headerFont);
        style.setFillForegroundColor(IndexedColors.LIGHT_BLUE.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        return style;
    }
    
    /**
     * Creates a style for section headers (A, B sections)
     */
    private CellStyle createSectionHeaderStyle(XSSFWorkbook wb) {
        XSSFCellStyle style = wb.createCellStyle();
        Font font = wb.createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short) 12);
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.LIGHT_GREEN.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        return style;
    }
    
    /**
     * Creates a style for project headers
     */
    private CellStyle createProjectHeaderStyle(XSSFWorkbook wb) {
        XSSFCellStyle style = wb.createCellStyle();
        Font font = wb.createFont();
        font.setBold(true);
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.LIGHT_YELLOW.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        return style;
    }
    
    /**
     * Creates a style for bullet items
     */
    private CellStyle createBulletItemStyle(XSSFWorkbook wb) {
        XSSFCellStyle style = wb.createCellStyle();
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        return style;
    }
}