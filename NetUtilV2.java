/******************************************************************************* 
 * Copyright (C) 2012-2015 Microfountain Technology, Inc. All Rights Reserved. 
 * 
 * Unauthorized copying of this file, via any medium is strictly prohibited.   
 * Proprietary and confidential
 * 
 * Last Modified: 2015-9-17 8:59:15
 ******************************************************************************/
package cn.com.xy.sms.sdk.net;

import android.text.TextUtils;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

import cn.com.xy.sms.sdk.Iservice.XyCallBack;
import cn.com.xy.sms.sdk.constant.Constant;
import cn.com.xy.sms.sdk.db.entity.SysParamEntityManager;
import cn.com.xy.sms.sdk.log.LogManager;
import cn.com.xy.sms.sdk.net.util.ApiListUtils;
import cn.com.xy.sms.sdk.threadpool.SmartSmsThreadPoolManager;
import cn.com.xy.sms.sdk.util.StringUtils;
import cn.com.xy.sms.sdk.util.XyUtil;

public class NetUtilV2 {
    /* SDK-999 yeweicong 20171219 start */
    private static String REQ_BASE_HOST_TOKEN_URL = "http://sdkapiv2.bizport.cn/";
    private static String REQ_BASE_HOST_TOKEN_URL_HTTPS = "https://sdkapiv2.bizport.cn/";
    /* SDK-999 yeweicong 20171219 end */

    private static String REQ_BASE_PROTOCOL = "https://";

    static{
        String url = SysParamEntityManager.getStringParam(Constant.getContext(), Constant.CUSTOM_SDK_NEW_ENGINE_URL);
        if(!StringUtils.isNull(url)){
            /* SDK-999 yeweicong 20171219 start */
            REQ_BASE_HOST_TOKEN_URL = url;
            REQ_BASE_HOST_TOKEN_URL_HTTPS = url;
            /* SDK-999 yeweicong 20171219 end */
        }
        String protocol = SysParamEntityManager.getStringParam(Constant.getContext(), Constant.SMARTSMS_PROTOCOL);
        if(!StringUtils.isNull(protocol)){
            REQ_BASE_PROTOCOL = protocol;
        }
    }
    private static String RESPONSE_URL = null;

    public static final String REQ_QUERY_TOEKN = "token";
    public static final String REQ_QUERY_VERSIONS_UPDATE = "version/data_update";
    public static final String REQ_QUERY_VERSIONS_DOWNLOAD = "version/data_download";
    public static final String REQ_QUERY_VERSIONS_PACKAGE = "version/package";
    
    public static final String REQ_VERIFY_AUTH = "data/auth-infos";
    
    private static final List<String> sResponseUrlList = new ArrayList<String>();
    
    private static boolean isRequestTokenIng = false;

    public static void queryNewTokenRequestIfNeed(boolean newthread, final XyCallBack tokenCallback) {
        /*SDK-953 pengyanjun 20180108 start*/
        if (!isTokenExpired()) {
            /*SDK-953 pengyanjun 20180108 end*/
            LogManager.e("v2_update", "queryNewTokenRequest V2, not need now");
            /* SDK-955 kangwenbo 20171124 start */
            XyUtil.doXycallBackResult(tokenCallback, Constant.STATE_CODE_OK," not need request token");
            /* SDK-955 kangwenbo 20171124 end */
            return;
        }
        queryNewTokenRequest(newthread, tokenCallback);
    }
    /*SDK-953 pengyanjun 20180108 start*/
    /**
     * 判断Token是否过期
     * @return true:表示token为空或已过期
     */
    private static boolean isTokenExpired(){
        String token = SysParamEntityManager.getStringParam(Constant.getContext(), NewXyHttpRunnableV2.V2_TOKEN);
        String tokenExpiredTime = SysParamEntityManager.getStringParam(Constant.getContext(), NewXyHttpRunnableV2.V2_TOKEN_EXPIRED_TIME);
        if (StringUtils.isNull2(token) || StringUtils.isNull2(tokenExpiredTime) ){
            return true;

        }
        try{
            if (System.currentTimeMillis() <= Long.parseLong(tokenExpiredTime)){
                return false;
            }
        }
        catch (Exception e){
            LogManager.e("peng", "isTokenExpired : " + e.toString());
        }
        return true;
    }
    /*SDK-953 pengyanjun 20180108 end*/

