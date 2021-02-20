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
 

package org.maxkey.authn.support.wsfederation;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.maxkey.authn.AbstractAuthenticationProvider;
import org.maxkey.configuration.ApplicationConfig;
import org.maxkey.constants.ConstantsLoginType;
import org.maxkey.util.StringUtils;
import org.maxkey.web.WebContext;
import org.opensaml.saml1.core.impl.AssertionImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.servlet.AsyncHandlerInterceptor;


public class HttpWsFederationEntryPoint implements AsyncHandlerInterceptor {
	private static final Logger _logger = LoggerFactory.getLogger(HttpWsFederationEntryPoint.class);
	
    boolean enable;
    
  	ApplicationConfig applicationConfig;
    
    AbstractAuthenticationProvider authenticationProvider ;
    
	WsFederationService wsFederationService;
	
	 @Override
	 public boolean preHandle(HttpServletRequest request,HttpServletResponse response, Object handler) throws Exception {
		 boolean isAuthenticated= WebContext.isAuthenticated();
		 String wsFederationWA = request.getParameter(WsFederationConstants.WA);
		 String wsFederationWResult = request.getParameter(WsFederationConstants.WRESULT);
		 
		 if(!enable 
				 || isAuthenticated 
				 || !applicationConfig.getLoginConfig().isWsFederation()
				 || wsFederationWA == null){
			 return true;
		 }
		 
		 _logger.debug("WsFederation Login Start ...");
		 _logger.info("Request url : "+ request.getRequestURL());
		 _logger.info("Request URI : "+ request.getRequestURI());
		 _logger.info("Request ContextPath : "+ request.getContextPath());
		 _logger.info("Request ServletPath : "+ request.getServletPath());
		 _logger.debug("RequestSessionId : "+ request.getRequestedSessionId());
		 _logger.debug("isRequestedSessionIdValid : "+ request.isRequestedSessionIdValid());
		 _logger.debug("getSession : "+ request.getSession(false));
		 
		// session not exists，session timeout，recreate new session
		 if(request.getSession(false) == null) {
		    _logger.info("recreate new session .");
			request.getSession(true);
		 }
		 
		 _logger.info("getSession.getId : "+ request.getSession().getId());

		//for WsFederation Login
		 _logger.debug("WsFederation : " + wsFederationWA +" , wsFederationWResult : " + wsFederationWResult);
		 if(applicationConfig.getLoginConfig().isWsFederation()
				 && StringUtils.isNotEmpty(wsFederationWA) 
				 && wsFederationWA.equalsIgnoreCase(WsFederationConstants.WSIGNIN)){
			 _logger.debug("wresult : {}"+wsFederationWResult);

			 final String wctx = request.getParameter(WsFederationConstants.WCTX);
			 _logger.debug("wctx : {}"+ wctx);

            // create credentials
            final AssertionImpl assertion = WsFederationUtils.parseTokenFromString(wsFederationWResult);
            //Validate the signature
            if (assertion != null && WsFederationUtils.validateSignature(assertion, wsFederationService.getWsFederationConfiguration().getSigningCertificates())) {
                final WsFederationCredential wsFederationCredential = WsFederationUtils.createCredentialFromToken(assertion);

                if (wsFederationCredential != null && wsFederationCredential.isValid(wsFederationService.getWsFederationConfiguration().getRelyingParty(),
                		wsFederationService.getWsFederationConfiguration().getIdentifier(),
                		wsFederationService.getWsFederationConfiguration().getTolerance())) {

                    //Give the library user a chance to change the attributes as necessary
                    if (wsFederationService.getWsFederationConfiguration().getAttributeMutator() != null) {
                    	wsFederationService.getWsFederationConfiguration().getAttributeMutator().modifyAttributes(
                    			wsFederationCredential.getAttributes(),
                    			wsFederationService.getWsFederationConfiguration().getUpnSuffix());
                    }

                    authenticationProvider.trustAuthentication(
                    		wsFederationCredential.getAttributes().get("").toString(),
                    		ConstantsLoginType.WSFEDERATION,
                    		"","","success");
                    return true;
                } else {
                    _logger.warn("SAML assertions are blank or no longer valid.");
                }
            } else {
                _logger.error("WS Requested Security Token is blank or the signature is not valid.");
            }
		 }
		
		 return true;
	 }

	 public HttpWsFederationEntryPoint() {
	        super();
	 }

    public HttpWsFederationEntryPoint (boolean enable) {
        super();
        this.enable = enable;
    }

    public HttpWsFederationEntryPoint(AbstractAuthenticationProvider authenticationProvider, WsFederationService wsFederationService,
			ApplicationConfig applicationConfig, boolean enable) {
		super();
		this.authenticationProvider = authenticationProvider;
		this.wsFederationService = wsFederationService;
		this.applicationConfig = applicationConfig;
		this.enable = enable;
	}

	public boolean isEnable() {
        return enable;
    }

    public void setEnable(boolean enable) {
        this.enable = enable;
    }

	public void setApplicationConfig(ApplicationConfig applicationConfig) {
		this.applicationConfig = applicationConfig;
	}

	public void setAuthenticationProvider(AbstractAuthenticationProvider authenticationProvider) {
		this.authenticationProvider = authenticationProvider;
	}

	public void setWsFederationService(WsFederationService wsFederationService) {
		this.wsFederationService = wsFederationService;
	}


	
}