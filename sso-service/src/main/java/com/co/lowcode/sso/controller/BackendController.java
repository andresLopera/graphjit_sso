package com.co.lowcode.sso.controller;

import java.io.IOException;
import java.net.ConnectException;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.json.JSONException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.co.lowcode.sso.model.Component;
import com.co.lowcode.sso.model.RequestQuery;
import com.co.lowcode.sso.service.AppService;
import com.co.lowcode.sso.service.FileService;
import com.co.lowcode.sso.service.QueryService;
import com.co.lowcode.sso.util.Util;

@RestController
public class BackendController {

    @Autowired
    QueryService qs;
    
    @Autowired
    FileService fs;
    
    @Autowired
    AppService as;
    
   

    @GetMapping("/getQuery")
    public  Object getQuery(@RequestParam String uuid) throws JSONException {
        return qs.query(uuid) ;
    }
    
    @GetMapping("/getReports")
    public  List getReports( HttpServletRequest request) throws JSONException {
    	String username = Util.getUserName(request.getHeader("authorization").substring(7));
		return qs.getReports(username) ;
    }
    
    @GetMapping("/getReport")
    public  List getReport(@RequestParam String uuid, HttpServletRequest request) throws JSONException {
    	String username = Util.getUserName(request.getHeader("authorization").substring(7));
		return qs.getReport(uuid,username) ;
    }
    
    @GetMapping("/getApps")
    public  List getApps( HttpServletRequest request) throws JSONException {
    	@SuppressWarnings("deprecation")
    	String username = Util.getUserName(request.getHeader("authorization").substring(7));
		return as.getApps(username);
    }
    
    @GetMapping("/getScreens")
    public  List getScreens( @RequestParam Long id, HttpServletRequest request) throws JSONException {
    	String username = Util.getUserName(request.getHeader("authorization").substring(7));
		return as.getScreens(id, username);
    }
    
    @GetMapping("/getAppsByEditor")
    public  List getAppsByEditor( HttpServletRequest request) throws JSONException {
    	@SuppressWarnings("deprecation")
    	String username = Util.getUserName(request.getHeader("authorization").substring(7));
		return as.getAppsByEditor(username);
    }
    
    @GetMapping("/getScreensByEditor")
    public  List getScreensByEditor( @RequestParam Long idApp, HttpServletRequest request) throws JSONException {
    	String username = Util.getUserName(request.getHeader("authorization").substring(7));
		return as.getScreensByEditor(idApp, username);
    }
    
    @GetMapping("/getComponentsByEditor")
    public  List getComponentsByEditor( @RequestParam Long idScreen, HttpServletRequest request) throws JSONException, ConnectException {
		String username = Util.getUserName(request.getHeader("authorization").substring(7));
		return as.getComponentsByEditor(idScreen, username);
    }
    
    @RequestMapping(value = "/saveComponent", method = RequestMethod.POST)
    public Component saveComponent(@RequestParam Long screenId, @RequestParam String parentId, 
						    		@RequestParam Integer insertPosition, @RequestParam String brickId, 
						    		HttpServletRequest request) throws java.net.ConnectException {
    	String username = Util.getUserName(request.getHeader("authorization").substring(7));
    	
        return as.saveComponent(brickId, screenId, parentId,insertPosition , username);
    }
    

    @RequestMapping(value = "/removeComponent", method = RequestMethod.GET)
    public void removeComponent(@RequestParam Long screenId, @RequestParam String componentId, @RequestParam String parentId, @RequestParam Integer position, HttpServletRequest request) throws java.net.ConnectException {
    	String username = Util.getUserName(request.getHeader("authorization").substring(7));
         as.removeComponent(screenId, componentId, parentId, position, username);
    }
    
    @GetMapping("/getComponents")
    public  List getComponents( @RequestParam Long idScreen, HttpServletRequest request) throws JSONException, ConnectException {
		String username = Util.getUserName(request.getHeader("authorization").substring(7));
		return as.getComponents(idScreen, username);
    }
    
    @RequestMapping(value = "/upload", method = RequestMethod.POST)
	public @ResponseBody Object upload(@RequestParam("file") MultipartFile multipartFile, 
		 HttpServletRequest request) throws IOException, InstantiationException, SQLException {
    	String username = Util.getUserName(request.getHeader("authorization").substring(7));
    	String uuid = fs.saveFile(multipartFile,username);
    	return uuid;
	}
    
    @GetMapping("/getFile")
    public  Object getFile(@RequestParam String uuid) throws JSONException {
        return fs.getFilePublic(uuid) ;
    }
    
    @GetMapping("/getFileByUuid")
    public  Object getFileByUuid(@RequestParam String uuid, HttpServletRequest request) throws JSONException {
    	String username = Util.getUserName(request.getHeader("authorization").substring(7));
        return fs.getFile(username, uuid);
    }

    @GetMapping("/getFilter")
    public  List getFilter(@RequestParam String uuid) throws JSONException {
        return qs.getFilters(uuid) ;
    }
    
    @RequestMapping(value = "/getParameter", method = RequestMethod.POST)
    public  List getParameter(@RequestParam String id, @RequestBody RequestQuery requestQuery) throws JSONException {
        return qs.getParameter(id, requestQuery) ;
    }
    
  

}