package org.maxkey.web.apps.contorller;

import java.util.List;

import org.maxkey.constants.OPERATEMESSAGE;
import org.maxkey.constants.PROTOCOLS;
import org.maxkey.crypto.ReciprocalUtils;
import org.maxkey.dao.service.AppsTokenBasedDetailsService;
import org.maxkey.domain.apps.AppsTokenBasedDetails;
import org.maxkey.web.WebContext;
import org.maxkey.web.message.Message;
import org.maxkey.web.message.MessageType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.ModelAndView;


@Controller
@RequestMapping(value={"/apps/tokenbased"})
public class TokenBasedDetailsController  extends BaseAppContorller {
	final static Logger _logger = LoggerFactory.getLogger(TokenBasedDetailsController.class);
	
	@Autowired
	AppsTokenBasedDetailsService tokenBasedDetailsService;
	
	
	@RequestMapping(value = { "/forwardAdd" })
	public ModelAndView forwardAdd() {
		ModelAndView modelAndView=new ModelAndView("apps/tokenbased/appAdd");
		AppsTokenBasedDetails tokenBasedDetails =new AppsTokenBasedDetails();
		tokenBasedDetails.setProtocol(PROTOCOLS.TOKENBASED);
		tokenBasedDetails.setSecret(ReciprocalUtils.generateKey(ReciprocalUtils.Algorithm.AES));
		tokenBasedDetails.setAlgorithmKey(tokenBasedDetails.getSecret());
		modelAndView.addObject("model",tokenBasedDetails);
		return modelAndView;
	}
	
	
	@RequestMapping(value={"/add"})
	public ModelAndView insert(@ModelAttribute("tokenBasedDetails") AppsTokenBasedDetails tokenBasedDetails) {
		_logger.debug("-Add  :" + tokenBasedDetails);
		
		transform(tokenBasedDetails);
		
		tokenBasedDetails.setAlgorithmKey(tokenBasedDetails.getSecret());
		
		if (tokenBasedDetailsService.insert(tokenBasedDetails)&&appsService.insert(tokenBasedDetails)) {
			  new Message(WebContext.getI18nValue(OPERATEMESSAGE.INSERT_SUCCESS),MessageType.success);
			
		} else {
			  new Message(WebContext.getI18nValue(OPERATEMESSAGE.INSERT_SUCCESS),MessageType.error);
		}
		return   WebContext.forward("forwardUpdate/"+tokenBasedDetails.getId());
	}
	
	@RequestMapping(value = { "/forwardUpdate/{id}" })
	public ModelAndView forwardUpdate(@PathVariable("id") String id) {
		ModelAndView modelAndView=new ModelAndView("apps/tokenbased/appUpdate");
		AppsTokenBasedDetails tokenBasedDetails=tokenBasedDetailsService.getAppDetails(id);
		decoderSecret(tokenBasedDetails);
		String algorithmKey=passwordReciprocal.decoder(tokenBasedDetails.getAlgorithmKey());
		tokenBasedDetails.setAlgorithmKey(algorithmKey);
		WebContext.setAttribute(tokenBasedDetails.getId(), tokenBasedDetails.getIcon());

		modelAndView.addObject("model",tokenBasedDetails);
		return modelAndView;
	}
	/**
	 * modify
	 * @param application
	 * @return
	 */
	@RequestMapping(value={"/update"})  
	public ModelAndView update(@ModelAttribute("tokenBasedDetails") AppsTokenBasedDetails tokenBasedDetails) {
		//
		_logger.debug("-update  application :" + tokenBasedDetails);
		transform(tokenBasedDetails);
		tokenBasedDetails.setAlgorithmKey(tokenBasedDetails.getSecret());
		if (tokenBasedDetailsService.update(tokenBasedDetails)&&appsService.update(tokenBasedDetails)) {
			  new Message(WebContext.getI18nValue(OPERATEMESSAGE.UPDATE_SUCCESS),MessageType.success);
			
		} else {
			  new Message(WebContext.getI18nValue(OPERATEMESSAGE.UPDATE_ERROR),MessageType.error);
		}
		return   WebContext.forward("forwardUpdate/"+tokenBasedDetails.getId());
	}
	

	@ResponseBody
	@RequestMapping(value={"/delete/{id}"})
	public Message delete(@PathVariable("id") String id) {
		_logger.debug("-delete  application :" + id);
		if (tokenBasedDetailsService.remove(id)&&appsService.remove(id)) {
			return  new Message(WebContext.getI18nValue(OPERATEMESSAGE.DELETE_SUCCESS),MessageType.success);
			
		} else {
			return  new Message(WebContext.getI18nValue(OPERATEMESSAGE.DELETE_SUCCESS),MessageType.error);
		}
	}
	
	
}