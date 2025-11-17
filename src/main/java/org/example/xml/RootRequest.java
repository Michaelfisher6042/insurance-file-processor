package org.example.xml;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;

import java.util.List;

@JacksonXmlRootElement(localName = "root")
public class RootRequest {
    @JacksonXmlProperty(localName = "requestDetails")
    private RequestDetailsDto requestDetails;

    @JacksonXmlElementWrapper(localName = "events")
    @JacksonXmlProperty(localName = "event")
    private List<EventDto> events;

    public RequestDetailsDto getRequestDetails() { return requestDetails; }
    public void setRequestDetails(RequestDetailsDto requestDetails) { this.requestDetails = requestDetails; }
    public List<EventDto> getEvents() { return events; }
    public void setEvents(List<EventDto> events) { this.events = events; }
}

