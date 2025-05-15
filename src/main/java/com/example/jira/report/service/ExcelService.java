package com.example.jira.report.service;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import com.example.jira.report.model.JiraIssue;
import com.example.jira.report.model.JiraProject;

@Service
public class ExcelService {

    private static final String BULLET_SYMBOL = "Φ"; // Phi symbol for bullet points

    public File buildReportOnDisk(List<JiraProject> projects,
            Map<String, List<JiraIssue>> issuesByProject)
            throws IOException {
        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet("Jira Tasks");            // Create simple styles without colors
            CellStyle boldStyle = createSimpleBoldStyle(wb);
            CellStyle normalStyle = createSimpleStyle(wb);

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
            }            // Generate the report in the new format
            int rowNum = 0;            // Section: Project Summary Details
            Row projectSummaryRow = sheet.createRow(rowNum++);
            Cell projectSummaryCell = projectSummaryRow.createCell(0);
            projectSummaryCell.setCellValue("Project Summary Details");
            projectSummaryCell.setCellStyle(boldStyle);
            sheet.addMergedRegion(new CellRangeAddress(rowNum - 1, rowNum - 1, 0, 3));            // Project summary headers
            Row summaryHeaderRow = sheet.createRow(rowNum++);
            Cell projectNameHeader = summaryHeaderRow.createCell(0);
            projectNameHeader.setCellValue("Project Name");
            projectNameHeader.setCellStyle(boldStyle);

            Cell totalTasksHeader = summaryHeaderRow.createCell(1);
            totalTasksHeader.setCellValue("Total Tasks");
            totalTasksHeader.setCellStyle(boldStyle);

            Cell completedTasksHeader = summaryHeaderRow.createCell(2);
            completedTasksHeader.setCellValue("Completed Tasks");
            completedTasksHeader.setCellStyle(boldStyle);

            Cell progressHeader = summaryHeaderRow.createCell(3);
            progressHeader.setCellValue("% Complete");
            progressHeader.setCellStyle(boldStyle);

            // Cell teamHeader = summaryHeaderRow.createCell(4);
            // teamHeader.setCellValue("Team");
            // teamHeader.setCellStyle(boldStyle);
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
            sheet.createRow(rowNum++);            // Section A: Completed Items
            Row sectionARow = sheet.createRow(rowNum++);
            Cell sectionAHeaderCell = sectionARow.createCell(0); // Changed variable name
            sectionAHeaderCell.setCellValue("A.\tCompleted Items");
            Cell sectionATeamCell = sectionARow.createCell(1); // Changed variable name
            sectionATeamCell.setCellValue("Team");
            sectionATeamCell.setCellStyle(boldStyle);
            sheet.addMergedRegion(new CellRangeAddress(rowNum - 1, rowNum - 1, 0, 3));            // Add completed items by project
            rowNum = addProjectItemsSection(sheet, rowNum, completedIssues, boldStyle, normalStyle);// Section B: InProgress Items
            Row sectionBRow = sheet.createRow(rowNum++);
            Cell sectionBCell = sectionBRow.createCell(0);
            sectionBCell.setCellValue("B.\tInProgress Items");
            sectionBCell.setCellStyle(boldStyle);
            sheet.addMergedRegion(new CellRangeAddress(rowNum - 1, rowNum - 1, 0, 3));            // Add in-progress items by project
            addProjectItemsSection(sheet, rowNum, inProgressIssues, boldStyle, normalStyle);            // Auto-size columns
            for (int i = 0; i <= 4; i++) {
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
            projectCell.setCellValue("\t" + projectName);
            projectCell.setCellStyle(projectStyle);
            sheet.addMergedRegion(new CellRangeAddress(rowNum - 1, rowNum - 1, 0, 3));            // Add issues with bullet points
            for (JiraIssue issue : issues) {
                Row issueRow = sheet.createRow(rowNum++);

                // Add bullet point in first column
                Cell bulletCell = issueRow.createCell(0);
                bulletCell.setCellValue("\t" + BULLET_SYMBOL);
                bulletCell.setCellStyle(bulletStyle);

                // Add issue key in second column
                Cell keyCell = issueRow.createCell(1);
                keyCell.setCellValue(issue.getKey());
                keyCell.setCellStyle(bulletStyle);

                // Add issue summary in third column
                Cell summaryCell = issueRow.createCell(2);
                summaryCell.setCellValue(issue.getFields().getSummary());
                summaryCell.setCellStyle(bulletStyle);

                // Add team name in fourth column
                Cell teamCell = issueRow.createCell(3);
                String teamName = getTeamName(issue); // Extract team name from issue
                teamCell.setCellValue(teamName);
                teamCell.setCellStyle(bulletStyle);
            }

            // Add empty row after project
            sheet.createRow(rowNum++);
        }

        return rowNum;
    }    // We've replaced all the styled methods with simpler ones

    /**
     * Creates a simple bold style without colors
     */
    private CellStyle createSimpleBoldStyle(XSSFWorkbook wb) {
        CellStyle style = wb.createCellStyle();
        Font font = wb.createFont();
        font.setBold(true);
        style.setFont(font);
        return style;
    }

    /**
     * Creates a simple style without colors or bold
     */
    private CellStyle createSimpleStyle(XSSFWorkbook wb) {
        return wb.createCellStyle();
    }

    /**
     * Extracts the team name from a Jira issue directly from the Jira API Note:
     * Currently, the customfield_10006 is not being returned by the Jira API
     * for any issues. This method will be ready to use as soon as the Team
     * field (customfield_10006) is configured in Jira.
     */
    private String getTeamName(JiraIssue issue) {
        // First try to get from customfield_10006 (TeamField) if available from API
        if (issue.getFields().getTeamField() != null) {
            // Try to get displayName first
            String teamName = issue.getFields().getTeamField().getDisplayName();
            if (teamName != null && !teamName.isEmpty()) {
                return teamName;
            }

            // Fall back to value field within TeamField if displayName is not available
            teamName = issue.getFields().getTeamField().getValue();
            if (teamName != null && !teamName.isEmpty()) {
                return teamName;
            }
        }

        // For now, return a message indicating the need to configure the team field in Jira
        return "Team field not available in Jira API";
    }
}
