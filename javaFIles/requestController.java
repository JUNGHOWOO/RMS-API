package com.bjworld21.cms.request.web;

import com.bjworld21.cms.common.util.AjaxResult;
import com.bjworld21.cms.common.util.ProjectUtility;
import com.bjworld21.cms.common.SystemConstant;
import com.bjworld21.cms.common.util.EgovSessionCookieUtil;
import com.bjworld21.cms.common.util.EgovStringUtil;
import com.bjworld21.cms.common.util.UploadFileVO;
import com.bjworld21.cms.common.util.EgovBasicLogger;
import com.bjworld21.cms.common.util.EgovFileMngUtil;
import com.bjworld21.cms.common.vo.SessionVO;
import com.bjworld21.cms.request.service.RequestService;
import com.bjworld21.cms.request.service.impl.RequestVO;
import com.bjworld21.cms.requestattachfile.service.RequestAttachFileService;
import com.bjworld21.cms.requestattachfile.service.impl.RequestAttachFileVO;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.json.JSONArray;
import org.json.JSONObject;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;
import javax.annotation.Resource;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.List;
import java.util.UUID;
import java.io.File;
import java.util.List;
import java.util.HashMap;

@Controller
@RequestMapping("/admin")
public class RequestController {

	Logger logger = LoggerFactory.getLogger(RequestController.class);

	@Value("${Globals.fileStorePath}")
	private String attachFileSavePath;

	@Resource(name = "requestService")
	private RequestService requestService;

	@Resource(name = "requestattachfileService")
	private RequestAttachFileService requestattachfileService;

	@RequestMapping("/request.do")
	public String request(HttpServletRequest request, Model model) throws Exception {
		return "request/request.at";
	}

	@RequestMapping(value = "/getRequestListAjax.do")
	@ResponseBody
	public HashMap<String, Object> getRequestListAjax(HttpServletRequest request, RequestVO paramVO) throws Exception {

		List<?> dataList = requestService.selectRequestList(paramVO);
		// Total Count
		Integer total = requestService.selectRequestListTotCnt(paramVO);

		HashMap<String, Object> listMap = new HashMap<String, Object>();
		listMap.put("draw", paramVO.getDraw());
		listMap.put("recordsTotal", total);
		listMap.put("recordsFiltered", total);
		listMap.put("data", dataList);
		return listMap;
	}

	@SuppressWarnings("unchecked")
	@RequestMapping(value = "/mergeRequestAjax.do")
	@ResponseBody
	public AjaxResult<String> mergeRequestAjax(HttpServletRequest request, RequestVO paramVO) throws Exception {

		AjaxResult<String> result = new AjaxResult<>();
		try {

			/**
			 * DB 인서트 로직
			 */
/*			if (StringUtils.isEmpty(paramVO.getSeq()))
				paramVO.setSeq(null);

			requestService.saveRequest(paramVO);

			result.setData("");
			result.setIsSuccess(SystemConstant.AJAX_SUCCESS);
			result.setMsg("데이터를 저장하였습니다.");
*/
			
			/**
			 * API 요청 로직
			 */
			JSONObject json = new JSONObject();
			json.put("requestSubject",paramVO.getRequestSubject());
			json.put("requestContents",paramVO.getRequestContents());
			json.put("regUserName", paramVO.getRegUserName());
			json.put("regUserPhone", paramVO.getRegUserPhone());
			
			JSONArray fileArray = new JSONArray();
			if(paramVO.getAttachSaveFileName() != null) {
				for (int i = 0; i < paramVO.getAttachSaveFileName().length; i++) {
					String oriFilename = paramVO.getAttachOriFileName()[i];
					String saveFilename = paramVO.getAttachSaveFileName()[i];
					String filesize = paramVO.getAttachFileSize()[i];
					
					File attachFile = new File(attachFileSavePath + File.separator + "request" + File.separator + saveFilename);
					JSONObject fileInfo = new JSONObject();
					fileInfo.put("oriFilename", oriFilename);
					fileInfo.put("saveFilename", saveFilename);
					fileInfo.put("filesize", filesize);
					fileInfo.put("fileData", ProjectUtility.fileToString(attachFile));
					fileArray.put(fileInfo);
				}
			}
			json.put("fileArray", fileArray);
			
			String apiUrl = "http://localhost:3000/v2/rms/createrequest";
			String method = "POST";
			HashMap<String, String> propertiesMap = new HashMap<>();
			propertiesMap.put("Content-Type", "application/json; charset=utf-8");
			propertiesMap.put("BJRMS-APIKEY", paramVO.getCompanyApiKey());
			//{requestSubject:"asdfasdf", requestContents:"asdfasdfsadf"}
			String responseData = ProjectUtility.requestUrl(apiUrl, method, propertiesMap, json.toString());
			logger.info(responseData);
			
		} catch (Exception e) {
			logger.error(e.getMessage());
			result.setIsSuccess(SystemConstant.AJAX_FAIL);
			result.setMsg(String.format(SystemConstant.AJAX_ERROR_MESSAGE, "데이터를 저장 중"));
		}

		return result;
	}	

