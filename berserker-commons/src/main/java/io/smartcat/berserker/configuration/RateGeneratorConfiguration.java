package io.smartcat.berserker.configuration;

import java.util.Map;

import io.smartcat.berserker.api.RateGenerator;

/**
 * Returns {@link RateGenerator} based on configuration parameters. Each {@link RateGenerator} implementation should go
 * with corresponding rate generator configuration implementation which would be used to construct that rate generator.
 */
public interface RateGeneratorConfiguration extends BaseConfiguration {

    /**
     * Returns rate generator based on configuration parameters.
     *
     * @param configuration Configuration specific to rate generator it should construct.
     * @return Instance of {@link RateGenerator}, never null.
     *
     * @throws ConfigurationParseException If there is an error during configuration parsing.
     */
    RateGenerator getRateGenerator(Map<String, Object> configuration) throws ConfigurationParseException;
}
