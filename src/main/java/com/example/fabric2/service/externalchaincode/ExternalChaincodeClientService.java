package com.example.fabric2.service.externalchaincode;

import com.example.fabric2.dto.ExternalChaincodeConnection;
import com.example.fabric2.dto.ExternalChaincodeMetadata;
import com.example.fabric2.dto.SdkAgentConnection;
import com.example.fabric2.service.management.PortAssigner;
import com.example.fabric2.service.tar.Tar;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.vavr.collection.HashMap;
import io.vavr.control.Try;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.log4j.Log4j2;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

@Service
@Log4j2
@AllArgsConstructor
public class ExternalChaincodeClientService {

    private final ObjectMapper objectMapper;
    private final ExternalChaincodeHostService chaincodeServerService;
    private final PortAssigner portAssigner;
    private final Tar tar;

    @Data
    public static class Result {
        private int resultCode;
    }


    public Flux<String> installExternalChaincode(ExternalChaincodeMetadata metadata, SdkAgentConnection sdkAgentConnection) {


        return Flux.from(portAssigner.assignRemotePort(sdkAgentConnection)
                .map(chaincodePort -> prepareConnectionJson(chaincodePort, sdkAgentConnection)))
                .map(connection -> preparePackageFromMetadatandConnectionInStream(metadata, connection))
                .flatMap(packageInputStream -> chaincodeServerService.installChaincodeFromPackage(packageInputStream));

//        Mono<String> stringMono = webClient.post().uri(sdkAgentConnection.getAddress())
//                .contentType(MediaType.MULTIPART_FORM_DATA)
//                .body(BodyInserters.fromMultipartData(buildMultipartBody(filePartFlux, metadata)))
//                .retrieve()
//                .bodyToMono(String.class);
//        return stringMono;
    }

    private InputStream preparePackageFromMetadatandConnectionInStream(ExternalChaincodeMetadata metadata, ExternalChaincodeConnection connection) {
        metadata.setConnection(connection);
        return Try.of(() -> tar.createTar(HashMap.of("metadata.json", objectMapper.writeValueAsString(metadata).getBytes())))
                .getOrElseThrow(e -> new RuntimeException("Error serializing json:" + metadata.toString(), e));
    }

    private ExternalChaincodeConnection prepareConnectionJson(Integer chaincodePort, SdkAgentConnection sdkAgentConnection) {
        String chaincodeAddress = sdkAgentConnection.getAddress() + ":" + chaincodePort;
        return ExternalChaincodeConnection.of(chaincodeAddress, "TODO");
    }

    public Flux<Path> packageExternalChaincode(ExternalChaincodeMetadata metadata, ExternalChaincodeConnection connection,
                                               Flux<DataBuffer> fileDataBuffer) {

        metadata.setConnection(connection);

        return Try.of(() -> Files.createTempDirectory("packageTest"))
                .map(tmpPath -> preparePackage(tmpPath, metadata, fileDataBuffer))
                .getOrElseThrow(e -> new RuntimeException("Error at packaging chaincode", e));

//        ;
//        Files.write(testDir.resolve("test.txt"), objectMapper.writeValueAsString(metadata).getBytes());

    }

    private Flux<Path> preparePackage(Path tmpPath, ExternalChaincodeMetadata metadata, Flux<DataBuffer> fileDataBuffer) {
//        DataBufferUtils.write(Files.newOutputStream(tmpPath.resolve("code.tar.gz")));
//        writeMetadataToFile(tmpPath, metadata);

/*        String packageName=metadata.getLabel()+"tar.gz";

        OutputStream outputStream = Files.newOutputStream(tmpPath.resolve(packageName));

        TarArchiveOutputStream archiveOutputStream = new TarArchiveOutputStream(new GzipCompressorOutputStream(outputStream));

        archiveOutputStream.setLongFileMode(TarArchiveOutputStream.LONGFILE_GNU);

        String name = "metadata.json";

        TarArchiveEntry archiveEntry = new TarArchiveEntry(name);
        archiveEntry.setMode(0100644);
        archiveEntry.setSize(mataDataBytes.length);
        archiveOutputStream.putArchiveEntry(archiveEntry);
        archiveOutputStream.write(mataDataBytes);
        archiveOutputStream.closeArchiveEntry();

        archiveEntry = new TarArchiveEntry("code.tar.gz");
        archiveEntry.setMode(0100644);
        archiveEntry.setSize(dataBytes.length);
        archiveOutputStream.putArchiveEntry(archiveEntry);
        archiveOutputStream.write(dataBytes);
        archiveOutputStream.closeArchiveEntry();
        archiveOutputStream.close();

        return fromBytes(bos.toByteArray());*/
        return Flux.just();
    }

    private Path writeMetadataToFile(Path tmpPath, ExternalChaincodeMetadata metadata) {
        return Try.of(() -> Files.write(tmpPath.resolve("metadata.json"), objectMapper.writeValueAsString(metadata).getBytes()))
                .getOrElseThrow((e) -> new RuntimeException("Cannot write metadata.json"));
    }


}