    /*SDK-879 kangwenbo 20170905 start */
    public static void queryNewTokenRequest(boolean newthread, final XyCallBack tokenCallBack){
        LogManager.e("v2_update", "queryNewTokenRequest V2...");
        /* SDK-914 kangwenbo 20171018 start*/
        if(isRequestTokenIng){
            /* SDK-955 kangwenbo 20171124 start */
            XyUtil.doXycallBackResult(tokenCallBack, Constant.STATE_CODE_ERROR_UNSPECIFY,"request token ing");
            /* SDK-955 kangwenbo 20171124 end */
            return;
        }
        isRequestTokenIng = true;
        /* SDK-914 kangwenbo 20171018 end*/

        try {
            XyCallBack callback = new XyCallBack() {
                @Override
                public void execute(Object... obj) {
                    isRequestTokenIng = false;
                    if (obj != null && obj.length > 1) {
                        int resutCode = (Integer) obj[0];
                        if (resutCode == 0) {
                            JSONObject responseObj = (JSONObject) obj[1];
                            handleNewTokenResponse(responseObj);
                            XyUtil.doXycallBackResult(tokenCallBack,Constant.STATE_CODE_OK );
                        }
                        else {
                            //LogManager.e("v2_update", "queryNewTokenRequest error:" + resutCode + "," + obj);
                            XyUtil.doXycallBackResult(tokenCallBack, Constant.STATE_CODE_ERROR_UNSPECIFY);
                        }
                    }
                    else
                        XyUtil.doXycallBackResult(tokenCallBack, Constant.STATE_CODE_ERROR_UNSPECIFY);
                }
            };
            String dataString = getTokenReqData();
            /* SDK-999 yeweicong 20171219 start */
            String tokenHost = NetUtil.isUseHttps() ? REQ_BASE_HOST_TOKEN_URL_HTTPS : REQ_BASE_HOST_TOKEN_URL;
            /* SDK-999 yeweicong 20171219 end */
            executeNewServiceHttpRequest(tokenHost + REQ_QUERY_TOEKN, dataString, callback, newthread, true, null);
        }
        catch (Throwable e) {
            isRequestTokenIng = false;
            /* SDK-955 kangwenbo 20171124 start */
            XyUtil.doXycallBackResult(tokenCallBack, Constant.STATE_CODE_ERROR_UNSPECIFY);
            /* SDK-955 kangwenbo 20171124 end */
            LogManager.e(Constant.TAG, "QueryTokenRequest: ", e);
        }
    }
    /*SDK-879 kangwenbo 20170905 end */


    public static void executeHttpRequest(boolean newthread, String requestUrl, String reqeustContent,
            final XyCallBack callback) {
        /*SDK-953 pengyanjun 20180108 start*/
        if (isTokenExpired()) {
            /*SDK-953 pengyanjun 20180108 end*/
            queryNewTokenRequest(true, null);
            /* SDK-955 kangwenbo 20171124 start */
            XyUtil.doXycallBackResult(callback, Constant.STATE_CODE_ERROR_UNSPECIFY,"not have token");
            /* SDK-955 kangwenbo 20171124 end */
            return;
        }
        /*SDK-954 pengyanjun 20180108 start*/
        String api = ApiListUtils.queryApiUrl();
        if (!StringUtils.isNull(reqeustContent)) {
            executeNewServiceHttpRequest(api + requestUrl, reqeustContent, callback, newthread, false, null);
        }
        /*SDK-954 pengyanjun 20180108 end*/
    }

