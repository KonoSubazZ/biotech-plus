<script setup lang="tsx">
import { ref } from 'vue';
import { NButton, NDivider, NTag } from 'naive-ui';
import {
  fetchDeleteReportMailConfig,
  fetchGetReportMailConfigList,
  fetchSendTestMail,
  fetchSetDefaultReportMailConfig
} from '@/service/api/report/mail-config';
import { useAuth } from '@/hooks/business/auth';
import { useAppStore } from '@/store/modules/app';
import { defaultTransform, useNaivePaginatedTable, useTableOperate } from '@/hooks/common/table';
import { $t } from '@/locales';
import ButtonIcon from '@/components/custom/button-icon.vue';
import ReportMailConfigOperateDrawer from './modules/mail-config-operate-drawer.vue';

defineOptions({
  name: 'ReportMailConfigList'
});

const appStore = useAppStore();
const { hasAuth } = useAuth();

const canEdit = () => hasAuth('report:mailConfig:edit');
const canTest = () => hasAuth('report:mailConfig:test');

const searchParams = ref<Api.Report.ReportMailConfigSearchParams>({
  pageNum: 1,
  pageSize: 10,
  fromEmail: null,
  params: {}
});

const { columns, columnChecks, data, getData, getDataByPage, loading, mobilePagination, scrollX } =
  useNaivePaginatedTable({
    api: () => fetchGetReportMailConfigList(searchParams.value),
    transform: response => defaultTransform(response),
    onPaginationParamsChange: params => {
      searchParams.value.pageNum = params.page;
      searchParams.value.pageSize = params.pageSize;
    },
    columns: () => [
      {
        key: 'isDefault',
        title: '默认',
        align: 'center',
        width: 80,
        render: row =>
          row.isDefault === '1' ? (
            <NTag type="info" size="small">
              默认
            </NTag>
          ) : (
            <>-</>
          )
      },
      { key: 'fromEmail', title: '发件邮箱', align: 'left', minWidth: 220, render: row => row.fromEmail ?? '-' },
      { key: 'fromName', title: '发件人昵称', align: 'center', width: 140, render: row => row.fromName ?? '-' },
      {
        key: 'smtp',
        title: 'SMTP',
        align: 'left',
        width: 220,
        render: row => `${row.smtpHost ?? '-'}:${row.smtpPort ?? '-'}${row.sslEnable === '1' ? '（SSL）' : ''}`
      },
      {
        key: 'passwordSet',
        title: '密码',
        align: 'center',
        width: 90,
        render: row =>
          row.passwordSet ? (
            <NTag type="success" size="small">
              已设置
            </NTag>
          ) : (
            <NTag type="warning" size="small">
              未设置
            </NTag>
          )
      },
      {
        key: 'enabled',
        title: '状态',
        align: 'center',
        width: 90,
        render: row =>
          row.enabled === '1' ? (
            <NTag type="success" size="small">
              启用
            </NTag>
          ) : (
            <NTag type="error" size="small">
              停用
            </NTag>
          )
      },
      { key: 'updateTime', title: '更新时间', align: 'center', minWidth: 160, render: row => row.updateTime ?? '-' },
      {
        key: 'operate',
        title: $t('common.operate'),
        align: 'center',
        width: 200,
        fixed: 'right',
        render: row => {
          const divider = () => (canEdit() ? <NDivider vertical /> : null);

          const defaultBtn = () => {
            // 已经是默认的不再给按钮
            if (!canEdit() || row.isDefault === '1') {
              return null;
            }
            return (
              <NButton text type="primary" size="small" onClick={() => handleSetDefault(row.id!)}>
                设为默认
              </NButton>
            );
          };

          const editBtn = () =>
            canEdit() ? (
              <ButtonIcon
                text
                type="primary"
                icon="material-symbols:drive-file-rename-outline-outline"
                tooltipContent={$t('common.edit')}
                onClick={() => handleEdit(row.id!)}
              />
            ) : null;

          const testBtn = () =>
            canTest() ? (
              <ButtonIcon
                text
                type="primary"
                icon="material-symbols:outgoing-mail"
                tooltipContent="发送测试邮件"
                onClick={() => openTest(row)}
              />
            ) : null;

          const deleteBtn = () =>
            canEdit() ? (
              <ButtonIcon
                text
                type="error"
                icon="material-symbols:delete-outline"
                tooltipContent={$t('common.delete')}
                popconfirmContent={$t('common.confirmDelete')}
                onPositiveClick={() => handleDelete(row.id!)}
              />
            ) : null;

          return (
            <div class="flex-center gap-8px">
              {defaultBtn()}
              {editBtn()}
              {divider()}
              {testBtn()}
              {divider()}
              {deleteBtn()}
            </div>
          );
        }
      }
    ]
  });

