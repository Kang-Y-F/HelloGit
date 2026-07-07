package com.neusoft.demo.service.serviceimpl;

import com.neusoft.demo.service.McpToolService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.mcp.SyncMcpToolCallbackProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * MCP工具调用服务实现
 */
@Slf4j
@Service
public class McpToolServiceImpl implements McpToolService {

    private final ChatClient chatClient;
    private final SyncMcpToolCallbackProvider mcpToolProvider;
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Autowired
    public McpToolServiceImpl(ChatClient chatClient, SyncMcpToolCallbackProvider mcpToolProvider) {
        this.chatClient = chatClient;
        this.mcpToolProvider = mcpToolProvider;
    }

    @Override
    public String answerWithMcpTools(String question) {
        log.info("MCP工具调用 - 通用问答: {}", question);

        try {
            return chatClient.prompt()
                    .user(question)
                    .tools(mcpToolProvider.getToolCallbacks())
                    .call()
                    .content();
        } catch (Exception e) {
            log.error("MCP工具调用失败", e);
            return "抱歉，AI服务暂时不可用，请稍后重试。";
        }
    }

    @Override
    public String assistDiagnosisWithMcp(Long patientId, String symptoms) {
        log.info("MCP工具调用 - 辅助诊断: patientId={}, symptoms={}", patientId, symptoms);

        String prompt = String.format("""
            你是一名脑科主任医师AI助手。请使用可用的MCP工具查询相关信息后，给出诊断建议。
            
            患者ID：%d
            症状：%s
            
            请按以下步骤操作：
            1. 调用 query_patient_record 工具查询患者病历
            2. 调用 query_medical_knowledge 工具检索相关疾病知识
            3. 综合以上信息，给出诊断建议、鉴别诊断、检查建议
            
            注意：必须实际调用工具，不能凭空猜测！
            """, patientId, symptoms);

        try {
            String result = chatClient.prompt()
                    .user(prompt)
                    .tools(mcpToolProvider.getToolCallbacks())
                    .call()
                    .content();

            log.info("辅助诊断完成，生成时间: {}", LocalDateTime.now().format(DATE_FMT));
            return result;
        } catch (Exception e) {
            log.error("辅助诊断失败", e);
            return "诊断服务暂时不可用，请咨询上级医师。";
        }
    }

    @Override
    public String guidePatientWithMcp(String symptoms) {
        log.info("MCP工具调用 - 智能导诊: symptoms={}", symptoms);

        String prompt = String.format("""
            你是智能导诊AI助手。请使用MCP工具查询相关医疗知识后，给出就诊建议。
            
            患者症状：%s
            
            请按以下步骤操作：
            1. 调用 query_medical_knowledge 工具检索相关疾病和科室信息
            2. 根据检索结果，推荐合适的就诊科室
            3. 给出简单的病情分析
            
            注意：必须基于工具返回的知识回答，不要编造！
            """, symptoms);

        try {
            String result = chatClient.prompt()
                    .user(prompt)
                    .tools(mcpToolProvider.getToolCallbacks())
                    .call()
                    .content();

            log.info("智能导诊完成，生成时间: {}", LocalDateTime.now().format(DATE_FMT));
            return result;
        } catch (Exception e) {
            log.error("智能导诊失败", e);
            return "导诊服务暂时不可用，请直接前往医院就诊。";
        }
    }

    @Override
    public String importHuatuoDataset(String filePath) {
        log.info("MCP工具调用 - 导入数据集: filePath={}", filePath);

        String prompt = String.format("""
            你是数据导入助手。请使用MCP工具读取华佗数据集文件，并统计信息。
            
            文件路径：%s
            
            请执行以下操作：
            1. 调用 read_huatuo_dataset 工具读取文件
            2. 统计记录总数
            3. 返回导入摘要
            
            注意：如果文件不存在或格式错误，请明确报告错误！
            """, filePath);

        try {
            String result = chatClient.prompt()
                    .user(prompt)
                    .tools(mcpToolProvider.getToolCallbacks())
                    .call()
                    .content();

            log.info("数据集导入完成，生成时间: {}", LocalDateTime.now().format(DATE_FMT));
            return result;
        } catch (Exception e) {
            log.error("数据集导入失败", e);
            throw new RuntimeException("导入失败: " + e.getMessage());
        }
    }

