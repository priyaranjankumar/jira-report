package com.example.jira.report.model;

import java.util.List;
import java.util.Map;

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
        private TeamField customfield_10001; // Updated to correct team field ID

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
            return customfield_10001;
        }

        public void setTeamField(TeamField teamField) {
            this.customfield_10001 = teamField;
        }

        // Direct access method for reflection
        public Object getCustomfield_10001() {
            return customfield_10001;
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

            private String id;
            private String name;
            private String avatarUrl;
            private boolean isVisible;
            private boolean isVerified;
            private String title;
            private boolean isShared;
            // Additional fields that may appear in the JSON
            private Object value;
            private Object self;
            private Object displayName;

            public String getId() {
                return id;
            }

            public void setId(String id) {
                this.id = id;
            }

            public String getName() {
                return name;
            }

            public void setName(String name) {
                this.name = name;
            }

            public String getAvatarUrl() {
                return avatarUrl;
            }

            public void setAvatarUrl(String avatarUrl) {
                this.avatarUrl = avatarUrl;
            }

            public boolean isVisible() {
                return isVisible;
            }

            public void setVisible(boolean isVisible) {
                this.isVisible = isVisible;
            }

            public boolean isVerified() {
                return isVerified;
            }

            public void setVerified(boolean isVerified) {
                this.isVerified = isVerified;
            }

            public String getTitle() {
                return title;
            }

            public void setTitle(String title) {
                this.title = title;
            }

            public boolean isShared() {
                return isShared;
            }

            public void setShared(boolean isShared) {
                this.isShared = isShared;
            }

            public Object getValue() {
                return value;
            }

            public void setValue(Object value) {
                this.value = value;
                // If name is null but value contains a name, extract it
                if (this.name == null && value instanceof Map) {
                    Map<?, ?> valueMap = (Map<?, ?>) value;
                    if (valueMap.containsKey("name")) {
                        this.name = valueMap.get("name").toString();
                    }
                }
            }

            public Object getSelf() {
                return self;
            }

            public void setSelf(Object self) {
                this.self = self;
            }

            public Object getDisplayName() {
                return displayName;
            }

            public void setDisplayName(Object displayName) {
                this.displayName = displayName;
                // If name is null but displayName is a string, use it
                if (this.name == null && displayName instanceof String) {
                    this.name = (String) displayName;
                }
            }

            @Override
            public String toString() {
                return "TeamField{"
                        + "id='" + id + '\''
                        + ", name='" + name + '\''
                        + ", title='" + title + '\''
                        + ", value=" + value
                        + ", displayName=" + displayName
                        + '}';
            }
        }
    }
}
