package com.example.jira.report.scheduler;

import com.example.jira.report.model.JiraIssue;
import com.example.jira.report.model.JiraProject;
import com.example.jira.report.service.EmailService;
import com.example.jira.report.service.ExcelService;
import com.example.jira.report.service.JiraService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.File;
import java.nio.file.Files;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

@Component
public class ReportScheduler {

    private final JiraService jiraService;
    private final ExcelService excelService;
    private final EmailService emailService;
    
    // Flag to prevent concurrent execution
    private final AtomicBoolean isRunning = new AtomicBoolean(false);

    public ReportScheduler(JiraService jiraService,
                           ExcelService excelService,
                           EmailService emailService) {
        this.jiraService = jiraService;
        this.excelService = excelService;
        this.emailService = emailService;
    }

    @Scheduled(cron = "${jira.report.cron}")
    public void runReport() {
        // Use atomic boolean to prevent multiple executions
        if (!isRunning.compareAndSet(false, true)) {
            System.out.println("⚠️ Report generation already in progress, skipping this run");
            return;
        }
        
        try {
            System.out.println("Starting report generation at: " + LocalDate.now() + " " + LocalTime.now());
            
            var projects = jiraService.fetchAllProjects();
            var map = new HashMap<String, List<JiraIssue>>();
            for (var p : projects) {
                map.put(p.getKey(), jiraService.fetchIssues(p.getKey()));
            }

            File reportFile = excelService.buildReportOnDisk(projects, map);
            System.out.println("✅ Report generated at: " + reportFile.getAbsolutePath());
            
            try {
                byte[] reportBytes = Files.readAllBytes(reportFile.toPath());
                
                String to = "priyaranjankumar712@gmail.com";
                String subject = "Daily Jira Report – " + LocalDate.now();
                String body = "Please find attached the daily Jira tasks report.";
                emailService.sendReport(to, subject, body, reportBytes);
                System.out.println("✅ Report emailed to " + to);
            } catch (Exception emailEx) {
                System.err.println("⚠️ Email delivery failed: " + emailEx.getMessage());
            }

        } catch (Exception ex) {
            System.err.println("❌ Failed to generate report:");
            ex.printStackTrace();
        } finally {
            // Always reset the flag when done
            isRunning.set(false);
        }
    }
}