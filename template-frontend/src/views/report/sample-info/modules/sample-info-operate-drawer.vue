<script setup lang="ts">
import { computed, ref, watch } from 'vue';
import { jsonClone } from '@sa/utils';
import { fetchCreateSampleInfo, fetchUpdateSampleInfo } from '@/service/api/report/sample-info';
import { useFormRules, useNaiveForm } from '@/hooks/common/form';
import { $t } from '@/locales';

defineOptions({
  name: 'SampleInfoOperateDrawer'
});

interface Props {
  /** 操作类型：add / edit */
  operateType: NaiveUI.TableOperateType;
  /** 编辑时的行数据 */
  rowData?: Api.Report.SampleInfo | null;
}

const props = defineProps<Props>();

interface Emits {
  (e: 'submitted'): void;
}

const emit = defineEmits<Emits>();

const visible = defineModel<boolean>('visible', {
  default: false
});

const { formRef, validate, restoreValidation } = useNaiveForm();
const { createRequiredRule } = useFormRules();

const saving = ref(false);

const title = computed(() => (props.operateType === 'add' ? '新增样本信息' : '编辑样本信息'));

/** 性别（单选用 NSelect，UX-CONTRACT.md） */
const genderOptions = [
  { label: '男', value: '男' },
  { label: '女', value: '女' },
  { label: '未知', value: '未知' }
];

const model = ref<Api.Report.SampleInfoForm>(createDefaultModel());

function createDefaultModel(): Api.Report.SampleInfoForm {
  return {
    subbarcode: '',
    barcode: null,
    patientId: null,
    personName: null,
    gender: null,
    birthday: null,
    age: null,
    patientPhone: null,
    hospital: null,
    receivedDate: null,
    specimenType: null,
    specimenQuantity: null,
    testingProgram: null,
    diseaseType: null,
    client: null,
    commissionDate: null,
    productName: null,
    remark: null
  };
}

const rules = {
  subbarcode: createRequiredRule('样本编号不能为空')
};

function handleUpdateModelWhenEdit() {
  model.value = createDefaultModel();

  if (props.operateType === 'edit' && props.rowData) {
    Object.assign(model.value, jsonClone(props.rowData));
  }
}

function closeDrawer() {
  visible.value = false;
}

async function handleSubmit() {
  await validate();

  // pending 时禁止重复提交与关闭；失败时保留抽屉与输入（UX-CONTRACT.md）
  saving.value = true;
  try {
    if (props.operateType === 'add') {
      const { error } = await fetchCreateSampleInfo(model.value);
      if (error) return;
      window.$message?.success($t('common.addSuccess'));
    }

    if (props.operateType === 'edit') {
      const { error } = await fetchUpdateSampleInfo(model.value);
      if (error) return;
      window.$message?.success($t('common.updateSuccess'));
    }

    closeDrawer();
    emit('submitted');
  } finally {
    saving.value = false;
  }
}

watch(visible, () => {
  if (visible.value) {
    handleUpdateModelWhenEdit();
    restoreValidation();
  }
});
</script>

<template>
  <NDrawer v-model:show="visible" :trap-focus="false" display-directive="show" :width="780" class="max-w-90%">
    <NDrawerContent :title="title" :native-scrollbar="false" closable>
      <NForm ref="formRef" :model="model" :rules="rules">
        <div class="grid grid-cols-1 gap-16px md:grid-cols-2">
          <NFormItem label="样本编号" path="subbarcode">
            <NInput v-model:value="model.subbarcode" placeholder="请输入样本编号（质控记录按它关联）" />
          </NFormItem>
          <NFormItem label="条码" path="barcode">
            <NInput v-model:value="model.barcode" placeholder="请输入条码" />
          </NFormItem>
          <NFormItem label="患者编号" path="patientId">
            <NInput v-model:value="model.patientId" placeholder="请输入患者编号" />
          </NFormItem>
          <NFormItem label="患者姓名" path="personName">
            <NInput v-model:value="model.personName" placeholder="请输入患者姓名" />
          </NFormItem>
          <NFormItem label="性别" path="gender">
            <NSelect v-model:value="model.gender" :options="genderOptions" clearable placeholder="请选择性别" />
          </NFormItem>
          <NFormItem label="出生日期" path="birthday">
            <NDatePicker v-model:formatted-value="model.birthday" type="date" value-format="yyyy-MM-dd" clearable />
          </NFormItem>
          <NFormItem label="年龄" path="age">
            <NInput v-model:value="model.age" placeholder="请输入年龄" />
          </NFormItem>
          <NFormItem label="患者电话" path="patientPhone">
            <NInput v-model:value="model.patientPhone" placeholder="请输入患者电话" />
          </NFormItem>
          <NFormItem label="医院" path="hospital">
            <NInput v-model:value="model.hospital" placeholder="请输入医院" />
          </NFormItem>
          <NFormItem label="接收日期" path="receivedDate">
            <NDatePicker v-model:formatted-value="model.receivedDate" type="date" value-format="yyyy-MM-dd" clearable />
          </NFormItem>
          <NFormItem label="样本类型" path="specimenType">
            <NInput v-model:value="model.specimenType" placeholder="如 石蜡切片、外周血" />
          </NFormItem>
          <NFormItem label="样本数量" path="specimenQuantity">
            <NInput v-model:value="model.specimenQuantity" placeholder="请输入样本数量" />
          </NFormItem>
          <NFormItem label="检测方案" path="testingProgram">
            <NInput v-model:value="model.testingProgram" placeholder="请输入检测方案" />
          </NFormItem>
          <NFormItem label="录单癌种" path="diseaseType">
            <NInput v-model:value="model.diseaseType" placeholder="请输入录单癌种" />
          </NFormItem>
          <NFormItem label="客户" path="client">
            <NInput v-model:value="model.client" placeholder="请输入客户" />
          </NFormItem>
          <NFormItem label="委托日期" path="commissionDate">
            <NDatePicker v-model:formatted-value="model.commissionDate" type="date" value-format="yyyy-MM-dd" clearable />
          </NFormItem>
          <NFormItem class="md:col-span-2" label="录单产品" path="productName">
            <NInput v-model:value="model.productName" placeholder="请输入录单产品（与产品配置里的产品名称对应）" />
          </NFormItem>
          <NFormItem class="md:col-span-2" label="备注" path="remark">
            <NInput v-model:value="model.remark" type="textarea" placeholder="请输入备注" />
          </NFormItem>
        </div>
      </NForm>
      <template #footer>
        <NSpace :size="16">
          <NButton :disabled="saving" @click="closeDrawer">{{ $t('common.cancel') }}</NButton>
          <NButton type="primary" :loading="saving" @click="handleSubmit">{{ $t('common.confirm') }}</NButton>
        </NSpace>
      </template>
    </NDrawerContent>
  </NDrawer>
</template>

<style scoped></style>
