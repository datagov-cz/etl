package com.linkedpipes.etl.executor.monitor.execution.overview;

import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Java representation of an overview JSON.
 * Designed to obtain needed information from JSON not to be a comprehensive
 * representation.
 */
public class OverviewObject {

    private static final Logger LOG =
            LoggerFactory.getLogger(OverviewObject.class);

    private String pipeline;

    private Integer progressCurrent;

    private Integer progressTotal;

    private Integer progressTotalMap;

    private Integer progressCurrentMapped;

    private Integer progressCurrentExecuted;

    private Date start;

    private Date finish;

    private String status;

    private Date lastChange;

    private Long directorySize;

    public static OverviewObject fromJson(JsonNode root) {
        OverviewObject overview = new OverviewObject();

        overview.status = root.get("status").get("@id").asText();
        overview.lastChange = getLastChange(root);

        if (root.get("pipeline") != null) {
            JsonNode id = root.get("pipeline").get("@id");
            if (id != null) {
                overview.pipeline = id.asText();
            }
        }

        if (root.get("executionStarted") != null) {
            overview.start = asDate(root.get("executionStarted").asText());
        }

        if (root.get("executionFinished") != null) {
            overview.finish = asDate(root.get("executionFinished").asText());
        }

        JsonNode progress = root.get("pipelineProgress");
        if (root.get("pipelineProgress") != null) {
            overview.progressCurrent = progress.get("current").asInt();
            overview.progressTotal = progress.get("total").asInt();

            overview.progressTotalMap =
                    getIntOptional(progress, "total_map");
            overview.progressCurrentMapped =
                    getIntOptional(progress, "current_mapped");
            overview.progressCurrentExecuted =
                    getIntOptional(progress, "current_executed");
        }

        if (root.get("directorySize") != null) {
            overview.directorySize = root.get("directorySize").asLong();
        } else {
            overview.directorySize = null;
        }

        return overview;
    }

    private static Date asDate(String str) {
        if (str == null) {
            return null;
        }
        DateFormat dateFormat = new
                SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS");
        try {
            return dateFormat.parse(str);
        } catch (ParseException ex) {
            LOG.info("Can not parse date from overview: ", str);
            return null;
        }
    }

    private static Integer getIntOptional(JsonNode node, String name) {
        if (node.get(name) == null) {
            return null;
        } else {
            return node.get(name).asInt();
        }
    }

    public static String getIri(JsonNode root) {
        return root.get("execution").get("@id").asText();
    }

    public static Date getLastChange(JsonNode root) {
        return asDate(root.get("lastChange").asText());
    }

    public Date getLastChange() {
        if (lastChange == null) {
            return null;
        }
        return new Date(lastChange.getTime());
    }

    public String getPipeline() {
        return pipeline;
    }

    public void setPipeline(String pipeline) {
        this.pipeline = pipeline;
    }

    public Integer getProgressCurrent() {
        return progressCurrent;
    }

    public Integer getProgressTotal() {
        return progressTotal;
    }

    public Integer getProgressTotalMap() {
        return progressTotalMap;
    }

    public Integer getProgressCurrentMapped() {
        return progressCurrentMapped;
    }

    public Integer getProgressCurrentExecuted() {
        return progressCurrentExecuted;
    }

    public Date getStart() {
        if (start == null) {
            return null;
        }
        return new Date(start.getTime());
    }

    public Date getFinish() {
        if (finish == null) {
            return null;
        }
        return new Date(finish.getTime());
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Long getDirectorySize() {
        return directorySize;
    }

}
