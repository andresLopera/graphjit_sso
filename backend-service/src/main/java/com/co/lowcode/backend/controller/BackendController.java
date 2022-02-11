package com.co.lowcode.backend.controller;

import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.co.lowcode.backend.model.RequestQuery;
import com.co.lowcode.backend.util.Util;
import com.co.lowcode.report.service.QueryService;

@RestController
public class BackendController {

	@Autowired
	QueryService qs;
	
	
	
	@Bean
	public QueryService jwtConfig() {
		return new QueryService();
	}

	@RequestMapping(value = "/query", method = RequestMethod.POST)
	public Object query(@RequestBody RequestQuery requestQuery, HttpServletRequest request) throws Exception {
		String username = Util.getUserName(request.getHeader("authorization").substring(7));
		requestQuery.setUsername(username);
		return qs.query(requestQuery, 0);
	}

	@RequestMapping(value = "/service", method = RequestMethod.POST)
	public Object service(@RequestBody RequestQuery requestQuery, HttpServletRequest request) throws Exception {
		String username = Util.getUserName(request.getHeader("authorization").substring(7));
		requestQuery.setUsername(username);
		System.out.println(qs.buildQuery(requestQuery, 0));
		return qs.buildQuery(requestQuery, 0).get("QUERY");
	}
	
	
	
	//Devuelve un objeto si el arreglo tiene una posición
		@RequestMapping(value = "/serviceFit", method = RequestMethod.POST)
		public Object serviceFit(@RequestBody RequestQuery requestQuery, HttpServletRequest request) throws Exception {
			String username = Util.getUserName(request.getHeader("authorization").substring(7));
			requestQuery.setUsername(username);
			
			 List<Map<String, Object>> result =  (List<Map<String, Object>>)qs.buildQuery(requestQuery, 0).get("QUERY");
			 if(result.size()==1) {
				return result.get(0);
			 }
			return result;
		}
		
		//Servicio publico 
		@RequestMapping(value = "/public/service", method = RequestMethod.POST)
		public Object publicService(@RequestBody RequestQuery requestQuery, @RequestParam(defaultValue="true") Boolean isArray) throws Exception {
			List<Map<String, Object>> result =  (List<Map<String, Object>>)qs.buildQuery(requestQuery, 1).get("QUERY");
			
			if(isArray) {
				return result;
			}
			 if(result.size()==1) {
				return result.get(0);
			 }
			return result;
		}



}
