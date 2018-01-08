package io.smartcat.berserker.csv.configuration;

import java.io.FileReader;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.smartcat.berserker.api.DataSource;
import io.smartcat.berserker.configuration.DataSourceConfiguration;
import io.smartcat.berserker.csv.datasource.CSVDataSource;

/**
 * Configuration to construct {@link CSVDataSource}.
 */
public class CSVConfiguration implements DataSourceConfiguration {

    private static final String PARSER = "parser";
    
    private final ObjectMapper objectMapper;
    
    public CSVConfiguration() {
        this.objectMapper = new ObjectMapper();
    }
    
    @Override
    public String getName() {
        return "CSV";
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Override
    public DataSource<?> getDataSource(Map<String, Object> configuration) {
        Map<String, Object> parserSettingsMap = (Map) configuration.get(PARSER);
        if (parserSettingsMap == null) {
            throw new RuntimeException("'" + PARSER + "' section missing in configuration.");
        }
        ParserSettings parserSettings = objectMapper.convertValue(parserSettingsMap, ParserSettings.class);
        if (parserSettings.filename == null || parserSettings.filename.isEmpty()) {
            throw new RuntimeException("'file' property in '" + PARSER + "' section is mandatory.");
        }
        CSVParser csvParser = createCSVParser(parserSettings);
        ConfigRowMapper rowMapper = new ConfigRowMapper(configuration);
        return new CSVDataSource<>(csvParser, rowMapper);
    }

    private CSVParser createCSVParser(ParserSettings settings) {
        CSVFormat csvFormat = CSVFormat.DEFAULT.withDelimiter(settings.delimiter)
                .withRecordSeparator(settings.recordSeparator)
                .withTrim(settings.trim)
                .withQuote(settings.quote)
                .withCommentMarker(settings.commentMarker)
                .withSkipHeaderRecord(settings.skipHeaderRecord)
                .withIgnoreEmptyLines(settings.ignoreEmptyLines)
                .withNullString(settings.nullString);
        try {
            Path filePath = Paths.get(settings.filename);
            return new CSVParser(new FileReader(filePath.toFile()), csvFormat);
        } catch(Exception e) {
            throw e instanceof RuntimeException ? (RuntimeException) e : new RuntimeException(e);
        }
    }
}
