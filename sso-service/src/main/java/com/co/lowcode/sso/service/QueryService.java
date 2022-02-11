package com.co.lowcode.sso.service;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import javax.sql.DataSource;
import javax.transaction.Transactional;

import org.hibernate.transform.AliasToEntityMapResultTransformer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import com.co.lowcode.sso.model.RequestQuery;

@Service
public class QueryService {
	@PersistenceContext
	private EntityManager entityManager;

	@Autowired
	DataSource dataSource;

	public List<Map<String, Object>> getResultQuery(String query, RequestQuery requestQuery) {

		Query q = entityManager.createNativeQuery(query);
		q.getHints();

		Set<javax.persistence.Parameter<?>> params = q.getParameters();
		for (javax.persistence.Parameter<?> p : params) {
			String paramName = p.getName();

			Map<String, Object> m = (LinkedHashMap<String, Object>) requestQuery.getParams();

			Map<String, Object> map = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
			if (m != null) {
				map.putAll(m);
			}
			if (map.get(paramName) != null) {
				Class clazz = map.get(paramName).getClass();
				if (clazz == String.class || clazz == Integer.class || clazz == BigDecimal.class
						|| clazz == Double.class || clazz == Boolean.class) {
					q.setParameter(paramName, map.get(paramName));
				} else {
					LinkedHashMap<String, Object> param = (LinkedHashMap<String, Object>) map.get(paramName);
					q.setParameter(paramName, param.get("CODE"));
				}
			} else {
				q.setParameter(paramName, "");
			}
		}

		org.hibernate.Query hibernateQuery = ((org.hibernate.jpa.HibernateQuery) q).getHibernateQuery();

		hibernateQuery.setResultTransformer(AliasToEntityMapResultTransformer.INSTANCE);
		List<Map<String, Object>> res = hibernateQuery.list();

		return res;
	}

