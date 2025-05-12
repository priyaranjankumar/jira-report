package com.example.jira.report.service;

import com.example.jira.report.model.JiraIssue;
import com.example.jira.report.model.JiraProject;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@Service
public class ExcelService {
    public File buildReportOnDisk(List<JiraProject> projects,
                                  Map<String, List<JiraIssue>> issuesByProject)
            throws IOException {
        try (Workbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet("Jira Tasks");
            
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
            
            // Header for main sheet
            Row h = sheet.createRow(0);
            h.createCell(0).setCellValue("Project Name");
            h.createCell(1).setCellValue("Project Key");
            h.createCell(2).setCellValue("Issue Key");
            h.createCell(3).setCellValue("Summary");
            h.createCell(4).setCellValue("Status");
            h.createCell(5).setCellValue("Assignee");

            int rowNum = 1;
            for (JiraProject proj : projects) {
                List<JiraIssue> issues = issuesByProject.get(proj.getKey());
                if (issues == null || issues.isEmpty()) {
                    // Add an empty row for projects with no issues
                    Row r = sheet.createRow(rowNum++);
                    r.createCell(0).setCellValue(proj.getName());
                    r.createCell(1).setCellValue(proj.getKey());
                    r.createCell(2).setCellValue("No issues found");
                    continue;
                }
                
                for (JiraIssue issue : issues) {
                    Row r = sheet.createRow(rowNum++);
                    r.createCell(0).setCellValue(proj.getName());
                    r.createCell(1).setCellValue(proj.getKey());
                    r.createCell(2).setCellValue(issue.getKey());
                    r.createCell(3).setCellValue(issue.getFields().getSummary());
                    r.createCell(4).setCellValue(issue.getFields().getStatus().getName());
                    var assg = issue.getFields().getAssignee();
                    r.createCell(5).setCellValue(assg != null
                            ? assg.getDisplayName() : "Unassigned");
                }
            }
            
            // Auto-size
            for (int i = 0; i <= 5; i++) sheet.autoSizeColumn(i);

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
}