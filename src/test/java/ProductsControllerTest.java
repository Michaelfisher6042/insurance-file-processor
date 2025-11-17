
import org.example.controller.ProductsController;
import org.example.model.EventEntity;
import org.example.model.ProductEntity;
import org.example.model.RequestDetailsEntity;
import org.example.repository.EventRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class ProductsControllerTest {

    @Mock
    private EventRepository eventRepository;

    @InjectMocks
    private ProductsController productsController;

    @Test
    void testGetProductsByInsured_NoEventsFound() {
        // Arrange
        String insuredId = "123";
        when(eventRepository.findByInsuredId(insuredId)).thenReturn(Collections.emptyList());

        // Act
        ResponseEntity<?> response = productsController.getProductsByInsured(insuredId);

        // Assert
        assertEquals(404, response.getStatusCodeValue());
    }

    @Test
    void testGetProductsByInsured_EventsWithProducts() {
        // Arrange
        String insuredId = "123";
        EventEntity event = new EventEntity();
        event.setId("event1");
        event.setInsuredId(insuredId);

        RequestDetailsEntity requestDetails = new RequestDetailsEntity();
        requestDetails.setSourceCompany("CompanyA");
        event.setRequestDetails(requestDetails);

        ProductEntity product = new ProductEntity();
        product.setId("prod1");
        product.setType("type1");
        product.setPrice(BigDecimal.valueOf(100.0));
        product.setStartDate(LocalDate.now());
        product.setEndDate(LocalDate.now().plusDays(30));
        event.setProducts(List.of(product));

        when(eventRepository.findByInsuredId(insuredId)).thenReturn(List.of(event));

        // Act
        ResponseEntity<?> response = productsController.getProductsByInsured(insuredId);

        // Assert
        assertEquals(200, response.getStatusCodeValue());
        ProductsController.InsuredProductsResponse body = (ProductsController.InsuredProductsResponse) response.getBody();
        assertNotNull(body);
        assertEquals(insuredId, body.insuredId);
        assertEquals(1, body.groups.size());
        assertEquals("CompanyA", body.groups.get(0).sourceCompany);
        assertEquals(1, body.groups.get(0).products.size());
        assertEquals("prod1", body.groups.get(0).products.get(0).id);
    }

    @Test
    void testGetProductsByInsured_EventsWithNullRequestDetails() {
        // Arrange
        String insuredId = "123";
        EventEntity event = new EventEntity();
        event.setId("event1");
        event.setInsuredId(insuredId);
        event.setRequestDetails(null); // null requestDetails

        ProductEntity product = new ProductEntity();
        product.setId("prod1");
        product.setType("type1");
        product.setPrice(BigDecimal.valueOf(100.0));
        product.setStartDate(LocalDate.now());
        product.setEndDate(LocalDate.now().plusDays(30));
        event.setProducts(List.of(product));

        when(eventRepository.findByInsuredId(insuredId)).thenReturn(List.of(event));

        // Act
        ResponseEntity<?> response = productsController.getProductsByInsured(insuredId);

        // Assert
        assertEquals(200, response.getStatusCodeValue());
        ProductsController.InsuredProductsResponse body = (ProductsController.InsuredProductsResponse) response.getBody();
        assertNotNull(body);
        assertEquals(insuredId, body.insuredId);
        assertEquals(1, body.groups.size());
        assertEquals("unknown", body.groups.get(0).sourceCompany); // should be "unknown"
        assertEquals(1, body.groups.get(0).products.size());
    }

    @Test
    void testGetProductsByInsured_EventsWithNullProducts() {
        // Arrange
        String insuredId = "123";
        EventEntity event = new EventEntity();
        event.setId("event1");
        event.setInsuredId(insuredId);

        RequestDetailsEntity requestDetails = new RequestDetailsEntity();
        requestDetails.setSourceCompany("CompanyA");
        event.setRequestDetails(requestDetails);
        event.setProducts(null); // null products

        when(eventRepository.findByInsuredId(insuredId)).thenReturn(List.of(event));

        // Act
        ResponseEntity<?> response = productsController.getProductsByInsured(insuredId);

        // Assert
        assertEquals(200, response.getStatusCodeValue());
        ProductsController.InsuredProductsResponse body = (ProductsController.InsuredProductsResponse) response.getBody();
        assertNotNull(body);
        assertEquals(insuredId, body.insuredId);
        assertEquals(1, body.groups.size());
        assertEquals("CompanyA", body.groups.get(0).sourceCompany);
        assertEquals(0, body.groups.get(0).products.size()); // no products added
    }

    @Test
    void testGetProductsByInsured_MultipleEventsMultipleCompanies() {
        // Arrange
        String insuredId = "123";

        EventEntity event1 = new EventEntity();
        event1.setId("event1");
        event1.setInsuredId(insuredId);
        RequestDetailsEntity rd1 = new RequestDetailsEntity();
        rd1.setSourceCompany("CompanyA");
        event1.setRequestDetails(rd1);
        ProductEntity p1 = new ProductEntity();
        p1.setId("prod1");
        p1.setType("type1");
        p1.setPrice(BigDecimal.valueOf(100.0));
        p1.setStartDate(LocalDate.now());
        p1.setEndDate(LocalDate.now().plusDays(30));
        event1.setProducts(List.of(p1));

        EventEntity event2 = new EventEntity();
        event2.setId("event2");
        event2.setInsuredId(insuredId);
        RequestDetailsEntity rd2 = new RequestDetailsEntity();
        rd2.setSourceCompany("CompanyB");
        event2.setRequestDetails(rd2);
        ProductEntity p2 = new ProductEntity();
        p2.setId("prod2");
        p2.setType("type2");
        p2.setPrice(BigDecimal.valueOf(200.0));
        p2.setStartDate(LocalDate.now());
        p2.setEndDate(LocalDate.now().plusDays(60));
        event2.setProducts(List.of(p2));

        when(eventRepository.findByInsuredId(insuredId)).thenReturn(List.of(event1, event2));

        // Act
        ResponseEntity<?> response = productsController.getProductsByInsured(insuredId);

        // Assert
        assertEquals(200, response.getStatusCodeValue());
        ProductsController.InsuredProductsResponse body = (ProductsController.InsuredProductsResponse) response.getBody();
        assertNotNull(body);
        assertEquals(insuredId, body.insuredId);
        assertEquals(2, body.groups.size());
        // Check groups are present
        boolean hasCompanyA = body.groups.stream().anyMatch(g -> "CompanyA".equals(g.sourceCompany) && g.products.size() == 1);
        boolean hasCompanyB = body.groups.stream().anyMatch(g -> "CompanyB".equals(g.sourceCompany) && g.products.size() == 1);
        assertTrue(hasCompanyA);
        assertTrue(hasCompanyB);
    }

    @Test
    void testGetProductsByInsured_EventWithMultipleProducts() {
        // Arrange
        String insuredId = "123";
        EventEntity event = new EventEntity();
        event.setId("event1");
        event.setInsuredId(insuredId);

        RequestDetailsEntity requestDetails = new RequestDetailsEntity();
        requestDetails.setSourceCompany("CompanyA");
        event.setRequestDetails(requestDetails);

        ProductEntity product1 = new ProductEntity();
        product1.setId("prod1");
        product1.setType("type1");
        product1.setPrice(BigDecimal.valueOf(100.0));
        product1.setStartDate(LocalDate.now());
        product1.setEndDate(LocalDate.now().plusDays(30));

        ProductEntity product2 = new ProductEntity();
        product2.setId("prod2");
        product2.setType("type2");
        product2.setPrice(BigDecimal.valueOf(150.0));
        product2.setStartDate(LocalDate.now());
        product2.setEndDate(LocalDate.now().plusDays(45));

        event.setProducts(List.of(product1, product2));

        when(eventRepository.findByInsuredId(insuredId)).thenReturn(List.of(event));

        // Act
        ResponseEntity<?> response = productsController.getProductsByInsured(insuredId);

        // Assert
        assertEquals(200, response.getStatusCodeValue());
        ProductsController.InsuredProductsResponse body = (ProductsController.InsuredProductsResponse) response.getBody();
        assertNotNull(body);
        assertEquals(insuredId, body.insuredId);
        assertEquals(1, body.groups.size());
        assertEquals("CompanyA", body.groups.get(0).sourceCompany);
        assertEquals(2, body.groups.get(0).products.size());
        assertTrue(body.groups.get(0).products.stream().anyMatch(p -> "prod1".equals(p.id)));
        assertTrue(body.groups.get(0).products.stream().anyMatch(p -> "prod2".equals(p.id)));
    }
}
