package com.magicrepokit.auth.constant;

/**
 * Auth常量
 */
public interface MRKAuthConstant {

    /**
     * 客户端表名
     */
    String CLIENT_TABLE_NAME = "mrk_client";

    /**
     * 客户端SQL字段
     */
    String CLIENT_FIELDS = "client_id, client_secret, resource_ids, scope, " +
            "authorized_grant_types, web_server_redirect_uri, authorities, access_token_validity, " +
            "refresh_token_validity, additional_information, autoapprove ";

    /**
     * 客户端查询SQL
     */
    String BASE_SELECT = "SELECT " + CLIENT_FIELDS + "FROM " + CLIENT_TABLE_NAME;

    /**
     * 查询client_id
     */
    String SELECT_BY_CLIENT_ID = BASE_SELECT + " where client_id = ?";

    /**
     * redis中授权码存储前缀key
     */
    String REDIS_KEY_AUTHORIZATION_CODE_PREFIX = "mrk:auth:authorization_code:";
}