    private static boolean responseUrlIsExist() {
        boolean opRet = false;
        if (StringUtils.isNull(RESPONSE_URL)) {
            RESPONSE_URL = SysParamEntityManager.getStringParam(Constant.getContext(),
                    NewXyHttpRunnableV2.V2_RESPONSE_URL);
        }
        if (!StringUtils.isNull(RESPONSE_URL)) {
            opRet = true;
        }
        LogManager.e("update", "responseUrlIsExist:" + opRet);
        return opRet;
    }

    private static void handleNewTokenResponse(JSONObject jsonObject) {
		LogManager.e("pengyanjun", "handleNewTokenResponse=" + jsonObject);
        if (jsonObject == null) {
            return;
        }
        /*SDK-954 pengyanjun 20180108 start*/
        try {
            JSONObject bodyObject = jsonObject.optJSONObject("body");
            String aesKey = bodyObject.optString("aeskey");
            String aesIv = bodyObject.optString("iv");
            String token = bodyObject.optString("token");
            long expire = bodyObject.optLong("expire",0);
            long tokenExpiredTime = expire * 1000 + System.currentTimeMillis();
            NewXyHttpRunnableV2.updateTokenAndKeys(token, aesKey, aesIv);
            SysParamEntityManager.setParam(NewXyHttpRunnableV2.V2_TOKEN, token);
            SysParamEntityManager.setParam(NewXyHttpRunnableV2.V2_TOKEN_EXPIRED_TIME, String.valueOf(tokenExpiredTime));
            SysParamEntityManager.setParam(NewXyHttpRunnableV2.V2_AESKEY, aesKey);
            SysParamEntityManager.setParam(NewXyHttpRunnableV2.V2_AESIV, aesIv);

            JSONArray array = bodyObject.optJSONArray("apilist");
            ApiListUtils.saveApiList(array);
        }
        catch (Exception e) {
            LogManager.e("peng", "handleNewTokenResponse : " + e.toString());
        }
        /*SDK-954 pengyanjun 20180108 end*/
    }

    private static String getTokenReqData() {
        String jsonData = "{\"timestamp\":\"" + System.currentTimeMillis() + "\"}";
        return jsonData;
    }

    private static void executeNewServiceHttpRequest(String requestUrl, String reqeustContent, XyCallBack callBack,
            boolean newThread, boolean isLogin, Map<String, String> extendMap) {
        LogManager.e("v2_update", "req url:" + requestUrl + ",new thd:" + newThread);
        LogManager.e("v2_update", "req data:" + reqeustContent);
        LogManager.e("v2_update", "*********************************************");
        /* SDK-875/yangzhi/2017.09.01---start--- */
        if (!NetUtil.checkAccessNetWork(2)) {
            /* SDK-985 yeweicong 20171205 start */
            callBack.execute(-1, "error network"); // 访问失败
            /* SDK-985 yeweicong 20171205 end */
            return;
        }
        /* SDK-875/yangzhi/2017.09.01---end--- */
        NewXyHttpRunnableV2 runnable = new NewXyHttpRunnableV2(requestUrl, reqeustContent, callBack, isLogin, extendMap);
        if (newThread) {
            SmartSmsThreadPoolManager.pool.execute(runnable);
        }
        else {
            runnable.run();
        }
    }

    public static void changeUrl() {
        if (sResponseUrlList.size() > 0) {
            String originalUrl = SysParamEntityManager.getStringParam(Constant.getContext(),
                    NewXyHttpRunnableV2.V2_RESPONSE_URL);
            boolean isContain = false;
            if (sResponseUrlList.contains(originalUrl)) {
                sResponseUrlList.remove(originalUrl);
                isContain = true;
            }
            String responseUrl = sResponseUrlList.size() == 0 ? originalUrl : sResponseUrlList.get(new Random()
                    .nextInt(sResponseUrlList.size()));

            if (isContain)
                sResponseUrlList.add(originalUrl);
            RESPONSE_URL = responseUrl;
            SysParamEntityManager.setParam(NewXyHttpRunnableV2.V2_RESPONSE_URL, responseUrl);
        }
    }

   
}
