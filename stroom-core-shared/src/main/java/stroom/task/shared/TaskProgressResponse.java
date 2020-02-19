package stroom.task.shared;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import stroom.util.shared.PageResponse;
import stroom.util.shared.ResultPage;

import java.util.List;

public class TaskProgressResponse extends ResultPage<TaskProgress> {
    @JsonCreator
    public TaskProgressResponse(@JsonProperty("values") final List<TaskProgress> values,
                                @JsonProperty("pageResponse") final PageResponse pageResponse) {
        super(values, pageResponse);
    }
}
