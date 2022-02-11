package com.co.lowcode.sso.service;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import javax.transaction.Transactional;

import org.hibernate.transform.AliasToEntityMapResultTransformer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.co.lowcode.sso.config.GeneralConfig;
import com.co.lowcode.sso.model.RequestQuery;
import com.co.lowcode.sso.util.Util;

@Service
public class FileService {

	@PersistenceContext
	private EntityManager entityManager;
	
	@Autowired
	private GeneralConfig config;
	
	@Autowired
	public QueryService queryService;
	

	public Map<String, Object> getFilePublic(String uuid) {
		String query = "SELECT NAME, PATH FROM FILES WHERE UUID = :UUID";
		Query q = entityManager.createNativeQuery(query);
		q.setParameter("UUID", uuid);
		org.hibernate.Query hibernateQuery = ((org.hibernate.jpa.HibernateQuery) q).getHibernateQuery();
		hibernateQuery.setResultTransformer(AliasToEntityMapResultTransformer.INSTANCE);
		List<Map<String, Object>> res = hibernateQuery.list();
		if (res.size() > 0) {
			Map<String, Object> response = res.get(0);
			String path = response.get("PATH").toString();
			Map<String, Object> r = new HashMap<>(); 
			r.put("fileEncoded", Util.encodeFileToBase64(path));
			r.put("filename",  response.get("NAME").toString());
			return r;
		}
		return null;
	}
	
	public Map<String, Object> getFile(String username, String uuid) {
		String query = "SELECT NAME, PATH FROM FILES WHERE UUID = :UUID AND (USERNAME = :USERNAME OR USERNAME IS NULL)";
		Query q = entityManager.createNativeQuery(query);
		q.setParameter("UUID", uuid);
		q.setParameter("USERNAME", username);
		org.hibernate.Query hibernateQuery = ((org.hibernate.jpa.HibernateQuery) q).getHibernateQuery();
		hibernateQuery.setResultTransformer(AliasToEntityMapResultTransformer.INSTANCE);
		List<Map<String, Object>> res = hibernateQuery.list();
		if (res.size() > 0) {
			Map<String, Object> response = res.get(0);
			String path = response.get("PATH").toString();
			Map<String, Object> r = new HashMap<>(); 
			r.put("fileEncoded", Util.encodeFileToBase64(path));
			r.put("filename",  response.get("NAME").toString());
			return r;
		}
		return null;
	}
	
	@Transactional
	public String saveFile(MultipartFile multipartFile, String username) throws IOException, SQLException {
		String uuid = UUID.randomUUID().toString().replace("-", "");
		String originalFileName = Util.limpiarAcentos(multipartFile.getOriginalFilename());
		String filename = uuid +  originalFileName;
		String folder = config.getFilesPath();
		File file = Util.convertMultiPartFiletoFile(multipartFile, folder , filename);
		RequestQuery rq = new RequestQuery();
		Map<String, Object> params = new LinkedHashMap<>();
		params.put("NAME", originalFileName);
		params.put("CREATED_AT", new Date());
		params.put("UUID", uuid);
		String path = folder != ""?folder + "/" + filename:filename;
		params.put("PATH", path);
		params.put("USERNAME", username);
		rq.setParams(params);
		queryService.insert(rq, "FILES");
		return uuid;
	}
}
