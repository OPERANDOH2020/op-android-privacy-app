/*
 * Copyright (c) 2016 {UPRC}.
 *
 * OperandoApp is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * OperandoApp is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with OperandoApp.  If not, see <http://www.gnu.org/licenses/>.
 *
 * Contributors:
 *       Nikos Lykousas {UPRC}, Constantinos Patsakis {UPRC}
 * Initially developed in the context of OPERANDO EU project www.operando.eu
 */

package eu.operando.operandoapp.service;

import android.app.AlertDialog;
import android.app.Service;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.wifi.WifiManager;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;
import android.widget.EditText;
import android.widget.Toast;

import org.apache.commons.lang3.StringUtils;
import org.littleshoot.proxy.ActivityTrackerAdapter;
import org.littleshoot.proxy.FlowContext;
import org.littleshoot.proxy.HttpFilters;
import org.littleshoot.proxy.HttpFiltersAdapter;
import org.littleshoot.proxy.HttpFiltersSource;
import org.littleshoot.proxy.HttpFiltersSourceAdapter;
import org.littleshoot.proxy.HttpProxyServer;
import org.littleshoot.proxy.impl.DefaultHttpProxyServer;
import org.littleshoot.proxy.impl.ProxyUtils;
import org.littleshoot.proxy.mitm.CertificateSniffingMitmManager;

import java.io.File;
import java.io.FileOutputStream;
import java.io.StringBufferInputStream;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.SocketAddress;
import java.net.URLDecoder;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import eu.operando.operandoapp.MainContext;
import eu.operando.operandoapp.R;
import eu.operando.operandoapp.database.model.AllowedDomain;
import eu.operando.operandoapp.database.model.BlockedDomain;
import eu.operando.operandoapp.database.model.PendingNotification;
import eu.operando.operandoapp.database.DatabaseHelper;
import eu.operando.operandoapp.database.model.ResponseFilter;
import eu.operando.operandoapp.database.model.TrustedAccessPoint;
import eu.operando.operandoapp.util.MainUtil;
import eu.operando.operandoapp.util.ProcExplorer;
import eu.operando.operandoapp.util.RequestFilterUtil;
import eu.operando.operandoapp.wifi.model.WiFiDetail;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelHandlerInvoker;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.ChannelPromise;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.HttpChunkedInput;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpMessage;
import io.netty.handler.codec.http.HttpObject;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http.LastHttpContent;
import io.netty.util.concurrent.EventExecutorGroup;

/**
 * Created by nikos on 8/4/2016.
 */

public class ProxyService extends Service {

    private static final String CustomHeaderField = "OperandoMetaInfo";
    private HttpProxyServer proxy;
    private int port = 8899;
    private MainContext mainContext = MainContext.INSTANCE;
    private ProcExplorer procExplorer;
    private DatabaseHelper db;
    private RequestFilterUtil requestFilterUtil;
    private String applicationInfo;

    private String[] locationInfo, contactsInfo, macAdresses;
    private String IMEI, phoneNumber, subscriberID, carrierName, androidID;

