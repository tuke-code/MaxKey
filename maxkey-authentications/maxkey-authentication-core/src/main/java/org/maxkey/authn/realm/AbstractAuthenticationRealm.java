/*
 * Copyright [2020] [MaxKey of copyright http://www.maxkey.top]
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
 

package org.maxkey.authn.realm;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import javax.servlet.http.HttpServletResponse;

import org.maxkey.authn.support.rememberme.AbstractRemeberMeService;
import org.maxkey.entity.Groups;
import org.maxkey.entity.HistoryLogin;
import org.maxkey.entity.UserInfo;
import org.maxkey.persistence.db.LoginHistoryService;
import org.maxkey.persistence.db.PasswordPolicyValidator;
import org.maxkey.persistence.service.UserInfoService;
import org.maxkey.persistence.db.LoginService;
import org.maxkey.util.DateUtils;
import org.maxkey.web.WebConstants;
import org.maxkey.web.WebContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.GrantedAuthority;

/**
 * AbstractAuthenticationRealm.
 * @author Crystal.Sea
 *
 */
public abstract class AbstractAuthenticationRealm {
    private static Logger _logger = LoggerFactory.getLogger(AbstractAuthenticationRealm.class);

    protected JdbcTemplate jdbcTemplate;
    
    protected boolean provisioning;
    
    protected PasswordPolicyValidator passwordPolicyValidator;
    
    protected LoginService loginService;

    protected LoginHistoryService loginHistoryService;

    protected AbstractRemeberMeService remeberMeService;
    
    protected boolean ldapSupport;
    
    protected  AbstractAuthenticationRealm ldapAuthenticationRealm;
    
    protected UserInfoService userInfoService;
    
   

    /**
     * 
     */
    public AbstractAuthenticationRealm() {

    }

