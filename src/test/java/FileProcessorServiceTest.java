import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import org.example.model.RequestDetailsEntity;
import org.example.repository.RequestDetailsRepository;
import org.example.service.FileProcessorService;
import org.example.xml.RootRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.nio.file.*;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class FileProcessorServiceTest {

    @Mock
    private RequestDetailsRepository requestDetailsRepository;

    @Mock
    private XmlMapper xmlMapper;

    @InjectMocks
    private FileProcessorService fileProcessorService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(fileProcessorService, "inputDir", "./test-input");
        ReflectionTestUtils.setField(fileProcessorService, "backupDir", "./test-backup");
    }

    @Test
    void testPollFolder_Success() {
        // Mock processFiles to do nothing
        try (MockedStatic<Files> mockedFiles = Mockito.mockStatic(Files.class)) {
            mockedFiles.when(() -> Files.exists(any(Path.class))).thenReturn(true);
            mockedFiles.when(() -> Files.newDirectoryStream(any(Path.class), any(String.class))).thenReturn(mock(DirectoryStream.class));

            fileProcessorService.pollFolder();

            // Verify processFiles was called
            // Since it's private, we can't directly verify, but no exception should be thrown
        }
    }

    @Test
    void testPollFolder_Exception() {
        // Mock processFiles to throw exception
        try (MockedStatic<Files> mockedFiles = Mockito.mockStatic(Files.class)) {
            mockedFiles.when(() -> Files.exists(any(Path.class))).thenThrow(new RuntimeException("Test exception"));

            fileProcessorService.pollFolder();

            // Should catch and log, no rethrow
        }
    }

    @Test
    void testProcessFiles_DirectoryCreation() throws IOException {
        try (MockedStatic<Files> mockedFiles = Mockito.mockStatic(Files.class);
             MockedStatic<Paths> mockedPaths = Mockito.mockStatic(Paths.class)) {
            Path mockPath = mock(Path.class);
            when(mockPath.isAbsolute()).thenReturn(true); // Make it absolute to skip resolve
            mockedPaths.when(() -> Paths.get(anyString())).thenReturn(mockPath);
            mockedFiles.when(() -> Files.exists(mockPath)).thenReturn(false);
            mockedFiles.when(() -> Files.createDirectories(mockPath)).thenReturn(mockPath);
            DirectoryStream<Path> mockStream = mock(DirectoryStream.class);
            when(mockStream.iterator()).thenReturn(List.<Path>of().iterator());
            mockedFiles.when(() -> Files.newDirectoryStream(mockPath, "*.xml")).thenReturn(mockStream);

            fileProcessorService.processFiles();

            mockedFiles.verify(() -> Files.createDirectories(mockPath));
        }
    }

    @Test
    void testProcessFiles_IOException() throws IOException {
        try (MockedStatic<Files> mockedFiles = Mockito.mockStatic(Files.class);
             MockedStatic<Paths> mockedPaths = Mockito.mockStatic(Paths.class)) {
            Path mockPath = mock(Path.class);
            when(mockPath.isAbsolute()).thenReturn(true);
            mockedPaths.when(() -> Paths.get(anyString())).thenReturn(mockPath);
            mockedFiles.when(() -> Files.exists(mockPath)).thenReturn(true);
            // Mock newDirectoryStream to throw IOException to trigger the catch
            mockedFiles.when(() -> Files.newDirectoryStream(mockPath, "*.xml")).thenThrow(new IOException("Test IO"));

            fileProcessorService.processFiles();

            // Should log error
        }
    }

    @Test
    void testProcessFiles_DirectoryExists() throws IOException {
        try (MockedStatic<Files> mockedFiles = Mockito.mockStatic(Files.class);
             MockedStatic<Paths> mockedPaths = Mockito.mockStatic(Paths.class)) {
            Path mockPath = mock(Path.class);
            when(mockPath.isAbsolute()).thenReturn(true);
            mockedPaths.when(() -> Paths.get(anyString())).thenReturn(mockPath);
            mockedFiles.when(() -> Files.exists(mockPath)).thenReturn(true); // Directory exists
            DirectoryStream<Path> mockStream = mock(DirectoryStream.class);
            when(mockStream.iterator()).thenReturn(List.<Path>of().iterator());
            mockedFiles.when(() -> Files.newDirectoryStream(mockPath, "*.xml")).thenReturn(mockStream);

            fileProcessorService.processFiles();

            // Verify createDirectories was not called
            mockedFiles.verify(() -> Files.createDirectories(mockPath), never());
        }
    }

    @Test
    void testProcessFiles_WithFiles() throws IOException {
        try (MockedStatic<Files> mockedFiles = Mockito.mockStatic(Files.class);
             MockedStatic<Paths> mockedPaths = Mockito.mockStatic(Paths.class)) {
            Path mockPath = mock(Path.class);
            when(mockPath.isAbsolute()).thenReturn(true);
            mockedPaths.when(() -> Paths.get(anyString())).thenReturn(mockPath);
            mockedFiles.when(() -> Files.exists(mockPath)).thenReturn(true);
            DirectoryStream<Path> mockStream = mock(DirectoryStream.class);
            Path mockFilePath = mock(Path.class);
            when(mockFilePath.toAbsolutePath()).thenReturn(mockFilePath);
            when(mockStream.iterator()).thenReturn(List.of(mockFilePath).iterator());
            mockedFiles.when(() -> Files.newDirectoryStream(mockPath, "*.xml")).thenReturn(mockStream);

            // Mock for processSingleFile
            mockedFiles.when(() -> Files.exists(mockFilePath)).thenReturn(true);
            mockedFiles.when(() -> Files.isRegularFile(mockFilePath)).thenReturn(true);
            mockedFiles.when(() -> Files.isReadable(mockFilePath)).thenReturn(true);
            mockedFiles.when(() -> Files.newInputStream(any(Path.class), any())).thenReturn(mock(java.io.InputStream.class));
            when(xmlMapper.readValue(any(java.io.InputStream.class), eq(org.example.xml.RootRequest.class))).thenReturn(null); // Skip processing

            fileProcessorService.processFiles();

            // Verify processSingleFile was called (indirectly)
            verify(xmlMapper).readValue(any(java.io.InputStream.class), eq(org.example.xml.RootRequest.class));
        }
    }

    @Test
    void testProcessFiles_WithFiles_NullRoot() throws IOException {
        try (MockedStatic<Files> mockedFiles = Mockito.mockStatic(Files.class);
             MockedStatic<Paths> mockedPaths = Mockito.mockStatic(Paths.class)) {
            Path mockPath = mock(Path.class);
            when(mockPath.isAbsolute()).thenReturn(true);
            mockedPaths.when(() -> Paths.get(anyString())).thenReturn(mockPath);
            mockedFiles.when(() -> Files.exists(mockPath)).thenReturn(true);
            DirectoryStream<Path> mockStream = mock(DirectoryStream.class);
            Path mockFilePath = mock(Path.class);
            when(mockFilePath.toAbsolutePath()).thenReturn(mockFilePath);
            when(mockStream.iterator()).thenReturn(List.of(mockFilePath).iterator());
            mockedFiles.when(() -> Files.newDirectoryStream(mockPath, "*.xml")).thenReturn(mockStream);

            // Mock for processSingleFile: null root
            mockedFiles.when(() -> Files.exists(mockFilePath)).thenReturn(true);
            mockedFiles.when(() -> Files.isRegularFile(mockFilePath)).thenReturn(true);
            mockedFiles.when(() -> Files.isReadable(mockFilePath)).thenReturn(true);
            mockedFiles.when(() -> Files.newInputStream(any(Path.class), any())).thenReturn(mock(java.io.InputStream.class));
            when(xmlMapper.readValue(any(java.io.InputStream.class), eq(RootRequest.class))).thenReturn(null);

            fileProcessorService.processFiles();

            verify(requestDetailsRepository, never()).save(any());
        }
    }

    @Test
    void testProcessFiles_WithFiles_NullRequestDetails() throws IOException {
        try (MockedStatic<Files> mockedFiles = Mockito.mockStatic(Files.class);
             MockedStatic<Paths> mockedPaths = Mockito.mockStatic(Paths.class)) {
            Path mockPath = mock(Path.class);
            when(mockPath.isAbsolute()).thenReturn(true);
            mockedPaths.when(() -> Paths.get(anyString())).thenReturn(mockPath);
            mockedFiles.when(() -> Files.exists(mockPath)).thenReturn(true);
            DirectoryStream<Path> mockStream = mock(DirectoryStream.class);
            Path mockFilePath = mock(Path.class);
            when(mockFilePath.toAbsolutePath()).thenReturn(mockFilePath);
            when(mockStream.iterator()).thenReturn(List.of(mockFilePath).iterator());
            mockedFiles.when(() -> Files.newDirectoryStream(mockPath, "*.xml")).thenReturn(mockStream);

            // Mock for processSingleFile: null requestDetails
            mockedFiles.when(() -> Files.exists(mockFilePath)).thenReturn(true);
            mockedFiles.when(() -> Files.isRegularFile(mockFilePath)).thenReturn(true);
            mockedFiles.when(() -> Files.isReadable(mockFilePath)).thenReturn(true);
            mockedFiles.when(() -> Files.newInputStream(any(Path.class), any())).thenReturn(mock(java.io.InputStream.class));
            RootRequest root = new RootRequest();
            root.setRequestDetails(null);
            when(xmlMapper.readValue(any(java.io.InputStream.class), eq(RootRequest.class))).thenReturn(root);

            fileProcessorService.processFiles();

            verify(requestDetailsRepository, never()).save(any());
        }
    }

    @Test
    void testProcessFiles_WithFiles_Successful() throws IOException {
        try (MockedStatic<Files> mockedFiles = Mockito.mockStatic(Files.class);
             MockedStatic<Paths> mockedPaths = Mockito.mockStatic(Paths.class)) {
            Path mockPath = mock(Path.class);
            when(mockPath.isAbsolute()).thenReturn(true);
            mockedPaths.when(() -> Paths.get(anyString())).thenReturn(mockPath);
            mockedFiles.when(() -> Files.exists(mockPath)).thenReturn(true);
            DirectoryStream<Path> mockStream = mock(DirectoryStream.class);
            Path mockFilePath = mock(Path.class);
            when(mockFilePath.toAbsolutePath()).thenReturn(mockFilePath);
            when(mockStream.iterator()).thenReturn(List.of(mockFilePath).iterator());
            mockedFiles.when(() -> Files.newDirectoryStream(mockPath, "*.xml")).thenReturn(mockStream);

            // Mock for processSingleFile: successful processing
            mockedFiles.when(() -> Files.exists(mockFilePath)).thenReturn(true);
            mockedFiles.when(() -> Files.isRegularFile(mockFilePath)).thenReturn(true);
            mockedFiles.when(() -> Files.isReadable(mockFilePath)).thenReturn(true);
            mockedFiles.when(() -> Files.newInputStream(any(Path.class), any())).thenReturn(mock(java.io.InputStream.class));
            RootRequest root = new RootRequest();
            org.example.xml.RequestDetailsDto rdDto = new org.example.xml.RequestDetailsDto();
            rdDto.setId("rd1");
            rdDto.setAcceptDate("2023-01-01 12:00:00.000000000");
            rdDto.setSourceCompany("CompanyA");
            root.setRequestDetails(rdDto);
            root.setEvents(List.of());
            when(xmlMapper.readValue(any(java.io.InputStream.class), eq(RootRequest.class))).thenReturn(root);

            fileProcessorService.processFiles();

            verify(requestDetailsRepository).save(any(RequestDetailsEntity.class));
        }
    }

    @Test
    void testProcessFiles_WithFiles_DateParseFailure() throws IOException {
        try (MockedStatic<Files> mockedFiles = Mockito.mockStatic(Files.class);
             MockedStatic<Paths> mockedPaths = Mockito.mockStatic(Paths.class)) {
            Path mockPath = mock(Path.class);
            when(mockPath.isAbsolute()).thenReturn(true);
            mockedPaths.when(() -> Paths.get(anyString())).thenReturn(mockPath);
            mockedFiles.when(() -> Files.exists(mockPath)).thenReturn(true);
            DirectoryStream<Path> mockStream = mock(DirectoryStream.class);
            Path mockFilePath = mock(Path.class);
            when(mockFilePath.toAbsolutePath()).thenReturn(mockFilePath);
            when(mockStream.iterator()).thenReturn(List.of(mockFilePath).iterator());
            mockedFiles.when(() -> Files.newDirectoryStream(mockPath, "*.xml")).thenReturn(mockStream);

            // Mock for processSingleFile: date parse failure
            mockedFiles.when(() -> Files.exists(mockFilePath)).thenReturn(true);
            mockedFiles.when(() -> Files.isRegularFile(mockFilePath)).thenReturn(true);
            mockedFiles.when(() -> Files.isReadable(mockFilePath)).thenReturn(true);
            mockedFiles.when(() -> Files.newInputStream(any(Path.class), any())).thenReturn(mock(java.io.InputStream.class));
            RootRequest root = new RootRequest();
            org.example.xml.RequestDetailsDto rdDto = new org.example.xml.RequestDetailsDto();
            rdDto.setId("rd1");
            rdDto.setAcceptDate("invalid-date");
            rdDto.setSourceCompany("CompanyA");
            root.setRequestDetails(rdDto);
            root.setEvents(List.of());
            when(xmlMapper.readValue(any(java.io.InputStream.class), eq(RootRequest.class))).thenReturn(root);

            fileProcessorService.processFiles();

            verify(requestDetailsRepository).save(any(RequestDetailsEntity.class));
        }
    }

    @Test
    void testProcessFiles_WithFiles_ProductParseFailure() throws IOException {
        try (MockedStatic<Files> mockedFiles = Mockito.mockStatic(Files.class);
             MockedStatic<Paths> mockedPaths = Mockito.mockStatic(Paths.class)) {
            Path mockPath = mock(Path.class);
            when(mockPath.isAbsolute()).thenReturn(true);
            mockedPaths.when(() -> Paths.get(anyString())).thenReturn(mockPath);
            mockedFiles.when(() -> Files.exists(mockPath)).thenReturn(true);
            DirectoryStream<Path> mockStream = mock(DirectoryStream.class);
            Path mockFilePath = mock(Path.class);
            when(mockFilePath.toAbsolutePath()).thenReturn(mockFilePath);
            when(mockStream.iterator()).thenReturn(List.of(mockFilePath).iterator());
            mockedFiles.when(() -> Files.newDirectoryStream(mockPath, "*.xml")).thenReturn(mockStream);

            // Mock for processSingleFile: product parse failure
            mockedFiles.when(() -> Files.exists(mockFilePath)).thenReturn(true);
            mockedFiles.when(() -> Files.isRegularFile(mockFilePath)).thenReturn(true);
            mockedFiles.when(() -> Files.isReadable(mockFilePath)).thenReturn(true);
            mockedFiles.when(() -> Files.newInputStream(any(Path.class), any())).thenReturn(mock(java.io.InputStream.class));
            RootRequest root = new RootRequest();
            org.example.xml.RequestDetailsDto rdDto = new org.example.xml.RequestDetailsDto();
            rdDto.setId("rd1");
            rdDto.setAcceptDate("2023-01-01 12:00:00.000000000");
            rdDto.setSourceCompany("CompanyA");
            root.setRequestDetails(rdDto);
            org.example.xml.EventDto eventDto = new org.example.xml.EventDto();
            eventDto.setId("e1");
            eventDto.setType("type1");
            eventDto.setInsuredId("insured1");
            org.example.xml.ProductDto productDto = new org.example.xml.ProductDto();
            productDto.setType("prodType");
            productDto.setPrice("invalid-price");
            productDto.setStartDate("invalid-date");
            productDto.setEndDate("invalid-date");
            eventDto.setProducts(List.of(productDto));
            root.setEvents(List.of(eventDto));
            when(xmlMapper.readValue(any(java.io.InputStream.class), eq(RootRequest.class))).thenReturn(root);

            fileProcessorService.processFiles();

            verify(requestDetailsRepository).save(any(RequestDetailsEntity.class));
        }
    }

    @Test
    void testProcessFiles_WithFiles_AccessDenied() throws IOException {
        try (MockedStatic<Files> mockedFiles = Mockito.mockStatic(Files.class);
             MockedStatic<Paths> mockedPaths = Mockito.mockStatic(Paths.class)) {
            Path mockPath = mock(Path.class);
            when(mockPath.isAbsolute()).thenReturn(true);
            mockedPaths.when(() -> Paths.get(anyString())).thenReturn(mockPath);
            mockedFiles.when(() -> Files.exists(mockPath)).thenReturn(true);
            DirectoryStream<Path> mockStream = mock(DirectoryStream.class);
            Path mockFilePath = mock(Path.class);
            when(mockFilePath.toAbsolutePath()).thenReturn(mockFilePath);
            when(mockStream.iterator()).thenReturn(List.of(mockFilePath).iterator());
            mockedFiles.when(() -> Files.newDirectoryStream(mockPath, "*.xml")).thenReturn(mockStream);

            // Mock for processSingleFile: AccessDeniedException
            mockedFiles.when(() -> Files.exists(mockFilePath)).thenReturn(true);
            mockedFiles.when(() -> Files.isRegularFile(mockFilePath)).thenReturn(true);
            mockedFiles.when(() -> Files.isReadable(mockFilePath)).thenReturn(true);
            mockedFiles.when(() -> Files.newInputStream(any(Path.class), any())).thenThrow(new AccessDeniedException("Access denied"));

            fileProcessorService.processFiles();

            // Should not save
            verify(requestDetailsRepository, never()).save(any());
        }
    }

    @Test
    void testProcessFiles_WithFiles_GeneralException() throws IOException {
        try (MockedStatic<Files> mockedFiles = Mockito.mockStatic(Files.class);
             MockedStatic<Paths> mockedPaths = Mockito.mockStatic(Paths.class)) {
            Path mockPath = mock(Path.class);
            when(mockPath.isAbsolute()).thenReturn(true);
            mockedPaths.when(() -> Paths.get(anyString())).thenReturn(mockPath);
            mockedFiles.when(() -> Files.exists(mockPath)).thenReturn(true);
            DirectoryStream<Path> mockStream = mock(DirectoryStream.class);
            Path mockFilePath = mock(Path.class);
            when(mockFilePath.toAbsolutePath()).thenReturn(mockFilePath);
            when(mockStream.iterator()).thenReturn(List.of(mockFilePath).iterator());
            mockedFiles.when(() -> Files.newDirectoryStream(mockPath, "*.xml")).thenReturn(mockStream);

            // Mock for processSingleFile: general exception
            mockedFiles.when(() -> Files.exists(mockFilePath)).thenReturn(true);
            mockedFiles.when(() -> Files.isRegularFile(mockFilePath)).thenReturn(true);
            mockedFiles.when(() -> Files.isReadable(mockFilePath)).thenReturn(true);
            mockedFiles.when(() -> Files.newInputStream(any(Path.class), any())).thenThrow(new RuntimeException("General error"));

            fileProcessorService.processFiles();

            // Should not save
            verify(requestDetailsRepository, never()).save(any());
        }
    }
}
