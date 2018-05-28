
package ru.slavasim.fifa.model;

import com.fasterxml.jackson.annotation.*;
import lombok.Data;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
        "Cached",
        "CachedOn",
        "Data"
})
public class Availability {

    @JsonProperty("Cached")
    private Boolean cached;
    @JsonProperty("CachedOn")
    private String cachedOn;
    @JsonProperty("Data")
    private List<AvailabilityData> data = null;
    @JsonIgnore
    private Map<String, Object> additionalProperties = new HashMap<String, Object>();

    @JsonAnyGetter
    public Map<String, Object> getAdditionalProperties() {
        return this.additionalProperties;
    }

    @JsonAnySetter
    public void setAdditionalProperty(String name, Object value) {
        this.additionalProperties.put(name, value);
    }

}
