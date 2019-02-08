package stroom.security.dao;

import stroom.docref.DocRef;
import stroom.security.shared.DocumentPermissionJooq;

import java.util.Set;

public interface DocumentPermissionDao {

    Set<String> getPermissionsForDocumentForUser(DocRef document, String userUuid);

    DocumentPermissionJooq getPermissionsForDocument(DocRef document);

    void addPermission(String userUuid, DocRef document, String permission);

    void removePermission(String userUuid, DocRef document, String permission);

    void clearDocumentPermissions(DocRef document);

    void clearUserPermissions(String userUuid);
}