package com.example.jira.report.model;

public class JiraIssue {
    private String key;
    private Fields fields;

    public String getKey() { return key; }
    public void setKey(String key) { this.key = key; }

    public Fields getFields() { return fields; }
    public void setFields(Fields fields) { this.fields = fields; }

    public static class Fields {
        private String summary;
        private Status status;
        private Assignee assignee;

        public String getSummary() { return summary; }
        public void setSummary(String s) { this.summary = s; }

        public Status getStatus() { return status; }
        public void setStatus(Status status) { this.status = status; }

        public Assignee getAssignee() { return assignee; }
        public void setAssignee(Assignee a) { this.assignee = a; }

        public static class Status {
            private String name;
            public String getName() { return name; }
            public void setName(String n) { this.name = n; }
        }
        public static class Assignee {
            private String displayName;
            public String getDisplayName() { return displayName; }
            public void setDisplayName(String d) { this.displayName = d; }
        }
    }
}