	public Map<String, Object> query(String uuid) {

		String query = "SELECT ID_REPORT, QUERY, TYPE, PUBLIC_END, CAPTCHA  FROM REPORT WHERE UUID = ?0";
		Query q = entityManager.createNativeQuery(query);
		q.setParameter(0, uuid);
		org.hibernate.Query hibernateQuery = ((org.hibernate.jpa.HibernateQuery) q).getHibernateQuery();
		hibernateQuery.setResultTransformer(AliasToEntityMapResultTransformer.INSTANCE);
		List<Map<String, Object>> res = hibernateQuery.list();
		if (res.size() > 0) {

			Map<String, Object> response = res.get(0);

			query = "SELECT NAME,ALIAS,COLUMNORDER,MASK FROM REPORT_DETAIL RD INNER JOIN REPORT_REPORT_DETAIL RRD on "
					+ "RD.ID_REPORT_DETAIL = RRD.REPORT_DETAIL_ID "
					+ "WHERE RRD.REPORT_ID = ?0 ORDER BY COLUMNORDER ASC";

			q = entityManager.createNativeQuery(query);
			q.setParameter(0, response.get("ID_REPORT"));
			hibernateQuery = ((org.hibernate.jpa.HibernateQuery) q).getHibernateQuery();
			hibernateQuery.setResultTransformer(AliasToEntityMapResultTransformer.INSTANCE);
			List<Map<String, Object>> res2 = hibernateQuery.list();
			response.put("DETAIL", res2);

			if (response.get("TYPE").equals("CHART")) {

				query = "SELECT C.IDCHART, C.NAME, C.QUERY FROM CHART C INNER JOIN REPORT_CHART RC ON C.IDCHART = RC.CHART_ID "
						+ " INNER JOIN REPORT R ON R.IDREPORT = RC.REPORT_ID WHERE RC.REPORT_ID = ?0";

				q = entityManager.createNativeQuery(query);
				q.setParameter(0, response.get("IDREPORT"));
				hibernateQuery = ((org.hibernate.jpa.HibernateQuery) q).getHibernateQuery();
				hibernateQuery.setResultTransformer(AliasToEntityMapResultTransformer.INSTANCE);
				List<Map<String, Object>> res3 = hibernateQuery.list();
				if (res3.size() > 0) {
					List<Map<String, Object>> charts = new ArrayList<Map<String, Object>>();
					for (Map<String, Object> m : res3) {
						BigDecimal idChart = (BigDecimal) m.get("IDCHART");
						Map<String, Object> chart = new HashMap<String, Object>();
						query = "SELECT S.NAME,S.QUERY,S.RADIUS1,S.RADIUS2,S.SMOOTH,S.TYPE, S.NAMECOLUMN, S.VALUECOLUMN FROM SERIES S "
								+ " INNER JOIN CHART_SERIES RS " + " ON S.IDSERIES = RS.SERIES_ID "
								+ " INNER JOIN CHART R ON RS.CHART_ID = R.IDCHART " + " WHERE RS.CHART_ID = ?0";

						q = entityManager.createNativeQuery(query);
						q.setParameter(0, idChart);
						hibernateQuery = ((org.hibernate.jpa.HibernateQuery) q).getHibernateQuery();
						hibernateQuery.setResultTransformer(AliasToEntityMapResultTransformer.INSTANCE);

						List<Map<String, Object>> res4 = hibernateQuery.list();
						Map<String, Object> series = new HashMap<String, Object>();
						series.put("SERIES", res4);

						query = "SELECT A.AXIS, A.QUERY, A.FORMATTER, A.INSIDE,A.TYPE, A.NAMECOLUMN  FROM AXES A INNER JOIN CHART_AXES CA"
								+ " ON A.IDAXES = CA.AXES_ID "
								+ "INNER JOIN CHART C ON CA.CHART_ID = C.IDCHART WHERE CA.CHART_ID = ?0";

						q = entityManager.createNativeQuery(query);
						q.setParameter(0, idChart);
						hibernateQuery = ((org.hibernate.jpa.HibernateQuery) q).getHibernateQuery();
						hibernateQuery.setResultTransformer(AliasToEntityMapResultTransformer.INSTANCE);
						List<Map<String, Object>> res5 = hibernateQuery.list();
						Map<String, Object> axes = new HashMap<String, Object>();
						// axes.put("AXES", );
						chart.put("AXES", res5);
						chart.put("SERIES", res4);
						chart.put("QUERY", (String) m.get("QUERY"));
						chart.put("NAME", (String) m.get("NAME"));
						charts.add(chart);
					}
					response.put("CHARTS", charts);
				}
			}

			query = "SELECT BUTTON, UUIDREPORT, CONFIRM, TYPE FROM REPORT_ACTION RD INNER JOIN REPORT_REPORT_ACTION RRD on "
					+ "RD.ID_REPORT_ACTION = RRD.REPORT_ACTION_ID " + "WHERE RRD.REPORT_ID = ?0";

			q = entityManager.createNativeQuery(query);
			q.setParameter(0, response.get("ID_REPORT"));
			hibernateQuery = ((org.hibernate.jpa.HibernateQuery) q).getHibernateQuery();
			hibernateQuery.setResultTransformer(AliasToEntityMapResultTransformer.INSTANCE);
			List<Map<String, Object>> res5 = hibernateQuery.list();
			response.put("ACTION", res5);

			query = "SELECT COLUMNREF, CSS, WORDS FROM REPORT_STYLE RD INNER JOIN REPORT_REPORT_STYLE RRD on "
					+ "RD.ID_STYLE = RRD.REPORT_STYLE_ID " + "WHERE RRD.REPORT_ID = ?0";

			q = entityManager.createNativeQuery(query);
			q.setParameter(0, response.get("ID_REPORT"));
			hibernateQuery = ((org.hibernate.jpa.HibernateQuery) q).getHibernateQuery();
			hibernateQuery.setResultTransformer(AliasToEntityMapResultTransformer.INSTANCE);
			List<Map<String, Object>> resstyle = hibernateQuery.list();
			response.put("STYLE", resstyle);

			return response;

		}
		return null;
	}

	public List<Map<String, Object>> getReports(String username) {
		Query q = entityManager.createNativeQuery("SELECT distinct DESCRIPTION,NAME,UUID, TYPE FROM REPORT R "
				+ "INNER JOIN ROLE_REPORT RR ON r.idreport = rr.report_id "
				+ "INNER JOIN USUARIO_ROLE UR ON ur.role_id = rr.role_id "
				+ "INNER JOIN USUARIO U ON u.id_usuario = ur.user_id " + "WHERE u.username = ?0");
		q.setParameter(0, username);
		org.hibernate.Query hibernateQuery = ((org.hibernate.jpa.HibernateQuery) q).getHibernateQuery();

		hibernateQuery.setResultTransformer(AliasToEntityMapResultTransformer.INSTANCE);
		List<Map<String, Object>> res = hibernateQuery.list();
		return res;
	}

