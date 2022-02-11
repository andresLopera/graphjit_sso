package com.co.lowcode.sso.controller;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import javax.mail.MessagingException;
import javax.servlet.http.HttpServletRequest;

import org.json.JSONException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.co.lowcode.sso.service.UserService;

import freemarker.core.ParseException;
import freemarker.template.MalformedTemplateNameException;
import freemarker.template.TemplateException;
import freemarker.template.TemplateNotFoundException;

@RestController
public class UserController {

	@Autowired
	UserService us;

	@RequestMapping(value = "/createAccount", method = RequestMethod.POST)
	public Map<String,Object> createAccount(@RequestBody Map<String, Object> user) throws JSONException, TemplateNotFoundException,
			MalformedTemplateNameException, ParseException, IOException, TemplateException, MessagingException, SQLException {
		return us.createAccount(user, "");
	}

	
	@RequestMapping(value = "/activateAccount", method = RequestMethod.GET)
	public void activateAccount(@RequestParam String token, @RequestParam String password) {
		us.activateAccount(token, password);
	}
	
	@RequestMapping(value="/forgotPassword",method={RequestMethod.GET}, produces={"application/json"})
	public void forgotPassword(@RequestParam(value="email") String email) throws TemplateNotFoundException, 
	MalformedTemplateNameException, ParseException, IOException, TemplateException, MessagingException{
		us.forgotPassword(email);	
	}
	
	@GetMapping("/getUsers")
    public  List<Map<String,Object>> getUsers(  HttpServletRequest request)  {
		return us.getUsers();
    }
	
	@GetMapping("/getRoles")
    public  List<Map<String,Object>> getRoles(  HttpServletRequest request)  {
		return us.getRoles();
    }
    


}
