package stroom.elastic.impl.http;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import stroom.docref.DocRef;
import stroom.elastic.api.ElasticIndexWriter;
import stroom.elastic.api.ElasticIndexWriterFactory;
import stroom.elastic.impl.ElasticIndexConfigCache;
import stroom.elastic.impl.ElasticIndexConfigDoc;
import stroom.pipeline.errorhandler.LoggedException;
import stroom.security.api.SecurityContext;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Optional;

@Singleton
class HttpElasticIndexWriterFactory implements ElasticIndexWriterFactory {

    private static final Logger LOGGER = LoggerFactory.getLogger(HttpElasticIndexWriterFactory.class);

    private final ElasticIndexConfigCache elasticIndexCache;
    private final SecurityContext securityContext;

    @Inject
    HttpElasticIndexWriterFactory(final ElasticIndexConfigCache elasticIndexCache,
                                  final SecurityContext securityContext) {
        this.elasticIndexCache = elasticIndexCache;
        this.securityContext = securityContext;
    }

    @Override
    public Optional<ElasticIndexWriter> create(final DocRef elasticConfigRef) {
        return securityContext.asProcessingUserResult(() -> {
            // Get the index and index fields from the cache.
            final ElasticIndexConfigDoc elasticIndexConfigDoc = elasticIndexCache.get(elasticConfigRef);
            if (elasticIndexConfigDoc == null) {
//                log(Severity.FATAL_ERROR, "Unable to load index", null);
                throw new LoggedException("Unable to load index");
            }

            return Optional.of(new HttpElasticIndexWriter(elasticIndexConfigDoc));
        });
    }
}
