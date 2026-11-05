# Tenant administration UI contract

Business source: the current user request and `../template-backend/script/sql/update/update_5.6.2-tenant-version.sql`. The server owns tenancy and authorization.

| Capability | Canonical owner | Source of truth | Allowed variants | Verification |
|---|---|---|---|---|
| Table Selection | NDataTable and useTableOperate | Existing user/notice flows | Current page selection | Tenant CRUD browser run |
| Select/Listbox | NSelect and tenant-select.vue | Tenant options API | Admin chooses; ordinary users see their own tenant | User create and select popup |
| Form | NForm and useNaiveForm | Backend validation | Create/edit drawer | Empty, invalid and duplicate identifiers |
| Scrollbar | Global styles and scrollbar.scss | DESIGN.md | Internal table scroll | Narrow viewport |
| Toast | Application window.$message provider | Existing hooks | Success/error messages | CRUD success and failure |
| CRUD | Service APIs and table hooks | Backend contracts | Create resets page; edit preserves page | API and browser verification |
| Report regeneration | Report interpretation APIs and existing Naive UI controls | Report status and generated artifact | 31/32/33/52 may generate; only current successful artifacts may submit | Report regression tests and acceptance checklist |
| Report reassessment | Existing sample-flow personal review queue | Server-assigned original reviewer | Regenerated reports submit directly to status 41; other reviewers cannot act | Review and tenant regression tests |

- Tenant management is the first System Management menu entry and is available only to the platform administrator (user ID 1).
- User-facing tenancy labels are “药企管理 / Pharmaceutical Company Management”, including company identifiers, names and user affiliation. Internal tenant_id, language keys and APIs retain their technical names.
- Every created user selects a tenant; ordinary users can select only their own tenant. User and tenant identifiers are immutable after creation.
- Tenant search is visible, supports clear/reset and uses explicit search submission with server pagination.
- Failed saves keep the drawer and input; pending saves prevent repeat submission and closing.
- Deletion uses shared confirmation. The default tenant cannot be deleted or disabled. A tenant containing users or business data cannot be deleted; the server provides a recoverable error.
- Disabling a tenant denies subsequent API requests and login for its users.
- Shared role/configuration definitions are platform-managed. Tenant permissions do not grant platform-wide data access.

## Report interpretation and versions

- Sent reports display “重新解读”. Opening an old report remains read-only; the reinterpret action creates a linked record only after a successful batch scan.
- Reinterpret scanning stays disabled until the external scanner passes the current schema integration checks. Failures preserve the current page and never show a created report.
- Generation parameters remain editable in allowed states. Busy controls prevent repeat actions without resetting input after failure. Template identity locks after a business version is reserved.
- Changed parameters show a realtime preview and require regeneration before submitting. Submitted reports show the frozen generated JSON; missing artifacts are an explicit error.
- Business versions display as V1/V2, independently of template versions and internal generation attempts. Generation success refreshes the report header and artifact preview.
- Resubmission targets the original reviewer’s personal “报告审核中” queue. UI visibility follows the server assignment; authorization is always checked again by the server.
