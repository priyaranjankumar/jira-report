package com.example.jira.report.model;
import java.util.List;

public class IssueSearchResult {
    private List<JiraIssue> issues;
    public List<JiraIssue> getIssues() { return issues; }
    public void setIssues(List<JiraIssue> issues) { this.issues = issues; }
}


