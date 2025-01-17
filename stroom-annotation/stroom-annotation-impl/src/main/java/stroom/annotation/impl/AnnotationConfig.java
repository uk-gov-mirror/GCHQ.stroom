package stroom.annotation.impl;

import stroom.config.common.DbConfig;
import stroom.config.common.HasDbConfig;
import stroom.util.shared.AbstractConfig;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;

import java.util.ArrayList;
import java.util.List;
import javax.inject.Singleton;

@Singleton
public class AnnotationConfig extends AbstractConfig implements HasDbConfig {

    private DbConfig dbConfig = new DbConfig();
    private List<String> statusValues = new ArrayList<>();
    private List<String> standardComments = new ArrayList<>();
    private String createText = "Create Annotation";

    public AnnotationConfig() {
        statusValues.add("New");
        statusValues.add("Assigned");
        statusValues.add("Closed");
    }

    @JsonProperty("db")
    public DbConfig getDbConfig() {
        return dbConfig;
    }

    public void setDbConfig(final DbConfig dbConfig) {
        this.dbConfig = dbConfig;
    }

    @JsonProperty("statusValues")
    @JsonPropertyDescription("The different status values that can be set on an annotation")
    public List<String> getStatusValues() {
        return statusValues;
    }

    public void setStatusValues(final List<String> statusValues) {
        this.statusValues = statusValues;
    }

    @JsonProperty("standardComments")
    @JsonPropertyDescription("A list of standard comments that can be added to annotations")
    public List<String> getStandardComments() {
        return standardComments;
    }

    public void setStandardComments(final List<String> standardComments) {
        this.standardComments = standardComments;
    }

    @JsonProperty("createText")
    @JsonPropertyDescription("The text to display to create an annotation")
    public String getCreateText() {
        return createText;
    }

    public void setCreateText(final String createText) {
        this.createText = createText;
    }
}
