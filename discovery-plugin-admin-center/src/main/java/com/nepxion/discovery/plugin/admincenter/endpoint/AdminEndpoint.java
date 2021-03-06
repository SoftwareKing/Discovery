package com.nepxion.discovery.plugin.admincenter.endpoint;

/**
 * <p>Title: Nepxion Discovery</p>
 * <p>Description: Nepxion Discovery</p>
 * <p>Copyright: Copyright (c) 2017-2050</p>
 * <p>Company: Nepxion</p>
 * @author Haojun Ren
 * @version 1.0
 */

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.endpoint.mvc.AbstractMvcEndpoint;
import org.springframework.cloud.client.serviceregistry.Registration;
import org.springframework.cloud.client.serviceregistry.ServiceRegistry;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jmx.export.annotation.ManagedOperation;
import org.springframework.jmx.export.annotation.ManagedResource;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import com.nepxion.discovery.plugin.framework.constant.PluginConstant;
import com.nepxion.discovery.plugin.framework.context.PluginContextAware;
import com.nepxion.discovery.plugin.framework.entity.RuleEntity;
import com.nepxion.discovery.plugin.framework.event.PluginPublisher;
import com.nepxion.discovery.plugin.framework.exception.PluginException;

@ManagedResource(description = "Admin Endpoint")
@SuppressWarnings("unchecked")
public class AdminEndpoint extends AbstractMvcEndpoint {
    private static final Logger LOG = LoggerFactory.getLogger(AdminEndpoint.class);

    @SuppressWarnings("rawtypes")
    private ServiceRegistry serviceRegistry;
    private Registration registration;

    @Autowired
    private PluginContextAware pluginContextAware;

    @Autowired
    private PluginPublisher pluginPublisher;

    @Autowired
    private RuleEntity ruleEntity;

    @SuppressWarnings("rawtypes")
    public AdminEndpoint(ServiceRegistry serviceRegistry) {
        super("/admin", true, true);

        this.serviceRegistry = serviceRegistry;
    }

    public void setRegistration(Registration registration) {
        this.registration = registration;
    }

    // 发送规则配置信息
    @RequestMapping(path = "config", method = RequestMethod.POST)
    @ResponseBody
    @ManagedOperation
    public Object config(@RequestBody String config) {
        Boolean discoveryControlEnabled = pluginContextAware.isDiscoveryControlEnabled();
        if (!discoveryControlEnabled) {
            return new ResponseEntity<>(Collections.singletonMap("Message", "Discovery control is disabled"), HttpStatus.NOT_FOUND);
        }

        try {
            InputStream inputStream = IOUtils.toInputStream(config, PluginConstant.ENCODING_UTF_8);
            pluginPublisher.asyncPublish(inputStream);
        } catch (IOException e) {
            throw new PluginException("To input stream failed", e);
        }

        return "success";
    }

    // 查看当前生效的规则配置信息
    @RequestMapping(path = "view", method = RequestMethod.GET)
    @ResponseBody
    @ManagedOperation
    public String view() {
        return ruleEntity.getContent();
    }

    @RequestMapping(path = "deregister", method = RequestMethod.POST)
    @ResponseBody
    @ManagedOperation
    public Object deregister() {
        if (registration == null) {
            throw new PluginException("No registration found");
        }

        serviceRegistry.deregister(registration);

        LOG.info("Deregister for serviceId={} successfully", registration.getServiceId());

        return "success";
    }

    @RequestMapping(path = "status", method = RequestMethod.POST)
    @ResponseBody
    @ManagedOperation
    public Object status(@RequestBody String status) {
        if (registration == null) {
            throw new PluginException("No registration found");
        }

        serviceRegistry.setStatus(registration, status);

        LOG.info("Set status for serviceId={} status={} successfully", registration.getServiceId(), status);

        return "success";
    }
}