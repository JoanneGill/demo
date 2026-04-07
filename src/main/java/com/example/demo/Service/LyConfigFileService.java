package com.example.demo.Service;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
public class LyConfigFileService {

    private static final Logger log = LoggerFactory.getLogger(LyConfigFileService.class);

    private static final DateTimeFormatter BAK_TS = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private static final Gson GSON = new GsonBuilder()
            .disableHtmlEscaping()
            .setPrettyPrinting()
            .create();

    @Value("${yl.config.path.win}")
    private String windowsPath;

    @Value("${yl.config.path.linux}")
    private String unixPath;

    public Path resolveConfigPath() {
        final String osName = System.getProperty("os.name", "").toLowerCase();
        final String filePath = osName.contains("win") ? windowsPath : unixPath;
        return Path.of(filePath);
    }

    public void writeConfig(JsonObject root) throws IOException {
        Path target = resolveConfigPath().toAbsolutePath().normalize();
        Path parent = target.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }

        if (Files.exists(target)) {
            Path bak = target.resolveSibling(target.getFileName() + ".bak-" + LocalDateTime.now().format(BAK_TS));
            Files.copy(target, bak, StandardCopyOption.COPY_ATTRIBUTES);
            log.info("lyConfig backup created: {}", bak);
        }

        Path tmp = target.resolveSibling(target.getFileName() + ".tmp");
        Files.writeString(tmp, GSON.toJson(root), StandardCharsets.UTF_8);

        try {
            Files.move(tmp, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException e) {
            Files.move(tmp, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }
}

