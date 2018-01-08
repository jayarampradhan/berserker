package io.smartcat.berserker.csv.configuration;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.csv.CSVRecord;
import org.parboiled.Parboiled;
import org.parboiled.parserunners.ReportingParseRunner;
import org.parboiled.support.ParsingResult;

import io.smartcat.berserker.csv.datasource.CSVDataSource.RowMapper;
import io.smartcat.ranger.ObjectGenerator;
import io.smartcat.ranger.core.CompositeValue;
import io.smartcat.ranger.core.ConstantValue;
import io.smartcat.ranger.core.Value;
import io.smartcat.ranger.core.ValueProxy;
import io.smartcat.ranger.parser.InvalidReferenceNameException;
import io.smartcat.ranger.parser.ValueExpressionParser;

public class ConfigRowMapper implements RowMapper<Object> {

    private static final Pattern COLUMN_NAMES = Pattern.compile("^c[1-9]\\d*$");
    private static final String VALUES = "values";
    private static final String OUTPUT = "output";

    private final Map<String, Object> values;
    private final Object outputExpression;

    private ExtendedValueExpressionParser parser;
    private ReportingParseRunner<Value<?>> parseRunner;
    private Map<String, ValueProxy<?>> proxyValues;
    private ObjectGenerator<Object> objectGenerator;

    @SuppressWarnings("unchecked")
    public ConfigRowMapper(Map<String, Object> config) {
        checkSectionExistence(config, VALUES);
        checkSectionExistence(config, OUTPUT);
        this.values = (Map<String, Object>) config.get(VALUES);
        this.outputExpression = config.get(OUTPUT);
        this.objectGenerator = createObjectGenerator();
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Override
    public Object map(CSVRecord record) {
        // set CSV values
        List<ValueProxy> proxies = new ArrayList<>();
        for (int i = 0; i < record.size(); i++) {
            String name = "c" + (i + 1);
            ValueProxy proxy = proxyValues.get(name);
            if (proxy != null) {
                proxy.setDelegate(ConstantValue.of(record.get(i)));
                proxies.add(proxy);
            }
        }

        // generate result
        Object result = objectGenerator.next();

        // reset CSV values
        //proxies.forEach(x -> x.setDelegate(null));
        return result;
    }

    @SuppressWarnings({ "unchecked" })
    private ObjectGenerator<Object> createObjectGenerator() {
        buildModel();
        return new ObjectGenerator<>((Value<Object>) parseSimpleValue("", outputExpression));
    }

    private void buildModel() {
        this.proxyValues = new HashMap<>();
        this.parser = Parboiled.createParser(ExtendedValueExpressionParser.class, proxyValues);
        this.parseRunner = new ReportingParseRunner<>(parser.value());
        if (values != null) {
            checkForReservedValueNames();
            createProxies();
            parseValues();
        }
    }

    private void checkSectionExistence(Map<String, Object> config, String name) {
        if (!config.containsKey(name)) {
            throw new RuntimeException("Configuraiton must contain '" + name + "' section.");
        }
    }
    
    private void checkForReservedValueNames() {
        for (Map.Entry<String, Object> entry : values.entrySet()) {
            Matcher m = COLUMN_NAMES.matcher(entry.getKey());
            if (m.matches()) {
                throw new RuntimeException("Property '" + entry.getKey() + "' is reserved for CSV column names. It cannot be defined within configuration.");
            }
        }
    }

    private void createProxies() {
        for (Map.Entry<String, Object> entry : values.entrySet()) {
            createProxy(entry.getKey(), entry.getValue());
        }
    }

    @SuppressWarnings("unchecked")
    private void createProxy(String key, Object value) {
        proxyValues.put(key, new ValueProxy<>());
        if (value instanceof Map) {
            for (Map.Entry<String, Object> entry : ((Map<String, Object>) value).entrySet()) {
                createProxy(key + "." + entry.getKey(), entry.getValue());
            }
        }
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private void parseValues() {
        for (Map.Entry<String, Object> entry : values.entrySet()) {
            Value<?> val = parse(entry.getKey(), entry.getValue());
            ValueProxy proxy = proxyValues.get(entry.getKey());
            proxy.setDelegate(val);
            entry.setValue(proxy);
        }
    }

    @SuppressWarnings("unchecked")
    private Value<?> parse(String parentName, Object def) {
        if (def instanceof Map) {
            return parseCompositeValue(parentName, (Map<String, Object>) def);
        } else {
            return parseSimpleValue(parentName, def);
        }
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private Value<?> parseCompositeValue(String parentName, Map<String, Object> def) {
        Map<String, Value<?>> values = new HashMap<>();
        for (String property : def.keySet()) {
            String fullName = parentName + "." + property;
            Value<?> val = parse(fullName, def.get(property));
            ValueProxy proxy = proxyValues.get(fullName);
            proxy.setDelegate(val);
            values.put(property, proxy);
        }
        return new CompositeValue(values);
    }

    private Value<?> parseSimpleValue(String parentName, Object def) {
        // handle String as expression and all other types as primitives
        if (def instanceof String) {
            parser.setParentName(stripOffLastReference(parentName));
            ParsingResult<Value<?>> result = parseRunner.run((String) def);
            return result.valueStack.pop();
        } else {
            return ConstantValue.of(def);
        }
    }

    private String stripOffLastReference(String name) {
        if (!name.contains(".")) {
            return "";
        } else {
            return name.substring(0, name.lastIndexOf('.'));
        }
    }

    private static class ExtendedValueExpressionParser extends ValueExpressionParser {
        
        private final Map<String, ValueProxy<?>> proxyValues;

        public ExtendedValueExpressionParser(Map<String, ValueProxy<?>> proxyValues) {
            super(proxyValues);
            this.proxyValues = proxyValues;
        }

        @Override
        public Value<?> getValueProxy(String name) {
            try {
                return super.getValueProxy(name);
            } catch (InvalidReferenceNameException e) {
                Matcher m = COLUMN_NAMES.matcher(name);
                if (m.matches()) {
                    ValueProxy<Object> valueProxy = new ValueProxy<>();
                    proxyValues.put(name, valueProxy);
                    return valueProxy;
                } else {
                    throw e;
                }
            }
        }
    }

}