    @Override
    public void onCreate() {
        MainUtil.initializeMainContext(getApplicationContext());
        procExplorer = new ProcExplorer(mainContext.getContext());
        db = mainContext.getDatabaseHelper();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (proxy == null) {
            Log.e("OPERANDO", "-- PROXY ON START COMMAND--");
            HttpFiltersSource filtersSource = getFiltersSource();
            try {
                proxy = DefaultHttpProxyServer.bootstrap()
                        .withPort(port)
                        .withManInTheMiddle(new CertificateSniffingMitmManager(mainContext.getAuthority()))
                        .withAllowLocalOnly(false)
                        .withFiltersSource(filtersSource)
                        .withName("OperandoProxy")
                        .plusActivityTracker(new ActivityTrackerAdapter() {

                            /*
                            Get the package responsible for each request
                             */
                            @Override
                            public void requestReceivedFromClient(FlowContext flowContext,
                                                                  HttpRequest httpRequest) {
                                if (!MainUtil.isProxyPaused(mainContext)) {
                                    httpRequest.headers().add(CustomHeaderField, procExplorer.handleCommand(flowContext.getClientAddress()));
                                }
                            }
                        })
                        .start();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return START_STICKY;
    }

    private HttpFiltersSource getFiltersSource() {
        return new HttpFiltersSourceAdapter() {
            @Override
            public HttpFilters filterRequest(HttpRequest originalRequest) {
                return new HttpFiltersAdapter(originalRequest) {
                    @Override
                    public HttpObject serverToProxyResponse(HttpObject httpObject) {
                        //check for proxy running
                        if (MainUtil.isProxyPaused(mainContext)) return httpObject;

                        if (httpObject instanceof HttpMessage) {
                            HttpMessage response = (HttpMessage) httpObject;
                            response.headers().set(HttpHeaderNames.CACHE_CONTROL, "no-cache, no-store, must-revalidate");
                            response.headers().set(HttpHeaderNames.PRAGMA, "no-cache");
                            response.headers().set(HttpHeaderNames.EXPIRES, "0");
                        }
                        try {
                            Method content = httpObject.getClass().getMethod("content");
                            if (content != null) {
                                ByteBuf buf = (ByteBuf) content.invoke(httpObject);
                                boolean flag = false;
                                List<ResponseFilter> responseFilters = db.getAllResponseFilters();
                                if (responseFilters.size() > 0) {
                                    String contentStr = buf.toString(Charset.forName("UTF-8")); //Charset.forName(Charset.forName("UTF-8")
                                    for (ResponseFilter responseFilter : responseFilters) {
                                        String toReplace = responseFilter.getContent();
                                        if (StringUtils.containsIgnoreCase(contentStr, toReplace)) {
                                            contentStr = contentStr.replaceAll("(?i)" + toReplace, StringUtils.leftPad("", toReplace.length(), '#'));
                                            flag = true;
                                        }
                                    }
                                    if (flag) {
                                        buf.clear().writeBytes(contentStr.getBytes(Charset.forName("UTF-8")));
                                    }
                                }
                            }
                        } catch (IndexOutOfBoundsException ex) {
                            ex.printStackTrace();
                            Log.e("Exception", ex.getMessage());
                        } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException ex) {
                            //ignore
                        }
                        return httpObject;
                    }

                    @Override
                    public HttpResponse clientToProxyRequest(HttpObject httpObject) {
                        //check for proxy running
                        if (MainUtil.isProxyPaused(mainContext)){
                            return null;
                        }

                        //check for trusted access point
                        String ssid = ((WifiManager) getSystemService(Context.WIFI_SERVICE)).getConnectionInfo().getSSID();
                        String bssid = ((WifiManager) getSystemService(Context.WIFI_SERVICE)).getConnectionInfo().getBSSID();
                        boolean trusted = false;
                        TrustedAccessPoint curr_tap = new TrustedAccessPoint(ssid, bssid);
                        for (TrustedAccessPoint tap : db.getAllTrustedAccessPoints()){
                            if (curr_tap.isEqual(tap)){
                                trusted = true;
                            }
                        }
                        if (!trusted){
                            return getUntrustedGatewayResponse();
                        }

                        //check for blocked url


                        //check for exfiltration
                        requestFilterUtil = new RequestFilterUtil(getApplicationContext());
                        locationInfo = requestFilterUtil.getLocationInfo();
                        contactsInfo = requestFilterUtil.getContactsInfo();
                        IMEI = requestFilterUtil.getIMEI();
                        phoneNumber = requestFilterUtil.getPhoneNumber();
                        subscriberID = requestFilterUtil.getSubscriberID();
                        carrierName = requestFilterUtil.getCarrierName();
                        androidID = requestFilterUtil.getAndroidID();
                        macAdresses = requestFilterUtil.getMacAddresses();

                        if (httpObject instanceof HttpMessage) {
                            HttpMessage request = (HttpMessage) httpObject;
                            if (request.headers().contains(CustomHeaderField)) {
                                applicationInfo = request.headers().get(CustomHeaderField);
                                request.headers().remove(CustomHeaderField);
                            }
                            if (request.headers().contains(HttpHeaderNames.ACCEPT_ENCODING)) {
                                request.headers().remove(HttpHeaderNames.ACCEPT_ENCODING);
                            }
                            if (!ProxyUtils.isCONNECT(request) && request.headers().contains(HttpHeaderNames.HOST)) {
                                String hostName = ((HttpRequest)request).uri(); //request.headers().get(HttpHeaderNames.HOST).toLowerCase();
                                if (db.isDomainBlocked(hostName))
                                    return getBlockedHostResponse(hostName);
                            }
                        }

                        String requestURI;
                        Set<RequestFilterUtil.FilterType> exfiltrated = new HashSet<>();

                        if (httpObject instanceof HttpRequest) {
                            HttpRequest request = (HttpRequest) httpObject;
                            requestURI = request.uri();
                            try {
                                requestURI = URLDecoder.decode(requestURI, "UTF-8");
                            } catch (UnsupportedEncodingException e) {
                                e.printStackTrace();
                            }
                            if (locationInfo.length > 0) {
                                //tolerate location miscalculation
                                float latitude = Float.parseFloat(locationInfo[0]);
                                float longitude = Float.parseFloat(locationInfo[1]);
                                Matcher m = Pattern.compile("\\d+\\.\\d+").matcher(requestURI);
                                List<String> floats_in_uri = new ArrayList();
                                while (m.find()) {
                                    floats_in_uri.add(m.group());
                                }
                                for (String s : floats_in_uri) {
                                    if (Math.abs(Float.parseFloat(s) - latitude) < 0.5 || Math.abs(Float.parseFloat(s) - longitude) < 0.1) {
                                        exfiltrated.add(RequestFilterUtil.FilterType.LOCATION);
                                    }
                                }
                            }
                            if (StringUtils.containsAny(requestURI, contactsInfo)) {
                                exfiltrated.add(RequestFilterUtil.FilterType.CONTACTS);
                            }
                            if (StringUtils.containsAny(requestURI, macAdresses)) {
                                exfiltrated.add(RequestFilterUtil.FilterType.MACADRESSES);
                            }
                            if (requestURI.contains(IMEI) && !IMEI.equals("")) {
                                exfiltrated.add(RequestFilterUtil.FilterType.IMEI);
                            }
                            if (requestURI.contains(phoneNumber) && !phoneNumber.equals("")){
                                exfiltrated.add(RequestFilterUtil.FilterType.PHONENUMBER);
                            }
                            if (requestURI.contains(subscriberID) && !subscriberID.equals("")) {
                                exfiltrated.add(RequestFilterUtil.FilterType.IMSI);
                            }
                            if (requestURI.contains(carrierName) && !carrierName.equals("")) {
                                exfiltrated.add(RequestFilterUtil.FilterType.CARRIERNAME);
                            }
                            if (requestURI.contains(androidID) && !androidID.equals("")) {
                                exfiltrated.add(RequestFilterUtil.FilterType.ANDROIDID);
                            }
                        }
                        try {
                            Method content = httpObject.getClass().getMethod("content");
                            if (content != null) {
                                ByteBuf buf = (ByteBuf) content.invoke(httpObject);
                                String contentStr = buf.toString(Charset.forName("UTF-8"));
                                if (locationInfo.length > 0) {
                                    //tolerate location miscalculation
                                    float latitude = Float.parseFloat(locationInfo[0]);
                                    float longitude = Float.parseFloat(locationInfo[1]);
                                    Matcher m = Pattern.compile("\\d+\\.\\d+").matcher(contentStr);
                                    List<String> floats_in_uri = new ArrayList();
                                    while (m.find()) {
                                        floats_in_uri.add(m.group());
                                    }
                                    for (String s : floats_in_uri) {
                                        if (Math.abs(Float.parseFloat(s) - latitude) < 0.5 || Math.abs(Float.parseFloat(s) - longitude) < 0.1) {
                                            exfiltrated.add(RequestFilterUtil.FilterType.LOCATION);
                                        }
                                    }
                                }
                                if (StringUtils.containsAny(contentStr, contactsInfo)) {
                                    exfiltrated.add(RequestFilterUtil.FilterType.CONTACTS);
                                }
                                if (StringUtils.containsAny(contentStr, macAdresses)) {
                                    exfiltrated.add(RequestFilterUtil.FilterType.MACADRESSES);
                                }
                                if (contentStr.contains(IMEI) && !IMEI.equals("")) {
                                    exfiltrated.add(RequestFilterUtil.FilterType.IMEI);
                                }
                                if (contentStr.contains(phoneNumber) && !phoneNumber.equals("")){
                                    exfiltrated.add(RequestFilterUtil.FilterType.PHONENUMBER);
                                }
                                if (contentStr.contains(subscriberID) && !subscriberID.equals("")) {
                                    exfiltrated.add(RequestFilterUtil.FilterType.IMSI);
                                }
                                if (contentStr.contains(carrierName) && !carrierName.equals("")) {
                                    exfiltrated.add(RequestFilterUtil.FilterType.CARRIERNAME);
                                }
                                if (contentStr.contains(androidID) && !androidID.equals("")) {
                                    exfiltrated.add(RequestFilterUtil.FilterType.ANDROIDID);
                                }
                            }
                        } catch (IndexOutOfBoundsException ex) {
                            ex.printStackTrace();
                            Log.e("Exception", ex.getMessage());
                        } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException ex) {
                            //ignore
                        }

                        //check exfiltrated list
                        if (!exfiltrated.isEmpty()) {
                            //retrieve all blocked and allowed domains
                            List<BlockedDomain> blocked = db.getAllBlockedDomains();
                            List<AllowedDomain> allowed = db.getAllAllowedDomains();
                            //get application name from app info
                            String appName = applicationInfo.replaceAll("\\(.+?\\)", "");
                            //check blocked domains
                            //if domain is stored as blocked, return a forbidden response
                            for (BlockedDomain b_dmn : blocked) {
                                if (b_dmn.info.equals(appName)) {
                                    return getForbiddenRequestResponse(applicationInfo, exfiltrated);
                                }
                            }
                            //if domain is stored as allowed, return null for actual response
                            for (AllowedDomain a_dmn : allowed) {
                                if (a_dmn.info.equals(appName)) {
                                    return null;
                                }
                            }
                            //get exfiltrated info to string array
                            String[] exfiltrated_array = new String[exfiltrated.size()];
                            int i = 0;
                            for (RequestFilterUtil.FilterType filter_type : exfiltrated) {
                                exfiltrated_array[i] = filter_type.name();
                                i++;
                            }
                            //retrieve all pending notifications
                            List<PendingNotification> pending = db.getAllPendingNotifications();
                            for (PendingNotification pending_notification : pending) {
                                //if pending notification includes specific app name and app permissions return response that a pending notification exists
                                if (pending_notification.app_info.equals(applicationInfo)) {
                                    return getPendingResponse();
                                }
                            }
                            //if none pending notification exists, display a new notification
                            int notificationId = mainContext.getNotificationId();
                            mainContext.getNotificationUtil().displayExfiltratedNotification(getBaseContext(), applicationInfo, exfiltrated, notificationId);
                            mainContext.setNotificationId(notificationId + 3);
                            //and update statistics
                            db.updateStatistics(exfiltrated);
                            return getAwaitingResponse();
                        }
                        return null;
                    }
                };
            }
        };
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        proxy.stop();
        proxy = null;
        Log.e("OPERANDO", "-- PROXY KILLED--");
    }