const { drawerVisible, operateType, editingData, handleAdd, handleEdit, onDeleted } = useTableOperate(
  data,
  'id',
  getData
);

/** 设为默认：同租户其它行会被后端清成非默认 */
async function handleSetDefault(id: number) {
  const { error } = await fetchSetDefaultReportMailConfig(id);
  if (error) {
    return;
  }
  window.$message?.success('已设为默认邮箱');
  getData();
}

/** 删除：删掉的若是默认，后端会自动把最早的一条补成默认 */
async function handleDelete(id: number) {
  const { error } = await fetchDeleteReportMailConfig(id);
  if (error) {
    return;
  }
  onDeleted();
}

/** 发送测试邮件：手填收件人，用这一条配置真实发一封 */
const testVisible = ref(false);
const testTo = ref('');
const testConfig = ref<Api.Report.ReportMailConfig | null>(null);
const testing = ref(false);

function openTest(row: Api.Report.ReportMailConfig) {
  testConfig.value = row;
  testTo.value = '';
  testVisible.value = true;
}

async function handleTest() {
  if (!testTo.value.trim()) {
    window.$message?.error('请填写测试收件人');
    return;
  }
  testing.value = true;
  try {
    const { error } = await fetchSendTestMail({ to: testTo.value.trim(), configId: testConfig.value?.id ?? undefined });
    if (error) {
      return;
    }
    window.$message?.success('测试邮件已发出，请查收');
    testVisible.value = false;
  } finally {
    testing.value = false;
  }
}

async function search() {
  getDataByPage();
}

function reset() {
  searchParams.value.fromEmail = null;
  getDataByPage();
}
</script>

<template>
  <div class="min-h-500px flex-col-stretch gap-16px overflow-hidden lt-sm:overflow-auto">
    <NForm label-placement="left" :label-width="80">
      <div class="flex flex-wrap items-start">
        <NFormItem class="w-full pr-24px sm:w-1/2 xl:w-1/3" label="发件邮箱">
          <NInput v-model:value="searchParams.fromEmail" placeholder="模糊匹配" clearable @keyup.enter="search" />
        </NFormItem>
        <NFormItem class="ml-auto" :show-feedback="false">
          <NSpace :size="16">
            <NButton @click="reset">
              <template #icon>
                <icon-ic-round-refresh class="text-icon" />
              </template>
              {{ $t('common.reset') }}
            </NButton>
            <NButton type="primary" ghost @click="search">
              <template #icon>
                <icon-ic-round-search class="text-icon" />
              </template>
              {{ $t('common.search') }}
            </NButton>
          </NSpace>
        </NFormItem>
      </div>
    </NForm>

    <NCard title="邮件配置" :bordered="false" size="small" class="card-wrapper sm:flex-1-hidden">
      <template #header-extra>
        <TableHeaderOperation
          v-model:columns="columnChecks"
          :disabled-delete="true"
          :loading="loading"
          :show-add="canEdit()"
          :show-delete="false"
          :show-export="false"
          @add="handleAdd"
          @refresh="getData"
        />
      </template>

      <NDataTable
        :columns="columns"
        :data="data"
        :flex-height="!appStore.isMobile"
        :loading="loading"
        :pagination="mobilePagination"
        :row-key="row => row.id"
        :scroll-x="scrollX"
        remote
        size="small"
        class="sm:h-full"
      />
    </NCard>

    <ReportMailConfigOperateDrawer
      v-model:visible="drawerVisible"
      :operate-type="operateType"
      :row-data="editingData"
      @submitted="getData"
    />

    <NModal
      v-model:show="testVisible"
      preset="card"
      :title="`发送测试邮件：${testConfig?.fromEmail ?? ''}`"
      :bordered="false"
      class="w-90vw max-w-480px"
    >
      <NForm label-placement="left" :label-width="80">
        <NFormItem label="收件人">
          <NInput v-model:value="testTo" placeholder="请输入用于接收测试邮件的邮箱" />
        </NFormItem>
      </NForm>
      <template #footer>
        <NSpace justify="end">
          <NButton @click="testVisible = false">取消</NButton>
          <NButton type="primary" :loading="testing" @click="handleTest">发送</NButton>
        </NSpace>
      </template>
    </NModal>
  </div>
</template>

<style scoped></style>