    @Override
    public String interpretLabResultsWithMcp(List<String> abnormalItems, String patientSymptoms) {
        log.info("MCP工具调用 - 检验解读: abnormalItems={}, symptoms={}", abnormalItems, patientSymptoms);

        String itemsStr = String.join("、", abnormalItems);
        String prompt = String.format("""
            你是一名检验科主任医师AI助手。请使用MCP工具检索相关医学知识后，对检验结果进行专业解读。
            
            异常检验项目：%s
            患者症状：%s
            
            请按以下步骤操作：
            1. 调用 query_medical_knowledge 工具检索这些异常指标的临床意义
            2. 结合患者症状，分析可能的病因
            3. 给出综合判断和建议
            
            输出要求：
            1. 【总体评价】：正常/轻度异常/显著异常/危急值
            2. 【关键异常逐项解读】：每个异常项的临床意义
            3. 【综合判断】：这些异常指标之间的关联性，最可能的诊断方向
            4. 【建议】：是否需要复查、进一步检查或紧急处理
            
            注意：必须基于工具返回的知识回答！
            """, itemsStr, patientSymptoms);

        try {
            String result = chatClient.prompt()
                    .user(prompt)
                    .tools(mcpToolProvider.getToolCallbacks())
                    .call()
                    .content();

            log.info("检验解读完成，生成时间: {}", LocalDateTime.now().format(DATE_FMT));
            return result;
        } catch (Exception e) {
            log.error("检验解读失败", e);
            return "检验解读服务暂时不可用，请咨询检验科医师。";
        }
    }

    // ========== 新增：智能分诊 ==========
    @Override
    public String triageWithMcp(String symptoms) {
        log.info("MCP工具调用 - 智能分诊: symptoms={}", symptoms);

        String prompt = String.format("""
            你是一名专业的脑科分诊AI。请使用MCP工具查询相关医疗知识后，给出分诊建议。
            
            患者描述如下症状：
            %s
            
            请按以下步骤操作：
            1. 调用 query_medical_knowledge 工具检索该症状相关的疾病知识
            2. 结合检索结果，给出分诊建议
            
            请严格按以下格式输出（简洁、专业）：
            【初步判断】
            （描述可能的病情方向，1-2句话）
            【建议检查】
            （列出建议优先做的检查项目，用顿号分隔）
            【就诊紧急度】
            （普通 / 较紧急 / 急诊，并说明原因）
            【注意事项】
            （患者就诊前的注意事项，1-2条）
            
            注意：必须基于工具返回的知识回答，不要凭空编造！
            """, symptoms);

        try {
            String result = chatClient.prompt()
                    .user(prompt)
                    .tools(mcpToolProvider.getToolCallbacks())
                    .call()
                    .content();

            log.info("智能分诊完成，生成时间: {}", LocalDateTime.now().format(DATE_FMT));
            return result;
        } catch (Exception e) {
            log.error("MCP智能分诊失败", e);
            throw new RuntimeException("分诊服务暂时不可用: " + e.getMessage());
        }
    }

