<script setup lang="tsx">
import { ref } from 'vue';
import { NButton, NDivider } from 'naive-ui';
import { fetchBatchDeleteSampleInfo, fetchGetSampleInfoList } from '@/service/api/report/sample-info';
import { useAppStore } from '@/store/modules/app';
import { useAuth } from '@/hooks/business/auth';
import { defaultTransform, useNaivePaginatedTable, useTableOperate } from '@/hooks/common/table';
import { $t } from '@/locales';
import ButtonIcon from '@/components/custom/button-icon.vue';
import SampleInfoImportModal from './modules/sample-info-import-modal.vue';
import SampleInfoOperateDrawer from './modules/sample-info-operate-drawer.vue';
import SampleInfoSearch from './modules/sample-info-search.vue';

defineOptions({
  name: 'SampleInfoList'
});

const appStore = useAppStore();
const { hasAuth } = useAuth();

const importModalVisible = ref(false);

const searchParams = ref<Api.Report.SampleInfoSearchParams>({
  pageNum: 1,
  pageSize: 10,
  barcode: null,
  patientName: null,
  customerName: null,
  cancerType: null,
  erpTestName: null,
  params: {}
});

/** 列表列 = 源表 122 列（列太多时用右上角「列设置」关掉不关心的列） */
const { columns, columnChecks, data, getData, getDataByPage, loading, mobilePagination, scrollX } =
  useNaivePaginatedTable({
    api: () => fetchGetSampleInfoList(searchParams.value),
    transform: response => defaultTransform(response),
    onPaginationParamsChange: params => {
      searchParams.value.pageNum = params.page;
      searchParams.value.pageSize = params.pageSize;
    },
    columns: () => [
      { type: 'selection', align: 'center', width: 48 },
      { key: 'age', title: '年龄', align: 'center', minWidth: 140 },
      { key: 'barcode', title: '样本编号', align: 'center', minWidth: 140 },
      { key: 'bed', title: '床位', align: 'center', minWidth: 140 },
      { key: 'birthDay', title: '出生日期', align: 'center', minWidth: 140 },
      { key: 'birthplace', title: 'BIRTHPLACE', align: 'center', minWidth: 140 },
      { key: 'cancerType', title: '录单癌种', align: 'center', minWidth: 140 },
      { key: 'clinicalRemark', title: '临床备注', align: 'center', minWidth: 140 },
      { key: 'clinicalStages', title: '临床分期', align: 'center', minWidth: 140 },
      { key: 'collectDate', title: '收样日期', align: 'center', minWidth: 140 },
      { key: 'customDesc', title: '客户名称', align: 'center', minWidth: 140 },
      { key: 'customerName', title: '客户', align: 'center', minWidth: 140 },
      { key: 'departmentCode', title: '科室编码', align: 'center', minWidth: 140 },
      { key: 'doctorName', title: '医生姓名', align: 'center', minWidth: 140 },
      { key: 'emailAddress', title: '邮箱', align: 'center', minWidth: 140 },
      { key: 'enterDate', title: '委托日期', align: 'center', minWidth: 140 },
      { key: 'erpSalerName', title: 'ERP销售', align: 'center', minWidth: 140 },
      { key: 'erpTestName', title: '录单产品', align: 'center', minWidth: 140 },
      { key: 'familyFirst', title: '一级亲属患癌情况', align: 'center', minWidth: 140 },
      { key: 'familyFirstAge', title: '一级亲属年龄', align: 'center', minWidth: 140 },
      { key: 'familyFirstCancerType', title: '一级亲属癌种', align: 'center', minWidth: 140 },
      { key: 'familyFirstConfirmTime', title: '一级亲属确诊时间', align: 'center', minWidth: 140 },
      { key: 'familySecond', title: '二级亲属患癌情况', align: 'center', minWidth: 140 },
      { key: 'familySecondAge', title: '二级亲属年龄', align: 'center', minWidth: 140 },
      { key: 'familySecondCancerType', title: '二级亲属癌种', align: 'center', minWidth: 140 },
      { key: 'familySecondConfirmTime', title: '二级亲属确诊时间', align: 'center', minWidth: 140 },
      { key: 'fastCode', title: '加急编号', align: 'center', minWidth: 140 },
      { key: 'firstTreatment', title: '第一次治疗史', align: 'center', minWidth: 140 },
      { key: 'fromOrgan', title: '取材部位', align: 'center', minWidth: 140 },
      { key: 'geneResult', title: '基因检测结果', align: 'center', minWidth: 220 },
      { key: 'geneType', title: '基因型', align: 'center', minWidth: 220 },
      { key: 'getSpecDate', title: '取材日期', align: 'center', minWidth: 140 },
      { key: 'libraryName', title: '文库编号', align: 'center', minWidth: 140 },
      { key: 'locationName', title: '病区', align: 'center', minWidth: 140 },
      { key: 'mailingAddress', title: '邮寄地址', align: 'center', minWidth: 220 },
      { key: 'managerEmail', title: '经理邮箱', align: 'center', minWidth: 140 },
      { key: 'orderCode', title: '订单编号', align: 'center', minWidth: 140 },
      { key: 'outpatient', title: '门诊号', align: 'center', minWidth: 140 },
      { key: 'pathologicalType', title: '病理类型', align: 'center', minWidth: 140 },
      { key: 'pathologyNum', title: '病理号', align: 'center', minWidth: 140 },
      { key: 'patientName', title: '姓名', align: 'center', minWidth: 140 },
      { key: 'patientPhone', title: '患者电话', align: 'center', minWidth: 140 },
      { key: 'payAmount', title: '收款金额', align: 'center', minWidth: 140 },
      { key: 'pcode', title: '患者编号', align: 'center', minWidth: 140 },
      { key: 'pmEmail', title: '项目经理邮箱', align: 'center', minWidth: 140 },
      { key: 'receiverTelephone', title: '报告接收人电话', align: 'center', minWidth: 140 },
      { key: 'recorder', title: '录单人', align: 'center', minWidth: 140 },
      { key: 'recorderCode', title: '录单人编码', align: 'center', minWidth: 140 },
      { key: 'reportReceiver', title: '报告接收人', align: 'center', minWidth: 140 },
      { key: 'room', title: '房间', align: 'center', minWidth: 140 },
      { key: 'salerEmail', title: '销售邮箱(ERP)', align: 'center', minWidth: 140 },
      { key: 'sampleRemark', title: '样本备注', align: 'center', minWidth: 220 },
      { key: 'sampleSource', title: '样本来源', align: 'center', minWidth: 140 },
      { key: 'sampleTime', title: '采样日期', align: 'center', minWidth: 140 },
      { key: 'sampleType', title: '样本类型', align: 'center', minWidth: 140 },
      { key: 'secondTreatment', title: '第二次治疗史', align: 'center', minWidth: 220 },
      { key: 'sendDate', title: '寄出日期', align: 'center', minWidth: 140 },
      { key: 'sex', title: '性别', align: 'center', minWidth: 140 },
      { key: 'specimenNo', title: '标本编号', align: 'center', minWidth: 140 },
      { key: 'specimenNum', title: '样本数量', align: 'center', minWidth: 140 },
      { key: 'supportEmail', title: '支持邮箱', align: 'center', minWidth: 140 },
      { key: 'thirdTreatment', title: '第三次治疗史', align: 'center', minWidth: 220 },
      { key: 'unit', title: '单位', align: 'center', minWidth: 140 },
      { key: 'admissionDoctorEmail', title: '接诊医生邮箱', align: 'center', minWidth: 140 },
      { key: 'admissionDoctorPhone', title: '接诊医生电话', align: 'center', minWidth: 140 },
      { key: 'admissionHospital', title: '就诊医院', align: 'center', minWidth: 220 },
      { key: 'cancerType1', title: '癌种1', align: 'center', minWidth: 140 },
      { key: 'cancerTypeCode', title: '癌种编码', align: 'center', minWidth: 140 },
      { key: 'contractName', title: '合同名称', align: 'center', minWidth: 140 },
      { key: 'contractsNo', title: '合同编号', align: 'center', minWidth: 140 },
      { key: 'corpDesc', title: '单位名称', align: 'center', minWidth: 140 },
      { key: 'corpNo', title: '单位编号', align: 'center', minWidth: 140 },
      { key: 'customerType', title: '客户类型', align: 'center', minWidth: 140 },
      { key: 'detectionMethod', title: '检测方法', align: 'center', minWidth: 220 },
      { key: 'detectionTime', title: '检测时间', align: 'center', minWidth: 140 },
      { key: 'expressName', title: '快递公司', align: 'center', minWidth: 140 },
      { key: 'expressNo', title: '快递单号', align: 'center', minWidth: 140 },
      { key: 'familyKinshipTwoCancer', title: '二级亲属患癌情况', align: 'center', minWidth: 140 },
      { key: 'firstTreatmentDrugRegimen', title: '第一次治疗用药方案', align: 'center', minWidth: 220 },
      { key: 'firstTreatmentDuration', title: '第一次治疗时长', align: 'center', minWidth: 140 },
      { key: 'firstTreatmentEffect', title: '第一次治疗疗效', align: 'center', minWidth: 220 },
      { key: 'firstTreatmentMethod', title: '第一次治疗方式', align: 'center', minWidth: 220 },
      { key: 'firstTreatmentTime', title: '第一次治疗时间', align: 'center', minWidth: 140 },
      { key: 'laboratoryName', title: '实验室', align: 'center', minWidth: 140 },
      { key: 'operateManagerCode', title: '运营负责人编码', align: 'center', minWidth: 140 },
      { key: 'operateManagerDesc', title: '运营负责人', align: 'center', minWidth: 140 },
      { key: 'operateManagerEmail', title: '运营负责人邮箱', align: 'center', minWidth: 140 },
      { key: 'orderMoney', title: '订单金额', align: 'center', minWidth: 140 },
      { key: 'otherAttachments', title: '其它附件', align: 'center', minWidth: 220 },
      { key: 'pathologyReport', title: '病理报告', align: 'center', minWidth: 220 },
      { key: 'patientInfoEmail', title: '患者信息邮箱', align: 'center', minWidth: 140 },
      { key: 'patientInfoIsCancer', title: '患者是否肿瘤', align: 'center', minWidth: 140 },
      { key: 'payFinishDate', title: '收款完成日期', align: 'center', minWidth: 140 },
      { key: 'recorderDesc', title: '录单单位', align: 'center', minWidth: 140 },
      { key: 'salesMan', title: '销售', align: 'center', minWidth: 140 },
      { key: 'salesManCode', title: '销售编码', align: 'center', minWidth: 140 },
      { key: 'salesManEmail', title: '销售邮箱', align: 'center', minWidth: 140 },
      { key: 'sampleAddress', title: '送样地址', align: 'center', minWidth: 220 },
      { key: 'sampleContactDesc', title: '送样联系人', align: 'center', minWidth: 140 },
      { key: 'sampleContactPhone', title: '送样联系电话', align: 'center', minWidth: 140 },
      { key: 'sampleNumUnit', title: '数量单位', align: 'center', minWidth: 140 },
      { key: 'sampleProductCode', title: '产品编码', align: 'center', minWidth: 140 },
      { key: 'secondTreatmentDrugRegimen', title: '第二次治疗用药方案', align: 'center', minWidth: 220 },
      { key: 'secondTreatmentDuration', title: '第二次治疗时长', align: 'center', minWidth: 140 },
      { key: 'secondTreatmentEffect', title: '第二次治疗疗效', align: 'center', minWidth: 220 },
      { key: 'secondTreatmentMethod', title: '第二次治疗方式', align: 'center', minWidth: 220 },
      { key: 'secondTreatmentTime', title: '第二次治疗时间', align: 'center', minWidth: 140 },
      { key: 'serialNumber', title: '流水号', align: 'center', minWidth: 140 },
      { key: 'specificCancer', title: '具体癌种', align: 'center', minWidth: 140 },
      { key: 'testingInstitution', title: '送检机构', align: 'center', minWidth: 140 },
      { key: 'thirdTreatmentDrugRegimen', title: '第三次治疗用药方案', align: 'center', minWidth: 220 },
      { key: 'thirdTreatmentDuration', title: '第三次治疗时长', align: 'center', minWidth: 140 },
      { key: 'thirdTreatmentEffect', title: '第三次治疗疗效', align: 'center', minWidth: 220 },
      { key: 'thirdTreatmentMethod', title: '第三次治疗方式', align: 'center', minWidth: 220 },
      { key: 'thirdTreatmentTime', title: '第三次治疗时间', align: 'center', minWidth: 140 },
      { key: 'departmentDesc', title: '科室名称', align: 'center', minWidth: 140 },
      { key: 'abnormalRemark', title: '异常备注', align: 'center', minWidth: 220 },
      { key: 'checkoutLogic', title: '结算渠道', align: 'center', minWidth: 140 },
      { key: 'dyDate', title: '到样日期', align: 'center', minWidth: 140 },
      { key: 'paymentDate', title: '账期(天)', align: 'center', minWidth: 140 },
      { key: 'sampleAttribute', title: '样本属性', align: 'center', minWidth: 140 },
      { key: 'signTime', title: '签约时间', align: 'center', minWidth: 140 },
      { key: 'block', title: '冻结状态', align: 'center', minWidth: 140 },
      {
        key: 'operate',
        title: $t('common.operate'),
        align: 'center',
        width: 140,
        fixed: 'right',
        render: row => {
          // 每个按钮单独判权限；两个都没有时连分隔线都不渲染
          const divider = () => {
            if (!hasAuth('report:sampleInfo:edit') || !hasAuth('report:sampleInfo:remove')) {
              return null;
            }
            return <NDivider vertical />;
          };

          const editBtn = () => {
            if (!hasAuth('report:sampleInfo:edit')) {
              return null;
            }
            return (
              <ButtonIcon
                text
                type="primary"
                icon="material-symbols:drive-file-rename-outline-outline"
                tooltipContent={$t('common.edit')}
                onClick={() => handleEdit(row.id)}
              />
            );
          };

          const deleteBtn = () => {
            if (!hasAuth('report:sampleInfo:remove')) {
              return null;
            }
            return (
              <ButtonIcon
                text
                type="error"
                icon="material-symbols:delete-outline"
                tooltipContent={$t('common.delete')}
                popconfirmContent={$t('common.confirmDelete')}
                onPositiveClick={() => handleDelete(row.id)}
              />
            );
          };

          return (
            <div class="flex-center gap-8px">
              {editBtn()}
              {divider()}
              {deleteBtn()}
            </div>
          );
        }
      }
    ]
  });

