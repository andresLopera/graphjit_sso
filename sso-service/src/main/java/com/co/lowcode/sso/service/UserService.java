package com.co.lowcode.sso.service;

import java.io.IOException;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.mail.MessagingException;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import javax.transaction.Transactional;

import org.hibernate.transform.AliasToEntityMapResultTransformer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.ui.freemarker.FreeMarkerTemplateUtils;

import com.co.lowcode.sso.config.GeneralConfig;
import com.co.lowcode.sso.config.SmtpMailSender;
import com.co.lowcode.sso.exception.EmailInvalidException;
import com.co.lowcode.sso.exception.TokenExpiredException;
import com.co.lowcode.sso.exception.UserDuplicateException;
import com.co.lowcode.sso.exception.UserNotFoudException;
import com.co.lowcode.sso.model.RequestQuery;
import com.co.lowcode.sso.util.Util;

import freemarker.core.ParseException;
import freemarker.template.Configuration;
import freemarker.template.MalformedTemplateNameException;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import freemarker.template.TemplateNotFoundException;




@Service
public class UserService {

	@PersistenceContext
	private EntityManager entityManager;

	@Autowired
	private QueryService queryService;

	@Autowired
	RabbitMQSender rabbitMQSender;

	@Autowired
	private GeneralConfig config;

	@Autowired
	private Configuration configuration;

	@Autowired
	private SmtpMailSender smtpMailSender;
	

	@Transactional
	public Map<String, Object> createAccount(Map<String, Object> user, String roleName) throws TemplateNotFoundException,
			MalformedTemplateNameException, ParseException, IOException, TemplateException, MessagingException, SQLException {
		if (!existUser(user)) {
			String tokenActivation =UUID.randomUUID().toString();
			RequestQuery rq = new RequestQuery();
			Boolean ldap = user.get("LDAP")==null?false: (Boolean) user.get("LDAP");
			Map<String, Object> params = new LinkedHashMap<>();
			params.put("USERNAME", user.get("USERNAME"));
			params.put("FULLNAME", user.get("FULLNAME"));
			params.put("LDAP", ldap);
			params.put("ACTIVE", ldap);
			params.put("TOKENACTIVATION", tokenActivation);
			user.put("TOKENACTIVATION", tokenActivation);
			rq.setParams(params);
			if (!ldap) {
				if (Util.validateEmailAddress(user.get("USERNAME").toString())) {
					sendMailActivation(user);
				} else {
					throw new EmailInvalidException();
				}
			}
			
			Map<String, Object> createdUser = queryService.create(params, "USERS", "ID_USER");
			params = new LinkedHashMap<>();
			params.put("USER_ID", createdUser.get("ID_USER"));
			params.put("ROLE_ID", user.get("ID_ROLE"));
			
			queryService.create(params, "ROLE_USERS", "USER_ID");
			
			return createdUser;
		} else {
			throw new UserDuplicateException(user.get("USERNAME").toString());
		}
	}
	
	public void forgotPassword(String email) throws TemplateNotFoundException, MalformedTemplateNameException, ParseException, IOException, TemplateException, MessagingException{
		RequestQuery rq = new RequestQuery();
		Map<String, Object> user = new LinkedHashMap<>();
		user.put("USERNAME", email);
		if (!existUser(user)) {
			throw new UserNotFoudException(email);
		}
		user.put("TOKENACTIVATION",UUID.randomUUID().toString());
		rq.setParams(user);
		Map<String, Object> where = new LinkedHashMap<String, Object>(); 
		where.put("USERNAME", email);
		rq.setWhere(where);
		queryService.update(rq, "USERS");
		sendEmailForgotPassword(user);
	}
	
