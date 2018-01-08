package io.smartcat.berserker.csv.configuration;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ParserSettings {

    public String filename;
    public char delimiter = ',';
    
    @JsonProperty("record-separator")
    public String recordSeparator = "\n";
    public boolean trim = true;
    public Character quote = null;
    
    @JsonProperty("commentMarker")
    public char commentMarker = '#';
    public boolean skipHeaderRecord = false;
    
    @JsonProperty("ignore-empty-lines")
    public boolean ignoreEmptyLines = true;
    public String nullString = null;
}
