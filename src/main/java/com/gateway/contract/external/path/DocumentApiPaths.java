package com.gateway.contract.external.path;

/** 문서/블록 관련 외부 공개 경로 */
public final class DocumentApiPaths {
    private DocumentApiPaths() {}

    /** 일반 사용자/에디터 경로 */
    public static final String ALL = "/api/v1/documents/**";
    public static final String DETAIL = "/api/v1/documents/{documentId}";
    public static final String BLOCKS = "/api/v1/documents/{documentId}/blocks";
    public static final String MOVE = "/api/v1/documents/{documentId}/move";
    public static final String RESTORE = "/api/v1/documents/{documentId}/restore";
    public static final String TRANSACTIONS = "/api/v1/documents/{documentId}/transactions";

    /** 관리자/운영용 외부 경로 */
    public static final String ADMIN_ALL = "/api/v1/admin/**";
    public static final String ADMIN_DOCUMENT_BLOCKS = "/api/v1/admin/documents/{documentId}/blocks";
    public static final String ADMIN_BLOCK_DETAIL = "/api/v1/admin/blocks/{blockId}";
    public static final String ADMIN_BLOCK_MOVE = "/api/v1/admin/blocks/{blockId}/move";
}