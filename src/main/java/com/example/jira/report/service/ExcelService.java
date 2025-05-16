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
            Cell sectionAHeaderCell = sectionARow.createCell(0);
            sectionAHeaderCell.setCellValue("A.");
            sectionAHeaderCell.setCellStyle(boldStyle);

            Cell completedItemsHeader = sectionARow.createCell(1);
            completedItemsHeader.setCellValue("Completed Items");
            completedItemsHeader.setCellStyle(boldStyle);

            // Add column headers for the completed items section
            Row columnHeaderRow = sheet.createRow(rowNum++);
            // Empty cells for A. and project name columns
            columnHeaderRow.createCell(0);
            columnHeaderRow.createCell(1);
            // Project name header
            Cell projectNameCell = columnHeaderRow.createCell(1);
            projectNameCell.setCellValue("Project Name");
            projectNameCell.setCellStyle(boldStyle);

            // Issue key header
            Cell keyHeader = columnHeaderRow.createCell(2);
            keyHeader.setCellValue("Issue");
            keyHeader.setCellStyle(boldStyle);

            // Summary header
            Cell summaryHeader = columnHeaderRow.createCell(3);
            summaryHeader.setCellValue("Description");
            summaryHeader.setCellStyle(boldStyle);

            // Team header
            Cell teamHeader = columnHeaderRow.createCell(4);
            teamHeader.setCellValue("Team");
            teamHeader.setCellStyle(boldStyle);

            // Add completed items by project
            rowNum = addProjectItemsSection(sheet, rowNum, completedIssues, boldStyle, normalStyle);

            // Section B: InProgress Items
            Row sectionBRow = sheet.createRow(rowNum++);
            Cell sectionBCell = sectionBRow.createCell(0);
            sectionBCell.setCellValue("B.");
            sectionBCell.setCellStyle(boldStyle);

            Cell inProgressItemsHeader = sectionBRow.createCell(1);
            inProgressItemsHeader.setCellValue("In Progress Items");
            inProgressItemsHeader.setCellStyle(boldStyle);

            // Add column headers for the in progress items section
            Row inProgressColumnHeaderRow = sheet.createRow(rowNum++);
            // Empty cells for B. and project name columns
            inProgressColumnHeaderRow.createCell(0);
            inProgressColumnHeaderRow.createCell(1);

            // Issue key header
            Cell inProgressKeyHeader = inProgressColumnHeaderRow.createCell(2);
            inProgressKeyHeader.setCellValue("Issue");
            inProgressKeyHeader.setCellStyle(boldStyle);

            // Summary header
            Cell inProgressSummaryHeader = inProgressColumnHeaderRow.createCell(3);
            inProgressSummaryHeader.setCellValue("Description");
            inProgressSummaryHeader.setCellStyle(boldStyle);

            // Team header
            Cell inProgressTeamHeader = inProgressColumnHeaderRow.createCell(4);
            inProgressTeamHeader.setCellValue("Team");
            inProgressTeamHeader.setCellStyle(boldStyle);

            // Add in-progress items by project
            addProjectItemsSection(sheet, rowNum, inProgressIssues, boldStyle, normalStyle);// Auto-size columns            // Auto-size columns for better readability
            for (int i = 0; i <= 4; i++) {
                sheet.autoSizeColumn(i);
            }

            // Set specific column widths to match the markdown table format
            sheet.setColumnWidth(0, 1500);  // A column - narrow for section letters
            sheet.setColumnWidth(1, 5000);  // B column - for bullet points and project names
            sheet.setColumnWidth(2, 3500);  // C column - for issue keys
            sheet.setColumnWidth(3, 15000); // D column - wider for descriptions
            sheet.setColumnWidth(4, 4000);  // E column - for team names

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
            Cell projectCell = projectRow.createCell(1);
            projectCell.setCellValue(projectName);
            projectCell.setCellStyle(projectStyle);

            // Add issues with bullet points
            for (JiraIssue issue : issues) {
                Row issueRow = sheet.createRow(rowNum++);

                // Add empty first column (for indentation)
                issueRow.createCell(0);

                // Add bullet point in second column
                Cell bulletCell = issueRow.createCell(1);
                bulletCell.setCellValue(BULLET_SYMBOL);
                bulletCell.setCellStyle(bulletStyle);

                // Add issue key in third column
                Cell keyCell = issueRow.createCell(2);
                keyCell.setCellValue(issue.getKey());
                keyCell.setCellStyle(bulletStyle);

                // Add issue summary in fourth column
                Cell summaryCell = issueRow.createCell(3);
                summaryCell.setCellValue(issue.getFields().getSummary());
                summaryCell.setCellStyle(bulletStyle);

                // Add team name in fifth column
                Cell teamCell = issueRow.createCell(4);
                String teamName = getTeamName(issue); // Extract team name from issue
                teamCell.setCellValue(teamName);
                teamCell.setCellStyle(bulletStyle);
            }

            // Add empty row after project
            sheet.createRow(rowNum++);
        }

        return rowNum;
    }// We've replaced all the styled methods with simpler ones

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
     * Extracts the team name from a Jira issue directly from the Jira API using
     * customfield_10001 which contains the team information.
     */
    private String getTeamName(JiraIssue issue) {
        try {
            // Debug output to help diagnose issues
            System.out.println("Issue key: " + issue.getKey());
            System.out.println("Has fields: " + (issue.getFields() != null));

            if (issue.getFields() == null) {
                return "No fields available";
            }

            // Use reflection to access fields directly for debugging
            System.out.println("Fields class: " + issue.getFields().getClass().getName());

            try {
                // Try to access customfield_10001 directly through reflection
                java.lang.reflect.Field[] fields = issue.getFields().getClass().getDeclaredFields();
                System.out.println("Available fields in Fields class:");
                for (java.lang.reflect.Field field : fields) {
                    field.setAccessible(true);
                    String fieldName = field.getName();
                    Object fieldValue = field.get(issue.getFields());
                    System.out.println("  - " + fieldName + ": " + (fieldValue != null ? fieldValue.getClass().getName() : "null"));

                    // Check if this is our team field
                    if (fieldName.equals("customfield_10001")) {
                        System.out.println("    Found customfield_10001: " + fieldValue);
                        if (fieldValue != null) {
                            // If it's a Map or has a name/title property, try to extract it
                            if (fieldValue instanceof Map) {
                                Map<?, ?> map = (Map<?, ?>) fieldValue;
                                if (map.containsKey("name")) {
                                    return map.get("name").toString();
                                } else if (map.containsKey("title")) {
                                    return map.get("title").toString();
                                } else if (map.containsKey("id")) {
                                    return map.get("id").toString();
                                }
                            }
                        }
                    }
                }
            } catch (Exception e) {
                System.out.println("Error accessing fields via reflection: " + e.getMessage());
            }

            // Check if TeamField exists through getter
            System.out.println("Has team field: " + (issue.getFields().getTeamField() != null));

            if (issue.getFields().getTeamField() != null) {
                // Try to print the full team field object for debugging
                System.out.println("TeamField toString: " + issue.getFields().getTeamField().toString());

                // Try to get the name property first (most meaningful)
                String teamName = issue.getFields().getTeamField().getName();
                if (teamName != null && !teamName.isEmpty()) {
                    System.out.println("Found team name: " + teamName);
                    return teamName;
                }

                // Try the title property next
                teamName = issue.getFields().getTeamField().getTitle();
                if (teamName != null && !teamName.isEmpty()) {
                    System.out.println("Found team title: " + teamName);
                    return teamName;
                }

                // Fall back to ID as a last resort
                teamName = issue.getFields().getTeamField().getId();
                if (teamName != null && !teamName.isEmpty()) {
                    System.out.println("Using team ID as fallback: " + teamName);
                    return teamName;
                }

                // If we get here, the TeamField object exists but has no usable properties
                return "Team field data incomplete";
            }

            // Fall back approach: Try to directly access fields with alternative naming
            try {
                java.lang.reflect.Method method = issue.getFields().getClass().getMethod("getCustomfield_10001");
                Object result = method.invoke(issue.getFields());
                if (result != null) {
                    System.out.println("Direct access to customfield_10001 successful, type: " + result.getClass().getName());
                    return "Team: " + result.toString();
                }
            } catch (Exception e) {
                System.out.println("Error with direct method access: " + e.getMessage());
            }

            // If we reach here, the team field wasn't found in the API response
            System.out.println("No team field found for issue: " + issue.getKey());
            return "No team assigned";
        } catch (Exception e) {
            System.err.println("Error extracting team name for issue " + issue.getKey() + ": " + e.getMessage());
            e.printStackTrace();
            return "Error: " + e.getMessage();
        }
    }
}
