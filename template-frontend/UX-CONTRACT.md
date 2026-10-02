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

- Tenant management is the first System Management menu entry and is available only to the platform administrator (user ID 1).
- User-facing tenancy labels are “药企管理 / Pharmaceutical Company Management”, including company identifiers, names and user affiliation. Internal tenant_id, language keys and APIs retain their technical names.
- Every created user selects a tenant; ordinary users can select only their own tenant. User and tenant identifiers are immutable after creation.
- Tenant search is visible, supports clear/reset and uses explicit search submission with server pagination.
- Failed saves keep the drawer and input; pending saves prevent repeat submission and closing.
- Deletion uses shared confirmation. The default tenant cannot be deleted or disabled. A tenant containing users or business data cannot be deleted; the server provides a recoverable error.
- Disabling a tenant denies subsequent API requests and login for its users.
- Shared role/configuration definitions are platform-managed. Tenant permissions do not grant platform-wide data access.
