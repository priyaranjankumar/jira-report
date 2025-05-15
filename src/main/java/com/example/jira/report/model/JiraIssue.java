package com.example.jira.report.model;

import java.util.List;

public class JiraIssue {

    private String key;
    private Fields fields;

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public Fields getFields() {
        return fields;
    }

    public void setFields(Fields fields) {
        this.fields = fields;
    }

    public static class Fields {

        private String summary;
        private Status status;
        private Assignee assignee;
        private List<Component> components;
        private TeamField customfield_10006; // This might be your team field in Jira

        public String getSummary() {
            return summary;
        }

        public void setSummary(String s) {
            this.summary = s;
        }

        public Status getStatus() {
            return status;
        }

        public void setStatus(Status status) {
            this.status = status;
        }

        public Assignee getAssignee() {
            return assignee;
        }

        public void setAssignee(Assignee a) {
            this.assignee = a;
        }

        public List<Component> getComponents() {
            return components;
        }

        public void setComponents(List<Component> components) {
            this.components = components;
        }

        public TeamField getTeamField() {
            return customfield_10006;
        }

        public void setTeamField(TeamField teamField) {
            this.customfield_10006 = teamField;
        }

        public static class Status {

            private String name;

            public String getName() {
                return name;
            }

            public void setName(String n) {
                this.name = n;
            }
        }

        public static class Assignee {

            private String displayName;

            public String getDisplayName() {
                return displayName;
            }

            public void setDisplayName(String d) {
                this.displayName = d;
            }
        }

        public static class Component {

            private String name;

            public String getName() {
                return name;
            }

            public void setName(String n) {
                this.name = n;
            }
        }

        public static class TeamField {

            private String value;
            private String displayName;

            public String getValue() {
                return value;
            }

            public void setValue(String v) {
                this.value = v;
            }

            public String getDisplayName() {
                return displayName;
            }

            public void setDisplayName(String d) {
                this.displayName = d;
            }
        }
    }
}
