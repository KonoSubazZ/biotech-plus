package org.dromara.report.service.impl;

import lombok.RequiredArgsConstructor;
import org.dromara.common.satoken.utils.TenantContext;
import org.dromara.report.mapper.NkbEvidenceMapper;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

/**
 * 癌种范围解析（对齐 report_en7 的 getDiseaseList + solidTumorFiltration）
 * <p>
 * 规则：
 * <ul>
 *   <li>范围 = 本癌种 + 全部祖先 + 全部子孙（MySQL 5.7 无递归 CTE，按层 BFS，上限 6 层）</li>
 *   <li>性别剔除：男性剔 120（女性生殖器官肿瘤）及其子孙；女性剔 3856（男性生殖器官癌症）及其子孙</li>
 *   <li>瘤种互斥：实体瘤(10000003) 与 血液肿瘤(2531) 互相剔除对方及其子孙</li>
 * </ul>
 *
 * @author <你的名字>
 */
@Component
@RequiredArgsConstructor
public class NkbDiseaseScopeResolver {

    private final NkbEvidenceMapper nkbEvidenceMapper;

    /** 癌种上下展开的最大层数 */
    private static final int MAX_DISEASE_DEPTH = 6;

    /** en7 solidTumorFiltration 的固定 ID */
    private static final int FEMALE_ORGAN_TUMOR = 120;
    private static final int MALE_ORGAN_TUMOR = 3856;
    private static final int BLOOD_TUMOR = 2531;
    private static final int SOLID_TUMOR = 10000003;

    /**
     * 癌种范围（en7 getDiseaseList + solidTumorFiltration）
     *
     * @param diseaseId 本癌种ID
     * @param gender    规范性别 MALE/FEMALE/UNKNOWN
     * @return 范围
     */
    public DiseaseScope resolve(Long diseaseId, String gender) {
        if (diseaseId == null) {
            return new DiseaseScope(List.of(), List.of(), List.of());
        }
        List<Long> ancestors = ancestorsOf(diseaseId);
        List<Long> descendants = descendantsOf(diseaseId, new ArrayList<>());

        List<Long> scope = new ArrayList<>();
        scope.addAll(ancestors);
        scope.add(diseaseId);
        scope.addAll(descendants);

        Set<Long> excluded = excludedDiseases(scope, gender);
        scope.removeAll(excluded);
        List<Long> parents = new ArrayList<>(ancestors);
        parents.add(diseaseId);
        return new DiseaseScope(scope, parents, new ArrayList<>(excluded));
    }

    /** 全部祖先（逐层向上，直到没有新父级） */
    private List<Long> ancestorsOf(Long diseaseId) {
        List<Long> ancestors = new ArrayList<>();
        List<Long> layer = List.of(diseaseId);
        for (int depth = 0; depth < MAX_DISEASE_DEPTH; depth++) {
            if (layer.isEmpty()) {
                break;
            }
            List<Long> current = layer;
            List<Long> parents = query(() -> nkbEvidenceMapper.selectParentDiseaseIds(current));
            List<Long> fresh = parents.stream()
                .filter(id -> id != null && !ancestors.contains(id) && !id.equals(diseaseId)).toList();
            ancestors.addAll(fresh);
            layer = fresh;
        }
        return ancestors;
    }

    /** 全部子孙（逐层向下） */
    private List<Long> descendantsOf(Long diseaseId, List<Long> collected) {
        List<Long> layer = List.of(diseaseId);
        for (int depth = 0; depth < MAX_DISEASE_DEPTH; depth++) {
            if (layer.isEmpty()) {
                break;
            }
            List<Long> current = layer;
            List<Long> children = query(() -> nkbEvidenceMapper.selectChildDiseaseIds(current));
            List<Long> fresh = children.stream()
                .filter(id -> id != null && !collected.contains(id) && !id.equals(diseaseId)).toList();
            collected.addAll(fresh);
            layer = fresh;
        }
        return collected;
    }

    /** 性别 / 实体瘤·血液瘤互斥剔除集合（en7 solidTumorFiltration） */
    private Set<Long> excludedDiseases(List<Long> scope, String gender) {
        Set<Long> excluded = new LinkedHashSet<>();
        if ("MALE".equals(gender)) {
            excluded.addAll(expand(FEMALE_ORGAN_TUMOR));
        } else if ("FEMALE".equals(gender)) {
            excluded.addAll(expand(MALE_ORGAN_TUMOR));
        }
        if (scope.contains((long) SOLID_TUMOR)) {
            excluded.addAll(expand(BLOOD_TUMOR));
        } else if (scope.contains((long) BLOOD_TUMOR)) {
            excluded.addAll(expand(SOLID_TUMOR));
        }
        return excluded;
    }

    /** 某癌种 + 其全部子孙 */
    private List<Long> expand(int rootId) {
        List<Long> all = new ArrayList<>(List.of((long) rootId));
        List<Long> layer = List.of((long) rootId);
        for (int depth = 0; depth < MAX_DISEASE_DEPTH && !layer.isEmpty(); depth++) {
            List<Long> current = layer;
            List<Long> children = query(() -> nkbEvidenceMapper.selectChildDiseaseIds(current));
            List<Long> fresh = children.stream().filter(id -> id != null && !all.contains(id)).toList();
            all.addAll(fresh);
            layer = fresh;
        }
        return all;
    }



    private <T> List<T> query(Supplier<List<T>> action) {
        List<T> result = TenantContext.withoutTenant(action::get);
        return result == null ? List.of() : result;
    }

    /**
     * 癌种范围
     *
     * @param diseaseIds       参与证据过滤的癌种（本癌种 + 祖先 + 子孙，已剔除性别/瘤种冲突）
     * @param parentDiseaseIds 本癌种 + 祖先（give 判定与「其他癌种」区分用）
     * @param excludedIds      被剔除的癌种（性别/实体瘤·血液瘤）
     */
    public record DiseaseScope(List<Long> diseaseIds, List<Long> parentDiseaseIds, List<Long> excludedIds) {
    }
}
