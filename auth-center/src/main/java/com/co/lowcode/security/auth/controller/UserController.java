package com.co.lowcode.security.auth.controller;
 
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import com.co.lowcode.lineabase.model.User;
import com.co.lowcode.lineabase.oauth.model.LoginResponse;
import com.co.lowcode.lineabase.oauth.service.UserServiceI;
import com.co.lowcode.security.auth.util.Util;

import co.ldap.Ldap;

@RestController
@RequestMapping( produces = { "application/json" })
public class UserController  {
	@Autowired
	private UserServiceI userService;
	
	@RequestMapping(value = "getToken", method = RequestMethod.GET)
	public @ResponseBody ResponseEntity<LoginResponse> getToken(@RequestParam String refreshToken, 
																@RequestParam String appName,
																HttpServletRequest request) throws Exception {
		return ResponseEntity.ok(userService.getToken(request, refreshToken,appName));
	}
	
	@RequestMapping(value = "getApiToken", method = RequestMethod.GET)
	public @ResponseBody ResponseEntity<LoginResponse> getApiToken(@RequestParam String apiToken,
																   @RequestParam String appName) throws IOException {
		
		return ResponseEntity.ok(userService.getApiToken(apiToken,appName));
	}
	
	@RequestMapping(value = "getLdapUsers", method = RequestMethod.GET)
	public @ResponseBody ResponseEntity<List<Map<String,Object>>> getLdapUsers() throws Exception {
		return ResponseEntity.ok( Ldap.getInstance().getAllUsers());
	}
	
	@RequestMapping(value = "getInfoUser", method = RequestMethod.GET)
	public @ResponseBody ResponseEntity<Map<String,Object>> getInfoUser(HttpServletRequest request) throws Exception {
		String username = Util.getUserName(request.getHeader("authorization").substring(7));
		User user = userService.getUserByUserName(username);
		Map<String,Object> response = new HashMap<String, Object>();
		response.put("username", user.getUsername());
		response.put("name", user.getFullName());
		return ResponseEntity.ok( response);
	}
	
}