    public AbstractAuthenticationRealm(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public PasswordPolicyValidator getPasswordPolicyValidator() {
        return passwordPolicyValidator;
    }

    public LoginService getUserInfoLoginService() {
        return loginService;
    }

    public UserInfo loadUserInfo(String username, String password) {
        return loginService.loadUserInfo(username, password);
    }

    public abstract boolean passwordMatches(UserInfo userInfo, String password);
    

    public static boolean isAuthenticated() {
        if (WebContext.getUserInfo() != null) {
            return true;
        } else {
            return false;
        }
    }


    public List<Groups> queryGroups(UserInfo userInfo) {
       return loginService.queryGroups(userInfo);
    }

    /**
     * grant Authority by userinfo
     * 
     * @param userInfo
     * @return ArrayList<GrantedAuthority>
     */
    public ArrayList<GrantedAuthority> grantAuthority(UserInfo userInfo) {
        return loginService.grantAuthority(userInfo);
    }
    
    /**
     * grant Authority by grantedAuthoritys
     * 
     * @param grantedAuthoritys
     * @return ArrayList<GrantedAuthority Apps>
     */
    public ArrayList<GrantedAuthority> queryAuthorizedApps(ArrayList<GrantedAuthority> grantedAuthoritys) {
        return loginService.queryAuthorizedApps(grantedAuthoritys);
    }

    /**
     * login log write to log db
     * 
     * @param uid
     * @param j_username
     * @param type
     * @param code
     * @param message
     */
    public boolean insertLoginHistory(UserInfo userInfo, String type, String provider, String code, String message) {
        HistoryLogin historyLogin = new HistoryLogin();
        historyLogin.setSessionId(WebContext.genId());
        historyLogin.setSessionStatus(7);
        if(WebContext.getAttribute(WebConstants.CURRENT_USER_SESSION_ID) !=null) {
            historyLogin.setSessionStatus(1);
            historyLogin.setSessionId(WebContext.getAttribute(WebConstants.CURRENT_USER_SESSION_ID).toString());
        }
        
        _logger.debug("user session id is {} . ",historyLogin.getSessionId());
        
        userInfo.setLastLoginTime(DateUtils.formatDateTime(new Date()));
        userInfo.setLastLoginIp(WebContext.getRequestIpAddress());
        
        Browser browser = resolveBrowser();
        historyLogin.setBrowser(browser.getName());
        historyLogin.setPlatform(browser.getPlatform());
        historyLogin.setSourceIp(userInfo.getLastLoginIp());
        historyLogin.setProvider(provider);
        historyLogin.setCode(code);
        historyLogin.setLoginType(type);
        historyLogin.setMessage(message);
        historyLogin.setUserId(userInfo.getId());
        historyLogin.setUsername(userInfo.getUsername());
        historyLogin.setDisplayName(userInfo.getDisplayName());
        
        loginHistoryService.login(historyLogin);
        
        loginService.setLastLoginInfo(userInfo);

        return true;
    }

    /**
     * logout user and remove RemeberMe token 
     * @param response
     * @return
     */
    public boolean logout(HttpServletResponse response) {
        if (isAuthenticated()) {
            Object sessionIdAttribute = WebContext.getAttribute(WebConstants.CURRENT_USER_SESSION_ID);
            UserInfo userInfo = WebContext.getUserInfo();
            userInfo.setLastLogoffTime(DateUtils.formatDateTime(new Date()));
            
            if (sessionIdAttribute != null) {
                remeberMeService.removeRemeberMe(response);

                loginHistoryService.logoff(userInfo.getLastLogoffTime(), sessionIdAttribute.toString());
            }
            
            loginService.setLastLogoffInfo(userInfo);
            
            _logger.debug("Session " + WebContext.getAttribute(WebConstants.CURRENT_USER_SESSION_ID) + ", user "
                    + userInfo.getUsername() + " Logout, datetime " + userInfo.getLastLogoffTime() + " .");
          //remove login user session id
            WebContext.removeAttribute(WebConstants.CURRENT_USER_SESSION_ID);
        }
        return true;

    }
    
    
    public Browser  resolveBrowser() {
        Browser browser =new Browser();
        String userAgent = WebContext.getRequest().getHeader("User-Agent");
        String[] arrayUserAgent = null;
        if (userAgent.indexOf("MSIE") > 0) {
            arrayUserAgent = userAgent.split(";");
            browser.setName(arrayUserAgent[1].trim());
            browser.setPlatform(arrayUserAgent[2].trim());
        } else if (userAgent.indexOf("Trident") > 0) {
            arrayUserAgent = userAgent.split(";");
            browser.setName( "MSIE/" + arrayUserAgent[3].split("\\)")[0]);

            browser.setPlatform( arrayUserAgent[0].split("\\(")[1]);
        } else if (userAgent.indexOf("Chrome") > 0) {
            arrayUserAgent = userAgent.split(" ");
            // browser=arrayUserAgent[8].trim();
            for (int i = 0; i < arrayUserAgent.length; i++) {
                if (arrayUserAgent[i].contains("Chrome")) {
                    browser.setName( arrayUserAgent[i].trim());
                    browser.setName( browser.getName().substring(0, browser.getName().indexOf('.')));
                }
            }
            browser.setPlatform( (arrayUserAgent[1].substring(1) + " " + arrayUserAgent[2] + " "
                    + arrayUserAgent[3].substring(0, arrayUserAgent[3].length() - 1)).trim());
        } else if (userAgent.indexOf("Firefox") > 0) {
            arrayUserAgent = userAgent.split(" ");
            for (int i = 0; i < arrayUserAgent.length; i++) {
                if (arrayUserAgent[i].contains("Firefox")) {
                    browser.setName( arrayUserAgent[i].trim());
                    browser.setName(browser.getName().substring(0, browser.getName().indexOf('.')));
                }
            }
            browser.setPlatform( (arrayUserAgent[1].substring(1) + " " + arrayUserAgent[2] + " "
                    + arrayUserAgent[3].substring(0, arrayUserAgent[3].length() - 1)).trim());

        }
        
        return browser;
    }
    
    
    public class Browser{
        
        private  String platform;
        
        private  String name;
        
        public String getPlatform() {
            return platform;
        }
        public void setPlatform(String platform) {
            this.platform = platform;
        }
        public String getName() {
            return name;
        }
        public void setName(String browser) {
            this.name = browser;
        }
        
        
    }
    
}
