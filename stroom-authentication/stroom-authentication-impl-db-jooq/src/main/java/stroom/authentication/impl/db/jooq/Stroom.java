/*
 * This file is generated by jOOQ.
 */
package stroom.authentication.impl.db.jooq;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.annotation.processing.Generated;

import org.jooq.Catalog;
import org.jooq.Table;
import org.jooq.impl.SchemaImpl;

import stroom.authentication.impl.db.jooq.tables.Account;
import stroom.authentication.impl.db.jooq.tables.JsonWebKey;
import stroom.authentication.impl.db.jooq.tables.OauthClient;
import stroom.authentication.impl.db.jooq.tables.Token;
import stroom.authentication.impl.db.jooq.tables.TokenType;


/**
 * This class is generated by jOOQ.
 */
@Generated(
    value = {
        "http://www.jooq.org",
        "jOOQ version:3.12.3"
    },
    comments = "This class is generated by jOOQ"
)
@SuppressWarnings({ "all", "unchecked", "rawtypes" })
public class Stroom extends SchemaImpl {

    private static final long serialVersionUID = 488724268;

    /**
     * The reference instance of <code>stroom</code>
     */
    public static final Stroom STROOM = new Stroom();

    /**
     * The table <code>stroom.account</code>.
     */
    public final Account ACCOUNT = stroom.authentication.impl.db.jooq.tables.Account.ACCOUNT;

    /**
     * The table <code>stroom.json_web_key</code>.
     */
    public final JsonWebKey JSON_WEB_KEY = stroom.authentication.impl.db.jooq.tables.JsonWebKey.JSON_WEB_KEY;

    /**
     * The table <code>stroom.oauth_client</code>.
     */
    public final OauthClient OAUTH_CLIENT = stroom.authentication.impl.db.jooq.tables.OauthClient.OAUTH_CLIENT;

    /**
     * The table <code>stroom.token</code>.
     */
    public final Token TOKEN = stroom.authentication.impl.db.jooq.tables.Token.TOKEN;

    /**
     * The table <code>stroom.token_type</code>.
     */
    public final TokenType TOKEN_TYPE = stroom.authentication.impl.db.jooq.tables.TokenType.TOKEN_TYPE;

    /**
     * No further instances allowed
     */
    private Stroom() {
        super("stroom", null);
    }


    @Override
    public Catalog getCatalog() {
        return DefaultCatalog.DEFAULT_CATALOG;
    }

    @Override
    public final List<Table<?>> getTables() {
        List result = new ArrayList();
        result.addAll(getTables0());
        return result;
    }

    private final List<Table<?>> getTables0() {
        return Arrays.<Table<?>>asList(
            Account.ACCOUNT,
            JsonWebKey.JSON_WEB_KEY,
            OauthClient.OAUTH_CLIENT,
            Token.TOKEN,
            TokenType.TOKEN_TYPE);
    }
}