	public List<Map<String, Object>> getReport(String uuid, String username) {
		Query q = entityManager.createNativeQuery("SELECT distinct DESCRIPTION,NAME,UUID, TYPE FROM REPORT R "
				+ "INNER JOIN ROLE_REPORT RR ON r.idreport = rr.report_id "
				+ "INNER JOIN USUARIO_ROLE UR ON ur.role_id = rr.role_id "
				+ "INNER JOIN USUARIO U ON u.id_usuario = ur.user_id " + "WHERE u.username = ?0 and R.UUID = ?1");
		q.setParameter(0, username);
		q.setParameter(1, uuid);
		org.hibernate.Query hibernateQuery = ((org.hibernate.jpa.HibernateQuery) q).getHibernateQuery();

		hibernateQuery.setResultTransformer(AliasToEntityMapResultTransformer.INSTANCE);
		List<Map<String, Object>> res = hibernateQuery.list();
		return res;
	}

	public List<Map<String, Object>> getFilters(String uuidReport) {

		String query = " SELECT RF.UUID, RF.NAME, RF.LABEL, RF.UUIDPARENT, RF.NAMEIDPARENT,"
				+ " RF.TYPE, RF.COLUMNORDER, RF.IDPARAMETER, RF.ENDPOINT " + " FROM REPORT R "
				+ "     INNER JOIN REPORT_REPORT_FILTER RRF " + "	ON R.IDREPORT = RRF.REPORT_ID "
				+ "    INNER JOIN REPORT_FILTER RF on RRF.REPORT_FILTER_ID = RF.ID_REPORT_FILTER "
				+ "	WHERE R.UUID = ?0 ORDER BY RF.COLUMNORDER ASC";

		Query q = entityManager.createNativeQuery(query);
		q = entityManager.createNativeQuery(query);
		q.setParameter(0, uuidReport);
		org.hibernate.Query hibernateQuery = ((org.hibernate.jpa.HibernateQuery) q).getHibernateQuery();
		hibernateQuery.setResultTransformer(AliasToEntityMapResultTransformer.INSTANCE);
		List<Map<String, Object>> res3 = hibernateQuery.list();
		if (res3.size() > 0) {
			for (Map<String, Object> map : res3) {
				String uuidFilter = map.get("UUID").toString();
				List<Map<String, Object>> validators = getValidator(uuidFilter);
				if (validators.size() > 0) {
					map.put("VALIDATOR", validators);
				}
			}
		}
		return res3;
	}

	public List<Map<String, Object>> getValidator(String uuidFilter) {

		String query = "SELECT FV.TYPE,FV.NAME,FV.DETAIL,FV.MESSAGE FROM REPORT_FILTER RF "
				+ "    INNER JOIN REPORT_FILTER_FILTER_VALIDATOR RFFV on RF.ID_REPORT_FILTER = RFFV.REPORT_FILTER_ID "
				+ "    INNER JOIN FILTER_VALIDATOR FV on RFFV.FILTER_VALIDATOR_ID = FV.IDVALIDATOR "
				+ "WHERE RF.UUID = ?0 ";
		Query q = entityManager.createNativeQuery(query);
		q.setParameter(0, uuidFilter);
		org.hibernate.Query hibernateQuery = ((org.hibernate.jpa.HibernateQuery) q).getHibernateQuery();
		hibernateQuery.setResultTransformer(AliasToEntityMapResultTransformer.INSTANCE);
		List<Map<String, Object>> res = hibernateQuery.list();

		return res;
	}

