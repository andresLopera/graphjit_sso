package com.co.lowcode.lineabase.oauth.service;

import java.io.IOException;
import java.math.BigInteger;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.co.lowcode.lineabase.model.Endpoint;
import com.co.lowcode.lineabase.model.Role;
import com.co.lowcode.lineabase.model.Route;
import com.co.lowcode.lineabase.model.User;
import com.co.lowcode.lineabase.oauth.model.LoginResponse;
import com.co.lowcode.lineabase.oauth.model.RouteResponse;
import com.co.lowcode.lineabase.oauth.model.UserRepositoryUserDetails;
import com.co.lowcode.lineabase.repository.UserRepository;
import com.co.lowcode.security.common.JwtAuthenticationConfig;
import com.co.lowcode.service.RedisService;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;

@Service
public class UserService implements UserServiceI, UserDetailsService{

	@Autowired
	private UserRepository userRepository;

	@Autowired 
	JwtAuthenticationConfig config;
	
	@Autowired
	RedisService redisService;

	@PersistenceContext
	private EntityManager entityManager;

	@Override
	public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
		User user = userRepository.findByUsername(username);
		if (user == null) {
			throw new UsernameNotFoundException(String.format("El usuario %s no existe", username));
		}
		return new UserRepositoryUserDetails(user);
	}
	@Override
	public User getUserByUserName(String username){
		return userRepository.findByUsername(username);
	}
	@Override
	public Set<Role> findRoleByUsername(String username) {
		User user = userRepository.findByUsername(username);
		if (user == null) {
			return null;
		}
		Set<Role> r = user.getRoles();
		for (Role role : r) {
			Set<Route> opcion = role.getRoutes();
			for (Route o : opcion) {
				o.setRoles(null);
			}
		}
		return r;
	}

	@Override
	public List<RouteResponse> findOpcionByUsernameAndApp(String username,String app_name) {

		Query q = entityManager.createNativeQuery("select distinct o.id_route, o.idParent, o.name, "
				+ "									o.path, o.type, c.uuid as component_id, o.icon, o.menuOrder "
				+ "						from users u "
				+ "				        inner join role_users ur on u.id_user = ur.user_id "
				+ "				        inner join role r2 on ur.role_id = r2.id_role "
				+ "				        inner join role_route ro on r2.id_role = ro.role_id "
				+ "				        inner join route o on ro.route_id = o.id_route "
				+ "				        inner join app_route ao on o.id_route = ao.route_id "
				+ "				        inner join app a on ao.app_id = a.id_app "
				+ "                     inner join component c on c.id_component = o.component_id_component "
				+ "				 where username = :username and  a.name = :app_name order by o.menuOrder asc");

		q.setParameter("username", username);
		q.setParameter("app_name", app_name);
		List<Object[]> result =  q.getResultList();
		List<RouteResponse> opciones = new ArrayList<>();
		for(Object[] o : result) {
			RouteResponse opcion = new RouteResponse();
			if(o[0]!=null) {
				opcion.setIdRoute((((BigInteger)o[0])).longValue());
			}
			if(o[1]!=null) {
				opcion.setIdParent((((BigInteger)o[1])).longValue());
			}
			 
			
			opcion.setNameRoute((String)o[2]);
			opcion.setPath((String)o[3]);
			opcion.setType((String) o[4]);	
			opcion.setComponentId((String) o[5]);
			opcion.setIcon(o[6]!=null?(String) o[6]:null);
			opcion.setMenuOrder(o[7]!=null?(Integer) o[7]:null);
			
			opciones.add(opcion);
		}

		return opciones;
	}

	
	@Override
	public List<RouteResponse> findOpcionByApiTokenAndApp(String apiToken, String app_name) {

		Query q = entityManager.createNativeQuery("select distinct o.name, o.path, o.type, c.uuid as component_id from users u "
				+ "				        inner join role_users ur on u.id_user = ur.user_id "
				+ "				        inner join role r2 on ur.role_id = r2.id_role "
				+ "				        inner join role_route ro on r2.id_role = ro.role_id "
				+ "				        inner join route o on ro.route_id = o.id_route "
				+ "				        inner join app_route ao on o.id_route = ao.route_id "
				+ "				        inner join app a on ao.app_id = a.id_app "
				+ "                     inner join component c on c.id_component = o.component_id_component "
				+ "				 where username = :apiToken and  a.name = :app_name");

		q.setParameter("apiToken", apiToken);
		q.setParameter("app_name", app_name);
		List<Object[]> result =  q.getResultList();
		List<RouteResponse> opciones = new ArrayList<>();
		for(Object[] o : result) {
			RouteResponse opcion = new RouteResponse();
			/*	if(o[0]!=null) {
				opcion.setIdRoute((((BigDecimal)o[0])).longValue());
			}
			if(o[1]!=null) {
				opcion.setIdParent((((BigDecimal)o[1])).longValue());
			}
			 */
			opcion.setNameRoute((String)o[0]);
			opcion.setPath((String)o[1]);
			opcion.setType((String) o[2]);	
			opcion.setComponentId((String) o[3]);
			opciones.add(opcion);
		}

		return opciones;
	}
	@Override
	public void saveRefreshToken(String login, String refreshToken) {
		User user =  userRepository.findByUsername(login);
		user.setRefreshToken(refreshToken);
		userRepository.save(user);
	}

	@Override
	public LoginResponse getToken(HttpServletRequest request, String refreshToken, String appName) throws Exception {
		String tokenReq = request.getHeader("authorization").substring(7);
		Claims claims = Jwts.parser()
				.setSigningKey(config.getSecret().getBytes())
				.parseClaimsJws(tokenReq)
				.getBody();
		String username = claims.getSubject();
		List<String> authorites = new ArrayList<>();

		User user = userRepository.findByUsername(username);
		if(user.getRefreshToken().equals(refreshToken)) {
			LoginResponse loginResponse = new LoginResponse();
			List<RouteResponse> routes = findOpcionByUsernameAndApp(username, appName);
			loginResponse.setRoutes(routes);
			String refreshTokenGen = UUID.randomUUID().toString();
			loginResponse.setRefreshToken(refreshTokenGen);
			saveRefreshToken(username,refreshTokenGen);

			List<Endpoint> endpoints  = new ArrayList<>();
			Set<Role> roles = user.getRoles();
			for(Role r : roles) {
				authorites.add(r.getAuthority());
				for(Endpoint e: r.getEndpoint()) {
					e.setId(null);
					endpoints.add(e);
				}
			}
			String uuid = UUID.randomUUID().toString();
			Instant now = Instant.now();
			String token = Jwts.builder()
					.setSubject(username)
					.claim("authorities", authorites)
					//.claim("endpoints",endpoints)
					.claim("uuid", uuid)
					.setIssuedAt(Date.from(now))
					.setExpiration(Date.from(now.plusSeconds(config.getExpiration())))
					.signWith(SignatureAlgorithm.HS256, config.getSecret().getBytes())

					.compact();
			loginResponse.setToken(token);
			
			 try {
		        	redisService.setValue(String.format("%s:%s", user.getUsername().toLowerCase(), uuid),
		        						  endpoints, TimeUnit.SECONDS, 3600L, true);
		        	
		        	
		     }catch(Exception e) {
		        	throw new Exception("ERROR RD: Comuniquese con el administrador");
		     }

			return loginResponse;
		}
		return null;
	}

	@Override
	public LoginResponse getApiToken(String apiToken, String appName) throws IOException {
	
		List<String> authorites = new ArrayList<>();

		User user = userRepository.findByApiToken(apiToken);
		
		if(user != null) {
			LoginResponse loginResponse = new LoginResponse();
			List<RouteResponse> routes = findOpcionByApiTokenAndApp(apiToken, appName);
			loginResponse.setRoutes(routes);
			String refreshTokenGen = UUID.randomUUID().toString();
			loginResponse.setRefreshToken(refreshTokenGen);
			saveRefreshToken(user.getUsername(),refreshTokenGen);

			List<Endpoint> endpoints  = new ArrayList<>();
			Set<Role> roles = user.getRoles();
			for(Role r : roles) {
				authorites.add(r.getAuthority());
				for(Endpoint e: r.getEndpoint()) {
					e.setId(null);
					endpoints.add(e);
				}
			}
			String uuid = UUID.randomUUID().toString();
			Instant now = Instant.now();
			String token = Jwts.builder()
					.setSubject(user.getUsername())
					.claim("authorities", authorites)
					//.claim("endpoints",endpoints)
					.claim("uuid", uuid)
					.setIssuedAt(Date.from(now))
					.setExpiration(Date.from(now.plusSeconds(config.getExpiration())))
					.signWith(SignatureAlgorithm.HS256, config.getSecret().getBytes())

					.compact();
			loginResponse.setToken(token);
			
			 try {
		        	redisService.setValue(String.format("%s:%s", user.getUsername().toLowerCase(), uuid),
		        						  endpoints, TimeUnit.SECONDS, 3600L, true);
		     }catch(Exception e) {
		        	throw new IOException("ERROR RD: Comuniquese con el administrador");
		     }

			return loginResponse;

		}

		return null;
		

	}
	
}
