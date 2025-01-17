package stroom.index.impl.db;

import stroom.collection.api.CollectionService;
import stroom.dictionary.api.WordListProvider;
import stroom.docref.DocRef;
import stroom.docref.DocRefInfo;
import stroom.docrefinfo.api.DocRefInfoService;
import stroom.index.impl.IndexVolumeGroupService;
import stroom.index.mock.MockIndexVolumeGroupService;
import stroom.security.api.SecurityContext;
import stroom.test.common.util.db.DbTestModule;
import stroom.util.db.ForceLegacyMigration;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class TestModule extends AbstractModule {

    static final String TEST_USER = "testUser";

    @Override
    protected void configure() {
        install(new DbTestModule());

        // Create a test security context
        final SecurityContext securityContext = mock(SecurityContext.class);
        when(securityContext.getUserId()).thenReturn(TEST_USER);
        bind(SecurityContext.class).toInstance(securityContext);
        bind(ForceLegacyMigration.class).toInstance(new ForceLegacyMigration() {
        });
        bind(IndexVolumeGroupService.class).toInstance(new MockIndexVolumeGroupService());
    }

    @Provides
    CollectionService collectionService() {
        return new CollectionService() {
            @Override
            public Set<DocRef> getChildren(final DocRef folder, final String type) {
                return null;
            }

            @Override
            public Set<DocRef> getDescendants(final DocRef folder, final String type) {
                return null;
            }
        };
    }

    @Provides
    WordListProvider wordListProvider() {
        return new WordListProvider() {
            @Override
            public List<DocRef> findByName(final String dictionaryName) {
                return Collections.emptyList();
            }

            @Override
            public String getCombinedData(final DocRef dictionaryRef) {
                return null;
            }

            @Override
            public String[] getWords(final DocRef dictionaryRef) {
                return null;
            }
        };
    }

    @Provides
    DocRefInfoService docRefInfoService() {
        return new DocRefInfoService() {
            @Override
            public Optional<DocRefInfo> info(final DocRef docRef) {
                return Optional.empty();
            }

            @Override
            public Optional<String> name(final DocRef docRef) {
                return Optional.ofNullable(docRef.getName());
            }
        };
    }
}