    // ========== 新增：CT影像AI分析 ==========
    @Override
    public String analyzeCtWithMcp(Long reportId, String patientInfo, String artifactInfo,
                                   String recordInfo, String labInfo) {
        log.info("MCP工具调用 - CT影像分析: reportId={}", reportId);

        String prompt = String.format("""
            你是一名专业的脑科影像AI辅助诊断助手。请使用MCP工具检索相关医学知识后，
            结合以下患者完整资料，给出结构化的影像分析报告。
            
            ━━━ 患者信息 ━━━
            %s
            
            ━━━ CT伪影检测结果 ━━━
            %s
            
            ━━━ 历史病历 ━━━
            %s
            
            ━━━ 检验报告 ━━━
            %s
            
            请按以下步骤操作：
            1. 调用 query_medical_knowledge 工具检索与上述病历/检验异常相关的疾病知识
            2. 结合检索结果与影像伪影数据，综合判断
            
            ━━━ 输出要求 ━━━
            请严格按以下格式输出（共4段，每段以标题开头），语言专业、简洁、有针对性，不要泛泛而谈。
            
            【影像质量评估】
            （基于伪影数据判断本次CT影像质量是否满足诊断需求，2-3句）
            
            【疾病可能性分析】
            （结合病历、检验、伪影数据及检索到的知识，推断可能的疾病或异常，按可能性排序，2-4条）
            
            【重点关注区域】
            （指出影像中医生应重点观察的解剖部位或异常征象，3-5个要点）
            
            【建议下一步检查】
            （建议进一步的影像/化验/会诊项目，2-3条）
            
            注意：必须实际调用工具，不能凭空猜测！
            """, patientInfo, artifactInfo, recordInfo, labInfo);

        try {
            String result = chatClient.prompt()
                    .user(prompt)
                    .tools(mcpToolProvider.getToolCallbacks())
                    .call()
                    .content();

            log.info("CT影像MCP分析完成，生成时间: {}", LocalDateTime.now().format(DATE_FMT));
            return result;
        } catch (Exception e) {
            log.error("MCP影像分析生成失败 reportId={}", reportId, e);
            throw new RuntimeException("AI服务暂时不可用，请稍后重试");
        }
    }

    // ========== 新增：诊疗建议生成 ==========
    @Override
    public String generateAdviceWithMcp(String chiefComplaint, String presentHistory, String checkResult,
                                        String checkCandidates, String labCandidates, String drugCandidates) {
        log.info("MCP工具调用 - 诊疗建议生成（含候选清单）: chiefComplaint={}", chiefComplaint);

        String prompt = String.format("""
        你是一名专业的脑科AI助理医生。请使用MCP工具检索相关医学知识后，
        根据以下患者信息，并【严格从下方"可选项目清单"中挑选】，给出结构化的诊疗建议。

        患者主诉：%s
        现病史：%s
        初步检查结果：%s

        ━━━ 可选检查项目（本科室） ━━━
        %s

        ━━━ 可选检验项目（本科室） ━━━
        %s

        ━━━ 可选药品（含处方属性与禁忌） ━━━
        %s

        请按以下步骤操作：
        1. 调用 query_medical_knowledge 工具检索与主诉/检查结果相关的疾病知识
        2. 结合检索结果，并仅在上面清单范围内挑选检查、检验、药品

        请严格按照以下格式输出，不要添加其他内容：
        【诊断建议】
        （填写初步诊断）
        【检查建议】
        （只能从"可选检查项目"清单中选择，多项用顿号分隔，若无需检查填"暂无"）
        【检验建议】
        （只能从"可选检验项目"清单中选择，多项用顿号分隔，若无需检验填"暂无"）
        【用药建议】
        （只能从"可选药品"清单中选择，注意避开患者禁忌与处方权限，多项用顿号分隔，暂无则填"暂无"）

        注意：必须基于工具返回的知识回答！清单之外的项目/药品一律不允许出现，清单为空或无合适项时直接填"暂无"，不要编造。
        """, chiefComplaint, presentHistory, checkResult, checkCandidates, labCandidates, drugCandidates);

        try {
            String result = chatClient.prompt()
                    .user(prompt)
                    .tools(mcpToolProvider.getToolCallbacks())
                    .call()
                    .content();

            log.info("MCP诊疗建议生成完成，生成时间: {}", LocalDateTime.now().format(DATE_FMT));
            return result;
        } catch (Exception e) {
            log.error("MCP诊疗建议生成失败", e);
            throw new RuntimeException("AI服务暂时不可用，请稍后重试");
        }
    }
}