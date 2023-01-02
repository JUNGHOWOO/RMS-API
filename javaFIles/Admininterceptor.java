package com.bjworld21.cms.common;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

import javax.annotation.Resource;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.ModelAndViewDefiningException;
import org.springframework.web.servlet.mvc.WebContentInterceptor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.bjworld21.cms.accesslog.service.AccessLogService;
import com.bjworld21.cms.accesslog.service.impl.AccessLogVO;
import com.bjworld21.cms.adminuser.service.impl.AdminUserVO;
import com.bjworld21.cms.common.util.ProjectUtility;
import com.bjworld21.cms.common.util.AjaxResult;
import com.bjworld21.cms.common.util.EgovBasicLogger;
import com.bjworld21.cms.common.util.EgovDateUtil;
import com.bjworld21.cms.common.util.EgovSessionCookieUtil;
import com.bjworld21.cms.common.util.EgovStringUtil;
import com.bjworld21.cms.common.vo.SessionVO;
import com.bjworld21.cms.menu.service.MenuService;
import com.bjworld21.cms.menu.service.impl.MenuVO;
import com.bjworld21.cms.systemaccessip.service.SystemAccessIpService;


public class AdminInterceptor extends WebContentInterceptor {

	@Value("${config.ipcheck}")
    private String isIPCheck;
	
	@Resource(name="menuService")
	private MenuService menuService;
	
	@Resource(name="systemaccessipService")
    private SystemAccessIpService systemAccessIpService;
	
	@Resource(name="accesslogService")
	private AccessLogService accessLogService;
	
	@Override
	public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
			throws ServletException {
		String path = request.getServletPath();

		try {

			String remoteIp = ProjectUtility.getRemoteIp(request);
			if(isIPCheck.equals("1")) {
				int count = systemAccessIpService.selectSystemAccessIpByIP(remoteIp);
				
				if(count == 0) {
					String requestedWith = request.getHeader("x-requested-with");
					
					if(!EgovStringUtil.isEmpty(requestedWith) && requestedWith.equals("XMLHttpRequest")) {
						ObjectMapper mapper = new ObjectMapper();
						AjaxResult<String> result = new AjaxResult<>();
						result.setIsSuccess("0");
						result.setMsg("접근할수 없는 IP 입니다. 관리자에게 문의 해 주세요.");
						response.setContentType("application/json");
						response.setCharacterEncoding("utf-8");
						//response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
						response.getWriter().write(mapper.writeValueAsString(result));
						return false;
					}
					else
					{
						ModelAndView modelAndView = new ModelAndView("redirect:" + SystemConstant.AccessDenyUrl);
						throw new ModelAndViewDefiningException(modelAndView);
					}
				}
			}
			if ("XMLHttpRequest".equals(request.getHeader("X-Requested-With"))) {
				return true;
			}
			
			//TODO ajax 요청 시에도 session 체크를 하도록 수정해야 함.
			AdminUserVO sessionVO = ProjectUtility.getSessionAdminUser();
			if (sessionVO != null && sessionVO.getUserId() != null) {
				return true;
			} else {
				ModelAndView modelAndView = new ModelAndView("redirect:" + SystemConstant.AdminLoginUrl);
				throw new ModelAndViewDefiningException(modelAndView);
			}
		} catch (Exception e) {
			ModelAndView modelAndView = new ModelAndView("redirect:" + SystemConstant.AdminLoginUrl);
			throw new ModelAndViewDefiningException(modelAndView);
		}
	}

	@Override
	public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView m)
			throws Exception {
		String path = request.getServletPath();
		String boardMasterSeq = request.getParameter("bm");
		String menuSeq = request.getParameter("menuSeq");
		
		AdminUserVO sessionVO = ProjectUtility.getSessionAdminUser();
		
		try {
			if(!path.equals("/admin/dragDropUploadAjax.do")) {
				if (sessionVO != null && sessionVO.getUserId() != null) {
					
					/*Enumeration<String> params = request.getParameterNames();
					while(params.hasMoreElements()) {
						String key = params.nextElement();
						String value = request.getParameter(key);
						
						System.out.println(key + " : " + value);
					}*/
					
					/*if(path.contains("insert") 
							|| path.contains("update")
							|| path.contains("delete")
							|| path.contains("merge")
							) {
						
					}*/
					
					AccessLogVO accessLogParamVO = new AccessLogVO();
					accessLogParamVO.setUserId(sessionVO.getUserId());
					accessLogParamVO.setUserName(sessionVO.getUserName());
					accessLogParamVO.setDeptcode(sessionVO.getUserDeptSeq());
					accessLogParamVO.setDeptname(sessionVO.getUserDeptName());
					accessLogParamVO.setMenuSeq("0");
					accessLogParamVO.setLink(path);
					accessLogParamVO.setLogContents("");
					accessLogParamVO.setRemoteIP(ProjectUtility.getRemoteIp(request));
					accessLogService.insertAccessLog(accessLogParamVO);
				}
			}
		}
		catch(Exception ex) {
			EgovBasicLogger.info(ex.getMessage());
		}
		
		if ("XMLHttpRequest".equals(request.getHeader("X-Requested-With"))) {
			return;
		}

		if (path.endsWith("download.do"))
			return;
		
		try {
			
			if(sessionVO.getIsSystemManager().equals("N")) {
				
				MenuVO pathMenuVO = new MenuVO();
				pathMenuVO.setMenuLink(path);
				pathMenuVO.setBoardMasterSeq(boardMasterSeq);
				pathMenuVO.setMenuClassification("1");
				MenuVO currentMenuVO = menuService.selectMenu(pathMenuVO);
				
				if(currentMenuVO != null){
					boolean allowMenu = false; 
					
					if(sessionVO.getAuthMenus() != null) {
						String [] authMenus = sessionVO.getAuthMenus().split(",");
						
						for (int i = 0; i < authMenus.length; i++) {
							if(authMenus[i].equals(currentMenuVO.getSeq())){
								allowMenu = true;
								break;
							}
						}
					}
					if(!allowMenu) {
						ProjectUtility.writeResponseMessage(response, "<script>alert('접근 권한이 없는 메뉴 입니다.'); localStorage.removeItem('isAutoLogin'); localStorage.removeItem('userToken'); location.href='/admin/logout.do';</script>");
						return;
					}
				}
			}
			
			MenuVO menuParamVO = new MenuVO();
			menuParamVO.setBoardMasterSeq(boardMasterSeq);
			menuParamVO.setCurrentPath(path);
			menuParamVO.setMenuSeq(menuSeq);
			if(sessionVO.getIsSystemManager().equals("N")) {
				menuParamVO.setAuthMenus(sessionVO.getAuthMenus());
			}
			
			StringBuffer adminLeftMenu = menuService.getHtmlAdminLeftMenu(menuParamVO); 
			m.getModel().put("leftMenuHtml", adminLeftMenu);
			m.getModel().put("sessionVO", sessionVO);
			m.getModel().put("scriptVersion", EgovDateUtil.getCurrentDateTime("yyyyMMddHHmmss"));
		}
		catch(Exception ex) {
			logger.info(ex.getMessage());
			m.getModel().put("leftMenuHtml", "");
		}
	}
}