    //region Responses

    private HttpResponse getPendingResponse(){
        String body = "<!DOCTYPE HTML \"-//IETF//DTD HTML 2.0//EN\">\n"
                + "<html><head>\n"
                + "<title>" + "Pending Notification" + "</title>\n"
                + "</head><body>\n"
                + "<h1>The page you are about to visit requires some sensitive information. Please see your pending notifications in Operando to allow or block such requests.</h1>"
                + "</body></html>\n";
        byte[] bytes = body.getBytes(Charset.forName("UTF-8"));
        ByteBuf content = Unpooled.copiedBuffer(bytes);
        HttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.BAD_GATEWAY, content);
        response.headers().set(HttpHeaderNames.CONTENT_LENGTH, bytes.length);
        response.headers().set("Content-Type", "text/html; charset=UTF-8");
        response.headers().set("Date", ProxyUtils.formatDate(new Date()));
        response.headers().set(HttpHeaderNames.CONNECTION, "close");
        return response;
    }

    private HttpResponse getAwaitingResponse(){
        String body = "<!DOCTYPE HTML \"-//IETF//DTD HTML 2.0//EN\">\n"
                + "<html><head>\n"
                + "<title>" + "Awaiting Response" + "</title>\n"
                + "</head><body>\n"
                + "<h1>The page you are about to visit requires some sensitive information. Please see your notification bar to allow or block such requests.</h1>"
                + "</body></html>\n";
        byte[] bytes = body.getBytes(Charset.forName("UTF-8"));
        ByteBuf content = Unpooled.copiedBuffer(bytes);
        HttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.BAD_GATEWAY, content);
        response.headers().set(HttpHeaderNames.CONTENT_LENGTH, bytes.length);
        response.headers().set("Content-Type", "text/html; charset=UTF-8");
        response.headers().set("Date", ProxyUtils.formatDate(new Date()));
        response.headers().set(HttpHeaderNames.CONNECTION, "close");
        return response;
    }

    private HttpResponse getBlockedHostResponse(String hostName) {
        String body = "<!DOCTYPE HTML \"-//IETF//DTD HTML 2.0//EN\">\n"
                + "<html><head>\n"
                + "<title>" + "Bad Gateway" + "</title>\n"
                + "</head><body>\n"
                + "<h1>Host '" + hostName + "' is blocked by OperandoApp.</h1>"
                + "</body></html>\n";
        byte[] bytes = body.getBytes(Charset.forName("UTF-8"));
        ByteBuf content = Unpooled.copiedBuffer(bytes);
        HttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.BAD_GATEWAY, content);
        response.headers().set(HttpHeaderNames.CONTENT_LENGTH, bytes.length);
        response.headers().set("Content-Type", "text/html; charset=UTF-8");
        response.headers().set("Date", ProxyUtils.formatDate(new Date()));
        response.headers().set(HttpHeaderNames.CONNECTION, "close");
        return response;
    }

    private HttpResponse getUntrustedGatewayResponse() {
        String body = "<!DOCTYPE HTML \"-//IETF//DTD HTML 2.0//EN\">\n"
                + "<html><head>\n"
                + "<title>" + "Untrusted Gateway" + "</title>\n"
                + "</head><body>\n"
                + "<h1>Gateway is not included in your trusted list.</h1>"
                + "<h2>Check Operando settings.</h2>"
                + "</body></html>\n";
        byte[] bytes = body.getBytes(Charset.forName("UTF-8"));
        ByteBuf content = Unpooled.copiedBuffer(bytes);
        HttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.BAD_GATEWAY, content);
        response.headers().set(HttpHeaderNames.CONTENT_LENGTH, bytes.length);
        response.headers().set("Content-Type", "text/html; charset=UTF-8");
        response.headers().set("Date", ProxyUtils.formatDate(new Date()));
        response.headers().set(HttpHeaderNames.CONNECTION, "close");
        return response;
    }

    private HttpResponse getForbiddenRequestResponse(String applicationInfo, Set<RequestFilterUtil.FilterType> exfiltrated) {
        String body = "<!DOCTYPE HTML \"-//IETF//DTD HTML 2.0//EN\">\n"
                + "<html><head>\n"
                + "<title>" + "Forbidden" + "</title>\n"
                + "</head><body>\n"
                + "<h1>Request sent by: '" + applicationInfo + "',<br/> contains sensitive data: " + RequestFilterUtil.messageForMatchedFilters(exfiltrated) + "</h1>"
                + "</body></html>\n";
        byte[] bytes = body.getBytes(Charset.forName("UTF-8"));
        ByteBuf content = Unpooled.copiedBuffer(bytes);
        HttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.FORBIDDEN, content);
        response.headers().set(HttpHeaderNames.CONTENT_LENGTH, bytes.length);
        response.headers().set("Content-Type", "text/html; charset=UTF-8");
        response.headers().set("Date", ProxyUtils.formatDate(new Date()));
        response.headers().set(HttpHeaderNames.CONNECTION, "close");
        return response;
    }

    //endregion

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

}
