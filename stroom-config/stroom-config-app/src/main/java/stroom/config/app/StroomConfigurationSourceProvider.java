package stroom.config.app;

import stroom.util.io.DiffUtil;
import stroom.util.io.HomeDirProvider;
import stroom.util.io.HomeDirProviderImpl;
import stroom.util.io.PathConfig;
import stroom.util.io.PathCreator;
import stroom.util.io.StreamUtil;
import stroom.util.io.TempDirProvider;
import stroom.util.io.TempDirProviderImpl;
import stroom.util.logging.LogUtil;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import io.dropwizard.configuration.ConfigurationSourceProvider;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Paths;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

public class StroomConfigurationSourceProvider implements ConfigurationSourceProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(StroomConfigurationSourceProvider.class);

    private static final String SOURCE_DEFAULTS = "defaults";
    private static final String SOURCE_YAML = "YAML";
    private static final List<String> JSON_POINTERS_TO_INSPECT = List.of(
            "/server",
            "/logging");

    private static final List<String> KEYS_TO_MUTATE = List.of(
            "currentLogFilename",
            "archivedLogFilenamePattern");

    private static final String PATH_CONFIG_JSON_POINTER = "/appConfig/path";
    private static final String STROOM_HOME_JSON_POINTER = PATH_CONFIG_JSON_POINTER + "/home";
    private static final String STROOM_TEMP_JSON_POINTER = PATH_CONFIG_JSON_POINTER + "/temp";

    private final ConfigurationSourceProvider delegate;

    public StroomConfigurationSourceProvider(final ConfigurationSourceProvider delegate) {
        this.delegate = delegate;
    }

    @Override
    public InputStream open(final String path) throws IOException {
        log("Applying path substitutions to Drop Wizard configuration in file {}",
                Paths.get(path).toAbsolutePath().normalize().toString());

        try (final InputStream in = delegate.open(path)) {
            // This is the yaml tree after passing though the delegate
            // substitutions
            final ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
            final JsonNode rootNode = mapper.readTree(in);

            // Parse the yaml to find out if the home/temp props have been set so
            // we can construct a PathCreator to do the path substitution on the drop wiz
            // section of the yaml
            final PathCreator pathCreator = getPathCreator(rootNode);
            final Function<String, String> logDirMutator = currLogDir ->
                    pathCreator.makeAbsolute(pathCreator.replaceSystemProperties(currLogDir.trim()));

            JSON_POINTERS_TO_INSPECT.forEach(jsonPointerExp ->
                    mutateNodes(rootNode, jsonPointerExp, KEYS_TO_MUTATE, logDirMutator));

            final ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            mapper.writeValue(byteArrayOutputStream, rootNode);

//            dumpYamlDiff(path, in, mapper, rootNode);

            return new ByteArrayInputStream(byteArrayOutputStream.toByteArray());
        }
    }

    private void dumpYamlDiff(final String path,
                              final InputStream in,
                              final ObjectMapper mapper,
                              final JsonNode rootNode) throws IOException {
        in.reset();
        try {
            final String originalYaml = StreamUtil.streamToString(in);
            final String newYaml = mapper.writeValueAsString(rootNode);
            DiffUtil.unifiedDiff(
                    originalYaml,
                    newYaml,
                    true,
                    3,
                    diffLines ->
                            log("Comparing original and modified yaml:\n{}",
                                    String.join("\n", diffLines)));
        } catch (IOException e) {
            log("Unable to read file " + path, e);
        }
    }


    private void mutateNodes(final JsonNode rootNode,
                             final String jsonPointerExpr,
                             final List<String> names,
                             final Function<String, String> valueMutator) {
        final JsonNode parentNode = rootNode.at(jsonPointerExpr);
        if (parentNode.isMissingNode()) {
            throw new RuntimeException(LogUtil.message("jsonPointerExpr {}, not found in yaml",
                    jsonPointerExpr));
        } else {
            mutateNodes(parentNode, names, valueMutator, jsonPointerExpr);
        }
    }

    private void mutateNodes(final JsonNode parent,
                             final List<String> names,
                             final Function<String, String> valueMutator,
                             final String path) {

        if (parent instanceof ArrayNode) {
            for (int i = 0; i < parent.size(); i++) {
                mutateNodes(parent.get(i), names, valueMutator, path + "/" + i);
            }
        } else if (parent instanceof ObjectNode) {
            parent.fields().forEachRemaining(entry -> {
                final String valueNodePath = path + "/" + entry.getKey();
                if (names.contains(entry.getKey())) {
                    // found our node so mutate it
                    final String value = entry.getValue().textValue();
                    final String newValue = valueMutator.apply(value);
                    log("Replacing value for \"{}\": [{}] => [{}]",
                            valueNodePath, value, newValue);
                    ((ObjectNode) parent).put(entry.getKey(), newValue);
                } else {
                    // not our node so recurse into it
                    mutateNodes(entry.getValue(), names, valueMutator, path + "/" + entry.getKey());
                }
            });
        }
    }

    @NotNull
    private PathCreator getPathCreator(final JsonNode rootNode) {
        Objects.requireNonNull(rootNode);

        final Optional<String> optHome = getNodeValue(rootNode, STROOM_HOME_JSON_POINTER);
        final Optional<String> optTemp = getNodeValue(rootNode, STROOM_TEMP_JSON_POINTER);

        // A vanilla PathConfig with the hard coded defaults
        final PathConfig pathConfig = new PathConfig();

        final String homeSource = Objects.equals(pathConfig.getHome(), optHome.orElse(null))
                ? SOURCE_DEFAULTS
                : SOURCE_YAML;
        final String tempSource = Objects.equals(pathConfig.getTemp(), optTemp.orElse(null))
                ? SOURCE_DEFAULTS
                : SOURCE_YAML;

        // Set the values from the YAML if we have them
        optHome.ifPresent(pathConfig::setHome);
        optTemp.ifPresent(pathConfig::setTemp);

        final HomeDirProvider homeDirProvider = new HomeDirProviderImpl(pathConfig);
        final TempDirProvider tempDirProvider = new TempDirProviderImpl(pathConfig, homeDirProvider);
        final PathCreator pathCreator = new PathCreator(homeDirProvider, tempDirProvider);

        log("Using stroom home [{}] from {} for Drop Wizard config path substitutions",
                homeDirProvider.get().toAbsolutePath(),
                homeSource);
        log("Using stroom temp [{}] from {} for Drop Wizard config path substitutions",
                tempDirProvider.get().toAbsolutePath(),
                tempSource);
        return pathCreator;
    }

    private Optional<String> getNodeValue(final JsonNode rootNode, final String jsonPointerExpr) {
        Objects.requireNonNull(rootNode);
        Objects.requireNonNull(jsonPointerExpr);

        final JsonNode node = rootNode.at(jsonPointerExpr);
        if (node.isMissingNode()) {
            return Optional.empty();
        } else {
            return Optional.ofNullable(node.textValue());
        }
    }

    private void log(final String msg, Object... args) {
        // Use system.out as we have no logger at this point
        System.out.println(LogUtil.message(msg, args));
    }
}