	public List<Map<String, Object>> getParameter(String uuidReportFilter, RequestQuery requestQuery) {

		String query = "SELECT IDPARAMETER, QUERY FROM REPORT_FILTER WHERE UUID = ?0";
		Query q = entityManager.createNativeQuery(query);
		q.setParameter(0, uuidReportFilter);
		org.hibernate.Query hibernateQuery = ((org.hibernate.jpa.HibernateQuery) q).getHibernateQuery();
		hibernateQuery.setResultTransformer(AliasToEntityMapResultTransformer.INSTANCE);
		List<Map<String, Object>> res = hibernateQuery.list();
		BigDecimal idParameter = null;
		String queryParameter = "";
		if (res.size() > 0) {
			for (Map<String, Object> map : res) {
				idParameter = (BigDecimal) map.get("IDPARAMETER");
				queryParameter = map.get("QUERY") != null ? map.get("QUERY").toString() : null;
			}
		}

		if (idParameter == null && queryParameter == null && requestQuery.getParam() != null) {
			String query2 = "SELECT ID_PARAMETER,CODE,IDPARENT,NAME FROM PARAMETER WHERE IDPARENT = ?0";
			Query q2 = entityManager.createNativeQuery(query2);
			q2.setParameter(0, requestQuery.getParam());
			org.hibernate.Query hibernateQuery2 = ((org.hibernate.jpa.HibernateQuery) q2).getHibernateQuery();
			hibernateQuery2.setResultTransformer(AliasToEntityMapResultTransformer.INSTANCE);
			return hibernateQuery2.list();
		}

		if (idParameter != null) {
			String query2 = "SELECT ID_PARAMETER,CODE,IDPARENT,NAME FROM PARAMETER WHERE IDPARENT = ?0";
			Query q2 = entityManager.createNativeQuery(query2);
			q2.setParameter(0, idParameter);
			org.hibernate.Query hibernateQuery2 = ((org.hibernate.jpa.HibernateQuery) q2).getHibernateQuery();
			hibernateQuery2.setResultTransformer(AliasToEntityMapResultTransformer.INSTANCE);
			return hibernateQuery2.list();
		}

		if (queryParameter != null) {
			Query q2 = entityManager.createNativeQuery(queryParameter);
			org.hibernate.Query hibernateQuery2 = ((org.hibernate.jpa.HibernateQuery) q2).getHibernateQuery();
			hibernateQuery2.setResultTransformer(AliasToEntityMapResultTransformer.INSTANCE);
			return hibernateQuery2.list();
		}

		return null;
	}

	@Transactional
	public Map<String, Object> insert(RequestQuery requestQuery, String tableName) throws SQLException {

		Map<String, Object> mapResponse = new LinkedHashMap<String, Object>();
		Map<String, Object> paramsRequestQuery = (LinkedHashMap<String, Object>) requestQuery.getParams();
		String s1 = "";
		String s2 = "";
		int i = 1;
		for (Map.Entry<String, Object> entry : paramsRequestQuery.entrySet()) {
			if (paramsRequestQuery.entrySet().size() == i) {
				s1 = s1 + entry.getKey();
				s2 = s2 + ":" + entry.getKey();
				break;
			}
			s1 = s1 + entry.getKey() + ",";
			s2 = s2 + ":" + entry.getKey() + ",";
			i++;
		}
		String query = String.format("INSERT INTO %s (%s)  VALUES (%s)", tableName, s1, s2);

		Query q = entityManager.createNativeQuery(query);
		Set<javax.persistence.Parameter<?>> params = q.getParameters();
		for (javax.persistence.Parameter<?> p : params) {
			String paramName = p.getName();

			Map<String, Object> m = (LinkedHashMap<String, Object>) requestQuery.getParams();

			Map<String, Object> map = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);

			map.putAll(m);

			if (map.get(paramName) != null) {
				Class clazz = map.get(paramName).getClass();
				if (clazz == String.class || clazz == Integer.class || 
						clazz == Date.class || clazz == Boolean.class || 
						clazz == BigDecimal.class || clazz == Long.class) {
					q.setParameter(paramName, map.get(paramName));
				} else {
					LinkedHashMap<String, Object> param = (LinkedHashMap<String, Object>) map.get(paramName);
					q.setParameter(paramName, param.get("CODE"));
				}
			} else {
				q.setParameter(paramName, null);
			}
		}
		int affectedRows = q.executeUpdate();

		if (affectedRows == 0) {
			throw new SQLException("No hubo inserciones");
		}

