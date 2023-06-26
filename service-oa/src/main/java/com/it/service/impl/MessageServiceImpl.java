package com.it.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.it.custom.LoginUserInfoHelper;
import com.it.model.process.Process;
import com.it.model.process.ProcessTemplate;
import com.it.model.system.SysUser;
import com.it.service.MessageService;
import com.it.service.OaProcessService;
import com.it.service.OaProcessTemplateService;
import com.it.service.SysUserService;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import me.chanjar.weixin.mp.api.WxMpService;
import me.chanjar.weixin.mp.bean.template.WxMpTemplateData;
import me.chanjar.weixin.mp.bean.template.WxMpTemplateMessage;
import org.joda.time.DateTime;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.util.Map;

/**
 * @author 杨振华
 * @since 2023/6/26
 */
@Service
@Slf4j
public class MessageServiceImpl implements MessageService {
    @Resource
    private WxMpService wxMpService;

    @Resource
    private OaProcessService processService;

    @Resource
    private OaProcessTemplateService processTemplateService;

    @Resource
    private SysUserService sysUserService;
    /**
     * 推送待审批人员
     *
     * @param processId
     * @param userId
     * @param taskId
     */
    @SneakyThrows
    @Override
    public void pushPendingMessage(Long processId, Long userId, String taskId) {
        // 根据id查询数据
        Process process = processService.getById(processId);
        SysUser user = sysUserService.getById(userId);
        // 审批模版信息
        ProcessTemplate processTemplate = processTemplateService.getById(process.getProcessTemplateId());
        // 提交审批人的信息
        SysUser submitUser = sysUserService.getById(process.getUserId());

        // 获取要给消息的人的openid
        String openId = user.getOpenId();
        if (StringUtils.isEmpty(openId)){
            // //方便测试，给默认值（开发者本人的openId）
            openId = "oNR9W6uwhHYL5jvNcWbt4s0R6bsI";
        }

        // 设置消息发送信息
        WxMpTemplateMessage templateMessage = WxMpTemplateMessage
                .builder()
                .toUser(openId)// 给谁发信息，openid
                // 模版信息id
                .templateId("HvZmPEHlwMcBNl4e4L8bkoCgTaj_Ci0Hnr_CLw9FPBg")
                // 点击信息跳转的地址
                .url("http://ggkt1.vipgz1.91tunnel.com/#/show/" + processId + "/0")//点击模板消息要访问的网址
                .build();
        JSONObject jsonObject = JSON.parseObject(process.getFormValues());
        JSONObject formShowData = jsonObject.getJSONObject("formShowData");
        StringBuffer content = new StringBuffer();
        for (Map.Entry entry : formShowData.entrySet()) {
            content.append(entry.getKey()).append("：").append(entry.getValue()).append("\n ");
        }

        // 设置模版参数
        templateMessage.addData(new WxMpTemplateData("first", submitUser.getName()+"提交了"+processTemplate.getName()+"审批申请，请注意查看。", "#272727"));
        templateMessage.addData(new WxMpTemplateData("keyword1", process.getProcessCode(), "#272727"));
        templateMessage.addData(new WxMpTemplateData("keyword2", new DateTime(process.getCreateTime()).toString("yyyy-MM-dd HH:mm:ss"), "#272727"));
        templateMessage.addData(new WxMpTemplateData("content", content.toString(), "#272727"));

        // 消息发送
        String msg = wxMpService.getTemplateMsgService().sendTemplateMsg(templateMessage);
        log.info("推送消息返回：{}", msg);
    }

    /**
     * 审批后推送提交审批人员
     * @param processId
     * @param userId
     * @param status
     */
    @SneakyThrows
    @Override
    public void pushProcessedMessage(Long processId, Long userId, Integer status) {
        Process process = processService.getById(processId);
        ProcessTemplate processTemplate = processTemplateService.getById(process.getProcessTemplateId());
        SysUser sysUser = sysUserService.getById(userId);
        SysUser currentSysUser = sysUserService.getById(LoginUserInfoHelper.getUserId());
        String openid = sysUser.getOpenId();
        if(StringUtils.isEmpty(openid)) {
            openid = "oNR9W6uwhHYL5jvNcWbt4s0R6bsI";
        }
        WxMpTemplateMessage templateMessage = WxMpTemplateMessage.builder()
                .toUser(openid)//要推送的用户openid
                .templateId("PAQTiup2RKQGXclXi7JzqVooiGkA6mQEECNYz7Urn8M")//模板id
                .url("http://ggkt1.vipgz1.91tunnel.com/#/show/" + processId + "/0")//点击模板消息要访问的网址
                .build();
        JSONObject jsonObject = JSON.parseObject(process.getFormValues());
        JSONObject formShowData = jsonObject.getJSONObject("formShowData");
        StringBuffer content = new StringBuffer();
        for (Map.Entry entry : formShowData.entrySet()) {
            content.append(entry.getKey()).append("：").append(entry.getValue()).append("\n ");
        }
        templateMessage.addData(new WxMpTemplateData("first", "你发起的"+processTemplate.getName()+"审批申请已经被处理了，请注意查看。", "#272727"));
        templateMessage.addData(new WxMpTemplateData("keyword1", process.getProcessCode(), "#272727"));
        templateMessage.addData(new WxMpTemplateData("keyword2", new DateTime(process.getCreateTime()).toString("yyyy-MM-dd HH:mm:ss"), "#272727"));
        templateMessage.addData(new WxMpTemplateData("keyword3", currentSysUser.getName(), "#272727"));
        templateMessage.addData(new WxMpTemplateData("keyword4", status == 1 ? "审批通过" : "审批拒绝", status == 1 ? "#009966" : "#FF0033"));
        templateMessage.addData(new WxMpTemplateData("content", content.toString(), "#272727"));
        String msg = wxMpService.getTemplateMsgService().sendTemplateMsg(templateMessage);
        log.info("推送消息返回：{}", msg);
    }

}
