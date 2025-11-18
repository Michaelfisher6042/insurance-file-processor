package org.example.xml;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import lombok.Data;

import java.util.List;

@JacksonXmlRootElement(localName = "root")
@Data
public class RootRequest {
    @JacksonXmlProperty(localName = "requestDetails")
    private RequestDetailsDto requestDetails;

    @JacksonXmlElementWrapper(localName = "events")
    @JacksonXmlProperty(localName = "event")
    private List<EventDto> events;
}

