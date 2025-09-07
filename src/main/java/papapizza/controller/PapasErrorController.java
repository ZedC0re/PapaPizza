package papapizza.controller;

import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.RequestDispatcher;
import javax.servlet.http.HttpServletRequest;

@Controller
public class PapasErrorController implements ErrorController {

	@RequestMapping(value = "/error")
	@java.lang.SuppressWarnings("squid:S3752")
	public ModelAndView handleError(HttpServletRequest httpRequest){
		ModelAndView mav = new ModelAndView("err/error");

		int errCode = Integer.parseInt(httpRequest.getAttribute(RequestDispatcher.ERROR_STATUS_CODE).toString());
		Object errMsg = httpRequest.getAttribute(RequestDispatcher.ERROR_MESSAGE);
		Object errEx = httpRequest.getAttribute(RequestDispatcher.ERROR_EXCEPTION);

		mav.addObject("errCode",errCode);
		mav.addObject("errMsg",errMsg==null?"":errMsg.toString());
		mav.addObject("errEx",errEx==null?"":errEx.toString());

		return mav;
	}
}
