/**
 * 样本流转（报告管理 › 样本流转）类型定义
 * <p>
 * 声明 Api.Report 命名空间，与 report-interpretation.api.d.ts / report.api.d.ts 自动合并。
 */
declare namespace Api {
  namespace Report {
    // ---------------------------------------------------------------------
    // 样本流转：一行 = 一个分析批次（analysis_data）⟕ sample_file ⟕ 最新一份报告
    // ---------------------------------------------------------------------

    /** 样本流转列表行（= 后端 SampleFlowRowVo） */
    interface SampleFlowRow {
      /** 分析数据ID */
      analysisId: number;
      /** 分析日期（YYYYMMDD） */
      analysisDate: string | null;
      /** 样本编号 */
      subbarcode: string | null;
      /** 患者编号 */
      barcode: string | null;
      /** 产品名称 */
      product: string | null;
      /** 驱动状态（int 码：10 驱动失败 / 11 驱动成功 / 12 驱动中） */
      driveStatus: number | null;
      /** 驱动执行时间 */
      driveExecutedAt: string | null;
      /** 送检单位 / 客户（sample_file.customer_name） */
      customerName: string | null;
      /** 患者姓名 */
      patientName: string | null;
      /** 样本类型 */
      sampleType: string | null;
      /** 录单产品 */
      erpTestName: string | null;
      /** 报告ID（为空 = 该批次还没有报告记录） */
      reportId: number | null;
      /** 报告名（最近一次生成） */
      reportName: string | null;
      /** 解读癌种 */
      analysisDisease: string | null;
      /** 报告状态（int 码：20/21/30/31/32/33/40/41/42/43/50/52/53/60/61/62） */
      reportStatus: number | null;
      /** 报告解读人（认领人 user_id） */
      interpreter: number | null;
      /** 报告解读人昵称 */
      interpreterName: string | null;
      /** 报告解读认领时间 */
      reportInterpretedAt: string | null;
      /** 生信看点人昵称 */
      bioinfoViewedByName: string | null;
      /** 生信看点完成时间 */
      bioinfoViewedAt: string | null;
      /** 生信复核人昵称 */
      bioinfoReviewedByName: string | null;
      /** 生信复核时间 */
      bioinfoReviewedAt: string | null;
      /** 报告文件（转化后的 PDF 文件名，取自 report_file_path） */
      reportFilePdfName: string | null;
      /** 签名件（签名后 PDF 文件名，取自 report_signed_file_path） */
      reportSignedFileName: string | null;
      /** 签名开始时间（送签时间） */
      reportSignStartedAt: string | null;
      /** 签名完成时间（上上签回调） */
      reportSignedAt: string | null;
      /** 报告生成人 user_id */
      reportGeneratedBy: number | null;
      /** 报告生成人昵称 */
      reportGeneratedByName: string | null;
      /** 报告生成时间 */
      reportGeneratedAt: string | null;
      /** 报告复核人 user_id */
      reportCheckedBy: number | null;
      /** 报告复核人昵称 */
      reportCheckedByName: string | null;
      /** 报告复核时间 */
      reportCheckedAt: string | null;
      /** 报告发送人 user_id */
      reportSentBy: number | null;
      /** 报告发送人昵称 */
      reportSentByName: string | null;
      /** 报告发送时间 */
      reportSentAt: string | null;
      /** 回退来源状态（审核失败回退时非空） */
      revertFrom: number | null;
      /**
       * 本行最新更新时间（analysis_data / analysis_report / sample_file 三者 update_time 的最大值）。
       * 前端轮询对比它 + 行集合判断「有没有变化」，没变化就不替换表格。
       */
      latestUpdateTime: string | null;
    }

    /**
     * 样本流转查询条件：tab 必填（页签决定状态与可见性过滤），其余与报告解读列表同口径。
     */
    type SampleFlowSearchParams = CommonType.RecordNullable<
      Pick<SampleFlowRow, 'analysisDate' | 'subbarcode' | 'product'> &
        Common.CommonSearchParams & { showAll: boolean }
    > & {
      /** 页签：driver / bioinfo-view / bioinfo-review / pending-interpret / interpreting /
       *  pending-review / reviewing / signing / sending */
      tab: string;
    };

    /** 页签「处理」入参 */
    interface SampleFlowHandleParams {
      /** 页签 */
      tab: string;
      /** 分析数据ID */
      analysisId: number;
    }

    /** 页签「处理」结果 */
    interface SampleFlowHandleResult {
      /** 分析数据ID */
      analysisId: number;
      /** 报告ID（跳转详情页用；为空表示还没有报告记录） */
      reportId: number | null;
      /** 跳转目标页签（files / variants / preview / review / send；为空 = 不跳转） */
      targetTab: string | null;
      /** 处理后的报告状态 */
      reportStatus: number | null;
      /** 页面提示 */
      message: string | null;
    }
  }
}
