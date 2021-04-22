package com.cleafy.elasticsearch6.plugins.http;

import com.cleafy.elasticsearch6.plugins.http.auth.AuthCredentials;
import com.cleafy.elasticsearch6.plugins.http.auth.HttpBasicAuthenticator;
import com.cleafy.elasticsearch6.plugins.http.utils.AppProperties;
import com.cleafy.elasticsearch6.plugins.http.utils.Globals;
import com.cleafy.elasticsearch6.plugins.http.utils.LoggerUtils;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set; 

import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.client.node.NodeClient;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.rest.*;
import org.elasticsearch.transport.TransportException;

public class BasicRestFilter {
    private final HttpBasicAuthenticator httpBasicAuthenticator;
    private boolean isUnauthLogEnabled;
    private Set<String> ipwhitelist = new HashSet();

    public BasicRestFilter(final Settings settings) {
        super();
        //设置本机到白名单
        ipwhitelist.add("localhost");
        ipwhitelist.add("127.0.0.1");
        this.httpBasicAuthenticator = new HttpBasicAuthenticator(settings, 
        		new AuthCredentials(settings.get(Globals.SETTINGS_USERNAME), settings.get(Globals.SETTINGS_PASSWORD).getBytes()));
        this.isUnauthLogEnabled = settings.getAsBoolean(Globals.SETTINGS_LOG, false);
        String ipwhitelistStr = settings.get( Globals.SETTINGS_IPWHITELIST);
        handleIpWhiteList(ipwhitelistStr);
        //处理配置文件中的白名单
        ipwhitelistStr = AppProperties.getProperty(Globals.SETTINGS_IPWHITELIST);
        handleIpWhiteList(ipwhitelistStr);
     }

    /**
     * 处理白名单字符串
     * @param ipwhitelistStr 白名单字符串
     */
    private void handleIpWhiteList(String ipwhitelistStr){
         if ( ipwhitelistStr != null && !"".equals( ipwhitelistStr) ) {
             String[] ips = ipwhitelistStr.split(",");
             for (String ip : ips) {
                 ipwhitelist.add(ip.trim());
             }
         }
     }

    public RestHandler wrap(RestHandler original) {
        return (request, channel, client) -> {
            if (checkAndAuthenticateRequest(request, channel, client)) {
                original.handleRequest(request, channel, client);
            }
        };
    }

    private boolean checkAndAuthenticateRequest(RestRequest request, RestChannel channel, NodeClient client) throws Exception {
        ElasticsearchException forbiddenException = new TransportException("Forbidden");
        String ip = this.getAddress(request).getHostAddress();
        try {
        	//ip white list
        	if ( this.ipwhitelist.size() > 0) {
        		if ( this.ipwhitelist.contains( ip)) {
        			return true;
        		}
        	}
        	//user and pwd 
            if (this.httpBasicAuthenticator.authenticate(request)) {
                LoggerUtils.logRequest(request, getClass());
                return true;
            }

            if (this.isUnauthLogEnabled) { LoggerUtils.logUnAuthorizedRequest(request, getClass()); }
            channel.sendResponse(new BytesRestResponse(channel, RestStatus.FORBIDDEN, forbiddenException));
        } catch (Exception e) {
            if (this.isUnauthLogEnabled) { LoggerUtils.logUnAuthorizedRequest(request, getClass()); }
            channel.sendResponse(new BytesRestResponse(channel, RestStatus.FORBIDDEN, forbiddenException));
            return false;
        }
        LoggerUtils.log("http basic auth is not successed! ip = " + ip);
        return false;
    }
    
    
    private static InetAddress getAddress(RestRequest request) {
        return ((InetSocketAddress) request.getRemoteAddress()).getAddress();
    }
}