const {
  drawerVisible,
  operateType,
  editingData,
  handleAdd,
  handleEdit,
  checkedRowKeys,
  onBatchDeleted,
  onDeleted
} = useTableOperate(data, 'id', getData);

/** 批量删除：与单条删除共用同一个批量接口，避免两边各写一套 */
async function handleBatchDelete() {
  const { error } = await fetchBatchDeleteSampleInfo(checkedRowKeys.value);
  if (error) return;
  onBatchDeleted();
}

/** 单条删除：传一个 id 的数组 */
async function handleDelete(id: CommonType.IdType) {
  const { error } = await fetchBatchDeleteSampleInfo([id]);
  if (error) return;
  onDeleted();
}
</script>

<template>
  <div class="min-h-500px flex-col-stretch gap-16px overflow-hidden lt-sm:overflow-auto">
    <SampleInfoSearch v-model:model="searchParams" @search="getDataByPage" />

    <NCard title="样本信息" :bordered="false" size="small" class="card-wrapper sm:flex-1-hidden">
      <template #header-extra>
        <TableHeaderOperation
          v-model:columns="columnChecks"
          :disabled-delete="checkedRowKeys.length === 0"
          :loading="loading"
          :show-add="hasAuth('report:sampleInfo:add')"
          :show-delete="hasAuth('report:sampleInfo:remove')"
          :show-export="false"
          @add="handleAdd"
          @delete="handleBatchDelete"
          @refresh="getData"
        >
          <template #prefix>
            <NButton
              v-if="hasAuth('report:sampleInfo:import')"
              size="small"
              ghost
              type="primary"
              @click="importModalVisible = true"
            >
              <template #icon>
                <icon-material-symbols-upload class="text-icon" />
              </template>
              导入
            </NButton>
          </template>
        </TableHeaderOperation>
      </template>

      <NDataTable
        v-model:checked-row-keys="checkedRowKeys"
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

    <SampleInfoOperateDrawer
      v-model:visible="drawerVisible"
      :operate-type="operateType"
      :row-data="editingData"
      @submitted="getData"
    />

    <SampleInfoImportModal v-model:visible="importModalVisible" @submitted="getData" />
  </div>
</template>

<style scoped></style>