	public void sendEmailForgotPassword(Map<String, Object> user)throws TemplateNotFoundException, MalformedTemplateNameException, ParseException, IOException, TemplateException, MessagingException {
		Map<String, Object> data = new HashMap<>();
		data.put("userName", user.get("USERNAME"));
		data.put("domain", config.getUrlEmailRestorePassword());
		data.put("token", user.get("TOKENACTIVATION"));
		data.put("logo", config.getUrlLogo());
        Template  template = configuration.getTemplate(config.getEmailRestorePasswordTemplate());
        String readyParsedTemplate = FreeMarkerTemplateUtils.processTemplateIntoString(template,data);
		smtpMailSender.send(user.get("USERNAME").toString(), "Cambio de Contraseña de Cuenta", readyParsedTemplate);
	}
	

	public Boolean existUser(Map<String,Object> user) {
		String query = "SELECT USERNAME  FROM USERS WHERE USERNAME = :username";
		Query q = entityManager.createNativeQuery(query);
		q.setParameter("username", user.get("USERNAME"));
		org.hibernate.Query hibernateQuery = ((org.hibernate.jpa.HibernateQuery) q).getHibernateQuery();
		hibernateQuery.setResultTransformer(AliasToEntityMapResultTransformer.INSTANCE);
		List<Map<String, Object>> res = hibernateQuery.list();
		if (res.size() > 0) {
			return true;
		}
		return false;
	}

	public Boolean existToken(String token) {
		String query = "SELECT USERNAME  FROM USERS WHERE TOKENACTIVATION = :token";
		Query q = entityManager.createNativeQuery(query);
		q.setParameter("token", token);
		org.hibernate.Query hibernateQuery = ((org.hibernate.jpa.HibernateQuery) q).getHibernateQuery();
		hibernateQuery.setResultTransformer(AliasToEntityMapResultTransformer.INSTANCE);
		List<Map<String, Object>> res = hibernateQuery.list();
		if (res.size() > 0) {
			return true;
		}
		return false;
	}

	public void activateAccount(String token, String password) {
		BCryptPasswordEncoder bcryptEncoder = new BCryptPasswordEncoder();

		if (!existToken(token)) {
			throw new TokenExpiredException();
		}
		RequestQuery rq = new RequestQuery();
		Map<String, Object> params = new LinkedHashMap<>();
		params.put("PASSWORD", bcryptEncoder.encode(password));
		params.put("ACTIVE", true);
		params.put("TOKENACTIVATION", null);
		
		Map<String,Object>	whereRequestQuery = new LinkedHashMap<String, Object>();
		whereRequestQuery.put("TOKENACTIVATION", token);
		rq.setWhere(whereRequestQuery);
		rq.setParams(params);
		queryService.update(rq, "USERS");
	}

	private void sendMailActivation(Map<String,Object> user) throws TemplateNotFoundException, MalformedTemplateNameException,
			ParseException, IOException, TemplateException, MessagingException {
		Map<String, Object> data = new HashMap<>();
		data.put("userName", user.get("USERNAME"));
		data.put("name", user.get("FULLNAME"));
		data.put("appName", config.getAppName());
		data.put("domain", config.getUrlEmailActivation());
		data.put("token", user.get("TOKENACTIVATION"));
		data.put("logo", config.getUrlLogo());
		// rabbitMQSender.send(data);
		Template template = configuration.getTemplate(config.getEmailActivationTemplate());
		String readyParsedTemplate = FreeMarkerTemplateUtils.processTemplateIntoString(template, data);
		smtpMailSender.send(user.get("USERNAME").toString(), "Activación de Cuenta Monitoreo " + config.getCompany(),
				readyParsedTemplate);
	}
	
	public List<Map<String, Object>> getUsers(){
		String query = "SELECT ID_USER, FULLNAME, USERNAME, LDAP, ACTIVE FROM USERS";
		RequestQuery requestQuery = new RequestQuery();
		return queryService.getResultQuery(query, requestQuery);
	}
	
	public List<Map<String, Object>> getRoles(){
		String query = "SELECT ID_ROLE, NAME FROM ROLE";
		RequestQuery requestQuery = new RequestQuery();
		return queryService.getResultQuery(query, requestQuery);
	}
	
	

}
