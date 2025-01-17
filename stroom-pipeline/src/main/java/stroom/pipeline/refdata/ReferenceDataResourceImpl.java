package stroom.pipeline.refdata;

import stroom.event.logging.rs.api.AutoLogged;
import stroom.event.logging.rs.api.AutoLogged.OperationType;
import stroom.pipeline.refdata.store.RefStoreEntry;
import stroom.util.logging.LogUtil;
import stroom.util.time.StroomDuration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import javax.inject.Inject;
import javax.inject.Provider;

@AutoLogged
public class ReferenceDataResourceImpl implements ReferenceDataResource {

    private static final Logger LOGGER = LoggerFactory.getLogger(ReferenceDataResourceImpl.class);

    private final Provider<ReferenceDataService> referenceDataServiceProvider;

    @Inject
    public ReferenceDataResourceImpl(final Provider<ReferenceDataService> referenceDataServiceProvider) {
        this.referenceDataServiceProvider = referenceDataServiceProvider;
    }

    @AutoLogged(OperationType.VIEW)
    @Override
    public List<RefStoreEntry> entries(final Integer limit) {
        return referenceDataServiceProvider.get()
                .entries(limit != null
                        ? limit
                        : 100);
    }

    @AutoLogged(OperationType.VIEW)
    @Override
    public String lookup(final RefDataLookupRequest refDataLookupRequest) {
        return referenceDataServiceProvider.get()
                .lookup(refDataLookupRequest);
    }

    @AutoLogged(OperationType.DELETE)
    @Override
    public void purge(final String purgeAge) {
        StroomDuration purgeAgeDuration;
        try {
            purgeAgeDuration = StroomDuration.parse(purgeAge);
        } catch (Exception e) {
            throw new IllegalArgumentException(LogUtil.message(
                    "Can't parse purgeAge [{}]", purgeAge), e);
        }
        try {
            referenceDataServiceProvider.get()
                    .purge(purgeAgeDuration);
        } catch (Exception e) {
            LOGGER.error("Failed to purgeAge " + purgeAge, e);
        }
    }
}
