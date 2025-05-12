package com.example.jira.report.service;
import org.springframework.web.util.UriComponentsBuilder;  // add this import at the top
import com.example.jira.report.model.IssueSearchResult;
import com.example.jira.report.model.JiraIssue;
import com.example.jira.report.model.JiraProject;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriUtils;
import org.springframework.web.util.UriComponentsBuilder;
import java.util.Arrays;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Collections;
import org.springframework.http.ResponseEntity;


@Service
public class JiraService {
    private final RestTemplate rest;
    private final String baseUrl;

    public JiraService(RestTemplateBuilder builder,
                       @Value("${jira.base-url}") String baseUrl,
                       @Value("${jira.email}") String email,
                       @Value("${jira.api-token}") String token) {
        this.baseUrl = baseUrl;
        this.rest = builder
                .basicAuthentication(email, token)
                .build();
    }


    /** Fetch all projects */


    public List<JiraProject> fetchAllProjects() {
        String url = UriComponentsBuilder
                .fromUriString(baseUrl + "/project/search")
                .queryParam("maxResults", 500)
                .toUriString();

        // Use JsonNode parsing as before, or map to a wrapper class
        JsonNode resp = rest.getForObject(url, JsonNode.class);
        List<JiraProject> projects = new ArrayList<>();
        for (JsonNode n : resp.path("values")) {
            JiraProject p = new JiraProject();
            p.setKey(n.get("key").asText());
            p.setName(n.get("name").asText());
            projects.add(p);
        }
        return projects;
    }
    
/** Fetch issues for one project */
public List<JiraIssue> fetchIssues(String projectKey) {
    // Use the RestTemplate's exchange method for more control
    System.out.println("Fetching issues for project key: " + projectKey);
    
    // Use a map for query parameters to ensure proper encoding
    UriComponentsBuilder builder = UriComponentsBuilder
            .fromUriString(baseUrl + "/search")
            .queryParam("fields", "summary,status,assignee")
            .queryParam("maxResults", 500);
    
    // Add the JQL parameter with proper encoding
    Map<String, String> uriVariables = new HashMap<>();
    uriVariables.put("projectKey", projectKey);
    
    // Use a request entity with the properly encoded URI
    String finalUrl = builder.build().toUriString() + "&jql={jql}";
    String jql = "project = '{projectKey}'";
    
    System.out.println("Request URL template: " + finalUrl);
    
    ResponseEntity<IssueSearchResult> response = rest.getForEntity(
            finalUrl, 
            IssueSearchResult.class,
            Collections.singletonMap("jql", jql.replace("{projectKey}", projectKey))
    );
    
    return (response.getBody() != null ? response.getBody().getIssues() : List.of());
}
}