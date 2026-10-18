declare namespace Api {
  namespace Audit {
    interface Event {
      id: CommonType.IdType;
      tenantId: string;
      operationId: string;
      auditScope: 'SAMPLE' | 'NON_SAMPLE';
      action: string;
      subbarcode?: string | null;
      analysisId?: CommonType.IdType | null;
      reportId?: CommonType.IdType | null;
      actorId: string;
      actorName: string;
      source: string;
      outcome: string;
      occurredAt: string;
      changeCount: number;
    }
    interface SearchParams {
      pageNum: number;
      pageSize: number;
      subbarcode: string | null;
      analysisId: string | null;
      auditScope: string | null;
      action: string | null;
      actorName: string | null;
      ascending: boolean;
      includeSampleCommon: boolean;
    }
    interface Change {
      id: CommonType.IdType;
      eventId: CommonType.IdType;
      tableName: string;
      objectKey: string;
      changeType: string;
      beforeJson: string | null;
      afterJson: string | null;
      changedFieldsJson: string;
    }
    interface Detail {
      event: Event;
      changes: Change[];
    }
    interface FieldChange {
      field: string;
      before: unknown;
      after: unknown;
    }
    interface SearchEmits {
      (event: 'search'): void;
    }
  }
}