		return mapResponse;
	}

	public Map<String, Object> create(Map<String, Object> paramsRequestQuery, String tableName, String keyGenerated) throws SQLException {
		Map<String, Object> mapResponse = new LinkedHashMap<String, Object>();
		String s1 = "";
		String s2 = "";
		int i = 1;
		for (Map.Entry<String, Object> entry : paramsRequestQuery.entrySet()) {
			if (paramsRequestQuery.entrySet().size() == i) {
				s1 = s1 + entry.getKey();
				s2 = s2 + "?";
				break;
			}
			s1 = s1 + entry.getKey() + ",";
			s2 = s2 + "?,";
			i++;
		}

		String query = String.format("INSERT INTO %s (%s)  VALUES (%s)", tableName, s1, s2);
		String gK[]= {keyGenerated};
		try (
			Connection connection = dataSource.getConnection();
			PreparedStatement statement = connection.prepareStatement(query, gK);
		) {
			i = 1;
			for (Map.Entry<String, Object> entry : paramsRequestQuery.entrySet()) {
				statement.setObject(i, entry.getValue());
				i++;
			}
			
			int affectedRows = statement.executeUpdate();
			if (affectedRows == 0) {
				throw new SQLException("Creating user failed, no rows affected.");
			}

			try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
				if (generatedKeys.next()) {
					mapResponse.put(keyGenerated, generatedKeys.getObject(1));
				} else {
					throw new SQLException("Creating user failed, no ID obtained.");
				}
			}
		}
		return mapResponse;
	}

	@Transactional
	public Map<String, Object> update(RequestQuery requestQuery, String tableName) {

		Map<String, Object> mapEmail = new LinkedHashMap<String, Object>();
		Map<String, Object> paramsRequestQuery = (LinkedHashMap<String, Object>) requestQuery.getParams();
		String s1 = "";
		String s2 = "";
		int i = 1;
		for (Map.Entry<String, Object> entry : paramsRequestQuery.entrySet()) {
			if (paramsRequestQuery.entrySet().size() == i) {
				s1 = s1 + entry.getKey() + "=:" + entry.getKey();
				break;
			}
			s1 = s1 + entry.getKey() + "=:" + entry.getKey() + ",";
			i++;
		}
		i = 1;
		Map<String, Object> whereRequestQuery = (LinkedHashMap<String, Object>) requestQuery.getWhere();
		for (Map.Entry<String, Object> entry : whereRequestQuery.entrySet()) {
			if (whereRequestQuery.entrySet().size() == i) {
				s2 = s2 + entry.getKey() + "=:" + entry.getKey();
				break;
			}
			s2 = s2 + entry.getKey() + "=:" + entry.getKey() + ",";
			i++;
		}

		String query = String.format("UPDATE  %s SET %s  WHERE %s", tableName, s1, s2);
		Query q = entityManager.createNativeQuery(query);
		Set<javax.persistence.Parameter<?>> params = q.getParameters();
		Map<String, Object> m = (LinkedHashMap<String, Object>) requestQuery.getParams();
		Map<String, Object> map = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
		map.putAll(m);

		Map<String, Object> mwhere = (LinkedHashMap<String, Object>) requestQuery.getWhere();
		Map<String, Object> map2 = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
		map2.putAll(mwhere);

		for (javax.persistence.Parameter<?> p : params) {
			String paramName = p.getName();
			if (map.get(paramName) != null) {
				Class clazz = map.get(paramName).getClass();
				if (clazz == String.class || clazz == Integer.class || clazz == Boolean.class
						|| clazz == BigDecimal.class) {
					q.setParameter(paramName, map.get(paramName));
				} else {
					LinkedHashMap<String, Object> param = (LinkedHashMap<String, Object>) map.get(paramName);
					q.setParameter(paramName, param.get("CODE"));
				}
			} else if (map2.get(paramName) != null) {
				Class clazz = map2.get(paramName).getClass();
				if (clazz == String.class || clazz == Integer.class) {
					q.setParameter(paramName, map2.get(paramName));
				} else {
					LinkedHashMap<String, Object> param = (LinkedHashMap<String, Object>) map2.get(paramName);
					q.setParameter(paramName, param.get("CODE"));
				}
			} else {
				q.setParameter(paramName, null);
			}
		}
		q.executeUpdate();
		return mapEmail;
	}

}