import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import org.example.entities.RequestDetailsEntity;
import org.example.repository.RequestDetailsRepository;
import org.example.service.*;
import org.example.entities.XmlRootRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.*;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class FileProcessorServiceTest {

    @Mock
    private RequestDetailsRepository requestDetailsRepository;

    @Mock
    private RequestDetailsService requestDetailsService;

    @Mock
    private XmlMapper xmlMapper;

    @Mock
    private BackupService backupService;

    private FileProcessorService fileProcessorService;

    private FileBatchProcessor fileBatchProcessor;

    private FileScheduledService fileScheduledService;

    private Path mockPath;
    private Path mockFilePath;

    @BeforeEach
    void setUp() {
        FileProcessorService realFileProcessorService = new FileProcessorService(requestDetailsRepository, requestDetailsService, backupService);
        fileProcessorService = spy(realFileProcessorService);
        ReflectionTestUtils.setField(fileProcessorService, "xmlMapper", xmlMapper);
        fileBatchProcessor = new FileBatchProcessor(fileProcessorService);
        Path testInput = Paths.get("test-input").toAbsolutePath();
        ReflectionTestUtils.setField(fileBatchProcessor, "inputDir", testInput.toString());
        Path testBackup = Paths.get("test-backup").toAbsolutePath();
        ReflectionTestUtils.setField(backupService, "backupDir", testBackup.toString());
        fileScheduledService = new FileScheduledService(fileBatchProcessor);
        mockPath = mock(Path.class);
        mockFilePath = mock(Path.class);
    }

    @Test
    void testPollFolder_Success() {
        try (MockedStatic<Files> mockedFiles = Mockito.mockStatic(Files.class);
             MockedStatic<Paths> mockedPaths = Mockito.mockStatic(Paths.class)) {
            mockedPaths.when(() -> Paths.get(anyString())).thenReturn(mockPath);
            mockedFiles.when(() -> Files.exists(mockPath)).thenReturn(true);
            DirectoryStream<Path> mockStream = mock(DirectoryStream.class);
            when(mockStream.iterator()).thenReturn(List.<Path>of().iterator());
            mockedFiles.when(() -> Files.newDirectoryStream(mockPath, "*.xml")).thenReturn(mockStream);

            fileScheduledService.Schedule();

            // Verify no exception
        }
    }

    @Test
    void testPollFolder_Exception() {
        try (MockedStatic<Files> mockedFiles = Mockito.mockStatic(Files.class);
             MockedStatic<Paths> mockedPaths = Mockito.mockStatic(Paths.class)) {
            mockedPaths.when(() -> Paths.get(anyString())).thenReturn(mockPath);
            mockedFiles.when(() -> Files.exists(mockPath)).thenThrow(new RuntimeException("Test exception"));

            fileScheduledService.Schedule();

            // Should catch and log
        }
    }

    @Test
    void testProcessFiles_DirectoryCreation() throws IOException {
        try (MockedStatic<Files> mockedFiles = Mockito.mockStatic(Files.class);
             MockedStatic<Paths> mockedPaths = Mockito.mockStatic(Paths.class)) {
            mockedPaths.when(() -> Paths.get(anyString())).thenReturn(mockPath);
               mockedFiles.when(() -> Files.exists(mockPath)).thenReturn(false);
            mockedFiles.when(() -> Files.createDirectories(mockPath)).thenReturn(mockPath);
            DirectoryStream<Path> mockStream = mock(DirectoryStream.class);
            when(mockStream.iterator()).thenReturn(List.<Path>of().iterator());
            mockedFiles.when(() -> Files.newDirectoryStream(mockPath, "*.xml")).thenReturn(mockStream);

            fileBatchProcessor.processFiles();

            mockedFiles.verify(() -> Files.createDirectories(mockPath));
        }
    }

    @Test
    void testProcessFiles_IOException() throws IOException {
        try (MockedStatic<Files> mockedFiles = Mockito.mockStatic(Files.class);
             MockedStatic<Paths> mockedPaths = Mockito.mockStatic(Paths.class)) {
            mockedPaths.when(() -> Paths.get(anyString())).thenReturn(mockPath);
                     mockedFiles.when(() -> Files.exists(mockPath)).thenReturn(true);
            DirectoryStream<Path> mockStream = mock(DirectoryStream.class);
            when(mockStream.iterator()).thenReturn(List.<Path>of().iterator());
            doThrow(new IOException("Test IO")).when(mockStream).close();
            mockedFiles.when(() -> Files.newDirectoryStream(mockPath, "*.xml")).thenReturn(mockStream);

            fileBatchProcessor.processFiles();

            // Should log error
        }
    }

    @Test
    void testProcessFiles_DirectoryExists() throws IOException {
        try (MockedStatic<Files> mockedFiles = Mockito.mockStatic(Files.class);
             MockedStatic<Paths> mockedPaths = Mockito.mockStatic(Paths.class)) {
            mockedPaths.when(() -> Paths.get(anyString())).thenReturn(mockPath);
            mockedFiles.when(() -> Files.exists(mockPath)).thenReturn(true);
            DirectoryStream<Path> mockStream = mock(DirectoryStream.class);
            when(mockStream.iterator()).thenReturn(List.<Path>of().iterator());
            mockedFiles.when(() -> Files.newDirectoryStream(mockPath, "*.xml")).thenReturn(mockStream);

            fileBatchProcessor.processFiles();

            mockedFiles.verify(() -> Files.createDirectories(mockPath), never());
        }
    }

    @Test
    void testProcessFiles_WithFiles() throws IOException {
        try (MockedStatic<Files> mockedFiles = Mockito.mockStatic(Files.class);
             MockedStatic<Paths> mockedPaths = Mockito.mockStatic(Paths.class)) {
            mockedPaths.when(() -> Paths.get(anyString())).thenReturn(mockPath);
            lenient().when(mockPath.resolve(any(Path.class))).thenReturn(mockPath);
            mockedFiles.when(() -> Files.exists(mockPath)).thenReturn(true);
            DirectoryStream<Path> mockStream = mock(DirectoryStream.class);
            when(mockStream.iterator()).thenReturn(List.of(mockFilePath).iterator());
            mockedFiles.when(() -> Files.newDirectoryStream(mockPath, "*.xml")).thenReturn(mockStream);

            mockedFiles.when(() -> Files.exists(mockFilePath)).thenReturn(true);
            mockedFiles.when(() -> Files.isRegularFile(mockFilePath)).thenReturn(true);
            mockedFiles.when(() -> Files.isReadable(mockFilePath)).thenReturn(true);
            InputStream mockIs = mock(InputStream.class);
            mockedFiles.when(() -> Files.newInputStream(any(Path.class), any())).thenReturn(mockIs);
            when(xmlMapper.readValue(any(InputStream.class), eq(XmlRootRequest.class))).thenReturn(null);
            lenient().when(mockPath.resolve(any(Path.class))).thenReturn(mockPath);
            mockedFiles.when(() -> Files.move(any(Path.class), any(Path.class))).then(invocation -> null);

            fileBatchProcessor.processFiles();

            verify(xmlMapper).readValue(any(InputStream.class), eq(XmlRootRequest.class));
        }
    }

    @Test
    void testProcessFiles_WithFiles_NullRoot() throws IOException {
        try (MockedStatic<Files> mockedFiles = Mockito.mockStatic(Files.class);
             MockedStatic<Paths> mockedPaths = Mockito.mockStatic(Paths.class)) {
            mockedPaths.when(() -> Paths.get(anyString())).thenReturn(mockPath);
            mockedFiles.when(() -> Files.exists(mockPath)).thenReturn(true);
            DirectoryStream<Path> mockStream = mock(DirectoryStream.class);
            when(mockStream.iterator()).thenReturn(List.of(mockFilePath).iterator());
            mockedFiles.when(() -> Files.newDirectoryStream(mockPath, "*.xml")).thenReturn(mockStream);

            mockedFiles.when(() -> Files.exists(mockFilePath)).thenReturn(true);
            mockedFiles.when(() -> Files.isRegularFile(mockFilePath)).thenReturn(true);
            mockedFiles.when(() -> Files.isReadable(mockFilePath)).thenReturn(true);
            InputStream mockIs = mock(InputStream.class);
            mockedFiles.when(() -> Files.newInputStream(any(Path.class), any())).thenReturn(mockIs);
            when(xmlMapper.readValue(any(InputStream.class), eq(XmlRootRequest.class))).thenReturn(null);
            mockedFiles.when(() -> Files.move(any(Path.class), any(Path.class))).then(invocation -> null);

            fileBatchProcessor.processFiles();

            verify(requestDetailsRepository, never()).save(any());
        }
    }

    @Test
    void testProcessFiles_WithFiles_NullRequestDetails() throws IOException {
        try (MockedStatic<Files> mockedFiles = Mockito.mockStatic(Files.class);
             MockedStatic<Paths> mockedPaths = Mockito.mockStatic(Paths.class)) {
            mockedPaths.when(() -> Paths.get(anyString())).thenReturn(mockPath);
            lenient().when(mockPath.resolve(any(Path.class))).thenReturn(mockPath);
            mockedFiles.when(() -> Files.exists(mockPath)).thenReturn(true);
            DirectoryStream<Path> mockStream = mock(DirectoryStream.class);
            when(mockStream.iterator()).thenReturn(List.of(mockFilePath).iterator());
            mockedFiles.when(() -> Files.newDirectoryStream(mockPath, "*.xml")).thenReturn(mockStream);

            mockedFiles.when(() -> Files.exists(mockFilePath)).thenReturn(true);
            mockedFiles.when(() -> Files.isRegularFile(mockFilePath)).thenReturn(true);
            mockedFiles.when(() -> Files.isReadable(mockFilePath)).thenReturn(true);
            InputStream mockIs = mock(InputStream.class);
            mockedFiles.when(() -> Files.newInputStream(any(Path.class), any())).thenReturn(mockIs);
            XmlRootRequest root = new XmlRootRequest();
            root.setRequestDetails(null);

            fileBatchProcessor.processFiles();

            verify(requestDetailsRepository, never()).save(any());
        }
    }

    @Test
    void testProcessFiles_WithFiles_ReadException() throws IOException {
        try (MockedStatic<Files> mockedFiles = Mockito.mockStatic(Files.class);
             MockedStatic<Paths> mockedPaths = Mockito.mockStatic(Paths.class)) {
            mockedPaths.when(() -> Paths.get(anyString())).thenReturn(mockPath);
            lenient().when(mockPath.resolve(any(Path.class))).thenReturn(mockPath);
            mockedFiles.when(() -> Files.exists(mockPath)).thenReturn(true);
            DirectoryStream<Path> mockStream = mock(DirectoryStream.class);
            when(mockStream.iterator()).thenReturn(List.of(mockFilePath).iterator());
            mockedFiles.when(() -> Files.newDirectoryStream(mockPath, "*.xml")).thenReturn(mockStream);

            mockedFiles.when(() -> Files.exists(mockFilePath)).thenReturn(true);
            mockedFiles.when(() -> Files.isRegularFile(mockFilePath)).thenReturn(true);
            mockedFiles.when(() -> Files.isReadable(mockFilePath)).thenReturn(true);
            InputStream mockIs = mock(InputStream.class);
            mockedFiles.when(() -> Files.newInputStream(any(Path.class), any())).thenReturn(mockIs);
            when(xmlMapper.readValue(any(InputStream.class), eq(XmlRootRequest.class))).thenThrow(new IOException("Read error"));
            mockedFiles.when(() -> Files.move(any(Path.class), any(Path.class))).then(invocation -> null);

            fileBatchProcessor.processFiles();

            verify(requestDetailsRepository, never()).save(any());
        }
    }

    @Test
    void testProcessFiles_WithFiles_Successful() throws IOException {
        when(mockFilePath.getFileName()).thenReturn(mockPath);
        try (MockedStatic<Files> mockedFiles = Mockito.mockStatic(Files.class);
             MockedStatic<Paths> mockedPaths = Mockito.mockStatic(Paths.class)) {
            mockedPaths.when(() -> Paths.get(anyString())).thenReturn(mockPath);
            lenient().when(mockPath.resolve(any(Path.class))).thenReturn(mockPath);
            mockedFiles.when(() -> Files.exists(mockPath)).thenReturn(true);
            DirectoryStream<Path> mockStream = mock(DirectoryStream.class);
            when(mockStream.iterator()).thenReturn(List.of(mockFilePath).iterator());
            mockedFiles.when(() -> Files.newDirectoryStream(mockPath, "*.xml")).thenReturn(mockStream);

            mockedFiles.when(() -> Files.exists(mockFilePath)).thenReturn(true);
            mockedFiles.when(() -> Files.isRegularFile(mockFilePath)).thenReturn(true);
            mockedFiles.when(() -> Files.isReadable(mockFilePath)).thenReturn(true);
            InputStream mockIs = mock(InputStream.class);
            mockedFiles.when(() -> Files.newInputStream(any(Path.class), any())).thenReturn(mockIs);
            XmlRootRequest root = new XmlRootRequest();
            org.example.dto.RequestDetailsDto rdDto = new org.example.dto.RequestDetailsDto();
            rdDto.setId("rd1");
            rdDto.setAcceptDate("2023-01-01 12:00:00.000000000");
            rdDto.setSourceCompany("CompanyA");
            root.setRequestDetails(rdDto);
            root.setEvents(List.of());
            when(xmlMapper.readValue(any(InputStream.class), eq(XmlRootRequest.class))).thenReturn(root);
            RequestDetailsEntity entity = new RequestDetailsEntity();
            when(requestDetailsService.getRequestDetailsEntity(root)).thenReturn(entity);
            mockedFiles.when(() -> Files.move(any(Path.class), any(Path.class))).then(invocation -> null);

            fileBatchProcessor.processFiles();

            verify(requestDetailsRepository).save(entity);
        }
    }

    @Test
    void testProcessFiles_WithFiles_AccessDenied() throws IOException {
         try (MockedStatic<Files> mockedFiles = Mockito.mockStatic(Files.class);
             MockedStatic<Paths> mockedPaths = Mockito.mockStatic(Paths.class)) {
            mockedPaths.when(() -> Paths.get(anyString())).thenReturn(mockPath);
            lenient().when(mockPath.resolve(any(Path.class))).thenReturn(mockPath);
            mockedFiles.when(() -> Files.exists(mockPath)).thenReturn(true);
            DirectoryStream<Path> mockStream = mock(DirectoryStream.class);
            when(mockStream.iterator()).thenReturn(List.of(mockFilePath).iterator());
            mockedFiles.when(() -> Files.newDirectoryStream(mockPath, "*.xml")).thenReturn(mockStream);

            mockedFiles.when(() -> Files.exists(mockFilePath)).thenReturn(true);
            mockedFiles.when(() -> Files.isRegularFile(mockFilePath)).thenReturn(true);
            mockedFiles.when(() -> Files.isReadable(mockFilePath)).thenReturn(true);
            mockedFiles.when(() -> Files.newInputStream(any(Path.class), any())).thenThrow(new AccessDeniedException("Access denied"));
            mockedFiles.when(() -> Files.move(any(Path.class), any(Path.class))).then(invocation -> null);

             fileBatchProcessor.processFiles();

            verify(requestDetailsRepository, never()).save(any());
        }
    }

    @Test
    void testProcessFiles_WithFiles_GeneralException() throws IOException {
        try (MockedStatic<Files> mockedFiles = Mockito.mockStatic(Files.class);
             MockedStatic<Paths> mockedPaths = Mockito.mockStatic(Paths.class)) {
            mockedPaths.when(() -> Paths.get(anyString())).thenReturn(mockPath);
            lenient().when(mockPath.resolve(any(Path.class))).thenReturn(mockPath);
            mockedFiles.when(() -> Files.exists(mockPath)).thenReturn(true);
            DirectoryStream<Path> mockStream = mock(DirectoryStream.class);
            when(mockStream.iterator()).thenReturn(List.of(mockFilePath).iterator());
            mockedFiles.when(() -> Files.newDirectoryStream(mockPath, "*.xml")).thenReturn(mockStream);

            mockedFiles.when(() -> Files.exists(mockFilePath)).thenReturn(true);
            mockedFiles.when(() -> Files.isRegularFile(mockFilePath)).thenReturn(true);
            mockedFiles.when(() -> Files.isReadable(mockFilePath)).thenReturn(true);
            mockedFiles.when(() -> Files.newInputStream(any(Path.class), any())).thenThrow(new RuntimeException("General error"));
            mockedFiles.when(() -> Files.move(any(Path.class), any(Path.class))).then(invocation -> null);

            fileBatchProcessor.processFiles();

            verify(requestDetailsRepository, never()).save(any());
        }
    }

    @Test
    void testProcessFiles_WithFiles_UnreadableFile() throws IOException {
      try (MockedStatic<Files> mockedFiles = Mockito.mockStatic(Files.class);
             MockedStatic<Paths> mockedPaths = Mockito.mockStatic(Paths.class)) {
            mockedPaths.when(() -> Paths.get(anyString())).thenReturn(mockPath);

            lenient().when(mockPath.resolve(any(Path.class))).thenReturn(mockPath);
            mockedFiles.when(() -> Files.exists(mockPath)).thenReturn(true);
            DirectoryStream<Path> mockStream = mock(DirectoryStream.class);
            when(mockStream.iterator()).thenReturn(List.of(mockFilePath).iterator());
            mockedFiles.when(() -> Files.newDirectoryStream(mockPath, "*.xml")).thenReturn(mockStream);

            mockedFiles.when(() -> Files.exists(mockFilePath)).thenReturn(false); // File not exists
            fileBatchProcessor.processFiles();

            verify(requestDetailsRepository, never()).save(any());
        }
    }
}