	@RequestMapping(value = "/selectRequestAjax.do")
	@ResponseBody
	public AjaxResult<RequestVO> selectRequestAjax(HttpServletRequest request, RequestVO paramVO) throws Exception {

		AjaxResult<RequestVO> result = new AjaxResult<>();

		try {
			RequestVO viewVO = requestService.selectRequest(paramVO);

			if (viewVO != null) {
				RequestAttachFileVO fileParamVO = new RequestAttachFileVO();
				fileParamVO.setRequestSeq(viewVO.getSeq());

				List<RequestAttachFileVO> listAttachFile = requestattachfileService
						.selectRequestAttachFileList(fileParamVO);
				viewVO.setListAttachFile(listAttachFile);
			}

			result.setIsSuccess(SystemConstant.AJAX_SUCCESS);
			result.setData(viewVO);
		} catch (Exception e) {
			logger.error(e.getMessage());
			result.setIsSuccess(SystemConstant.AJAX_FAIL);
			result.setMsg(String.format(SystemConstant.AJAX_ERROR_MESSAGE, " 데이터를 불러 오는 중"));
		}

		return result;
	}

	@RequestMapping(value = "/deleteRequestAjax.do")
	@ResponseBody
	public AjaxResult<String> deleteRequestAjax(HttpServletRequest request, RequestVO paramVO) throws Exception {

		AjaxResult<String> result = new AjaxResult<>();

		try {
			requestService.deleteRequest(paramVO);
			result.setIsSuccess(SystemConstant.AJAX_SUCCESS);
			result.setData("");
			result.setMsg("데이터를 삭제하였습니다.");
		} catch (Exception e) {
			logger.error(e.getMessage());
			result.setIsSuccess(SystemConstant.AJAX_FAIL);
			result.setMsg(String.format(SystemConstant.AJAX_ERROR_MESSAGE, " 데이터를 삭제 하는 중"));
		}

		return result;
	}
	
	@RequestMapping(value = "/completeRequestAjax.do")
    @ResponseBody
    public AjaxResult<String> completeRequestAjax(HttpServletRequest request
            , RequestVO paramVO ) throws Exception{
    	
        AjaxResult<String> result = new AjaxResult<>();
        try {       
        	requestService.completeRequest(paramVO);
			result.setData("");
			result.setIsSuccess(SystemConstant.AJAX_SUCCESS);
			result.setMsg("처리완료 상태로 변경하였습니다.");
        	
		} catch (Exception e) {
            logger.error(e.getMessage());
			result.setIsSuccess(SystemConstant.AJAX_FAIL);
			result.setMsg(String.format(SystemConstant.AJAX_ERROR_MESSAGE, "상태 변경 중"));
		}
        
        return result;
    }

	@RequestMapping("/requestexceldownload.do")
	public void applicationreceptionexceldownload(HttpServletRequest request, HttpServletResponse response,
			RequestVO paramVO) throws Exception {
		try {
			requestService.excelDownload(request, response, paramVO);
		} catch (Exception ex) {
			ProjectUtility.writeResponseMessage(response,
					"<script>alert('엑셀 파일 생성 중 오류가 발생하였습니다.'); history.back(); </script>");
		}
	}

	@RequestMapping(value = "/downloadrequestfile.do")
	@ResponseBody
	public void downloadrequestfile(HttpServletRequest request, HttpServletResponse response,
			RequestAttachFileVO paramVO) throws Exception {

		try {
			RequestAttachFileVO viewVO = requestattachfileService.selectRequestAttachFile(paramVO);
			if (viewVO != null) {
				String saveFileName = viewVO.getSaveFilename();
				String uploadFolderPath = attachFileSavePath + File.separator + "request";
				EgovFileMngUtil.downFile(request, response, viewVO.getOriFilename(),
						uploadFolderPath + File.separator + saveFileName);
			} else
				throw new Exception("요청한 파일데이터가 존재하지 않습니다.");
		} catch (Exception ex) {
			try {
				ProjectUtility.writeResponseMessage(response,
						"<script>alert('다운로드 하려는 파일에 문제가 발생하였습니다.'); history.back(); </script>");
			} catch (Exception e) {
				EgovBasicLogger.info(e.getMessage());
			}
		}
	}

	@RequestMapping(value = "/deleteRequestAttachFileAjax.do")
	@ResponseBody
	public AjaxResult<String> deleteRequestAttachFile(HttpServletRequest request, RequestAttachFileVO paramVO)
			throws Exception {

		AjaxResult<String> result = new AjaxResult<>();

		try {
			RequestAttachFileVO viewFile = requestattachfileService.selectRequestAttachFile(paramVO);

			if (viewFile != null) {
				String saveFileName = viewFile.getSaveFilename();
				if (!EgovStringUtil.isEmpty(saveFileName)) {
					String saveFullPath = attachFileSavePath + File.separator + "request" + File.separator
							+ saveFileName;

					try {
						File f = new File(saveFullPath);
						f.delete();
					} catch (Exception ex) {
						EgovBasicLogger.info(ex.getMessage());
					}
				}
			}

			requestattachfileService.deleteRequestAttachFile(paramVO);
			result.setIsSuccess(SystemConstant.AJAX_SUCCESS);
			result.setData("");
			result.setMsg("첨부파일을 삭제하였습니다.");
		} catch (Exception e) {
			logger.error(e.getMessage());
			result.setIsSuccess(SystemConstant.AJAX_FAIL);
			result.setMsg(String.format(SystemConstant.AJAX_ERROR_MESSAGE, "첨부파일을 삭제 하는 중"));
		}

		return result;
	}

}